package com.example.springboot.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
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
import com.example.springboot.service.AdminService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
            Principal principal
    ) {
        return ResponseEntity.ok(adminService.updateUser(id, request, principal.getName()));
    }

    /**
     * Soft delete — deactivates the user account (sets enabled = false).
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> softDeleteUser(
            @PathVariable Integer id,
            Principal principal
    ) {
        adminService.softDeleteUser(id, principal.getName());
        return ResponseEntity.ok(Map.of("message", "User account has been deactivated."));
    }

    /**
     * Hard delete — permanently removes the user record from the database.
     */
    @DeleteMapping("/users/{id}/permanent")
    public ResponseEntity<Map<String, String>> hardDeleteUser(
            @PathVariable Integer id,
            Principal principal
    ) {
        adminService.hardDeleteUser(id, principal.getName());
        return ResponseEntity.ok(Map.of("message", "User account has been permanently deleted."));
    }

    /**
     * Re-enable a previously deactivated (soft-deleted) user account.
     */
    @PutMapping("/users/{id}/enable")
    public ResponseEntity<Map<String, String>> reEnableUser(@PathVariable Integer id) {
        adminService.reEnableUser(id);
        return ResponseEntity.ok(Map.of("message", "User account has been re-enabled."));
    }
}
