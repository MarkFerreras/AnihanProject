package com.example.springboot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.AdminCreateUserRequest;
import com.example.springboot.dto.AdminUpdateUserRequest;
import com.example.springboot.dto.AdminUserResponse;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new user account. Applies defaults for optional fields.
     */
    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        // Check for duplicate username
        if (userRepository.existsByUsername(request.username().trim())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Derive effective email
        String email = (request.email() != null && !request.email().isBlank())
                ? request.email().trim()
                : "user@anihan.local";

        // Check for duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already taken by another account");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role().trim());
        user.setLastName(request.lastName() != null && !request.lastName().isBlank()
                ? request.lastName().trim() : "User");
        user.setFirstName(request.firstName() != null && !request.firstName().isBlank()
                ? request.firstName().trim() : "New");
        user.setMiddleName(request.middleName() != null && !request.middleName().isBlank()
                ? request.middleName().trim() : "N/A");
        user.setEmail(email);
        user.setBirthdate(request.birthdate());
        user.setAge(AgeCalculator.calculateAge(request.birthdate()));
        user.setEnabled(true);

        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by("role", "lastName", "firstName", "username"))
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse getUserById(Integer userId) {
        User user = findUserById(userId);
        // Silently recalculate and persist age from birthdate.
        // This is NOT logged to system_logs.
        if (user.getBirthdate() != null) {
            user.setAge(AgeCalculator.calculateAge(user.getBirthdate()));
            userRepository.save(user);
        }
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateUser(Integer userId, AdminUpdateUserRequest request, String currentUsername) {
        User user = findUserById(userId);

        if (user.getUsername().equals(currentUsername) && !user.getRole().equals(request.role())) {
            throw new AccessDeniedException(
                    "You cannot change your own role to prevent losing administrative access."
            );
        }

        // Handle username change if provided
        if (request.username() != null && !request.username().isBlank()
                && !request.username().trim().equals(user.getUsername())) {
            userRepository.findByUsername(request.username().trim())
                    .filter(existing -> !existing.getUserId().equals(user.getUserId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Username is already taken by another account");
                    });
            user.setUsername(request.username().trim());
        }

        userRepository.findByEmail(request.email().trim())
                .filter(existing -> !existing.getUserId().equals(user.getUserId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email is already taken by another account");
                });

        user.setEmail(request.email().trim());
        user.setRole(request.role().trim());
        user.setLastName(request.lastName().trim());
        user.setFirstName(request.firstName().trim());
        user.setMiddleName(request.middleName().trim());
        user.setBirthdate(request.birthdate());
        user.setAge(AgeCalculator.calculateAge(request.birthdate()));

        // Only update password if provided (non-null and non-blank)
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setPasswordChangedAt(LocalDateTime.now());
        }

        return AdminUserResponse.from(userRepository.save(user));
    }

    /**
     * Soft delete — sets enabled = false so the user can no longer log in.
     * The record is preserved for auditing purposes.
     */
    @Transactional
    public void softDeleteUser(Integer userId, String currentUsername) {
        User user = findUserById(userId);
        preventSelfDeletion(user, currentUsername);

        user.setEnabled(false);
        userRepository.save(user);
    }

    /**
     * Hard delete — permanently removes the user record from the database.
     * This action cannot be undone.
     */
    @Transactional
    public void hardDeleteUser(Integer userId, String currentUsername) {
        User user = findUserById(userId);
        preventSelfDeletion(user, currentUsername);

        userRepository.delete(user);
    }

    /**
     * Re-enable a previously soft-deleted user, allowing them to log in again.
     */
    @Transactional
    public void reEnableUser(Integer userId) {
        User user = findUserById(userId);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private void preventSelfDeletion(User user, String currentUsername) {
        if (user.getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("You cannot delete your own account.");
        }
    }

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
