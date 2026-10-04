package com.example.springboot.service;

import java.util.Locale;

/**
 * Which status changes the Registrar may make through
 * {@code PUT /api/registrar/student-records/{id}/status}, and what extra input each
 * needs (spec 2026-10-01 SO checklist §4.2). Only moves into or out of Completed or
 * Graduated are restricted; moves among Enrolling / Submitted / Active behave as
 * before. Pure and stateless. To change a rule, edit this class and its mirror
 * {@code statusRule} in {@code js/registrar-students.js} (the server stays the authority).
 */
public final class StudentStatusTransitions {

    private static final String ACTIVE = "active";
    private static final String COMPLETED = "completed";
    private static final String GRADUATED = "graduated";

    /** The outcome for one from → to pair. {@code rejection} is null when the move is allowed. */
    public record Rule(boolean allowed, boolean requiresCompletionDate, boolean requiresReason,
                       boolean clearsCompletionDate, String rejection) {

        static Rule allow() {
            return new Rule(true, false, false, false, null);
        }

        static Rule reject(String rejection) {
            return new Rule(false, false, false, false, rejection);
        }
    }

    private StudentStatusTransitions() {
    }

    public static Rule check(String fromStatus, String toStatus) {
        String from = normalize(fromStatus);
        String to = normalize(toStatus);
        if (from.equals(to)) {
            return Rule.allow();
        }
        if (to.equals(COMPLETED)) {
            if (from.equals(ACTIVE)) {
                return new Rule(true, true, false, false, null);
            }
            if (from.equals(GRADUATED)) {
                return new Rule(true, false, true, false, null);
            }
            return Rule.reject("Only an Active student can be marked Completed.");
        }
        if (to.equals(GRADUATED)) {
            if (from.equals(COMPLETED)) {
                return Rule.allow();
            }
            if (from.equals(ACTIVE)) {
                // Archive escape hatch: digitized records of students who graduated long ago.
                return new Rule(true, true, true, false, null);
            }
            return Rule.reject("Only a Completed student can be marked Graduated.");
        }
        if (from.equals(GRADUATED)) {
            return Rule.reject("A Graduated student can only be moved back to Completed.");
        }
        if (from.equals(COMPLETED)) {
            return to.equals(ACTIVE)
                    ? new Rule(true, false, false, true, null)
                    : Rule.reject("A Completed student can only move back to Active or on to Graduated.");
        }
        return Rule.allow();
    }

    /** Statuses for which completion_date may be set or corrected: Completed and Graduated. */
    public static boolean isCompletedOrGraduated(String status) {
        String normalized = normalize(status);
        return normalized.equals(COMPLETED) || normalized.equals(GRADUATED);
    }

    private static String normalize(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }
}
