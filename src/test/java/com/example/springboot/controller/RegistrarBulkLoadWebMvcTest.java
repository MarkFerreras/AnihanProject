package com.example.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.SystemLogService;

/**
 * Bulk load WebMvc tests for the Registrar Student Records endpoint.
 *
 * Verifies that GET /api/registrar/student-records can serialize 200
 * student record summaries as JSON and that the optional ?q= search
 * parameter is forwarded to the service layer correctly.
 *
 * All record data is generated programmatically in JVM memory via Mockito
 * mocks. No real database is touched and no hard-coded dummy records are
 * persisted anywhere.
 */
@WebMvcTest(RegistrarController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class RegistrarBulkLoadWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrarService registrarService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private SystemLogService systemLogService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void getRecordsReturnsTwoHundredRecordsAsJson() throws Exception {
        when(registrarService.getAllRecords(isNull(), isNull(), isNull()))
                .thenReturn(buildSummaryList(200));

        long startMs = System.currentTimeMillis();
        mockMvc.perform(get("/api/registrar/student-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(200))
                .andExpect(jsonPath("$[0].studentId").value("STU-0"))
                .andExpect(jsonPath("$[199].studentId").value("STU-199"))
                .andExpect(jsonPath("$[0].batchCode").value("BATCH-0"))
                .andExpect(jsonPath("$[0].courseCode").value("CARS"))
                .andExpect(jsonPath("$[0].sectionCode").value("SEC-0"));
        long elapsedMs = System.currentTimeMillis() - startMs;

        // Non-functional requirement: record retrieval < 5 seconds
        assertTrue(elapsedMs < 5000,
                "Serializing 200 student records via /api/registrar/student-records took "
                        + elapsedMs + "ms — exceeds 5-second limit");
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void searchEndpointForwardsQueryParamToService() throws Exception {
        // Mock the service to return a filtered subset only when called with the query
        List<StudentRecordSummaryResponse> filtered = List.of(
                new StudentRecordSummaryResponse(43, "STU-42", "Last_42", "First_42",
                        "BATCH-42", "CARS", "SEC-42", "Active")
        );
        when(registrarService.getAllRecords(eq("Last_42"), isNull(), isNull()))
                .thenReturn(filtered);

        mockMvc.perform(get("/api/registrar/student-records").param("q", "Last_42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value("STU-42"))
                .andExpect(jsonPath("$[0].lastName").value("Last_42"))
                .andExpect(jsonPath("$[0].studentStatus").value("Active"));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void batchYearRangeFilterForwardsParamsToService() throws Exception {
        List<StudentRecordSummaryResponse> filtered = buildSummaryList(50);
        when(registrarService.getAllRecords(isNull(), eq(2024), eq(2026)))
                .thenReturn(filtered);

        mockMvc.perform(get("/api/registrar/student-records")
                        .param("fromYear", "2024")
                        .param("toYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void batchYearRangeFilterRejectsInvalidRange() throws Exception {
        mockMvc.perform(get("/api/registrar/student-records")
                        .param("fromYear", "2030")
                        .param("toYear", "2020"))
                .andExpect(status().isBadRequest());
    }

    private List<StudentRecordSummaryResponse> buildSummaryList(int count) {
        String[] statuses = {"Enrolling", "Submitted", "Active"};
        List<StudentRecordSummaryResponse> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new StudentRecordSummaryResponse(
                    i + 1,
                    "STU-" + i,
                    "Last_" + i,
                    "First_" + i,
                    "BATCH-" + i,
                    "CARS",
                    "SEC-" + i,
                    statuses[i % statuses.length]
            ));
        }
        return list;
    }
}
