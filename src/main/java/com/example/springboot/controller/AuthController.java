package com.example.springboot.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.LoginRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          SystemLogService systemLogService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
    }

    /**
     * POST /api/auth/login
     * Authenticates the user, creates a session, and returns username + role.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request,
                                                      HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // Store authentication in the security context and create a session
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_UNKNOWN");

        // Log the login action
        String ipAddress = httpRequest.getRemoteAddr();
        userRepository.findByUsername(username).ifPresent(user ->
                systemLogService.logAction(user.getUserId(), username, role, "User logged in", ipAddress)
        );

        return ResponseEntity.ok(Map.of(
                "username", username,
                "role", role
        ));
    }

    /**
     * POST /api/auth/logout
     * Invalidates the current session.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        // Capture user identity BEFORE clearing context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_UNKNOWN");
            String ipAddress = request.getRemoteAddr();

            userRepository.findByUsername(username).ifPresent(user ->
                    systemLogService.logAction(user.getUserId(), username, role, "User logged out", ipAddress)
            );
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * GET /api/auth/me
     * Returns the current authenticated user's info including personal details,
     * or 401 if not authenticated.
     */
    @Transactional
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Not authenticated"));
        }

        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_UNKNOWN");

        // Fetch full user entity for personal details
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("role", role);

        userRepository.findByUsername(username).ifPresent(user -> {
            // Silently recalculate and persist age from birthdate.
            // This is NOT logged to system_logs.
            if (user.getBirthdate() != null) {
                user.setAge(com.example.springboot.service.AgeCalculator.calculateAge(user.getBirthdate()));
                userRepository.save(user);
            }

            response.put("lastName", user.getLastName());
            response.put("firstName", user.getFirstName());
            response.put("middleName", user.getMiddleName());
            response.put("age", user.getAge());
            response.put("birthdate", user.getBirthdate());
        });

        return ResponseEntity.ok(response);
    }
}
