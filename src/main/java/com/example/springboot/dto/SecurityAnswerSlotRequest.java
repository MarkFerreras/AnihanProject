package com.example.springboot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One of a user's 2 security-question slots being set up or edited.
 * Exactly one of {@link #questionId} / {@link #customQuestion} must be
 * supplied — validated in {@code SecurityQuestionService}, since a
 * cross-field "exactly one of" rule doesn't express cleanly as a single
 * Bean Validation annotation here.
 */
public record SecurityAnswerSlotRequest(
        Integer questionId,

        @Size(max = 255, message = "Custom question is too long")
        String customQuestion,

        @NotBlank(message = "Answer is required")
        @Size(min = MIN_ANSWER_LENGTH, max = MAX_ANSWER_LENGTH,
                message = "Answer must be between 3 and 100 characters")
        String answer
) {

    /**
     * The single definition of answer length limits — punctuation is
     * intentionally unrestricted (no @Pattern), and comparison is always
     * case-insensitive (the answer is lowercased + trimmed before hashing).
     */
    public static final int MIN_ANSWER_LENGTH = 3;
    public static final int MAX_ANSWER_LENGTH = 100;
}
