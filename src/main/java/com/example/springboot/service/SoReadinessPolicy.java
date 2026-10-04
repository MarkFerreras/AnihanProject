package com.example.springboot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.example.springboot.dto.registrar.SoChecklistItem;
import com.example.springboot.dto.registrar.SoChecklistResponse;

/**
 * Which TESDA Special Order requirements a student meets (spec 2026-10-01 SO checklist §6).
 * Covers the per-student items only — batch-level items (List of Students, Attendance
 * Sheet, Registries of Workers) are a later card, so "complete" never means "ready to file".
 * Kept separate from {@link RequiredDocumentPolicy}, which answers a different question
 * (intake/export completeness); both use the shared {@code DocumentService.*_TYPE} constants.
 * Pure and stateless — to change a rule, edit this class.
 */
public final class SoReadinessPolicy {

    public enum State { MET, WARNING, UNMET, NOT_DUE }

    public enum Stage { NOT_APPLICABLE, PREVIEW, FINAL }

    /** The student_records facts the checklist reads. Course and batch are already section-first. */
    public record Student(String studentId, String status, String lastName, String firstName,
                          String middleName, LocalDate birthdate, String sex, String permanentAddress,
                          String courseCode, String batchCode, LocalDate enrollmentDate,
                          LocalDate completionDate, String employmentStatus) {
    }

    /** One class enrollment and its grade row; {@code graded} is false when there is no grade row. */
    public record Enrollment(String subjectCode, boolean graded, boolean locked, LocalDateTime lockedAt,
                             String remarks, String gradeStatus) {
    }

    private static final String COMPETENT = "COMPETENT";
    private static final String NOT_COMPETENT = "NOT_COMPETENT";
    private static final String NOT_DUE_DETAIL = "Due once the student is Completed";
    private static final String FORM_IX_PREFIX = "Form IX - ";

    private static final List<String> FORM_IX_TYPES = List.of(
            DocumentService.FORM_IX_BPP_TYPE,
            DocumentService.FORM_IX_COOKERY_TYPE,
            DocumentService.FORM_IX_FBS_TYPE);

    private SoReadinessPolicy() {
    }

    public static SoChecklistResponse evaluate(Student student, Set<String> documentTypes,
                                               LocalDateTime newestTorUpload, List<Enrollment> enrollments) {
        Stage stage = stageFor(student.status());
        if (stage == Stage.NOT_APPLICABLE) {
            return new SoChecklistResponse(stage.name(), null, 0, List.of());
        }
        Set<String> present = documentTypes == null ? Set.of() : documentTypes;
        List<Enrollment> rows = enrollments == null ? List.of() : enrollments;
        boolean completionDue = stage == Stage.FINAL;

        List<SoChecklistItem> items = new ArrayList<>();
        items.add(document("psa", "PSA Birth Certificate", DocumentService.PSA_BIRTH_CERTIFICATE_TYPE, present));
        items.add(studentInformation(student));
        items.add(student.enrollmentDate() != null
                ? item("start_date", "Start and End Date — start", State.MET, "Enrolled " + student.enrollmentDate())
                : item("start_date", "Start and End Date — start", State.UNMET, "Enrollment date not set"));
        items.add(dueOnCompletion(completionDue, "end_date", "Start and End Date — end", () ->
                student.completionDate() != null
                        ? item("end_date", "Start and End Date — end", State.MET, "Completed " + student.completionDate())
                        : item("end_date", "Start and End Date — end", State.UNMET, "Completion date not set")));
        items.add(dueOnCompletion(completionDue, "form_ix", "Permanent Record (Form IX) — at least one",
                () -> formIx(present)));
        items.add(dueOnCompletion(completionDue, "tor", "Transcript of Records",
                () -> tor(present.contains(DocumentService.TOR_TYPE), newestTorUpload, rows)));
        items.add(dueOnCompletion(completionDue, "tvet_certificate", "Certificate of TVET Program",
                () -> document("tvet_certificate", "Certificate of TVET Program",
                        DocumentService.TVET_CERTIFICATE_TYPE, present)));
        items.add(dueOnCompletion(completionDue, "ojt_report", "OJT Report",
                () -> document("ojt_report", "OJT Report", DocumentService.OJT_REPORT_TYPE, present)));
        items.add(dueOnCompletion(completionDue, "employment_status", "Employment Status", () ->
                isBlank(student.employmentStatus())
                        ? item("employment_status", "Employment Status", State.UNMET, "Employment status not set")
                        : item("employment_status", "Employment Status", State.MET, student.employmentStatus())));

        int warnings = (int) items.stream().filter(i -> State.WARNING.name().equals(i.state())).count();
        Boolean complete = stage == Stage.FINAL
                ? items.stream().noneMatch(i -> State.UNMET.name().equals(i.state()))
                : null;
        return new SoChecklistResponse(stage.name(), complete, warnings, items);
    }

    /** Enrolling/Submitted/unknown → no checklist; Active → preview; Completed/Graduated → final. */
    static Stage stageFor(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "active" -> Stage.PREVIEW;
            case "completed", "graduated" -> Stage.FINAL;
            default -> Stage.NOT_APPLICABLE;
        };
    }

    private static SoChecklistItem dueOnCompletion(boolean due, String key, String label,
                                                   Supplier<SoChecklistItem> check) {
        return due ? check.get() : item(key, label, State.NOT_DUE, NOT_DUE_DETAIL);
    }

    /** "Others" (labelled or not) is never a key here, so it never satisfies anything. */
    private static SoChecklistItem document(String key, String label, String type, Set<String> present) {
        return present.contains(type)
                ? item(key, label, State.MET, "On file")
                : item(key, label, State.UNMET, "No " + label + " on file");
    }

    private static SoChecklistItem studentInformation(Student s) {
        String key = "student_info";
        String label = "Student Information";
        List<String> missing = new ArrayList<>();
        if (isBlank(s.lastName())) missing.add("last name");
        if (isBlank(s.firstName())) missing.add("first name");
        if (s.birthdate() == null) missing.add("birthdate");
        if (isBlank(s.sex())) missing.add("sex");
        if (isBlank(s.permanentAddress())) missing.add("permanent address");
        if (isBlank(s.courseCode())) missing.add("course");
        if (isBlank(s.batchCode())) missing.add("batch");
        if (!missing.isEmpty()) {
            return item(key, label, State.UNMET, "Missing: " + String.join(", ", missing));
        }
        if (isBlank(s.middleName())) {
            return item(key, label, State.WARNING, "No middle name — confirm this is correct");
        }
        return item(key, label, State.MET, "All required fields present");
    }

    private static SoChecklistItem formIx(Set<String> present) {
        String key = "form_ix";
        String label = "Permanent Record (Form IX) — at least one";
        List<String> onFile = FORM_IX_TYPES.stream()
                .filter(present::contains)
                .map(type -> type.substring(FORM_IX_PREFIX.length()))
                .toList();
        return onFile.isEmpty()
                ? item(key, label, State.UNMET, "No Form IX on file")
                : item(key, label, State.MET, "Form IX: " + String.join(", ", onFile));
    }

    /**
     * §6.3: UNMET without a TOR file or with any enrollment that lacks a locked COMPETENT
     * grade; WARNING when the newest TOR predates the latest grade lock; MET otherwise.
     */
    private static SoChecklistItem tor(boolean torOnFile, LocalDateTime newestTorUpload, List<Enrollment> enrollments) {
        String key = "tor";
        String label = "Transcript of Records";
        List<String> problems = new ArrayList<>();
        if (enrollments.isEmpty()) {
            problems.add("the student has no class enrollments");
        }
        for (Enrollment enrollment : enrollments) {
            String problem = gradeProblem(enrollment);
            if (problem != null) {
                problems.add(problem);
            }
        }
        if (!torOnFile) {
            String detail = problems.isEmpty() ? "No TOR on file" : "No TOR on file; " + String.join("; ", problems);
            return item(key, label, State.UNMET, detail);
        }
        if (!problems.isEmpty()) {
            return item(key, label, State.UNMET, "TOR on file, but " + String.join("; ", problems));
        }
        LocalDateTime lastLock = enrollments.stream()
                .map(Enrollment::lockedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (lastLock != null && newestTorUpload != null && newestTorUpload.isBefore(lastLock)) {
            return item(key, label, State.WARNING, "TOR uploaded before the last grade lock — re-check it");
        }
        int count = enrollments.size();
        return item(key, label, State.MET, "On file; all " + count + " grade" + (count == 1 ? "" : "s") + " final");
    }

    /** Null when the enrollment's grade is final and Competent. */
    private static String gradeProblem(Enrollment e) {
        String subject = e.subjectCode();
        if (!e.graded()) {
            return subject + " has no grade";
        }
        if (!e.locked()) {
            return subject + " is not locked";
        }
        if (COMPETENT.equals(e.remarks())) {
            return null;
        }
        if (NOT_COMPETENT.equals(e.remarks())) {
            return "Not Competent in " + subject;
        }
        if (!isBlank(e.gradeStatus())) {
            return e.gradeStatus().trim() + " in " + subject;
        }
        return subject + " has no final grade";
    }

    private static SoChecklistItem item(String key, String label, State state, String detail) {
        return new SoChecklistItem(key, label, state.name(), detail);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
