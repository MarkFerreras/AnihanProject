package com.example.springboot.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.UpdatePersonalDetailsRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Updates the username of the currently authenticated user.
     * Validates current password before allowing the change.
     * Refreshes the SecurityContext so the session reflects the new username.
     */
    @Transactional
    public void updateUsername(String currentUsername, String newUsername, String currentPassword) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Check if new username is same as current
        if (currentUsername.equals(newUsername)) {
            throw new IllegalArgumentException("New username is the same as the current username");
        }

        // Check if new username is already taken
        if (userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Update username
        user.setUsername(newUsername);
        userRepository.save(user);

        // Refresh the SecurityContext with the new username
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                newUsername,
                null,
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    /**
     * Updates the password of the currently authenticated user.
     * Validates current password and confirms new password match before allowing the change.
     */
    @Transactional
    public void updatePassword(String username, String currentPassword,
                               String newPassword, String confirmNewPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Verify new passwords match
        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        // Verify new password is different from current
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        // Hash and save new password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Updates the personal details of the currently authenticated user.
     * Subject and section fields are only accepted for users with ROLE_TRAINER.
     */
    @Transactional
    public User updatePersonalDetails(String username, UpdatePersonalDetailsRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Only overwrite fields that the client actually provided (non-null)
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.middleName() != null) {
            user.setMiddleName(request.middleName());
        }
        if (request.birthdate() != null) {
            user.setBirthdate(request.birthdate());
        }

        // Always recalculate age from the current birthdate on the entity
        user.setAge(AgeCalculator.calculateAge(user.getBirthdate()));

        return userRepository.save(user);
    }
}
