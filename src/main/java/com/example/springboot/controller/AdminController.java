package com.example.springboot.controller;

import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User userDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return userRepository.findById(id).map(user -> {
            // Prevent self-role modification if changing to a different role
            if (user.getUsername().equals(currentUsername) && !user.getRole().equals(userDetails.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You cannot change your own role to prevent losing administrative access."));
            }

            user.setUsername(userDetails.getUsername());
            user.setEmail(userDetails.getEmail());
            user.setRole(userDetails.getRole());
            user.setFirstName(userDetails.getFirstName());
            user.setLastName(userDetails.getLastName());
            user.setMiddleName(userDetails.getMiddleName());
            user.setBirthdate(userDetails.getBirthdate());
            user.setAge(userDetails.getAge());
            
            userRepository.save(user);
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
