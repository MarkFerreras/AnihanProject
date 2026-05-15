package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSectionRequest(
        @NotBlank(message = "Section name is required")
        @Size(max = 25, message = "Section name must be at most 25 characters")
        String sectionName
) {
}
