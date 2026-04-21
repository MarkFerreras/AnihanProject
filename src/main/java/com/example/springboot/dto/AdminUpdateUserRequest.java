package com.example.springboot.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
        @Pattern(regexp = "^\\S+$", message = "Username must not contain spaces")
        @Size(min = 1, message = "Username must not be empty")
        String username,

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

        @NotNull(message = "Birthdate is required")
        @PastOrPresent(message = "Birthdate cannot be in the future")
        LocalDate birthdate,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
