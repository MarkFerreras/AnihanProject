package com.example.springboot.dto.trainer;

import java.util.List;

public record GradeSummaryResponse(
    Integer classId,
    String subjectName,
    String sectionName,
    String semester,
    boolean locked,
    List<StudentGradeRow> students
) {
}
