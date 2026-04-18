package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.SystemLogResponse;
import com.example.springboot.model.SystemLog;
import com.example.springboot.repository.SystemLogRepository;

@ExtendWith(MockitoExtension.class)
class SystemLogServiceTest {

    @Mock
    private SystemLogRepository systemLogRepository;

    @InjectMocks
    private SystemLogService systemLogService;

    // ========== logAction tests (unchanged) ==========

    @Test
    void logActionSavesSystemLog() {
        when(systemLogRepository.save(any(SystemLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        systemLogService.logAction(1, "admin", "ROLE_ADMIN", "User logged in", "127.0.0.1");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());

        SystemLog saved = captor.getValue();
        assertEquals(1, saved.getUserId());
        assertEquals("admin", saved.getUsername());
        assertEquals("ROLE_ADMIN", saved.getRole());
        assertEquals("User logged in", saved.getAction());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void logActionHandlesNullUserId() {
        when(systemLogRepository.save(any(SystemLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        systemLogService.logAction(null, "unknown", "ROLE_UNKNOWN", "Edge case action", "192.168.1.1");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());

        SystemLog saved = captor.getValue();
        assertEquals(null, saved.getUserId());
        assertEquals("unknown", saved.getUsername());
    }

    // ========== getLogs tests (date filtering) ==========

    @Test
    void getLogsDefaultsToSevenDays() {
        when(systemLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<SystemLogResponse> results = systemLogService.getLogs(null, null, null);

        assertEquals(0, results.size());

        // Verify the repository was called with a 7-day window
        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(systemLogRepository).findByTimestampBetweenOrderByTimestampDesc(
                startCaptor.capture(), endCaptor.capture());

        LocalDateTime capturedStart = startCaptor.getValue();
        LocalDateTime capturedEnd = endCaptor.getValue();
        // The window should be approximately 7 days
        long daysDiff = java.time.Duration.between(capturedStart, capturedEnd).toDays();
        assertEquals(7, daysDiff);
    }

    @Test
    void getLogsFourteenDayFilter() {
        SystemLog log = new SystemLog(1, "admin", "ROLE_ADMIN", "Test action", "127.0.0.1");
        log.setLogId(100);

        when(systemLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(log));

        List<SystemLogResponse> results = systemLogService.getLogs(14, null, null);

        assertEquals(1, results.size());
        assertEquals(100, results.get(0).logId());

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(systemLogRepository).findByTimestampBetweenOrderByTimestampDesc(
                startCaptor.capture(), endCaptor.capture());

        long daysDiff = java.time.Duration.between(startCaptor.getValue(), endCaptor.getValue()).toDays();
        assertEquals(14, daysDiff);
    }

    @Test
    void getLogsThirtyDayFilter() {
        when(systemLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        systemLogService.getLogs(30, null, null);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(systemLogRepository).findByTimestampBetweenOrderByTimestampDesc(
                startCaptor.capture(), endCaptor.capture());

        long daysDiff = java.time.Duration.between(startCaptor.getValue(), endCaptor.getValue()).toDays();
        assertEquals(30, daysDiff);
    }

    @Test
    void getLogsCustomDateRange() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 18);

        SystemLog log1 = new SystemLog(1, "admin", "ROLE_ADMIN", "Logged in", "127.0.0.1");
        log1.setLogId(200);
        SystemLog log2 = new SystemLog(2, "registrar", "ROLE_REGISTRAR", "Viewed records", "10.0.0.1");
        log2.setLogId(201);

        when(systemLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(log1, log2));

        List<SystemLogResponse> results = systemLogService.getLogs(null, start, end);

        assertEquals(2, results.size());
        assertEquals("admin", results.get(0).username());
        assertEquals("registrar", results.get(1).username());

        // Verify inclusive range boundaries
        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(systemLogRepository).findByTimestampBetweenOrderByTimestampDesc(
                startCaptor.capture(), endCaptor.capture());

        assertEquals(start.atStartOfDay(), startCaptor.getValue());
        assertEquals(end.atTime(LocalTime.MAX), endCaptor.getValue());
    }

    @Test
    void getLogsCustomDateRangeIgnoresRangeDays() {
        LocalDate start = LocalDate.of(2026, 4, 10);
        LocalDate end = LocalDate.of(2026, 4, 15);

        when(systemLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Pass rangeDays=30 along with custom dates — custom should take precedence
        systemLogService.getLogs(30, start, end);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(systemLogRepository).findByTimestampBetweenOrderByTimestampDesc(
                startCaptor.capture(), endCaptor.capture());

        // Should use the custom range, not 30 days
        assertEquals(start.atStartOfDay(), startCaptor.getValue());
        assertEquals(end.atTime(LocalTime.MAX), endCaptor.getValue());
    }

    @Test
    void getLogsInvalidDateRangeThrows() {
        LocalDate start = LocalDate.of(2026, 4, 18);
        LocalDate end = LocalDate.of(2026, 4, 1);

        assertThrows(IllegalArgumentException.class, () -> {
            systemLogService.getLogs(null, start, end);
        });
    }

    @Test
    void getLogsReturnsEmptyListWhenNoLogsInRange() {
        when(systemLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<SystemLogResponse> results = systemLogService.getLogs(7, null, null);

        assertEquals(0, results.size());
    }

    @Test
    void getLogsReturnsMappedResponses() {
        SystemLog log1 = new SystemLog(1, "admin", "ROLE_ADMIN", "Logged in", "127.0.0.1");
        log1.setLogId(100);

        SystemLog log2 = new SystemLog(2, "registrar", "ROLE_REGISTRAR", "Viewed records", "10.0.0.1");
        log2.setLogId(101);

        when(systemLogRepository.findByTimestampBetweenOrderByTimestampDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(log1, log2));

        List<SystemLogResponse> results = systemLogService.getLogs(7, null, null);

        assertEquals(2, results.size());

        assertEquals(100, results.get(0).logId());
        assertEquals("admin", results.get(0).username());
        assertEquals("ROLE_ADMIN", results.get(0).role());
        assertEquals("Logged in", results.get(0).action());

        assertEquals(101, results.get(1).logId());
        assertEquals("registrar", results.get(1).username());
        assertEquals("ROLE_REGISTRAR", results.get(1).role());
        assertEquals("Viewed records", results.get(1).action());
    }
}
