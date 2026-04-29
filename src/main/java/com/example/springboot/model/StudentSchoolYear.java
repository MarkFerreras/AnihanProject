package com.example.springboot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "student_school_years",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "row_index"}))
public class StudentSchoolYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "school_year_id")
    private Integer schoolYearId;

    @Column(name = "student_id", length = 20, nullable = false)
    private String studentId;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    @Column(name = "sy_start", length = 10)
    private String syStart;

    @Column(name = "sem_start", length = 10)
    private String semStart;

    @Column(name = "sy_end", length = 10)
    private String syEnd;

    @Column(name = "sem_end", length = 10)
    private String semEnd;

    @Column(name = "remarks")
    private String remarks;

    public StudentSchoolYear() {}

    public Integer getSchoolYearId() { return schoolYearId; }
    public void setSchoolYearId(Integer schoolYearId) { this.schoolYearId = schoolYearId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Integer getRowIndex() { return rowIndex; }
    public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }
    public String getSyStart() { return syStart; }
    public void setSyStart(String syStart) { this.syStart = syStart; }
    public String getSemStart() { return semStart; }
    public void setSemStart(String semStart) { this.semStart = semStart; }
    public String getSyEnd() { return syEnd; }
    public void setSyEnd(String syEnd) { this.syEnd = syEnd; }
    public String getSemEnd() { return semEnd; }
    public void setSemEnd(String semEnd) { this.semEnd = semEnd; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
