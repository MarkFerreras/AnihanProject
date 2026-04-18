package com.example.springboot.service;

import java.time.LocalDateTime;
import java.util.List;

import com.example.springboot.dto.SystemLogResponse;

public record SystemLogQueryResult(
        List<SystemLogResponse> logs,
        LocalDateTime windowStart,
        LocalDateTime windowEnd
) {
}
