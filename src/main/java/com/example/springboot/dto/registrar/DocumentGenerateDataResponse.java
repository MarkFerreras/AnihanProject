package com.example.springboot.dto.registrar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated auto-fill payload for the document generation page (TOR / Form IX).
 * Everything the database knows about a student in one call; the registrar
 * reviews and completes the rest in the browser before generating.
 */
public record DocumentGenerateDataResponse(
        StudentPart student,
        List<ParentPart> parents,
        List<EducationPart> education,
        List<TesdaPart> tesdaQualifications,
        OjtPart ojt,
        List<GradePart> grades
) {

    public record StudentPart(
            String studentId,
            String lastName,
            String firstName,
            String middleName,
            LocalDate birthdate,
            Integer age,
            String sex,
            String permanentAddress,
            String contactNo,
            String email,
            LocalDate enrollmentDate,
            String studentStatus,
            String batchCode,
            Short batchYear,
            String courseCode,
            String courseName,
            String sectionCode,
            String sectionName
    ) {
    }

    public record ParentPart(String relation, String fullName, String address) {
    }

    public record EducationPart(String level, String schoolName, String schoolAddress, String endedYear) {
    }

    public record TesdaPart(Integer slot, String title, String centerAddress,
                            LocalDate assessmentDate, String result) {
    }

    public record OjtPart(String companyName, String companyAddress, BigDecimal hoursRendered) {
    }

    /**
     * One graded subject for the document tables. {@code finalGrade} is the
     * printed FINAL cell — the 1.00–5.00 equivalent as a string, or a status
     * code (C / FA / INC / D). {@code reExamGrade} is the equivalent string or
     * null. {@code remarks} is the human label (Competent / Not Competent) or
     * null. Attendance hours are internal and are not carried here — the
     * document's HOURS column comes from the fixed curriculum.
     */
    public record GradePart(String subjectCode, String finalGrade, String reExamGrade, String remarks) {
    }
}