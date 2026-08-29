package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.dto.registrar.StudentNumberImportOutcome;
import com.example.springboot.dto.registrar.StudentNumberImportReport;
import com.example.springboot.dto.registrar.StudentNumberImportRowResult;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.StudentRecordRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentNumberImportServiceTest {

    @Mock
    private StudentRecordRepository studentRecordRepository;

    private StudentNumberImportService service;

    /** In-memory stand-in for the students table, keyed by reference and by number. */
    private final Map<String, StudentRecord> byReference = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new StudentNumberImportService(new StudentNumberSheetParser(), studentRecordRepository);

        when(studentRecordRepository.findByStudentId(any()))
                .thenAnswer(inv -> Optional.ofNullable(byReference.get(inv.getArgument(0))));
        when(studentRecordRepository.findByStudentNumber(any()))
                .thenAnswer(inv -> byReference.values().stream()
                        .filter(r -> inv.getArgument(0).equals(r.getStudentNumber()))
                        .findFirst());
        when(studentRecordRepository.findById(any()))
                .thenAnswer(inv -> byReference.values().stream()
                        .filter(r -> inv.getArgument(0).equals(r.getRecordId()))
                        .findFirst());
        when(studentRecordRepository.save(any(StudentRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private StudentRecord givenStudent(int recordId, String reference, String number,
                                       String last, String first) {
        StudentRecord r = new StudentRecord();
        r.setRecordId(recordId);
        r.setStudentId(reference);
        r.setStudentNumber(number);
        r.setLastName(last);
        r.setFirstName(first);
        byReference.put(reference, r);
        return r;
    }

    private MultipartFile csv(String body) {
        return new MockMultipartFile("file", "sheet.csv", "text/csv",
                body.getBytes(StandardCharsets.UTF_8));
    }

    private StudentNumberImportOutcome outcomeOf(StudentNumberImportReport report, int index) {
        return report.rows().get(index).outcome();
    }

    // ----- Happy path -----

    @Test
    void previewClassifiesANewAssignmentButWritesNothing() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                """), false);

        assertEquals(1, report.totalRows());
        assertEquals(1, report.applicableRows());
        assertEquals(StudentNumberImportOutcome.WILL_ASSIGN, outcomeOf(report, 0));
        assertTrue(report.applied() == false);
        verify(studentRecordRepository, never()).save(any());
        assertNull(byReference.get("SR20260001").getStudentNumber());
    }

    @Test
    void applyWritesTheAssignment() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.apply(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                """), false);

        assertTrue(report.applied());
        assertEquals(1, report.applicableRows());
        assertEquals("2026-001", byReference.get("SR20260001").getStudentNumber());
    }

    @Test
    void applyWritesOnlyTheApplicableRows() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");
        givenStudent(2, "SR20260002", null, "Reyes", "Anna");

        StudentNumberImportReport report = service.apply(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                SR99999999,2026-002
                SR20260002,
                """), false);

        assertEquals(1, report.applicableRows());
        assertEquals("2026-001", byReference.get("SR20260001").getStudentNumber());
        assertNull(byReference.get("SR20260002").getStudentNumber(), "Blank row must not write");
    }

    // ----- Every non-applicable outcome -----

    @Test
    void blankNumberIsSkippedNotTreatedAsAnError() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR20260001,
                """), false);

        assertEquals(StudentNumberImportOutcome.BLANK, outcomeOf(report, 0));
        assertEquals(0, report.applicableRows());
    }

    @Test
    void unknownReferenceIsReported() {
        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR99999999,2026-001
                """), false);

        assertEquals(StudentNumberImportOutcome.UNKNOWN_REFERENCE, outcomeOf(report, 0));
        assertTrue(report.rows().get(0).message().contains("SR99999999"));
    }

    @Test
    void numberAlreadyHeldByAnotherStudentIsAConflict() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");
        givenStudent(2, "SR20260002", "2026-001", "Reyes", "Anna");

        StudentNumberImportReport report = service.apply(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                """), false);

        assertEquals(StudentNumberImportOutcome.CONFLICT_IN_USE, outcomeOf(report, 0));
        assertTrue(report.rows().get(0).message().contains("Reyes"),
                "Message should name the holder: " + report.rows().get(0).message());
        assertNull(byReference.get("SR20260001").getStudentNumber());
        assertEquals("2026-001", byReference.get("SR20260002").getStudentNumber(), "Holder untouched");
    }

    @Test
    void existingDifferentNumberIsNotOverwrittenByDefault() {
        givenStudent(1, "SR20260001", "2026-OLD", "Ligan", "Sean");

        StudentNumberImportReport report = service.apply(csv("""
                Reference No.,Student Number
                SR20260001,2026-NEW
                """), false);

        assertEquals(StudentNumberImportOutcome.CONFLICT_EXISTING, outcomeOf(report, 0));
        assertEquals("2026-OLD", byReference.get("SR20260001").getStudentNumber());
    }

    @Test
    void existingNumberIsOverwrittenWhenExplicitlyAllowed() {
        givenStudent(1, "SR20260001", "2026-OLD", "Ligan", "Sean");

        StudentNumberImportReport report = service.apply(csv("""
                Reference No.,Student Number
                SR20260001,2026-NEW
                """), true);

        assertEquals(StudentNumberImportOutcome.WILL_OVERWRITE, outcomeOf(report, 0));
        assertEquals("2026-NEW", byReference.get("SR20260001").getStudentNumber());
    }

    @Test
    void aValueThatAlreadyMatchesIsReportedAsUnchanged() {
        givenStudent(1, "SR20260001", "2026-001", "Ligan", "Sean");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                """), false);

        assertEquals(StudentNumberImportOutcome.UNCHANGED, outcomeOf(report, 0));
        assertEquals(0, report.applicableRows());
    }

    @Test
    void aNumberRepeatedInTheFileBlocksBothRows() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");
        givenStudent(2, "SR20260002", null, "Reyes", "Anna");

        StudentNumberImportReport report = service.apply(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                SR20260002,2026-001
                """), false);

        assertEquals(StudentNumberImportOutcome.DUPLICATE_IN_FILE, outcomeOf(report, 0));
        assertEquals(StudentNumberImportOutcome.DUPLICATE_IN_FILE, outcomeOf(report, 1));
        assertEquals(0, report.applicableRows(), "Neither row is applied — we cannot know which was meant");
        assertNull(byReference.get("SR20260001").getStudentNumber());
        assertNull(byReference.get("SR20260002").getStudentNumber());
    }

    @Test
    void illegalCharactersAreRejectedWithTheSameRuleAsSingleAssignment() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR20260001,2026 001!
                """), false);

        assertEquals(StudentNumberImportOutcome.INVALID_FORMAT, outcomeOf(report, 0));
    }

    @Test
    void overlyLongNumbersAreRejected() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR20260001,123456789012345678901
                """), false);

        assertEquals(StudentNumberImportOutcome.INVALID_FORMAT, outcomeOf(report, 0));
        assertTrue(report.rows().get(0).message().contains("20"));
    }

    /** Rows slipping out of alignment is the realistic spreadsheet accident. */
    @Test
    void nameDisagreementIsAppliedButFlagged() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.apply(csv("""
                Reference No.,Last Name,First Name,Student Number
                SR20260001,Santos,Bea,2026-001
                """), false);

        StudentNumberImportRowResult row = report.rows().get(0);
        assertEquals(StudentNumberImportOutcome.NAME_MISMATCH, row.outcome());
        assertTrue(row.outcome().applicable(), "A name mismatch is a warning, not a rejection");
        assertEquals("2026-001", byReference.get("SR20260001").getStudentNumber());
        assertTrue(row.message().contains("Santos") && row.message().contains("Ligan"), row.message());
    }

    @Test
    void matchingNamesDoNotTriggerAWarning() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Last Name,First Name,Student Number
                SR20260001,ligan,SEAN,2026-001
                """), false);

        assertEquals(StudentNumberImportOutcome.WILL_ASSIGN, outcomeOf(report, 0),
                "Name comparison must be case-insensitive");
    }

    // ----- Counts and file guards -----

    @Test
    void reportCountsEveryOutcome() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");
        givenStudent(2, "SR20260002", "2026-002", "Reyes", "Anna");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                SR20260002,2026-002
                SR99999999,2026-003
                SR20260001,
                """), false);

        assertEquals(4, report.totalRows());
        assertEquals(1, report.countsByOutcome().get(StudentNumberImportOutcome.WILL_ASSIGN));
        assertEquals(1, report.countsByOutcome().get(StudentNumberImportOutcome.UNCHANGED));
        assertEquals(1, report.countsByOutcome().get(StudentNumberImportOutcome.UNKNOWN_REFERENCE));
        assertEquals(1, report.countsByOutcome().get(StudentNumberImportOutcome.BLANK));
    }

    @Test
    void rejectsAnEmptyUpload() {
        MultipartFile empty = new MockMultipartFile("file", "sheet.csv", "text/csv", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service.preview(empty, false));
    }

    @Test
    void rejectsAnUnsupportedExtension() {
        MultipartFile pdf = new MockMultipartFile("file", "sheet.pdf", "application/pdf",
                "data".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.preview(pdf, false));
        assertTrue(ex.getMessage().contains("csv"), ex.getMessage());
    }

    @Test
    void reportEchoesTheFileName() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                """), false);

        assertEquals("sheet.csv", report.fileName());
    }

    @Test
    void rowNumbersPointAtTheSpreadsheetRow() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        StudentNumberImportReport report = service.preview(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                """), false);

        assertEquals(2, report.rows().get(0).rowNumber());
    }

    @Test
    void importedNumbersAreTrimmed() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        service.apply(csv("""
                Reference No.,Student Number
                SR20260001,  2026-001
                """), false);

        assertEquals("2026-001", byReference.get("SR20260001").getStudentNumber());
    }

    @Test
    void aSheetOfOnlyBlankNumbersAppliesNothing() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");
        givenStudent(2, "SR20260002", null, "Reyes", "Anna");

        StudentNumberImportReport report = service.apply(csv("""
                Reference No.,Student Number
                SR20260001,
                SR20260002,
                """), false);

        assertEquals(0, report.applicableRows());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void xlsxUploadsAreAcceptedToo() {
        givenStudent(1, "SR20260001", null, "Ligan", "Sean");

        byte[] xlsx = new StudentNumberExportService()
                .export(StudentNumberExportFormat.XLSX, List.of(
                        new com.example.springboot.dto.registrar.StudentRecordSummaryResponse(
                                1, "SR20260001", "2026-001", "Ligan", "Sean",
                                "B2026A", "CARS", "SEC-A", "Active")))
                .content();

        MultipartFile file = new MockMultipartFile("file", "sheet.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);

        StudentNumberImportReport report = service.apply(file, false);

        assertEquals(StudentNumberImportOutcome.WILL_ASSIGN, outcomeOf(report, 0));
        assertEquals("2026-001", byReference.get("SR20260001").getStudentNumber());
    }
}
