package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.SystemLogService;

/**
 * Endpoint tests for the two core mutating actions on a student record —
 * {@code PUT /api/registrar/student-records/{id}} (general update) and
 * {@code DELETE /api/registrar/student-records/{id}} — neither of which had any
 * controller-level test coverage before this file (only service-level Mockito tests
 * existed in {@code RegistrarBulkLoadTest}).
 */
@WebMvcTest(RegistrarController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class RegistrarRecordCrudControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private StudentRecordDetailsResponse details(String studentId, String lastName, String firstName) {
        return new StudentRecordDetailsResponse(
                1, studentId, null, lastName, firstName, null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null, "Active",
                null, List.of(), List.of(), null, null, null, null);
    }

    private static final String VALID_UPDATE_JSON = """
            {
              "studentId": "SR20260001",
              "lastName": "Reyes",
              "firstName": "Anna",
              "middleName": "Cruz",
              "batchCode": null,
              "courseCode": null,
              "sectionCode": null,
              "ojt": null,
              "tesdaQualifications": [],
              "schoolYears": [],
              "father": null,
              "mother": null,
              "guardian": null
            }
            """;

    // ----- PUT /{recordId} (general update) -----

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void updateRecordReturns200AndLogsIt() throws Exception {
        when(registrarService.updateRecord(eq(1), any()))
                .thenReturn(details("SR20260001", "Reyes", "Anna"));

        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("SR20260001"))
                .andExpect(jsonPath("$.lastName").value("Reyes"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Updated student record: Reyes, Anna (ID: SR20260001)"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void updateRecordRequiresValidPayload() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "",
                                  "lastName": "",
                                  "firstName": "",
                                  "ojt": null,
                                  "tesdaQualifications": [],
                                  "schoolYears": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.studentId").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.firstName").exists());

        verify(registrarService, never()).updateRecord(any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void updateRecordReturns404WhenUnknown() throws Exception {
        when(registrarService.updateRecord(eq(999), any()))
                .thenThrow(new NoSuchElementException("Student record not found: 999"));

        mvc.perform(put("/api/registrar/student-records/999").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void updateRecordSurfacesServiceRejectionAsBadRequest() throws Exception {
        when(registrarService.updateRecord(eq(1), any()))
                .thenThrow(new IllegalArgumentException("Student ID cannot be changed."));

        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Student ID cannot be changed."));

        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void updateRecordRejectsNonRegistrar() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isForbidden());

        verify(registrarService, never()).updateRecord(any(), any());
    }

    @Test
    void updateRecordRejectsAnonymous() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isUnauthorized());

        verify(registrarService, never()).updateRecord(any(), any());
    }

    // ----- DELETE /{recordId} -----

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void deleteRecordReturns204AndLogsIt() throws Exception {
        when(registrarService.getRecordById(1)).thenReturn(details("SR20260001", "Reyes", "Anna"));

        mvc.perform(delete("/api/registrar/student-records/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(registrarService).deleteRecord(1);
        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Deleted student record: Reyes, Anna (ID: SR20260001)"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void deleteRecordReturns404WhenUnknown() throws Exception {
        when(registrarService.getRecordById(999))
                .thenThrow(new NoSuchElementException("Student record not found: 999"));

        mvc.perform(delete("/api/registrar/student-records/999").with(csrf()))
                .andExpect(status().isNotFound());

        verify(registrarService, never()).deleteRecord(any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void deleteRecordRejectsNonRegistrar() throws Exception {
        mvc.perform(delete("/api/registrar/student-records/1").with(csrf()))
                .andExpect(status().isForbidden());

        verify(registrarService, never()).deleteRecord(any());
    }

    @Test
    void deleteRecordRejectsAnonymous() throws Exception {
        mvc.perform(delete("/api/registrar/student-records/1").with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(registrarService, never()).deleteRecord(any());
    }
}
