package com.example.springboot.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_ojt")
public class StudentOjt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ojt_id")
    private Integer ojtId;

    @Column(name = "student_id", length = 20, nullable = false, unique = true)
    private String studentId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_address")
    private String companyAddress;

    @Column(name = "hours_rendered", precision = 6, scale = 2)
    private BigDecimal hoursRendered;

    public StudentOjt() {}

    public Integer getOjtId() { return ojtId; }
    public void setOjtId(Integer ojtId) { this.ojtId = ojtId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    public BigDecimal getHoursRendered() { return hoursRendered; }
    public void setHoursRendered(BigDecimal hoursRendered) { this.hoursRendered = hoursRendered; }
}
