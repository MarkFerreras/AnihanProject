package com.example.springboot.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Integer gradeId;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "student_id", nullable = false)
    private StudentRecord student;

    @ManyToOne
    @JoinColumn(name = "subject_code", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    /** Raw percentage the trainer enters. Null when {@link #gradeStatus} is used instead. */
    @Column(name = "final_percentage", precision = 5, scale = 2)
    private BigDecimal finalPercentage;

    /** Raw percentage for the optional re-exam. Only meaningful when the final is a failing mark. */
    @Column(name = "re_exam_percentage", precision = 5, scale = 2)
    private BigDecimal reExamPercentage;

    /** 1.00–5.00 grade equivalent, transmuted from {@link #finalPercentage}. */
    @Column(name = "final_grade", precision = 5, scale = 2)
    private BigDecimal finalGrade;

    /** 1.00–5.00 grade equivalent, transmuted from {@link #reExamPercentage}. */
    @Column(name = "re_exam_grade", precision = 5, scale = 2)
    private BigDecimal reExamGrade;

    /** C / FA / INC / D — a TOR status used INSTEAD of a numeric grade. Null for percentage grades. */
    @Column(name = "grade_status", length = 5)
    private String gradeStatus;

    /** Derived token: COMPETENT / NOT_COMPETENT / null. Never entered directly by the trainer. */
    @Column(name = "remarks", length = 20)
    private String remarks;

    /** Attendance hours (TESDA requirement); separate from the fixed curriculum hours. */
    @Column(name = "hours_rendered", precision = 5, scale = 2)
    private BigDecimal hoursRendered;

    @Column(name = "locked")
    private boolean locked = false;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    public Grade() {
    }

    public Integer getGradeId() {
        return gradeId;
    }

    public void setGradeId(Integer gradeId) {
        this.gradeId = gradeId;
    }

    public StudentRecord getStudent() {
        return student;
    }

    public void setStudent(StudentRecord student) {
        this.student = student;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public void setSchoolClass(SchoolClass schoolClass) {
        this.schoolClass = schoolClass;
    }

    public BigDecimal getFinalPercentage() {
        return finalPercentage;
    }

    public void setFinalPercentage(BigDecimal finalPercentage) {
        this.finalPercentage = finalPercentage;
    }

    public BigDecimal getReExamPercentage() {
        return reExamPercentage;
    }

    public void setReExamPercentage(BigDecimal reExamPercentage) {
        this.reExamPercentage = reExamPercentage;
    }

    public BigDecimal getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(BigDecimal finalGrade) {
        this.finalGrade = finalGrade;
    }

    public BigDecimal getReExamGrade() {
        return reExamGrade;
    }

    public void setReExamGrade(BigDecimal reExamGrade) {
        this.reExamGrade = reExamGrade;
    }

    public String getGradeStatus() {
        return gradeStatus;
    }

    public void setGradeStatus(String gradeStatus) {
        this.gradeStatus = gradeStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public BigDecimal getHoursRendered() {
        return hoursRendered;
    }

    public void setHoursRendered(BigDecimal hoursRendered) {
        this.hoursRendered = hoursRendered;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }
}
