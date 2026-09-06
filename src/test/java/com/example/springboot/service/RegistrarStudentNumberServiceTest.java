package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
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
import com.example.springboot.repository.StudentUploadRepository;

/**
 * Tests the registrar-controlled student number: assigning, changing, clearing,
 * uniqueness enforcement, and the "who is still missing one?" list filter.
 *
 * <p>The student number is deliberately nullable and never auto-generated — the
 * real numbers come from the school's paper archive, so the system must not invent
 * them. These tests pin that behaviour down.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarStudentNumberServiceTest {

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
    @Mock private StudentUploadRepository uploadRepository;
    @Mock private StorageService storageService;
    @Mock private GradeRepository gradeRepository;

    @InjectMocks
    private RegistrarService registrarService;

    private StudentRecord record(Integer recordId, String studentId, String studentNumber) {
        StudentRecord r = new StudentRecord();
        r.setRecordId(recordId);
        r.setStudentId(studentId);
        r.setStudentNumber(studentNumber);
        r.setLastName("Lipata");
        r.setFirstName("Maria");
        r.setStudentStatus("Active");
        return r;
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
    void assignsStudentNumberToRecordThatHasNone() {
        StudentRecord target = record(1, "SR20260001", null);
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.findByStudentNumber("2026-001")).thenReturn(Optional.empty());
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignStudentNumber(1, "2026-001");

        assertEquals("2026-001", result.studentNumber());
        assertEquals("2026-001", target.getStudentNumber());
        // The internal reference must never be touched by this path.
        assertEquals("SR20260001", target.getStudentId());
    }

    @Test
    void trimsSurroundingWhitespaceBeforeSaving() {
        StudentRecord target = record(1, "SR20260001", null);
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.findByStudentNumber("2026-001")).thenReturn(Optional.empty());
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        registrarService.assignStudentNumber(1, "  2026-001  ");

        assertEquals("2026-001", target.getStudentNumber());
    }

    @Test
    void overwritesAnExistingStudentNumber() {
        StudentRecord target = record(1, "SR20260001", "2026-OLD");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.findByStudentNumber("2026-NEW")).thenReturn(Optional.empty());
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignStudentNumber(1, "2026-NEW");

        assertEquals("2026-NEW", result.studentNumber());
    }

    /** Re-saving the same number on the same record must not trip the uniqueness guard. */
    @Test
    void reassigningTheSameNumberToTheSameRecordSucceeds() {
        StudentRecord target = record(1, "SR20260001", "2026-001");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.findByStudentNumber("2026-001")).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignStudentNumber(1, "2026-001");

        assertEquals("2026-001", result.studentNumber());
    }

    // ----- Clearing -----

    @Test
    void blankInputClearsTheStudentNumber() {
        StudentRecord target = record(1, "SR20260001", "2026-001");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.assignStudentNumber(1, "   ");

        assertNull(result.studentNumber());
        assertNull(target.getStudentNumber());
        // No uniqueness lookup is needed when clearing.
        verify(studentRecordRepository, never()).findByStudentNumber(any());
    }

    @Test
    void nullInputClearsTheStudentNumber() {
        StudentRecord target = record(1, "SR20260001", "2026-001");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        assertNull(registrarService.assignStudentNumber(1, null).studentNumber());
    }

    // ----- Guards -----

    @Test
    void rejectsANumberAlreadyAssignedToAnotherStudent() {
        StudentRecord target = record(1, "SR20260001", null);
        StudentRecord other = record(2, "SR20260002", "2026-001");
        other.setLastName("Reyes");
        other.setFirstName("Ana");

        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.findByStudentNumber("2026-001")).thenReturn(Optional.of(other));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.assignStudentNumber(1, "2026-001"));

        assertTrue(ex.getMessage().contains("already assigned"),
                "Message should tell the registrar the number is taken: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Reyes"),
                "Message should name the student holding it: " + ex.getMessage());
        assertNull(target.getStudentNumber(), "Target must be left untouched on rejection");
        verify(studentRecordRepository, never()).save(any(StudentRecord.class));
    }

    @Test
    void throwsWhenRecordDoesNotExist() {
        when(studentRecordRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> registrarService.assignStudentNumber(999, "2026-001"));
    }

    /**
     * The edit form loads a record and PUTs the whole thing back, but its payload carries
     * no student number (that field is read-only there). {@code updateRecord} must therefore
     * leave the number alone — otherwise every routine edit would silently wipe it.
     */
    @Test
    void updatingTheRecordDoesNotWipeTheStudentNumber() {
        StudentRecord target = record(1, "SR20260001", "2026-001");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordUpdateRequest request = new StudentRecordUpdateRequest(
                "SR20260001", "Lipata-Edited", "Maria", null, null,
                null, null, null, null, null, null, null, false, null, null,
                null, null, null, null, null, null, "Active",
                null, List.of(), List.of(), null, null, null);

        StudentRecordDetailsResponse result = registrarService.updateRecord(1, request);

        assertEquals("Lipata-Edited", result.lastName(), "The edit itself must apply");
        assertEquals("2026-001", result.studentNumber(), "Student number must survive a record update");
        assertEquals("2026-001", target.getStudentNumber());
    }

    // ----- List filter -----

    @Test
    void hasStudentNumberFilterPartitionsRecords() {
        when(studentRecordRepository.findAll()).thenReturn(List.of(
                record(1, "SR20260001", "2026-001"),
                record(2, "SR20260002", null),
                record(3, "SR20260003", "   "),   // blank counts as missing
                record(4, "SR20260004", "2026-004")
        ));

        List<StudentRecordSummaryResponse> missing =
                registrarService.getAllRecords(null, null, null, null, Boolean.FALSE);
        assertEquals(2, missing.size(), "Blank and null both count as 'no student number'");

        List<StudentRecordSummaryResponse> assigned =
                registrarService.getAllRecords(null, null, null, null, Boolean.TRUE);
        assertEquals(2, assigned.size());

        List<StudentRecordSummaryResponse> all =
                registrarService.getAllRecords(null, null, null, null, null);
        assertEquals(4, all.size(), "A null filter must not restrict the result set");
    }

    @Test
    void freeTextSearchMatchesOnStudentNumber() {
        when(studentRecordRepository.findAll()).thenReturn(List.of(
                record(1, "SR20260001", "2026-001"),
                record(2, "SR20260002", null)
        ));

        List<StudentRecordSummaryResponse> found = registrarService.getAllRecords("2026-001");

        assertEquals(1, found.size());
        assertEquals("2026-001", found.get(0).studentNumber());
    }
}
