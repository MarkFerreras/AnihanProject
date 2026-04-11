package com.example.springboot.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.springboot.model.User;

public record AdminUserResponse(
        Integer userId,
        String username,
        String email,
        String role,
        String lastName,
        String firstName,
        String middleName,
        Integer age,
        LocalDate birthdate,
        Boolean enabled,
        LocalDateTime passwordChangedAt
) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getLastName(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getAge(),
                user.getBirthdate(),
                user.getEnabled(),
                user.getPasswordChangedAt()
        );
    }
}
