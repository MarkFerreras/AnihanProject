package com.example.springboot.dto.student;

public record EducationItemDto(
    String level,
    String schoolName,
    String schoolAddress,
    String gradeYear,
    String semester,
    String endedYear
) {}
