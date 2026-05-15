package com.example.springboot.dto.registrar;

import com.example.springboot.model.StudentRecord;

public record EligibleSectionStudentResponse(
        String studentId,
        String lastName,
        String firstName,
        String batchCode,
        Short batchYear,
        String courseCode,
        String courseName
) {
    public static EligibleSectionStudentResponse from(StudentRecord s) {
        return new EligibleSectionStudentResponse(
                s.getStudentId(),
                s.getLastName(),
                s.getFirstName(),
                s.getBatch() != null ? s.getBatch().getBatchCode() : null,
                s.getBatch() != null ? s.getBatch().getBatchYear() : null,
                s.getCourse() != null ? s.getCourse().getCourseCode() : null,
                s.getCourse() != null ? s.getCourse().getCourseName() : null
        );
    }
}
