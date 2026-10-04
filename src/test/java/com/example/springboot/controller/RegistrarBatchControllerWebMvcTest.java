package com.example.springboot.controller;

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

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
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
 * Endpoint tests for {@code PUT /api/registrar/student-records/{id}/batch} —
 * the single write path for manually assigning, changing, or clearing a student's batch.
 */
@WebMvcTest(RegistrarController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class RegistrarBatchControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private StudentRecordDetailsResponse details(String batchCode) {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", null, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                batchCode, null, null, null, "Active",
                null, List.of(), List.of(), null, null, null, null,
                null, null);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void assignsBatchAndLogsIt() throws Exception {
        when(registrarService.assignBatch(eq(1), eq("B2026A")))
                .thenReturn(details("B2026A"));

        mvc.perform(put("/api/registrar/student-records/1/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"B2026A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchCode").value("B2026A"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Assigned batch B2026A to: Lipata, Maria"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void clearingBatchLogsAClearAction() throws Exception {
        when(registrarService.assignBatch(eq(1), eq("")))
                .thenReturn(details(null));

        mvc.perform(put("/api/registrar/student-records/1/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchCode").doesNotExist());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Cleared batch for: Lipata, Maria"), any());
    }

    /** A null body value is the JSON-native way to say "clear it" and must be accepted. */
    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void acceptsNullBatchCode() throws Exception {
        when(registrarService.assignBatch(eq(1), isNull()))
                .thenReturn(details(null));

        mvc.perform(put("/api/registrar/student-records/1/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400WhenSectionInvariantViolated() throws Exception {
        when(registrarService.assignBatch(eq(1), eq("")))
                .thenThrow(new IllegalArgumentException(
                        "Cannot change or clear batch while student is enrolled in section "
                                + "SEC-A (Batch B2026A). Remove the student from the section first."));

        mvc.perform(put("/api/registrar/student-records/1/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Cannot change or clear batch while student is enrolled in section "
                                + "SEC-A (Batch B2026A). Remove the student from the section first."));

        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400WhenCodeExceeds20Chars() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"123456789012345678901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.batchCode").exists());

        verify(registrarService, never()).assignBatch(any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns404WhenRecordDoesNotExist() throws Exception {
        when(registrarService.assignBatch(eq(999), any()))
                .thenThrow(new NoSuchElementException("Student record not found: 999"));

        mvc.perform(put("/api/registrar/student-records/999/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"B2026A\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotAssignBatch() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"B2026A\"}"))
                .andExpect(status().isForbidden());

        verify(registrarService, never()).assignBatch(any(), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotAssignBatch() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"B2026A\"}"))
                .andExpect(status().isForbidden());

        verify(registrarService, never()).assignBatch(any(), any());
    }

    @Test
    void anonymousCannotAssignBatch() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/batch").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"B2026A\"}"))
                .andExpect(status().isUnauthorized());

        verify(registrarService, never()).assignBatch(any(), any());
    }
}
