package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.NotBlank;

public record CreateSectionRequest(
    @NotBlank String sectionCode,
    @NotBlank String sectionName,
    @NotBlank String batchCode,
    @NotBlank String courseCode
) {
}
