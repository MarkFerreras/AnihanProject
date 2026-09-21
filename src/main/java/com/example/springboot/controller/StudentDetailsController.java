package com.example.springboot.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.student.StudentDetailsRequest;
import com.example.springboot.dto.student.StudentDetailsResponse;
import com.example.springboot.service.StudentDetailsService;

@RestController
@RequestMapping("/api/student")
public class StudentDetailsController {

    private final StudentDetailsService studentDetailsService;

    public StudentDetailsController(StudentDetailsService studentDetailsService) {
        this.studentDetailsService = studentDetailsService;
    }

    /**
     * Creates a minimal "Enrolling" student record (name + status only)
     * so that uploads can reference the student_id FK.
     * No substantive data is persisted at this stage.
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody Map<String, String> body) {
        String lastName = body.getOrDefault("lastName", "").trim();
        String firstName = body.getOrDefault("firstName", "").trim();
        String middleName = body.getOrDefault("middleName", "").trim();
        if (lastName.isBlank() || firstName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            StudentDetailsResponse response =
                    studentDetailsService.startOrResume(lastName, firstName, middleName);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentDetailsResponse> load(@PathVariable String studentId) {
        try {
            return ResponseEntity.ok(studentDetailsService.load(studentId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Final submit: accepts the full enrollment payload, persists all data
     * (student record, parents, education) in one transaction, and sets
     * status to "Submitted".
     */
    @PostMapping("/{studentId}/submit")
    public ResponseEntity<StudentDetailsResponse> submit(
            @PathVariable String studentId,
            @RequestBody StudentDetailsRequest req) {
        try {
            return ResponseEntity.ok(studentDetailsService.submitEnrollment(studentId, req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
