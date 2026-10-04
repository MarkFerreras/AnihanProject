package com.example.springboot.dto.registrar;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code PUT /api/registrar/student-records/{recordId}/status}.
 *
 * <p>Deliberately its own endpoint rather than a field on {@link StudentRecordUpdateRequest} —
 * a status change is a workflow decision, not an incidental edit, so it gets its own dedicated
 * action and its own audit log line, the same treatment already given to {@code student_number}.
 *
 * <p>"Submitted" is deliberately excluded from the allowed values: that status is set only by
 * the student portal's own enrollment flow and is never one the Registrar assigns directly.
 */
public record UpdateStudentStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = ALLOWED_VALUES_PATTERN, message = "Status must be one of: " + ALLOWED_VALUES_DISPLAY)
        String studentStatus,

        // Required for Active -> Completed and Active -> Graduated. Which moves need it is
        // decided by StudentStatusTransitions and checked in RegistrarService.updateStatus.
        LocalDate completionDate,

        // Required for Active -> Graduated and Graduated -> Completed (same checking as above).
        // Logged with the status change, so it is capped to keep system_logs.action under 500.
        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
    public static final String ALLOWED_VALUES_PATTERN = "^(Enrolling|Active|Completed|Graduated)$";
    public static final String ALLOWED_VALUES_DISPLAY = "Enrolling, Active, Completed, Graduated";
}
