package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
        return new StudentRecordDetailsResponse(
                1, "SR20260001", null, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null, status,
                null, List.of(), List.of(), null, null, null, null);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void changesStatusAndLogsTheTransition() throws Exception {
        when(registrarService.getRecordById(1)).thenReturn(details("Enrolling"));
        when(registrarService.updateStatus(eq(1), eq("Active"))).thenReturn(details("Active"));

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

        verify(registrarService, never()).updateStatus(any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForABlankStatus() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(registrarService, never()).updateStatus(any(), any());
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

        verify(registrarService, never()).updateStatus(any(), any());
    }

    @Test
    void anonymousCannotChangeStatus() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Active\"}"))
                .andExpect(status().isUnauthorized());

        verify(registrarService, never()).updateStatus(any(), any());
    }
}
