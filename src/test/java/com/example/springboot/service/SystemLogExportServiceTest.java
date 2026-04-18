package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import com.example.springboot.dto.SystemLogResponse;

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
