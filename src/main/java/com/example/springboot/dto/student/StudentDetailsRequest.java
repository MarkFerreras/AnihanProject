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

    // Step 1 — Religion (merged into Personal in the public wizard).
    // baptized/baptismDate/baptismPlace are retained for registrar-side use and
    // backward compatibility; the public wizard always sends baptized=false.
    String religion,
    Boolean baptized,
    LocalDate baptismDate,
    String baptismPlace,

    // Step 2 — Family
    ParentDto father,
    ParentDto mother,
    GuardianDto guardian,

    // Step 3 — Education
    List<EducationItemDto> educationHistory,
    List<SchoolYearDto> schoolYears
) {}
