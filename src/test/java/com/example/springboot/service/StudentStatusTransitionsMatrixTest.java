package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.springboot.service.StudentStatusTransitions.Rule;

/**
 * Task 11 (T11-01, T11-02): the full 5x5 lifecycle matrix.
 * ISO 25010 characteristic: Functional suitability (correctness of lifecycle rules).
 */
class StudentStatusTransitionsMatrixTest {

    private static final String E = "Enrolling";
    private static final String S = "Submitted";
    private static final String A = "Active";
    private static final String C = "Completed";
    private static final String G = "Graduated";

    private static Arguments row(String from, String to, boolean allowed, boolean date, boolean reason, boolean clears) {
        return Arguments.of(from, to, allowed, date, reason, clears);
    }

    static Stream<Arguments> matrix() {
        return Stream.of(
                row(E, E, true, false, false, false), row(E, S, true, false, false, false),
                row(E, A, true, false, false, false), row(E, C, false, false, false, false),
                row(E, G, false, false, false, false),
                row(S, E, true, false, false, false), row(S, S, true, false, false, false),
                row(S, A, true, false, false, false), row(S, C, false, false, false, false),
                row(S, G, false, false, false, false),
                row(A, E, true, false, false, false), row(A, S, true, false, false, false),
                row(A, A, true, false, false, false), row(A, C, true, true, false, false),
                row(A, G, true, true, true, false),
                row(C, E, false, false, false, false), row(C, S, false, false, false, false),
                row(C, A, true, false, false, true), row(C, C, true, false, false, false),
                row(C, G, true, false, false, false),
                row(G, E, false, false, false, false), row(G, S, false, false, false, false),
                row(G, A, false, false, false, false), row(G, C, true, false, true, false),
                row(G, G, true, false, false, false));
    }

    /** T11-01 */
    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("matrix")
    void fullStatusMatrix_allowedBlockedAndRequirements(String from, String to, boolean allowed,
            boolean needsDate, boolean needsReason, boolean clearsDate) {
        Rule rule = StudentStatusTransitions.check(from, to);

        assertEquals(allowed, rule.allowed(), "allowed");
        assertEquals(needsDate, rule.requiresCompletionDate(), "requiresCompletionDate");
        assertEquals(needsReason, rule.requiresReason(), "requiresReason");
        assertEquals(clearsDate, rule.clearsCompletionDate(), "clearsCompletionDate");
        if (allowed) {
            assertNull(rule.rejection());
        } else {
            assertNotNull(rule.rejection());
        }
    }

    /** T11-02: same-status is an allowed no-op that demands nothing (verified against current behaviour). */
    @ParameterizedTest
    @MethodSource("sameStatus")
    void sameStatusTransitionIsRejectedOrNoop(String status) {
        Rule rule = StudentStatusTransitions.check(status, status);

        assertEquals(true, rule.allowed());
        assertEquals(false, rule.requiresCompletionDate());
        assertEquals(false, rule.requiresReason());
        assertEquals(false, rule.clearsCompletionDate());
        assertNull(rule.rejection());
    }

    static Stream<String> sameStatus() {
        return Stream.of(E, S, A, C, G);
    }
}
