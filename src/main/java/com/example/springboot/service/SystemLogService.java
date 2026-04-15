package com.example.springboot.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.SystemLogResponse;
import com.example.springboot.model.SystemLog;
import com.example.springboot.repository.SystemLogRepository;

@Service
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    public SystemLogService(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    /**
     * Records a user action in the system log.
     *
     * @param userId    the ID of the user performing the action (nullable for edge cases)
     * @param username  the username of the user
     * @param role      the role of the user (e.g. ROLE_ADMIN)
     * @param action    human-readable description of the action
     * @param ipAddress the IP address of the request
     */
    @Transactional
    public void logAction(Integer userId, String username, String role, String action, String ipAddress) {
        SystemLog log = new SystemLog(userId, username, role, action, ipAddress);
        systemLogRepository.save(log);
    }

    /**
     * Retrieves all system logs ordered by timestamp descending (newest first).
     */
    @Transactional(readOnly = true)
    public List<SystemLogResponse> getAllLogs() {
        return systemLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(SystemLogResponse::from)
                .toList();
    }
}
