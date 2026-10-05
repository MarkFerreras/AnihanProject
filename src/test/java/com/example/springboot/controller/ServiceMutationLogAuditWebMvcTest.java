package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.RequestBuilder;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.AdminUserResponse;
import com.example.springboot.dto.registrar.ClassResponse;
import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.ClassManagementService;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.DocumentExportService;
import com.example.springboot.service.DocumentFolderService;
import com.example.springboot.service.DocumentGenerationService;
import com.example.springboot.service.DocumentService;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.SystemLogService;
import com.example.springboot.service.TrainerGradeService;

/**
 * ISO 25010 characteristic: Security (accountability / non-repudiation) - every state-changing
 * endpoint of the Admin, Registrar, Class-management, Trainer-grade and Document controllers
 * writes exactly one {@code system_logs} row with the documented action text, and a rejected
 * request writes none.
 */
@WebMvcTest({ AdminController.class, RegistrarController.class, ClassManagementController.class,
        TrainerGradeController.class, DocumentController.class })
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ServiceMutationLogAuditWebMvcTest {

    private static final String TOR_TYPE = "Transcript of Records (TOR)";

    @Autowired private MockMvc mvc;
    @MockitoBean private AdminService adminService;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private ClassManagementService classManagementService;
    @MockitoBean private TrainerGradeService trainerGradeService;
    @MockitoBean private DocumentService documentService;
    @MockitoBean private DocumentFolderService documentFolderService;
    @MockitoBean private DocumentExportService documentExportService;
    @MockitoBean private DocumentGenerationService documentGenerationService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    // ---- fixtures --------------------------------------------------------------------------

    private static RequestPostProcessor as(String username, String role) {
        return user(username).roles(role);
    }

    private static AdminUserResponse adminUser(int id, String username, String role) {
        return new AdminUserResponse(id, username, username + "@anihan.local", role, "Last", "First", "Mid",
                30, LocalDate.of(1995, 1, 1), true, null, false);
    }

    private static StudentRecordDetailsResponse details(String status) {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", null, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null, status,
                null, List.of(), List.of(), null, null, null, null,
                null, null);
    }

    private static ClassResponse classResponse() {
        return new ClassResponse(1, "SEC-A", "Section A", "CK-101", "Basic Cookery",
                10, "Dela Cruz, Juan", "2026", null, 0L);
    }

    private static final String NEW_USER_JSON = """
            {"username":"newuser","password":"password123","role":"ROLE_TRAINER","lastName":"L",
             "firstName":"F","middleName":"M","email":"n@x.com","birthdate":"1990-01-01"}""";
    private static final String UPDATE_USER_JSON = """
            {"username":"newuser","email":"n@x.com","role":"ROLE_TRAINER","lastName":"L",
             "firstName":"F","middleName":"M","birthdate":"1990-01-01"}""";

    /** Arranges the mocks for one case and returns the request to perform. */
    private RequestBuilder arrange(String caseName) {
        switch (caseName) {
            case "admin-create":
                when(adminService.createUser(any())).thenReturn(adminUser(5, "newuser", "ROLE_TRAINER"));
                return post("/api/admin/users").with(as("admin", "ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_USER_JSON);
            case "admin-update":
                when(adminService.getUserById(5)).thenReturn(adminUser(5, "newuser", "ROLE_TRAINER"));
                when(adminService.updateUser(eq(5), any(), anyString()))
                        .thenReturn(adminUser(5, "newuser", "ROLE_TRAINER"));
                return put("/api/admin/users/5").with(as("admin", "ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_USER_JSON);
            case "admin-disable":
                when(adminService.getUserById(5)).thenReturn(adminUser(5, "newuser", "ROLE_TRAINER"));
                when(adminService.softDeleteUser(eq(5), anyString())).thenReturn(0);
                return delete("/api/admin/users/5").with(as("admin", "ADMIN")).with(csrf());
            case "registrar-status":
                when(registrarService.getRecordById(1)).thenReturn(details("Enrolling"));
                when(registrarService.updateStatus(eq(1), eq("Active"), isNull(), isNull()))
                        .thenReturn(details("Active"));
                return put("/api/registrar/student-records/1/status").with(as("registrar", "REGISTRAR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Active\"}");
            case "registrar-batch":
                when(registrarService.assignBatch(eq(1), isNull())).thenReturn(details("Active"));
                return put("/api/registrar/student-records/1/batch").with(as("registrar", "REGISTRAR"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}");
            case "registrar-delete":
                when(registrarService.getRecordById(1)).thenReturn(details("Active"));
                return delete("/api/registrar/student-records/1").with(as("registrar", "REGISTRAR")).with(csrf());
            case "class-create":
                when(classManagementService.createClass(any())).thenReturn(classResponse());
                return post("/api/registrar/classes").with(as("registrar", "REGISTRAR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionCode\":\"SEC-A\",\"subjectCode\":\"CK-101\",\"semester\":\"2026\"}");
            case "class-reassign-trainer":
                when(classManagementService.updateClassTrainer(eq(1), any())).thenReturn(classResponse());
                return put("/api/registrar/classes/1/trainer").with(as("registrar", "REGISTRAR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"trainerId\":10}");
            case "class-enroll":
                return post("/api/registrar/classes/enroll").with(as("registrar", "REGISTRAR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":1,\"studentId\":\"SR20260001\"}");
            case "class-unenroll":
                return delete("/api/registrar/enrollments/5").with(as("registrar", "REGISTRAR")).with(csrf());
            case "trainer-lock":
                return post("/api/trainer/classes/1/grades/lock").with(as("trainer", "TRAINER")).with(csrf());
            case "trainer-unlock":
                return post("/api/trainer/classes/1/grades/unlock").with(as("trainer", "TRAINER")).with(csrf());
            case "document-upload":
                when(documentService.upload(eq("SR20260001"), eq(TOR_TYPE), any())).thenReturn(summary());
                return multipart("/api/registrar/documents")
                        .file(new MockMultipartFile("file", "tor-scan.pdf", "application/pdf", new byte[] {1, 2}))
                        .param("studentId", "SR20260001").param("documentType", TOR_TYPE)
                        .with(as("registrar", "REGISTRAR")).with(csrf());
            case "document-delete":
                when(documentService.delete(1)).thenReturn(summary());
                return delete("/api/registrar/documents/1").with(as("registrar", "REGISTRAR")).with(csrf());
            default:
                throw new IllegalArgumentException(caseName);
        }
    }

    private static DocumentSummaryResponse summary() {
        return new DocumentSummaryResponse(1, "SR20260001", "Lipata", "Maria", TOR_TYPE,
                "tor-scan.pdf", "application/pdf", 2, LocalDateTime.now());
    }

    private static String expectedAction(String caseName) {
        return switch (caseName) {
            case "admin-create" -> "Created new account: newuser (ROLE_TRAINER)";
            case "admin-update" -> "Updated user details for: newuser";
            case "admin-disable" -> "Deactivated account: newuser";
            case "registrar-status" -> "Changed status of Lipata, Maria from Enrolling to Active";
            case "registrar-batch" -> "Cleared batch for: Lipata, Maria";
            case "registrar-delete" -> "Deleted student record: Lipata, Maria (ID: SR20260001)";
            case "class-create" -> "Created class: Basic Cookery in Section A (Semester 2026)";
            case "class-reassign-trainer" ->
                    "Assigned trainer Dela Cruz, Juan to class #1 (CK-101 / SEC-A)";
            case "class-enroll" -> "Enrolled student SR20260001 in class #1";
            case "class-unenroll" -> "Removed enrollment #5";
            case "trainer-lock" -> "Locked grades for class #1";
            case "trainer-unlock" -> "Unlocked grades for class #1";
            case "document-upload" ->
                    "Uploaded document 'tor-scan.pdf' (" + TOR_TYPE + ") for student SR20260001";
            case "document-delete" ->
                    "Deleted document 'tor-scan.pdf' (" + TOR_TYPE + ") of student SR20260001";
            default -> throw new IllegalArgumentException(caseName);
        };
    }

    // ---- T12-15 ----------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} writes exactly one log row")
    @ValueSource(strings = {
            "admin-create", "admin-update", "admin-disable",
            "registrar-status", "registrar-batch", "registrar-delete",
            "class-create", "class-reassign-trainer", "class-enroll", "class-unenroll",
            "trainer-lock", "trainer-unlock",
            "document-upload", "document-delete" })
    void everyStateChangingServiceMethodWritesExactlyOneLog(String caseName) throws Exception {
        mvc.perform(arrange(caseName)).andExpect(status().is2xxSuccessful());

        verify(systemLogService, times(1)).logAction(any(), anyString(), anyString(),
                eq(expectedAction(caseName)), any());
        verifyNoMoreInteractions(systemLogService);
    }

    @Test
    void adminUpdateWithPasswordResetWritesTwoSeparateRowsByDesign() throws Exception {
        // Documented exception to "one row": a password reset is logged as its own action.
        when(adminService.getUserById(5)).thenReturn(adminUser(5, "newuser", "ROLE_TRAINER"));
        when(adminService.updateUser(eq(5), any(), anyString())).thenReturn(adminUser(5, "newuser", "ROLE_TRAINER"));

        mvc.perform(put("/api/admin/users/5").with(as("admin", "ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"newuser","email":"n@x.com","role":"ROLE_TRAINER","lastName":"L",
                         "firstName":"F","middleName":"M","birthdate":"1990-01-01","password":"newpassword1"}"""))
                .andExpect(status().isOk());

        verify(systemLogService).logAction(any(), anyString(), anyString(),
                eq("Updated user details for: newuser"), any());
        verify(systemLogService).logAction(any(), anyString(), anyString(),
                eq("Reset password for: newuser"), any());
        verifyNoMoreInteractions(systemLogService);
    }

    @Test
    void rejectedMutationsWriteNoLogRow() throws Exception {
        when(adminService.createUser(any())).thenThrow(new IllegalArgumentException("Username is already taken"));
        mvc.perform(post("/api/admin/users").with(as("admin", "ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(NEW_USER_JSON))
                .andExpect(status().is4xxClientError());

        when(classManagementService.createClass(any())).thenThrow(
                new IllegalArgumentException("A class for this section, subject, and semester already exists."));
        mvc.perform(post("/api/registrar/classes").with(as("registrar", "REGISTRAR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sectionCode\":\"SEC-A\",\"subjectCode\":\"CK-101\",\"semester\":\"2026\"}"))
                .andExpect(status().is4xxClientError());

        doThrow(new IllegalArgumentException("Grades are already locked"))
                .when(trainerGradeService).lockGrades(anyInt());
        mvc.perform(post("/api/trainer/classes/1/grades/lock").with(as("trainer", "TRAINER")).with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(systemLogService);
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }
}
