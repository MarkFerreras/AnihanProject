package com.example.springboot.dto.registrar;

import java.time.LocalDate;

import com.example.springboot.model.StudentRecord;

public record StudentRecordDetailsResponse(
        Integer recordId,
        String studentId,
        String lastName,
        String firstName,
        String middleName,
        LocalDate birthdate,
        Integer age,
        String sex,
        String civilStatus,
        String permanentAddress,
        String temporaryAddress,
        String email,
        String contactNo,
        String religion,
        Boolean baptized,
        LocalDate baptismDate,
        String baptismPlace,
        Integer siblingCount,
        Integer brotherCount,
        Integer sisterCount,
        String batchCode,
        String courseCode,
        String sectionCode,
        LocalDate enrollmentDate,
        String studentStatus
) {

    public static StudentRecordDetailsResponse from(StudentRecord r) {
        return new StudentRecordDetailsResponse(
                r.getRecordId(),
                r.getStudentId(),
                r.getLastName(),
                r.getFirstName(),
                r.getMiddleName(),
                r.getBirthdate(),
                r.getAge(),
                r.getSex(),
                r.getCivilStatus(),
                r.getPermanentAddress(),
                r.getTemporaryAddress(),
                r.getEmail(),
                r.getContactNo(),
                r.getReligion(),
                r.getBaptized(),
                r.getBaptismDate(),
                r.getBaptismPlace(),
                r.getSiblingCount(),
                r.getBrotherCount(),
                r.getSisterCount(),
                r.getBatch() != null ? r.getBatch().getBatchCode() : null,
                r.getCourse() != null ? r.getCourse().getCourseCode() : null,
                r.getSection() != null ? r.getSection().getSectionCode() : null,
                r.getEnrollmentDate(),
                r.getStudentStatus()
        );
    }
}
