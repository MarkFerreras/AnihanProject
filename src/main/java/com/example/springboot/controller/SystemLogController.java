package com.example.springboot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.SystemLogResponse;
import com.example.springboot.service.SystemLogService;

@RestController
@RequestMapping("/api/logs")
public class SystemLogController {

    private final SystemLogService systemLogService;

    public SystemLogController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    /**
     * GET /api/logs
     * Returns all system logs ordered by timestamp descending.
     * Restricted to ADMIN role via SecurityConfig.
     */
    @GetMapping
    public ResponseEntity<List<SystemLogResponse>> getAllLogs() {
        return ResponseEntity.ok(systemLogService.getAllLogs());
    }
}
