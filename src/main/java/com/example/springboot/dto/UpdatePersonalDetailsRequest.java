package com.example.springboot.dto;

import java.time.LocalDate;

public record UpdatePersonalDetailsRequest(
        String lastName,
        String firstName,
        String middleName,
        LocalDate birthdate
) {
}

