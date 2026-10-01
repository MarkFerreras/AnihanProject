package com.example.springboot.service;

import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Derives a {@code courses.course_code} from a course name typed by the registrar, e.g.
 * "Culinary Arts and Restaurant Services" -> "CARS". Pure (no Spring, no DB): the caller
 * supplies an {@code isTaken} predicate so collisions get a numeric suffix (CARS2, CARS3...).
 */
public final class CourseCodeGenerator {

    /** courses.course_code is VARCHAR(20). */
    static final int MAX_CODE_LENGTH = 20;
    /** Leaves room for a suffix of up to 3 digits. */
    private static final int MAX_BASE_LENGTH = MAX_CODE_LENGTH - 3;

    private static final Set<String> STOP_WORDS =
            Set.of("a", "an", "and", "at", "for", "in", "of", "on", "the", "to", "with");

    private CourseCodeGenerator() {
    }

    public static String baseCode(String courseName) {
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name is required.");
        }
        String trimmed = courseName.trim();

        StringBuilder initials = new StringBuilder();
        for (String word : trimmed.split("[^\\p{L}\\p{N}]+")) {
            if (word.isEmpty() || STOP_WORDS.contains(word.toLowerCase(Locale.ROOT))) {
                continue;
            }
            initials.append(Character.toUpperCase(word.charAt(0)));
        }

        String code = initials.length() >= 2
                ? initials.toString()
                : trimmed.replaceAll("[^\\p{L}\\p{N}]", "").toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Course name must contain letters or digits.");
        }
        return code.length() > MAX_BASE_LENGTH ? code.substring(0, MAX_BASE_LENGTH) : code;
    }

    public static String uniqueCode(String courseName, Predicate<String> isTaken) {
        String base = baseCode(courseName);
        if (!isTaken.test(base)) {
            return base;
        }
        for (int n = 2; n < 1000; n++) {
            String candidate = base + n;
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique course code for: " + courseName);
    }
}
