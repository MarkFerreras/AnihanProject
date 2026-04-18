package com.example.springboot.service;

import org.springframework.http.MediaType;

public record SystemLogExportFile(
        byte[] content,
        MediaType mediaType,
        String fileName
) {
}
