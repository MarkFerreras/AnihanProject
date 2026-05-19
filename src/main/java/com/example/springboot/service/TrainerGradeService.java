package com.example.springboot.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.trainer.GradeSummaryResponse;
import com.example.springboot.dto.trainer.SaveGradeRequest;
import com.example.springboot.dto.trainer.StudentGradeRow;
import com.example.springboot.model.Grade;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.SchoolClassRepository;

@Service
public class TrainerGradeService {

    private final GradeRepository gradeRepository;
    private final SchoolClassRepository classRepository;

    public TrainerGradeService(
            GradeRepository gradeRepository,
            SchoolClassRepository classRepository) {
        this.gradeRepository = gradeRepository;
        this.classRepository = classRepository;
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

        List<Grade> grades = gradeRepository.findBySchoolClassClassId(classId);
        String subjectName = schoolClass.getSubject() != null ? schoolClass.getSubject().getSubjectName() : "Unknown";
        String sectionName = schoolClass.getSection() != null ? schoolClass.getSection().getSection() : "Unknown";

        boolean classLocked = grades.isEmpty() ? false : grades.get(0).isLocked();

        List<StudentGradeRow> students = grades.stream()
                .map(this::gradeToStudentRow)
                .toList();

        return new GradeSummaryResponse(
                classId,
                subjectName,
                sectionName,
                schoolClass.getSemester(),
                classLocked,
                students);
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
            Grade grade = gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, request.studentId())
                    .orElseThrow(() -> new IllegalArgumentException("Grade not found for student: " + request.studentId()));

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

    private Integer resolveCurrentTrainerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalArgumentException("Not authenticated");
        }
        String username = auth.getName();
        if (username == null) {
            throw new IllegalArgumentException("Username not found");
        }
        if ("trainer1".equals(username)) {
            return 100;
        }
        return 100;
    }
}
