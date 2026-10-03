package com.example.springboot.controller;

import java.util.Map;
import java.util.Set;

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

    /** Statuses that make a name a duplicate. Enrolling is resumable, so it is not listed. */
    private static final Set<String> BLOCKING_STATUSES = Set.of("Submitted", "Active", "Completed", "Graduated");

    /**
     * Returns exists=true when a Submitted, Active, Completed or Graduated record already exists
     * for this name, so Enrolling/Draft records are treated as resumable rather than duplicates.
     * StudentDetailsService.startOrResume still blocks any non-Enrolling match on its own.
     */
    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Boolean>> checkDuplicate(
            @RequestParam String lastName,
            @RequestParam String firstName,
            @RequestParam String middleName) {

        boolean blocked = studentRecordRepository
                .findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                        lastName.trim(), firstName.trim(), middleName.trim())
                .stream()
                .anyMatch(r -> BLOCKING_STATUSES.contains(r.getStudentStatus()));

        return ResponseEntity.ok(Map.of("exists", blocked));
    }
}
