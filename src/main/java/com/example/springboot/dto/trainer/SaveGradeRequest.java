package com.example.springboot.dto.trainer;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveGradeRequest(
    @NotBlank(message = "Student ID is required")
    String studentId,

    @DecimalMin(value = "1.0", message = "Midterm grade must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Midterm grade must be between 1.0 and 5.0")
    BigDecimal midtermGrade,

    @DecimalMin(value = "1.0", message = "Finals grade must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Finals grade must be between 1.0 and 5.0")
    BigDecimal finalsGrade,

    @DecimalMin(value = "1.0", message = "Re-exam grade must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Re-exam grade must be between 1.0 and 5.0")
    BigDecimal reExamGrade,

    @DecimalMin(value = "0.0", message = "Hours studied must be between 0.0 and 9.99")
    @DecimalMax(value = "9.99", message = "Hours studied must be between 0.0 and 9.99")
    BigDecimal hoursStudied,

    @Size(max = 255, message = "Remarks must not exceed 255 characters")
    String remarks
) {
}
