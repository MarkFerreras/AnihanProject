package com.example.springboot.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String encodedPassword = passwordEncoder.encode("password123");

            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(new User("admin", encodedPassword, "admin@anihan.edu", "ROLE_ADMIN"));
                System.out.println("[DataSeeder] Created admin account");
            }

            if (!userRepository.existsByUsername("registrar")) {
                userRepository.save(new User("registrar", encodedPassword, "registrar@anihan.edu", "ROLE_REGISTRAR"));
                System.out.println("[DataSeeder] Created registrar account");
            }

            if (!userRepository.existsByUsername("trainer")) {
                userRepository.save(new User("trainer", encodedPassword, "trainer@anihan.edu", "ROLE_TRAINER"));
                System.out.println("[DataSeeder] Created trainer account");
            }

            System.out.println("[DataSeeder] User seeding complete");
        };
    }
}
