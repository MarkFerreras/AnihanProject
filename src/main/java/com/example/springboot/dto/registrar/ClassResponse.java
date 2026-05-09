package com.example.springboot.dto.registrar;

import com.example.springboot.model.SchoolClass;

public record ClassResponse(
    Integer classId,
    String sectionCode,
    String sectionName,
    String subjectCode,
    String subjectName,
    Integer trainerId,
    String trainerName,
    String semester,
    String createdAt,
    long enrolledCount
) {
    public static ClassResponse from(SchoolClass c, long enrolledCount) {
        String trainerName = null;
        Integer trainerId = null;
        if (c.getTrainer() != null) {
            trainerId = c.getTrainer().getUserId();
            trainerName = c.getTrainer().getLastName() + ", " + c.getTrainer().getFirstName();
        }
        return new ClassResponse(
                c.getClassId(),
                c.getSection().getSectionCode(),
                c.getSection().getSection(),
                c.getSubject().getSubjectCode(),
                c.getSubject().getSubjectName(),
                trainerId,
                trainerName,
                c.getSemester(),
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : null,
                enrolledCount
        );
    }
}
