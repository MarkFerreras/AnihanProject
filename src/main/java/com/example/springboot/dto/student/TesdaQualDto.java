package com.example.springboot.dto.student;

import java.time.LocalDate;

public record TesdaQualDto(
    Integer slot,
    String title,
    String centerAddress,
    LocalDate assessmentDate,
    String result
) {}
