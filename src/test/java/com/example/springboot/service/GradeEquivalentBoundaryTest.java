package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Task 11 (T11-03): transmutation boundaries at every band edge.
 * ISO 25010 characteristic: Functional suitability (correctness of grade maths).
 */
class GradeEquivalentBoundaryTest {

    /** T11-03 */
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "100.01, 1.00", "100, 1.00", "99, 1.00", "98.99, 1.25", "96, 1.25", "95.99, 1.50",
            "93, 1.50", "92.99, 1.75", "90, 1.75", "89.99, 2.00", "87, 2.00", "86.99, 2.25",
            "84, 2.25", "83.99, 2.50", "81, 2.50", "80.99, 2.75", "78, 2.75", "77.99, 3.00",
            "75, 3.00", "74.99, 4.00", "70, 4.00", "69.99, 5.00", "0, 5.00", "-0.01, 5.00", "-50, 5.00"
    })
    void gradeEquivalentBoundaries(String percentage, String expected) {
        assertEquals(new BigDecimal(expected), GradeEquivalent.toEquivalent(new BigDecimal(percentage)));
    }

    @Test
    void gradeEquivalentBoundaries_nullPercentageGivesNull() {
        assertNull(GradeEquivalent.toEquivalent(null));
    }
}
