package com.example.springboot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.trainer.TrainerBatchResponse;
import com.example.springboot.dto.trainer.TrainerClassResponse;
import com.example.springboot.dto.trainer.TrainerClassStudentResponse;
import com.example.springboot.dto.trainer.TrainerSubjectResponse;
import com.example.springboot.dto.trainer.TrainerSubjectStudentResponse;
import com.example.springboot.service.TrainerService;

@RestController
@RequestMapping("/api/trainer")
public class TrainerController {

    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<TrainerSubjectResponse>> getMySubjects() {
        return ResponseEntity.ok(trainerService.getMyAssignedSubjects());
    }

    @GetMapping("/subjects/{subjectCode}/students")
    public ResponseEntity<List<TrainerSubjectStudentResponse>> getStudentsForSubject(
            @PathVariable String subjectCode) {
        return ResponseEntity.ok(trainerService.getStudentsForSubject(subjectCode));
    }

    @GetMapping("/classes")
    public ResponseEntity<List<TrainerClassResponse>> getMyClasses(
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "batchYear", required = false) Short batchYear,
            @RequestParam(value = "batchCode", required = false) String batchCode) {
        return ResponseEntity.ok(trainerService.getMyClasses(semester, batchYear, batchCode));
    }

    @GetMapping("/classes/semesters")
    public ResponseEntity<List<String>> getAvailableSemesters() {
        return ResponseEntity.ok(trainerService.getAvailableSemesters());
    }

    @GetMapping("/classes/batches")
    public ResponseEntity<List<TrainerBatchResponse>> getMyBatches() {
        return ResponseEntity.ok(trainerService.getMyBatches());
    }

    @GetMapping("/classes/{classId}/students")
    public ResponseEntity<List<TrainerClassStudentResponse>> getStudentsForClass(
            @PathVariable Integer classId) {
        return ResponseEntity.ok(trainerService.getStudentsForClass(classId));
    }
}
