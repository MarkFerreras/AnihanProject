package com.example.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

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
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.SystemLogService;

/** Validation of the SO-checklist fields on {@code PUT /api/registrar/student-records/{id}}. */
@WebMvcTest(RegistrarController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class RegistrarRecordUpdateControllerWebMvcTest {

    private static final String BODY_START =
            "{\"studentId\":\"SR20260001\",\"lastName\":\"Lipata\",\"firstName\":\"Maria\"";

    @Autowired private MockMvc mvc;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private StudentRecordDetailsResponse details() {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", null, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, LocalDate.of(2025, 6, 2), "Active",
                null, List.of(), List.of(), null, null, null, null,
                null, "Self-employed");
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void forwardsTheEnrollmentDateAndEmploymentStatus() throws Exception {
        when(registrarService.updateRecord(eq(1), any())).thenReturn(details());

        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_START + ",\"enrollmentDate\":\"2025-06-02\","
                                + "\"employmentStatus\":\"Self-employed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("Self-employed"));

        ArgumentCaptor<StudentRecordUpdateRequest> sent = ArgumentCaptor.forClass(StudentRecordUpdateRequest.class);
        verify(registrarService).updateRecord(eq(1), sent.capture());
        assertEquals(LocalDate.of(2025, 6, 2), sent.getValue().enrollmentDate());
        assertEquals("Self-employed", sent.getValue().employmentStatus());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForAnUnknownEmploymentStatus() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_START + ",\"employmentStatus\":\"Abroad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.employmentStatus").exists());

        verify(registrarService, never()).updateRecord(any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForAnEnrollmentDateInTheFuture() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_START + ",\"enrollmentDate\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.enrollmentDate").exists());

        verify(registrarService, never()).updateRecord(any(), any());
    }
}
