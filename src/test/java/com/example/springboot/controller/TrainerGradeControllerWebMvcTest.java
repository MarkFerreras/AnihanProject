package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
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
import com.example.springboot.dto.trainer.GradeSummaryResponse;
import com.example.springboot.dto.trainer.StudentGradeRow;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SystemLogService;
import com.example.springboot.service.TrainerGradeService;

@WebMvcTest(TrainerGradeController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
public class TrainerGradeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainerGradeService gradeService;

    @MockitoBean
    private SystemLogService systemLogService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final String BASE = "/api/trainer/classes/1/grades";

    @Test
    @WithMockUser(roles = "TRAINER")
    void getGradesReturnsTheSummary() throws Exception {
        GradeSummaryResponse response = new GradeSummaryResponse(
                1, "Culinary Arts", "Section A", "2026", false,
                List.of(new StudentGradeRow(
                        "STU001", "Doe", "Jane", "M",
                        new BigDecimal("88"), new BigDecimal("2.00"), null,
                        null, null, "COMPETENT", new BigDecimal("40"), false)));
        when(gradeService.getGradesForClass(1)).thenReturn(response);

        mockMvc.perform(get(BASE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(1))
                .andExpect(jsonPath("$.students.length()").value(1))
                .andExpect(jsonPath("$.students[0].finalGrade").value(2.00))
                .andExpect(jsonPath("$.students[0].remarks").value("COMPETENT"));
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    void getGradesBadRequestOnServiceReject() throws Exception {
        when(gradeService.getGradesForClass(1))
                .thenThrow(new IllegalArgumentException("You are not assigned to this class"));
        mockMvc.perform(get(BASE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void saveGradesOkAndLogged() throws Exception {
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(userWithId(12)));
        String payload = "[{\"studentId\":\"STU001\",\"finalPercentage\":88,\"gradeStatus\":null,"
                + "\"reExamPercentage\":null,\"hoursRendered\":40}]";

        mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        verify(gradeService).saveGrades(eq(1), anyList());
        verify(systemLogService).logAction(any(), any(), any(), contains("Saved grades for class #1"), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void saveGradesBadRequestWhenServiceRejects() throws Exception {
        doThrow(new IllegalArgumentException("Grade is locked and cannot be modified: STU001"))
                .when(gradeService).saveGrades(eq(1), anyList());
        String payload = "[{\"studentId\":\"STU001\",\"finalPercentage\":88,\"hoursRendered\":40}]";

        mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void lockAndUnlockOk() throws Exception {
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(userWithId(12)));

        mockMvc.perform(post(BASE + "/lock").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/unlock").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(gradeService).lockGrades(1);
        verify(gradeService).unlockGrades(1);
    }

    @Test
    void getGradesUnauthorizedWithoutSession() throws Exception {
        mockMvc.perform(get(BASE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "REGISTRAR")
    void getGradesForbiddenForNonTrainer() throws Exception {
        mockMvc.perform(get(BASE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private static User userWithId(int id) {
        User u = new User();
        u.setUserId(id);
        u.setUsername("trainer");
        return u;
    }
}
