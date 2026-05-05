package com.example.springboot.service;

import java.time.LocalDate;
import java.time.Period;

/**
 * Utility class for computing age from a birthdate.
 * Centralises the age derivation so every caller uses the same logic.
 */
public final class AgeCalculator {

    private AgeCalculator() {
        // utility class — not instantiable
    }

    /**
     * Calculates the age in completed years between the given birthdate and today.
     *
     * @param birthdate the date of birth; may be {@code null}
     * @return the age in years, or {@code null} if birthdate is {@code null}
     */
    public static Integer calculateAge(LocalDate birthdate) {
        if (birthdate == null) {
            return null;
        }
        return Period.between(birthdate, LocalDate.now()).getYears();
    }
}
