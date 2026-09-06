package com.example.springboot.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.dto.registrar.StudentNumberImportReport;
import com.example.springboot.dto.registrar.StudentNumberImportRowResult;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.StudentNumberExportFormat;
import com.example.springboot.service.StudentNumberExportService;
import com.example.springboot.service.StudentNumberImportService;
import com.example.springboot.service.SystemLogExportFile;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Bulk export/import of student numbers, kept separate from {@link RegistrarController}
 * so the student-record CRUD controller does not grow a third responsibility.
 *
 * <p>RBAC needs no extra configuration: {@code /api/registrar/**} is already REGISTRAR-only
 * in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/registrar/student-numbers")
public class StudentNumberController {

    private final RegistrarService registrarService;
    private final StudentNumberExportService exportService;
    private final StudentNumberImportService importService;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    public StudentNumberController(RegistrarService registrarService,
                                   StudentNumberExportService exportService,
                                   StudentNumberImportService importService,
                                   SystemLogService systemLogService,
                                   UserRepository userRepository) {
        this.registrarService = registrarService;
        this.exportService = exportService;
        this.importService = importService;
        this.systemLogService = systemLogService;
        this.userRepository = userRepository;
    }

    /**
     * Exports the encoding sheet for the CURRENT filter selection — so "export the students
     * without numbers" is just {@code hasStudentNumber=false} plus this endpoint.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String format,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "fromYear", required = false) Integer fromYear,
            @RequestParam(value = "toYear", required = false) Integer toYear,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "hasStudentNumber", required = false) Boolean hasStudentNumber,
            HttpServletRequest httpRequest
    ) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            throw new IllegalArgumentException("fromYear must not be greater than toYear");
        }

        StudentNumberExportFormat exportFormat = StudentNumberExportFormat.from(format);
        List<StudentRecordSummaryResponse> students =
                registrarService.getAllRecords(query, fromYear, toYear, status, hasStudentNumber);
        SystemLogExportFile file = exportService.export(exportFormat, students);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Exported student number sheet: " + file.fileName()
                        + " (" + students.size() + " students)",
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName())
                        .build()
                        .toString())
                .body(file.content());
    }

    /** Validates the uploaded sheet and reports what would happen. Writes nothing, logs nothing. */
    @PostMapping("/import/preview")
    public ResponseEntity<StudentNumberImportReport> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "allowOverwrite", defaultValue = "false") boolean allowOverwrite
    ) {
        return ResponseEntity.ok(importService.preview(file, allowOverwrite));
    }

    /**
     * Applies the sheet. Writes one {@code system_logs} row per assignment plus a summary row,
     * so a bulk import is auditable at the same granularity as a single assignment.
     */
    @PostMapping("/import/apply")
    public ResponseEntity<StudentNumberImportReport> applyImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "allowOverwrite", defaultValue = "false") boolean allowOverwrite,
            HttpServletRequest httpRequest
    ) {
        StudentNumberImportReport report = importService.apply(file, allowOverwrite);

        LogContext ctx = getLogContext();
        String ip = httpRequest.getRemoteAddr();

        for (StudentNumberImportRowResult row : report.rows()) {
            if (row.outcome().applicable()) {
                systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                        "Assigned student number " + row.studentNumber() + " to: " + row.studentName()
                                + " (import)",
                        ip);
            }
        }

        int skipped = report.totalRows() - report.applicableRows();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Imported student numbers from " + report.fileName() + ": "
                        + report.applicableRows() + " assigned, " + skipped + " skipped",
                ip);

        return ResponseEntity.ok(report);
    }

    private LogContext getLogContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_UNKNOWN");
        Integer userId = userRepository.findByUsername(username)
                .map(User::getUserId)
                .orElse(null);
        return new LogContext(userId, username, role);
    }

    private record LogContext(Integer userId, String username, String role) {}
}
