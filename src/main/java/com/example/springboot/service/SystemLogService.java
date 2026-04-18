package com.example.springboot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
     * Retrieves system logs filtered by date range.
     * <p>
     * Filter precedence:
     * <ol>
     *   <li>If both {@code startDate} and {@code endDate} are present, use the custom inclusive range.</li>
     *   <li>Else if {@code rangeDays} is present, use a rolling window ending at the current time.</li>
     *   <li>Else default to the last 7 days.</li>
     * </ol>
     *
     * @param rangeDays number of days back from now (typically 7, 14, or 30)
     * @param startDate custom range start (inclusive, start of day)
     * @param endDate   custom range end (inclusive, end of day)
     * @return filtered logs ordered by timestamp descending
     * @throws IllegalArgumentException if startDate is after endDate
     */
    @Transactional(readOnly = true)
    public List<SystemLogResponse> getLogs(Integer rangeDays, LocalDate startDate, LocalDate endDate) {
        return queryLogs(rangeDays, startDate, endDate).logs();
    }

    @Transactional(readOnly = true)
    public SystemLogQueryResult queryLogs(Integer rangeDays, LocalDate startDate, LocalDate endDate) {
        LocalDateTime windowStart;
        LocalDateTime windowEnd;

        if (startDate != null && endDate != null) {
            // Custom date range — inclusive for the full selected day
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("startDate must not be after endDate");
            }
            windowStart = startDate.atStartOfDay();
            windowEnd = endDate.atTime(LocalTime.MAX);
        } else {
            // Preset range or default
            int days = (rangeDays != null) ? rangeDays : 7;
            windowEnd = LocalDateTime.now();
            windowStart = windowEnd.minusDays(days);
        }

        List<SystemLogResponse> logs = systemLogRepository.findByTimestampBetweenOrderByTimestampDesc(windowStart, windowEnd)
                .stream()
                .map(SystemLogResponse::from)
                .toList();

        return new SystemLogQueryResult(logs, windowStart, windowEnd);
    }
}
