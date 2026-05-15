package com.example.springboot.dto.registrar;

import com.example.springboot.model.StudentRecord;

public record SectionStudentResponse(
        String studentId,
        String lastName,
        String firstName,
        String middleName,
        String studentStatus
) {
    public static SectionStudentResponse from(StudentRecord s) {
        return new SectionStudentResponse(
                s.getStudentId(),
                s.getLastName(),
                s.getFirstName(),
                s.getMiddleName(),
                s.getStudentStatus()
        );
    }
}
