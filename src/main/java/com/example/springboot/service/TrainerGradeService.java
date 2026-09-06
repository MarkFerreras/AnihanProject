package com.example.springboot.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.trainer.GradeSummaryResponse;
import com.example.springboot.dto.trainer.SaveGradeRequest;
import com.example.springboot.dto.trainer.StudentGradeRow;
import com.example.springboot.model.ClassEnrollment;
import com.example.springboot.model.Grade;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.User;
import com.example.springboot.repository.ClassEnrollmentRepository;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.SchoolClassRepository;
import com.example.springboot.repository.UserRepository;

@Service
public class TrainerGradeService {

    private static final BigDecimal MIN_PCT = BigDecimal.ZERO;
    private static final BigDecimal MAX_PCT = new BigDecimal("100");
    private static final BigDecimal MAX_HOURS = new BigDecimal("100");

    private final GradeRepository gradeRepository;
    private final SchoolClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public TrainerGradeService(
            GradeRepository gradeRepository,
            SchoolClassRepository classRepository,
            ClassEnrollmentRepository enrollmentRepository,
            UserRepository userRepository) {
        this.gradeRepository = gradeRepository;
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------
    // Read
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public GradeSummaryResponse getGradesForClass(Integer classId) {
        SchoolClass schoolClass = requireOwnedClass(classId);

        List<Grade> existingGrades = gradeRepository.findBySchoolClassClassId(classId);
        List<ClassEnrollment> enrollments = enrollmentRepository.findBySchoolClassClassId(classId);

        String subjectName = schoolClass.getSubject() != null ? schoolClass.getSubject().getSubjectName() : "Unknown";
        String sectionName = schoolClass.getSection() != null ? schoolClass.getSection().getSection() : "Unknown";
        boolean classLocked = !existingGrades.isEmpty() && existingGrades.get(0).isLocked();

        List<StudentGradeRow> students = enrollments.stream()
                .map(enrollment -> {
                    var student = enrollment.getStudent();
                    Optional<Grade> gradeOpt = existingGrades.stream()
                            .filter(g -> g.getStudent().getStudentId().equals(student.getStudentId()))
                            .findFirst();
                    if (gradeOpt.isPresent()) {
                        Grade g = gradeOpt.get();
                        return new StudentGradeRow(
                                student.getStudentId(),
                                student.getLastName(),
                                student.getFirstName(),
                                student.getMiddleName(),
                                g.getFinalPercentage(),
                                g.getFinalGrade(),
                                g.getGradeStatus(),
                                g.getReExamPercentage(),
                                g.getReExamGrade(),
                                g.getRemarks(),
                                g.getHoursRendered(),
                                g.isLocked());
                    }
                    return new StudentGradeRow(
                            student.getStudentId(),
                            student.getLastName(),
                            student.getFirstName(),
                            student.getMiddleName(),
                            null, null, null, null, null, null, null, false);
                })
                .sorted((a, b) -> {
                    int cmp = nullSafeCompare(a.lastName(), b.lastName());
                    if (cmp != 0) return cmp;
                    return nullSafeCompare(a.firstName(), b.firstName());
                })
                .toList();

        return new GradeSummaryResponse(
                classId, subjectName, sectionName, schoolClass.getSemester(), classLocked, students);
    }

    // -------------------------------------------------------
    // Write
    // -------------------------------------------------------

    @Transactional
    public void saveGrades(Integer classId, List<SaveGradeRequest> gradeUpdates) {
        SchoolClass schoolClass = requireOwnedClass(classId);

        for (SaveGradeRequest request : gradeUpdates) {
            if (!enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, request.studentId())) {
                throw new IllegalArgumentException("Student not enrolled in this class: " + request.studentId());
            }

            Grade grade = gradeRepository
                    .findBySchoolClassClassIdAndStudentStudentId(classId, request.studentId())
                    .orElseGet(() -> {
                        ClassEnrollment enrollment = enrollmentRepository.findBySchoolClassClassId(classId).stream()
                                .filter(e -> e.getStudent().getStudentId().equals(request.studentId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Enrollment not found for student: " + request.studentId()));
                        Grade g = new Grade();
                        g.setStudent(enrollment.getStudent());
                        g.setSubject(schoolClass.getSubject());
                        g.setSchoolClass(schoolClass);
                        return g;
                    });

            if (grade.isLocked()) {
                throw new IllegalArgumentException("Grade is locked and cannot be modified: " + request.studentId());
            }

            applyRequest(grade, request);
            gradeRepository.save(grade);
        }
    }

    /**
     * Validates one request and maps it onto the grade row: transmutes the
     * percentage(s) to equivalents, or records a status code, and derives the
     * competency remark.
     */
    private void applyRequest(Grade grade, SaveGradeRequest request) {
        boolean hasPercentage = request.finalPercentage() != null;
        boolean hasStatus = request.gradeStatus() != null && !request.gradeStatus().isBlank();

        if (hasPercentage == hasStatus) {
            throw new IllegalArgumentException(
                    "Each graded student needs exactly one of a final percentage or a status code: "
                            + request.studentId());
        }
        if (request.hoursRendered() == null) {
            throw new IllegalArgumentException("Hours rendered is required: " + request.studentId());
        }
        checkRange(request.hoursRendered(), MIN_PCT, MAX_HOURS, "Hours rendered", request.studentId());

        grade.setHoursRendered(request.hoursRendered());

        if (hasStatus) {
            String status = request.gradeStatus().trim().toUpperCase();
            if (request.reExamPercentage() != null) {
                throw new IllegalArgumentException(
                        "A re-exam grade cannot be recorded alongside a status code: " + request.studentId());
            }
            grade.setGradeStatus(status);
            grade.setFinalPercentage(null);
            grade.setFinalGrade(null);
            grade.setReExamPercentage(null);
            grade.setReExamGrade(null);
            grade.setRemarks(remarkForStatus(status));
            return;
        }

        // Percentage path
        checkRange(request.finalPercentage(), MIN_PCT, MAX_PCT, "Final percentage", request.studentId());
        BigDecimal finalEquivalent = GradeEquivalent.toEquivalent(request.finalPercentage());
        grade.setGradeStatus(null);
        grade.setFinalPercentage(request.finalPercentage());
        grade.setFinalGrade(finalEquivalent);

        if (request.reExamPercentage() != null) {
            if (!GradeEquivalent.isFailing(finalEquivalent)) {
                throw new IllegalArgumentException(
                        "A re-exam grade is only allowed when the final grade is a failing mark: "
                                + request.studentId());
            }
            checkRange(request.reExamPercentage(), MIN_PCT, MAX_PCT, "Re-exam percentage", request.studentId());
            grade.setReExamPercentage(request.reExamPercentage());
            grade.setReExamGrade(GradeEquivalent.toEquivalent(request.reExamPercentage()));
        } else {
            grade.setReExamPercentage(null);
            grade.setReExamGrade(null);
        }

        grade.setRemarks(GradeEquivalent.remarkFor(GradeEquivalent.effective(grade)));
    }

    /** C / D / INC → Complete = Competent, Failure Due to Absences = Not Competent, the rest carry no remark. */
    private static String remarkForStatus(String status) {
        return switch (status) {
            case "C" -> GradeEquivalent.COMPETENT;
            case "FA" -> GradeEquivalent.NOT_COMPETENT;
            default -> null; // INC, D
        };
    }

    private static void checkRange(BigDecimal value, BigDecimal min, BigDecimal max, String name, String studentId) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    name + " must be between " + min.toPlainString() + " and " + max.toPlainString()
                            + " (" + studentId + "), got: " + value.toPlainString());
        }
    }

    @Transactional
    public void lockGrades(Integer classId) {
        requireOwnedClass(classId);
        LocalDateTime now = LocalDateTime.now();
        for (Grade grade : gradeRepository.findBySchoolClassClassId(classId)) {
            grade.setLocked(true);
            grade.setLockedAt(now);
            gradeRepository.save(grade);
        }
    }

    @Transactional
    public void unlockGrades(Integer classId) {
        requireOwnedClass(classId);
        for (Grade grade : gradeRepository.findBySchoolClassClassId(classId)) {
            grade.setLocked(false);
            grade.setLockedAt(null);
            gradeRepository.save(grade);
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private SchoolClass requireOwnedClass(Integer classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));
        Integer trainerId = resolveCurrentTrainerId();
        if (schoolClass.getTrainer() == null || !schoolClass.getTrainer().getUserId().equals(trainerId)) {
            throw new IllegalArgumentException("You are not assigned to this class");
        }
        return schoolClass;
    }

    private int nullSafeCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareToIgnoreCase(b);
    }

    private Integer resolveCurrentTrainerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("Not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user has no matching User row: " + username));
        return user.getUserId();
    }
}
