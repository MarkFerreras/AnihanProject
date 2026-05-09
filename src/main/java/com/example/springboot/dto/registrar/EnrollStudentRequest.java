package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnrollStudentRequest(
    @NotNull Integer classId,
    @NotBlank String studentId
) {
}
