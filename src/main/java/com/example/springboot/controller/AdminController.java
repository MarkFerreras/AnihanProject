package com.example.springboot.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.AdminUpdateUserRequest;
import com.example.springboot.dto.AdminUserResponse;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService,
                           SystemLogService systemLogService,
                           UserRepository userRepository) {
        this.adminService = adminService;
        this.systemLogService = systemLogService;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody AdminUpdateUserRequest request,
            Principal principal,
            HttpServletRequest httpRequest
    ) {
        // Capture target username before update (it might change)
        AdminUserResponse beforeUpdate = adminService.getUserById(id);
        String targetUsername = beforeUpdate.username();

        AdminUserResponse updated = adminService.updateUser(id, request, principal.getName());

        // Log action
        String ipAddress = httpRequest.getRemoteAddr();
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Updated user details for: " + targetUsername, ipAddress);

        // Log password reset separately if password was provided
        if (request.password() != null && !request.password().isBlank()) {
            systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                    "Reset password for: " + targetUsername, ipAddress);
        }

        return ResponseEntity.ok(updated);
    }

    /**
     * Soft delete — deactivates the user account (sets enabled = false).
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> softDeleteUser(
            @PathVariable Integer id,
            Principal principal,
            HttpServletRequest httpRequest
    ) {
        // Capture target username before deactivation
        AdminUserResponse target = adminService.getUserById(id);

        adminService.softDeleteUser(id, principal.getName());

        String ipAddress = httpRequest.getRemoteAddr();
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Deactivated account: " + target.username(), ipAddress);

        return ResponseEntity.ok(Map.of("message", "User account has been deactivated."));
    }

    /**
     * Hard delete — permanently removes the user record from the database.
     */
    @DeleteMapping("/users/{id}/permanent")
    public ResponseEntity<Map<String, String>> hardDeleteUser(
            @PathVariable Integer id,
            Principal principal,
            HttpServletRequest httpRequest
    ) {
        // Capture target username BEFORE deletion
        AdminUserResponse target = adminService.getUserById(id);

        adminService.hardDeleteUser(id, principal.getName());

        String ipAddress = httpRequest.getRemoteAddr();
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Permanently deleted account: " + target.username(), ipAddress);

        return ResponseEntity.ok(Map.of("message", "User account has been permanently deleted."));
    }

    /**
     * Re-enable a previously deactivated (soft-deleted) user account.
     */
    @PutMapping("/users/{id}/enable")
    public ResponseEntity<Map<String, String>> reEnableUser(
            @PathVariable Integer id,
            HttpServletRequest httpRequest
    ) {
        // Capture target username before re-enabling
        AdminUserResponse target = adminService.getUserById(id);

        adminService.reEnableUser(id);

        String ipAddress = httpRequest.getRemoteAddr();
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Re-enabled account: " + target.username(), ipAddress);

        return ResponseEntity.ok(Map.of("message", "User account has been re-enabled."));
    }

    /**
     * Helper to extract the current admin's userId, username, and role from the security context.
     */
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
