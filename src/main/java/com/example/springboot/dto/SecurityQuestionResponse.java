package com.example.springboot.dto;

import com.example.springboot.model.SecurityQuestion;

/** One of the 6 fixed default security questions, for a picker dropdown. */
public record SecurityQuestionResponse(Integer questionId, String questionText) {

    public static SecurityQuestionResponse from(SecurityQuestion question) {
        return new SecurityQuestionResponse(question.getQuestionId(), question.getQuestionText());
    }
}
