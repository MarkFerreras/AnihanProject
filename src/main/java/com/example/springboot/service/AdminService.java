package com.example.springboot.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.AdminUpdateUserRequest;
import com.example.springboot.dto.AdminUserResponse;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by("role", "lastName", "firstName", "username"))
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Integer userId) {
        return AdminUserResponse.from(findUserById(userId));
    }

    @Transactional
    public AdminUserResponse updateUser(Integer userId, AdminUpdateUserRequest request, String currentUsername) {
        User user = findUserById(userId);

        if (user.getUsername().equals(currentUsername) && !user.getRole().equals(request.role())) {
            throw new AccessDeniedException(
                    "You cannot change your own role to prevent losing administrative access."
            );
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
        user.setAge(request.age());
        user.setBirthdate(request.birthdate());

        return AdminUserResponse.from(userRepository.save(user));
    }

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
