package com.example.springboot.dto.trainer;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * One graded student in a bulk save. A row supplies EITHER a {@code finalPercentage}
 * OR a {@code gradeStatus} (never both, never neither). {@code hoursRendered} is
 * mandatory. {@code reExamPercentage} is only accepted when the final is a failing
 * mark. The competency remark is derived server-side and is not part of this request.
 */
public record SaveGradeRequest(
    @NotBlank(message = "Student ID is required")
    String studentId,

    @DecimalMin(value = "0.0", message = "Final percentage must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Final percentage must be between 0 and 100")
    BigDecimal finalPercentage,

    @Pattern(regexp = "C|FA|INC|D", message = "Grade status must be one of C, FA, INC, D")
    String gradeStatus,

    @DecimalMin(value = "0.0", message = "Re-exam percentage must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Re-exam percentage must be between 0 and 100")
    BigDecimal reExamPercentage,

    @NotNull(message = "Hours rendered is required")
    @DecimalMin(value = "0.0", message = "Hours rendered must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Hours rendered must be between 0 and 100")
    BigDecimal hoursRendered
) {
}
