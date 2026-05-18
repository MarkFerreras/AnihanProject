package com.example.springboot.controller;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.trainer.TrainerClassResponse;
import com.example.springboot.dto.trainer.TrainerClassStudentResponse;
import com.example.springboot.dto.trainer.TrainerSubjectResponse;
import com.example.springboot.dto.trainer.TrainerSubjectStudentResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.TrainerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainerController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class TrainerControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private TrainerService service;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void getMySubjectsReturnsListForTrainer() throws Exception {
        var row = new TrainerSubjectResponse(
                "CK-101", "Basic Cookery", "Cookery NC II", 3,
                22L, List.of("Section A", "Section B"),
                List.of("Culinary Arts and Restaurant Services"));
        when(service.getMyAssignedSubjects()).thenReturn(List.of(row));

        mvc.perform(get("/api/trainer/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectCode").value("CK-101"))
                .andExpect(jsonPath("$[0].enrolledCount").value(22))
                .andExpect(jsonPath("$[0].sectionNames.length()").value(2))
                .andExpect(jsonPath("$[0].sectionNames[0]").value("Section A"));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void getMySubjectsReturns403ForNonTrainer() throws Exception {
        mvc.perform(get("/api/trainer/subjects"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMySubjectsReturns401ForAnonymous() throws Exception {
        mvc.perform(get("/api/trainer/subjects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void getStudentsForSubjectReturnsRoster() throws Exception {
        var s = new TrainerSubjectStudentResponse("2026-001", "Cruz", "Maria", "L",
                "CARS-2026-A", "Section A");
        when(service.getStudentsForSubject("CK-101")).thenReturn(List.of(s));

        mvc.perform(get("/api/trainer/subjects/CK-101/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value("2026-001"))
                .andExpect(jsonPath("$[0].sectionName").value("Section A"));
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void getStudentsForSubjectReturns400WhenServiceThrows() throws Exception {
        when(service.getStudentsForSubject("CK-999"))
                .thenThrow(new IllegalArgumentException("You are not assigned to any class for subject: CK-999"));

        mvc.perform(get("/api/trainer/subjects/CK-999/students"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void getMyClassesReturnsList() throws Exception {
        var row = new TrainerClassResponse(2, "CARS-2026-A", "Section A",
                "CK-101", "Basic Cookery",
                "Culinary Arts and Restaurant Services", "2026", 12L);
        when(service.getMyClasses()).thenReturn(List.of(row));

        mvc.perform(get("/api/trainer/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].classId").value(2))
                .andExpect(jsonPath("$[0].semester").value("2026"))
                .andExpect(jsonPath("$[0].enrolledCount").value(12));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getMyClassesReturns403ForNonTrainer() throws Exception {
        mvc.perform(get("/api/trainer/classes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void getStudentsForClassReturnsRoster() throws Exception {
        var s = new TrainerClassStudentResponse("2026-001", "Cruz", "Maria", "L");
        when(service.getStudentsForClass(1)).thenReturn(List.of(s));

        mvc.perform(get("/api/trainer/classes/1/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value("2026-001"))
                .andExpect(jsonPath("$[0].lastName").value("Cruz"));
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void getStudentsForClassReturns400WhenServiceRejects() throws Exception {
        when(service.getStudentsForClass(99))
                .thenThrow(new IllegalArgumentException("You are not assigned to class #99"));

        mvc.perform(get("/api/trainer/classes/99/students"))
                .andExpect(status().isBadRequest());
    }
}
