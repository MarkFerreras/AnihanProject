package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class AgeCalculatorTest {

    @Test
    void calculateAgeReturnsZeroForNullBirthdate() {
        assertEquals(0, AgeCalculator.calculateAge(null));
    }

    @Test
    void calculateAgeReturnsZeroForTodaysBirthdate() {
        assertEquals(0, AgeCalculator.calculateAge(LocalDate.now()));
    }

    @Test
    void calculateAgeReturnsCorrectYearsForKnownDate() {
        // Use a date far enough in the past that the year count is stable
        // regardless of what day the test runs on.
        LocalDate birthdate = LocalDate.of(2000, 1, 1);
        int expectedMinAge = LocalDate.now().getYear() - 2000 - 1; // before birthday this year
        int expectedMaxAge = LocalDate.now().getYear() - 2000;     // after birthday this year

        int age = AgeCalculator.calculateAge(birthdate);

        assertTrue(age >= expectedMinAge && age <= expectedMaxAge,
                "Expected age between " + expectedMinAge + " and " + expectedMaxAge + ", got " + age);
    }

    @Test
    void calculateAgeReturnsNegativeForFutureBirthdate() {
        LocalDate futureDate = LocalDate.now().plusYears(5);
        int age = AgeCalculator.calculateAge(futureDate);

        assertTrue(age < 0, "Expected negative age for future birthdate, got " + age);
    }

    @Test
    void calculateAgeHandlesBirthdayNotYetReachedThisYear() {
        // Pick a date that is guaranteed to be "next month" relative to now,
        // but in a past year, so the birthday hasn't happened yet this year.
        LocalDate now = LocalDate.now();
        LocalDate birthdate = now.minusYears(20).plusMonths(1);

        // Birthday hasn't occurred yet this year, so age should be 19
        assertEquals(19, AgeCalculator.calculateAge(birthdate));
    }

    @Test
    void calculateAgeHandlesBirthdayAlreadyPassedThisYear() {
        // Pick a date that is guaranteed to be "last month" relative to now,
        // in a past year, so the birthday has already happened this year.
        LocalDate now = LocalDate.now();
        LocalDate birthdate = now.minusYears(20).minusMonths(1);

        // Birthday already occurred this year, so age should be 20
        assertEquals(20, AgeCalculator.calculateAge(birthdate));
    }
}
