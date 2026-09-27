package com.example.springboot.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.springboot.dto.registrar.DocumentFolderHierarchyResponse;
import com.example.springboot.dto.registrar.DocumentFolderHierarchyResponse.SectionFolderDto;
import com.example.springboot.dto.registrar.DocumentFolderHierarchyResponse.StudentFolderDto;
import com.example.springboot.repository.DocumentFolderRepository;
import com.example.springboot.repository.DocumentFolderRepository.BatchRow;
import com.example.springboot.repository.DocumentFolderRepository.SectionRow;
import com.example.springboot.repository.DocumentFolderRepository.StudentRow;

/**
 * Assembles the Documents folder tree (Batch -> Section/Unassigned -> Student)
 * in memory from three flat, BLOB-free reads. A student with a section always
 * lands under that <em>section's</em> batch, even when the student's own
 * {@code batch_code} is null or differs — see spec §2.
 */
@Service
public class DocumentFolderService {

    private final DocumentFolderRepository documentFolderRepository;

    public DocumentFolderService(DocumentFolderRepository documentFolderRepository) {
        this.documentFolderRepository = documentFolderRepository;
    }

    public List<DocumentFolderHierarchyResponse> getFolderHierarchy() {
        List<BatchRow> batches = documentFolderRepository.findBatchRows();
        List<SectionRow> sections = documentFolderRepository.findSectionRows();
        List<StudentRow> students = documentFolderRepository.findStudentRows();

        Map<String, List<SectionRow>> sectionsByBatch = sections.stream()
                .collect(Collectors.groupingBy(SectionRow::batchCode, LinkedHashMap::new, Collectors.toList()));

        Map<String, List<StudentRow>> studentsBySection = new LinkedHashMap<>();
        Map<String, List<StudentRow>> unassignedByBatch = new LinkedHashMap<>();
        for (StudentRow row : students) {
            if (row.sectionCode() != null) {
                studentsBySection.computeIfAbsent(row.sectionCode(), k -> new ArrayList<>()).add(row);
            } else {
                unassignedByBatch.computeIfAbsent(row.batchCode(), k -> new ArrayList<>()).add(row);
            }
        }

        List<BatchRow> sortedBatches = batches.stream()
                .sorted(Comparator.comparing(BatchRow::batchYear, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BatchRow::batchCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        List<DocumentFolderHierarchyResponse> result = new ArrayList<>();
        for (BatchRow batch : sortedBatches) {
            result.add(buildBatchNode(batch.batchCode(), batch.batchYear(),
                    sectionsByBatch.getOrDefault(batch.batchCode(), List.of()),
                    studentsBySection,
                    unassignedByBatch.getOrDefault(batch.batchCode(), List.of())));
        }

        List<StudentRow> noBatchStudents = unassignedByBatch.get(null);
        if (noBatchStudents != null && !noBatchStudents.isEmpty()) {
            result.add(buildBatchNode(null, null, List.of(), studentsBySection, noBatchStudents));
        }

        return result;
    }

    private DocumentFolderHierarchyResponse buildBatchNode(String batchCode, Short batchYear,
            List<SectionRow> sectionRows, Map<String, List<StudentRow>> studentsBySection,
            List<StudentRow> unassignedRows) {

        List<SectionRow> sortedSections = sectionRows.stream()
                .sorted(Comparator.comparing(SectionRow::sectionCode))
                .toList();

        List<SectionFolderDto> sectionDtos = new ArrayList<>();
        long studentTotal = 0;
        long documentTotal = 0;

        for (SectionRow section : sortedSections) {
            List<StudentRow> sectionStudents = studentsBySection.getOrDefault(section.sectionCode(), List.of());
            List<StudentFolderDto> studentDtos = toSortedStudentDtos(sectionStudents);
            long sectionDocs = sectionStudents.stream().mapToLong(StudentRow::documentCount).sum();
            sectionDtos.add(new SectionFolderDto(section.sectionCode(), section.sectionName(),
                    section.courseName(), sectionStudents.size(), sectionDocs, studentDtos));
            studentTotal += sectionStudents.size();
            documentTotal += sectionDocs;
        }

        List<StudentFolderDto> unassignedDtos = toSortedStudentDtos(unassignedRows);
        long unassignedDocs = unassignedRows.stream().mapToLong(StudentRow::documentCount).sum();
        studentTotal += unassignedRows.size();
        documentTotal += unassignedDocs;

        return new DocumentFolderHierarchyResponse(batchCode, batchYear, studentTotal, documentTotal,
                sectionDtos, unassignedDtos);
    }

    private List<StudentFolderDto> toSortedStudentDtos(List<StudentRow> rows) {
        return rows.stream()
                .sorted(Comparator.comparing(StudentRow::lastName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StudentRow::firstName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StudentRow::studentId))
                .map(row -> new StudentFolderDto(row.studentId(), row.studentNumber(), row.firstName(),
                        row.lastName(), row.studentStatus(), row.documentCount()))
                .toList();
    }
}
