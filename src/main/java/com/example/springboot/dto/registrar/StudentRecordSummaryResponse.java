package com.example.springboot.dto.registrar;

import com.example.springboot.model.StudentRecord;

public record StudentRecordSummaryResponse(
        Integer recordId,
        String studentId,
        String lastName,
        String firstName,
        String batchCode,
        String courseCode,
        String sectionCode,
        String studentStatus
) {

    public static StudentRecordSummaryResponse from(StudentRecord r) {
        return new StudentRecordSummaryResponse(
                r.getRecordId(),
                r.getStudentId(),
                r.getLastName(),
                r.getFirstName(),
                r.getBatch() != null ? r.getBatch().getBatchCode() : null,
                r.getCourse() != null ? r.getCourse().getCourseCode() : null,
                r.getSection() != null ? r.getSection().getSectionCode() : null,
                r.getStudentStatus()
        );
    }
}
