package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.springboot.dto.registrar.SoChecklistItem;
import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.service.SoReadinessPolicy.Enrollment;
import com.example.springboot.service.SoReadinessPolicy.Student;

/**
 * Task 11 (T11-04, T11-05): every SO checklist item x state, and the stale-TOR clock rule.
 * ISO 25010 characteristic: Functional suitability (completeness and correctness of SO readiness).
 */
class SoReadinessPolicyMatrixTest {

    private static final String PSA = "PSA Birth Certificate";
    private static final String TOR = "Transcript of Records (TOR)";
    private static final String TVET = "Certificate of TVET Program";
    private static final String OJT = "OJT Report";
    private static final String COOKERY = "Form IX - Cookery NC II";

    private static final LocalDateTime LOCK = LocalDateTime.of(2026, 3, 20, 12, 0);
    private static final LocalDate ENROLLED = LocalDate.of(2025, 6, 2);
    private static final LocalDate COMPLETED = LocalDate.of(2026, 3, 20);

    private static Student student(String status, String middle, LocalDate enrolled, LocalDate completed, String employment) {
        return new Student("SR1", status, "Dela Cruz", "Ana", middle, LocalDate.of(2005, 2, 14), "Female",
                "Quezon City", "CARS", "B2026A", enrolled, completed, employment);
    }

    private static Student fullStudent(String status) {
        return student(status, "Reyes", ENROLLED, COMPLETED, "Employed");
    }

    private static List<Enrollment> goodGrades() {
        return List.of(new Enrollment("BPP-101", true, true, LOCK, "COMPETENT", null));
    }

    private static Set<String> allDocs() {
        return new HashSet<>(Set.of(PSA, COOKERY, TOR, TVET, OJT));
    }

    private static Set<String> without(String type) {
        Set<String> d = allDocs();
        d.remove(type);
        return d;
    }

    private static String stateOf(SoChecklistResponse r, String key) {
        return r.items().stream().filter(i -> i.key().equals(key)).map(SoChecklistItem::state)
                .findFirst().orElseThrow();
    }

    private static Arguments row(String key, String expected, Student s, Set<String> docs) {
        return Arguments.of(key + " " + expected, key, expected, s, docs);
    }

    static Stream<Arguments> items() {
        Student full = fullStudent("Completed");
        Student active = fullStudent("Active");
        Student noMiddle = student("Completed", null, ENROLLED, COMPLETED, "Employed");
        Student noLastName = new Student("SR1", "Completed", "", "Ana", "R", LocalDate.of(2005, 2, 14),
                "Female", "QC", "CARS", "B1", ENROLLED, COMPLETED, "Employed");
        return Stream.of(
                row("psa", "MET", full, allDocs()),
                row("psa", "UNMET", full, without(PSA)),
                row("student_info", "MET", full, allDocs()),
                row("student_info", "WARNING", noMiddle, allDocs()),
                row("student_info", "UNMET", noLastName, allDocs()),
                row("start_date", "MET", full, allDocs()),
                row("start_date", "UNMET", student("Completed", "R", null, COMPLETED, "Employed"), allDocs()),
                row("end_date", "MET", full, allDocs()),
                row("end_date", "UNMET", student("Completed", "R", ENROLLED, null, "Employed"), allDocs()),
                row("end_date", "NOT_DUE", active, allDocs()),
                row("form_ix", "MET", full, allDocs()),
                row("form_ix", "UNMET", full, without(COOKERY)),
                row("form_ix", "NOT_DUE", active, allDocs()),
                row("tor", "MET", full, allDocs()),
                row("tor", "UNMET", full, without(TOR)),
                row("tor", "NOT_DUE", active, allDocs()),
                row("tvet_certificate", "MET", full, allDocs()),
                row("tvet_certificate", "UNMET", full, without(TVET)),
                row("tvet_certificate", "NOT_DUE", active, allDocs()),
                row("ojt_report", "MET", full, allDocs()),
                row("ojt_report", "UNMET", full, without(OJT)),
                row("ojt_report", "NOT_DUE", active, allDocs()),
                row("employment_status", "MET", full, allDocs()),
                row("employment_status", "UNMET", student("Completed", "R", ENROLLED, COMPLETED, " "), allDocs()),
                row("employment_status", "NOT_DUE", active, allDocs()));
    }

    /** T11-04 */
    @ParameterizedTest(name = "{0}")
    @MethodSource("items")
    void soChecklistEachItemMetMissingWarning(String label, String key, String expected, Student s,
            Set<String> docs) {
        SoChecklistResponse result = SoReadinessPolicy.evaluate(s, docs, LOCK.plusDays(1), goodGrades());

        assertEquals(expected, stateOf(result, key));
    }

    static Stream<Arguments> torTimings() {
        return Stream.of(
                Arguments.of("TOR 59 min before lock", LOCK.minusMinutes(59), "WARNING"),
                Arguments.of("TOR 61 min before lock", LOCK.minusMinutes(61), "WARNING"),
                Arguments.of("TOR 9 h before lock (UTC vs UTC+8 skew)", LOCK.minusHours(9), "WARNING"),
                Arguments.of("TOR exactly at lock", LOCK, "MET"),
                Arguments.of("TOR 59 min after lock", LOCK.plusMinutes(59), "MET"),
                Arguments.of("TOR 61 min after lock", LOCK.plusMinutes(61), "MET"),
                Arguments.of("TOR upload time unknown", null, "MET"));
    }

    /**
     * T11-05: the policy compares the two timestamps as-is, with no tolerance window, so even a
     * 59-minute (or 9-hour clock-skew) predate warns. Any UTC / UTC+8 normalisation must happen
     * before the values reach the policy.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("torTimings")
    void staleTorWarningWithinAndOutsideClockOffset(String label, LocalDateTime torUpload, String expected) {
        SoChecklistResponse result = SoReadinessPolicy.evaluate(fullStudent("Completed"), allDocs(), torUpload,
                goodGrades());

        assertEquals(expected, stateOf(result, "tor"));
    }
}
