package com.example.springboot.dto.student;

import java.time.LocalDate;

public record GuardianDto(
    String relation,
    String lastName,
    String firstName,
    String middleName,
    LocalDate birthdate,
    String address
) {}
