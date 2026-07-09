package com.example.springboot.dto.registrar;

import java.time.LocalDateTime;

/**
 * Listing row for the registrar Documents page. Deliberately excludes the
 * LONGBLOB content so table queries never load file bytes into memory.
 */
public record DocumentSummaryResponse(
        Integer documentId,
        String studentId,
        String lastName,
        String firstName,
        String documentType,
        String fileName,
        String fileType,
        Integer fileSize,
        LocalDateTime uploadDate
) {
}