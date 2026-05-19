package com.example.springboot.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.trainer.GradeSummaryResponse;
import com.example.springboot.dto.trainer.SaveGradeRequest;
import com.example.springboot.service.SystemLogService;
import com.example.springboot.service.TrainerGradeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/trainer/classes/{classId}/grades")
@PreAuthorize("hasRole('TRAINER')")
public class TrainerGradeController {

    private static final Logger log = LoggerFactory.getLogger(TrainerGradeController.class);

    private final TrainerGradeService gradeService;
    private final SystemLogService systemLogService;

    public TrainerGradeController(TrainerGradeService gradeService, SystemLogService systemLogService) {
        this.gradeService = gradeService;
        this.systemLogService = systemLogService;
    }

    @GetMapping
    public ResponseEntity<?> getGrades(@PathVariable Integer classId) {
        try {
            GradeSummaryResponse response = gradeService.getGradesForClass(classId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get grades for class {}: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> saveGrades(
            @PathVariable Integer classId,
            @RequestBody List<SaveGradeRequest> gradeUpdates) {
        try {
            gradeService.saveGrades(classId, gradeUpdates);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to save grades for class {}: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/lock")
    public ResponseEntity<?> lockGrades(@PathVariable Integer classId) {
        try {
            gradeService.lockGrades(classId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to lock grades for class {}: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/unlock")
    public ResponseEntity<?> unlockGrades(@PathVariable Integer classId) {
        try {
            gradeService.unlockGrades(classId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to unlock grades for class {}: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
