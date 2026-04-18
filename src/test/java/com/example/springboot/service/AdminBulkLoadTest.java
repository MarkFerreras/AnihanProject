package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.springboot.dto.AdminUserResponse;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

/**
 * Bulk load tests for the Admin "View All Users" table.
 *
 * Verifies that AdminService.getAllUsers() can handle 100 users
 * correctly — DTO mapping, field accuracy, and performance.
 *
 * All user data is generated programmatically in memory via Mockito
 * mocks. No real database is touched and no hard-coded dummy users
 * are persisted anywhere.
 */
@ExtendWith(MockitoExtension.class)
class AdminBulkLoadTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getAllUsersReturnsOneHundredUsers() {
        List<User> bulkUsers = buildUserList(100);
        when(userRepository.findAll(any(Sort.class))).thenReturn(bulkUsers);

        List<AdminUserResponse> result = adminService.getAllUsers();

        assertEquals(100, result.size(), "getAllUsers should return exactly 100 user DTOs");
        assertEquals("user_0", result.get(0).username());
        assertEquals("user_99", result.get(99).username());
    }

    @Test
    void getAllUsersMapsAllDtoFieldsCorrectly() {
        List<User> bulkUsers = buildUserList(100);
        when(userRepository.findAll(any(Sort.class))).thenReturn(bulkUsers);

        List<AdminUserResponse> result = adminService.getAllUsers();

        // Spot-check several entries spread across the list
        for (int i : new int[]{0, 25, 50, 75, 99}) {
            AdminUserResponse dto = result.get(i);
            assertEquals(i + 1, dto.userId());
            assertEquals("user_" + i, dto.username());
            assertEquals("user_" + i + "@anihan.local", dto.email());
            assertEquals("Last_" + i, dto.lastName());
            assertEquals("First_" + i, dto.firstName());
            assertEquals("Middle_" + i, dto.middleName());
            assertEquals(20 + (i % 40), dto.age());
            assertEquals(true, dto.enabled());
            // AdminUserResponse record has no password field — safe by design
        }
    }

    @Test
    void getAllUsersCompletesWithinPerformanceBound() {
        List<User> bulkUsers = buildUserList(100);
        when(userRepository.findAll(any(Sort.class))).thenReturn(bulkUsers);

        long startMs = System.currentTimeMillis();
        List<AdminUserResponse> result = adminService.getAllUsers();
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertEquals(100, result.size());
        // Non-functional requirement: record retrieval < 5 seconds
        assertTrue(elapsedMs < 5000,
                "Retrieving 100 users took " + elapsedMs + "ms — exceeds 5-second limit");
    }

    /**
     * Generates a list of unique User objects for bulk testing.
     * All data lives only in memory — nothing is persisted to any database.
     */
    private List<User> buildUserList(int count) {
        String[] roles = {"ROLE_ADMIN", "ROLE_REGISTRAR", "ROLE_TRAINER"};
        List<User> users = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            User u = new User();
            u.setUserId(i + 1);
            u.setUsername("user_" + i);
            u.setPassword("encoded-password-" + i);
            u.setEmail("user_" + i + "@anihan.local");
            u.setRole(roles[i % roles.length]);
            u.setLastName("Last_" + i);
            u.setFirstName("First_" + i);
            u.setMiddleName("Middle_" + i);
            u.setAge(20 + (i % 40));
            u.setBirthdate(LocalDate.of(2000, 1, 1).plusDays(i));
            u.setEnabled(true);
            users.add(u);
        }
        return users;
    }
}
