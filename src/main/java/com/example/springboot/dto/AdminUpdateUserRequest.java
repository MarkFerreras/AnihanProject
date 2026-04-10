package com.example.springboot.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

public record AdminUpdateUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Role is required")
        @Pattern(
                regexp = "ROLE_ADMIN|ROLE_REGISTRAR|ROLE_TRAINER",
                message = "Role must be ROLE_ADMIN, ROLE_REGISTRAR, or ROLE_TRAINER"
        )
        String role,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Middle name is required")
        String middleName,

        @NotNull(message = "Age is required")
        @Min(value = 1, message = "Age must be at least 1")
        @Max(value = 150, message = "Age must be at most 150")
        Integer age,

        @NotNull(message = "Birthdate is required")
        @PastOrPresent(message = "Birthdate cannot be in the future")
        LocalDate birthdate
) {
}
