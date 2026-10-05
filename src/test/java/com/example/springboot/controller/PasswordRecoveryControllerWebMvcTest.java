package com.example.springboot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SecurityQuestionService;
import com.example.springboot.service.SecurityQuestionService.LookupResult;
import com.example.springboot.service.SessionAuthenticationHelper;
import com.example.springboot.service.SystemLogService;

/**
 * ISO 25010: Security (authenticity and accountability of the password-recovery flow).
 */
@WebMvcTest(PasswordRecoveryController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class PasswordRecoveryControllerWebMvcTest {

    private static final String ISSUED_AT = "PENDING_RESET_ISSUED_AT";
    private static final String STRONG = "Str0ng!Pass";

    @Autowired private MockMvc mvc;

    @MockitoBean private SecurityQuestionService securityQuestionService;
    @MockitoBean private SessionAuthenticationHelper sessionAuthenticationHelper;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private static User user(boolean locked) {
        User u = new User("jdoe", "x", "jdoe@example.com", "REGISTRAR");
        u.setUserId(7);
        u.setSecurityLocked(locked);
        return u;
    }

    private static String answers() {
        return "{\"answers\":[\"a\",\"b\"]}";
    }

    private static String resetBody(String p, String c) {
        return "{\"newPassword\":\"" + p + "\",\"confirmNewPassword\":\"" + c + "\"}";
    }

    private static MockHttpSession sessionIssued(Instant at) {
        MockHttpSession s = new MockHttpSession();
        if (at != null) {
            s.setAttribute(ISSUED_AT, at);
        }
        return s;
    }

    // ---- lookup ----

    @Test
    void lookupWithKnownEmailReturnsTwoQuestionTexts() throws Exception {
        when(securityQuestionService.lookupByEmail("jdoe@example.com"))
                .thenReturn(new LookupResult("jdoe", List.of("Q1?", "Q2?")));

        mvc.perform(post("/api/password-recovery/lookup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"jdoe@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionTexts.length()").value(2))
                .andExpect(jsonPath("$.questionTexts[0]").value("Q1?"));

        verify(sessionAuthenticationHelper).establishRestrictedSession(any(), eq("jdoe"), eq("PENDING_VERIFICATION"));
    }

    @Test
    void lookupWithUnknownEmailReturnsGeneric404() throws Exception {
        when(securityQuestionService.lookupByEmail(anyString()))
                .thenThrow(new NoSuchElementException("No matching account"));

        mvc.perform(post("/api/password-recovery/lookup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No matching account"));

        verifyNoInteractions(sessionAuthenticationHelper);
    }

    @Test
    void lookupRejectsInvalidEmailBody() throws Exception {
        for (String body : List.of("{\"email\":\"\"}", "{\"email\":\"   \"}", "{}")) {
            mvc.perform(post("/api/password-recovery/lookup").contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(securityQuestionService);
    }

    @Test
    void lookupAcceptsMalformedEmailWithoutFormatValidation_currentlyNoEmailConstraint() throws Exception {
        // FINDING: EmailLookupRequest has only @NotBlank (no @Email), so a non-email string is not
        // rejected with 400 at the controller; it reaches the service. Expected: 400 before the service.
        when(securityQuestionService.lookupByEmail("not-an-email"))
                .thenThrow(new NoSuchElementException("No matching account"));

        mvc.perform(post("/api/password-recovery/lookup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isNotFound());

        verify(securityQuestionService).lookupByEmail("not-an-email");
    }

    @Test
    void lookupIsPublic() throws Exception {
        when(securityQuestionService.lookupByEmail(anyString()))
                .thenReturn(new LookupResult("jdoe", List.of("Q1?", "Q2?")));

        mvc.perform(post("/api/password-recovery/lookup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"jdoe@example.com\"}"))
                .andExpect(status().isOk());
    }

    // ---- verify ----

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_VERIFICATION")
    void verifyWithCorrectAnswersUpgradesToPendingReset() throws Exception {
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user(false)));
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/api/password-recovery/verify").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(answers()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verified."));

        verify(sessionAuthenticationHelper).establishRestrictedSession(any(), eq("jdoe"), eq("PENDING_RESET"));
        assertThat(session.getAttribute(ISSUED_AT)).isInstanceOf(Instant.class);
    }

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_VERIFICATION")
    void verifyWithWrongAnswersReturns400AndDoesNotUpgrade() throws Exception {
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user(false)));
        doThrow(new IllegalArgumentException("Incorrect answers"))
                .when(securityQuestionService).verifyAnswers(eq("jdoe"), any());
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/api/password-recovery/verify").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(answers()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect answers"));

        verify(sessionAuthenticationHelper, never()).establishRestrictedSession(any(), anyString(), eq("PENDING_RESET"));
        assertThat(session.getAttribute(ISSUED_AT)).isNull();
    }

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_VERIFICATION")
    void verifyLogsLockoutOnlyOnTransitionToLocked() throws Exception {
        // first read (before): not locked; second read (after failure): locked
        when(userRepository.findByUsername("jdoe"))
                .thenReturn(Optional.of(user(false)), Optional.of(user(true)));
        doThrow(new IllegalArgumentException("locked now"))
                .when(securityQuestionService).verifyAnswers(eq("jdoe"), any());

        mvc.perform(post("/api/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON).content(answers()))
                .andExpect(status().isBadRequest());

        verify(systemLogService).logAction(eq(7), eq("jdoe"), eq("REGISTRAR"),
                eq("Account locked due to repeated failed security question attempts"), anyString());
    }

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_VERIFICATION")
    void verifyAgainstAlreadyLockedAccountDoesNotLogAgain() throws Exception {
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user(true)));
        doThrow(new IllegalArgumentException("locked"))
                .when(securityQuestionService).verifyAnswers(eq("jdoe"), any());

        mvc.perform(post("/api/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON).content(answers()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(systemLogService);
    }

    @Test
    void verifyRequiresPendingVerificationRole() throws Exception {
        mvc.perform(post("/api/password-recovery/verify")
                .contentType(MediaType.APPLICATION_JSON).content(answers()))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/password-recovery/verify")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("boss").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(answers()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(securityQuestionService);
    }

    // ---- reset ----

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_RESET")
    void resetWithinWindowChangesPasswordAndEstablishesFullSession() throws Exception {
        User u = user(false);
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(u));

        mvc.perform(post("/api/password-recovery/reset")
                .session(sessionIssued(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .contentType(MediaType.APPLICATION_JSON).content(resetBody(STRONG, STRONG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully."))
                .andExpect(jsonPath("$.role").value("REGISTRAR"));

        verify(securityQuestionService).resetPassword("jdoe", STRONG, STRONG);
        verify(sessionAuthenticationHelper).establishFullSession(any(), eq(u));
        verify(systemLogService).logAction(eq(7), eq("jdoe"), eq("REGISTRAR"),
                eq("Password reset via security questions"), anyString());
    }

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_RESET")
    void resetAfterWindowIsRejectedAndSessionInvalidated() throws Exception {
        MockHttpSession session = sessionIssued(Instant.now().minus(11, ChronoUnit.MINUTES));

        mvc.perform(post("/api/password-recovery/reset").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(resetBody(STRONG, STRONG)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")));

        assertThat(session.isInvalid()).isTrue();
        verify(securityQuestionService, never()).resetPassword(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_RESET")
    void resetWithNoIssuedAtIsRejected() throws Exception {
        mvc.perform(post("/api/password-recovery/reset").session(sessionIssued(null))
                .contentType(MediaType.APPLICATION_JSON).content(resetBody(STRONG, STRONG)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")));

        verify(securityQuestionService, never()).resetPassword(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_RESET")
    void resetRejectsMismatchedOrBlankPasswords() throws Exception {
        doThrow(new IllegalArgumentException("Passwords do not match"))
                .when(securityQuestionService).resetPassword(anyString(), anyString(), anyString());

        mvc.perform(post("/api/password-recovery/reset")
                .session(sessionIssued(Instant.now()))
                .contentType(MediaType.APPLICATION_JSON).content(resetBody(STRONG, "Other1!Pass")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));

        mvc.perform(post("/api/password-recovery/reset")
                .session(sessionIssued(Instant.now()))
                .contentType(MediaType.APPLICATION_JSON).content(resetBody("", "")))
                .andExpect(status().isBadRequest());

        verify(systemLogService, never()).logAction(anyInt(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_VERIFICATION")
    void resetRequiresPendingResetRole() throws Exception {
        mvc.perform(post("/api/password-recovery/reset")
                .session(sessionIssued(Instant.now()))
                .contentType(MediaType.APPLICATION_JSON).content(resetBody(STRONG, STRONG)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/password-recovery/reset")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("boss").roles("REGISTRAR"))
                .session(sessionIssued(Instant.now()))
                .contentType(MediaType.APPLICATION_JSON).content(resetBody(STRONG, STRONG)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(securityQuestionService);
    }
}
