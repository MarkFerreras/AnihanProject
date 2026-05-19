package com.example.springboot.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

    private final TrainerGradeService gradeService;
    private final SystemLogService systemLogService;

    public TrainerGradeController(TrainerGradeService gradeService, SystemLogService systemLogService) {
        this.gradeService = gradeService;
        this.systemLogService = systemLogService;
    }

    @GetMapping
    public ResponseEntity<GradeSummaryResponse> getGrades(@PathVariable Integer classId) {
        try {
            GradeSummaryResponse response = gradeService.getGradesForClass(classId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping
    public ResponseEntity<Void> saveGrades(
            @PathVariable Integer classId,
            @Valid @RequestBody List<SaveGradeRequest> gradeUpdates) {
        try {
            gradeService.saveGrades(classId, gradeUpdates);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/lock")
    public ResponseEntity<Void> lockGrades(@PathVariable Integer classId) {
        try {
            gradeService.lockGrades(classId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/unlock")
    public ResponseEntity<Void> unlockGrades(@PathVariable Integer classId) {
        try {
            gradeService.unlockGrades(classId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
