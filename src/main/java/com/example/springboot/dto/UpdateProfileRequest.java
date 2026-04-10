package com.example.springboot.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
