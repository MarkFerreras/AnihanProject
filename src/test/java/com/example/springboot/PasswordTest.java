package com.example.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    @Test
    public void printPassword() {
        System.out.println("HASH--->" + new BCryptPasswordEncoder().encode("password123"));
    }
}
