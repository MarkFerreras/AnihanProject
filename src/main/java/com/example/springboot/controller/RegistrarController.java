package com.example.springboot.controller;

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

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/registrar/student-records")
public class RegistrarController {

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
            @RequestParam(value = "status", required = false) String status
    ) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            throw new IllegalArgumentException("fromYear must not be greater than toYear");
        }
        return ResponseEntity.ok(registrarService.getAllRecords(query, fromYear, toYear, status));
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
