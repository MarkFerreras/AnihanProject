package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.NotBlank;

public record CreateClassRequest(
    @NotBlank String sectionCode,
    @NotBlank String subjectCode,
    Integer trainerId,
    @NotBlank String semester
) {
}
