package com.example.springboot.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /api/password-recovery/verify}. Answers are matched
 * positionally against the 2 questions returned by the preceding lookup
 * call (same order, by slot). Both must be correct (AND logic) — see
 * {@code SecurityQuestionService.verifyAnswers} for the lockout rules.
 */
public record VerifyAnswersRequest(
        @NotNull
        @Size(min = 2, max = 2, message = "Both answers are required")
        List<String> answers
) {
}
