package com.example.springboot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.SecurityAnswerSlotRequest;
import com.example.springboot.dto.SecurityQuestionResponse;
import com.example.springboot.model.SecurityQuestion;
import com.example.springboot.model.User;
import com.example.springboot.model.UserSecurityAnswer;
import com.example.springboot.repository.SecurityQuestionRepository;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.repository.UserSecurityAnswerRepository;

/**
 * Core logic for the "forgot password" security-questions feature: setup,
 * editing, the forgot-password email lookup, answer verification with its
 * lockout state machine, and the final password reset.
 *
 * <p>This service never returns a stored answer — only question text — and
 * never trusts a client-supplied user id for the forgot-password flow; the
 * controller resolves "which account" purely from the authenticated
 * username on the request (see {@link SessionAuthenticationHelper}).
 */
@Service
public class SecurityQuestionService {

    /** A user has finished setup once both slots are saved. */
    public static final int REQUIRED_ANSWER_COUNT = 2;

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_DECAY_MINUTES = 15;

    private final SecurityQuestionRepository securityQuestionRepository;
    private final UserSecurityAnswerRepository userSecurityAnswerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityQuestionService(SecurityQuestionRepository securityQuestionRepository,
            UserSecurityAnswerRepository userSecurityAnswerRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.securityQuestionRepository = securityQuestionRepository;
        this.userSecurityAnswerRepository = userSecurityAnswerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<SecurityQuestionResponse> getDefaultQuestions() {
        return securityQuestionRepository.findByActiveTrue().stream()
                .map(SecurityQuestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isSetupComplete(Integer userId) {
        return userSecurityAnswerRepository.countByUserUserId(userId) >= REQUIRED_ANSWER_COUNT;
    }

    /** First-time setup — fails if the user already has answers on file. */
    @Transactional
    public void setupAnswers(User user, List<SecurityAnswerSlotRequest> slots) {
        if (isSetupComplete(user.getUserId())) {
            throw new IllegalArgumentException(
                    "Security questions are already set up. Use the account settings page to change them.");
        }
        saveAnswers(user, slots);
    }

    @Transactional(readOnly = true)
    public List<String> getCurrentQuestionsForEdit(Integer userId) {
        List<UserSecurityAnswer> answers = userSecurityAnswerRepository.findByUserUserIdOrderBySlot(userId);
        return answers.stream().map(UserSecurityAnswer::resolveQuestionText).toList();
    }

    /** Re-picking and re-answering both slots — requires the current password. */
    @Transactional
    public void replaceAnswers(User user, String currentPassword, List<SecurityAnswerSlotRequest> slots) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        userSecurityAnswerRepository.deleteByUserUserId(user.getUserId());
        userSecurityAnswerRepository.flush();
        saveAnswers(user, slots);
    }

    private void saveAnswers(User user, List<SecurityAnswerSlotRequest> slots) {
        validateSlots(slots);

        int slotNumber = 1;
        for (SecurityAnswerSlotRequest slot : slots) {
            UserSecurityAnswer answer = new UserSecurityAnswer();
            answer.setUser(user);
            answer.setSlot(slotNumber++);
            if (slot.questionId() != null) {
                SecurityQuestion question = securityQuestionRepository.findById(slot.questionId())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown security question selected"));
                answer.setQuestion(question);
            } else {
                answer.setCustomQuestion(slot.customQuestion().trim());
            }
            answer.setAnswerHash(passwordEncoder.encode(normalizeAnswer(slot.answer())));
            userSecurityAnswerRepository.save(answer);
        }
    }

    private void validateSlots(List<SecurityAnswerSlotRequest> slots) {
        List<SecurityQuestionResponse> defaults = getDefaultQuestions();

        boolean seenDefaultQuestionId = false;
        Integer firstDefaultQuestionId = null;

        for (SecurityAnswerSlotRequest slot : slots) {
            boolean hasDefault = slot.questionId() != null;
            boolean hasCustom = slot.customQuestion() != null && !slot.customQuestion().isBlank();

            if (hasDefault == hasCustom) {
                throw new IllegalArgumentException(
                        "Each security question must be either a default question or a custom one, not both or neither");
            }

            if (hasDefault) {
                if (seenDefaultQuestionId && slot.questionId().equals(firstDefaultQuestionId)) {
                    throw new IllegalArgumentException("The same default question cannot be selected twice");
                }
                seenDefaultQuestionId = true;
                firstDefaultQuestionId = slot.questionId();
            } else {
                String normalizedCustom = normalizeForComparison(slot.customQuestion());
                boolean matchesDefault = defaults.stream()
                        .anyMatch(q -> normalizeForComparison(q.questionText()).equals(normalizedCustom));
                if (matchesDefault) {
                    throw new IllegalArgumentException(
                            "This matches one of the default questions — please select it from the list instead of retyping it");
                }
            }
        }
    }

    /** trim + collapse whitespace + strip trailing punctuation + lowercase. */
    private String normalizeForComparison(String text) {
        return text.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\p{Punct}]+$", "")
                .toLowerCase();
    }

    /** Case-insensitive comparison: trim + lowercase, nothing else restricted. */
    private String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase();
    }

    // ----- Forgot-password flow -----

    public record LookupResult(String username, List<String> questionTexts) {
    }

    @Transactional(readOnly = true)
    public LookupResult lookupByEmail(String email) {
        User user = userRepository.findByEmail(email.trim())
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()))
                .filter(u -> !Boolean.TRUE.equals(u.getSecurityLocked()))
                .orElseThrow(SecurityQuestionService::accountNotFound);

        List<UserSecurityAnswer> answers = userSecurityAnswerRepository.findByUserUserIdOrderBySlot(user.getUserId());
        if (answers.size() < REQUIRED_ANSWER_COUNT) {
            // Setup was never completed — nothing to verify against. Handled
            // the same as "not found" so this doesn't leak setup status.
            throw accountNotFound();
        }

        List<String> questionTexts = answers.stream().map(UserSecurityAnswer::resolveQuestionText).toList();
        return new LookupResult(user.getUsername(), questionTexts);
    }

    private static NoSuchElementException accountNotFound() {
        return new NoSuchElementException("We couldn't find an account matching that information.");
    }

    /**
     * Checks both answers (AND logic) against the account identified by the
     * restricted {@code PENDING_VERIFICATION} session's username — never a
     * client-supplied id. Implements the agreed lockout state machine:
     * 3 wrong attempts locks the account (only an admin can clear it); the
     * failed-attempt count decays back to 0 after 15 minutes measured from
     * the first failure in the streak, but only while not yet locked; a
     * correct answer resets the count immediately.
     */
    @Transactional
    public void verifyAnswers(String username, List<String> answers) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Account not found"));

        if (Boolean.TRUE.equals(user.getSecurityLocked())) {
            throw new IllegalArgumentException(
                    "Account is locked due to repeated failed security question attempts. "
                    + "Contact your administrator.");
        }

        if (user.getFailedSecurityAttempts() > 0 && user.getSecurityLockoutStartedAt() != null
                && user.getSecurityLockoutStartedAt().isBefore(LocalDateTime.now().minusMinutes(LOCKOUT_DECAY_MINUTES))) {
            user.setFailedSecurityAttempts(0);
            user.setSecurityLockoutStartedAt(null);
        }

        List<UserSecurityAnswer> stored = userSecurityAnswerRepository.findByUserUserIdOrderBySlot(user.getUserId());
        boolean allCorrect = stored.size() == answers.size() && allMatch(stored, answers);

        if (allCorrect) {
            user.setFailedSecurityAttempts(0);
            user.setSecurityLockoutStartedAt(null);
            userRepository.save(user);
            return;
        }

        if (user.getFailedSecurityAttempts() == 0) {
            user.setSecurityLockoutStartedAt(LocalDateTime.now());
        }
        user.setFailedSecurityAttempts(user.getFailedSecurityAttempts() + 1);
        if (user.getFailedSecurityAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setSecurityLocked(true);
        }
        userRepository.save(user);

        // Never reveal which answer was wrong or how many attempts remain.
        throw new IllegalArgumentException("One or more answers were incorrect.");
    }

    private boolean allMatch(List<UserSecurityAnswer> stored, List<String> answers) {
        for (int i = 0; i < stored.size(); i++) {
            String normalized = normalizeAnswer(answers.get(i));
            if (!passwordEncoder.matches(normalized, stored.get(i).getAnswerHash())) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public void resetPassword(String username, String newPassword, String confirmNewPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Account not found"));

        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
