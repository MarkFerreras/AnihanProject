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

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Optional;

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
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

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
}