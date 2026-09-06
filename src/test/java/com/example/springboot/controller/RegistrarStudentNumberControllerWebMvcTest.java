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
 * Endpoint tests for {@code PUT /api/registrar/student-records/{id}/student-number} —
 * the single write path for the registrar-controlled student number.
 */
@WebMvcTest(RegistrarController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class RegistrarStudentNumberControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private StudentRecordDetailsResponse details(String studentNumber) {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", studentNumber, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null, "Active",
                null, List.of(), List.of(), null, null, null, null);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void assignsStudentNumberAndLogsIt() throws Exception {
        when(registrarService.assignStudentNumber(eq(1), eq("2026-001")))
                .thenReturn(details("2026-001"));

        mvc.perform(put("/api/registrar/student-records/1/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"2026-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNumber").value("2026-001"))
                .andExpect(jsonPath("$.studentId").value("SR20260001"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Assigned student number 2026-001 to: Lipata, Maria"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void clearingStudentNumberLogsAClearAction() throws Exception {
        when(registrarService.assignStudentNumber(eq(1), eq("")))
                .thenReturn(details(null));

        mvc.perform(put("/api/registrar/student-records/1/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNumber").doesNotExist());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Cleared student number for: Lipata, Maria"), any());
    }

    /** A null body value is the JSON-native way to say "clear it" and must be accepted. */
    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void acceptsNullStudentNumber() throws Exception {
        when(registrarService.assignStudentNumber(eq(1), isNull()))
                .thenReturn(details(null));

        mvc.perform(put("/api/registrar/student-records/1/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400WhenNumberIsAlreadyTaken() throws Exception {
        when(registrarService.assignStudentNumber(eq(1), eq("2026-001")))
                .thenThrow(new IllegalArgumentException(
                        "Student number 2026-001 is already assigned to Reyes, Ana."));

        mvc.perform(put("/api/registrar/student-records/1/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"2026-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Student number 2026-001 is already assigned to Reyes, Ana."));

        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400WhenNumberContainsIllegalCharacters() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"2026 001!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentNumber").exists());

        verify(registrarService, never()).assignStudentNumber(any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400WhenNumberIsTooLong() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"123456789012345678901\"}"))
                .andExpect(status().isBadRequest());

        verify(registrarService, never()).assignStudentNumber(any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns404WhenRecordDoesNotExist() throws Exception {
        when(registrarService.assignStudentNumber(eq(999), any()))
                .thenThrow(new NoSuchElementException("Student record not found: 999"));

        mvc.perform(put("/api/registrar/student-records/999/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"2026-001\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotAssignStudentNumbers() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"2026-001\"}"))
                .andExpect(status().isForbidden());

        verify(registrarService, never()).assignStudentNumber(any(), any());
    }

    @Test
    void anonymousCannotAssignStudentNumbers() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/student-number").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"2026-001\"}"))
                .andExpect(status().isUnauthorized());

        verify(registrarService, never()).assignStudentNumber(any(), any());
    }
}
