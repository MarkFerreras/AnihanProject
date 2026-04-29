package com.example.springboot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "student_education",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "level"}))
public class StudentEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_id")
    private Integer educationId;

    @Column(name = "student_id", length = 20, nullable = false)
    private String studentId;

    @Column(name = "level", length = 10, nullable = false)
    private String level;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "school_address")
    private String schoolAddress;

    @Column(name = "grade_year")
    private String gradeYear;

    @Column(name = "semester")
    private String semester;

    @Column(name = "ended_year", length = 7)
    private String endedYear;

    public StudentEducation() {}

    public Integer getEducationId() { return educationId; }
    public void setEducationId(Integer educationId) { this.educationId = educationId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
    public String getSchoolAddress() { return schoolAddress; }
    public void setSchoolAddress(String schoolAddress) { this.schoolAddress = schoolAddress; }
    public String getGradeYear() { return gradeYear; }
    public void setGradeYear(String gradeYear) { this.gradeYear = gradeYear; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getEndedYear() { return endedYear; }
    public void setEndedYear(String endedYear) { this.endedYear = endedYear; }
}
