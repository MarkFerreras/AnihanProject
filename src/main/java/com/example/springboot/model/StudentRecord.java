package com.example.springboot.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_records")
public class StudentRecord {

    @Id
    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name", nullable = false)
    private String middleName;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @Column(name = "age")
    private Integer age;

    @Column(name = "sex", length = 10)
    private String sex;

    @Column(name = "civil_status", length = 50)
    private String civilStatus;

    @Column(name = "permanent_address")
    private String permanentAddress;

    @Column(name = "temporary_address")
    private String temporaryAddress;

    @Column(name = "email")
    private String email;

    @Column(name = "contact_no")
    private String contactNo;

    @Column(name = "religion")
    private String religion;

    @Column(name = "baptized", nullable = false)
    private Boolean baptized = false;

    @Column(name = "baptism_date")
    private LocalDate baptismDate;

    @Column(name = "baptism_place")
    private String baptismPlace;

    @Column(name = "sibling_count")
    private Integer siblingCount;

    @Column(name = "brother_count")
    private Integer brotherCount;

    @Column(name = "sister_count")
    private Integer sisterCount;

    @ManyToOne(optional = true)
    @JoinColumn(name = "batch_code", nullable = true)
    private Batch batch;

    @ManyToOne(optional = true)
    @JoinColumn(name = "course_code", nullable = true)
    private Course course;

    @ManyToOne(optional = true)
    @JoinColumn(name = "section_code", nullable = true)
    private Section section;

    @Lob
    @Column(name = "profile_picture", columnDefinition = "MEDIUMBLOB")
    private byte[] profilePicture;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "student_status", nullable = false, length = 25)
    private String studentStatus = "Enrolling";

    public StudentRecord() {
    }

    // Getters and Setters

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public LocalDate getBirthdate() { return birthdate; }
    public void setBirthdate(LocalDate birthdate) { this.birthdate = birthdate; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public String getCivilStatus() { return civilStatus; }
    public void setCivilStatus(String civilStatus) { this.civilStatus = civilStatus; }

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public String getTemporaryAddress() { return temporaryAddress; }
    public void setTemporaryAddress(String temporaryAddress) { this.temporaryAddress = temporaryAddress; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public Boolean getBaptized() { return baptized; }
    public void setBaptized(Boolean baptized) { this.baptized = baptized; }

    public LocalDate getBaptismDate() { return baptismDate; }
    public void setBaptismDate(LocalDate baptismDate) { this.baptismDate = baptismDate; }

    public String getBaptismPlace() { return baptismPlace; }
    public void setBaptismPlace(String baptismPlace) { this.baptismPlace = baptismPlace; }

    public Integer getSiblingCount() { return siblingCount; }
    public void setSiblingCount(Integer siblingCount) { this.siblingCount = siblingCount; }

    public Integer getBrotherCount() { return brotherCount; }
    public void setBrotherCount(Integer brotherCount) { this.brotherCount = brotherCount; }

    public Integer getSisterCount() { return sisterCount; }
    public void setSisterCount(Integer sisterCount) { this.sisterCount = sisterCount; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }

    public byte[] getProfilePicture() { return profilePicture; }
    public void setProfilePicture(byte[] profilePicture) { this.profilePicture = profilePicture; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public String getStudentStatus() { return studentStatus; }
    public void setStudentStatus(String studentStatus) { this.studentStatus = studentStatus; }
}
