package com.example.springboot.dto.registrar;

import java.util.List;

import com.example.springboot.model.Subject;

public record SubjectResponse(
    String subjectCode,
    String subjectName,
    String competencyType,
    Integer qualificationCode,
    String qualificationName,
    Integer units,
    List<String> trainers
) {
    /**
     * Maps a subject with no class-derived trainer list — used right after a
     * create/update, where the caller reloads the full table anyway.
     */
    public static SubjectResponse from(Subject s) {
        return from(s, List.of());
    }

    /**
     * {@code trainers} is the distinct set of trainer names teaching this
     * subject's classes. Trainer assignment is class-level only; a subject has
     * many classes and therefore may have many trainers.
     */
    public static SubjectResponse from(Subject s, List<String> trainers) {
        return new SubjectResponse(
                s.getSubjectCode(),
                s.getSubjectName(),
                s.getCompetencyType(),
                s.getQualification() != null ? s.getQualification().getQualificationCode() : null,
                s.getQualification() != null ? s.getQualification().getQualificationName() : null,
                s.getUnits(),
                trainers
        );
    }
}
