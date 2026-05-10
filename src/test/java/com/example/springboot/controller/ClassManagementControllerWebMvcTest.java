package com.example.springboot.controller;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.ClassResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.ClassManagementService;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SystemLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClassManagementController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ClassManagementControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClassManagementService service;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private ClassResponse buildClassResponse(Integer trainerId, String trainerName) {
        return new ClassResponse(1, "SEC-A", "Section A", "CK-101", "Basic Cookery",
                trainerId, trainerName, "2026", null, 0L);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void putClassTrainerAssignsAndLogs() throws Exception {
        var resp = buildClassResponse(10, "Dela Cruz, Juan");
        when(service.updateClassTrainer(eq(1), any())).thenReturn(resp);

        mvc.perform(put("/api/registrar/classes/1/trainer").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trainerId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerId").value(10))
                .andExpect(jsonPath("$.trainerName").value("Dela Cruz, Juan"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Assigned trainer"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void putClassTrainerUnassignsAndLogs() throws Exception {
        var resp = buildClassResponse(null, null);
        when(service.updateClassTrainer(eq(1), any())).thenReturn(resp);

        mvc.perform(put("/api/registrar/classes/1/trainer").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trainerId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerId").doesNotExist());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Unassigned trainer"), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotUpdateClassTrainer() throws Exception {
        mvc.perform(put("/api/registrar/classes/1/trainer").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trainerId\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void putClassTrainerReturns400WhenServiceThrows() throws Exception {
        when(service.updateClassTrainer(eq(1), any()))
                .thenThrow(new IllegalArgumentException("Trainer account is disabled: jdelacruz"));

        mvc.perform(put("/api/registrar/classes/1/trainer").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trainerId\":10}"))
                .andExpect(status().isBadRequest());
    }
}
