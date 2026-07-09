package com.example.springboot.controller;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.Document;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.DocumentGenerationService;
import com.example.springboot.service.DocumentService;
import com.example.springboot.service.SystemLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class DocumentControllerWebMvcTest {

    private static final String TOR_TYPE = "Transcript of Records (TOR)";

    @Autowired private MockMvc mvc;
    @MockitoBean private DocumentService documentService;
    @MockitoBean private DocumentGenerationService documentGenerationService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private DocumentSummaryResponse sampleSummary() {
        return new DocumentSummaryResponse(1, "SR20260001", "Dela Cruz", "Maria",
                TOR_TYPE, "tor-scan.pdf", "application/pdf", 9, LocalDateTime.now());
    }

    private Document sampleDocument() {
        StudentRecord student = new StudentRecord();
        student.setStudentId("SR20260001");
        Document doc = new Document();
        doc.setDocumentId(1);
        doc.setStudent(student);
        doc.setDocumentType(TOR_TYPE);
        doc.setFileName("tor-scan.pdf");
        doc.setFileType("application/pdf");
        doc.setContentData("pdf-bytes".getBytes(StandardCharsets.UTF_8));
        doc.setFileSize(9);
        return doc;
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void listReturnsDocumentsForRegistrar() throws Exception {
        when(documentService.getDocuments(any(), any(), any(), any()))
                .thenReturn(List.of(sampleSummary()));

        mvc.perform(get("/api/registrar/documents").param("q", "Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value("SR20260001"))
                .andExpect(jsonPath("$[0].fileName").value("tor-scan.pdf"));
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void listForbiddenForTrainer() throws Exception {
        mvc.perform(get("/api/registrar/documents"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUnauthorizedWhenAnonymous() throws Exception {
        mvc.perform(get("/api/registrar/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void typesReturnsList() throws Exception {
        when(documentService.getDocumentTypes()).thenReturn(List.of(TOR_TYPE));
        mvc.perform(get("/api/registrar/documents/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(TOR_TYPE));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void uploadCreatesDocumentAndLogs() throws Exception {
        when(documentService.upload(eq("SR20260001"), eq(TOR_TYPE), any()))
                .thenReturn(sampleSummary());

        var file = new MockMultipartFile("file", "tor-scan.pdf", "application/pdf",
                "pdf-bytes".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/registrar/documents").file(file)
                        .param("studentId", "SR20260001")
                        .param("documentType", TOR_TYPE)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("tor-scan.pdf"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Uploaded document"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void uploadReturns400WhenServiceRejects() throws Exception {
        when(documentService.upload(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Unsupported file type. Allowed: pdf, docx, xlsx"));

        var file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "x".getBytes());

        mvc.perform(multipart("/api/registrar/documents").file(file)
                        .param("studentId", "SR20260001")
                        .param("documentType", TOR_TYPE)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported file type")));

        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void downloadReturnsAttachmentAndLogs() throws Exception {
        when(documentService.getDocument(1)).thenReturn(sampleDocument());

        mvc.perform(get("/api/registrar/documents/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("pdf-bytes".getBytes(StandardCharsets.UTF_8)));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Downloaded document"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void viewReturnsInlineWithoutLogging() throws Exception {
        when(documentService.getDocument(1)).thenReturn(sampleDocument());

        mvc.perform(get("/api/registrar/documents/1/view"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("inline")));

        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void generateCreatesDocumentAndLogs() throws Exception {
        when(documentService.saveGenerated(eq("SR20260001"), eq(TOR_TYPE),
                eq("SR20260001-TOR.html"), any()))
                .thenReturn(new DocumentSummaryResponse(2, "SR20260001", "Dela Cruz", "Maria",
                        TOR_TYPE, "SR20260001-TOR.html", "text/html", 40, LocalDateTime.now()));

        mvc.perform(post("/api/registrar/documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"SR20260001","documentType":"Transcript of Records (TOR)",
                                 "fileName":"SR20260001-TOR.html","html":"<html><body>doc</body></html>"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType").value("text/html"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Generated document"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void generateRejectsBlankFields() throws Exception {
        mvc.perform(post("/api/registrar/documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"","documentType":"","fileName":"","html":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void generateDataReturnsAggregatedPayload() throws Exception {
        when(documentGenerationService.getGenerateData("SR20260001"))
                .thenReturn(new com.example.springboot.dto.registrar.DocumentGenerateDataResponse(
                        new com.example.springboot.dto.registrar.DocumentGenerateDataResponse.StudentPart(
                                "SR20260001", "Dela Cruz", "Maria", null, null, null, null,
                                null, null, null, null, "Active",
                                null, null, null, null, null, null),
                        List.of(), List.of(), List.of(), null, List.of()));

        mvc.perform(get("/api/registrar/documents/generate-data/SR20260001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.studentId").value("SR20260001"));
    }
}