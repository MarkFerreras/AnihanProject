package com.example.springboot.model;

import java.math.BigDecimal;

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
    @JoinColumn(name = "student_id", nullable = false)
    private StudentRecord student;

    @ManyToOne
    @JoinColumn(name = "subject_code", nullable = false)
    private Subject subject;

    @Column(name = "final_grade", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalGrade = BigDecimal.ZERO;

    @Column(name = "re_exam_grade", precision = 5, scale = 2)
    private BigDecimal reExamGrade;

    @Column(name = "hours_studied", nullable = false, precision = 3, scale = 2)
    private BigDecimal hoursStudied;

    @Column(name = "remarks", nullable = false)
    private String remarks;

    public Grade() {
    }

    // Getters and Setters

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

    public BigDecimal getHoursStudied() {
        return hoursStudied;
    }

    public void setHoursStudied(BigDecimal hoursStudied) {
        this.hoursStudied = hoursStudied;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
