package com.example.springboot.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^\\S+$", message = "Username must not contain spaces")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Role is required")
        @Pattern(
                regexp = "ROLE_ADMIN|ROLE_REGISTRAR|ROLE_TRAINER",
                message = "Role must be ROLE_ADMIN, ROLE_REGISTRAR, or ROLE_TRAINER"
        )
        String role,

        String lastName,

        String firstName,

        String middleName,

        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Birthdate is required")
        @PastOrPresent(message = "Birthdate cannot be in the future")
        LocalDate birthdate
) {
}

