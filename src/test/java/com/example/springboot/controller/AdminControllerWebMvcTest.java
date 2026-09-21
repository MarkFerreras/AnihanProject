package com.example.springboot.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

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
                                                null,
                                                false)));

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
        void getSingleUserReturns200() throws Exception {
                when(adminService.getUserById(9)).thenReturn(adminUser(9, "trainer"));

                mockMvc.perform(get("/api/admin/users/9"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("trainer"));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getSingleUserReturns404WhenUnknown() throws Exception {
                when(adminService.getUserById(999))
                                .thenThrow(new NoSuchElementException("User not found"));

                mockMvc.perform(get("/api/admin/users/999"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "registrar", roles = "REGISTRAR")
        void getSingleUserRejectsNonAdmin() throws Exception {
                mockMvc.perform(get("/api/admin/users/9"))
                                .andExpect(status().isForbidden());

                verify(adminService, never()).getUserById(any());
        }

        @Test
        void getSingleUserRejectsAnonymous() throws Exception {
                mockMvc.perform(get("/api/admin/users/9"))
                                .andExpect(status().isUnauthorized());

                verify(adminService, never()).getUserById(any());
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
                                                  "birthdate": "2030-01-01"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors.email").exists())
                                .andExpect(jsonPath("$.errors.role").exists());
        }

        private static final String VALID_UPDATE_USER_JSON = """
                        {
                          "email": "registrar@anihan.edu",
                          "role": "ROLE_REGISTRAR",
                          "lastName": "Cruz",
                          "firstName": "Maria",
                          "middleName": "Santos",
                          "birthdate": "1998-03-20"
                        }
                        """;

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void updateUserReturns200AndLogsIt() throws Exception {
                when(adminService.getUserById(2)).thenReturn(adminUser(2, "registrar"));
                when(adminService.updateUser(eq(2), any(), eq("admin")))
                                .thenReturn(adminUser(2, "registrar"));

                mockMvc.perform(put("/api/admin/users/2").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_UPDATE_USER_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("registrar"));

                verify(systemLogService).logAction(any(), any(), any(),
                                contains("Updated user details for: registrar"), any());
                verify(systemLogService, never()).logAction(any(), any(), any(),
                                contains("Reset password"), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void updateUserLogsPasswordResetOnlyWhenProvided() throws Exception {
                when(adminService.getUserById(2)).thenReturn(adminUser(2, "registrar"));
                when(adminService.updateUser(eq(2), any(), eq("admin")))
                                .thenReturn(adminUser(2, "registrar"));

                mockMvc.perform(put("/api/admin/users/2").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "email": "registrar@anihan.edu",
                                                  "role": "ROLE_REGISTRAR",
                                                  "lastName": "Cruz",
                                                  "firstName": "Maria",
                                                  "middleName": "Santos",
                                                  "birthdate": "1998-03-20",
                                                  "password": "NewPass123!"
                                                }
                                                """))
                                .andExpect(status().isOk());

                verify(systemLogService).logAction(any(), any(), any(),
                                contains("Reset password for: registrar"), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void updateUserReturns404WhenUnknown() throws Exception {
                when(adminService.getUserById(999))
                                .thenThrow(new NoSuchElementException("User not found"));

                mockMvc.perform(put("/api/admin/users/999").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_UPDATE_USER_JSON))
                                .andExpect(status().isNotFound());

                verify(adminService, never()).updateUser(any(), any(), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void permanentDeleteBlockedWhenTrainerHasLockedGrades() throws Exception {
                when(adminService.getUserById(9)).thenReturn(adminUser(9, "trainer"));
                when(adminService.hardDeleteUser(eq(9), any())).thenThrow(new IllegalArgumentException(
                                "This trainer has locked grades in 2 class(es). "
                                                + "Deactivate the account instead, or reassign those classes first."));

                mockMvc.perform(delete("/api/admin/users/9/permanent").with(csrf()))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(containsString("locked grades in 2 class(es)")));

                // The block must abort before any audit row is written.
                verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void permanentDeleteOfTrainerReportsUnassignedClassCount() throws Exception {
                when(adminService.getUserById(9)).thenReturn(adminUser(9, "trainer"));
                when(adminService.hardDeleteUser(eq(9), any())).thenReturn(2);

                mockMvc.perform(delete("/api/admin/users/9/permanent").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value(containsString("2 class(es) now have no trainer")));

                verify(systemLogService).logAction(any(), any(), any(),
                                contains("Permanently deleted account: trainer (unassigned from 2 class(es))"), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void softDeleteOfTrainerReportsRemainingClassCount() throws Exception {
                when(adminService.getUserById(9)).thenReturn(adminUser(9, "trainer"));
                when(adminService.softDeleteUser(eq(9), any())).thenReturn(1);

                mockMvc.perform(delete("/api/admin/users/9").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value(containsString("still assigned to 1 class(es)")));

                verify(systemLogService).logAction(any(), any(), any(),
                                contains("Deactivated account: trainer (still trainer-of-record on 1 class(es))"), any());
        }

        // ----- Re-enable account -----

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void reEnableUserReturns200AndLogsIt() throws Exception {
                when(adminService.getUserById(9)).thenReturn(adminUser(9, "trainer"));

                mockMvc.perform(put("/api/admin/users/9/enable").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("User account has been re-enabled."));

                verify(systemLogService).logAction(any(), any(), any(),
                                contains("Re-enabled account: trainer"), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void reEnableUserReturns404WhenUnknown() throws Exception {
                when(adminService.getUserById(999))
                                .thenThrow(new NoSuchElementException("User not found"));

                mockMvc.perform(put("/api/admin/users/999/enable").with(csrf()))
                                .andExpect(status().isNotFound());

                verify(adminService, never()).reEnableUser(any());
        }

        @Test
        @WithMockUser(username = "registrar", roles = "REGISTRAR")
        void reEnableUserRejectsNonAdmin() throws Exception {
                mockMvc.perform(put("/api/admin/users/9/enable").with(csrf()))
                                .andExpect(status().isForbidden());

                verify(adminService, never()).reEnableUser(any());
        }

        @Test
        void reEnableUserRejectsAnonymous() throws Exception {
                mockMvc.perform(put("/api/admin/users/9/enable").with(csrf()))
                                .andExpect(status().isUnauthorized());

                verify(adminService, never()).reEnableUser(any());
        }

        // ----- Unlock account -----

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void unlockUserReturns200AndLogsIt() throws Exception {
                when(adminService.getUserById(9)).thenReturn(adminUser(9, "trainer"));

                mockMvc.perform(put("/api/admin/users/9/unlock").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Account unlocked."));

                verify(systemLogService).logAction(any(), any(), any(),
                                contains("Unlocked account: trainer"), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void unlockUserReturns404WhenUnknown() throws Exception {
                when(adminService.getUserById(999))
                                .thenThrow(new NoSuchElementException("User not found"));

                mockMvc.perform(put("/api/admin/users/999/unlock").with(csrf()))
                                .andExpect(status().isNotFound());

                verify(adminService, never()).unlockUser(any());
        }

        @Test
        @WithMockUser(username = "registrar", roles = "REGISTRAR")
        void unlockUserRejectsNonAdmin() throws Exception {
                mockMvc.perform(put("/api/admin/users/9/unlock").with(csrf()))
                                .andExpect(status().isForbidden());

                verify(adminService, never()).unlockUser(any());
        }

        @Test
        void unlockUserRejectsAnonymous() throws Exception {
                mockMvc.perform(put("/api/admin/users/9/unlock").with(csrf()))
                                .andExpect(status().isUnauthorized());

                verify(adminService, never()).unlockUser(any());
        }

        // ----- Create account -----

        private static final String VALID_CREATE_USER_JSON = """
                        {
                          "username": "newtrainer",
                          "password": "Pass1234!",
                          "role": "ROLE_TRAINER",
                          "lastName": "Cruz",
                          "firstName": "Maria",
                          "middleName": "Santos",
                          "email": "newtrainer@anihan.local",
                          "birthdate": "2000-01-01"
                        }
                        """;

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void createUserReturns201AndLogsIt() throws Exception {
                when(adminService.createUser(any())).thenReturn(adminUser(10, "newtrainer"));

                mockMvc.perform(post("/api/admin/users").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_USER_JSON))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.username").value("newtrainer"));

                verify(systemLogService).logAction(any(), any(), any(),
                                contains("Created new account: newtrainer (ROLE_TRAINER)"), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void createUserRequiresValidPayload() throws Exception {
                mockMvc.perform(post("/api/admin/users").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "username": "",
                                                  "password": "short",
                                                  "role": "ROLE_BOGUS",
                                                  "birthdate": "2030-01-01"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors.username").exists())
                                .andExpect(jsonPath("$.errors.password").exists())
                                .andExpect(jsonPath("$.errors.role").exists())
                                .andExpect(jsonPath("$.errors.birthdate").exists());

                verify(adminService, never()).createUser(any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void createUserRejectsDuplicateUsername() throws Exception {
                when(adminService.createUser(any()))
                                .thenThrow(new IllegalArgumentException("Username is already taken"));

                mockMvc.perform(post("/api/admin/users").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_USER_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Username is already taken"));

                verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void createUserRejectsDuplicateEmail() throws Exception {
                when(adminService.createUser(any()))
                                .thenThrow(new IllegalArgumentException("Email is already taken by another account"));

                mockMvc.perform(post("/api/admin/users").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_USER_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Email is already taken by another account"));

                verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
        }

        @Test
        @WithMockUser(username = "registrar", roles = "REGISTRAR")
        void createUserRejectsNonAdmin() throws Exception {
                mockMvc.perform(post("/api/admin/users").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_USER_JSON))
                                .andExpect(status().isForbidden());

                verify(adminService, never()).createUser(any());
        }

        @Test
        void createUserRejectsAnonymous() throws Exception {
                mockMvc.perform(post("/api/admin/users").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_USER_JSON))
                                .andExpect(status().isUnauthorized());

                verify(adminService, never()).createUser(any());
        }

        private AdminUserResponse adminUser(int id, String username) {
                return new AdminUserResponse(id, username, username + "@anihan.edu", "ROLE_TRAINER",
                                "Cruz", "Maria", "Santos", 30, LocalDate.of(1996, 4, 11), true, null, false);
        }
}
