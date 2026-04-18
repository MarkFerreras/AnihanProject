package com.example.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.AdminUserResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SystemLogService;

/**
 * Bulk load WebMvc tests for the Admin "View All Users" endpoint.
 *
 * Verifies that GET /api/admin/users can serialize 100 users as JSON
 * and return them through the HTTP layer with correct status and content.
 *
 * All user data is generated programmatically in memory via Mockito
 * mocks. No real database is touched and no hard-coded dummy users
 * are persisted anywhere.
 */
@WebMvcTest(AdminController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AdminBulkLoadWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private SystemLogService systemLogService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUsersReturnsOneHundredUsersAsJson() throws Exception {
        when(adminService.getAllUsers()).thenReturn(buildUserResponseList(100));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100))
                .andExpect(jsonPath("$[0].username").value("user_0"))
                .andExpect(jsonPath("$[99].username").value("user_99"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[50].password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUsersOneHundredCompletesWithinPerformanceBound() throws Exception {
        when(adminService.getAllUsers()).thenReturn(buildUserResponseList(100));

        long startMs = System.currentTimeMillis();
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100));
        long elapsedMs = System.currentTimeMillis() - startMs;

        // Non-functional requirement: record retrieval < 5 seconds
        assertTrue(elapsedMs < 5000,
                "Serializing 100 users via /api/admin/users took " + elapsedMs
                        + "ms — exceeds 5-second limit");
    }

    /**
     * Generates a list of AdminUserResponse DTOs for bulk testing.
     * All data lives only in memory — nothing is persisted to any database.
     */
    private List<AdminUserResponse> buildUserResponseList(int count) {
        String[] roles = {"ROLE_ADMIN", "ROLE_REGISTRAR", "ROLE_TRAINER"};
        List<AdminUserResponse> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new AdminUserResponse(
                    i + 1,
                    "user_" + i,
                    "user_" + i + "@anihan.local",
                    roles[i % roles.length],
                    "Last_" + i,
                    "First_" + i,
                    "Middle_" + i,
                    20 + (i % 40),
                    LocalDate.of(2000, 1, 1).plusDays(i),
                    true,
                    null
            ));
        }
        return list;
    }
}
