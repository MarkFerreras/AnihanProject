package com.example.springboot.dto.registrar;

import com.example.springboot.model.User;

public record TrainerResponse(
    Integer userId,
    String fullName
) {
    public static TrainerResponse from(User u) {
        return new TrainerResponse(
                u.getUserId(),
                u.getLastName() + ", " + u.getFirstName()
        );
    }
}
