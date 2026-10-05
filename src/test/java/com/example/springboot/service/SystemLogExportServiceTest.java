package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import com.example.springboot.dto.SystemLogResponse;
import com.example.springboot.repository.SystemLogRepository;

class SystemLogExportServiceTest {

    private final SystemLogExportService systemLogExportService = new SystemLogExportService();

    @Test
    void exportCsvIncludesSummaryAndHeaders() {
        SystemLogExportFile exportFile = systemLogExportService.export(SystemLogExportFormat.CSV, sampleQueryResult());
        String csv = new String(exportFile.content(), StandardCharsets.UTF_8);

        assertEquals("system-logs-2026-04-01_to_2026-04-18.csv", exportFile.fileName());
        assertTrue(csv.contains("System Logs Export"));
        assertTrue(csv.contains("Selected Range,2026-04-01 to 2026-04-18"));
        assertTrue(csv.contains("Date & Time,User ID,Username,Role,Action,IP Address"));
        assertTrue(csv.contains("\"Updated, details for registrar\""));
    }

    @Test
    void exportXlsxIncludesSummaryAndTableData() throws Exception {
        SystemLogExportFile exportFile = systemLogExportService.export(SystemLogExportFormat.XLSX, sampleQueryResult());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exportFile.content()))) {
            assertEquals("system-logs-2026-04-01_to_2026-04-18.xlsx", exportFile.fileName());
            assertEquals("System Logs Export", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Selected Range", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("2026-04-01 to 2026-04-18",
                    workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("Date & Time", workbook.getSheetAt(0).getRow(4).getCell(0).getStringCellValue());
            assertEquals("admin", workbook.getSheetAt(0).getRow(5).getCell(2).getStringCellValue());
            assertEquals("ADMIN", workbook.getSheetAt(0).getRow(5).getCell(3).getStringCellValue());
        }
    }

    @Test
    void exportDocxIncludesSummaryAndTableData() throws Exception {
        SystemLogExportFile exportFile = systemLogExportService.export(SystemLogExportFormat.DOCX, sampleQueryResult());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(exportFile.content()))) {
            assertEquals("system-logs-2026-04-01_to_2026-04-18.docx", exportFile.fileName());
            assertTrue(document.getParagraphArray(0).getText().contains("System Logs Export"));
            assertTrue(document.getParagraphArray(1).getText().contains("Selected Range: 2026-04-01 to 2026-04-18"));
            assertEquals("Date & Time", document.getTables().get(0).getRow(0).getCell(0).getText());
            assertEquals("admin", document.getTables().get(0).getRow(1).getCell(2).getText());
            assertEquals("Updated, details for registrar", document.getTables().get(0).getRow(1).getCell(4).getText());
        }
    }

    // ----- Task 13c additions (ISO 25010: Security / Functional correctness) -----

    @Test
    void logExportRespectsRangeAndEscapesCsv() {
        // Export renders exactly the rows it is handed; range filtering is the query's job
        // (SystemLogService.queryLogs), so verify both halves together with a real window.
        SystemLogRepository repo = org.mockito.Mockito.mock(SystemLogRepository.class);
        com.example.springboot.model.SystemLog inRange = new com.example.springboot.model.SystemLog(
                1, "admin", "ROLE_ADMIN", "Said \"hi\", then\nleft", "127.0.0.1");
        inRange.setTimestamp(LocalDateTime.of(2026, 4, 10, 9, 0));
        org.mockito.Mockito.when(repo.findByTimestampBetweenOrderByTimestampDesc(
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 4, 1, 0, 0)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 4, 18).atTime(java.time.LocalTime.MAX))))
                .thenReturn(List.of(inRange));
        SystemLogQueryResult result = new SystemLogService(repo)
                .queryLogs(null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 18));

        String csv = new String(systemLogExportService.export(SystemLogExportFormat.CSV, result).content(),
                StandardCharsets.UTF_8);

        assertTrue(csv.contains("Selected Range,2026-04-01 to 2026-04-18"));
        assertTrue(csv.contains("\"Said \"\"hi\"\", then\nleft\""), csv);
        assertEquals(1, result.logs().size());
        assertTrue(!csv.contains("2026-03-"), "Nothing outside the window appears");
    }

    @Test
    void logExportEmptyRangeAndInvalidRange() {
        SystemLogQueryResult empty = new SystemLogQueryResult(List.of(),
                LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 4, 18, 23, 59, 59));
        String csv = new String(systemLogExportService.export(SystemLogExportFormat.CSV, empty).content(),
                StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");
        assertEquals("Date & Time,User ID,Username,Role,Action,IP Address", lines[lines.length - 1],
                "Zero rows -> the header row is the last line");

        SystemLogRepository repo = org.mockito.Mockito.mock(SystemLogRepository.class);
        assertThrows(IllegalArgumentException.class, () -> new SystemLogService(repo)
                .queryLogs(null, LocalDate.of(2026, 4, 18), LocalDate.of(2026, 4, 1)));
        org.mockito.Mockito.verifyNoInteractions(repo);
    }

    private SystemLogQueryResult sampleQueryResult() {
        return new SystemLogQueryResult(
                List.of(new SystemLogResponse(
                        10,
                        1,
                        "admin",
                        "ROLE_ADMIN",
                        "Updated, details for registrar",
                        "127.0.0.1",
                        LocalDateTime.of(2026, 4, 18, 10, 30, 15)
                )),
                LocalDateTime.of(2026, 4, 1, 0, 0, 0),
                LocalDateTime.of(2026, 4, 18, 23, 59, 59)
        );
    }
}
