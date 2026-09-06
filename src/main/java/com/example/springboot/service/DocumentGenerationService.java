package com.example.springboot.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.springboot.dto.registrar.DocumentGenerateDataResponse;
import com.example.springboot.dto.registrar.DocumentGenerateDataResponse.EducationPart;
import com.example.springboot.dto.registrar.DocumentGenerateDataResponse.GradePart;
import com.example.springboot.dto.registrar.DocumentGenerateDataResponse.OjtPart;
import com.example.springboot.dto.registrar.DocumentGenerateDataResponse.ParentPart;
import com.example.springboot.dto.registrar.DocumentGenerateDataResponse.StudentPart;
import com.example.springboot.dto.registrar.DocumentGenerateDataResponse.TesdaPart;
import com.example.springboot.model.Grade;
import com.example.springboot.model.Parent;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentOjtRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentTesdaQualificationRepository;

@Service
public class DocumentGenerationService {

    private final StudentRecordRepository studentRecordRepository;
    private final ParentRepository parentRepository;
    private final StudentEducationRepository educationRepository;
    private final StudentTesdaQualificationRepository tesdaRepository;
    private final StudentOjtRepository ojtRepository;
    private final GradeRepository gradeRepository;

    public DocumentGenerationService(StudentRecordRepository studentRecordRepository,
                                     ParentRepository parentRepository,
                                     StudentEducationRepository educationRepository,
                                     StudentTesdaQualificationRepository tesdaRepository,
                                     StudentOjtRepository ojtRepository,
                                     GradeRepository gradeRepository) {
        this.studentRecordRepository = studentRecordRepository;
        this.parentRepository = parentRepository;
        this.educationRepository = educationRepository;
        this.tesdaRepository = tesdaRepository;
        this.ojtRepository = ojtRepository;
        this.gradeRepository = gradeRepository;
    }

    public DocumentGenerateDataResponse getGenerateData(String studentId) {
        StudentRecord record = studentRecordRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("No student record found for ID: " + studentId));

        StudentPart student = new StudentPart(
                record.getStudentId(),
                record.getLastName(),
                record.getFirstName(),
                record.getMiddleName(),
                record.getBirthdate(),
                record.getAge(),
                record.getSex(),
                record.getPermanentAddress(),
                record.getContactNo(),
                record.getEmail(),
                record.getEnrollmentDate(),
                record.getStudentStatus(),
                record.getBatch() == null ? null : record.getBatch().getBatchCode(),
                record.getBatch() == null ? null : record.getBatch().getBatchYear(),
                record.getCourse() == null ? null : record.getCourse().getCourseCode(),
                record.getCourse() == null ? null : record.getCourse().getCourseName(),
                record.getSection() == null ? null : record.getSection().getSectionCode(),
                record.getSection() == null ? null : record.getSection().getSection());

        List<ParentPart> parents = parentRepository.findByStudentStudentId(studentId).stream()
                .map(p -> new ParentPart(p.getRelation(), parentFullName(p), p.getAddress()))
                .toList();

        List<EducationPart> education = educationRepository.findByStudentIdOrderByLevel(studentId).stream()
                .map(e -> new EducationPart(e.getLevel(), e.getSchoolName(), e.getSchoolAddress(), e.getEndedYear()))
                .toList();

        List<TesdaPart> tesda = tesdaRepository.findByStudentIdOrderBySlot(studentId).stream()
                .map(t -> new TesdaPart(t.getSlot(), t.getTitle(), t.getCenterAddress(),
                        t.getAssessmentDate(), t.getResult()))
                .toList();

        OjtPart ojt = ojtRepository.findByStudentId(studentId)
                .map(o -> new OjtPart(o.getCompanyName(), o.getCompanyAddress(), o.getHoursRendered()))
                .orElse(null);

        List<GradePart> grades = gradeRepository.findByStudentStudentId(studentId).stream()
                .map(g -> new GradePart(
                        g.getSubject().getSubjectCode(),
                        gradeCell(g),
                        g.getReExamGrade() != null ? g.getReExamGrade().toPlainString() : null,
                        remarkLabel(g.getRemarks())))
                .toList();

        return new DocumentGenerateDataResponse(student, parents, education, tesda, ojt, grades);
    }

    /** The printed FINAL cell: a status code when present, otherwise the equivalent as a string. */
    private static String gradeCell(Grade g) {
        if (g.getGradeStatus() != null && !g.getGradeStatus().isBlank()) {
            return g.getGradeStatus();
        }
        return g.getFinalGrade() != null ? g.getFinalGrade().toPlainString() : null;
    }

    /** Maps the stored remark token to the label printed in the Remarks column. */
    private static String remarkLabel(String token) {
        if (token == null) {
            return null;
        }
        return switch (token) {
            case "COMPETENT" -> "Competent";
            case "NOT_COMPETENT" -> "Not Competent";
            default -> null;
        };
    }

    private static String parentFullName(Parent p) {
        String name = Stream.of(p.getFirstName(), p.getMiddleName(), p.getFamilyName())
                .filter(StringUtils::hasText)
                .reduce((a, b) -> a + " " + b)
                .orElse("");
        return name.isBlank() ? null : name;
    }
}