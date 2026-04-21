package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.AccountService;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SystemLogService;

@WebMvcTest(AccountController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AccountControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private SystemLogService systemLogService;

    @MockitoBean
    private UserRepository userRepository;

    // ========== PUT /api/account/profile ==========

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateProfileSucceeds() throws Exception {
        User user = buildUser(1, "admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        doNothing().when(accountService).updateUsername(eq("admin"), eq("newAdmin"), eq("password123"));

        mockMvc.perform(put("/api/account/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newAdmin",
                                  "currentPassword": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Username updated successfully"))
                .andExpect(jsonPath("$.username").value("newAdmin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateProfileRejectsBlankUsername() throws Exception {
        mockMvc.perform(put("/api/account/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "currentPassword": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateProfileRejectsWrongPassword() throws Exception {
        User user = buildUser(1, "admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(accountService).updateUsername(eq("admin"), eq("newAdmin"), eq("wrongPass"));

        mockMvc.perform(put("/api/account/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newAdmin",
                                  "currentPassword": "wrongPass"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    void updateProfileRejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(put("/api/account/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newUser",
                                  "currentPassword": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ========== PUT /api/account/password ==========

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePasswordSucceeds() throws Exception {
        User user = buildUser(1, "admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        doNothing().when(accountService).updatePassword(
                eq("admin"), eq("OldPass1!"), eq("NewPass1!"), eq("NewPass1!"));

        mockMvc.perform(put("/api/account/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPass1!",
                                  "newPassword": "NewPass1!",
                                  "confirmNewPassword": "NewPass1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully. Please log in again."));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatePasswordRejectsWeakNewPassword() throws Exception {
        mockMvc.perform(put("/api/account/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPass1!",
                                  "newPassword": "weak",
                                  "confirmNewPassword": "weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    @Test
    void updatePasswordRejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(put("/api/account/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "OldPass1!",
                                  "newPassword": "NewPass1!",
                                  "confirmNewPassword": "NewPass1!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ========== PUT /api/account/details ==========

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void updateDetailsSucceeds() throws Exception {
        User user = buildUser(3, "trainer", "ROLE_TRAINER");
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));

        User updatedUser = buildUser(3, "trainer", "ROLE_TRAINER");
        updatedUser.setLastName("Dela Cruz");
        updatedUser.setFirstName("Juan");
        updatedUser.setMiddleName("Santos");
        updatedUser.setBirthdate(LocalDate.of(2001, 5, 15));
        updatedUser.setAge(com.example.springboot.service.AgeCalculator.calculateAge(LocalDate.of(2001, 5, 15)));

        when(accountService.updatePersonalDetails(eq("trainer"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/account/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastName": "Dela Cruz",
                                  "firstName": "Juan",
                                  "middleName": "Santos",
                                  "birthdate": "2001-05-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Personal details updated successfully"))
                .andExpect(jsonPath("$.lastName").value("Dela Cruz"))
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.middleName").value("Santos"))
                .andExpect(jsonPath("$.age").isNumber());
    }

    @Test
    void updateDetailsRejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(put("/api/account/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastName": "Test",
                                  "firstName": "User"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ========== Helper ==========

    private User buildUser(Integer userId, String username, String role) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(username + "@anihan.edu");
        user.setRole(role);
        user.setPassword("encoded-password");
        user.setLastName("Test");
        user.setFirstName("User");
        user.setMiddleName("Middle");
        user.setAge(30);
        user.setBirthdate(LocalDate.of(1996, 4, 11));
        user.setEnabled(true);
        return user;
    }
}
