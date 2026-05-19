package com.example.springboot.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private BigDecimal computeFinalGrade(BigDecimal midterm, BigDecimal finals) {
        if (midterm == null || finals == null) {
            return null;
        }
        return midterm.multiply(new BigDecimal("0.4"))
                .add(finals.multiply(new BigDecimal("0.6")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getEffectiveGrade(BigDecimal finalGrade, BigDecimal reExamGrade) {
        if (finalGrade == null) {
            return null;
        }
        if (reExamGrade != null && finalGrade.compareTo(new BigDecimal("3.0")) > 0) {
            return reExamGrade;
        }
        return finalGrade;
    }

    private BigDecimal computeGwa(List<Grade> gradesForStudent) {
        BigDecimal totalWeighted = BigDecimal.ZERO;
        BigDecimal totalUnits = BigDecimal.ZERO;

        for (Grade grade : gradesForStudent) {
            if (grade.getSchoolClass() == null || grade.getSchoolClass().getSubject() == null) {
                continue;
            }
            BigDecimal effective = getEffectiveGrade(grade.getFinalGrade(), grade.getReExamGrade());
            if (effective == null) {
                continue;
            }
            Integer units = grade.getSchoolClass().getSubject().getUnits();
            if (units == null) {
                continue;
            }
            totalWeighted = totalWeighted.add(effective.multiply(new BigDecimal(units)));
            totalUnits = totalUnits.add(new BigDecimal(units));
        }

        if (totalUnits.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return totalWeighted.divide(totalUnits, 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public GradeSummaryResponse getGradesForClass(Integer classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        Integer trainerId = resolveCurrentTrainerId();
        if (schoolClass.getTrainer() == null || !schoolClass.getTrainer().getUserId().equals(trainerId)) {
            throw new IllegalArgumentException("You are not assigned to this class");
        }

        // Get existing grades for this class
        List<Grade> existingGrades = gradeRepository.findBySchoolClassClassId(classId);

        // Get all enrolled students for this class
        List<ClassEnrollment> enrollments = enrollmentRepository.findBySchoolClassClassId(classId);

        String subjectName = schoolClass.getSubject() != null ? schoolClass.getSubject().getSubjectName() : "Unknown";
        String sectionName = schoolClass.getSection() != null ? schoolClass.getSection().getSection() : "Unknown";

        boolean classLocked = !existingGrades.isEmpty() && existingGrades.get(0).isLocked();

        // Build student rows from enrollments, attaching existing grade data if available
        List<StudentGradeRow> students = enrollments.stream()
                .map(enrollment -> {
                    var student = enrollment.getStudent();
                    // Look for existing grade for this student in this class
                    Optional<Grade> gradeOpt = existingGrades.stream()
                            .filter(g -> g.getStudent().getStudentId().equals(student.getStudentId()))
                            .findFirst();

                    if (gradeOpt.isPresent()) {
                        return gradeToStudentRow(gradeOpt.get());
                    } else {
                        // No grade row yet — return an empty row for this enrolled student
                        return new StudentGradeRow(
                                student.getStudentId(),
                                student.getLastName(),
                                student.getFirstName(),
                                student.getMiddleName(),
                                null, null, null, null, null, null, null, false);
                    }
                })
                .sorted((a, b) -> {
                    int cmp = nullSafeCompare(a.lastName(), b.lastName());
                    if (cmp != 0) return cmp;
                    return nullSafeCompare(a.firstName(), b.firstName());
                })
                .toList();

        return new GradeSummaryResponse(
                classId,
                subjectName,
                sectionName,
                schoolClass.getSemester(),
                classLocked,
                students);
    }

    private int nullSafeCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareToIgnoreCase(b);
    }

    private StudentGradeRow gradeToStudentRow(Grade grade) {
        String studentId = grade.getStudent().getStudentId();
        List<Grade> allStudentGrades = gradeRepository.findByStudentStudentId(studentId);
        BigDecimal gwa = computeGwa(allStudentGrades);

        return new StudentGradeRow(
                studentId,
                grade.getStudent().getLastName(),
                grade.getStudent().getFirstName(),
                grade.getStudent().getMiddleName(),
                grade.getMidtermGrade(),
                grade.getFinalsGrade(),
                grade.getFinalGrade(),
                grade.getReExamGrade(),
                grade.getHoursStudied(),
                grade.getRemarks(),
                gwa,
                grade.isLocked());
    }

    @Transactional
    public void saveGrades(Integer classId, List<SaveGradeRequest> gradeUpdates) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        Integer trainerId = resolveCurrentTrainerId();
        if (schoolClass.getTrainer() == null || !schoolClass.getTrainer().getUserId().equals(trainerId)) {
            throw new IllegalArgumentException("You are not assigned to this class");
        }

        for (SaveGradeRequest request : gradeUpdates) {
            // Verify student is enrolled in this class
            if (!enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, request.studentId())) {
                throw new IllegalArgumentException("Student not enrolled in this class: " + request.studentId());
            }

            // Find existing grade or create a new one (upsert)
            Grade grade = gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, request.studentId())
                    .orElseGet(() -> {
                        // Auto-create the grade row for this enrolled student
                        ClassEnrollment enrollment = enrollmentRepository.findBySchoolClassClassId(classId).stream()
                                .filter(e -> e.getStudent().getStudentId().equals(request.studentId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Enrollment not found for student: " + request.studentId()));
                        Grade newGrade = new Grade();
                        newGrade.setStudent(enrollment.getStudent());
                        newGrade.setSubject(schoolClass.getSubject());
                        newGrade.setSchoolClass(schoolClass);
                        return newGrade;
                    });

            if (grade.isLocked()) {
                throw new IllegalArgumentException("Grade is locked and cannot be modified: " + request.studentId());
            }

            grade.setMidtermGrade(request.midtermGrade());
            grade.setFinalsGrade(request.finalsGrade());
            grade.setReExamGrade(request.reExamGrade());
            grade.setHoursStudied(request.hoursStudied());
            grade.setRemarks(request.remarks());

            if (request.midtermGrade() != null && request.finalsGrade() != null) {
                BigDecimal finalGrade = computeFinalGrade(request.midtermGrade(), request.finalsGrade());
                grade.setFinalGrade(finalGrade);
            }

            gradeRepository.save(grade);
        }
    }

    @Transactional
    public void lockGrades(Integer classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        Integer trainerId = resolveCurrentTrainerId();
        if (schoolClass.getTrainer() == null || !schoolClass.getTrainer().getUserId().equals(trainerId)) {
            throw new IllegalArgumentException("You are not assigned to this class");
        }

        List<Grade> grades = gradeRepository.findBySchoolClassClassId(classId);
        LocalDateTime now = LocalDateTime.now();

        for (Grade grade : grades) {
            grade.setLocked(true);
            grade.setLockedAt(now);
            gradeRepository.save(grade);
        }
    }

    @Transactional
    public void unlockGrades(Integer classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        Integer trainerId = resolveCurrentTrainerId();
        if (schoolClass.getTrainer() == null || !schoolClass.getTrainer().getUserId().equals(trainerId)) {
            throw new IllegalArgumentException("You are not assigned to this class");
        }

        List<Grade> grades = gradeRepository.findBySchoolClassClassId(classId);

        for (Grade grade : grades) {
            grade.setLocked(false);
            grade.setLockedAt(null);
            gradeRepository.save(grade);
        }
    }

    /**
     * Resolves the current authenticated trainer's user ID from the database.
     * Uses the same proven pattern as TrainerService.resolveCurrentTrainerId().
     */
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
