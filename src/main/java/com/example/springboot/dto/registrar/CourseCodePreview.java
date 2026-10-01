package com.example.springboot.dto.registrar;

/** Code a typed course name resolves to: an existing course's code, or the one that would be generated. */
public record CourseCodePreview(String code, boolean existing) {
}
