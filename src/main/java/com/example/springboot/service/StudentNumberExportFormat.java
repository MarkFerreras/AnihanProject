package com.example.springboot.service;

import org.springframework.http.MediaType;

/**
 * Formats the student-number report can be exported as.
 *
 * <p>Deliberately separate from {@link SystemLogExportFormat}: that enum is scoped to
 * logs and includes DOCX, which is not round-trippable and so is not offered here — an
 * export from this page exists to be edited and imported back.
 */
public enum StudentNumberExportFormat {
    CSV("csv", MediaType.parseMediaType("text/csv")),
    XLSX("xlsx", MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    private final String extension;
    private final MediaType mediaType;

    StudentNumberExportFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public static StudentNumberExportFormat from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("format is required");
        }
        for (StudentNumberExportFormat format : values()) {
            if (format.extension.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported export format: " + value + ". Allowed: csv, xlsx.");
    }
}
