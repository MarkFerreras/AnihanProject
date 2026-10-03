package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.OtherGuardianRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentOjtRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentSchoolYearRepository;
import com.example.springboot.repository.StudentTesdaQualificationRepository;

/**
 * Tests the registrar-controlled manual batch assignment and the section invariant that
 * a student enrolled in a section cannot have their batch cleared or diverged from their
 * section's own batch.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarBatchServiceTest {

    @Mock private StudentRecordRepository studentRecordRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private StudentOjtRepository studentOjtRepository;
    @Mock private StudentTesdaQualificationRepository tesdaQualRepository;
    @Mock private StudentSchoolYearRepository schoolYearRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private OtherGuardianRepository guardianRepository;
    @Mock private StudentEducationRepository educationRepository;
    @Mock private GradeRepository gradeRepository;

    @InjectMocks
    private RegistrarService registrarService;

    private StudentRecord record(Integer recordId, String studentId) {
        StudentRecord r = new StudentRecord();
        r.setRecordId(recordId);
        r.setStudentId(studentId);
        r.setLastName("Lipata");
        r.setFirstName("Maria");
        r.setStudentStatus("Active");
        return r;
    }

    /** Attaches a Section (with its own Batch) to the record, to exercise the invariant guard. */
    private Section sectionWithBatch(String sectionCode, String batchCode) {
        Section section = new Section();
        section.setSectionCode(sectionCode);
        section.setBatch(new Batch(batchCode, (short) 2026));
        return section;
    }

    /** buildDetailsResponse reads these child collections; empty is fine for these tests. */
    private void stubEmptyChildLookups() {
        when(studentOjtRepository.findByStudentId(any())).thenReturn(Optional.empty());
        when(tesdaQualRepository.findByStudentIdOrderBySlot(any())).thenReturn(List.of());
        when(schoolYearRepository.findByStudentIdOrderByRowIndex(any())).thenReturn(List.of());
        when(parentRepository.findByStudentStudentIdAndRelation(any(), any())).thenReturn(Optional.empty());
        when(guardianRepository.findByStudentStudentId(any())).thenReturn(List.of());
        when(gradeRepository.findByStudentStudentId(any())).thenReturn(List.of());
    }

    // ----- Assigning -----

    @Test
    void assignBatch_newBatchCode_createsBatchAndAssigns() {
        StudentRecord target = record(1, "SR20260001");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(batchRepository.findById("B2027A")).thenReturn(Optional.empty());
        when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignBatch(1, "B2027A");

        ArgumentCaptor<Batch> batchCaptor = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepository).save(batchCaptor.capture());
        Batch created = batchCaptor.getValue();
        assertEquals("B2027A", created.getBatchCode());
        assertEquals((short) LocalDate.now().getYear(), created.getBatchYear());

        assertEquals("B2027A", result.batchCode());
        assertEquals("B2027A", target.getBatch().getBatchCode());
    }

    @Test
    void assignBatch_existingBatchCode_reusesBatch() {
        StudentRecord target = record(1, "SR20260001");
        Batch existing = new Batch("B2026A", (short) 2026);
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(existing));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignBatch(1, "B2026A");

        verify(batchRepository, never()).save(any(Batch.class));
        assertEquals("B2026A", target.getBatch().getBatchCode());
        assertEquals("B2026A", result.batchCode());
    }

    // ----- Clearing -----

    @Test
    void assignBatch_blankOrNull_clearsBatch() {
        StudentRecord target = record(1, "SR20260001");
        target.setBatch(new Batch("B2026A", (short) 2026));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignBatch(1, "");

        assertNull(result.batchCode());
        assertNull(target.getBatch());
    }

    @Test
    void assignBatch_null_clearsBatch() {
        StudentRecord target = record(1, "SR20260001");
        target.setBatch(new Batch("B2026A", (short) 2026));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignBatch(1, null);

        assertNull(result.batchCode());
        assertNull(target.getBatch());
    }

    // ----- Section invariant -----

    @Test
    void assignBatch_studentInDifferentSection_throws400() {
        StudentRecord target = record(1, "SR20260001");
        target.setSection(sectionWithBatch("SEC-A", "B2026A"));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        assertThrows(IllegalArgumentException.class,
                () -> registrarService.assignBatch(1, "B2027A"));

        verify(studentRecordRepository, never()).save(any(StudentRecord.class));
    }

    @Test
    void assignBatch_studentInSection_clearBatch_throws400() {
        StudentRecord target = record(1, "SR20260001");
        target.setSection(sectionWithBatch("SEC-A", "B2026A"));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        assertThrows(IllegalArgumentException.class,
                () -> registrarService.assignBatch(1, ""));

        verify(studentRecordRepository, never()).save(any(StudentRecord.class));
    }

    @Test
    void assignBatch_studentInSection_clearBatchWithNull_throws400() {
        StudentRecord target = record(1, "SR20260001");
        target.setSection(sectionWithBatch("SEC-A", "B2026A"));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        assertThrows(IllegalArgumentException.class,
                () -> registrarService.assignBatch(1, null));

        verify(studentRecordRepository, never()).save(any(StudentRecord.class));
    }

    @Test
    void assignBatch_studentInMatchingSection_succeeds() {
        StudentRecord target = record(1, "SR20260001");
        target.setSection(sectionWithBatch("SEC-A", "B2026A"));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(target.getSection().getBatch()));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignBatch(1, "B2026A");

        assertEquals("B2026A", result.batchCode());
    }

    // ----- Guards -----

    @Test
    void assignBatch_unknownRecordId_throws404() {
        when(studentRecordRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> registrarService.assignBatch(999, "B2027A"));
    }

    // ----- Edit-form isolation -----

    /**
     * The edit form must never touch batch anymore — batch is exclusively assigned through
     * {@code assignBatch}. Even a garbage batchCode in the edit payload must be ignored.
     */
    @Test
    void updateRecord_editForm_preservesBatch() {
        StudentRecord target = record(1, "SR20260001");
        target.setBatch(new Batch("B2026A", (short) 2026));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordUpdateRequest request = new StudentRecordUpdateRequest(
                "SR20260001", "Lipata-Edited", "Maria", null, null,
                null, null, null, null, null, null, null, false, null, null,
                null, null, null, "B2099Z", null, null,
                null, List.of(), List.of(), null, null, null,
                null, null, null);

        StudentRecordDetailsResponse result = registrarService.updateRecord(1, request);

        assertEquals("Lipata-Edited", result.lastName(), "The edit itself must apply");
        assertEquals("B2026A", result.batchCode(), "Batch must survive a record update untouched");
        assertEquals("B2026A", target.getBatch().getBatchCode());
        verify(batchRepository, never()).findById(any());
    }
}
