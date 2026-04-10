package com.example.springboot.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmNewPassword
) {
}
