package com.example.springboot.service;

import org.springframework.http.MediaType;

public enum SystemLogExportFormat {
    CSV("csv", MediaType.parseMediaType("text/csv")),
    XLSX("xlsx", MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
    DOCX("docx", MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

    private final String extension;
    private final MediaType mediaType;

    SystemLogExportFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public static SystemLogExportFormat from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("format is required");
        }

        for (SystemLogExportFormat format : values()) {
            if (format.extension.equalsIgnoreCase(value)) {
                return format;
            }
        }

        throw new IllegalArgumentException("Unsupported export format: " + value);
    }
}
