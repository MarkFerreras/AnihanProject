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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void getStudentsInSectionReturnsList() throws Exception {
        when(classManagementService.getStudentsInSection("SEC-A")).thenReturn(java.util.List.of(
                new com.example.springboot.dto.registrar.SectionStudentResponse(
                        "S0001", "Cruz", "Ana", "M", "Active")));

        mvc.perform(get("/api/registrar/sections/SEC-A/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value("S0001"));
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void getEligibleStudentsForSectionPassesFilters() throws Exception {
        when(classManagementService.getEligibleStudentsForSection("B2026", "CARS"))
                .thenReturn(java.util.List.of());

        mvc.perform(get("/api/registrar/sections/eligible-students")
                        .param("batchCode", "B2026")
                        .param("courseCode", "CARS"))
                .andExpect(status().isOk());

        verify(classManagementService).getEligibleStudentsForSection("B2026", "CARS");
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void postAssignStudentsLogsAndReturnsSummary() throws Exception {
        com.example.springboot.dto.registrar.SectionAssignmentResultResponse stub =
                new com.example.springboot.dto.registrar.SectionAssignmentResultResponse(
                        2, java.util.List.of(), java.util.List.of());
        when(classManagementService.assignStudentsToSection(eq("SEC-A"), any())).thenReturn(stub);

        mvc.perform(post("/api/registrar/sections/SEC-A/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"S0001\",\"S0002\"]}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedCount").value(2));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Assigned 2 students to section SEC-A"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void deleteStudentFromSectionLogsCascade() throws Exception {
        when(classManagementService.removeStudentFromSection("SEC-A", "S0001")).thenReturn(2);

        mvc.perform(delete("/api/registrar/sections/SEC-A/students/S0001")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Removed student S0001 from section SEC-A (cascaded 2 class enrollment(s))"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void postEnrollSectionIntoClassLogsSummary() throws Exception {
        com.example.springboot.dto.registrar.BulkEnrollSectionResponse stub =
                new com.example.springboot.dto.registrar.BulkEnrollSectionResponse(23, 5, 2, 30);
        when(classManagementService.bulkEnrollSectionIntoClass(42)).thenReturn(stub);

        mvc.perform(post("/api/registrar/classes/42/enroll-section")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCount").value(23))
                .andExpect(jsonPath("$.skippedAlreadyEnrolled").value(5))
                .andExpect(jsonPath("$.skippedIneligible").value(2));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                contains("Bulk-enrolled section into class #42: 23 enrolled, 5 already enrolled, 2 ineligible"),
                any());
    }
}
