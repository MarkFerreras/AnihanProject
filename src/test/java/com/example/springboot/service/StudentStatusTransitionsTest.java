package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.springboot.service.StudentStatusTransitions.Rule;

/** Pins the status-move table in spec 2026-10-01 SO checklist §4.2. */
class StudentStatusTransitionsTest {

    private static Rule check(String from, String to) {
        return StudentStatusTransitions.check(from, to);
    }

    @Test
    void activeToCompletedNeedsACompletionDateOnly() {
        Rule rule = check("Active", "Completed");
        assertTrue(rule.allowed());
        assertTrue(rule.requiresCompletionDate());
        assertFalse(rule.requiresReason());
        assertFalse(rule.clearsCompletionDate());
        assertNull(rule.rejection());
    }

    @Test
    void onlyAnActiveStudentMayBecomeCompletedFromBelow() {
        for (String from : List.of("Enrolling", "Submitted")) {
            Rule rule = check(from, "Completed");
            assertFalse(rule.allowed(), from + " -> Completed must be rejected");
            assertEquals("Only an Active student can be marked Completed.", rule.rejection());
        }
    }

    @Test
    void completedBackToActiveClearsTheCompletionDate() {
        Rule rule = check("Completed", "Active");
        assertTrue(rule.allowed());
        assertTrue(rule.clearsCompletionDate());
        assertFalse(rule.requiresCompletionDate());
        assertFalse(rule.requiresReason());
    }

    @Test
    void completedToGraduatedNeedsNothingExtra() {
        Rule rule = check("Completed", "Graduated");
        assertTrue(rule.allowed());
        assertFalse(rule.requiresCompletionDate());
        assertFalse(rule.requiresReason());
        assertFalse(rule.clearsCompletionDate());
    }

    @Test
    void activeToGraduatedIsTheArchiveEscapeHatchAndNeedsADateAndAReason() {
        Rule rule = check("Active", "Graduated");
        assertTrue(rule.allowed());
        assertTrue(rule.requiresCompletionDate());
        assertTrue(rule.requiresReason());
        assertFalse(rule.clearsCompletionDate());
    }

    @Test
    void graduatedBackToCompletedNeedsAReasonAndKeepsTheDate() {
        Rule rule = check("Graduated", "Completed");
        assertTrue(rule.allowed());
        assertTrue(rule.requiresReason());
        assertFalse(rule.requiresCompletionDate());
        assertFalse(rule.clearsCompletionDate());
    }

    @Test
    void graduatedCannotMoveAnywhereButCompleted() {
        for (String to : List.of("Active", "Enrolling")) {
            Rule rule = check("Graduated", to);
            assertFalse(rule.allowed(), "Graduated -> " + to + " must be rejected");
            assertEquals("A Graduated student can only be moved back to Completed.", rule.rejection());
        }
    }

    @Test
    void onlyACompletedOrActiveStudentMayBecomeGraduated() {
        for (String from : List.of("Enrolling", "Submitted")) {
            Rule rule = check(from, "Graduated");
            assertFalse(rule.allowed(), from + " -> Graduated must be rejected");
            assertEquals("Only a Completed student can be marked Graduated.", rule.rejection());
        }
    }

    @Test
    void completedCannotGoBackToEnrolling() {
        Rule rule = check("Completed", "Enrolling");
        assertFalse(rule.allowed());
        assertEquals("A Completed student can only move back to Active or on to Graduated.", rule.rejection());
    }

    @Test
    void movesAmongEnrollingSubmittedAndActiveAreUnchanged() {
        List<String[]> pairs = List.of(
                new String[] { "Enrolling", "Active" }, new String[] { "Active", "Enrolling" },
                new String[] { "Submitted", "Active" }, new String[] { "Submitted", "Enrolling" });
        for (String[] pair : pairs) {
            Rule rule = check(pair[0], pair[1]);
            assertTrue(rule.allowed(), pair[0] + " -> " + pair[1]);
            assertFalse(rule.requiresCompletionDate());
            assertFalse(rule.requiresReason());
            assertFalse(rule.clearsCompletionDate());
        }
    }

    @Test
    void keepingTheSameStatusIsAllowedAndNeedsNothing() {
        for (String status : List.of("Completed", "Graduated", "Active")) {
            Rule rule = check(status, status);
            assertTrue(rule.allowed(), status);
            assertFalse(rule.requiresCompletionDate());
            assertFalse(rule.requiresReason());
            assertFalse(rule.clearsCompletionDate());
        }
    }

    @Test
    void statusesAreMatchedIgnoringCaseAndSurroundingSpaces() {
        assertTrue(check(" active ", "COMPLETED").requiresCompletionDate());
        assertFalse(check("graduated", " Active").allowed());
    }

    @Test
    void completionDateIsEditableOnlyForCompletedOrGraduated() {
        assertTrue(StudentStatusTransitions.isCompletedOrGraduated("Completed"));
        assertTrue(StudentStatusTransitions.isCompletedOrGraduated(" graduated "));
        for (String status : new String[] { "Active", "Enrolling", "Submitted", null }) {
            assertFalse(StudentStatusTransitions.isCompletedOrGraduated(status), String.valueOf(status));
        }
    }
}
