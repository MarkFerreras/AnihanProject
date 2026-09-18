package com.example.springboot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = MIN_LENGTH, message = "Password must be at least 8 characters")
        @Pattern(regexp = PATTERN, message = PATTERN_MESSAGE)
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Size(min = MIN_LENGTH, message = "Password must be at least 8 characters")
        String confirmNewPassword
) {

    /**
     * The single definition of this project's strong, self-service password
     * policy — reused by {@link com.example.springboot.dto.ResetPasswordRequest}
     * so a security-questions password reset can never drift from a normal
     * self-service password change. (Admin-issued resets intentionally use a
     * weaker policy — see AdminUpdateUserRequest and memory-bank/decisions.md.)
     */
    public static final int MIN_LENGTH = 8;
    public static final String PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";
    public static final String PATTERN_MESSAGE =
            "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character";
}
