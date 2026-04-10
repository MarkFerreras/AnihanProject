package com.example.springboot.dto;

import java.time.LocalDate;

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
        LocalDate birthdate
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
                user.getBirthdate()
        );
    }
}
