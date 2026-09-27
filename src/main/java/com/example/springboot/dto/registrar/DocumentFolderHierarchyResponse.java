package com.example.springboot.dto.registrar;

import java.util.List;

/**
 * One batch node of the Documents folder tree. Both {@code batchCode} and
 * {@code batchYear} null denotes the synthetic "No Batch" group, which has no
 * sections — every student it holds is unassigned.
 */
public record DocumentFolderHierarchyResponse(
        String batchCode,
        Short batchYear,
        long studentCount,
        long documentCount,
        List<SectionFolderDto> sections,
        List<StudentFolderDto> unassignedStudents
) {

    public record SectionFolderDto(
            String sectionCode,
            String sectionName,
            String courseName,
            long studentCount,
            long documentCount,
            List<StudentFolderDto> students
    ) {
    }

    public record StudentFolderDto(
            String studentId,
            String studentNumber,
            String firstName,
            String lastName,
            String studentStatus,
            long documentCount
    ) {
    }
}
