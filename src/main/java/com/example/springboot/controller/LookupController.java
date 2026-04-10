package com.example.springboot.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.SubjectRepository;

/**
 * Read-only lookup endpoints for populating dropdown selectors.
 * Accessible to any authenticated user.
 */
@RestController
@RequestMapping("/api/lookup")
public class LookupController {

    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;

    public LookupController(SubjectRepository subjectRepository, SectionRepository sectionRepository) {
        this.subjectRepository = subjectRepository;
        this.sectionRepository = sectionRepository;
    }

    /**
     * GET /api/lookup/subjects
     * Returns all subjects as [{code, name}].
     */
    @GetMapping("/subjects")
    public ResponseEntity<List<Map<String, String>>> getSubjects() {
        List<Map<String, String>> subjects = subjectRepository.findAll().stream()
                .map(s -> Map.of(
                        "code", s.getSubjectCode(),
                        "name", s.getSubjectName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(subjects);
    }

    /**
     * GET /api/lookup/sections
     * Returns all sections as [{code, name}].
     */
    @GetMapping("/sections")
    public ResponseEntity<List<Map<String, String>>> getSections() {
        List<Map<String, String>> sections = sectionRepository.findAll().stream()
                .map(s -> Map.of(
                        "code", s.getSectionCode(),
                        "name", s.getSection()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(sections);
    }
}
