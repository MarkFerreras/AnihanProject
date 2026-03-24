package com.example.springboot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sections")
public class Section {

    @Id
    @Column(name = "section_code", length = 20)
    private String sectionCode;

    @Column(name = "section", nullable = false, length = 25)
    private String section;

    @ManyToOne
    @JoinColumn(name = "batch_code", nullable = false)
    private Batch batch;

    @ManyToOne
    @JoinColumn(name = "course_code", nullable = false)
    private Course course;

    public Section() {
    }

    // Getters and Setters

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
