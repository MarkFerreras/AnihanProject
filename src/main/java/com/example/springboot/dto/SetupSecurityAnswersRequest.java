package com.example.springboot.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for the mandatory first-time security-question setup. */
public record SetupSecurityAnswersRequest(
        @NotNull
        @Size(min = 2, max = 2, message = "Exactly 2 security questions are required")
        @Valid
        List<SecurityAnswerSlotRequest> slots
) {
}
