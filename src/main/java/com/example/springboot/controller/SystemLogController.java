package com.example.springboot.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.SystemLogResponse;
import com.example.springboot.service.SystemLogExportFile;
import com.example.springboot.service.SystemLogExportFormat;
import com.example.springboot.service.SystemLogExportService;
import com.example.springboot.service.SystemLogService;

@RestController
@RequestMapping("/api/logs")
public class SystemLogController {

    private final SystemLogService systemLogService;
    private final SystemLogExportService systemLogExportService;

    public SystemLogController(SystemLogService systemLogService,
                               SystemLogExportService systemLogExportService) {
        this.systemLogService = systemLogService;
        this.systemLogExportService = systemLogExportService;
    }

    /**
     * GET /api/logs
     * Returns system logs filtered by date range, ordered by timestamp descending.
     * Restricted to ADMIN role via SecurityConfig.
     * <p>
     * Query parameters (all optional):
     * <ul>
     *   <li>{@code rangeDays} — preset day range (7, 14, or 30)</li>
     *   <li>{@code startDate} — custom range start in YYYY-MM-DD</li>
     *   <li>{@code endDate} — custom range end in YYYY-MM-DD</li>
     * </ul>
     * If no parameters are provided, defaults to the last 7 days.
     * If both startDate and endDate are present, the custom range takes precedence.
     * Returns 400 if startDate is after endDate.
     */
    @GetMapping
    public ResponseEntity<List<SystemLogResponse>> getLogs(
            @RequestParam(required = false) Integer rangeDays,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(systemLogService.getLogs(rangeDays, startDate, endDate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam String format,
            @RequestParam(required = false) Integer rangeDays,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            SystemLogExportFormat exportFormat = SystemLogExportFormat.from(format);
            SystemLogExportFile exportFile = systemLogExportService.export(
                    exportFormat,
                    systemLogService.queryLogs(rangeDays, startDate, endDate)
            );

            return ResponseEntity.ok()
                    .contentType(exportFile.mediaType())
                    .contentLength(exportFile.content().length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(exportFile.fileName())
                            .build()
                            .toString())
                    .body(exportFile.content());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
