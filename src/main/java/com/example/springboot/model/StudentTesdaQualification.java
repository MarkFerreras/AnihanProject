package com.example.springboot.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "student_tesda_qualifications",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "slot"}))
public class StudentTesdaQualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qual_id")
    private Integer qualId;

    @Column(name = "student_id", length = 20, nullable = false)
    private String studentId;

    @Column(name = "slot", nullable = false)
    private Integer slot;

    @Column(name = "title")
    private String title;

    @Column(name = "center_address")
    private String centerAddress;

    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    @Column(name = "result", length = 25)
    private String result;

    public StudentTesdaQualification() {}

    public Integer getQualId() { return qualId; }
    public void setQualId(Integer qualId) { this.qualId = qualId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Integer getSlot() { return slot; }
    public void setSlot(Integer slot) { this.slot = slot; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCenterAddress() { return centerAddress; }
    public void setCenterAddress(String centerAddress) { this.centerAddress = centerAddress; }
    public LocalDate getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDate assessmentDate) { this.assessmentDate = assessmentDate; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
