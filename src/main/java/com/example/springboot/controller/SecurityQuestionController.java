package com.example.springboot.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.EditSecurityAnswersRequest;
import com.example.springboot.dto.SecurityQuestionResponse;
import com.example.springboot.dto.SecurityQuestionTextsResponse;
import com.example.springboot.dto.SetupSecurityAnswersRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.SecurityQuestionService;
import com.example.springboot.service.SessionAuthenticationHelper;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Setup (mandatory, first login) and editing (from the account settings
 * modal) of a user's own 2 security-question slots. The forgot-password
 * flow itself — used while logged out — lives in
 * {@link PasswordRecoveryController}.
 */
@RestController
@RequestMapping("/api/account/security-questions")
public class SecurityQuestionController {

    private final SecurityQuestionService securityQuestionService;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final SessionAuthenticationHelper sessionAuthenticationHelper;

    public SecurityQuestionController(SecurityQuestionService securityQuestionService,
            UserRepository userRepository,
            SystemLogService systemLogService,
            SessionAuthenticationHelper sessionAuthenticationHelper) {
        this.securityQuestionService = securityQuestionService;
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.sessionAuthenticationHelper = sessionAuthenticationHelper;
    }

    @GetMapping("/default-questions")
    public ResponseEntity<List<SecurityQuestionResponse>> getDefaultQuestions() {
        return ResponseEntity.ok(securityQuestionService.getDefaultQuestions());
    }

    /**
     * POST /api/account/security-questions/setup
     * Mandatory first-time setup. On success, the session is upgraded from
     * the restricted {@code ROLE_PENDING_SETUP} to the account's real role,
     * so the user proceeds straight to their dashboard.
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(
            @Valid @RequestBody SetupSecurityAnswersRequest request,
            HttpServletRequest httpRequest) {

        AuthContext ctx = currentAuthContext();
        User user = ctx.user();

        securityQuestionService.setupAnswers(user, request.slots());
        sessionAuthenticationHelper.establishFullSession(httpRequest, user);

        systemLogService.logAction(user.getUserId(), user.getUsername(), user.getRole(),
                "Completed security question setup", httpRequest.getRemoteAddr());

        return ResponseEntity.ok(Map.of("message", "Security questions saved.", "role", user.getRole()));
    }

    @GetMapping
    public ResponseEntity<SecurityQuestionTextsResponse> getCurrentQuestions() {
        User user = currentAuthContext().user();
        return ResponseEntity.ok(
                new SecurityQuestionTextsResponse(securityQuestionService.getCurrentQuestionsForEdit(user.getUserId())));
    }

    /**
     * PUT /api/account/security-questions
     * Re-picks and re-answers both slots. Requires the current password —
     * this is a sensitive, auditable change, same standard as a password
     * change.
     */
    @PutMapping
    public ResponseEntity<Map<String, String>> editAnswers(
            @Valid @RequestBody EditSecurityAnswersRequest request,
            HttpServletRequest httpRequest) {

        AuthContext ctx = currentAuthContext();
        User user = ctx.user();

        securityQuestionService.replaceAnswers(user, request.currentPassword(), request.slots());

        systemLogService.logAction(user.getUserId(), user.getUsername(), ctx.role(),
                "Updated 'forgot password' security questions", httpRequest.getRemoteAddr());

        return ResponseEntity.ok(Map.of("message", "Security questions updated."));
    }

    private record AuthContext(User user, String role) {
    }

    private AuthContext currentAuthContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_UNKNOWN");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Account not found"));
        return new AuthContext(user, role);
    }
}
