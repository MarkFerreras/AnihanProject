package com.example.springboot.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;

/**
 * Builds the encoding sheet the Registrar fills in: one row per student, with the
 * Student Number column last and blank for anyone who does not have one yet.
 *
 * <p>Headers are written on row 1 with no preamble and use the canonical column names in
 * {@link StudentNumberImportMapping}, so an exported file re-imports with no cleanup —
 * the Registrar edits one column and uploads it back.
 *
 * <p>Modelled on {@link SystemLogExportService}; the CSV escaping rules are the same.
 */
@Service
public class StudentNumberExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SystemLogExportFile export(StudentNumberExportFormat format,
                                      List<StudentRecordSummaryResponse> students) {
        byte[] content = switch (format) {
            case CSV -> buildCsv(students);
            case XLSX -> buildXlsx(students);
        };
        String fileName = "student-numbers-" + DATE_FORMAT.format(LocalDate.now()) + "." + format.extension();
        return new SystemLogExportFile(content, format.mediaType(), fileName);
    }

    private byte[] buildCsv(List<StudentRecordSummaryResponse> students) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", StudentNumberImportMapping.EXPORT_COLUMNS)).append("\r\n");

        for (StudentRecordSummaryResponse s : students) {
            for (int i = 0; i < 10; i++) {
                if (i > 0) {
                    csv.append(',');
                }
                csv.append(csvEscape(valueAt(s, i)));
            }
            csv.append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildXlsx(List<StudentRecordSummaryResponse> students) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Student Numbers");

            // Force the Student Number column to text so Excel cannot eat a leading zero
            // ("0012" must not become 12) or reformat a long number into scientific notation.
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));

            Row header = sheet.createRow(0);
            for (int i = 0; i < StudentNumberImportMapping.EXPORT_COLUMNS.length; i++) {
                header.createCell(i).setCellValue(StudentNumberImportMapping.EXPORT_COLUMNS[i]);
            }

            for (int r = 0; r < students.size(); r++) {
                StudentRecordSummaryResponse s = students.get(r);
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < StudentNumberImportMapping.EXPORT_COLUMNS.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(valueAt(s, c));
                    if (c == 9) {
                        cell.setCellStyle(textStyle);
                    }
                }
            }

            for (int i = 0; i < StudentNumberImportMapping.EXPORT_COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate the XLSX export", e);
        }
    }

    /** Column order must match {@link StudentNumberImportMapping#EXPORT_COLUMNS}. */
    private String valueAt(StudentRecordSummaryResponse s, int column) {
        return switch (column) {
            case 0 -> blankToEmpty(s.studentId());
            case 1 -> s.recordId() != null ? s.recordId().toString() : "";
            case 2 -> blankToEmpty(s.lastName());
            case 3 -> blankToEmpty(s.firstName());
            // Middle name is not on the summary DTO; the column is exported for the
            // registrar's context and is ignored on import.
            case 4 -> "";
            case 5 -> blankToEmpty(s.batchCode());
            case 6 -> blankToEmpty(s.courseCode());
            case 7 -> blankToEmpty(s.sectionCode());
            case 8 -> blankToEmpty(s.studentStatus());
            // Left blank when unassigned — this is the cell the registrar fills in.
            case 9 -> blankToEmpty(s.studentNumber());
            default -> "";
        };
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String csvEscape(String value) {
        String safe = value != null ? value : "";
        String escaped = safe.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
