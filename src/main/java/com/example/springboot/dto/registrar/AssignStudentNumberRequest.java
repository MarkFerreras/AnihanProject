package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code PUT /api/registrar/student-records/{recordId}/student-number}.
 *
 * <p>Deliberately NOT {@code @NotBlank}: a blank or null value is a legitimate
 * request meaning "clear this student's number". Students are allowed to exist
 * without one until the Registrar (or the archive import) assigns it.
 *
 * <p>The pattern allows the shapes real student numbers take in the paper
 * archive — plain sequences, dashed, and slashed (e.g. {@code 2026-001},
 * {@code CARS/2026/014}).
 */
public record AssignStudentNumberRequest(
        @Size(max = MAX_LENGTH, message = "Student number must be at most 20 characters")
        @Pattern(regexp = PATTERN, message = ALLOWED_CHARS_MESSAGE)
        String studentNumber
) {

    /**
     * The single definition of what a valid student number looks like. The bulk import
     * validates against these same constants, so a value the Assign Number action accepts
     * is exactly a value the import accepts.
     *
     * <p>If the paper archive turns out to use spaces, dots, or other characters, widen
     * {@link #PATTERN} here — and remember the DB column is {@code VARCHAR(20)}, so
     * raising {@link #MAX_LENGTH} needs a migration too.
     */
    public static final int MAX_LENGTH = 20;
    public static final String PATTERN = "^[A-Za-z0-9/-]*$";
    public static final String ALLOWED_CHARS_MESSAGE =
            "Student number may only contain letters, digits, hyphens, and slashes";
}
