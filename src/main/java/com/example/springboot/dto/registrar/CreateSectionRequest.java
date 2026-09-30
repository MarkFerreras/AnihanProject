package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create Section payload. {@code batchCode} and {@code course} are free text: an unknown batch
 * code or course name is auto-created by ClassManagementService.createSection. {@code course}
 * may be an existing course code or name; sizes mirror the schema columns.
 */
public record CreateSectionRequest(
    @NotBlank @Size(max = 20, message = "Section code must not exceed 20 characters.") String sectionCode,
    @NotBlank @Size(max = 25, message = "Section name must not exceed 25 characters.") String sectionName,
    @NotBlank @Size(max = 20, message = "Batch code must not exceed 20 characters.") String batchCode,
    @NotBlank @Size(max = 100, message = "Course name must not exceed 100 characters.") String course
) {
}
