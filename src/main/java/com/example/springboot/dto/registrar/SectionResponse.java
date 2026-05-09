package com.example.springboot.dto.registrar;

import com.example.springboot.model.Section;

public record SectionResponse(
    String sectionCode,
    String sectionName,
    String batchCode,
    Short batchYear,
    String courseCode,
    String courseName
) {
    public static SectionResponse from(Section s) {
        return new SectionResponse(
                s.getSectionCode(),
                s.getSection(),
                s.getBatch() != null ? s.getBatch().getBatchCode() : null,
                s.getBatch() != null ? s.getBatch().getBatchYear() : null,
                s.getCourse() != null ? s.getCourse().getCourseCode() : null,
                s.getCourse() != null ? s.getCourse().getCourseName() : null
        );
    }
}
