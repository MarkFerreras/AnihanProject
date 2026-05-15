package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssignStudentsToSectionRequest(
        @NotEmpty(message = "studentIds must not be empty")
        List<@NotBlank String> studentIds
) {
}
