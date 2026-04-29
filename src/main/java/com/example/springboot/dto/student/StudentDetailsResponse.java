package com.example.springboot.dto.student;

import java.time.LocalDate;
import java.util.List;

public record StudentDetailsResponse(
    String studentId,
    String lastName,
    String firstName,
    String middleName,
    String studentStatus,

    // Personal
    String contactNo,
    LocalDate birthdate,
    Integer age,
    String sex,
    String civilStatus,
    String permanentAddress,
    String temporaryAddress,
    Integer siblingCount,
    Integer brotherCount,
    Integer sisterCount,

    // Religion
    String religion,
    Boolean baptized,
    LocalDate baptismDate,
    String baptismPlace,

    // Uploads
    UploadRefDto idPhotoRef,
    UploadRefDto baptismalCertRef,

    // Family
    ParentDto father,
    ParentDto mother,
    GuardianDto guardian,

    // Education
    List<EducationItemDto> educationHistory,
    List<SchoolYearDto> schoolYears,
    OjtDto ojt,
    List<TesdaQualDto> tesdaQualifications
) {}
