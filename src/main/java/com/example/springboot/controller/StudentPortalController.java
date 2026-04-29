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
     * Returns exists=true only when a Submitted or Active record already exists for this name,
     * so Enrolling/Draft records are treated as resumable rather than duplicates.
     */
    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(
            @RequestParam String lastName,
            @RequestParam String firstName,
            @RequestParam String middleName) {

        boolean blocked = studentRecordRepository
                .findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                        lastName.trim(), firstName.trim(), middleName.trim())
                .map(r -> "Submitted".equals(r.getStudentStatus()) || "Active".equals(r.getStudentStatus()))
                .orElse(false);

        return ResponseEntity.ok(Map.of("exists", blocked));
    }
}
