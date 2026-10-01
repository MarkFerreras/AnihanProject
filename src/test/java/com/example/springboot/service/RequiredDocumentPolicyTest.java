package com.example.springboot.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredDocumentPolicyTest {

    private static final List<String> INTAKE = List.of(
            "PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)");

    private static final Set<String> ALL_INTAKE_TYPES = Set.of(
            "PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)");

    @Test
    void activeStudentWithNothingMissesExactlyTheIntakeDocumentsInOrder() {
        assertEquals(INTAKE, RequiredDocumentPolicy.missing("Active", Set.of()));
    }

    @Test
    void activeStudentWithAllIntakeDocumentsIsComplete() {
        assertTrue(RequiredDocumentPolicy.missing("Active", ALL_INTAKE_TYPES).isEmpty());
    }

    @Test
    void nonGraduatedStatusesNeverRequireCompletionDocuments() {
        for (String status : List.of("Enrolling", "Submitted", "Active")) {
            assertTrue(RequiredDocumentPolicy.missing(status, ALL_INTAKE_TYPES).isEmpty(),
                    "completion documents must not be required for " + status);
        }
    }

    @Test
    void graduatedStudentWithNothingMissesAllSevenRequirementsInOrder() {
        assertEquals(List.of(
                        "PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)",
                        "Transcript of Records (TOR)", "Form IX (any one)", "OJT Report",
                        "Certificate of TVET Program"),
                RequiredDocumentPolicy.missing("Graduated", Set.of()));
    }

    @Test
    void anySingleFormIxSatisfiesTheFormIxRequirement() {
        for (String formIx : List.of(
                "Form IX - Bread and Pastry Production NC II",
                "Form IX - Cookery NC II",
                "Form IX - Food and Beverage Services NC II")) {
            Set<String> present = Set.of(
                    "PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)",
                    "Transcript of Records (TOR)", "OJT Report", "Certificate of TVET Program", formIx);
            assertTrue(RequiredDocumentPolicy.missing("Graduated", present).isEmpty(),
                    formIx + " alone must satisfy the Form IX requirement");
        }
    }

    @Test
    void graduatedStatusIsMatchedIgnoringCaseAndSurroundingSpaces() {
        assertEquals(7, RequiredDocumentPolicy.missing("  graduated ", Set.of()).size());
        assertEquals(7, RequiredDocumentPolicy.missing("GRADUATED", Set.of()).size());
    }

    @Test
    void nullOrUnknownStatusRequiresIntakeDocumentsOnly() {
        assertEquals(INTAKE, RequiredDocumentPolicy.missing(null, Set.of()));
        assertEquals(INTAKE, RequiredDocumentPolicy.missing("Dropped", Set.of()));
    }

    @Test
    void othersAndNullTypeSetsSatisfyNothing() {
        assertEquals(INTAKE, RequiredDocumentPolicy.missing("Active", Set.of("Others")));
        assertEquals(INTAKE, RequiredDocumentPolicy.missing("Active", null));
    }
}
