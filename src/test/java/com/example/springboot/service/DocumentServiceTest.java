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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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