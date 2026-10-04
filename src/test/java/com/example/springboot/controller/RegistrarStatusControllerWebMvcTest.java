package com.example.springboot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.SystemLogService;

/**
 * Endpoint tests for {@code PUT /api/registrar/student-records/{id}/status} — the sole write
 * path for a student's enrollment status, pulled out of the general update endpoint so a
 * status change is always deliberate and separately audited.
 */
@WebMvcTest(RegistrarController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class RegistrarStatusControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private StudentRecordDetailsResponse details(String status) {
        return details(status, null);
    }

    private StudentRecordDetailsResponse details(String status, LocalDate completionDate) {
        return details(status, completionDate, "Lipata");
    }

    private StudentRecordDetailsResponse details(String status, LocalDate completionDate, String lastName) {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", null, lastName, "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null, status,
                null, List.of(), List.of(), null, null, null, null,
                completionDate, null);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void changesStatusAndLogsTheTransition() throws Exception {
        when(registrarService.getRecordById(1)).thenReturn(details("Enrolling"));
        when(registrarService.updateStatus(eq(1), eq("Active"), isNull(), isNull()))
                .thenReturn(details("Active"));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Active\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentStatus").value("Active"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Changed status of Lipata, Maria from Enrolling to Active"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForAnUnknownStatusValue() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Submitted\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentStatus").exists());

        verify(registrarService, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForABlankStatus() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(registrarService, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns404WhenRecordDoesNotExist() throws Exception {
        when(registrarService.getRecordById(999))
                .thenThrow(new NoSuchElementException("Student record not found: 999"));

        mvc.perform(put("/api/registrar/student-records/999/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Active\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotChangeStatus() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Active\"}"))
                .andExpect(status().isForbidden());

        verify(registrarService, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void anonymousCannotChangeStatus() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Active\"}"))
                .andExpect(status().isUnauthorized());

        verify(registrarService, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void completingAStudentForwardsTheDateAndLogsIt() throws Exception {
        LocalDate done = LocalDate.of(2026, 9, 30);
        when(registrarService.getRecordById(1)).thenReturn(details("Active"));
        when(registrarService.updateStatus(eq(1), eq("Completed"), eq(done), isNull()))
                .thenReturn(details("Completed", done));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Completed\",\"completionDate\":\"2026-09-30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionDate").value("2026-09-30"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Changed status of Lipata, Maria from Active to Completed (completion date 2026-09-30)"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void archiveGraduationLogsTheDateAndTheTrimmedReason() throws Exception {
        LocalDate done = LocalDate.of(2014, 3, 15);
        when(registrarService.getRecordById(1)).thenReturn(details("Active"));
        when(registrarService.updateStatus(eq(1), eq("Graduated"), eq(done), eq("  Digitized archive record  ")))
                .thenReturn(details("Graduated", done));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Graduated\",\"completionDate\":\"2014-03-15\","
                                + "\"reason\":\"  Digitized archive record  \"}"))
                .andExpect(status().isOk());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Changed status of Lipata, Maria from Active to Graduated "
                        + "(completion date 2014-03-15; reason: Digitized archive record)"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void movingBackToActiveLogsTheClearedDate() throws Exception {
        when(registrarService.getRecordById(1)).thenReturn(details("Completed", LocalDate.of(2026, 9, 30)));
        when(registrarService.updateStatus(eq(1), eq("Active"), isNull(), isNull())).thenReturn(details("Active"));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Active\"}"))
                .andExpect(status().isOk());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Changed status of Lipata, Maria from Completed to Active (cleared completion date 2026-09-30)"),
                any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void graduatedBackToCompletedLogsTheReasonOnly() throws Exception {
        LocalDate done = LocalDate.of(2014, 3, 15);
        when(registrarService.getRecordById(1)).thenReturn(details("Graduated", done));
        when(registrarService.updateStatus(eq(1), eq("Completed"), isNull(), eq("Wrong batch on the SO")))
                .thenReturn(details("Completed", done));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Completed\",\"reason\":\"Wrong batch on the SO\"}"))
                .andExpect(status().isOk());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Changed status of Lipata, Maria from Graduated to Completed (reason: Wrong batch on the SO)"),
                any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void truncatesTheAuditLineToTheSystemLogsColumnWidth() throws Exception {
        // system_logs.action is VARCHAR(500); an over-long name + reason must not fail the insert.
        String longName = "L".repeat(300);
        String reason = "r".repeat(255);
        LocalDate done = LocalDate.of(2014, 3, 15);
        when(registrarService.getRecordById(1)).thenReturn(details("Graduated", done, longName));
        when(registrarService.updateStatus(eq(1), eq("Completed"), isNull(), eq(reason)))
                .thenReturn(details("Completed", done, longName));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Completed\",\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> action = ArgumentCaptor.forClass(String.class);
        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"), action.capture(), any());
        assertThat(action.getValue()).hasSize(500).endsWith("...")
                .startsWith("Changed status of " + longName);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForAReasonLongerThan255Characters()throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Graduated\",\"completionDate\":\"2014-03-15\",\"reason\":\""
                                + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());

        verify(registrarService, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400WithTheRuleMessageWhenTheMoveIsNotAllowed() throws Exception {
        when(registrarService.getRecordById(1)).thenReturn(details("Enrolling"));
        when(registrarService.updateStatus(eq(1), eq("Completed"), any(), any()))
                .thenThrow(new IllegalArgumentException("Only an Active student can be marked Completed."));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Completed\",\"completionDate\":\"2026-09-30\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only an Active student can be marked Completed."));

        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }
}
