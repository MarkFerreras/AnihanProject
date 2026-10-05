package com.example.springboot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
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

/**
 * Tests the SO-checklist fields on {@code RegistrarService.updateRecord}: enrollment date
 * (editable for every status), completion date (only for Completed/Graduated) and
 * employment status (spec 2026-10-01 SO checklist §4.3 and §5).
 */
@ExtendWith(MockitoExtension.class)
class RegistrarRecordFieldsServiceTest {

    private static final LocalDate ENROLLED = LocalDate.of(2025, 6, 2);
    private static final LocalDate COMPLETED = LocalDate.of(2026, 3, 20);

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

    private StudentRecord record(String status) {
        StudentRecord r = new StudentRecord();
        r.setRecordId(1);
        r.setStudentId("SR20260001");
        r.setLastName("Lipata");
        r.setFirstName("Maria");
        r.setStudentStatus(status);
        return r;
    }

    private static StudentRecordUpdateRequest request(LocalDate enrollmentDate, LocalDate completionDate,
                                                      String employmentStatus) {
        return new StudentRecordUpdateRequest(
                "SR20260001", "Lipata-Edited", "Maria", null, null,
                null, null, null, null, null, null, null, false, null, null,
                null, null, null, null, null, null,
                null, List.of(), List.of(), null, null, null,
                enrollmentDate, completionDate, employmentStatus);
    }

    private void stubSuccessfulSave(StudentRecord target) {
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentOjtRepository.findByStudentId(any())).thenReturn(Optional.empty());
        when(tesdaQualRepository.findByStudentIdOrderBySlot(any())).thenReturn(List.of());
        when(schoolYearRepository.findByStudentIdOrderByRowIndex(any())).thenReturn(List.of());
        when(parentRepository.findByStudentStudentIdAndRelation(any(), any())).thenReturn(Optional.empty());
        when(guardianRepository.findByStudentStudentId(any())).thenReturn(List.of());
        when(gradeRepository.findByStudentStudentId(any())).thenReturn(List.of());
    }

    @Test
    void theThreeFieldsAreWrittenExactlyAsSent() {
        StudentRecord target = record("Completed");
        target.setEnrollmentDate(LocalDate.of(2024, 1, 8));
        target.setCompletionDate(LocalDate.of(2025, 2, 14));
        target.setEmploymentStatus("Unemployed");
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result =
                registrarService.updateRecord(1, request(ENROLLED, COMPLETED, "Employed"));

        assertEquals("Lipata-Edited", result.lastName(), "The edit itself must apply");
        assertEquals(ENROLLED, target.getEnrollmentDate());
        assertEquals(COMPLETED, target.getCompletionDate());
        assertEquals("Employed", target.getEmploymentStatus());
        assertEquals(ENROLLED, result.enrollmentDate());
        assertEquals(COMPLETED, result.completionDate());
        assertEquals("Employed", result.employmentStatus());
    }

    @Test
    void anActiveStudentsStoredCompletionDateIsKeptWhenTheRequestEchoesIt() {
        StudentRecord target = record("Active");
        target.setCompletionDate(COMPLETED);
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(ENROLLED, COMPLETED, null));

        assertEquals(COMPLETED, target.getCompletionDate());
    }

    @Test
    void anActiveStudentsStoredCompletionDateIsKeptWhenTheRequestOmitsIt() {
        StudentRecord target = record("Active");
        target.setCompletionDate(COMPLETED);
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(ENROLLED, null, null));

        assertEquals(COMPLETED, target.getCompletionDate(), "Not editable for Active, so never wiped");
    }

    @Test
    void aDifferentCompletionDateIsRejectedForAnActiveStudentWithAStoredOne() {
        StudentRecord target = record("Active");
        target.setCompletionDate(COMPLETED);
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateRecord(1, request(ENROLLED, LocalDate.of(2026, 3, 18), null)));

        assertEquals("Completion date can only be set for a Completed or Graduated student. "
                + "Change the status first.", ex.getMessage());
        assertEquals(COMPLETED, target.getCompletionDate());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aNullCompletionDateClearsItForACompletedStudent() {
        // By design (spec 2026-10-01 SO checklist 4.3): the three fields are written exactly as
        // sent, so this relies on the edit form echoing the loaded completion date back
        // (FrontendContractTest pins that the form sends it).
        StudentRecord target = record("Completed");
        target.setCompletionDate(COMPLETED);
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(ENROLLED, null, null));

        assertNull(target.getCompletionDate());
    }

    @Test
    void updateRecordNeverChangesTheStatusOrTheStudentNumber() {
        StudentRecord target = record("Completed");
        target.setStudentNumber("2026-0042");
        target.setCompletionDate(COMPLETED);
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(ENROLLED, COMPLETED, "Employed"));

        assertEquals("Completed", target.getStudentStatus());
        assertEquals("2026-0042", target.getStudentNumber());
    }

    @Test
    void enrollmentDateIsEditableForAnActiveStudent() {
        StudentRecord target = record("Active");
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(ENROLLED, null, null));

        assertEquals(ENROLLED, target.getEnrollmentDate());
    }

    @Test
    void aCompletionDateCannotBeSetForAnActiveStudent() {
        StudentRecord target = record("Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateRecord(1, request(null, COMPLETED, null)));

        assertEquals("Completion date can only be set for a Completed or Graduated student. "
                + "Change the status first.", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aCompletionDateCanBeCorrectedForAGraduatedStudent() {
        StudentRecord target = record("Graduated");
        target.setCompletionDate(COMPLETED);
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(ENROLLED, LocalDate.of(2026, 3, 18), null));

        assertEquals(LocalDate.of(2026, 3, 18), target.getCompletionDate());
    }

    @Test
    void anEnrollmentDateAfterTheCompletionDateIsRejected() {
        StudentRecord target = record("Completed");
        target.setCompletionDate(COMPLETED);
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateRecord(1, request(LocalDate.of(2026, 4, 1), COMPLETED, null)));

        assertEquals("Enrollment date cannot be after the completion date.", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aNullEmploymentStatusMeansNotSet() {
        StudentRecord target = record("Completed");
        target.setCompletionDate(COMPLETED);
        target.setEmploymentStatus("Employed");
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(null, COMPLETED, null));

        assertNull(target.getEmploymentStatus());
    }

    // ========== Task 12: identifier immutability, course validation, failed update ==========

    private static StudentRecordUpdateRequest requestWith(String studentId, String courseCode) {
        return new StudentRecordUpdateRequest(
                studentId, "Lipata", "Maria", null, null,        // id, last, first, middle, birthdate
                null, null, null, null, null, null, null,        // sex .. religion
                false, null, null,                               // baptized, date, place
                null, null, null,                                // sibling counts
                null, courseCode, null,                          // batchCode, courseCode, sectionCode
                null, List.of(), List.of(), null, null, null,    // ojt, tesda, years, father, mother, guardian
                null, null, null);                               // enrollment, completion, employment
    }

    @Test
    void updateRecordNeverChangesStudentIdOrStudentNumber() {
        StudentRecord target = record("Active");
        target.setStudentNumber("2026-0042");
        stubSuccessfulSave(target);

        // 1. The payload type has no student-number component at all.
        assertThat(java.util.Arrays.stream(StudentRecordUpdateRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("studentNumber");

        // 2. A normal edit leaves both identifiers untouched.
        registrarService.updateRecord(1, requestWith("SR20260001", null));
        assertEquals("SR20260001", target.getStudentId());
        assertEquals("2026-0042", target.getStudentNumber());

        // 3. A payload carrying a different student id is rejected and nothing is saved.
        assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateRecord(1, requestWith("SR20269999", null)));
        assertEquals("SR20260001", target.getStudentId());
        assertEquals("2026-0042", target.getStudentNumber());
        verify(studentRecordRepository, times(1)).save(any(StudentRecord.class));
    }

    @Test
    void updateWithUnknownCourseIsRejected() {
        StudentRecord target = record("Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(courseRepository.findById("NOPE")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateRecord(1, requestWith("SR20260001", "NOPE")));

        assertEquals("Course code does not exist: NOPE", ex.getMessage());
        verify(studentRecordRepository, never()).save(any(StudentRecord.class));
    }

    @Test
    void failedUpdateWritesNothing() {
        StudentRecord target = record("Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(courseRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateRecord(1, requestWith("SR20260001", "NOPE")));

        // The failure happens before any persistence call, so no child table is touched.
        verify(studentRecordRepository, never()).save(any(StudentRecord.class));
        verifyNoInteractions(studentOjtRepository, tesdaQualRepository, schoolYearRepository,
                parentRepository, guardianRepository, educationRepository);
    }
}
