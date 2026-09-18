package com.example.springboot.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for {@code POST /api/password-recovery/lookup} — the forgot-password entry point. */
public record EmailLookupRequest(
        @NotBlank(message = "Email is required")
        String email
) {
}
