package com.example.springboot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /api/password-recovery/reset} — the final step of
 * the forgot-password flow. Reuses the exact same strong password policy as
 * a normal self-service change ({@link UpdatePasswordRequest}), not a
 * separately-typed copy that could drift.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "New password is required")
        @Size(min = UpdatePasswordRequest.MIN_LENGTH, message = "Password must be at least 8 characters")
        @Pattern(regexp = UpdatePasswordRequest.PATTERN, message = UpdatePasswordRequest.PATTERN_MESSAGE)
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmNewPassword
) {
}
