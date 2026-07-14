package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for saving a generated document (filled TOR / Form IX) as a
 * self-contained HTML file in the documents table. When {@code documentId} is
 * present, the existing generated document is updated in place (edit flow).
 */
public record GenerateDocumentRequest(
        @NotBlank(message = "Student ID is required") String studentId,
        @NotBlank(message = "Document type is required") String documentType,
        @NotBlank(message = "File name is required") String fileName,
        @NotBlank(message = "Document content is required") String html,
        Integer documentId
) {
}