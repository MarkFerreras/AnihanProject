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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.Section;
import com.example.springboot.model.Subject;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.SubjectRepository;
import com.example.springboot.service.CustomUserDetailsService;

/**
 * ISO 25010: Functional suitability (dropdown data contract) and Security (authentication required).
 */
@WebMvcTest(LookupController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class LookupControllerWebMvcTest {

    private static final String[] PATHS = {
            "/api/lookup/subjects", "/api/lookup/sections", "/api/lookup/batches", "/api/lookup/courses" };

    @Autowired private MockMvc mvc;

    @MockitoBean private SubjectRepository subjectRepository;
    @MockitoBean private SectionRepository sectionRepository;
    @MockitoBean private BatchRepository batchRepository;
    @MockitoBean private CourseRepository courseRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    // T7-01
    @Test
    @WithMockUser(roles = "REGISTRAR")
    void lookupSubjectsReturnsJsonList() throws Exception {
        Subject s = new Subject();
        s.setSubjectCode("SUB1");
        s.setSubjectName("Baking");
        when(subjectRepository.findAll()).thenReturn(List.of(s));
        mvc.perform(get(PATHS[0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("SUB1"))
                .andExpect(jsonPath("$[0].name").value("Baking"));
    }

    // T7-02
    @Test
    @WithMockUser(roles = "TRAINER")
    void lookupSectionsReturnsJsonList() throws Exception {
        Section s = new Section();
        s.setSectionCode("SEC1");
        s.setSection("A");
        when(sectionRepository.findAll()).thenReturn(List.of(s));
        mvc.perform(get(PATHS[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SEC1"))
                .andExpect(jsonPath("$[0].name").value("A"));
    }

    // T7-03
    @Test
    @WithMockUser(roles = "ADMIN")
    void lookupBatchesReturnsJsonList() throws Exception {
        when(batchRepository.findAll()).thenReturn(List.of(new Batch("B2026", (short) 2026)));
        mvc.perform(get(PATHS[2]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("B2026"))
                .andExpect(jsonPath("$[0].name").value("2026"));
    }

    // T7-04
    @Test
    @WithMockUser(roles = "REGISTRAR")
    void lookupCoursesReturnsJsonList() throws Exception {
        when(courseRepository.findAll()).thenReturn(List.of(new Course("CARS", "Culinary Arts")));
        mvc.perform(get(PATHS[3]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CARS"))
                .andExpect(jsonPath("$[0].name").value("Culinary Arts"));
    }

    // T7-01..04 (empty data)
    @Test
    @WithMockUser(roles = "REGISTRAR")
    void lookupEmptyDataReturnsEmptyArrayNotNull() throws Exception {
        when(subjectRepository.findAll()).thenReturn(List.of());
        when(sectionRepository.findAll()).thenReturn(List.of());
        when(batchRepository.findAll()).thenReturn(List.of());
        when(courseRepository.findAll()).thenReturn(List.of());
        for (String path : PATHS) {
            mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // T7-05
    @Test
    void lookupRequiresLogin() throws Exception {
        for (String path : PATHS) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}
