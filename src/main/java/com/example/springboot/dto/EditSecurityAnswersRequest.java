package com.example.springboot.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for re-picking and re-answering both security-question slots from
 * the account settings modal. {@link #currentPassword} is required —
 * changing account recovery questions is a sensitive action gated the same
 * way a self-service password change is.
 */
public record EditSecurityAnswersRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotNull
        @Size(min = 2, max = 2, message = "Exactly 2 security questions are required")
        @Valid
        List<SecurityAnswerSlotRequest> slots
) {
}
