package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ========== Default behavior (no params → 7-day default) ==========

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getLogsDefaultsToSevenDays() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 10, 30, 0);
        when(systemLogService.getLogs(isNull(), isNull(), isNull())).thenReturn(List.of(
                new SystemLogResponse(1, 1, "admin", "ROLE_ADMIN", "Logged in", "127.0.0.1", now)
        ));

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].action").value("Logged in"));
    }

    // ========== Preset range filters ==========

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getLogsWithRangeDays14() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 4, 16, 10, 30, 0);
        when(systemLogService.getLogs(eq(14), isNull(), isNull())).thenReturn(List.of(
                new SystemLogResponse(1, 1, "admin", "ROLE_ADMIN", "Logged in", "127.0.0.1", now),
                new SystemLogResponse(2, 2, "registrar", "ROLE_REGISTRAR", "Viewed records", "10.0.0.1", now)
        ));

        mockMvc.perform(get("/api/logs").param("rangeDays", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[1].username").value("registrar"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getLogsWithRangeDays30() throws Exception {
        when(systemLogService.getLogs(eq(30), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/logs").param("rangeDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ========== Custom date range ==========

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getLogsWithCustomDateRange() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 4, 10, 8, 0, 0);
        when(systemLogService.getLogs(isNull(),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 18)))).thenReturn(List.of(
                new SystemLogResponse(5, 1, "admin", "ROLE_ADMIN", "Custom range log", "127.0.0.1", now)
        ));

        mockMvc.perform(get("/api/logs")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("Custom range log"));
    }

    // ========== Invalid range → 400 ==========

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getLogsInvalidRangeReturns400() throws Exception {
        when(systemLogService.getLogs(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("startDate must not be after endDate"));

        mockMvc.perform(get("/api/logs")
                        .param("startDate", "2026-04-18")
                        .param("endDate", "2026-04-01"))
                .andExpect(status().isBadRequest());
    }

    // ========== Empty result ==========

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getLogsReturnsEmptyListWhenNoLogs() throws Exception {
        when(systemLogService.getLogs(isNull(), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ========== Security tests (unchanged) ==========

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
