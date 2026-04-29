package com.example.springboot.dto.student;

import java.time.LocalDateTime;

public record UploadRefDto(
    Integer uploadId,
    String kind,
    String originalName,
    String mimeType,
    Long sizeBytes,
    LocalDateTime uploadedAt
) {}
