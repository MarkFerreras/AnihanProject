package com.example.springboot.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.EmailLookupRequest;
import com.example.springboot.dto.ResetPasswordRequest;
import com.example.springboot.dto.SecurityQuestionTextsResponse;
import com.example.springboot.dto.VerifyAnswersRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.SecurityQuestionService;
import com.example.springboot.service.SessionAuthenticationHelper;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * The public "forgot password" flow: email lookup → answer both security
 * questions → set a new password. Each step hands off to the next via a
 * short-lived, single-purpose session role established by
 * {@link SessionAuthenticationHelper} and enforced by {@code SecurityConfig}
 * — never by a value the client sends, so a request can't be replayed
 * against a different account or skipped ahead.
 */
@RestController
@RequestMapping("/api/password-recovery")
public class PasswordRecoveryController {

    /** How long a successful answer-verification stays valid before the reset step must be used. */
    private static final long RESET_WINDOW_MINUTES = 10;
    private static final String RESET_ISSUED_AT_ATTRIBUTE = "PENDING_RESET_ISSUED_AT";

    private final SecurityQuestionService securityQuestionService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final SessionAuthenticationHelper sessionAuthenticationHelper;

    public PasswordRecoveryController(SecurityQuestionService securityQuestionService,
            UserRepository userRepository,
            SystemLogService systemLogService,
            SessionAuthenticationHelper sessionAuthenticationHelper) {
        this.securityQuestionService = securityQuestionService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.sessionAuthenticationHelper = sessionAuthenticationHelper;
    }

    /**
     * POST /api/password-recovery/lookup
     * Public. On a match, issues a session restricted to
     * {@code ROLE_PENDING_VERIFICATION} for that one account and returns its
     * 2 question texts. On no match — unknown email, setup incomplete,
     * disabled, or already locked — responds identically (404, generic
     * message) so the failure path can't easily be told apart from itself;
     * it cannot be made to look identical to a *success*, which is an
     * accepted, documented trade-off of any security-question recovery
     * scheme (see memory-bank/decisions.md).
     */
    @PostMapping("/lookup")
    public ResponseEntity<SecurityQuestionTextsResponse> lookup(
            @Valid @RequestBody EmailLookupRequest request,
            HttpServletRequest httpRequest) {

        SecurityQuestionService.LookupResult result = securityQuestionService.lookupByEmail(request.email());

        sessionAuthenticationHelper.establishRestrictedSession(httpRequest, result.username(), "PENDING_VERIFICATION");

        return ResponseEntity.ok(new SecurityQuestionTextsResponse(result.questionTexts()));
    }

    /**
     * POST /api/password-recovery/verify
     * Requires the {@code ROLE_PENDING_VERIFICATION} session from
     * {@code lookup}. Both answers must be correct. On success, upgrades the
     * session to {@code ROLE_PENDING_RESET} (time-limited — see
     * {@link #reset}); on failure, the account's attempt counter advances
     * per {@link SecurityQuestionService#verifyAnswers}.
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @Valid @RequestBody VerifyAnswersRequest request,
            HttpServletRequest httpRequest) {

        String username = currentUsername();

        try {
            securityQuestionService.verifyAnswers(username, request.answers());
        } catch (IllegalArgumentException ex) {
            logIfNewlyLocked(username, httpRequest);
            throw ex;
        }

        sessionAuthenticationHelper.establishRestrictedSession(httpRequest, username, "PENDING_RESET");
        httpRequest.getSession(true).setAttribute(RESET_ISSUED_AT_ATTRIBUTE, Instant.now());

        return ResponseEntity.ok(Map.of("message", "Verified."));
    }

    /**
     * POST /api/password-recovery/reset
     * Requires the {@code ROLE_PENDING_RESET} session from {@code verify},
     * and that session must be less than {@value #RESET_WINDOW_MINUTES}
     * minutes old — an attacker who reaches this step but doesn't use it
     * promptly is forced back to square one (the real password, or the
     * security questions again), not left with a lingering half-authenticated
     * session. On success, the session is upgraded to the account's real
     * role, landing the user on their dashboard without a second login.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        HttpSession session = httpRequest.getSession(false);
        Instant issuedAt = session != null ? (Instant) session.getAttribute(RESET_ISSUED_AT_ATTRIBUTE) : null;
        if (issuedAt == null || issuedAt.isBefore(Instant.now().minus(RESET_WINDOW_MINUTES, ChronoUnit.MINUTES))) {
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            throw new IllegalArgumentException(
                    "This password reset step has expired. Please start the forgot-password process again.");
        }

        String username = currentUsername();
        securityQuestionService.resetPassword(username, request.newPassword(), request.confirmNewPassword());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Account not found"));

        sessionAuthenticationHelper.establishFullSession(httpRequest, user);

        systemLogService.logAction(user.getUserId(), user.getUsername(), user.getRole(),
                "Password reset via security questions", httpRequest.getRemoteAddr());

        return ResponseEntity.ok(Map.of("message", "Password reset successfully.", "role", user.getRole()));
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void logIfNewlyLocked(String username, HttpServletRequest httpRequest) {
        userRepository.findByUsername(username)
                .filter(u -> Boolean.TRUE.equals(u.getSecurityLocked()))
                .ifPresent(u -> systemLogService.logAction(u.getUserId(), u.getUsername(), u.getRole(),
                        "Account locked due to repeated failed security question attempts",
                        httpRequest.getRemoteAddr()));
    }
}
