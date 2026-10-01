package com.example.springboot.dto.registrar;

import java.time.LocalDateTime;

/**
 * Listing row for the registrar Documents page. Deliberately excludes the
 * LONGBLOB content so table queries never load file bytes into memory.
 * {@code documentLabel} is the optional custom name of an "Others" document.
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
        LocalDateTime uploadDate,
        String documentLabel
) {

    /** An unlabelled summary (every document except a named "Others"). */
    public DocumentSummaryResponse(Integer documentId, String studentId, String lastName, String firstName,
                                   String documentType, String fileName, String fileType, Integer fileSize,
                                   LocalDateTime uploadDate) {
        this(documentId, studentId, lastName, firstName, documentType, fileName, fileType, fileSize,
                uploadDate, null);
    }
}