package com.example.springboot.dto;

import java.time.LocalDateTime;

import com.example.springboot.model.SystemLog;

public record SystemLogResponse(
        Integer logId,
        Integer userId,
        String username,
        String role,
        String action,
        String ipAddress,
        LocalDateTime timestamp
) {

    public static SystemLogResponse from(SystemLog log) {
        return new SystemLogResponse(
                log.getLogId(),
                log.getUserId(),
                log.getUsername(),
                log.getRole(),
                log.getAction(),
                log.getIpAddress(),
                log.getTimestamp()
        );
    }
}
