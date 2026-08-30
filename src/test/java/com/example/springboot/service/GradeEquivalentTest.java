package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.springboot.model.Grade;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.Subject;

class GradeEquivalentTest {

    private static BigDecimal eq(String pct) {
        return GradeEquivalent.toEquivalent(new BigDecimal(pct));
    }

    @Test
    void transmutesEachBandAtItsLowerBound() {
        assertEquals(new BigDecimal("1.00"), eq("100"));
        assertEquals(new BigDecimal("1.00"), eq("99"));
        assertEquals(new BigDecimal("1.25"), eq("96"));
        assertEquals(new BigDecimal("1.50"), eq("93"));
        assertEquals(new BigDecimal("1.75"), eq("90"));
        assertEquals(new BigDecimal("2.00"), eq("87"));
        assertEquals(new BigDecimal("2.25"), eq("84"));
        assertEquals(new BigDecimal("2.50"), eq("81"));
        assertEquals(new BigDecimal("2.75"), eq("78"));
        assertEquals(new BigDecimal("3.00"), eq("75"));
        assertEquals(new BigDecimal("4.00"), eq("70"));
        assertEquals(new BigDecimal("5.00"), eq("69"));
        assertEquals(new BigDecimal("5.00"), eq("0"));
    }

    @Test
    void decimalsResolveWithinTheBandTheyFallIn() {
        assertEquals(new BigDecimal("1.25"), eq("98.9"));   // < 99
        assertEquals(new BigDecimal("2.00"), eq("87.5"));
        assertEquals(new BigDecimal("3.00"), eq("75.0"));
        assertEquals(new BigDecimal("4.00"), eq("74.99"));  // < 75
        assertEquals(new BigDecimal("5.00"), eq("69.99"));  // < 70
    }

    @Test
    void nullPercentageGivesNullEquivalent() {
        assertNull(GradeEquivalent.toEquivalent(null));
    }

    @Test
    void failingIsStrictlyAboveThreePointZero() {
        assertFalse(GradeEquivalent.isFailing(new BigDecimal("3.00")));
        assertTrue(GradeEquivalent.isFailing(new BigDecimal("4.00")));
        assertTrue(GradeEquivalent.isFailing(new BigDecimal("5.00")));
        assertFalse(GradeEquivalent.isFailing(null));
    }

    @Test
    void remarkFollowsTheEffectiveEquivalent() {
        assertEquals(GradeEquivalent.COMPETENT, GradeEquivalent.remarkFor(new BigDecimal("3.00")));
        assertEquals(GradeEquivalent.NOT_COMPETENT, GradeEquivalent.remarkFor(new BigDecimal("4.00")));
        assertNull(GradeEquivalent.remarkFor(null));
    }

    @Test
    void effectivePrefersReExamOnlyWhenTheFinalFailed() {
        Grade passedNoReExam = grade("1.50", null);
        assertEquals(new BigDecimal("1.50"), GradeEquivalent.effective(passedNoReExam));

        Grade failedWithReExam = grade("4.00", "3.00");
        assertEquals(new BigDecimal("3.00"), GradeEquivalent.effective(failedWithReExam));

        Grade passedWithStrayReExam = grade("2.00", "1.00");
        assertEquals(new BigDecimal("2.00"), GradeEquivalent.effective(passedWithStrayReExam));

        assertNull(GradeEquivalent.effective(grade(null, null)));
    }

    @Test
    void gwaIsUnitsWeightedAcrossEffectiveEquivalents() {
        // (4.0*3 + 3.5*4) / (3+4) = 26/7 = 3.71
        Grade g1 = gradeWithUnits("4.00", null, 3);
        Grade g2 = gradeWithUnits("3.50", null, 4);
        assertEquals(new BigDecimal("3.71"), GradeEquivalent.gwa(List.of(g1, g2)));
    }

    @Test
    void gwaUsesReExamForFailedSubjects() {
        // final 4.00 failed, re-exam 2.50 -> effective 2.50 weighted by 3 units
        Grade g = gradeWithUnits("4.00", "2.50", 3);
        assertEquals(new BigDecimal("2.50"), GradeEquivalent.gwa(List.of(g)));
    }

    @Test
    void gwaIsNullWhenNothingContributes() {
        assertNull(GradeEquivalent.gwa(List.of()));
        assertNull(GradeEquivalent.gwa(List.of(grade(null, null))));
    }

    private static Grade grade(String finalEquiv, String reExamEquiv) {
        return gradeWithUnits(finalEquiv, reExamEquiv, null);
    }

    private static Grade gradeWithUnits(String finalEquiv, String reExamEquiv, Integer units) {
        Grade g = new Grade();
        if (finalEquiv != null) g.setFinalGrade(new BigDecimal(finalEquiv));
        if (reExamEquiv != null) g.setReExamGrade(new BigDecimal(reExamEquiv));
        if (units != null) {
            Subject subject = new Subject();
            subject.setUnits(units);
            SchoolClass sc = new SchoolClass();
            sc.setSubject(subject);
            g.setSchoolClass(sc);
        }
        return g;
    }
}
