package com.example.springboot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Which required documents a student is missing (spec 2026-10-01 §2).
 * Intake documents are required for every student; completion documents
 * once the student is Completed or Graduated (spec 2026-10-01 SO checklist
 * §4.5), so the warning stays meaningful for students who are still studying.
 * Pure and stateless — to change what is required, edit this class.
 */
public final class RequiredDocumentPolicy {

    /** Label reported when a Completed/Graduated student has none of the three Form IX types. */
    public static final String FORM_IX_ANY_LABEL = "Form IX (any one)";

    private static final Set<String> COMPLETION_STATUSES = Set.of("completed", "graduated");

    private static final List<String> INTAKE_TYPES = List.of(
            DocumentService.PSA_BIRTH_CERTIFICATE_TYPE,
            DocumentService.FORM_137_TYPE,
            DocumentService.ID_PICTURE_TYPE);

    private static final List<String> FORM_IX_TYPES = List.of(
            DocumentService.FORM_IX_BPP_TYPE,
            DocumentService.FORM_IX_COOKERY_TYPE,
            DocumentService.FORM_IX_FBS_TYPE);

    private RequiredDocumentPolicy() {
    }

    /**
     * Display labels of unmet requirements, in a fixed order; empty when the
     * student is complete. "Others" (labelled or not) never satisfies anything.
     */
    public static List<String> missing(String studentStatus, Set<String> presentDocumentTypes) {
        Set<String> present = presentDocumentTypes == null ? Set.of() : presentDocumentTypes;
        List<String> missing = new ArrayList<>();

        for (String type : INTAKE_TYPES) {
            requireType(present, type, missing);
        }

        if (requiresCompletionDocuments(studentStatus)) {
            requireType(present, DocumentService.TOR_TYPE, missing);
            if (FORM_IX_TYPES.stream().noneMatch(present::contains)) {
                missing.add(FORM_IX_ANY_LABEL);
            }
            requireType(present, DocumentService.OJT_REPORT_TYPE, missing);
            requireType(present, DocumentService.TVET_CERTIFICATE_TYPE, missing);
        }
        return missing;
    }

    private static void requireType(Set<String> present, String type, List<String> missing) {
        if (!present.contains(type)) {
            missing.add(type);
        }
    }

    /** Completed or Graduated; anything else (incl. null/unknown) gets intake requirements only. */
    private static boolean requiresCompletionDocuments(String studentStatus) {
        return studentStatus != null
                && COMPLETION_STATUSES.contains(studentStatus.trim().toLowerCase(Locale.ROOT));
    }
}
