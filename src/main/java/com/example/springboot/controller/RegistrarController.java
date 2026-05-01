package com.example.springboot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.service.RegistrarService;

@RestController
@RequestMapping("/api/registrar/student-records")
public class RegistrarController {

    private final RegistrarService registrarService;

    public RegistrarController(RegistrarService registrarService) {
        this.registrarService = registrarService;
    }

    @GetMapping
    public ResponseEntity<List<StudentRecordSummaryResponse>> list() {
        return ResponseEntity.ok(registrarService.getAllRecords());
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<StudentRecordDetailsResponse> detail(@PathVariable Integer recordId) {
        return ResponseEntity.ok(registrarService.getRecordById(recordId));
    }
}
