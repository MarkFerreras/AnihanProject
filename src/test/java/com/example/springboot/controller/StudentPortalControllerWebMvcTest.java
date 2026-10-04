package com.example.springboot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;

/** The public portal's name pre-check (spec 2026-10-01 SO checklist §4.5). */
@WebMvcTest(StudentPortalController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class StudentPortalControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentRecordRepository studentRecordRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private void existingStudentWithStatus(String status) {
        StudentRecord record = new StudentRecord();
        record.setStudentStatus(status);
        when(studentRecordRepository.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                "Dela Cruz", "Ana", "Reyes")).thenReturn(List.of(record));
    }

    private void expectExists(boolean exists) throws Exception {
        mvc.perform(get("/api/student-portal/check-duplicate")
                        .param("lastName", "Dela Cruz").param("firstName", "Ana").param("middleName", "Reyes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(exists));
    }

    @Test
    void aCompletedStudentsNameIsReportedAsExisting() throws Exception {
        existingStudentWithStatus("Completed");
        expectExists(true);
    }

    @Test
    void aGraduatedStudentsNameIsReportedAsExisting() throws Exception {
        existingStudentWithStatus("Graduated");
        expectExists(true);
    }

    @Test
    void aSubmittedStudentsNameIsReportedAsExisting() throws Exception {
        existingStudentWithStatus("Submitted");
        expectExists(true);
    }

    @Test
    void anActiveStudentsNameIsReportedAsExisting() throws Exception {
        existingStudentWithStatus("Active");
        expectExists(true);
    }

    @Test
    void aRecordWithNoStatusIsNotADuplicateAndDoesNotFail() throws Exception {
        existingStudentWithStatus(null);
        expectExists(false);
    }

    @Test
    void anEnrollingRecordIsResumableNotADuplicate() throws Exception {
        existingStudentWithStatus("Enrolling");
        expectExists(false);
    }
}
