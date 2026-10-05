package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.SecurityAnswerSlotRequest;
import com.example.springboot.dto.SecurityQuestionResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SecurityQuestionService;
import com.example.springboot.service.SessionAuthenticationHelper;
import com.example.springboot.service.SystemLogService;

/**
 * ISO 25010: Security (confidentiality of recovery answers, authenticity of the setup and edit flows).
 */
@WebMvcTest(SecurityQuestionController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SecurityQuestionControllerWebMvcTest {

    private static final String BASE = "/api/account/security-questions";

    @Autowired private MockMvc mvc;

    @MockitoBean private SecurityQuestionService securityQuestionService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private SessionAuthenticationHelper sessionAuthenticationHelper;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void stubUser() {
        user = new User("jdoe", "x", "jdoe@example.com", "TRAINER");
        user.setUserId(7);
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user));
    }

    private static String slot(String qid, String custom, String answer) {
        return "{\"questionId\":" + qid + ",\"customQuestion\":" + custom + ",\"answer\":\"" + answer + "\"}";
    }

    private static String setupBody(String... slots) {
        return "{\"slots\":[" + String.join(",", slots) + "]}";
    }

    private static String editBody(String pwd, String... slots) {
        return "{\"currentPassword\":" + pwd + ",\"slots\":[" + String.join(",", slots) + "]}";
    }

    private static final String VALID = setupBody(slot("1", "null", "Rex"), slot("2", "null", "Blue"));

    // T5-01
    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_SETUP")
    void defaultQuestionsListedForPendingSetup() throws Exception {
        when(securityQuestionService.getDefaultQuestions()).thenReturn(List.of(
                new SecurityQuestionResponse(1, "Pet?"), new SecurityQuestionResponse(2, "Color?")));

        mvc.perform(get(BASE + "/default-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].questionId").value(1))
                .andExpect(jsonPath("$[0].questionText").value("Pet?"))
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    // T5-02
    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_SETUP")
    void setupSavesAnswersAndPromotesSession() throws Exception {
        mvc.perform(post(BASE + "/setup").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TRAINER"));

        verify(securityQuestionService).setupAnswers(eq(user), eq(List.of(
                new SecurityAnswerSlotRequest(1, null, "Rex"),
                new SecurityAnswerSlotRequest(2, null, "Blue"))));
        verify(sessionAuthenticationHelper).establishFullSession(any(), eq(user));
        verify(systemLogService).logAction(eq(7), eq("jdoe"), eq("TRAINER"),
                eq("Completed security question setup"), anyString());
    }

    // T5-03
    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_SETUP")
    void setupRejectsFewerOrMoreThanTwoAnswers() throws Exception {
        String one = setupBody(slot("1", "null", "Rex"));
        String three = setupBody(slot("1", "null", "Rex"), slot("2", "null", "Blue"), slot("3", "null", "Red"));
        for (String body : List.of(one, three)) {
            mvc.perform(post(BASE + "/setup").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.slots").exists());
        }
        verifyNoInteractions(securityQuestionService);
    }

    // T5-04 (the duplicate rule lives in the service; the controller must surface it as 400)
    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_SETUP")
    void setupRejectsDuplicateQuestion() throws Exception {
        doThrow(new IllegalArgumentException("The same default question cannot be selected twice"))
                .when(securityQuestionService).setupAnswers(any(), any());

        mvc.perform(post(BASE + "/setup").contentType(MediaType.APPLICATION_JSON)
                        .content(setupBody(slot("1", "null", "Rex"), slot("1", "null", "Rex2"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The same default question cannot be selected twice"));

        verify(sessionAuthenticationHelper, never()).establishFullSession(any(), any());
        verify(systemLogService, never()).logAction(any(), anyString(), anyString(), anyString(), anyString());
    }

    // T5-05
    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_SETUP")
    void setupRejectsBlankAnswer() throws Exception {
        mvc.perform(post(BASE + "/setup").contentType(MediaType.APPLICATION_JSON)
                        .content(setupBody(slot("1", "null", "   "), slot("2", "null", "Blue"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['slots[0].answer']").exists());

        verifyNoInteractions(securityQuestionService);
    }

    // T5-06
    @Test
    @WithMockUser(username = "jdoe", roles = "TRAINER")
    void getOwnQuestionsRequiresRealRole() throws Exception {
        when(securityQuestionService.getCurrentQuestionsForEdit(7)).thenReturn(List.of("Q1?", "Q2?"));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionTexts[0]").value("Q1?"))
                .andExpect(jsonPath("$.questionTexts[1]").value("Q2?"));
    }

    @Test
    @WithMockUser(username = "jdoe", roles = "PENDING_SETUP")
    void getOwnQuestionsRejectsPendingSetupSession() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isForbidden());
        verifyNoInteractions(securityQuestionService);
    }

    @Test
    void getOwnQuestionsRejectsAnonymous() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(securityQuestionService);
    }

    // T5-07
    @Test
    @WithMockUser(username = "jdoe", roles = "TRAINER")
    void editAnswersRequiresCurrentPassword() throws Exception {
        String slots1 = slot("1", "null", "Rex");
        String slots2 = slot("2", "null", "Blue");

        // Missing / blank password: DTO validation -> 400, service never reached.
        mvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slots\":[" + slots1 + "," + slots2 + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").exists());
        mvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(editBody("\"\"", slots1, slots2)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(securityQuestionService);

        // Wrong password: service throws IllegalArgumentException -> 400 (not 401), nothing logged.
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(securityQuestionService).replaceAnswers(any(), eq("wrong"), any());
        mvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(editBody("\"wrong\"", slots1, slots2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
        verify(systemLogService, never()).logAction(any(), anyString(), anyString(), anyString(), anyString());

        // Correct password: 200 and audited.
        mvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(editBody("\"right\"", slots1, slots2)))
                .andExpect(status().isOk());
        verify(systemLogService).logAction(eq(7), eq("jdoe"), eq("ROLE_TRAINER"),
                eq("Updated 'forgot password' security questions"), anyString());
    }
}
