package com.example.springboot.dto.trainer;

public record TrainerClassStudentResponse(
        String studentId,
        String lastName,
        String firstName,
        String middleName
) {}
