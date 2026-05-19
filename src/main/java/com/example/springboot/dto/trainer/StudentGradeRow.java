package com.example.springboot.dto.trainer;

import java.math.BigDecimal;

public record StudentGradeRow(
    String studentId,
    String lastName,
    String firstName,
    String middleName,
    BigDecimal midtermGrade,
    BigDecimal finalsGrade,
    BigDecimal finalGrade,
    BigDecimal reExamGrade,
    BigDecimal hoursStudied,
    String remarks,
    BigDecimal gwa,
    boolean locked
) {
}
