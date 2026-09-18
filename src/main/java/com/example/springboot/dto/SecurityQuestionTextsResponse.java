package com.example.springboot.dto;

import java.util.List;

/**
 * Just the 2 question texts, in slot order — never the answers. Used both
 * for the "Edit Security Questions" modal (showing what's currently set)
 * and for the forgot-password page (showing what to answer).
 */
public record SecurityQuestionTextsResponse(List<String> questionTexts) {
}
