package com.example.springboot.dto.registrar;

import com.example.springboot.model.Subject;

public record SubjectResponse(
    String subjectCode,
    String subjectName,
    String qualificationName,
    Integer units,
    Integer trainerId,
    String trainerName
) {
    public static SubjectResponse from(Subject s) {
        String trainerName = null;
        Integer trainerId = null;
        if (s.getTrainer() != null) {
            trainerId = s.getTrainer().getUserId();
            trainerName = s.getTrainer().getLastName() + ", " + s.getTrainer().getFirstName();
        }
        return new SubjectResponse(
                s.getSubjectCode(),
                s.getSubjectName(),
                s.getQualification() != null ? s.getQualification().getQualificationName() : null,
                s.getUnits(),
                trainerId,
                trainerName
        );
    }
}
