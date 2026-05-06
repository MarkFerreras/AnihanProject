package com.example.springboot.dto.registrar;

import java.time.LocalDate;
import java.util.List;

import com.example.springboot.dto.student.OjtDto;
import com.example.springboot.dto.student.SchoolYearDto;
import com.example.springboot.dto.student.TesdaQualDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record StudentRecordUpdateRequest(
        @NotBlank(message = "Student ID is required")
        @Size(max = 20, message = "Student ID must be at most 20 characters")
        String studentId,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Middle name is required")
        String middleName,

        @PastOrPresent(message = "Birthdate cannot be in the future")
        LocalDate birthdate,

        String sex,
        String civilStatus,
        String permanentAddress,
        String temporaryAddress,
        String email,
        String contactNo,
        String religion,
        Boolean baptized,
        LocalDate baptismDate,
        String baptismPlace,
        Integer siblingCount,
        Integer brotherCount,
        Integer sisterCount,
        String batchCode,
        String courseCode,
        String sectionCode,
        @NotBlank(message = "Status is required")
        String studentStatus,

        OjtDto ojt,
        List<TesdaQualDto> tesdaQualifications,
        List<SchoolYearDto> schoolYears
) {
}
