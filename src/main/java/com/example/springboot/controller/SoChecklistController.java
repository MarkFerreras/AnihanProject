package com.example.springboot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.service.SoChecklistService;

/**
 * Per-student SO checklist behind the SO Checklist tab in registrar.html (spec 2026-10-01 SO
 * checklist §7). REGISTRAR only, via the /api/registrar/** rule. Read-only and not
 * audit-logged. Kept out of RegistrarController so that controller's tests stay untouched.
 */
@RestController
@RequestMapping("/api/registrar/student-records")
public class SoChecklistController {

    private final SoChecklistService soChecklistService;

    public SoChecklistController(SoChecklistService soChecklistService) {
        this.soChecklistService = soChecklistService;
    }

    @GetMapping("/{recordId}/so-checklist")
    public ResponseEntity<SoChecklistResponse> soChecklist(@PathVariable Integer recordId) {
        return ResponseEntity.ok(soChecklistService.checklist(recordId));
    }
}
