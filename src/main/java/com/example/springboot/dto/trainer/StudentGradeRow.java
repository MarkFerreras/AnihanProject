package com.example.springboot.dto.trainer;

import java.math.BigDecimal;

/**
 * One enrolled student's grade row for the trainer's grade-input modal.
 * {@code finalGrade} / {@code reExamGrade} are the transmuted 1.00–5.00
 * equivalents; {@code gradeStatus} is set instead of {@code finalPercentage}
 * for C/FA/INC/D rows; {@code remarks} is the derived competency token
 * (COMPETENT / NOT_COMPETENT / null). The Total GWA is registrar-only and is
 * deliberately not included here.
 */
public record StudentGradeRow(
    String studentId,
    String lastName,
    String firstName,
    String middleName,
    BigDecimal finalPercentage,
    BigDecimal finalGrade,
    String gradeStatus,
    BigDecimal reExamPercentage,
    BigDecimal reExamGrade,
    String remarks,
    BigDecimal hoursRendered,
    boolean locked
) {
}
