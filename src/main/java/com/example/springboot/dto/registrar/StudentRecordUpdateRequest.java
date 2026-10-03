package com.example.springboot.dto.registrar;

import java.time.LocalDate;
import java.util.List;

import com.example.springboot.dto.student.GuardianDto;
import com.example.springboot.dto.student.OjtDto;
import com.example.springboot.dto.student.ParentDto;
import com.example.springboot.dto.student.SchoolYearDto;
import com.example.springboot.dto.student.TesdaQualDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentRecordUpdateRequest(
        @NotBlank(message = "Student ID is required")
        @Size(max = 20, message = "Student ID must be at most 20 characters")
        String studentId,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "First name is required")
        String firstName,

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

        // Ignored by RegistrarService.updateRecord() — batch is written only through
        // PUT /api/registrar/student-records/{id}/batch (see RegistrarService.assignBatch).
        String batchCode,
        String courseCode,
        String sectionCode,

        OjtDto ojt,
        List<TesdaQualDto> tesdaQualifications,
        List<SchoolYearDto> schoolYears,
        ParentDto father,
        ParentDto mother,
        GuardianDto guardian,

        // The three fields below are written exactly as sent: null clears enrollmentDate and
        // employmentStatus for every status, and clears completionDate for Completed/Graduated
        // (it is left untouched for other statuses). The edit form must therefore always send the
        // loaded values back (spec 2026-10-01 SO checklist §4.3; pinned by FrontendContractTest).
        @PastOrPresent(message = "Enrollment date cannot be in the future")
        LocalDate enrollmentDate,

        // Accepted only while the student is Completed or Graduated — see
        // RegistrarService.updateRecord. Normally set by the status endpoint.
        @PastOrPresent(message = "Completion date cannot be in the future")
        LocalDate completionDate,

        @Pattern(regexp = EMPLOYMENT_STATUS_PATTERN,
                message = "Employment status must be one of: " + EMPLOYMENT_STATUS_DISPLAY)
        String employmentStatus
) {
    /**
     * The four Employment Status values (spec §5); null means "Not set". The
     * #editEmploymentStatus select in student-records.html lists the same values,
     * pinned by FrontendContractTest.
     */
    public static final String EMPLOYMENT_STATUS_PATTERN = "^(Employed|Self-employed|Unemployed|Further studies)$";
    public static final String EMPLOYMENT_STATUS_DISPLAY = "Employed, Self-employed, Unemployed, Further studies";
}
