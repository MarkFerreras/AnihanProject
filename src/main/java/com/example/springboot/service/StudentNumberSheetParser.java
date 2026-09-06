package com.example.springboot.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Reads a CSV or XLSX sheet into plain rows. Format mechanics ONLY — it performs no
 * database lookups and no business validation, which keeps it directly unit-testable
 * against sample files and easy to reason about when the school's format changes.
 *
 * <p>Which header means what is decided entirely by {@link StudentNumberImportMapping};
 * edit that file, not this one, to support a new layout.
 */
@Service
public class StudentNumberSheetParser {

    /** One data row, already normalised. Any field may be null if its column was absent or blank. */
    public record ParsedRow(
            int rowNumber,
            String reference,
            String studentNumber,
            String lastName,
            String firstName
    ) {
    }

    /** Column positions resolved from the detected header row. */
    private record HeaderLayout(int headerRowIndex, int reference, int studentNumber,
                                int lastName, int firstName) {
    }

    private static final int NOT_PRESENT = -1;

    public List<ParsedRow> parse(byte[] content, String extension) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("The uploaded file is empty.");
        }
        return switch (extension.toLowerCase()) {
            case "csv" -> parseCsv(content);
            case "xlsx" -> parseXlsx(content);
            default -> throw new IllegalArgumentException(
                    "Unsupported file type: " + extension + ". Allowed: csv, xlsx.");
        };
    }

    // ----- CSV -----

    private List<ParsedRow> parseCsv(byte[] content) {
        List<List<String>> grid = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    // Strip a UTF-8 BOM, which Excel writes and which would otherwise
                    // corrupt the first header cell.
                    line = line.replace("﻿", "");
                    first = false;
                }
                grid.add(splitCsvLine(line));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the CSV file.", e);
        }
        return toRows(grid);
    }

    /**
     * RFC-4180 splitter: fields may be quoted, quoted fields may contain commas, and a
     * doubled quote inside a quoted field is a literal quote. This is the read side of
     * the escaping {@code SystemLogExportService.csvEscape} performs.
     */
    private List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    // ----- XLSX -----

    private List<ParsedRow> parseXlsx(byte[] content) {
        List<List<String>> grid = new ArrayList<>();
        try (InputStream in = new java.io.ByteArrayInputStream(content);
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("The workbook contains no sheets.");
            }
            Sheet sheet = workbook.getSheetAt(0);
            // DataFormatter renders each cell as it appears in Excel, which keeps
            // identifiers as text instead of turning 2026001 into 2026001.0 or into
            // scientific notation.
            DataFormatter formatter = new DataFormatter();

            for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                List<String> cells = new ArrayList<>();
                if (row != null) {
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        cells.add(cell == null ? "" : formatter.formatCellValue(cell));
                    }
                }
                grid.add(cells);
            }
        } catch (IOException | org.apache.poi.EmptyFileException e) {
            throw new IllegalArgumentException("Could not read the Excel file. Is it a valid .xlsx?", e);
        }
        return toRows(grid);
    }

    // ----- Shared -----

    private List<ParsedRow> toRows(List<List<String>> grid) {
        HeaderLayout layout = detectHeader(grid);
        List<ParsedRow> rows = new ArrayList<>();

        for (int r = layout.headerRowIndex() + 1; r < grid.size(); r++) {
            List<String> cells = grid.get(r);

            String reference = cellAt(cells, layout.reference());
            String studentNumber = cellAt(cells, layout.studentNumber());
            String lastName = cellAt(cells, layout.lastName());
            String firstName = cellAt(cells, layout.firstName());

            // Skip rows that are entirely empty — trailing blank rows are normal in
            // hand-edited spreadsheets and are not an error.
            if (reference == null && studentNumber == null && lastName == null && firstName == null) {
                continue;
            }

            if (rows.size() >= StudentNumberImportMapping.MAX_DATA_ROWS) {
                throw new IllegalArgumentException(
                        "The file contains more than " + StudentNumberImportMapping.MAX_DATA_ROWS
                                + " rows, which is more than this import supports.");
            }

            // rowNumber is 1-based to match what the spreadsheet shows the registrar.
            rows.add(new ParsedRow(r + 1, reference, studentNumber, lastName, firstName));
        }

        return rows;
    }

    /**
     * Finds the header row by scanning the first {@link StudentNumberImportMapping#HEADER_SCAN_ROWS}
     * rows for one containing a recognised reference alias. This tolerates title and
     * metadata rows above the real header, which school-supplied files commonly have.
     */
    private HeaderLayout detectHeader(List<List<String>> grid) {
        Set<String> refTokens = StudentNumberImportMapping.referenceTokens();
        Set<String> numTokens = StudentNumberImportMapping.studentNumberTokens();
        Set<String> lastTokens = StudentNumberImportMapping.lastNameTokens();
        Set<String> firstTokens = StudentNumberImportMapping.firstNameTokens();

        int scanLimit = Math.min(grid.size(), StudentNumberImportMapping.HEADER_SCAN_ROWS);

        for (int r = 0; r < scanLimit; r++) {
            Map<String, Integer> positions = new HashMap<>();
            List<String> cells = grid.get(r);
            for (int c = 0; c < cells.size(); c++) {
                String token = StudentNumberImportMapping.normaliseHeader(cells.get(c));
                // First occurrence wins, so a stray repeated header later in the row
                // cannot displace the real column.
                positions.putIfAbsent(token, c);
            }

            int reference = firstMatch(positions, refTokens);
            if (reference == NOT_PRESENT) {
                continue;
            }
            int studentNumber = firstMatch(positions, numTokens);
            if (studentNumber == NOT_PRESENT) {
                throw new IllegalArgumentException(
                        "Found the reference column but no student number column. Expected a header like \""
                                + StudentNumberImportMapping.COLUMN_STUDENT_NUMBER + "\".");
            }
            return new HeaderLayout(r, reference, studentNumber,
                    firstMatch(positions, lastTokens), firstMatch(positions, firstTokens));
        }

        throw new IllegalArgumentException(
                "Could not find a header row in the first " + StudentNumberImportMapping.HEADER_SCAN_ROWS
                        + " rows. Expected a column headed \"" + StudentNumberImportMapping.COLUMN_REFERENCE
                        + "\" and one headed \"" + StudentNumberImportMapping.COLUMN_STUDENT_NUMBER + "\".");
    }

    private int firstMatch(Map<String, Integer> positions, Set<String> tokens) {
        for (String token : tokens) {
            Integer index = positions.get(token);
            if (index != null) {
                return index;
            }
        }
        return NOT_PRESENT;
    }

    private String cellAt(List<String> cells, int index) {
        if (index == NOT_PRESENT || index >= cells.size()) {
            return null;
        }
        return StudentNumberImportMapping.normaliseValue(cells.get(index));
    }
}
