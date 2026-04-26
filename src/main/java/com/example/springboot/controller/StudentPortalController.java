package com.example.springboot.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.repository.StudentRecordRepository;

@RestController
@RequestMapping("/api/student-portal")
public class StudentPortalController {

    private final StudentRecordRepository studentRecordRepository;

    public StudentPortalController(StudentRecordRepository studentRecordRepository) {
        this.studentRecordRepository = studentRecordRepository;
    }

    /**
     * Public endpoint — checks if a student record already exists
     * with the given first, middle, and last name (case-insensitive).
     */
    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(
            @RequestParam String lastName,
            @RequestParam String firstName,
            @RequestParam String middleName) {

        boolean exists = studentRecordRepository
                .existsByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                        lastName.trim(), firstName.trim(), middleName.trim());

        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
