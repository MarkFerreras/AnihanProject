package com.example.springboot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "qualifications")
public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qualification_code")
    private Integer qualificationCode;

    @Column(name = "qualification_name", nullable = false)
    private String qualificationName;

    @Column(name = "qualification_description", nullable = false)
    private String qualificationDescription;

    public Qualification() {
    }

    public Qualification(String qualificationName, String qualificationDescription) {
        this.qualificationName = qualificationName;
        this.qualificationDescription = qualificationDescription;
    }

    // Getters and Setters

    public Integer getQualificationCode() {
        return qualificationCode;
    }

    public void setQualificationCode(Integer qualificationCode) {
        this.qualificationCode = qualificationCode;
    }

    public String getQualificationName() {
        return qualificationName;
    }

    public void setQualificationName(String qualificationName) {
        this.qualificationName = qualificationName;
    }

    public String getQualificationDescription() {
        return qualificationDescription;
    }

    public void setQualificationDescription(String qualificationDescription) {
        this.qualificationDescription = qualificationDescription;
    }
}
