package com.example.springboot.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.example.springboot.model.Grade;

/**
 * TESDA / Anihan Transcript-of-Records grading math:
 * <ul>
 *   <li>raw percentage → 1.00–5.00 grade equivalent (the transmutation table
 *       printed on the TOR),</li>
 *   <li>derived competency remark (COMPETENT / NOT_COMPETENT),</li>
 *   <li>units-weighted Total GWA across a student's graded subjects.</li>
 * </ul>
 * Stateless — all methods are static and pure.
 */
public final class GradeEquivalent {

    public static final String COMPETENT = "COMPETENT";
    public static final String NOT_COMPETENT = "NOT_COMPETENT";

    /** Passing threshold on the equivalent scale (75% → 3.00). Equivalent &gt; 3.00 is a failing mark. */
    public static final BigDecimal PASSING = new BigDecimal("3.00");

    private GradeEquivalent() {
    }

    /**
     * Transmutes a raw percentage to its grade equivalent. Bands are read as
     * "≥ the band's lower bound", so decimals resolve unambiguously:
     * {@code 98.9 → 1.25}, {@code 74.99 → 4.00}, {@code 69.99 → 5.00}.
     * Returns {@code null} for a {@code null} percentage.
     */
    public static BigDecimal toEquivalent(BigDecimal percentage) {
        if (percentage == null) {
            return null;
        }
        double p = percentage.doubleValue();
        if (p >= 99) return new BigDecimal("1.00");
        if (p >= 96) return new BigDecimal("1.25");
        if (p >= 93) return new BigDecimal("1.50");
        if (p >= 90) return new BigDecimal("1.75");
        if (p >= 87) return new BigDecimal("2.00");
        if (p >= 84) return new BigDecimal("2.25");
        if (p >= 81) return new BigDecimal("2.50");
        if (p >= 78) return new BigDecimal("2.75");
        if (p >= 75) return new BigDecimal("3.00");
        if (p >= 70) return new BigDecimal("4.00");
        return new BigDecimal("5.00");
    }

    /** True when the equivalent is a failing mark (&gt; 3.00). */
    public static boolean isFailing(BigDecimal equivalent) {
        return equivalent != null && equivalent.compareTo(PASSING) > 0;
    }

    /**
     * The effective equivalent used for pass/fail and GWA: the re-exam equivalent
     * when the final failed and a re-exam exists, otherwise the final equivalent.
     */
    public static BigDecimal effective(Grade g) {
        BigDecimal fin = g.getFinalGrade();
        BigDecimal re = g.getReExamGrade();
        if (fin == null) {
            return null;
        }
        return (re != null && isFailing(fin)) ? re : fin;
    }

    /**
     * Units-weighted average of the effective equivalents across the given grades:
     * {@code Σ(effective × subject_units) / Σ(subject_units)}. Rows without an
     * effective equivalent or without a subject/units are skipped. Returns
     * {@code null} when nothing contributes.
     */
    public static BigDecimal gwa(List<Grade> grades) {
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal units = BigDecimal.ZERO;
        for (Grade g : grades) {
            if (g.getSchoolClass() == null || g.getSchoolClass().getSubject() == null) {
                continue;
            }
            BigDecimal eff = effective(g);
            Integer u = g.getSchoolClass().getSubject().getUnits();
            if (eff == null || u == null) {
                continue;
            }
            weighted = weighted.add(eff.multiply(BigDecimal.valueOf(u)));
            units = units.add(BigDecimal.valueOf(u));
        }
        if (units.signum() == 0) {
            return null;
        }
        return weighted.divide(units, 2, RoundingMode.HALF_UP);
    }

    /**
     * The competency remark for a percentage grade, based on its effective
     * equivalent: {@link #COMPETENT} when ≤ 3.00, else {@link #NOT_COMPETENT}.
     */
    public static String remarkFor(BigDecimal effectiveEquivalent) {
        if (effectiveEquivalent == null) {
            return null;
        }
        return effectiveEquivalent.compareTo(PASSING) <= 0 ? COMPETENT : NOT_COMPETENT;
    }
}
