package com.example.springboot.dto.registrar;

import com.example.springboot.model.Subject;

public record SubjectResponse(
    String subjectCode,
    String subjectName,
    String competencyType,
    Integer qualificationCode,
    String qualificationName,
    Integer units
) {
    public static SubjectResponse from(Subject s) {
        return new SubjectResponse(
                s.getSubjectCode(),
                s.getSubjectName(),
                s.getCompetencyType(),
                s.getQualification() != null ? s.getQualification().getQualificationCode() : null,
                s.getQualification() != null ? s.getQualification().getQualificationName() : null,
                s.getUnits()
        );
    }
}
