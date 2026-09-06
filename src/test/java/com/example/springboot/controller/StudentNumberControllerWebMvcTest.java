package com.example.springboot.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.StudentNumberImportOutcome;
import com.example.springboot.dto.registrar.StudentNumberImportReport;
import com.example.springboot.dto.registrar.StudentNumberImportRowResult;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.StudentNumberExportFormat;
import com.example.springboot.service.StudentNumberExportService;
import com.example.springboot.service.StudentNumberImportService;
import com.example.springboot.service.SystemLogExportFile;
import com.example.springboot.service.SystemLogService;

@WebMvcTest(StudentNumberController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class StudentNumberControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private StudentNumberExportService exportService;
    @MockitoBean private StudentNumberImportService importService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private MockMultipartFile sheet() {
        return new MockMultipartFile("file", "sheet.csv", "text/csv",
                "Reference No.,Student Number\nSR20260001,2026-001\n".getBytes(StandardCharsets.UTF_8));
    }

    private StudentNumberImportReport report(boolean applied, StudentNumberImportOutcome outcome) {
        var row = new StudentNumberImportRowResult(
                2, "SR20260001", "Ligan, Sean", "2026-001", outcome, "Assigning 2026-001.", 1);
        return new StudentNumberImportReport("sheet.csv", applied, 1,
                outcome.applicable() ? 1 : 0, Map.of(outcome, 1), List.of(row));
    }

    // ----- Export -----

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void exportsCsvAsAnAttachmentAndLogsIt() throws Exception {
        when(registrarService.getAllRecords(isNull(), isNull(), isNull(), isNull(), eq(Boolean.FALSE)))
                .thenReturn(List.of(new StudentRecordSummaryResponse(
                        1, "SR20260001", null, "Ligan", "Sean", "B2026A", "CARS", "SEC-A", "Active")));
        when(exportService.export(eq(StudentNumberExportFormat.CSV), any()))
                .thenReturn(new SystemLogExportFile("data".getBytes(StandardCharsets.UTF_8),
                        StudentNumberExportFormat.CSV.mediaType(), "student-numbers-2026-08-27.csv"));

        mvc.perform(get("/api/registrar/student-numbers/export")
                        .param("format", "csv")
                        .param("hasStudentNumber", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("student-numbers-2026-08-27.csv")));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Exported student number sheet"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void exportsXlsxWithTheSpreadsheetContentType() throws Exception {
        when(registrarService.getAllRecords(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(exportService.export(eq(StudentNumberExportFormat.XLSX), any()))
                .thenReturn(new SystemLogExportFile("data".getBytes(StandardCharsets.UTF_8),
                        StudentNumberExportFormat.XLSX.mediaType(), "student-numbers-2026-08-27.xlsx"));

        mvc.perform(get("/api/registrar/student-numbers/export").param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void rejectsAnUnsupportedExportFormat() throws Exception {
        mvc.perform(get("/api/registrar/student-numbers/export").param("format", "docx"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(exportService);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void rejectsAnInvertedYearRange() throws Exception {
        mvc.perform(get("/api/registrar/student-numbers/export")
                        .param("format", "csv")
                        .param("fromYear", "2030")
                        .param("toYear", "2020"))
                .andExpect(status().isBadRequest());
    }

    // ----- Preview -----

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void previewReturnsTheReportAndWritesNoLog() throws Exception {
        when(importService.preview(any(), eq(false)))
                .thenReturn(report(false, StudentNumberImportOutcome.WILL_ASSIGN));

        mvc.perform(multipart("/api/registrar/student-numbers/import/preview")
                        .file(sheet()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.applicableRows").value(1))
                .andExpect(jsonPath("$.rows[0].outcome").value("WILL_ASSIGN"));

        // A preview changes nothing, so it must not pollute the audit trail.
        verifyNoInteractions(systemLogService);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void previewForwardsTheOverwriteFlag() throws Exception {
        when(importService.preview(any(), eq(true)))
                .thenReturn(report(false, StudentNumberImportOutcome.WILL_OVERWRITE));

        mvc.perform(multipart("/api/registrar/student-numbers/import/preview")
                        .file(sheet()).param("allowOverwrite", "true").with(csrf()))
                .andExpect(status().isOk());

        verify(importService).preview(any(), eq(true));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void previewSurfacesAParseFailureAsA400() throws Exception {
        when(importService.preview(any(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("Could not find a header row."));

        mvc.perform(multipart("/api/registrar/student-numbers/import/preview")
                        .file(sheet()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Could not find a header row."));
    }

    // ----- Apply -----

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void applyLogsEachAssignmentAndASummary() throws Exception {
        when(importService.apply(any(), eq(false)))
                .thenReturn(report(true, StudentNumberImportOutcome.WILL_ASSIGN));

        mvc.perform(multipart("/api/registrar/student-numbers/import/apply")
                        .file(sheet()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Assigned student number 2026-001 to: Ligan, Sean (import)"), any());
        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Imported student numbers from sheet.csv: 1 assigned, 0 skipped"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void applyDoesNotLogPerRowForSkippedRows() throws Exception {
        when(importService.apply(any(), eq(false)))
                .thenReturn(report(true, StudentNumberImportOutcome.CONFLICT_IN_USE));

        mvc.perform(multipart("/api/registrar/student-numbers/import/apply")
                        .file(sheet()).with(csrf()))
                .andExpect(status().isOk());

        verify(systemLogService, never()).logAction(any(), any(), any(),
                contains("(import)"), any());
        verify(systemLogService).logAction(any(), any(), any(),
                contains("0 assigned, 1 skipped"), any());
    }

    // ----- RBAC -----

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotExport() throws Exception {
        mvc.perform(get("/api/registrar/student-numbers/export").param("format", "csv"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotApplyAnImport() throws Exception {
        mvc.perform(multipart("/api/registrar/student-numbers/import/apply")
                        .file(sheet()).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(importService);
    }

    @Test
    void anonymousCannotExport() throws Exception {
        mvc.perform(get("/api/registrar/student-numbers/export").param("format", "csv"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotApplyAnImport() throws Exception {
        mvc.perform(multipart("/api/registrar/student-numbers/import/apply")
                        .file(sheet()).with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(importService);
    }
}
