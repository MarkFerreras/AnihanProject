package com.example.springboot.dto.trainer;

import java.util.List;

public record TrainerSubjectResponse(
        String subjectCode,
        String subjectName,
        String qualificationName,
        Integer units,
        long enrolledCount,
        List<String> sectionNames,
        List<String> courseNames
) {}
