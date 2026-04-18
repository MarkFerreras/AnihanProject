package com.example.springboot.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;

import com.example.springboot.dto.SystemLogResponse;

@Service
public class SystemLogExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] COLUMN_HEADERS = {
            "Date & Time", "User ID", "Username", "Role", "Action", "IP Address"
    };

    public SystemLogExportFile export(SystemLogExportFormat format, SystemLogQueryResult queryResult) {
        byte[] content = switch (format) {
            case CSV -> buildCsv(queryResult);
            case XLSX -> buildXlsx(queryResult);
            case DOCX -> buildDocx(queryResult);
        };

        return new SystemLogExportFile(content, format.mediaType(), buildFileName(format, queryResult));
    }

    private byte[] buildCsv(SystemLogQueryResult queryResult) {
        StringBuilder csv = new StringBuilder();
        csv.append("System Logs Export").append("\r\n");
        csv.append("Selected Range,")
                .append(csvEscape(formatRange(queryResult.windowStart(), queryResult.windowEnd())))
                .append("\r\n");
        csv.append("Exported At,")
                .append(csvEscape(TIMESTAMP_FORMAT.format(LocalDateTime.now())))
                .append("\r\n");
        csv.append("\r\n");
        csv.append(String.join(",", COLUMN_HEADERS)).append("\r\n");

        for (SystemLogResponse log : queryResult.logs()) {
            csv.append(csvEscape(formatTimestamp(log.timestamp()))).append(',')
                    .append(csvEscape(log.userId() != null ? log.userId().toString() : "-")).append(',')
                    .append(csvEscape(log.username())).append(',')
                    .append(csvEscape(cleanRole(log.role()))).append(',')
                    .append(csvEscape(log.action())).append(',')
                    .append(csvEscape(log.ipAddress() != null ? log.ipAddress() : "-"))
                    .append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildXlsx(SystemLogQueryResult queryResult) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("System Logs");

            sheet.createRow(0).createCell(0).setCellValue("System Logs Export");
            sheet.createRow(1).createCell(0).setCellValue("Selected Range");
            sheet.getRow(1).createCell(1).setCellValue(
                    formatRange(queryResult.windowStart(), queryResult.windowEnd()));
            sheet.createRow(2).createCell(0).setCellValue("Exported At");
            sheet.getRow(2).createCell(1).setCellValue(TIMESTAMP_FORMAT.format(LocalDateTime.now()));

            Row headerRow = sheet.createRow(4);
            for (int i = 0; i < COLUMN_HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(COLUMN_HEADERS[i]);
            }

            List<SystemLogResponse> logs = queryResult.logs();
            for (int i = 0; i < logs.size(); i++) {
                SystemLogResponse log = logs.get(i);
                Row row = sheet.createRow(i + 5);
                row.createCell(0).setCellValue(formatTimestamp(log.timestamp()));
                row.createCell(1).setCellValue(log.userId() != null ? log.userId().toString() : "-");
                row.createCell(2).setCellValue(defaultString(log.username()));
                row.createCell(3).setCellValue(cleanRole(log.role()));
                row.createCell(4).setCellValue(defaultString(log.action()));
                row.createCell(5).setCellValue(log.ipAddress() != null ? log.ipAddress() : "-");
            }

            for (int i = 0; i < COLUMN_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate XLSX export", e);
        }
    }

    private byte[] buildDocx(SystemLogQueryResult queryResult) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            titleRun.setText("System Logs Export");

            XWPFParagraph metaParagraph = document.createParagraph();
            XWPFRun metaRun = metaParagraph.createRun();
            metaRun.setText("Selected Range: "
                    + formatRange(queryResult.windowStart(), queryResult.windowEnd()));
            metaRun.addBreak();
            metaRun.setText("Exported At: " + TIMESTAMP_FORMAT.format(LocalDateTime.now()));

            XWPFTable table = document.createTable(queryResult.logs().size() + 1, COLUMN_HEADERS.length);
            for (int i = 0; i < COLUMN_HEADERS.length; i++) {
                table.getRow(0).getCell(i).setText(COLUMN_HEADERS[i]);
            }

            for (int i = 0; i < queryResult.logs().size(); i++) {
                SystemLogResponse log = queryResult.logs().get(i);
                table.getRow(i + 1).getCell(0).setText(formatTimestamp(log.timestamp()));
                table.getRow(i + 1).getCell(1).setText(log.userId() != null ? log.userId().toString() : "-");
                table.getRow(i + 1).getCell(2).setText(defaultString(log.username()));
                table.getRow(i + 1).getCell(3).setText(cleanRole(log.role()));
                table.getRow(i + 1).getCell(4).setText(defaultString(log.action()));
                table.getRow(i + 1).getCell(5).setText(log.ipAddress() != null ? log.ipAddress() : "-");
            }

            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate DOCX export", e);
        }
    }

    private String buildFileName(SystemLogExportFormat format, SystemLogQueryResult queryResult) {
        return "system-logs-"
                + DATE_FORMAT.format(queryResult.windowStart().toLocalDate())
                + "_to_"
                + DATE_FORMAT.format(queryResult.windowEnd().toLocalDate())
                + "."
                + format.extension();
    }

    private String formatRange(LocalDateTime start, LocalDateTime end) {
        return DATE_FORMAT.format(start.toLocalDate()) + " to " + DATE_FORMAT.format(end.toLocalDate());
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        return timestamp != null ? TIMESTAMP_FORMAT.format(timestamp) : "-";
    }

    private String cleanRole(String role) {
        return defaultString(role).replace("ROLE_", "");
    }

    private String defaultString(String value) {
        return value != null ? value : "-";
    }

    private String csvEscape(String value) {
        String safeValue = value != null ? value : "-";
        String escaped = safeValue.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
