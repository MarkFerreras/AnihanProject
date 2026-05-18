package com.example.springboot.dto.trainer;

public record TrainerSubjectStudentResponse(
        String studentId,
        String lastName,
        String firstName,
        String middleName,
        String sectionCode,
        String sectionName
) {}
