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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
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
 * Tests {@code RegistrarService.updateStatus} — the sole write path for
 * {@code student_status}, deliberately pulled out of the general edit form (see
 * memory-bank/decisions.md) so a routine record edit can never change it.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarStatusServiceTest {

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

    private StudentRecord record(Integer recordId, String studentId, String status) {
        StudentRecord r = new StudentRecord();
        r.setRecordId(recordId);
        r.setStudentId(studentId);
        r.setLastName("Lipata");
        r.setFirstName("Maria");
        r.setStudentStatus(status);
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

    @Test
    void changesStatusToANewValue() {
        StudentRecord target = record(1, "SR20260001", "Enrolling");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();

        StudentRecordDetailsResponse result = registrarService.updateStatus(1, "Active", null, null);

        assertEquals("Active", result.studentStatus());
        assertEquals("Active", target.getStudentStatus());
    }

    @Test
    void unknownRecordThrows() {
        when(studentRecordRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> registrarService.updateStatus(999, "Active", null, null));
    }

    // ----- Completed / Graduated transitions (spec 2026-10-01 SO checklist 4.2) -----

    private void stubSuccessfulSave(StudentRecord target) {
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();
    }

    @Test
    void activeToCompletedSetsTheCompletionDate() {
        StudentRecord target = record(1, "SR20260001", "Active");
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result =
                registrarService.updateStatus(1, "Completed", LocalDate.of(2026, 9, 30), null);

        assertEquals("Completed", result.studentStatus());
        assertEquals(LocalDate.of(2026, 9, 30), result.completionDate());
        assertEquals(LocalDate.of(2026, 9, 30), target.getCompletionDate());
    }

    @Test
    void activeToCompletedWithoutADateIsRejected() {
        StudentRecord target = record(1, "SR20260001", "Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Completed", null, null));

        assertEquals("A completion date is required for this status change.", ex.getMessage());
        assertEquals("Active", target.getStudentStatus());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aCompletionDateInTheFutureIsRejected() {
        StudentRecord target = record(1, "SR20260001", "Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Completed", LocalDate.now().plusDays(1), null));

        assertEquals("Completion date cannot be in the future.", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aCompletionDateBeforeTheEnrollmentDateIsRejected() {
        StudentRecord target = record(1, "SR20260001", "Active");
        target.setEnrollmentDate(LocalDate.of(2025, 6, 2));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Completed", LocalDate.of(2025, 6, 1), null));

        assertEquals("Completion date cannot be before the enrollment date (2025-06-02).", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void completedBackToActiveClearsTheCompletionDate() {
        StudentRecord target = record(1, "SR20260001", "Completed");
        target.setCompletionDate(LocalDate.of(2026, 9, 30));
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result = registrarService.updateStatus(1, "Active", null, null);

        assertEquals("Active", result.studentStatus());
        assertNull(result.completionDate());
        assertNull(target.getCompletionDate());
    }

    @Test
    void activeToGraduatedWithoutAReasonIsRejected() {
        StudentRecord target = record(1, "SR20260001", "Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Graduated", LocalDate.of(2014, 3, 15), "   "));

        assertEquals("A reason is required for this status change.", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void activeToGraduatedWithADateAndAReasonSetsTheDate() {
        StudentRecord target = record(1, "SR20260001", "Active");
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result = registrarService.updateStatus(
                1, "Graduated", LocalDate.of(2014, 3, 15), "Digitized archive record");

        assertEquals("Graduated", result.studentStatus());
        assertEquals(LocalDate.of(2014, 3, 15), result.completionDate());
    }

    @Test
    void graduatedBackToCompletedKeepsTheCompletionDate() {
        StudentRecord target = record(1, "SR20260001", "Graduated");
        target.setCompletionDate(LocalDate.of(2014, 3, 15));
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result =
                registrarService.updateStatus(1, "Completed", null, "Wrong batch on the SO");

        assertEquals("Completed", result.studentStatus());
        assertEquals(LocalDate.of(2014, 3, 15), result.completionDate());
    }

    @Test
    void aDisallowedMoveIsRejectedWithTheRuleMessage() {
        StudentRecord target = record(1, "SR20260001", "Enrolling");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Completed", LocalDate.of(2026, 9, 30), null));

        assertEquals("Only an Active student can be marked Completed.", ex.getMessage());
        assertEquals("Enrolling", target.getStudentStatus());
        verify(studentRecordRepository, never()).save(any());
    }
}
