package com.example.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.student.StudentDetailsRequest;
import com.example.springboot.dto.student.StudentDetailsResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.StudentDetailsService;

/**
 * ISO 25010: Functional suitability (enrollment wizard contract), Security (public portal has no upload surface).
 */
@WebMvcTest(StudentDetailsController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class StudentDetailsControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @Autowired private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean private StudentDetailsService studentDetailsService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private static StudentDetailsResponse response(String id, String status) {
        return new StudentDetailsResponse(id, "Cruz", "Ana", "", status,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                List.of(), List.of());
    }

    // T7-06
    @Test
    void startCreatesStudentAndReturnsId() throws Exception {
        when(studentDetailsService.startOrResume("Cruz", "Ana", "")).thenReturn(response("SR2026001", "Enrolling"));
        mvc.perform(post("/api/student/start").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"Cruz\",\"firstName\":\"Ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("SR2026001"))
                .andExpect(jsonPath("$.studentStatus").value("Enrolling"));
    }

    // T7-07
    @Test
    void startRejectsMissingNames() throws Exception {
        String[] bodies = {
                "{\"lastName\":\"\",\"firstName\":\"Ana\"}",
                "{\"lastName\":\"Cruz\",\"firstName\":\"   \"}",
                "{}" };
        for (String body : bodies) {
            mvc.perform(post("/api/student/start").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(studentDetailsService, never()).startOrResume(any(), any(), any());
    }

    // T7-08
    @Test
    void startDuplicateReturns409OrMessage() throws Exception {
        when(studentDetailsService.startOrResume("Cruz", "Ana", ""))
                .thenThrow(new IllegalStateException("already submitted"));
        mvc.perform(post("/api/student/start").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"Cruz\",\"firstName\":\"Ana\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("already submitted"));
    }

    // T7-09
    @Test
    void getReturnsSavedDetails() throws Exception {
        when(studentDetailsService.load("SR2026001")).thenReturn(response("SR2026001", "Enrolling"));
        mvc.perform(get("/api/student/SR2026001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("SR2026001"))
                .andExpect(jsonPath("$.lastName").value("Cruz"))
                .andExpect(jsonPath("$.firstName").value("Ana"));
    }

    // FINDING: GET /api/student/{id} is permitAll and keyed only by the guessable SR{year}{seq}
    // reference, so an anonymous caller can read an enrollee's saved personal data. Expected: some
    // proof of ownership (token/session) before returning details.
    @Test
    void getAnonymousReturnsDetailsWithoutOwnershipProof_currentlyOpen() throws Exception {
        when(studentDetailsService.load("SR2026002")).thenReturn(response("SR2026002", "Enrolling"));
        mvc.perform(get("/api/student/SR2026002")).andExpect(status().isOk());
    }

    // T7-10
    @Test
    void getUnknownIdReturns404() throws Exception {
        when(studentDetailsService.load("NOPE")).thenThrow(new IllegalArgumentException("unknown"));
        mvc.perform(get("/api/student/NOPE")).andExpect(status().isNotFound());
    }

    // T7-11
    @Test
    void submitMovesStatusToSubmitted() throws Exception {
        when(studentDetailsService.submitEnrollment(eq("SR2026001"), any(StudentDetailsRequest.class)))
                .thenReturn(response("SR2026001", "Submitted"));
        mvc.perform(post("/api/student/SR2026001/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"Cruz\",\"firstName\":\"Ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentStatus").value("Submitted"));
        verify(studentDetailsService, times(1)).submitEnrollment(eq("SR2026001"), any(StudentDetailsRequest.class));
    }

    // T7-11 (service rejections)
    @Test
    void submitMapsServiceErrorsToStatusCodes() throws Exception {
        when(studentDetailsService.submitEnrollment(eq("DONE"), any())).thenThrow(new IllegalStateException("submitted"));
        when(studentDetailsService.submitEnrollment(eq("NOPE"), any())).thenThrow(new IllegalArgumentException("unknown"));
        mvc.perform(post("/api/student/DONE/submit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/student/NOPE/submit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    // T7-12
    @Test
    void submitIgnoresClientSuppliedAge() throws Exception {
        assertTrue(Arrays.stream(StudentDetailsRequest.class.getRecordComponents())
                .noneMatch(c -> c.getName().equals("age")), "request DTO must not carry age");
        when(studentDetailsService.submitEnrollment(eq("SR2026001"), any(StudentDetailsRequest.class)))
                .thenReturn(response("SR2026001", "Submitted"));
        mvc.perform(post("/api/student/SR2026001/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"Cruz\",\"firstName\":\"Ana\",\"birthdate\":\"2000-01-01\",\"age\":99}"))
                .andExpect(status().isOk());
        ArgumentCaptor<StudentDetailsRequest> captor = ArgumentCaptor.forClass(StudentDetailsRequest.class);
        verify(studentDetailsService).submitEnrollment(eq("SR2026001"), captor.capture());
        assertEquals("Cruz", captor.getValue().lastName());
        assertEquals(LocalDate.of(2000, 1, 1), captor.getValue().birthdate());
    }

    // T7-13
    @Test
    void portalHasNoFileUploadEndpoint() {
        List<String> offenders = new ArrayList<>();
        for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod hm = entry.getValue();
            if (info.getPathPatternsCondition() == null) {
                continue;
            }
            boolean portal = info.getPathPatternsCondition().getPatternValues().stream()
                    .anyMatch(p -> p.startsWith("/api/student"));
            if (!portal) {
                continue;
            }
            boolean consumesMultipart = info.getConsumesCondition().getConsumableMediaTypes().stream()
                    .anyMatch(m -> m.isCompatibleWith(MediaType.MULTIPART_FORM_DATA));
            boolean takesFile = Arrays.stream(hm.getMethod().getParameterTypes())
                    .anyMatch(MultipartFile.class::isAssignableFrom);
            if (consumesMultipart || takesFile) {
                offenders.add(info.toString());
            }
        }
        assertTrue(offenders.isEmpty(), "multipart endpoints under /api/student*: " + offenders);
    }
}
