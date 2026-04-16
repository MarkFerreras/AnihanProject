package com.example.springboot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.SystemLogResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SystemLogService;

import java.time.LocalDateTime;

@WebMvcTest(SystemLogController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SystemLogControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemLogService systemLogService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getLogsReturnsOkForAdmin() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 10, 30, 0);
        when(systemLogService.getAllLogs()).thenReturn(List.of(
                new SystemLogResponse(1, 1, "admin", "ROLE_ADMIN", "Logged in", "127.0.0.1", now),
                new SystemLogResponse(2, 2, "registrar", "ROLE_REGISTRAR", "Viewed records", "10.0.0.1", now)
        ));

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].action").value("Logged in"))
                .andExpect(jsonPath("$[1].username").value("registrar"))
                .andExpect(jsonPath("$[1].action").value("Viewed records"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getLogsReturnsEmptyListWhenNoLogs() throws Exception {
        when(systemLogService.getAllLogs()).thenReturn(List.of());

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void getLogsRejectNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void getLogsRejectTrainerUser() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLogsRejectUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isUnauthorized());
    }
}
