package com.example.springboot.controller;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.SubjectResponse;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClassManagementController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ClassManagementSubjectControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClassManagementService service;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void postSubjectsCreatesAndLogs() throws Exception {
        var resp = new SubjectResponse("CK-101", "Basic Cookery", "Cookery NC II", 3, null, null);
        when(service.createSubject(any())).thenReturn(resp);

        mvc.perform(post("/api/registrar/subjects").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"subjectCode":"CK-101","subjectName":"Basic Cookery","qualificationCode":1,"units":3}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectCode").value("CK-101"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Created subject"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void postSubjectsRejectsBlankCode() throws Exception {
        mvc.perform(post("/api/registrar/subjects").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"subjectCode":"","subjectName":"Basic Cookery","qualificationCode":1,"units":3}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void putSubjectUpdatesAndLogs() throws Exception {
        var resp = new SubjectResponse("CK-101", "Updated", "Cookery NC II", 4, null, null);
        when(service.updateSubject(eq("CK-101"), any())).thenReturn(resp);

        mvc.perform(put("/api/registrar/subjects/CK-101").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"subjectName":"Updated","qualificationCode":1,"units":4}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectName").value("Updated"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Updated subject"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void deleteSubjectReturnsNoContentAndLogs() throws Exception {
        doNothing().when(service).deleteSubject("CK-101");

        mvc.perform(delete("/api/registrar/subjects/CK-101").with(csrf()))
                .andExpect(status().isNoContent());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Deleted subject"), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotCreateSubject() throws Exception {
        mvc.perform(post("/api/registrar/subjects").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void getQualificationsReturnsList() throws Exception {
        when(service.getAllQualifications()).thenReturn(List.of());
        mvc.perform(get("/api/registrar/qualifications"))
                .andExpect(status().isOk());
    }
}
