package com.example.springboot.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "other_guardians")
public class OtherGuardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guardian_id")
    private Integer guardianId;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "student_id", nullable = false)
    private StudentRecord student;

    @Column(name = "relation", length = 20)
    private String relation;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @Column(name = "address")
    private String address;

    public OtherGuardian() {
    }

    // Getters and Setters

    public Integer getGuardianId() { return guardianId; }
    public void setGuardianId(Integer guardianId) { this.guardianId = guardianId; }

    public StudentRecord getStudent() { return student; }
    public void setStudent(StudentRecord student) { this.student = student; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public LocalDate getBirthdate() { return birthdate; }
    public void setBirthdate(LocalDate birthdate) { this.birthdate = birthdate; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
