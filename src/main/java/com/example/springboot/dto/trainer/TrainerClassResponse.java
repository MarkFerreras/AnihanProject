package com.example.springboot.dto.trainer;

public record TrainerClassResponse(
        Integer classId,
        String sectionCode,
        String sectionName,
        String subjectCode,
        String subjectName,
        String courseName,
        String semester,
        long enrolledCount
) {}
