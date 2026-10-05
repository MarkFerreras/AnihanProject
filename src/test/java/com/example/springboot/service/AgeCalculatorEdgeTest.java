package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Task 11 (T11-06): birthday edge cases for the age derivation.
 * ISO 25010 characteristic: Functional suitability (correctness of derived age).
 */
class AgeCalculatorEdgeTest {

    @Test
    void ageCalculatorBirthdayEdges_birthdayToday() {
        assertEquals(20, AgeCalculator.calculateAge(LocalDate.now().minusYears(20)));
    }

    @Test
    void ageCalculatorBirthdayEdges_birthdayTomorrowIsStillOneYearYounger() {
        assertEquals(19, AgeCalculator.calculateAge(LocalDate.now().minusYears(20).plusDays(1)));
    }

    @Test
    void ageCalculatorBirthdayEdges_birthdayYesterday() {
        assertEquals(20, AgeCalculator.calculateAge(LocalDate.now().minusYears(20).minusDays(1)));
    }

    @Test
    void ageCalculatorBirthdayEdges_leapDayBirthdate() {
        LocalDate today = LocalDate.now();
        // java.time.Period reaches a 29 Feb birthday on 1 Mar in non-leap years.
        LocalDate anniversary = today.isLeapYear()
                ? LocalDate.of(today.getYear(), 2, 29)
                : LocalDate.of(today.getYear(), 3, 1);
        int expected = today.getYear() - 2000 - (today.isBefore(anniversary) ? 1 : 0);

        assertEquals(expected, AgeCalculator.calculateAge(LocalDate.of(2000, 2, 29)));
    }

    @Test
    void ageCalculatorBirthdayEdges_futureBirthdateGivesNegativeAge() {
        // Current behaviour: no validation here; a future date yields a negative age, not null.
        assertEquals(-1, AgeCalculator.calculateAge(LocalDate.now().plusYears(1)));
    }

    @Test
    void ageCalculatorBirthdayEdges_nullGivesNull() {
        assertNull(AgeCalculator.calculateAge(null));
    }
}
