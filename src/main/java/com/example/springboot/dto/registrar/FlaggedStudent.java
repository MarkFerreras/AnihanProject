package com.example.springboot.dto.registrar;

import java.util.List;

/**
 * One student in an export scope who is missing required documents.
 * {@code documentCount == 0} means the student will not appear in the ZIP.
 */
public record FlaggedStudent(
        String studentId,
        String studentNumber,
        String lastName,
        String firstName,
        String studentStatus,
        String sectionCode,
        long documentCount,
        List<String> missing
) {
}
