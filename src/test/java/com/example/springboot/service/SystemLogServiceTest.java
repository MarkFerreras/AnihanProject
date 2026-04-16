package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

    @Test
    void getAllLogsReturnsMappedResponses() {
        SystemLog log1 = new SystemLog(1, "admin", "ROLE_ADMIN", "Logged in", "127.0.0.1");
        log1.setLogId(100);

        SystemLog log2 = new SystemLog(2, "registrar", "ROLE_REGISTRAR", "Viewed records", "10.0.0.1");
        log2.setLogId(101);

        when(systemLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of(log1, log2));

        List<SystemLogResponse> results = systemLogService.getAllLogs();

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

    @Test
    void getAllLogsReturnsEmptyListWhenNoLogs() {
        when(systemLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of());

        List<SystemLogResponse> results = systemLogService.getAllLogs();

        assertEquals(0, results.size());
    }
}
