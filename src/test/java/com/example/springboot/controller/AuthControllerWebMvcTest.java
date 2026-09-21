package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.repository.UserSecurityAnswerRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SessionAuthenticationHelper;
import com.example.springboot.service.SystemLogService;

/**
 * Pins the 2026-09-19 decision that logging in and logging out are NOT audited.
 *
 * <p>The {@code verifyNoInteractions(systemLogService)} assertions are the point of
 * this class: if someone re-introduces a {@code logAction} call in {@code AuthController},
 * the mock gets injected again and these tests fail.
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSecurityAnswerRepository userSecurityAnswerRepository;

    @MockitoBean
    private SessionAuthenticationHelper sessionAuthenticationHelper;

    @MockitoBean
    private SystemLogService systemLogService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User seedAdmin() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("admin");
        user.setRole("ROLE_ADMIN");
        return user;
    }

    private void stubSuccessfulAuthentication() {
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(seedAdmin()));
        when(userSecurityAnswerRepository.countByUserUserId(1)).thenReturn(2L);
    }

    @Test
    void loginWritesNoSystemLogRow() throws Exception {
        stubSuccessfulAuthentication();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        verifyNoInteractions(systemLogService);
    }

    @Test
    void loginStillReturnsUsernameAndRole() throws Exception {
        stubSuccessfulAuthentication();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void logoutWritesNoSystemLogRow() throws Exception {
        // Stubbed so the old logout() code path (which resolved the user via
        // userRepository.findByUsername(...).ifPresent(...) before logging)
        // would actually have run and this test would genuinely have failed
        // against it — not passed vacuously off Mockito's default
        // Optional.empty() for an unstubbed Optional-returning method.
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(seedAdmin()));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verifyNoInteractions(systemLogService);
    }
}
