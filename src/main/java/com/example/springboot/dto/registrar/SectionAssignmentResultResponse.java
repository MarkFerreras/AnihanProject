package com.example.springboot.dto.registrar;

import java.util.List;

public record SectionAssignmentResultResponse(
        int assignedCount,
        List<String> skippedStudentIds,
        List<String> reasons
) {
}
