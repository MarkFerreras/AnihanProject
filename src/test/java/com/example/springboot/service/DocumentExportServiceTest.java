package com.example.springboot.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.DocumentExportScope;
import com.example.springboot.exception.EmptyDocumentExportException;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.DocumentContentRepository;
import com.example.springboot.repository.DocumentFolderRepository;
import com.example.springboot.repository.DocumentFolderRepository.ExportRow;
import com.example.springboot.dto.registrar.ExportCheckResponse;
import com.example.springboot.repository.DocumentFolderRepository.CheckRow;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExportServiceTest {

    @Mock private DocumentFolderRepository documentFolderRepository;
    @Mock private DocumentContentRepository documentContentRepository;
    @Mock private StudentRecordRepository studentRecordRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private BatchRepository batchRepository;

    @InjectMocks private DocumentExportService service;

    // -------------------------------------------------------
    // Scope existence / empty export
    // -------------------------------------------------------

    @Test
    void prepareExportThrowsNotFoundWhenStudentUnknown() {
        when(studentRecordRepository.existsByStudentId("NOPE")).thenReturn(false);
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.prepareExport(DocumentExportScope.STUDENT, "NOPE"));
    }

    @Test
    void prepareExportThrowsNotFoundWhenSectionUnknown() {
        when(sectionRepository.existsById("NOPE")).thenReturn(false);
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.prepareExport(DocumentExportScope.SECTION, "NOPE"));
    }

    @Test
    void prepareExportThrowsNotFoundWhenBatchUnknownForUnassignedScope() {
        when(batchRepository.existsById("NOPE")).thenReturn(false);
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.prepareExport(DocumentExportScope.UNASSIGNED, "NOPE"));
    }

    @Test
    void prepareExportThrowsNotFoundWhenBatchUnknownForBatchScope() {
        when(batchRepository.existsById("NOPE")).thenReturn(false);
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.prepareExport(DocumentExportScope.BATCH, "NOPE"));
    }

    @Test
    void prepareExportThrowsEmptyDocumentExportExceptionWhenZeroDocuments() {
        when(studentRecordRepository.existsByStudentId("SR20260001")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.STUDENT, "SR20260001"))
                .thenReturn(List.of());

        assertThrows(EmptyDocumentExportException.class,
                () -> service.prepareExport(DocumentExportScope.STUDENT, "SR20260001"));
        verify(documentFolderRepository, never()).findCheckRows(any(), any());
    }

    // -------------------------------------------------------
    // ZIP filename per scope
    // -------------------------------------------------------

    @Test
    void prepareExportBuildsStudentFilenameFromNumberOrIdAndLastName() {
        when(studentRecordRepository.existsByStudentId("SR20260001")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.STUDENT, "SR20260001"))
                .thenReturn(List.of(row(1, "SR20260001", "12-345", "Maria", "Dela Cruz", null, "a.pdf")));

        var prepared = service.prepareExport(DocumentExportScope.STUDENT, "SR20260001");

        assertEquals("Documents_12-345_Dela Cruz.zip", prepared.fileName());
    }

    @Test
    void prepareExportFallsBackToStudentIdWhenStudentNumberAbsent() {
        when(studentRecordRepository.existsByStudentId("SR20260001")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.STUDENT, "SR20260001"))
                .thenReturn(List.of(row(1, "SR20260001", null, "Maria", "Dela Cruz", null, "a.pdf")));

        var prepared = service.prepareExport(DocumentExportScope.STUDENT, "SR20260001");

        assertEquals("Documents_SR20260001_Dela Cruz.zip", prepared.fileName());
    }

    @Test
    void prepareExportBuildsSectionFilename() {
        when(sectionRepository.existsById("S1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.SECTION, "S1"))
                .thenReturn(List.of(row(1, "SR1", null, "A", "One", "S1", "a.pdf")));

        assertEquals("Documents_Section_S1.zip",
                service.prepareExport(DocumentExportScope.SECTION, "S1").fileName());
    }

    @Test
    void prepareExportBuildsUnassignedFilename() {
        when(batchRepository.existsById("B2026A")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.UNASSIGNED, "B2026A"))
                .thenReturn(List.of(row(1, "SR1", null, "A", "One", null, "a.pdf")));

        assertEquals("Documents_Batch_B2026A_Unassigned.zip",
                service.prepareExport(DocumentExportScope.UNASSIGNED, "B2026A").fileName());
    }

    @Test
    void prepareExportBuildsBatchFilename() {
        when(batchRepository.existsById("B2026A")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.BATCH, "B2026A"))
                .thenReturn(List.of(row(1, "SR1", null, "A", "One", "S1", "a.pdf")));

        assertEquals("Documents_Batch_B2026A.zip",
                service.prepareExport(DocumentExportScope.BATCH, "B2026A").fileName());
    }

    // -------------------------------------------------------
    // Entry layout per scope
    // -------------------------------------------------------

    @Test
    void studentScopeEntriesAreFlatFileNamesAtRoot() {
        when(studentRecordRepository.existsByStudentId("SR1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.STUDENT, "SR1")).thenReturn(List.of(
                row(1, "SR1", null, "A", "One", null, "tor.pdf"),
                row(2, "SR1", null, "A", "One", null, "psa.pdf")));

        var entries = service.prepareExport(DocumentExportScope.STUDENT, "SR1").entries();

        assertEquals(2, entries.size());
        assertEquals("tor.pdf", entries.get(0).entryName());
        assertEquals("psa.pdf", entries.get(1).entryName());
    }

    @Test
    void sectionScopeEntriesUseStudentFolder() {
        when(sectionRepository.existsById("S1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.SECTION, "S1")).thenReturn(List.of(
                row(1, "SR1", "12-345", "Maria", "Dela Cruz", "S1", "tor.pdf"),
                row(2, "SR1", "12-345", "Maria", "Dela Cruz", "S1", "psa.pdf")));

        var entries = service.prepareExport(DocumentExportScope.SECTION, "S1").entries();

        String expectedFolder = "Dela Cruz_Maria_12-345_SR1";
        assertEquals(expectedFolder + "/tor.pdf", entries.get(0).entryName());
        assertEquals(expectedFolder + "/psa.pdf", entries.get(1).entryName());
    }

    @Test
    void batchScopeGroupsAssignedStudentsUnderSectionsAndOthersUnderUnassigned() {
        when(batchRepository.existsById("B2026A")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.BATCH, "B2026A")).thenReturn(List.of(
                row(1, "SR1", null, "Maria", "Dela Cruz", "S1", "a.pdf"),
                row(2, "SR2", null, "Juan", "Santos", null, "b.pdf")));

        var entries = service.prepareExport(DocumentExportScope.BATCH, "B2026A").entries();

        assertEquals("Sections/S1/Dela Cruz_Maria_SR1_SR1/a.pdf", entries.get(0).entryName());
        assertEquals("Unassigned/Santos_Juan_SR2_SR2/b.pdf", entries.get(1).entryName());
    }

    // -------------------------------------------------------
    // Name safety / uniqueness
    // -------------------------------------------------------

    @Test
    void duplicateFileNamesWithinTheSameStudentFolderGetSuffixed() {
        when(sectionRepository.existsById("S1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.SECTION, "S1")).thenReturn(List.of(
                row(1, "SR1", null, "A", "One", "S1", "scan.pdf"),
                row(2, "SR1", null, "A", "One", "S1", "scan.pdf"),
                row(3, "SR1", null, "A", "One", "S1", "SCAN.PDF")));

        var entries = service.prepareExport(DocumentExportScope.SECTION, "S1").entries();

        String folder = entries.get(0).entryName().substring(0, entries.get(0).entryName().indexOf('/'));
        assertEquals(folder + "/scan.pdf", entries.get(0).entryName());
        assertEquals(folder + "/scan (1).pdf", entries.get(1).entryName());
        assertEquals(folder + "/SCAN (2).PDF", entries.get(2).entryName());
    }

    @Test
    void studentFolderNamesTruncatedToTheSameLengthGetSuffixedInsteadOfColliding() {
        when(batchRepository.existsById("B2026A")).thenReturn(true);
        String longLastName = "A".repeat(150);
        when(documentFolderRepository.findExportRows(DocumentExportScope.UNASSIGNED, "B2026A")).thenReturn(List.of(
                row(1, "SR-AAAA-0001", null, "First", longLastName, null, "a.pdf"),
                row(2, "SR-AAAA-0002", null, "First", longLastName, null, "b.pdf")));

        var entries = service.prepareExport(DocumentExportScope.UNASSIGNED, "B2026A").entries();

        String folder1 = entries.get(0).entryName().substring(0, entries.get(0).entryName().indexOf('/'));
        String folder2 = entries.get(1).entryName().substring(0, entries.get(1).entryName().indexOf('/'));
        assertFalse(folder1.equals(folder2), "truncated-to-identical folder names must still be disambiguated");
    }

    @Test
    void unsafeAndControlCharactersAreStrippedFromFileNames() {
        when(studentRecordRepository.existsByStudentId("SR1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.STUDENT, "SR1")).thenReturn(List.of(
                row(1, "SR1", null, "A", "One", null, "..\\evil<>:\"|?*name.pdf")));

        var entries = service.prepareExport(DocumentExportScope.STUDENT, "SR1").entries();

        String name = entries.get(0).entryName();
        assertFalse(name.contains(".."));
        assertFalse(name.contains("\\"));
        assertFalse(name.contains(":"));
        assertFalse(name.contains("<"));
        assertFalse(name.contains("|"));
        assertTrue(name.endsWith(".pdf"));
    }

    @Test
    void windowsReservedDeviceNamesAreRenamed() {
        when(studentRecordRepository.existsByStudentId("SR1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.STUDENT, "SR1")).thenReturn(List.of(
                row(1, "SR1", null, "A", "One", null, "CON.pdf")));

        String name = service.prepareExport(DocumentExportScope.STUDENT, "SR1").entries().get(0).entryName();

        assertFalse(name.equalsIgnoreCase("CON.pdf"));
        assertTrue(name.toLowerCase().endsWith("con.pdf"));
    }

    @Test
    void longFileNamesAreCappedAtOneHundredTwentyCharsPreservingExtension() {
        when(studentRecordRepository.existsByStudentId("SR1")).thenReturn(true);
        String longName = "b".repeat(200) + ".pdf";
        when(documentFolderRepository.findExportRows(DocumentExportScope.STUDENT, "SR1")).thenReturn(List.of(
                row(1, "SR1", null, "A", "One", null, longName)));

        String name = service.prepareExport(DocumentExportScope.STUDENT, "SR1").entries().get(0).entryName();

        assertTrue(name.length() <= 120);
        assertTrue(name.endsWith(".pdf"));
    }

    @Test
    void blankFileNameFallsBackToADocumentIdBasedName() {
        when(studentRecordRepository.existsByStudentId("SR1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.STUDENT, "SR1")).thenReturn(List.of(
                row(42, "SR1", null, "A", "One", null, "   ")));

        String name = service.prepareExport(DocumentExportScope.STUDENT, "SR1").entries().get(0).entryName();

        assertEquals("document-42", name);
    }

    // -------------------------------------------------------
    // writeZip streaming
    // -------------------------------------------------------

    @Test
    void writeZipStreamsEachDocumentAndProducesAValidArchiveOnSuccess() throws IOException {
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(1);
            out.write("first-content".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(documentContentRepository).copyTo(eq(1), any());
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(1);
            out.write("second-content".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(documentContentRepository).copyTo(eq(2), any());

        var prepared = new DocumentExportService.PreparedExport("Documents_test.zip", List.of(
                new DocumentExportService.ExportEntry(1, "a.pdf"),
                new DocumentExportService.ExportEntry(2, "folder/b.pdf")), 0, 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.writeZip(prepared, out);

        var contents = unzip(out.toByteArray());
        assertEquals(2, contents.size());
        assertEquals("first-content", new String(contents.get("a.pdf"), StandardCharsets.UTF_8));
        assertEquals("second-content", new String(contents.get("folder/b.pdf"), StandardCharsets.UTF_8));
    }

    @Test
    void writeZipPropagatesFailureWithoutProducingAValidArchive() throws IOException {
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(1);
            out.write("ok".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(documentContentRepository).copyTo(eq(1), any());
        doThrow(new IOException("disk failure")).when(documentContentRepository).copyTo(eq(2), any());

        var prepared = new DocumentExportService.PreparedExport("Documents_test.zip", List.of(
                new DocumentExportService.ExportEntry(1, "a.pdf"),
                new DocumentExportService.ExportEntry(2, "b.pdf")), 0, 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(IOException.class, () -> service.writeZip(prepared, out));

        // The trailer (End Of Central Directory signature) must never be
        // written when an entry fails — that would be a misleading "success".
        byte[] bytes = out.toByteArray();
        assertFalse(containsEocdSignature(bytes), "an aborted export must not produce a complete ZIP trailer");
    }

    // -------------------------------------------------------
    // Pre-export missing-documents check
    // -------------------------------------------------------

    @Test
    void checkMissingGroupsRowsPerStudentAndCountsDocuments() {
        when(sectionRepository.existsById("S1")).thenReturn(true);
        when(documentFolderRepository.findCheckRows(DocumentExportScope.SECTION, "S1")).thenReturn(List.of(
                check("SR1", "Active", 1, "PSA Birth Certificate"),
                check("SR1", "Active", 2, "Form 137"),
                check("SR2", "Graduated", 3, "PSA Birth Certificate"),
                check("SR2", "Graduated", 4, "Form 137"),
                check("SR2", "Graduated", 5, "ID Picture (1x1 / 2x2)"),
                check("SR2", "Graduated", 6, "Form IX - Cookery NC II")));

        ExportCheckResponse result = service.checkMissing(DocumentExportScope.SECTION, "S1");

        assertEquals(2, result.studentsInScope());
        assertEquals(2, result.flagged().size());
        var first = result.flagged().get(0);
        assertEquals("SR1", first.studentId());
        assertEquals(2, first.documentCount());
        assertEquals(List.of("ID Picture (1x1 / 2x2)"), first.missing());
        var second = result.flagged().get(1);
        assertEquals("SR2", second.studentId());
        assertEquals(4, second.documentCount());
        assertEquals(List.of("Transcript of Records (TOR)", "OJT Report", "Certificate of TVET Program"),
                second.missing());
    }

    @Test
    void checkMissingFlagsAStudentWithZeroDocuments() {
        when(batchRepository.existsById("B2026A")).thenReturn(true);
        when(documentFolderRepository.findCheckRows(DocumentExportScope.UNASSIGNED, "B2026A"))
                .thenReturn(List.of(check("SR9", "Enrolling", null, null)));

        ExportCheckResponse result = service.checkMissing(DocumentExportScope.UNASSIGNED, "B2026A");

        assertEquals(1, result.studentsInScope());
        assertEquals(0, result.flagged().get(0).documentCount());
        assertEquals(List.of("PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)"),
                result.flagged().get(0).missing());
    }

    @Test
    void checkMissingReturnsNoFlaggedStudentsWhenEveryoneIsComplete() {
        when(studentRecordRepository.existsByStudentId("SR1")).thenReturn(true);
        when(documentFolderRepository.findCheckRows(DocumentExportScope.STUDENT, "SR1")).thenReturn(List.of(
                check("SR1", "Active", 1, "PSA Birth Certificate"),
                check("SR1", "Active", 2, "Form 137"),
                check("SR1", "Active", 3, "ID Picture (1x1 / 2x2)")));

        ExportCheckResponse result = service.checkMissing(DocumentExportScope.STUDENT, "SR1");

        assertEquals(1, result.studentsInScope());
        assertTrue(result.flagged().isEmpty());
    }

    @Test
    void checkMissingThrowsNotFoundWhenSectionUnknown() {
        when(sectionRepository.existsById("NOPE")).thenReturn(false);
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.checkMissing(DocumentExportScope.SECTION, "NOPE"));
    }

    @Test
    void prepareExportCarriesStudentsInScopeAndFlaggedCount() {
        when(sectionRepository.existsById("S1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.SECTION, "S1"))
                .thenReturn(List.of(row(1, "SR1", null, "A", "One", "S1", "psa.pdf")));
        when(documentFolderRepository.findCheckRows(DocumentExportScope.SECTION, "S1")).thenReturn(List.of(
                check("SR1", "Active", 1, "PSA Birth Certificate"),
                check("SR2", "Active", null, null)));

        var prepared = service.prepareExport(DocumentExportScope.SECTION, "S1");

        assertEquals(2, prepared.studentsInScope());
        assertEquals(2, prepared.flaggedCount());
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private static CheckRow check(String studentId, String status, Integer documentId, String documentType) {
        return new CheckRow(studentId, null, "First", "Last" + studentId, status, null, documentId, documentType);
    }

    private static ExportRow row(int documentId, String studentId, String studentNumber,
                                 String firstName, String lastName, String sectionCode, String fileName) {
        return new ExportRow(documentId, studentId, studentNumber, firstName, lastName, sectionCode, fileName);
    }

    private static java.util.Map<String, byte[]> unzip(byte[] zip) throws IOException {
        java.util.Map<String, byte[]> out = new java.util.HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                out.put(entry.getName(), in.readAllBytes());
            }
        }
        return out;
    }

    private static boolean containsEocdSignature(byte[] bytes) {
        byte[] sig = { 0x50, 0x4B, 0x05, 0x06 };
        outer:
        for (int i = 0; i <= bytes.length - sig.length; i++) {
            for (int j = 0; j < sig.length; j++) {
                if (bytes[i + j] != sig[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
