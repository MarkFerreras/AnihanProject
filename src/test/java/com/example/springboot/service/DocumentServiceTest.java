package com.example.springboot.service;

import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.model.Document;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.DocumentRepository;
import com.example.springboot.repository.StudentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    private static final String TOR_TYPE = "Transcript of Records (TOR)";

    @Mock private DocumentRepository documentRepository;
    @Mock private StudentRecordRepository studentRecordRepository;
    @Mock private SystemLogService systemLogService;

    @InjectMocks private DocumentService service;

    private StudentRecord student;

    @BeforeEach
    void setUp() {
        student = new StudentRecord();
        student.setStudentId("SR20260001");
        student.setLastName("Dela Cruz");
        student.setFirstName("Maria");
    }

    @Test
    void documentTypesIncludeAllFourTemplates() {
        var types = service.getDocumentTypes();
        assertTrue(types.contains(TOR_TYPE));
        assertTrue(types.contains("Form IX - Bread and Pastry Production NC II"));
        assertTrue(types.contains("Form IX - Cookery NC II"));
        assertTrue(types.contains("Form IX - Food and Beverage Services NC II"));
    }

    @Test
    void uploadSavesPdfAndReturnsSummary() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        var file = new MockMultipartFile("file", "tor-scan.pdf", "application/pdf",
                "pdf-bytes".getBytes(StandardCharsets.UTF_8));

        DocumentSummaryResponse summary = service.upload("SR20260001", TOR_TYPE, file);

        assertEquals("SR20260001", summary.studentId());
        assertEquals("tor-scan.pdf", summary.fileName());
        assertEquals("application/pdf", summary.fileType());
        assertEquals(TOR_TYPE, summary.documentType());
        assertEquals(9, summary.fileSize());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void uploadRejectsUnknownStudent() {
        when(studentRecordRepository.findByStudentId("NOPE")).thenReturn(Optional.empty());
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("NOPE", TOR_TYPE, file));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadRejectsUnknownDocumentType() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("SR20260001", "Random Type", file));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadRejectsDisallowedExtension() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "x".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("SR20260001", TOR_TYPE, file));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadRejectsFileNameOverTwoHundredFiftyFiveCharacters() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        String longName = "a".repeat(252) + ".pdf"; // 256 chars total
        var file = new MockMultipartFile("file", longName, "application/pdf", "x".getBytes());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.upload("SR20260001", TOR_TYPE, file));
        assertTrue(ex.getMessage().contains("255"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadRejectsFileNameWithControlCharacter() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var file = new MockMultipartFile("file", "a\u0007b.pdf", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("SR20260001", TOR_TYPE, file));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadRejectsFileNameWithReservedWindowsCharacter() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var file = new MockMultipartFile("file", "a:b.pdf", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("SR20260001", TOR_TYPE, file));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadRejectsDotOnlyFileName() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        // A single dot has no ".." substring, so this specifically exercises
        // the trailing-dots-stripped-to-blank check, not the "../" guard.
        var file = new MockMultipartFile("file", ".", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("SR20260001", TOR_TYPE, file));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadStripsABrowserSuppliedPathPrefix() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        var file = new MockMultipartFile("file", "C:\\fakepath\\tor-scan.pdf", "application/pdf",
                "x".getBytes(StandardCharsets.UTF_8));

        DocumentSummaryResponse summary = service.upload("SR20260001", TOR_TYPE, file);

        assertEquals("tor-scan.pdf", summary.fileName());
    }

    @Test
    void uploadRejectsEmptyFile() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.upload("SR20260001", TOR_TYPE, file));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void saveGeneratedStoresHtmlAndAppendsExtension() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentSummaryResponse summary = service.saveGenerated(
                "SR20260001", TOR_TYPE, "SR20260001-TOR", "<html><body>doc</body></html>");

        assertEquals("SR20260001-TOR.html", summary.fileName());
        assertEquals("text/html", summary.fileType());
        assertEquals(TOR_TYPE, summary.documentType());
    }

    @Test
    void saveGeneratedRejectsBlankContent() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveGenerated("SR20260001", TOR_TYPE, "file.html", "  "));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void getDocumentsNormalizesBlankFiltersToNull() {
        service.getDocuments("  ", "", null, "S1");
        verify(documentRepository).searchSummaries(null, null, null, "S1");
    }

    @Test
    void getDocumentThrowsWhenMissing() {
        when(documentRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.getDocument(99));
    }

    // -------------------------------------------------------
    // Exact-reference student document listing (folder explorer)
    // -------------------------------------------------------

    @Test
    void getStudentDocumentsReturnsExactMatchesWhenStudentExists() {
        when(studentRecordRepository.existsByStudentId("SR20260001")).thenReturn(true);
        when(documentRepository.findSummariesByStudentId("SR20260001"))
                .thenReturn(List.of(new DocumentSummaryResponse(1, "SR20260001", "Dela Cruz", "Maria",
                        TOR_TYPE, "tor-scan.pdf", "application/pdf", 9, null)));

        var docs = service.getStudentDocuments("SR20260001");

        assertEquals(1, docs.size());
        assertEquals("SR20260001", docs.get(0).studentId());
    }

    @Test
    void getStudentDocumentsReturnsEmptyListWhenStudentHasNoDocuments() {
        when(studentRecordRepository.existsByStudentId("SR20260001")).thenReturn(true);
        when(documentRepository.findSummariesByStudentId("SR20260001")).thenReturn(List.of());

        assertTrue(service.getStudentDocuments("SR20260001").isEmpty());
    }

    @Test
    void getStudentDocumentsThrowsWhenStudentDoesNotExist() {
        when(studentRecordRepository.existsByStudentId("NOPE")).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> service.getStudentDocuments("NOPE"));
        verify(documentRepository, never()).findSummariesByStudentId(any());
    }

    @Test
    void getStudentDocumentsRejectsBlankStudentId() {
        assertThrows(IllegalArgumentException.class, () -> service.getStudentDocuments("   "));
        verify(studentRecordRepository, never()).existsByStudentId(any());
    }

    @Test
    void getStudentDocumentsDoesNotMatchAPrefixCollidingStudentId() {
        // SR20260001 must not accidentally return documents belonging to SR202600010.
        when(studentRecordRepository.existsByStudentId("SR20260001")).thenReturn(true);
        when(documentRepository.findSummariesByStudentId("SR20260001")).thenReturn(List.of());

        service.getStudentDocuments("SR20260001");

        verify(documentRepository).findSummariesByStudentId("SR20260001");
        verify(documentRepository, never()).findSummariesByStudentId("SR202600010");
    }

    // -------------------------------------------------------
    // ID picture
    // -------------------------------------------------------

    @Test
    void findIdPictureReturnsEmptyWhenStudentHasNone() {
        when(documentRepository.findByStudentStudentIdAndDocumentType(
                "SR20260001", DocumentService.ID_PICTURE_TYPE))
                .thenReturn(Optional.empty());

        assertTrue(service.findIdPicture("SR20260001").isEmpty());
    }

    @Test
    void idPictureTypeIsAKnownDocumentType() {
        assertTrue(service.getDocumentTypes().contains(DocumentService.ID_PICTURE_TYPE));
    }

    @Test
    void uploadIdPictureSavesJpegAndReturnsSummary() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.findByStudentStudentIdAndDocumentType(
                "SR20260001", DocumentService.ID_PICTURE_TYPE)).thenReturn(Optional.empty());
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        var file = new MockMultipartFile("file", "maria-1x1.jpg", "image/jpeg",
                "jpeg-bytes".getBytes(StandardCharsets.UTF_8));

        DocumentSummaryResponse summary = service.uploadIdPicture("SR20260001", file);

        assertEquals("maria-1x1.jpg", summary.fileName());
        assertEquals("image/jpeg", summary.fileType());
        assertEquals(DocumentService.ID_PICTURE_TYPE, summary.documentType());
    }

    @Test
    void uploadIdPictureReplacesTheExistingPictureInPlace() {
        Document existing = new Document();
        existing.setDocumentId(42);
        existing.setStudent(student);
        existing.setDocumentType(DocumentService.ID_PICTURE_TYPE);
        existing.setFileName("old.png");
        existing.setFileType("image/png");
        existing.setContentData("old".getBytes(StandardCharsets.UTF_8));

        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.findByStudentStudentIdAndDocumentType(
                "SR20260001", DocumentService.ID_PICTURE_TYPE)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        var file = new MockMultipartFile("file", "new.jpg", "image/jpeg",
                "new-bytes".getBytes(StandardCharsets.UTF_8));

        DocumentSummaryResponse summary = service.uploadIdPicture("SR20260001", file);

        // Same row reused: no second ID picture accumulates for this student.
        assertEquals(42, summary.documentId());
        assertEquals("new.jpg", summary.fileName());
        verify(documentRepository, never()).delete(any(Document.class));
    }

    @Test
    void uploadIdPictureRejectsNonImageFile() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var file = new MockMultipartFile("file", "scan.pdf", "application/pdf",
                "pdf-bytes".getBytes(StandardCharsets.UTF_8));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.uploadIdPicture("SR20260001", file));
        assertTrue(ex.getMessage().toLowerCase().contains("jpg"));
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void uploadIdPictureRejectsFileOverTwoMegabytes() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        byte[] tooBig = new byte[2 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("file", "huge.png", "image/png", tooBig);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.uploadIdPicture("SR20260001", file));
        assertTrue(ex.getMessage().contains("2MB"));
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void uploadIdPictureRejectsUnknownStudent() {
        when(studentRecordRepository.findByStudentId("NOPE")).thenReturn(Optional.empty());

        var file = new MockMultipartFile("file", "a.jpg", "image/jpeg", "x".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadIdPicture("NOPE", file));
        verify(documentRepository, never()).save(any(Document.class));
    }

    // -------------------------------------------------------
    // Delete
    // -------------------------------------------------------

    @Test
    void deleteRemovesDocumentAndReturnsItsSummary() {
        Document doc = generatedDocument(7, "<html><body>doc</body></html>");
        when(documentRepository.findById(7)).thenReturn(Optional.of(doc));

        DocumentSummaryResponse summary = service.delete(7);

        assertEquals("SR20260001-TOR.html", summary.fileName());
        assertEquals("SR20260001", summary.studentId());
        verify(documentRepository).delete(doc);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(documentRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.delete(99));
        verify(documentRepository, never()).delete(any(Document.class));
    }

    // -------------------------------------------------------
    // Download preparation (generated HTML -> DOCX)
    // -------------------------------------------------------

    @Test
    void prepareDownloadConvertsGeneratedHtmlToDocxWithFriendlyName() throws Exception {
        String html = "<html><body>Generated TOR</body></html>";
        Document doc = generatedDocument(7, html);
        when(documentRepository.findById(7)).thenReturn(Optional.of(doc));

        DocumentService.DownloadPayload payload = service.prepareDownload(7);

        assertEquals("TOR-Dela Cruz Maria.docx", payload.fileName());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                payload.contentType());

        Map<String, byte[]> parts = unzip(payload.content());
        assertArrayEquals(html.getBytes(StandardCharsets.UTF_8), parts.get("word/afchunk.html"));
        assertTrue(new String(parts.get("word/document.xml"), StandardCharsets.UTF_8)
                .contains("altChunk"));
    }

    @Test
    void prepareDownloadKeepsUploadedFilesUnchanged() {
        Document doc = new Document();
        doc.setDocumentId(8);
        doc.setStudent(student);
        doc.setDocumentType("PSA Birth Certificate");
        doc.setFileName("psa.pdf");
        doc.setFileType("application/pdf");
        doc.setContentData("pdf-bytes".getBytes(StandardCharsets.UTF_8));
        doc.setFileSize(9);
        when(documentRepository.findById(8)).thenReturn(Optional.of(doc));

        DocumentService.DownloadPayload payload = service.prepareDownload(8);

        assertEquals("psa.pdf", payload.fileName());
        assertEquals("application/pdf", payload.contentType());
        assertArrayEquals("pdf-bytes".getBytes(StandardCharsets.UTF_8), payload.content());
    }

    // -------------------------------------------------------
    // Re-save (edit) of a generated document
    // -------------------------------------------------------

    @Test
    void saveGeneratedWithDocumentIdUpdatesTheExistingRow() {
        Document existing = generatedDocument(7, "<html><body>v1</body></html>");
        when(documentRepository.findById(7)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentSummaryResponse summary = service.saveGenerated(
                "SR20260001", TOR_TYPE, "SR20260001-TOR.html", "<html><body>v2</body></html>", 7);

        assertEquals("SR20260001-TOR.html", summary.fileName());
        verify(documentRepository).save(existing);
        assertArrayEquals("<html><body>v2</body></html>".getBytes(StandardCharsets.UTF_8),
                existing.getContentData());
    }

    @Test
    void saveGeneratedWithDocumentIdRejectsOverwritingAnUploadedFile() {
        Document uploaded = new Document();
        uploaded.setDocumentId(7);
        uploaded.setStudent(student);
        uploaded.setDocumentType("PSA Birth Certificate");
        uploaded.setFileName("psa.pdf");
        uploaded.setFileType("application/pdf");
        uploaded.setContentData("pdf-bytes".getBytes(StandardCharsets.UTF_8));
        uploaded.setFileSize(9);
        when(documentRepository.findById(7)).thenReturn(Optional.of(uploaded));

        assertThrows(IllegalArgumentException.class, () -> service.saveGenerated(
                "SR20260001", TOR_TYPE, "SR20260001-TOR.html", "<html><body>v2</body></html>", 7));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void saveGeneratedWithDocumentIdRejectsAnotherStudentsDocument() {
        StudentRecord other = new StudentRecord();
        other.setStudentId("SR20260099");
        Document existing = generatedDocument(7, "<html><body>v1</body></html>");
        existing.setStudent(other);
        when(documentRepository.findById(7)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.saveGenerated(
                "SR20260001", TOR_TYPE, "SR20260001-TOR.html", "<html><body>v2</body></html>", 7));
        verify(documentRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // Atomic bulk upload
    // -------------------------------------------------------

    private com.example.springboot.dto.registrar.DocumentAuditContext sampleAudit() {
        return new com.example.springboot.dto.registrar.DocumentAuditContext(
                1, "registrar", "ROLE_REGISTRAR", "127.0.0.1");
    }

    private MultipartFile fakeSizedFile(String name, long size) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.isEmpty()).thenReturn(false);
        lenient().when(file.getSize()).thenReturn(size);
        lenient().when(file.getOriginalFilename()).thenReturn(name);
        return file;
    }

    @Test
    void uploadBatchSavesAllFilesInInputOrderAndWritesOneAuditLog() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        java.util.concurrent.atomic.AtomicInteger idGen = new java.util.concurrent.atomic.AtomicInteger(100);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setDocumentId(idGen.incrementAndGet());
            return d;
        });
        // Return summaries in reverse order to prove the service reorders them.
        when(documentRepository.findSummariesByIds(any())).thenAnswer(inv -> {
            java.util.Collection<Integer> ids = inv.getArgument(0);
            List<DocumentSummaryResponse> out = new java.util.ArrayList<>();
            for (Integer id : ids) {
                out.add(0, new DocumentSummaryResponse(id, "SR20260001", "Dela Cruz", "Maria",
                        TOR_TYPE, "file" + id + ".pdf", "application/pdf", 9, null));
            }
            return out;
        });

        var f1 = new MockMultipartFile("files", "a.pdf", "application/pdf", "aaa".getBytes(StandardCharsets.UTF_8));
        var f2 = new MockMultipartFile("files", "b.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "bbb".getBytes(StandardCharsets.UTF_8));

        List<DocumentSummaryResponse> saved = service.uploadBatch("SR20260001",
                List.of(TOR_TYPE, "PSA Birth Certificate"), List.of(f1, f2), sampleAudit());

        assertEquals(2, saved.size());
        assertEquals(101, saved.get(0).documentId());
        assertEquals(102, saved.get(1).documentId());
        verify(documentRepository, times(2)).save(any(Document.class));
        verify(documentRepository).flush();
        verify(systemLogService).logAction(eq(1), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("2 document"), eq("127.0.0.1"));
    }

    @Test
    void uploadBatchRejectsZeroFiles() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class,
                () -> service.uploadBatch("SR20260001", List.of(), List.of(), sampleAudit()));
        verify(documentRepository, never()).save(any());
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    void uploadBatchRejectsMoreThanTwentyFiles() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        List<MultipartFile> files = new java.util.ArrayList<>();
        List<String> types = new java.util.ArrayList<>();
        for (int i = 0; i < 21; i++) {
            files.add(fakeSizedFile("f" + i + ".pdf", 10));
            types.add(TOR_TYPE);
        }

        assertThrows(IllegalArgumentException.class,
                () -> service.uploadBatch("SR20260001", types, files, sampleAudit()));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchAcceptsExactlyTwentyFiles() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findSummariesByIds(any())).thenReturn(List.of());

        List<MultipartFile> files = new java.util.ArrayList<>();
        List<String> types = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            files.add(new MockMultipartFile("files", "f" + i + ".pdf", "application/pdf",
                    "x".getBytes(StandardCharsets.UTF_8)));
            types.add(TOR_TYPE);
        }

        service.uploadBatch("SR20260001", types, files, sampleAudit());

        verify(documentRepository, times(20)).save(any(Document.class));
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf", "x".getBytes(StandardCharsets.UTF_8));
    }

    private List<Document> captureSavedDocuments() {
        List<Document> saved = new java.util.ArrayList<>();
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            saved.add(d);
            return d;
        });
        when(documentRepository.findSummariesByIds(any())).thenReturn(List.of());
        return saved;
    }

    @Test
    void uploadBatchStoresTrimmedLabelOnOthersAndNullForBlank() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        List<Document> saved = captureSavedDocuments();

        // Mixed batch: the Documents page sends "" for every non-Others row,
        // which must be accepted and stored as null; exactly 100 chars is allowed.
        service.uploadBatch("SR20260001", List.of("Others", "Others", TOR_TYPE, "Others"),
                java.util.Arrays.asList("  Medical Certificate  ", "   ", "", "x".repeat(100)),
                List.of(pdf("a.pdf"), pdf("b.pdf"), pdf("c.pdf"), pdf("d.pdf")), sampleAudit());

        assertEquals("Medical Certificate", saved.get(0).getDocumentLabel());
        assertNull(saved.get(1).getDocumentLabel());
        assertNull(saved.get(2).getDocumentLabel());
        assertEquals("x".repeat(100), saved.get(3).getDocumentLabel());
    }

    @Test
    void uploadBatchWithoutLabelsStoresNullLabel() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        List<Document> saved = captureSavedDocuments();

        service.uploadBatch("SR20260001", List.of("Others"), List.of(pdf("a.pdf")), sampleAudit());

        assertNull(saved.get(0).getDocumentLabel());
    }

    @Test
    void uploadBatchRejectsLabelOnNonOthersType() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of(TOR_TYPE), List.of("My TOR"), List.of(pdf("a.pdf")), sampleAudit()));

        assertTrue(ex.getMessage().contains("'Others'"));
        verify(documentRepository, never()).save(any());
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    void uploadBatchRejectsLabelOverOneHundredCharacters() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of("Others"), List.of("x".repeat(101)), List.of(pdf("a.pdf")), sampleAudit()));

        assertTrue(ex.getMessage().contains("100 characters"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsLabelWithControlCharacters() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of("Others"), List.of("Medical\u0007Certificate"), List.of(pdf("a.pdf")), sampleAudit()));

        assertTrue(ex.getMessage().contains("invalid characters"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsLabelEqualToAKnownTypeIgnoringCase() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of("Others"), List.of("  psa birth certificate "), List.of(pdf("a.pdf")), sampleAudit()));

        assertTrue(ex.getMessage().contains("pick it from the type list"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsLabelCountMismatch() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of("Others"), List.of("A", "B"), List.of(pdf("a.pdf")), sampleAudit()));

        assertEquals("The number of files and document labels must match.", ex.getMessage());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsFileOverTenMegabytes() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        MultipartFile huge = fakeSizedFile("huge.pdf", 10L * 1024 * 1024 + 1);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of(TOR_TYPE), List.of(huge), sampleAudit()));
        assertTrue(ex.getMessage().contains("10MB"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsCombinedSizeOverFiftyMegabytes() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        // Six files at 9MiB each = 54MiB combined; every file is individually under 10MiB.
        List<MultipartFile> files = new java.util.ArrayList<>();
        List<String> types = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            files.add(fakeSizedFile("f" + i + ".pdf", 9L * 1024 * 1024));
            types.add(TOR_TYPE);
        }

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.uploadBatch("SR20260001", types, files, sampleAudit()));
        assertTrue(ex.getMessage().contains("50MB"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsWhenAnyFileIsEmpty() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var ok = new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes());
        var empty = new MockMultipartFile("files", "b.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of(TOR_TYPE, TOR_TYPE), List.of(ok, empty), sampleAudit()));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsMismatchedFileAndTypeListSizes() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var f1 = new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of(TOR_TYPE, "PSA Birth Certificate"), List.of(f1), sampleAudit()));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsUnknownDocumentType() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var f1 = new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of("Not A Real Type"), List.of(f1), sampleAudit()));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsReservedIdPictureCategory() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var f1 = new MockMultipartFile("files", "a.jpg", "image/jpeg", "x".getBytes());

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of(DocumentService.ID_PICTURE_TYPE), List.of(f1), sampleAudit()));
        assertTrue(ex.getMessage().contains("ID Picture"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsDisallowedExtension() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        var f1 = new MockMultipartFile("files", "malware.exe", "application/octet-stream", "x".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of(TOR_TYPE), List.of(f1), sampleAudit()));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsFileNameOverTwoHundredFiftyFiveCharacters() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        String longName = "a".repeat(252) + ".pdf";
        var f1 = new MockMultipartFile("files", longName, "application/pdf", "x".getBytes());

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of(TOR_TYPE), List.of(f1), sampleAudit()));
        assertTrue(ex.getMessage().contains("255"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsDotOnlyFileName() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        // A single dot has no ".." substring, so this specifically exercises
        // the trailing-dots-stripped-to-blank check, not the "../" guard.
        var f1 = new MockMultipartFile("files", ".", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of(TOR_TYPE), List.of(f1), sampleAudit()));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchStripsABrowserSuppliedPathPrefix() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findSummariesByIds(any())).thenReturn(List.of());
        var f1 = new MockMultipartFile("files", "C:\\fakepath\\tor-scan.pdf", "application/pdf",
                "x".getBytes(StandardCharsets.UTF_8));

        service.uploadBatch("SR20260001", List.of(TOR_TYPE), List.of(f1), sampleAudit());

        var captor = org.mockito.ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertEquals("tor-scan.pdf", captor.getValue().getFileName());
    }

    @Test
    void uploadBatchRejectsUnknownStudentWith404NotFound() {
        when(studentRecordRepository.findByStudentId("NOPE")).thenReturn(Optional.empty());
        var f1 = new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes());

        assertThrows(NoSuchElementException.class, () -> service.uploadBatch(
                "NOPE", List.of(TOR_TYPE), List.of(f1), sampleAudit()));
        verify(documentRepository, never()).save(any());
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    void uploadBatchWrapsFileReadIOExceptionAsUncheckedForRollback() throws IOException {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getSize()).thenReturn(9L);
        when(broken.getOriginalFilename()).thenReturn("broken.pdf");
        when(broken.getBytes()).thenThrow(new IOException("disk failure"));

        assertThrows(java.io.UncheckedIOException.class, () -> service.uploadBatch(
                "SR20260001", List.of(TOR_TYPE), List.of(broken), sampleAudit()));
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    // ----- Task 13b additions -----

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> rejectedUploads() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("exe", "malware.exe",
                        new MockMultipartFile("files", "malware.exe", "application/octet-stream", new byte[] { 1 }),
                        "Unsupported file type"),
                org.junit.jupiter.params.provider.Arguments.of("zero-byte", "empty.pdf",
                        new MockMultipartFile("files", "empty.pdf", "application/pdf", new byte[0]),
                        "empty"),
                org.junit.jupiter.params.provider.Arguments.of("oversized", "big.pdf",
                        oversized("big.pdf"),
                        "10MB"));
    }

    private static MultipartFile oversized(String name) {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.isEmpty()).thenReturn(false);
        lenient().when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        lenient().when(file.getOriginalFilename()).thenReturn(name);
        return file;
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "{0}")
    @org.junit.jupiter.params.provider.MethodSource("rejectedUploads")
    void uploadRejectsDisallowedTypeEmptyAndOversizedFile(String label, String fileName,
                                                          MultipartFile file, String messagePart) {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch(
                "SR20260001", List.of(TOR_TYPE), List.of(file), sampleAudit()));

        assertTrue(ex.getMessage().contains(messagePart), ex.getMessage());
        verify(documentRepository, never()).save(any(Document.class));
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    void uploadSameTypeTwiceReplacesOrRejectsPerRule() {
        // Verified rule: there is no de-duplication by type. A second upload of the
        // same document type simply inserts another row (both are kept).
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        List<Document> saved = new java.util.ArrayList<>();
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return inv.getArgument(0);
        });

        service.upload("SR20260001", TOR_TYPE, pdf("first.pdf"));
        service.upload("SR20260001", TOR_TYPE, pdf("second.pdf"));

        assertEquals(2, saved.size());
        assertEquals(TOR_TYPE, saved.get(0).getDocumentType());
        assertEquals(TOR_TYPE, saved.get(1).getDocumentType());
        verify(documentRepository, never()).delete(any(Document.class));
    }

    @Test
    void customLabelOnlyForOthersAndNeverLogged() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        // Rejected on a non-"Others" type.
        assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of(TOR_TYPE), java.util.Arrays.asList("Secret Label"), List.of(pdf("a.pdf")), sampleAudit()));
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());

        // Accepted on "Others"; the audit text must not leak the label.
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setDocumentId(7);
            return d;
        });
        when(documentRepository.findSummariesByIds(any())).thenReturn(List.of());
        try {
            service.uploadBatch("SR20260001", List.of("Others"), java.util.Arrays.asList("Secret Label"),
                    List.of(pdf("b.pdf")), sampleAudit());
        } catch (RuntimeException ignored) {
            // the summary lookup is stubbed empty; only the audit call matters here
        }

        org.mockito.ArgumentCaptor<String> action = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(systemLogService).logAction(any(), any(), any(), action.capture(), any());
        assertFalse(action.getValue().contains("Secret Label"), action.getValue());
    }

    @Test
    void idPictureNeverInUploadDropdownTypes() throws IOException {
        // /types feeds BOTH dropdowns, so the endpoint list includes the ID picture
        // (needed by the filter). The upload dropdown is narrowed client-side.
        assertTrue(service.getDocumentTypes().contains(DocumentService.ID_PICTURE_TYPE),
                "filter dropdown needs the ID picture type");
        String js = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/static/js/registrar-documents.js"));
        assertTrue(js.contains("documentTypeChoices = types.filter(function (t) { return t !== 'ID Picture (1x1 / 2x2)'; })"),
                "upload choices must exclude the ID picture type");
        // And the server refuses it in batch upload.
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of(DocumentService.ID_PICTURE_TYPE), List.of(pdf("id.pdf")), sampleAudit()));
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Document generatedDocument(int id, String html) {
        Document doc = new Document();
        doc.setDocumentId(id);
        doc.setStudent(student);
        doc.setDocumentType(TOR_TYPE);
        doc.setFileName("SR20260001-TOR.html");
        doc.setFileType("text/html");
        doc.setContentData(html.getBytes(StandardCharsets.UTF_8));
        doc.setFileSize(doc.getContentData().length);
        return doc;
    }

    private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                parts.put(entry.getName(), in.readAllBytes());
            }
        }
        return parts;
    }
}