package com.example.springboot.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import com.example.springboot.dto.registrar.CreateClassRequest;
import com.example.springboot.dto.registrar.CreateSectionRequest;
import com.example.springboot.dto.registrar.CreateSubjectRequest;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.dto.trainer.SaveGradeRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Bean Validation tests for request DTOs.
 * ISO 25010 characteristic: Functional suitability (correctness of input rules) and Security (input validation).
 */
class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final String STRONG = "Passw0rd!";

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static <T> Set<String> props(T dto) {
        return validator.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static <T> Set<ConstraintViolation<T>> violations(T dto) {
        return validator.validate(dto);
    }

    // ---------- T8-01 ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void loginRequestRejectsBlankFields(String blank) {
        assertTrue(props(new LoginRequest(blank, "secret")).contains("username"));
        assertTrue(props(new LoginRequest("admin", blank)).contains("password"));
        assertTrue(props(new LoginRequest("admin", "secret")).isEmpty());
    }

    // ---------- T8-02 / T8-03 ----------

    private static AdminCreateUserRequest create(String username, String password, String role,
                                                 String email, LocalDate birthdate) {
        return new AdminCreateUserRequest(username, password, role, "Dela Cruz", "Ana", "B",
                email, birthdate);
    }

    static Stream<Arguments> badAdminCreate() {
        LocalDate ok = LocalDate.of(1990, 1, 1);
        return Stream.of(
                Arguments.of(create("user", "password123", "ROLE_ADMIN", "not-an-email", ok), "email"),
                Arguments.of(create("user", "password123", "ROLE_STUDENT", "a@b.com", ok), "role"),
                Arguments.of(create("user", "password123", "", "a@b.com", ok), "role"),
                Arguments.of(create("user", "short", "ROLE_ADMIN", "a@b.com", ok), "password"),
                Arguments.of(create("user", "", "ROLE_ADMIN", "a@b.com", ok), "password"),
                Arguments.of(create("us er", "password123", "ROLE_ADMIN", "a@b.com", ok), "username"),
                Arguments.of(create("", "password123", "ROLE_ADMIN", "a@b.com", ok), "username"),
                Arguments.of(create("user", "password123", "ROLE_ADMIN", "a@b.com", null), "birthdate"),
                Arguments.of(create("user", "password123", "ROLE_ADMIN", "a@b.com",
                        LocalDate.now().plusDays(1)), "birthdate"));
    }

    @ParameterizedTest
    @MethodSource("badAdminCreate")
    void adminCreateUserRejectsBadEmailRoleOrShortPassword(AdminCreateUserRequest dto, String expectedProp) {
        assertTrue(props(dto).contains(expectedProp),
                "expected violation on " + expectedProp + " but got " + props(dto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_REGISTRAR", "ROLE_TRAINER"})
    void adminCreateUserAcceptsValidPayload(String role) {
        assertTrue(violations(create("user.name", "password123", role, "a@b.com",
                LocalDate.of(1990, 1, 1))).isEmpty());
        // email is optional
        assertTrue(violations(create("user.name", "password123", role, null,
                LocalDate.now())).isEmpty());
    }

    // ---------- T8-04 ----------

    @Test
    void updatePasswordRequiresMatchingConfirmation() {
        // Mismatch is NOT a Bean Validation rule; it is enforced in AccountService
        // (newPassword.equals(confirmNewPassword)), not in the DTO.
        assertTrue(violations(new UpdatePasswordRequest("old", STRONG, "Different1!")).isEmpty(),
                "mismatch is a service-level check, not a DTO constraint");
        // Blank / missing confirmation is rejected at the DTO.
        assertTrue(props(new UpdatePasswordRequest("old", STRONG, "")).contains("confirmNewPassword"));
        assertTrue(props(new UpdatePasswordRequest("old", STRONG, null)).contains("confirmNewPassword"));
        assertTrue(props(new UpdatePasswordRequest("", STRONG, STRONG)).contains("currentPassword"));
        // Weak new passwords rejected by the strong policy.
        for (String weak : List.of("Sh0rt!1", "alllowercase1!", "ALLUPPERCASE1!", "NoDigits!!", "NoSpecial11a")) {
            assertTrue(props(new UpdatePasswordRequest("old", weak, weak)).contains("newPassword"),
                    "should reject " + weak);
        }
        assertTrue(violations(new UpdatePasswordRequest("old", STRONG, STRONG)).isEmpty());
    }

    // ---------- T8-05 ----------

    private static SaveGradeRequest grade(BigDecimal fin, String status, BigDecimal reExam, BigDecimal hours) {
        return new SaveGradeRequest("SR2026001", fin, status, reExam, hours);
    }

    @Test
    void saveGradeRejectsOutOfRangeAndNull() {
        BigDecimal h = new BigDecimal("10");
        // hoursRendered is mandatory
        assertTrue(props(grade(new BigDecimal("80"), null, null, null)).contains("hoursRendered"));
        // out of range final percentage
        assertTrue(props(grade(new BigDecimal("-0.01"), null, null, h)).contains("finalPercentage"));
        assertTrue(props(grade(new BigDecimal("100.01"), null, null, h)).contains("finalPercentage"));
        // out of range re-exam
        assertTrue(props(grade(new BigDecimal("50"), null, new BigDecimal("-1"), h)).contains("reExamPercentage"));
        assertTrue(props(grade(new BigDecimal("50"), null, new BigDecimal("101"), h)).contains("reExamPercentage"));
        // out of range hours
        assertTrue(props(grade(new BigDecimal("80"), null, null, new BigDecimal("-1"))).contains("hoursRendered"));
        assertTrue(props(grade(new BigDecimal("80"), null, null, new BigDecimal("100.5"))).contains("hoursRendered"));
        // bad status
        assertTrue(props(grade(null, "X", null, h)).contains("gradeStatus"));
        // blank student id
        assertTrue(props(new SaveGradeRequest(" ", new BigDecimal("80"), null, null, h)).contains("studentId"));
        // boundaries and the null-final/status alternatives are accepted
        assertTrue(violations(grade(BigDecimal.ZERO, null, null, BigDecimal.ZERO)).isEmpty());
        assertTrue(violations(grade(new BigDecimal("100"), null, null, new BigDecimal("100"))).isEmpty());
        for (String s : List.of("C", "FA", "INC", "D")) {
            assertTrue(violations(grade(null, s, null, h)).isEmpty(), s);
        }
    }

    // ---------- T8-06 ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void createSectionAndClassRequireMandatoryFields(String blank) {
        assertEquals(Set.of("sectionCode"),
                props(new CreateSectionRequest(blank, "Name", "B1", "Culinary Arts")));
        assertEquals(Set.of("sectionName"),
                props(new CreateSectionRequest("S1", blank, "B1", "Culinary Arts")));
        assertEquals(Set.of("batchCode"),
                props(new CreateSectionRequest("S1", "Name", blank, "Culinary Arts")));
        assertEquals(Set.of("course"),
                props(new CreateSectionRequest("S1", "Name", "B1", blank)));
        assertTrue(props(new CreateSectionRequest("S1", "Name", "B1", "Culinary Arts")).isEmpty());

        assertEquals(Set.of("sectionCode"), props(new CreateClassRequest(blank, "SUB1", null, "1st")));
        assertEquals(Set.of("subjectCode"), props(new CreateClassRequest("S1", blank, null, "1st")));
        assertEquals(Set.of("semester"), props(new CreateClassRequest("S1", "SUB1", null, blank)));
        assertTrue(props(new CreateClassRequest("S1", "SUB1", null, "1st")).isEmpty());
    }

    @Test
    void createSectionEnforcesLengthLimits() {
        assertTrue(props(new CreateSectionRequest("S".repeat(21), "N", "B", "C")).contains("sectionCode"));
        assertTrue(props(new CreateSectionRequest("S", "N".repeat(26), "B", "C")).contains("sectionName"));
        assertTrue(props(new CreateSectionRequest("S", "N", "B".repeat(21), "C")).contains("batchCode"));
        assertTrue(props(new CreateSectionRequest("S", "N", "B", "C".repeat(101))).contains("course"));
        assertTrue(props(new CreateSectionRequest("S".repeat(20), "N".repeat(25),
                "B".repeat(20), "C".repeat(100))).isEmpty());
    }

    // ---------- T8-07 ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void createSubjectRejectsBlankCodeOrName(String blank) {
        assertTrue(props(new CreateSubjectRequest(blank, "Name", "CORE", null, 1)).contains("subjectCode"));
        assertTrue(props(new CreateSubjectRequest("CODE", blank, "CORE", null, 1)).contains("subjectName"));
    }

    @Test
    void createSubjectRejectsBadTypeAndUnits() {
        assertTrue(props(new CreateSubjectRequest("C", "N", "OTHER", null, 1)).contains("competencyType"));
        assertTrue(props(new CreateSubjectRequest("C", "N", "CORE", null, 0)).contains("units"));
        assertTrue(props(new CreateSubjectRequest("C", "N", "CORE", null, null)).contains("units"));
        assertTrue(violations(new CreateSubjectRequest("C", "N", "BASIC", null, 1)).isEmpty());
    }

    // ---------- T8-08 ----------

    private static SecurityAnswerSlotRequest slot() {
        return new SecurityAnswerSlotRequest(1, null, "answer");
    }

    @Test
    void securityAnswersRequireExactlyTwoSlots() {
        assertTrue(props(new SetupSecurityAnswersRequest(null)).contains("slots"));
        assertTrue(props(new SetupSecurityAnswersRequest(List.of())).contains("slots"));
        assertTrue(props(new SetupSecurityAnswersRequest(List.of(slot()))).contains("slots"));
        assertTrue(props(new SetupSecurityAnswersRequest(List.of(slot(), slot(), slot()))).contains("slots"));
        assertTrue(violations(new SetupSecurityAnswersRequest(List.of(slot(), slot()))).isEmpty());

        assertTrue(props(new VerifyAnswersRequest(List.of("a"))).contains("answers"));
        assertTrue(props(new VerifyAnswersRequest(List.of("a", "b", "c"))).contains("answers"));
        assertTrue(violations(new VerifyAnswersRequest(List.of("a", "b"))).isEmpty());

        // nested slot validation cascades
        assertFalse(violations(new SetupSecurityAnswersRequest(
                List.of(slot(), new SecurityAnswerSlotRequest(1, null, "ab")))).isEmpty());
        // answer length bounds
        assertFalse(violations(new SecurityAnswerSlotRequest(1, null, "x".repeat(101))).isEmpty());
        assertTrue(violations(new SecurityAnswerSlotRequest(1, null, "abc")).isEmpty());
        assertFalse(violations(new SecurityAnswerSlotRequest(1, null, " ")).isEmpty());
    }

    // ---------- T8-09 ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void resetPasswordRequestRejectsBlank(String blank) {
        assertTrue(props(new ResetPasswordRequest(blank, STRONG)).contains("newPassword"));
        assertTrue(props(new ResetPasswordRequest(STRONG, blank)).contains("confirmNewPassword"));
        assertTrue(violations(new ResetPasswordRequest(STRONG, STRONG)).isEmpty());
        assertTrue(props(new ResetPasswordRequest("weakpass", "weakpass")).contains("newPassword"));
    }

    // ---------- T8-10 ----------

    @Test
    void studentRecordUpdateRequestHasNoStudentNumberOrAgeProperty() {
        List<String> names = Stream.of(StudentRecordUpdateRequest.class.getRecordComponents())
                .map(RecordComponent::getName).toList();
        assertFalse(names.contains("studentNumber"));
        assertFalse(names.contains("age"));
        assertTrue(names.contains("studentId"), "sanity: reference-number component is present");
    }

    // ---------- T8-11 ----------

    @Test
    void noRequestDtoExposesAge() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Request$")));
        List<String> scanned = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        scanner.findCandidateComponents("com.example.springboot.dto").forEach(bd -> {
            try {
                Class<?> c = Class.forName(bd.getBeanClassName());
                scanned.add(c.getSimpleName());
                for (var f : c.getDeclaredFields()) {
                    if (f.getName().equalsIgnoreCase("age")) {
                        offenders.add(c.getSimpleName());
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        });
        assertTrue(scanned.size() >= 20, "scan should find the request DTOs, found " + scanned);
        assertTrue(offenders.isEmpty(), "Request DTOs exposing age: " + offenders);
    }
}
