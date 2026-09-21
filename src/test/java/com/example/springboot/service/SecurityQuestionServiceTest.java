package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.springboot.dto.SecurityAnswerSlotRequest;
import com.example.springboot.model.SecurityQuestion;
import com.example.springboot.model.User;
import com.example.springboot.model.UserSecurityAnswer;
import com.example.springboot.repository.SecurityQuestionRepository;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.repository.UserSecurityAnswerRepository;

@ExtendWith(MockitoExtension.class)
class SecurityQuestionServiceTest {

    @Mock
    private SecurityQuestionRepository securityQuestionRepository;

    @Mock
    private UserSecurityAnswerRepository userSecurityAnswerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SecurityQuestionService service;

    private SecurityQuestion petQuestion;
    private SecurityQuestion colorQuestion;

    @BeforeEach
    void setUpDefaults() {
        petQuestion = buildQuestion(1, "What is the name of your pet?");
        colorQuestion = buildQuestion(2, "What is your favorite color");
        lenient().when(securityQuestionRepository.findByActiveTrue())
                .thenReturn(List.of(petQuestion, colorQuestion));
        lenient().when(securityQuestionRepository.findById(1)).thenReturn(Optional.of(petQuestion));
        lenient().when(securityQuestionRepository.findById(2)).thenReturn(Optional.of(colorQuestion));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    }

    // ========== setupAnswers ==========

    @Test
    void setupAnswersSavesBothSlotsWhenDistinctDefaults() {
        User user = buildUser(5, "trainer");
        when(userSecurityAnswerRepository.countByUserUserId(5)).thenReturn(0L);

        service.setupAnswers(user, List.of(
                new SecurityAnswerSlotRequest(1, null, "Rex"),
                new SecurityAnswerSlotRequest(2, null, "Blue")
        ));

        verify(userSecurityAnswerRepository, times(2)).save(any(UserSecurityAnswer.class));
    }

    @Test
    void setupAnswersThrowsWhenAlreadyComplete() {
        User user = buildUser(5, "trainer");
        when(userSecurityAnswerRepository.countByUserUserId(5)).thenReturn(2L);

        assertThrows(IllegalArgumentException.class, () -> service.setupAnswers(user, List.of(
                new SecurityAnswerSlotRequest(1, null, "Rex"),
                new SecurityAnswerSlotRequest(2, null, "Blue")
        )));

        verify(userSecurityAnswerRepository, never()).save(any());
    }

    @Test
    void setupAnswersRejectsDuplicateDefaultQuestion() {
        User user = buildUser(5, "trainer");
        when(userSecurityAnswerRepository.countByUserUserId(5)).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> service.setupAnswers(user, List.of(
                new SecurityAnswerSlotRequest(1, null, "Rex"),
                new SecurityAnswerSlotRequest(1, null, "Rex again")
        )));

        verify(userSecurityAnswerRepository, never()).save(any());
    }

    @Test
    void setupAnswersRejectsCustomQuestionMatchingDefaultWordForWord() {
        User user = buildUser(5, "trainer");
        when(userSecurityAnswerRepository.countByUserUserId(5)).thenReturn(0L);

        // Differs only by case and a trailing period from the default "What is your favorite color"
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.setupAnswers(user, List.of(
                new SecurityAnswerSlotRequest(1, null, "Rex"),
                new SecurityAnswerSlotRequest(null, "WHAT IS YOUR FAVORITE COLOR.", "Blue")
        )));

        assertTrue(ex.getMessage().contains("default questions"));
        verify(userSecurityAnswerRepository, never()).save(any());
    }

    @Test
    void setupAnswersAllowsTwoCustomQuestions() {
        User user = buildUser(5, "trainer");
        when(userSecurityAnswerRepository.countByUserUserId(5)).thenReturn(0L);

        service.setupAnswers(user, List.of(
                new SecurityAnswerSlotRequest(null, "What was your first phone?", "Nokia"),
                new SecurityAnswerSlotRequest(null, "What street did you grow up on?", "Main St")
        ));

        verify(userSecurityAnswerRepository, times(2)).save(any(UserSecurityAnswer.class));
    }

    @Test
    void setupAnswersRejectsSlotWithBothQuestionIdAndCustomQuestion() {
        User user = buildUser(5, "trainer");
        when(userSecurityAnswerRepository.countByUserUserId(5)).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> service.setupAnswers(user, List.of(
                new SecurityAnswerSlotRequest(1, "Also custom", "Rex"),
                new SecurityAnswerSlotRequest(2, null, "Blue")
        )));
    }

    // ========== replaceAnswers ==========

    @Test
    void replaceAnswersThrowsWhenCurrentPasswordWrong() {
        User user = buildUser(5, "trainer");
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.replaceAnswers(user, "wrong", List.of(
                new SecurityAnswerSlotRequest(1, null, "Rex"),
                new SecurityAnswerSlotRequest(2, null, "Blue")
        )));

        verify(userSecurityAnswerRepository, never()).deleteByUserUserId(any());
        verify(userSecurityAnswerRepository, never()).save(any());
    }

    @Test
    void replaceAnswersDeletesThenInsertsWhenPasswordCorrect() {
        User user = buildUser(5, "trainer");
        when(passwordEncoder.matches("correct", user.getPassword())).thenReturn(true);

        service.replaceAnswers(user, "correct", List.of(
                new SecurityAnswerSlotRequest(1, null, "Rex"),
                new SecurityAnswerSlotRequest(2, null, "Blue")
        ));

        verify(userSecurityAnswerRepository).deleteByUserUserId(5);
        verify(userSecurityAnswerRepository, times(2)).save(any(UserSecurityAnswer.class));
    }

    // ========== lookupByEmail ==========

    @Test
    void lookupByEmailThrowsWhenNoAccountMatches() {
        when(userRepository.findByEmail("ghost@anihan.local")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.lookupByEmail("ghost@anihan.local"));
    }

    @Test
    void lookupByEmailThrowsWhenAccountDisabled() {
        User user = buildUser(5, "trainer");
        user.setEnabled(false);
        when(userRepository.findByEmail("trainer@anihan.local")).thenReturn(Optional.of(user));

        assertThrows(NoSuchElementException.class, () -> service.lookupByEmail("trainer@anihan.local"));
    }

    @Test
    void lookupByEmailThrowsWhenAccountLocked() {
        User user = buildUser(5, "trainer");
        user.setSecurityLocked(true);
        when(userRepository.findByEmail("trainer@anihan.local")).thenReturn(Optional.of(user));

        assertThrows(NoSuchElementException.class, () -> service.lookupByEmail("trainer@anihan.local"));
    }

    @Test
    void lookupByEmailThrowsWhenSetupIncomplete() {
        User user = buildUser(5, "trainer");
        when(userRepository.findByEmail("trainer@anihan.local")).thenReturn(Optional.of(user));
        when(userSecurityAnswerRepository.findByUserUserIdOrderBySlot(5)).thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> service.lookupByEmail("trainer@anihan.local"));
    }

    @Test
    void lookupByEmailReturnsQuestionTextsInSlotOrder() {
        User user = buildUser(5, "trainer");
        when(userRepository.findByEmail("trainer@anihan.local")).thenReturn(Optional.of(user));
        when(userSecurityAnswerRepository.findByUserUserIdOrderBySlot(5)).thenReturn(List.of(
                buildStoredAnswer(user, petQuestion, null, "hash1", 1),
                buildStoredAnswer(user, null, "Custom question", "hash2", 2)
        ));

        SecurityQuestionService.LookupResult result = service.lookupByEmail("trainer@anihan.local");

        assertEquals("trainer", result.username());
        assertEquals(List.of("What is the name of your pet?", "Custom question"), result.questionTexts());
    }

    // ========== verifyAnswers (lockout state machine) ==========

    @Test
    void verifyAnswersThrowsImmediatelyWhenAlreadyLocked() {
        User user = buildUser(5, "trainer");
        user.setSecurityLocked(true);
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> service.verifyAnswers("trainer", List.of("a", "b")));

        verify(userSecurityAnswerRepository, never()).findByUserUserIdOrderBySlot(any());
    }

    @Test
    void verifyAnswersSucceedsAndResetsCounter() {
        User user = buildUser(5, "trainer");
        user.setFailedSecurityAttempts(2);
        user.setSecurityLockoutStartedAt(LocalDateTime.now());
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));
        when(userSecurityAnswerRepository.findByUserUserIdOrderBySlot(5)).thenReturn(List.of(
                buildStoredAnswer(user, petQuestion, null, "hash1", 1),
                buildStoredAnswer(user, colorQuestion, null, "hash2", 2)
        ));
        when(passwordEncoder.matches("rex", "hash1")).thenReturn(true);
        when(passwordEncoder.matches("blue", "hash2")).thenReturn(true);

        service.verifyAnswers("trainer", List.of("Rex", "Blue"));

        assertEquals(0, user.getFailedSecurityAttempts());
        assertEquals(null, user.getSecurityLockoutStartedAt());
        assertFalse(user.getSecurityLocked());
    }

    @Test
    void verifyAnswersIncrementsCounterOnWrongAnswer() {
        User user = buildUser(5, "trainer");
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));
        when(userSecurityAnswerRepository.findByUserUserIdOrderBySlot(5)).thenReturn(List.of(
                buildStoredAnswer(user, petQuestion, null, "hash1", 1),
                buildStoredAnswer(user, colorQuestion, null, "hash2", 2)
        ));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.verifyAnswers("trainer", List.of("wrong", "wrong")));

        assertEquals(1, user.getFailedSecurityAttempts());
        assertFalse(user.getSecurityLocked());
    }

    @Test
    void verifyAnswersLocksAccountOnThirdFailure() {
        User user = buildUser(5, "trainer");
        user.setFailedSecurityAttempts(2);
        user.setSecurityLockoutStartedAt(LocalDateTime.now());
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));
        when(userSecurityAnswerRepository.findByUserUserIdOrderBySlot(5)).thenReturn(List.of(
                buildStoredAnswer(user, petQuestion, null, "hash1", 1),
                buildStoredAnswer(user, colorQuestion, null, "hash2", 2)
        ));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.verifyAnswers("trainer", List.of("wrong", "wrong")));

        assertEquals(3, user.getFailedSecurityAttempts());
        assertTrue(user.getSecurityLocked());
    }

    @Test
    void verifyAnswersDecaysCounterAfter15MinutesSinceFirstFailure() {
        User user = buildUser(5, "trainer");
        user.setFailedSecurityAttempts(2);
        user.setSecurityLockoutStartedAt(LocalDateTime.now().minusMinutes(20));
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));
        when(userSecurityAnswerRepository.findByUserUserIdOrderBySlot(5)).thenReturn(List.of(
                buildStoredAnswer(user, petQuestion, null, "hash1", 1),
                buildStoredAnswer(user, colorQuestion, null, "hash2", 2)
        ));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Decays the stale 2-in-a-row back to 0 first, so this 3rd (chronologically)
        // wrong answer is treated as failure #1 of a new streak, not a lockout.
        assertThrows(IllegalArgumentException.class, () -> service.verifyAnswers("trainer", List.of("wrong", "wrong")));

        assertEquals(1, user.getFailedSecurityAttempts());
        assertFalse(user.getSecurityLocked());
    }

    // ========== resetPassword ==========

    @Test
    void resetPasswordThrowsWhenPasswordsDontMatch() {
        User user = buildUser(5, "trainer");
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("trainer", "NewPass1!", "Different1!"));
    }

    @Test
    void resetPasswordThrowsWhenSameAsCurrentPassword() {
        User user = buildUser(5, "trainer");
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewPass1!", user.getPassword())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("trainer", "NewPass1!", "NewPass1!"));
    }

    @Test
    void resetPasswordUpdatesPasswordAndTimestamp() {
        User user = buildUser(5, "trainer");
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewPass1!", user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("newHash");

        service.resetPassword("trainer", "NewPass1!", "NewPass1!");

        assertEquals("newHash", user.getPassword());
        assertTrue(user.getPasswordChangedAt() != null);
    }

    // ========== helpers ==========

    private User buildUser(Integer id, String username) {
        User user = new User(username, "oldHash", username + "@anihan.local", "ROLE_TRAINER");
        user.setUserId(id);
        return user;
    }

    private SecurityQuestion buildQuestion(Integer id, String text) {
        SecurityQuestion question = new SecurityQuestion();
        question.setQuestionId(id);
        question.setQuestionText(text);
        question.setActive(true);
        return question;
    }

    private UserSecurityAnswer buildStoredAnswer(User user, SecurityQuestion question, String customQuestion,
            String answerHash, int slot) {
        UserSecurityAnswer answer = new UserSecurityAnswer();
        answer.setUser(user);
        answer.setQuestion(question);
        answer.setCustomQuestion(customQuestion);
        answer.setAnswerHash(answerHash);
        answer.setSlot(slot);
        return answer;
    }
}
