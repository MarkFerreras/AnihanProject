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
import com.example.springboot.repository.UserSecurityAnswerRepository;
import com.example.springboot.service.SessionAuthenticationHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserSecurityAnswerRepository userSecurityAnswerRepository;
    private final SessionAuthenticationHelper sessionAuthenticationHelper;

    /** A user has finished security-question setup once both slots are saved. */
    private static final long REQUIRED_SECURITY_ANSWER_COUNT = 2;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          UserSecurityAnswerRepository userSecurityAnswerRepository,
                          SessionAuthenticationHelper sessionAuthenticationHelper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userSecurityAnswerRepository = userSecurityAnswerRepository;
        this.sessionAuthenticationHelper = sessionAuthenticationHelper;
    }

    /**
     * POST /api/auth/login
     * Authenticates the user, creates a session, and returns username + role.
     *
     * <p>If the account hasn't completed security-question setup yet, the
     * session is issued with ONLY {@code ROLE_PENDING_SETUP} instead of the
     * user's real role — {@code SecurityConfig} restricts that role to the
     * setup page/endpoints only, so the account cannot reach any dashboard
     * until setup is done. This is enforced server-side (not just by the
     * frontend redirect), since a session that IS authenticated would
     * otherwise be free to call any endpoint that only checks
     * {@code authenticated()}.
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

        String username = authentication.getName();
        String realRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_UNKNOWN");

        User user = userRepository.findByUsername(username).orElse(null);
        boolean setupComplete = user != null
                && userSecurityAnswerRepository.countByUserUserId(user.getUserId()) >= REQUIRED_SECURITY_ANSWER_COUNT;

        String responseRole;
        if (setupComplete) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            responseRole = realRole;
        } else {
            sessionAuthenticationHelper.establishRestrictedSession(httpRequest, username, "PENDING_SETUP");
            responseRole = "ROLE_PENDING_SETUP";
        }

        return ResponseEntity.ok(Map.of(
                "username", username,
                "role", responseRole
        ));
    }

    /**
     * POST /api/auth/logout
     * Invalidates the current session.
     *
     * <p>Logging out is deliberately NOT written to {@code system_logs} —
     * see memory-bank/decisions.md (2026-09-19).
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
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
        response.put("securityQuestionsSetUp",
                userRepository.findByUsername(username)
                        .map(u -> userSecurityAnswerRepository.countByUserUserId(u.getUserId())
                                >= REQUIRED_SECURITY_ANSWER_COUNT)
                        .orElse(false));

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
