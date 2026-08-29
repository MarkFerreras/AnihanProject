package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.service.StudentNumberSheetParser.ParsedRow;

class StudentNumberExportServiceTest {

    private final StudentNumberExportService exportService = new StudentNumberExportService();
    private final StudentNumberSheetParser parser = new StudentNumberSheetParser();

    private StudentRecordSummaryResponse student(String ref, String number, String last, String first) {
        return new StudentRecordSummaryResponse(
                1, ref, number, last, first, "B2026A", "CARS", "SEC-A", "Active");
    }

    @Test
    void csvStartsWithTheCanonicalHeaderRow() {
        SystemLogExportFile file = exportService.export(
                StudentNumberExportFormat.CSV, List.of(student("SR20260001", null, "Ligan", "Sean")));

        String csv = new String(file.content(), StandardCharsets.UTF_8);
        String header = csv.split("\r\n")[0];

        assertEquals(String.join(",", StudentNumberImportMapping.EXPORT_COLUMNS), header,
                "The header must match the import aliases exactly, or the file will not round-trip");
    }

    @Test
    void unassignedStudentsGetAnEmptyStudentNumberCell() {
        SystemLogExportFile file = exportService.export(
                StudentNumberExportFormat.CSV, List.of(student("SR20260001", null, "Ligan", "Sean")));

        String dataRow = new String(file.content(), StandardCharsets.UTF_8).split("\r\n")[1];

        assertTrue(dataRow.endsWith(","), "Student Number is the last column and must be left blank: " + dataRow);
    }

    @Test
    void csvEscapesCommasInNames() {
        SystemLogExportFile file = exportService.export(
                StudentNumberExportFormat.CSV, List.of(student("SR20260001", null, "Ligan, Jr.", "Sean")));

        String csv = new String(file.content(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Ligan, Jr.\""), csv);
    }

    @Test
    void fileNameCarriesTheFormatExtension() {
        assertTrue(exportService.export(StudentNumberExportFormat.CSV, List.of()).fileName().endsWith(".csv"));
        assertTrue(exportService.export(StudentNumberExportFormat.XLSX, List.of()).fileName().endsWith(".xlsx"));
    }

    // ----- Round trip: the guarantee the whole feature rests on -----

    @Test
    void anExportedCsvParsesBackCleanly() {
        SystemLogExportFile file = exportService.export(StudentNumberExportFormat.CSV, List.of(
                student("SR20260001", null, "Ligan", "Sean"),
                student("SR20260002", "2026-002", "Reyes", "Anna")));

        List<ParsedRow> rows = parser.parse(file.content(), "csv");

        assertEquals(2, rows.size());
        assertEquals("SR20260001", rows.get(0).reference());
        assertNull(rows.get(0).studentNumber(), "A blank cell must come back as null, not \"\"");
        assertEquals("Ligan", rows.get(0).lastName());
        assertEquals("2026-002", rows.get(1).studentNumber());
    }

    @Test
    void anExportedXlsxParsesBackCleanly() {
        SystemLogExportFile file = exportService.export(StudentNumberExportFormat.XLSX, List.of(
                student("SR20260001", null, "Ligan", "Sean"),
                student("SR20260002", "2026-002", "Reyes", "Anna")));

        List<ParsedRow> rows = parser.parse(file.content(), "xlsx");

        assertEquals(2, rows.size());
        assertEquals("SR20260001", rows.get(0).reference());
        assertNull(rows.get(0).studentNumber());
        assertEquals("2026-002", rows.get(1).studentNumber());
    }

    /** The text cell style on the Student Number column is what protects this. */
    @Test
    void leadingZerosSurviveAnXlsxRoundTrip() {
        SystemLogExportFile file = exportService.export(StudentNumberExportFormat.XLSX,
                List.of(student("SR20260001", "0012", "Ligan", "Sean")));

        assertEquals("0012", parser.parse(file.content(), "xlsx").get(0).studentNumber());
    }

    @Test
    void exportingNoStudentsStillProducesAUsableTemplate() {
        SystemLogExportFile file = exportService.export(StudentNumberExportFormat.XLSX, List.of());

        // Header-only: parses without error and yields zero rows.
        assertEquals(0, parser.parse(file.content(), "xlsx").size());
    }
}
