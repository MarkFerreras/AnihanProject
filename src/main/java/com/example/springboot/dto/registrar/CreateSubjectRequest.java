package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
        @NotBlank(message = "Subject code is required")
        @Size(max = 20, message = "Subject code must be at most 20 characters")
        String subjectCode,

        @NotBlank(message = "Subject name is required")
        @Size(max = 255, message = "Subject name must be at most 255 characters")
        String subjectName,

        @NotNull(message = "Qualification is required")
        Integer qualificationCode,

        @NotNull(message = "Units is required")
        @Min(value = 1, message = "Units must be at least 1")
        Integer units) {
}
