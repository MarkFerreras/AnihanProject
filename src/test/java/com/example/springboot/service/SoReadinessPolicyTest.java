package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.springboot.dto.registrar.SoChecklistItem;
import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.service.SoReadinessPolicy.Enrollment;
import com.example.springboot.service.SoReadinessPolicy.Student;

/** Pins spec 2026-10-01 SO checklist §6: stages, every item × state, and the TOR rule. */
class SoReadinessPolicyTest {

    private static final String PSA = "PSA Birth Certificate";
    private static final String TOR = "Transcript of Records (TOR)";
    private static final String TVET = "Certificate of TVET Program";
    private static final String OJT = "OJT Report";
    private static final String COOKERY = "Form IX - Cookery NC II";
    private static final String BPP = "Form IX - Bread and Pastry Production NC II";
    private static final String FBS = "Form IX - Food and Beverage Services NC II";

    private static final List<String> KEYS_IN_ORDER = List.of("psa", "student_info", "start_date", "end_date",
            "form_ix", "tor", "tvet_certificate", "ojt_report", "employment_status");

    /** A Completed student who meets every item; each test changes one thing. */
    private static final class Fixture {
        String status = "Completed";
        String lastName = "Dela Cruz";
        String firstName = "Ana";
        String middleName = "Reyes";
        LocalDate birthdate = LocalDate.of(2005, 2, 14);
        String sex = "Female";
        String permanentAddress = "Quezon City";
        String courseCode = "CARS";
        String batchCode = "B2026A";
        LocalDate enrollmentDate = LocalDate.of(2025, 6, 2);
        LocalDate completionDate = LocalDate.of(2026, 3, 20);
        String employmentStatus = "Employed";
        Set<String> documents = new HashSet<>(Set.of(PSA, COOKERY, TOR, TVET, OJT));
        LocalDateTime newestTor = LocalDateTime.of(2026, 3, 25, 10, 0);
        List<Enrollment> enrollments = new ArrayList<>(List.of(
                competent("BPP-101", LocalDateTime.of(2026, 3, 20, 9, 0)),
                competent("COOK-102", LocalDateTime.of(2026, 3, 21, 9, 0))));

        SoChecklistResponse evaluate() {
            return SoReadinessPolicy.evaluate(new Student("SR1", status, lastName, firstName, middleName,
                    birthdate, sex, permanentAddress, courseCode, batchCode, enrollmentDate,
                    completionDate, employmentStatus), documents, newestTor, enrollments);
        }
    }

    private static Enrollment competent(String subject, LocalDateTime lockedAt) {
        return new Enrollment(subject, true, true, lockedAt, "COMPETENT", null);
    }

    private static SoChecklistItem item(SoChecklistResponse response, String key) {
        return response.items().stream().filter(i -> i.key().equals(key)).findFirst().orElseThrow();
    }

    // ----- Stages -----

    @Test
    void aFullyMetCompletedStudentIsCompleteWithNoWarnings() {
        SoChecklistResponse result = new Fixture().evaluate();

        assertEquals("FINAL", result.stage());
        assertEquals(Boolean.TRUE, result.complete());
        assertEquals(0, result.warningCount());
        assertEquals(KEYS_IN_ORDER, result.items().stream().map(SoChecklistItem::key).toList());
        result.items().forEach(i -> assertEquals("MET", i.state(), i.key()));
    }

    @Test
    void graduatedStudentsGetTheSameFinalChecklist() {
        Fixture f = new Fixture();
        f.status = "Graduated";
        SoChecklistResponse result = f.evaluate();

        assertEquals("FINAL", result.stage());
        assertEquals(Boolean.TRUE, result.complete());
    }

    @Test
    void enrollingSubmittedAndUnknownStatusesGetNoChecklist() {
        for (String status : new String[] { "Enrolling", "Submitted", "Dropped", null }) {
            Fixture f = new Fixture();
            f.status = status;
            SoChecklistResponse result = f.evaluate();

            assertEquals("NOT_APPLICABLE", result.stage(), String.valueOf(status));
            assertNull(result.complete());
            assertEquals(0, result.warningCount());
            assertTrue(result.items().isEmpty());
        }
    }

    @Test
    void anActiveStudentGetsAPreviewWithCompletionItemsNotDue() {
        Fixture f = new Fixture();
        f.status = "Active";
        f.documents = new HashSet<>();   // nothing on file
        f.completionDate = null;
        f.employmentStatus = null;
        SoChecklistResponse result = f.evaluate();

        assertEquals("PREVIEW", result.stage());
        assertNull(result.complete(), "a preview never gives a verdict");
        assertEquals(KEYS_IN_ORDER, result.items().stream().map(SoChecklistItem::key).toList());
        assertEquals("UNMET", item(result, "psa").state(), "PSA is due while Active");
        assertEquals("MET", item(result, "student_info").state());
        assertEquals("MET", item(result, "start_date").state());
        for (String key : List.of("end_date", "form_ix", "tor", "tvet_certificate", "ojt_report", "employment_status")) {
            assertEquals("NOT_DUE", item(result, key).state(), key);
            assertEquals("Due once the student is Completed", item(result, key).detail(), key);
        }
    }

    @Test
    void statusIsMatchedIgnoringCaseAndSurroundingSpaces() {
        Fixture f = new Fixture();
        f.status = " graduated ";
        assertEquals("FINAL", f.evaluate().stage());
        f.status = "ACTIVE";
        assertEquals("PREVIEW", f.evaluate().stage());
    }

    // ----- Documents -----

    @Test
    void missingDocumentsAreUnmetWithReadableDetails() {
        Fixture f = new Fixture();
        f.documents = new HashSet<>();
        SoChecklistResponse result = f.evaluate();

        assertEquals(Boolean.FALSE, result.complete());
        assertEquals("No PSA Birth Certificate on file", item(result, "psa").detail());
        assertEquals("No Form IX on file", item(result, "form_ix").detail());
        assertEquals("No TOR on file", item(result, "tor").detail());
        assertEquals("No Certificate of TVET Program on file", item(result, "tvet_certificate").detail());
        assertEquals("No OJT Report on file", item(result, "ojt_report").detail());
        for (String key : List.of("psa", "form_ix", "tor", "tvet_certificate", "ojt_report")) {
            assertEquals("UNMET", item(result, key).state(), key);
        }
    }

    @Test
    void othersDocumentsNeverSatisfyAnItem() {
        Fixture f = new Fixture();
        f.documents = new HashSet<>(Set.of("Others"));
        SoChecklistResponse result = f.evaluate();

        assertEquals("UNMET", item(result, "psa").state());
        assertEquals("UNMET", item(result, "tor").state());
    }

    @Test
    void anyOneFormIxIsEnoughAndTheDetailNamesWhichIsOnFile() {
        Fixture f = new Fixture();
        f.documents = new HashSet<>(Set.of(PSA, TOR, TVET, OJT, FBS, BPP));
        SoChecklistItem formIx = item(f.evaluate(), "form_ix");

        assertEquals("MET", formIx.state());
        assertEquals("Form IX: Bread and Pastry Production NC II, Food and Beverage Services NC II", formIx.detail());
    }

    // ----- Student information, dates, employment -----

    @Test
    void studentInformationListsEveryMissingField() {
        Fixture f = new Fixture();
        f.birthdate = null;
        f.permanentAddress = "  ";
        f.courseCode = null;
        f.batchCode = null;
        SoChecklistItem info = item(f.evaluate(), "student_info");

        assertEquals("UNMET", info.state());
        assertEquals("Missing: birthdate, permanent address, course, batch", info.detail());
    }

    @Test
    void aBlankMiddleNameIsAWarningButTheStudentIsStillComplete() {
        Fixture f = new Fixture();
        f.middleName = "";
        SoChecklistResponse result = f.evaluate();

        assertEquals("WARNING", item(result, "student_info").state());
        assertEquals("No middle name — confirm this is correct", item(result, "student_info").detail());
        assertEquals(Boolean.TRUE, result.complete());
        assertEquals(1, result.warningCount());
    }

    @Test
    void datesAreUnmetUntilSetAndNameTheDateWhenMet() {
        Fixture f = new Fixture();
        assertEquals("Enrolled 2025-06-02", item(f.evaluate(), "start_date").detail());
        assertEquals("Completed 2026-03-20", item(f.evaluate(), "end_date").detail());

        f.enrollmentDate = null;
        f.completionDate = null;
        SoChecklistResponse result = f.evaluate();
        assertEquals("Enrollment date not set", item(result, "start_date").detail());
        assertEquals("Completion date not set", item(result, "end_date").detail());
        assertEquals("UNMET", item(result, "start_date").state());
        assertEquals("UNMET", item(result, "end_date").state());
    }

    @Test
    void unemployedCountsAsAnsweredButNotSetDoesNot() {
        Fixture f = new Fixture();
        f.employmentStatus = "Unemployed";
        SoChecklistItem answered = item(f.evaluate(), "employment_status");
        assertEquals("MET", answered.state());
        assertEquals("Unemployed", answered.detail());

        f.employmentStatus = null;
        SoChecklistItem notSet = item(f.evaluate(), "employment_status");
        assertEquals("UNMET", notSet.state());
        assertEquals("Employment status not set", notSet.detail());
    }

    // ----- TOR (§6.3) -----

    @Test
    void torIsUnmetWhenTheStudentHasNoEnrollments() {
        Fixture f = new Fixture();
        f.enrollments = new ArrayList<>();
        SoChecklistItem tor = item(f.evaluate(), "tor");

        assertEquals("UNMET", tor.state());
        assertEquals("TOR on file, but the student has no class enrollments", tor.detail());
    }

    @Test
    void torIsUnmetWhenAnEnrollmentHasNoGradeRow() {
        Fixture f = new Fixture();
        f.enrollments.add(new Enrollment("FBS-103", false, false, null, null, null));
        SoChecklistItem tor = item(f.evaluate(), "tor");

        assertEquals("UNMET", tor.state());
        assertEquals("TOR on file, but FBS-103 has no grade", tor.detail());
    }

    @Test
    void torIsUnmetWhenAGradeIsNotLocked() {
        Fixture f = new Fixture();
        f.enrollments.set(0, new Enrollment("BPP-101", true, false, null, "COMPETENT", null));
        SoChecklistItem tor = item(f.evaluate(), "tor");

        assertEquals("UNMET", tor.state());
        assertEquals("TOR on file, but BPP-101 is not locked", tor.detail());
    }

    @Test
    void torNamesIncompleteAndDroppedSubjects() {
        Fixture f = new Fixture();
        f.enrollments = new ArrayList<>(List.of(
                new Enrollment("COOK-102", true, true, LocalDateTime.of(2026, 3, 21, 9, 0), null, "INC"),
                new Enrollment("FBS-103", true, true, LocalDateTime.of(2026, 3, 21, 9, 0), null, "D")));
        SoChecklistItem tor = item(f.evaluate(), "tor");

        assertEquals("UNMET", tor.state());
        assertEquals("TOR on file, but INC in COOK-102; D in FBS-103", tor.detail());
    }

    @Test
    void torIsUnmetForNotCompetentOrAMissingFinalGrade() {
        Fixture f = new Fixture();
        f.enrollments = new ArrayList<>(List.of(
                new Enrollment("COOK-102", true, true, LocalDateTime.of(2026, 3, 21, 9, 0), "NOT_COMPETENT", "FA"),
                new Enrollment("FBS-103", true, true, LocalDateTime.of(2026, 3, 21, 9, 0), null, null)));
        SoChecklistItem tor = item(f.evaluate(), "tor");

        assertEquals("UNMET", tor.state());
        assertEquals("TOR on file, but Not Competent in COOK-102; FBS-103 has no final grade", tor.detail());
    }

    @Test
    void aPassedReExamCountsBecauseRemarksAlreadyReflectTheEffectiveGrade() {
        // GradeEquivalent.remarkFor derives remarks from the effective grade (re-exam included),
        // so a failed final + passed re-exam is stored as COMPETENT.
        Fixture f = new Fixture();
        f.enrollments.set(1, new Enrollment("COOK-102", true, true, LocalDateTime.of(2026, 3, 21, 9, 0),
                "COMPETENT", null));
        assertEquals("MET", item(f.evaluate(), "tor").state());
    }

    @Test
    void aTorUploadedBeforeTheLastGradeLockIsAWarning() {
        Fixture f = new Fixture();
        f.newestTor = LocalDateTime.of(2026, 3, 20, 12, 0); // after BPP-101's lock, before COOK-102's
        SoChecklistResponse result = f.evaluate();

        assertEquals("WARNING", item(result, "tor").state());
        assertEquals("TOR uploaded before the last grade lock — re-check it", item(result, "tor").detail());
        assertEquals(Boolean.TRUE, result.complete(), "a warning never blocks the verdict");
        assertEquals(1, result.warningCount());
    }

    @Test
    void aNewerTorClearsTheWarning() {
        Fixture f = new Fixture();
        f.newestTor = LocalDateTime.of(2026, 3, 21, 9, 30);
        SoChecklistItem tor = item(f.evaluate(), "tor");

        assertEquals("MET", tor.state());
        assertEquals("On file; all 2 grades final", tor.detail());
    }

    @Test
    void noTorFileIsUnmetAndStillListsGradeProblems() {
        Fixture f = new Fixture();
        f.documents.remove(TOR);
        f.newestTor = null;
        f.enrollments.set(0, new Enrollment("BPP-101", true, false, null, "COMPETENT", null));
        SoChecklistResponse result = f.evaluate();

        assertEquals("UNMET", item(result, "tor").state());
        assertEquals("No TOR on file; BPP-101 is not locked", item(result, "tor").detail());
        assertFalse(result.complete());
    }
}
