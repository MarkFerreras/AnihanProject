package com.example.springboot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(AdminController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AdminControllerWebMvcTest {

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
    void getUsersReturnsAdminDataWithoutPassword() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of(
                new AdminUserResponse(
                        1,
                        "admin",
                        "admin@anihan.edu",
                        "ROLE_ADMIN",
                        "Admin",
                        "System",
                        "Owner",
                        30,
                        LocalDate.of(1996, 4, 11),
                        true,
                        null
                )
        ));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].email").value("admin@anihan.edu"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void getUsersRejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUserRequiresValidPayload() throws Exception {
        mockMvc.perform(put("/api/admin/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "role": "",
                                  "lastName": "",
                                  "firstName": "",
                                  "middleName": "",
                                  "age": 0,
                                  "birthdate": "2030-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }
}
