package com.example.springboot.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.DocumentFolderHierarchyResponse;
import com.example.springboot.dto.registrar.DocumentFolderHierarchyResponse.StudentFolderDto;
import com.example.springboot.repository.DocumentFolderRepository;
import com.example.springboot.repository.DocumentFolderRepository.BatchRow;
import com.example.springboot.repository.DocumentFolderRepository.SectionRow;
import com.example.springboot.repository.DocumentFolderRepository.StudentRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentFolderServiceTest {

    @Mock private DocumentFolderRepository documentFolderRepository;

    @InjectMocks private DocumentFolderService service;

    @Test
    void batchesSortedByYearDescendingThenCode() {
        when(documentFolderRepository.findBatchRows()).thenReturn(List.of(
                new BatchRow("B2025A", (short) 2025),
                new BatchRow("B2026B", (short) 2026),
                new BatchRow("B2026A", (short) 2026)));
        when(documentFolderRepository.findSectionRows()).thenReturn(List.of());
        when(documentFolderRepository.findStudentRows()).thenReturn(List.of());

        List<DocumentFolderHierarchyResponse> tree = service.getFolderHierarchy();

        assertEquals(3, tree.size());
        assertEquals("B2026A", tree.get(0).batchCode());
        assertEquals("B2026B", tree.get(1).batchCode());
        assertEquals("B2025A", tree.get(2).batchCode());
    }

    @Test
    void emptyBatchAndEmptySectionStillAppear() {
        when(documentFolderRepository.findBatchRows()).thenReturn(List.of(new BatchRow("B2026A", (short) 2026)));
        when(documentFolderRepository.findSectionRows()).thenReturn(List.of(
                new SectionRow("S1", "Section 1", "Culinary Arts", "B2026A")));
        when(documentFolderRepository.findStudentRows()).thenReturn(List.of());

        List<DocumentFolderHierarchyResponse> tree = service.getFolderHierarchy();

        assertEquals(1, tree.size());
        DocumentFolderHierarchyResponse batch = tree.get(0);
        assertEquals(1, batch.sections().size());
        var section = batch.sections().get(0);
        assertEquals(0, section.studentCount());
        assertEquals(0, section.documentCount());
        assertTrue(section.students().isEmpty());
        assertEquals(0, batch.studentCount());
        assertTrue(batch.unassignedStudents().isEmpty());
    }

    @Test
    void assignedStudentUsesSectionsBatchRegardlessOfOwnMismatchedBatch() {
        when(documentFolderRepository.findBatchRows()).thenReturn(List.of(
                new BatchRow("B2026A", (short) 2026), new BatchRow("B2025A", (short) 2025)));
        when(documentFolderRepository.findSectionRows()).thenReturn(List.of(
                new SectionRow("S1", "Section 1", "Culinary Arts", "B2026A")));
        // Student's own batch (B2025A) differs from the section's batch (B2026A).
        when(documentFolderRepository.findStudentRows()).thenReturn(List.of(
                new StudentRow("SR1", null, "Maria", "Dela Cruz", "Active", "B2025A", "S1", 2)));

        List<DocumentFolderHierarchyResponse> tree = service.getFolderHierarchy();

        DocumentFolderHierarchyResponse b2026 = tree.stream()
                .filter(b -> "B2026A".equals(b.batchCode())).findFirst().orElseThrow();
        DocumentFolderHierarchyResponse b2025 = tree.stream()
                .filter(b -> "B2025A".equals(b.batchCode())).findFirst().orElseThrow();

        assertEquals(1, b2026.sections().get(0).students().size());
        assertEquals("SR1", b2026.sections().get(0).students().get(0).studentId());
        assertEquals(1, b2026.studentCount());
        assertEquals(2, b2026.documentCount());
        assertEquals(0, b2025.studentCount());
        assertTrue(b2025.unassignedStudents().isEmpty());
    }

    @Test
    void unassignedStudentWithNullBatchGoesUnderSyntheticNoBatchLast() {
        when(documentFolderRepository.findBatchRows()).thenReturn(List.of(new BatchRow("B2026A", (short) 2026)));
        when(documentFolderRepository.findSectionRows()).thenReturn(List.of());
        when(documentFolderRepository.findStudentRows()).thenReturn(List.of(
                new StudentRow("SR2", null, "Juan", "Santos", "Enrolling", null, null, 0)));

        List<DocumentFolderHierarchyResponse> tree = service.getFolderHierarchy();

        assertEquals(2, tree.size());
        DocumentFolderHierarchyResponse last = tree.get(tree.size() - 1);
        assertNull(last.batchCode());
        assertNull(last.batchYear());
        assertTrue(last.sections().isEmpty());
        assertEquals(1, last.unassignedStudents().size());
        assertEquals("SR2", last.unassignedStudents().get(0).studentId());
        assertEquals(0, last.unassignedStudents().get(0).documentCount());
    }

    @Test
    void noBatchOmittedWhenNoQualifyingStudents() {
        when(documentFolderRepository.findBatchRows()).thenReturn(List.of(new BatchRow("B2026A", (short) 2026)));
        when(documentFolderRepository.findSectionRows()).thenReturn(List.of());
        when(documentFolderRepository.findStudentRows()).thenReturn(List.of());

        List<DocumentFolderHierarchyResponse> tree = service.getFolderHierarchy();

        assertEquals(1, tree.size());
    }

    @Test
    void unassignedStudentWithOwnBatchGoesUnderOwnBatchNotNoBatch() {
        when(documentFolderRepository.findBatchRows()).thenReturn(List.of(new BatchRow("B2026A", (short) 2026)));
        when(documentFolderRepository.findSectionRows()).thenReturn(List.of());
        when(documentFolderRepository.findStudentRows()).thenReturn(List.of(
                new StudentRow("SR3", null, "Ana", "Reyes", "Inactive", "B2026A", null, 1)));

        List<DocumentFolderHierarchyResponse> tree = service.getFolderHierarchy();

        assertEquals(1, tree.size());
        assertEquals(1, tree.get(0).unassignedStudents().size());
        assertEquals("SR3", tree.get(0).unassignedStudents().get(0).studentId());
        assertEquals("Inactive", tree.get(0).unassignedStudents().get(0).studentStatus());
    }

    @Test
    void studentsSortedByLastNameThenFirstNameThenReference() {
        when(documentFolderRepository.findBatchRows()).thenReturn(List.of(new BatchRow("B2026A", (short) 2026)));
        when(documentFolderRepository.findSectionRows()).thenReturn(List.of());
        when(documentFolderRepository.findStudentRows()).thenReturn(List.of(
                new StudentRow("SR2", null, "Ana", "Zamora", "Active", "B2026A", null, 0),
                new StudentRow("SR1", null, "Bea", "Aquino", "Active", "B2026A", null, 0),
                new StudentRow("SR3", null, "Ana", "Aquino", "Active", "B2026A", null, 0)));

        List<StudentFolderDto> sorted = service.getFolderHierarchy().get(0).unassignedStudents();

        assertEquals("SR3", sorted.get(0).studentId());
        assertEquals("SR1", sorted.get(1).studentId());
        assertEquals("SR2", sorted.get(2).studentId());
    }

    @Test
    void aggregateCountsSumSectionsAndUnassignedAcrossTheBatch() {
        when(documentFolderRepository.findBatchRows()).thenReturn(List.of(new BatchRow("B2026A", (short) 2026)));
        when(documentFolderRepository.findSectionRows()).thenReturn(List.of(
                new SectionRow("S1", "Section 1", "Culinary Arts", "B2026A")));
        when(documentFolderRepository.findStudentRows()).thenReturn(List.of(
                new StudentRow("SR1", null, "A", "One", "Active", "B2026A", "S1", 3),
                new StudentRow("SR2", null, "B", "Two", "Active", "B2026A", "S1", 1),
                new StudentRow("SR3", null, "C", "Three", "Active", "B2026A", null, 5)));

        DocumentFolderHierarchyResponse batch = service.getFolderHierarchy().get(0);

        assertEquals(3, batch.studentCount());
        assertEquals(9, batch.documentCount());
        assertEquals(2, batch.sections().get(0).studentCount());
        assertEquals(4, batch.sections().get(0).documentCount());
    }
}
