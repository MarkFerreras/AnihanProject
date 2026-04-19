package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.springboot.dto.AdminUpdateUserRequest;
import com.example.springboot.dto.AdminUserResponse;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @InjectMocks
        private AdminService adminService;

        @Test
        void updateUserBlocksSelfRoleChange() {
                User admin = buildUser(
                                1,
                                "admin",
                                "admin@anihan.edu",
                                "ROLE_ADMIN",
                                "Admin",
                                "System",
                                "Owner");

                AdminUpdateUserRequest request = new AdminUpdateUserRequest(
                                null,
                                "admin@anihan.edu",
                                "ROLE_REGISTRAR",
                                "Admin",
                                "System",
                                "Owner",
                                LocalDate.of(1996, 4, 11),
                                null);

                when(userRepository.findById(1)).thenReturn(Optional.of(admin));

                AccessDeniedException exception = assertThrows(
                                AccessDeniedException.class,
                                () -> adminService.updateUser(1, request, "admin"));

                assertEquals(
                                "You cannot change your own role to prevent losing administrative access.",
                                exception.getMessage());
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void updateUserReturnsSanitizedResponse() {
                User registrar = buildUser(
                                2,
                                "registrar",
                                "registrar@anihan.edu",
                                "ROLE_REGISTRAR",
                                "Cruz",
                                "Maria",
                                "Santos");

                AdminUpdateUserRequest request = new AdminUpdateUserRequest(
                                null,
                                "records@anihan.edu",
                                "ROLE_REGISTRAR",
                                "Cruz",
                                "Maria",
                                "Santos",
                                LocalDate.of(1998, 3, 20),
                                null);

                when(userRepository.findById(2)).thenReturn(Optional.of(registrar));
                when(userRepository.findByEmail("records@anihan.edu")).thenReturn(Optional.empty());
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

                AdminUserResponse response = adminService.updateUser(2, request, "admin");

                assertEquals(2, response.userId());
                assertEquals("registrar", response.username());
                assertEquals("records@anihan.edu", response.email());
                assertEquals("ROLE_REGISTRAR", response.role());
                assertEquals("Cruz", response.lastName());
                assertEquals("Maria", response.firstName());
                assertEquals("Santos", response.middleName());
                assertEquals(AgeCalculator.calculateAge(LocalDate.of(1998, 3, 20)), response.age());
                assertEquals(LocalDate.of(1998, 3, 20), response.birthdate());
        }

        @Test
        void getUserByIdRecalculatesAgeFromBirthdate() {
                User user = buildUser(
                                3,
                                "trainer",
                                "trainer@anihan.edu",
                                "ROLE_TRAINER",
                                "Cruz",
                                "Maria",
                                "Santos");
                // Set a stale age that doesn't match the birthdate
                user.setAge(99);

                when(userRepository.findById(3)).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

                AdminUserResponse response = adminService.getUserById(3);

                // Age should be recalculated from birthdate, NOT the stale value
                assertEquals(AgeCalculator.calculateAge(LocalDate.of(1996, 4, 11)), response.age());
                verify(userRepository).save(user);
        }

        @Test
        void getUserByIdSkipsRecalculationWhenBirthdateIsNull() {
                User user = buildUser(
                                4,
                                "registrar",
                                "registrar@anihan.edu",
                                "ROLE_REGISTRAR",
                                "Reyes",
                                "Ana",
                                "Lim");
                user.setBirthdate(null);
                user.setAge(25);

                when(userRepository.findById(4)).thenReturn(Optional.of(user));

                AdminUserResponse response = adminService.getUserById(4);

                // Age should remain as-is (25) because birthdate is null
                assertEquals(25, response.age());
                // Should NOT call save since no recalculation happened
                verify(userRepository, never()).save(any(User.class));
        }

        private User buildUser(
                        Integer userId,
                        String username,
                        String email,
                        String role,
                        String lastName,
                        String firstName,
                        String middleName) {
                User user = new User();
                user.setUserId(userId);
                user.setUsername(username);
                user.setEmail(email);
                user.setRole(role);
                user.setPassword("encoded-password");
                user.setLastName(lastName);
                user.setFirstName(firstName);
                user.setMiddleName(middleName);
                user.setAge(30);
                user.setBirthdate(LocalDate.of(1996, 4, 11));
                user.setEnabled(true);
                return user;
        }
}
