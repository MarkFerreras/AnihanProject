package com.example.springboot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<List<TrainerClassResponse>> getMyClasses() {
        return ResponseEntity.ok(trainerService.getMyClasses());
    }

    @GetMapping("/classes/{classId}/students")
    public ResponseEntity<List<TrainerClassStudentResponse>> getStudentsForClass(
            @PathVariable Integer classId) {
        return ResponseEntity.ok(trainerService.getStudentsForClass(classId));
    }
}
