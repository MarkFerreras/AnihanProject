package com.example.springboot.dto.registrar;

public record TrainerSummaryResponse(
    Integer userId,
    String lastName,
    String firstName,
    String email,
    long classCount
) {
}
