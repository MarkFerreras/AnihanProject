package com.example.springboot.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.registrar.AssignBatchRequest;
import com.example.springboot.dto.registrar.AssignStudentNumberRequest;
import com.example.springboot.dto.registrar.StudentNumberAvailability;
import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.dto.registrar.UpdateStudentStatusRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.StudentStatusTransitions;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/registrar/student-records")
public class RegistrarController {

    /** system_logs.action is VARCHAR(500). */
    private static final int MAX_ACTION_LENGTH = 500;

    private final RegistrarService registrarService;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    public RegistrarController(RegistrarService registrarService,
                               SystemLogService systemLogService,
                               UserRepository userRepository) {
        this.registrarService = registrarService;
        this.systemLogService = systemLogService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<StudentRecordSummaryResponse>> list(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "fromYear", required = false) Integer fromYear,
            @RequestParam(value = "toYear", required = false) Integer toYear,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "hasStudentNumber", required = false) Boolean hasStudentNumber
    ) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            throw new IllegalArgumentException("fromYear must not be greater than toYear");
        }
        return ResponseEntity.ok(
                registrarService.getAllRecords(query, fromYear, toYear, status, hasStudentNumber));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<StudentRecordDetailsResponse> detail(@PathVariable Integer recordId) {
        return ResponseEntity.ok(registrarService.getRecordById(recordId));
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<StudentRecordDetailsResponse> update(
            @PathVariable Integer recordId,
            @Valid @RequestBody StudentRecordUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        StudentRecordDetailsResponse updated = registrarService.updateRecord(recordId, request);

        LogContext ctx = getLogContext();
        String ipAddress = httpRequest.getRemoteAddr();
        String studentName = updated.lastName() + ", " + updated.firstName();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Updated student record: " + studentName + " (ID: " + updated.studentId() + ")",
                ipAddress);

        return ResponseEntity.ok(updated);
    }

    /**
     * Assigns, changes, or clears a student's student number. A blank body value clears it —
     * a student with no number is a valid state until the registrar or the archive import
     * supplies one.
     */
    @PutMapping("/{recordId}/student-number")
    public ResponseEntity<StudentRecordDetailsResponse> assignStudentNumber(
            @PathVariable Integer recordId,
            @Valid @RequestBody AssignStudentNumberRequest request,
            HttpServletRequest httpRequest
    ) {
        StudentRecordDetailsResponse updated =
                registrarService.assignStudentNumber(recordId, request.studentNumber());

        LogContext ctx = getLogContext();
        String studentName = updated.lastName() + ", " + updated.firstName();
        String action = updated.studentNumber() == null
                ? "Cleared student number for: " + studentName
                : "Assigned student number " + updated.studentNumber() + " to: " + studentName;
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                action, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(updated);
    }

    /**
     * Read-only pre-check behind the Assign Student Number modal's inline warning. The PUT
     * above stays the authority; this only surfaces a clash before Save. Not audit-logged.
     */
    @GetMapping("/{recordId}/student-number/availability")
    public ResponseEntity<StudentNumberAvailability> checkStudentNumberAvailability(
            @PathVariable Integer recordId,
            @RequestParam(name = "number", required = false) String number) {
        return ResponseEntity.ok(registrarService.checkStudentNumberAvailability(recordId, number));
    }

    /**
     * Assigns, changes, or clears a student's batch. A blank body value clears it — but only
     * when the student isn't enrolled in a section (see RegistrarService.assignBatch's section
     * invariant). Entering a batch code that doesn't exist yet auto-creates it with the current
     * calendar year.
     */
    @PutMapping("/{recordId}/batch")
    public ResponseEntity<StudentRecordDetailsResponse> assignBatch(
            @PathVariable Integer recordId,
            @Valid @RequestBody AssignBatchRequest request,
            HttpServletRequest httpRequest
    ) {
        StudentRecordDetailsResponse updated =
                registrarService.assignBatch(recordId, request.batchCode());

        LogContext ctx = getLogContext();
        String studentName = updated.lastName() + ", " + updated.firstName();
        String action = updated.batchCode() == null
                ? "Cleared batch for: " + studentName
                : "Assigned batch " + updated.batchCode() + " to: " + studentName;
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                action, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(updated);
    }

    /**
     * Changes a student's enrollment status. Pulled out of the general update endpoint so a
     * status change is always a deliberate, separately audited action — mirrors the treatment
     * given to the student number. The audit line also records a set or cleared completion
     * date and, when the move required one, the reason (spec 2026-10-01 SO checklist 10).
     */
    @PutMapping("/{recordId}/status")
    public ResponseEntity<StudentRecordDetailsResponse> updateStatus(
            @PathVariable Integer recordId,
            @Valid @RequestBody UpdateStudentStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        StudentRecordDetailsResponse before = registrarService.getRecordById(recordId);

        StudentRecordDetailsResponse updated = registrarService.updateStatus(
                recordId, request.studentStatus(), request.completionDate(), request.reason());

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                statusChangeAction(before, updated, request.reason()),
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(updated);
    }

    /**
     * e.g. "Changed status of Dela Cruz, Ana from Active to Graduated (completion date
     * 2014-03-15; reason: Digitized archive record)". The reason is logged only when the move
     * required one (the service has already rejected a blank one).
     */
    private String statusChangeAction(StudentRecordDetailsResponse before,
                                      StudentRecordDetailsResponse after, String reason) {
        List<String> notes = new ArrayList<>();
        if (after.completionDate() != null && !after.completionDate().equals(before.completionDate())) {
            notes.add("completion date " + after.completionDate());
        } else if (after.completionDate() == null && before.completionDate() != null) {
            notes.add("cleared completion date " + before.completionDate());
        }
        if (StudentStatusTransitions.check(before.studentStatus(), after.studentStatus()).requiresReason()) {
            notes.add("reason: " + (reason == null ? "" : reason.trim()));
        }
        String action = "Changed status of " + after.lastName() + ", " + after.firstName()
                + " from " + before.studentStatus() + " to " + after.studentStatus();
        if (!notes.isEmpty()) {
            action += " (" + String.join("; ", notes) + ")";
        }
        return action.length() <= MAX_ACTION_LENGTH ? action : action.substring(0, MAX_ACTION_LENGTH - 3) + "...";
    }

    @DeleteMapping("/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer recordId, HttpServletRequest httpRequest) {
        StudentRecordDetailsResponse details = registrarService.getRecordById(recordId);
        String studentName = details.lastName() + ", " + details.firstName();
        String studentId = details.studentId();

        registrarService.deleteRecord(recordId);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Deleted student record: " + studentName + " (ID: " + studentId + ")",
                httpRequest.getRemoteAddr());
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
