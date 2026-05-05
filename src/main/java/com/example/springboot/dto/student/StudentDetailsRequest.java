package com.example.springboot.dto.student;

import java.time.LocalDate;
import java.util.List;

public record StudentDetailsRequest(
    // Step 1 — Personal
    String lastName,
    String firstName,
    String middleName,
    String contactNo,
    LocalDate birthdate,
    String sex,
    String civilStatus,
    String permanentAddress,
    String temporaryAddress,
    Integer siblingCount,
    Integer brotherCount,
    Integer sisterCount,

    // Step 2 — Religion
    String religion,
    Boolean baptized,
    LocalDate baptismDate,
    String baptismPlace,

    // Step 3 — Family
    ParentDto father,
    ParentDto mother,
    GuardianDto guardian,

    // Step 4 — Education
    List<EducationItemDto> educationHistory,
    List<SchoolYearDto> schoolYears
) {}
