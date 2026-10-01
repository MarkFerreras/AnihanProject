package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.CourseCodePreview;
import com.example.springboot.dto.registrar.SectionResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.ClassManagementService;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SectionCreationResult;
import com.example.springboot.service.SystemLogService;

@WebMvcTest(ClassManagementController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ClassManagementCourseControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClassManagementService classManagementService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private static final SectionResponse CREATED = new SectionResponse(
            "SEC-A", "Section A", "B2027A", (short) 2027, "BPP", "Bread and Pastry Production");
    private static final String BODY = "{\"sectionCode\":\"SEC-A\",\"sectionName\":\"Section A\","
            + "\"batchCode\":\"B2027A\",\"course\":\"Bread and Pastry Production\"}";

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void createSectionLogsTheAutoCreatedBatchAndCourse() throws Exception {
        when(classManagementService.createSection(any())).thenReturn(new SectionCreationResult(CREATED, true, true));

        mvc.perform(post("/api/registrar/sections").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseCode").value("BPP"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Created batch: B2027A"), any());
        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Created course: Bread and Pastry Production (BPP)"), any());
        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Created section"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void createSectionWithExistingBatchAndCourseLogsOnlyTheSection() throws Exception {
        when(classManagementService.createSection(any())).thenReturn(new SectionCreationResult(CREATED, false, false));

        mvc.perform(post("/api/registrar/sections").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY).with(csrf()))
                .andExpect(status().isCreated());

        verify(systemLogService, times(1)).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void createSectionRejectsAMissingCourse() throws Exception {
        mvc.perform(post("/api/registrar/sections").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionCode\":\"SEC-A\",\"sectionName\":\"Section A\",\"batchCode\":\"B2027A\"}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
        verify(classManagementService, never()).createSection(any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void createSectionRejectsASectionNameLongerThanTheColumn() throws Exception {
        mvc.perform(post("/api/registrar/sections").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionCode\":\"SEC-A\",\"sectionName\":\"Section A Morning Batch 2027 X\","
                                + "\"batchCode\":\"B2027A\",\"course\":\"CARS\"}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void previewCourseCodeReturnsTheCode() throws Exception {
        when(classManagementService.previewCourseCode("Bread and Pastry Production"))
                .thenReturn(new CourseCodePreview("BPP", false));

        mvc.perform(get("/api/registrar/courses/preview-code").param("name", "Bread and Pastry Production"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BPP"))
                .andExpect(jsonPath("$.existing").value(false));
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = {"TRAINER"})
    void trainerCannotPreviewCourseCodes() throws Exception {
        mvc.perform(get("/api/registrar/courses/preview-code").param("name", "X"))
                .andExpect(status().isForbidden());
    }
}
