package com.example.springboot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.SoChecklistItem;
import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SoChecklistService;

@WebMvcTest(SoChecklistController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SoChecklistControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private SoChecklistService soChecklistService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void registrarGetsTheChecklist() throws Exception {
        when(soChecklistService.checklist(1)).thenReturn(new SoChecklistResponse("FINAL", true, 1,
                List.of(new SoChecklistItem("psa", "PSA Birth Certificate", "MET", "On file"))));

        mvc.perform(get("/api/registrar/student-records/1/so-checklist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("FINAL"))
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.warningCount").value(1))
                .andExpect(jsonPath("$.items[0].key").value("psa"))
                .andExpect(jsonPath("$.items[0].state").value("MET"));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void anUnknownRecordIs404() throws Exception {
        when(soChecklistService.checklist(999))
                .thenThrow(new NoSuchElementException("Student record not found: 999"));

        mvc.perform(get("/api/registrar/student-records/999/so-checklist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void aTrainerIsForbidden() throws Exception {
        mvc.perform(get("/api/registrar/student-records/1/so-checklist"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get("/api/registrar/student-records/1/so-checklist"))
                .andExpect(status().isUnauthorized());
    }
}
