package com.example.springboot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @Column(name = "subject_code", length = 20)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @ManyToOne
    @JoinColumn(name = "qualification_code", nullable = false)
    private Qualification qualification;

    @Column(name = "units", nullable = false)
    private Integer units;

    @ManyToOne
    @JoinColumn(name = "trainer_id")
    private User trainer;

    public Subject() {
    }

    // Getters and Setters

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Qualification getQualification() {
        return qualification;
    }

    public void setQualification(Qualification qualification) {
        this.qualification = qualification;
    }

    public Integer getUnits() {
        return units;
    }

    public void setUnits(Integer units) {
        this.units = units;
    }

    public User getTrainer() {
        return trainer;
    }

    public void setTrainer(User trainer) {
        this.trainer = trainer;
    }
}
