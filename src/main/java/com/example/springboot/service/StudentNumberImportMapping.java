package com.example.springboot.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * THE FILE TO EDIT WHEN THE SCHOOL'S REAL RECORD FORMAT ARRIVES.
 * ============================================================================
 *
 * <p>Everything about <em>how a spreadsheet is recognised</em> lives here: which
 * header text marks which column, how header text and cell values are cleaned up,
 * and how far down the sheet to look for the header row. {@link StudentNumberSheetParser}
 * and {@link StudentNumberImportService} contain no format knowledge of their own —
 * they ask this class.
 *
 * <p><b>To support a new sheet layout, add strings to the alias lists below.</b>
 * That is normally the whole change. Aliases are matched after {@link #normaliseHeader},
 * so {@code "Student No."}, {@code "student no"}, {@code "STUDENT_NO"} and
 * {@code "Student  No"} all collapse to the same token — you only need one spelling
 * per genuinely different wording.
 *
 * <p><b>What would need more than an edit here:</b> if the archive identifies a student
 * by something the system does not store (an old ledger number, or a name + birthdate
 * pair), that is a change of matching <em>strategy</em>, not of aliases. See
 * {@link StudentNumberSheetParser} and the import service in that case.
 */
public final class StudentNumberImportMapping {

    private StudentNumberImportMapping() {
    }

    /**
     * Header texts that mark the MATCH KEY column — the value used to find the student.
     * This is {@code student_records.student_id}, shown in the UI as "Reference No.".
     *
     * <p>ADD THE SCHOOL'S ACTUAL WORDING HERE.
     */
    public static final List<String> REFERENCE_ALIASES = List.of(
            "Reference No.",
            "Reference No",
            "Reference",
            "Ref No.",
            "Student ID",
            "StudentID",
            "SR No.",
            "SR Number"
    );

    /**
     * Header texts for the column holding the value to import — the real student number.
     *
     * <p>ADD THE SCHOOL'S ACTUAL WORDING HERE.
     */
    public static final List<String> STUDENT_NUMBER_ALIASES = List.of(
            "Student Number",
            "Student No.",
            "Student No",
            "StudentNumber",
            "SN"
    );

    /**
     * Optional. Used ONLY to warn when the name in the sheet disagrees with the stored
     * record — never to match a student. A safety net against a mis-sorted spreadsheet
     * where numbers have slipped a row.
     */
    public static final List<String> LAST_NAME_ALIASES = List.of(
            "Last Name", "LastName", "Surname", "Family Name"
    );

    public static final List<String> FIRST_NAME_ALIASES = List.of(
            "First Name", "FirstName", "Given Name"
    );

    /**
     * How many rows from the top to search for the header row. School files commonly
     * carry a title and some metadata above the real header (this project's own log
     * export does exactly that), so the parser scans rather than assuming row 1.
     */
    public static final int HEADER_SCAN_ROWS = 10;

    /** Hard ceiling on data rows, so a malformed file cannot exhaust memory. */
    public static final int MAX_DATA_ROWS = 20_000;

    /**
     * Canonical header text written by the exporter. Kept in the same order the export
     * emits, and every entry is present in the alias lists above, so an exported file
     * re-imports with no editing beyond filling in the Student Number column.
     */
    public static final String COLUMN_REFERENCE = "Reference No.";
    public static final String COLUMN_RECORD_ID = "Record ID";
    public static final String COLUMN_LAST_NAME = "Last Name";
    public static final String COLUMN_FIRST_NAME = "First Name";
    public static final String COLUMN_MIDDLE_NAME = "Middle Name";
    public static final String COLUMN_BATCH = "Batch";
    public static final String COLUMN_COURSE = "Course";
    public static final String COLUMN_SECTION = "Section";
    public static final String COLUMN_STATUS = "Status";
    public static final String COLUMN_STUDENT_NUMBER = "Student Number";

    public static final String[] EXPORT_COLUMNS = {
            COLUMN_REFERENCE, COLUMN_RECORD_ID, COLUMN_LAST_NAME, COLUMN_FIRST_NAME,
            COLUMN_MIDDLE_NAME, COLUMN_BATCH, COLUMN_COURSE, COLUMN_SECTION,
            COLUMN_STATUS, COLUMN_STUDENT_NUMBER
    };

    /**
     * Collapses header text so cosmetic differences stop mattering: case, surrounding
     * and internal whitespace, and any non-alphanumeric character (dots, underscores,
     * hyphens, non-breaking spaces pasted out of Excel).
     *
     * <p>{@code "Student No."}, {@code "student_no"} and {@code "STUDENT  NO"} all
     * become {@code "studentno"}.
     */
    public static String normaliseHeader(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(' ', ' ')
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Cleans a cell value before it is used.
     *
     * <p>Handles the two things Excel reliably does to identifiers: it stores them as
     * numbers, so {@code 2026001} comes back as {@code "2026001.0"}; and it pads with
     * non-breaking spaces on paste. Internal runs of whitespace are collapsed.
     *
     * <p>NOTE: this deliberately does NOT strip leading zeros — {@code "0012"} is a
     * legitimate student number and must survive the round trip.
     *
     * <p>Add school-specific cleanup here (stripping a constant prefix, forcing case)
     * once the real format is known.
     */
    public static String normaliseValue(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.replace(' ', ' ').trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) {
            return null;
        }
        // Excel numeric cells arrive as "2026001.0"; the fractional part is an artefact
        // of the cell type, never part of the identifier.
        if (value.matches("\\d+\\.0+")) {
            value = value.substring(0, value.indexOf('.'));
        }
        return value;
    }

    /** Normalised alias tokens for the match-key column. */
    public static Set<String> referenceTokens() {
        return tokensOf(REFERENCE_ALIASES);
    }

    /** Normalised alias tokens for the student-number column. */
    public static Set<String> studentNumberTokens() {
        return tokensOf(STUDENT_NUMBER_ALIASES);
    }

    public static Set<String> lastNameTokens() {
        return tokensOf(LAST_NAME_ALIASES);
    }

    public static Set<String> firstNameTokens() {
        return tokensOf(FIRST_NAME_ALIASES);
    }

    private static Set<String> tokensOf(List<String> aliases) {
        return aliases.stream()
                .map(StudentNumberImportMapping::normaliseHeader)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
