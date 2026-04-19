package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.springboot.dto.UpdatePersonalDetailsRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    // ========== updateUsername Tests ==========

    @Test
    void updateUsernameSucceeds() {
        User user = buildUser("oldUser", "ROLE_ADMIN");
        when(userRepository.findByUsername("oldUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.updateUsername("oldUser", "newUser", "password123");

        assertEquals("newUser", user.getUsername());
        verify(userRepository).save(user);
    }

    @Test
    void updateUsernameThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updateUsername("ghost", "newUser", "password123"));

        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUsernameThrowsWhenPasswordIncorrect() {
        User user = buildUser("admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", user.getPassword())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updateUsername("admin", "newAdmin", "wrongPass"));

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUsernameThrowsWhenSameUsername() {
        User user = buildUser("admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updateUsername("admin", "admin", "password123"));

        assertEquals("New username is the same as the current username", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUsernameThrowsWhenUsernameTaken() {
        User user = buildUser("admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
        when(userRepository.existsByUsername("registrar")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updateUsername("admin", "registrar", "password123"));

        assertEquals("Username is already taken", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== updatePassword Tests ==========

    @Test
    void updatePasswordSucceeds() {
        User user = buildUser("admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("NewPass1!", user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded-new-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.updatePassword("admin", "oldPass", "NewPass1!", "NewPass1!");

        assertEquals("encoded-new-password", user.getPassword());
        assertNotNull(user.getPasswordChangedAt());
        verify(userRepository).save(user);
    }

    @Test
    void updatePasswordThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updatePassword("ghost", "old", "new", "new"));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void updatePasswordThrowsWhenCurrentPasswordIncorrect() {
        User user = buildUser("admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOld", user.getPassword())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updatePassword("admin", "wrongOld", "NewPass1!", "NewPass1!"));

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updatePasswordThrowsWhenNewPasswordsDoNotMatch() {
        User user = buildUser("admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", user.getPassword())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updatePassword("admin", "oldPass", "NewPass1!", "DifferentPass2!"));

        assertEquals("New passwords do not match", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updatePasswordThrowsWhenNewPasswordSameAsCurrent() {
        User user = buildUser("admin", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("oldPass", user.getPassword())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updatePassword("admin", "oldPass", "oldPass", "oldPass"));

        assertEquals("New password must be different from the current password", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== updatePersonalDetails Tests ==========

    @Test
    void updatePersonalDetailsSucceeds() {
        User user = buildUser("trainer", "ROLE_TRAINER");
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePersonalDetailsRequest request = new UpdatePersonalDetailsRequest(
                "Dela Cruz", "Juan", "Santos", LocalDate.of(2001, 5, 15)
        );

        User result = accountService.updatePersonalDetails("trainer", request);

        assertEquals("Dela Cruz", result.getLastName());
        assertEquals("Juan", result.getFirstName());
        assertEquals("Santos", result.getMiddleName());
        assertEquals(AgeCalculator.calculateAge(LocalDate.of(2001, 5, 15)), result.getAge());
        assertEquals(LocalDate.of(2001, 5, 15), result.getBirthdate());
        verify(userRepository).save(user);
    }

    @Test
    void updatePersonalDetailsThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        UpdatePersonalDetailsRequest request = new UpdatePersonalDetailsRequest(
                "Dela Cruz", "Juan", "Santos", LocalDate.of(2001, 5, 15)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updatePersonalDetails("ghost", request));

        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updatePersonalDetailsRecalculatesAgeFromNewBirthdate() {
        User user = buildUser("admin", "ROLE_ADMIN");
        // Start with a stale age that doesn't match any birthdate
        user.setAge(99);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate newBirthdate = LocalDate.of(2000, 6, 15);
        UpdatePersonalDetailsRequest request = new UpdatePersonalDetailsRequest(
                "Admin", "User", "Middle", newBirthdate
        );

        User result = accountService.updatePersonalDetails("admin", request);

        // Age should be freshly calculated from the NEW birthdate
        assertEquals(AgeCalculator.calculateAge(newBirthdate), result.getAge());
        assertEquals(newBirthdate, result.getBirthdate());
        verify(userRepository).save(user);
    }

    @Test
    void updatePersonalDetailsPreservesExistingBirthdateWhenRequestBirthdateIsNull() {
        User user = buildUser("trainer", "ROLE_TRAINER");
        // User already has a valid birthdate and age
        LocalDate existingBirthdate = LocalDate.of(1996, 4, 11);
        user.setBirthdate(existingBirthdate);
        user.setAge(30);

        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Request with null birthdate — should NOT wipe the existing one
        UpdatePersonalDetailsRequest request = new UpdatePersonalDetailsRequest(
                "Updated", "Name", "Here", null
        );

        User result = accountService.updatePersonalDetails("trainer", request);

        // Birthdate should remain the existing value, NOT be overwritten to null
        assertEquals(existingBirthdate, result.getBirthdate());
        // Age should be recalculated from the EXISTING birthdate
        assertEquals(AgeCalculator.calculateAge(existingBirthdate), result.getAge());
        // Name fields SHOULD be updated
        assertEquals("Updated", result.getLastName());
        assertEquals("Name", result.getFirstName());
        assertEquals("Here", result.getMiddleName());
        verify(userRepository).save(user);
    }

    // ========== Helper ==========

    private User buildUser(String username, String role) {
        User user = new User();
        user.setUserId(1);
        user.setUsername(username);
        user.setEmail(username + "@anihan.edu");
        user.setRole(role);
        user.setPassword("encoded-password");
        user.setLastName("Test");
        user.setFirstName("User");
        user.setMiddleName("Middle");
        user.setAge(30);
        user.setBirthdate(LocalDate.of(1996, 4, 11));
        user.setEnabled(true);
        return user;
    }
}

