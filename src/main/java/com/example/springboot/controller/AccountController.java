package com.example.springboot.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.UpdatePasswordRequest;
import com.example.springboot.dto.UpdatePersonalDetailsRequest;
import com.example.springboot.dto.UpdateProfileRequest;
import com.example.springboot.model.User;
import com.example.springboot.service.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * PUT /api/account/profile
     * Updates the username of the currently authenticated user.
     * Requires the current password for verification.
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        accountService.updateUsername(currentUsername, request.username(), request.currentPassword());

        return ResponseEntity.ok(Map.of(
                "message", "Username updated successfully",
                "username", request.username()
        ));
    }

    /**
     * PUT /api/account/password
     * Updates the password of the currently authenticated user.
     * Requires the current password for verification.
     * Invalidates the session after a successful password change to force re-login.
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            HttpServletRequest httpRequest) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        accountService.updatePassword(
                username,
                request.currentPassword(),
                request.newPassword(),
                request.confirmNewPassword()
        );

        // Invalidate session to force re-login with the new password
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "Password updated successfully. Please log in again."));
    }

    /**
     * PUT /api/account/details
     * Updates the personal details of the currently authenticated user.
     * Trainer-only fields (subjectCode, sectionCode) are ignored for non-Trainer roles.
     */
    @PutMapping("/details")
    public ResponseEntity<Map<String, Object>> updateDetails(
            @RequestBody UpdatePersonalDetailsRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User updated = accountService.updatePersonalDetails(username, request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Personal details updated successfully");
        response.put("lastName", updated.getLastName());
        response.put("firstName", updated.getFirstName());
        response.put("middleName", updated.getMiddleName());
        response.put("age", updated.getAge());
        response.put("birthdate", updated.getBirthdate());

        return ResponseEntity.ok(response);
    }
}
