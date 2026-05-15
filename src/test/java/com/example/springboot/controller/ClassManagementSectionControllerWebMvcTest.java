package com.example.springboot.controller;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.SectionResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClassManagementController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ClassManagementSectionControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClassManagementService classManagementService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void putSectionUpdatesNameAndLogs() throws Exception {
        SectionResponse stub = new SectionResponse("SEC-A", "Section A - Morning",
                "B2026", (short) 2026, "CARS", "Culinary Arts");
        when(classManagementService.updateSection(eq("SEC-A"), any())).thenReturn(stub);

        mvc.perform(put("/api/registrar/sections/SEC-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionName\":\"Section A - Morning\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionName").value("Section A - Morning"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Updated section"), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = {"TRAINER"})
    void trainerCannotUpdateSection() throws Exception {
        mvc.perform(put("/api/registrar/sections/SEC-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionName\":\"X\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
