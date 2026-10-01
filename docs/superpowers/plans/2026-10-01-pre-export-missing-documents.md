# Pre-Export Missing-Documents Check & Custom "Others" Names Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use **superpowers:subagent-driven-development** to implement this plan task-by-task (user-confirmed choice, 2026-10-01 — do not ask again, do not switch to executing-plans). Dispatch a fresh subagent per task, in order (Task 0 → Task 8), with the spec and code-quality reviews between tasks. Task 8 Step 2 (applying the migration to the live DB) must stop and ask the user. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When the Registrar clicks any Export button on the Documents page, warn (in a dialog, red "!" per student) which students in that scope are missing required documents; and let "Others" uploads carry a custom document name.

**Architecture:** A pure `RequiredDocumentPolicy` decides what a student is missing from their status + document types. A new read-only endpoint `GET /api/registrar/documents/export-check/{scope}/{key}` runs one BLOB-free `student_records LEFT JOIN documents` query with the same scope membership as the existing ZIP export, and the page calls it before navigating to the unchanged streaming export URL. `prepareExport` runs the same check so the export audit row records the flagged count. Custom names live in a new nullable `documents.document_label` column; `document_type` stays `Others`.

**Tech Stack:** Java 25, Spring Boot 4.0.4, Spring Data JPA + `JdbcTemplate`, MySQL 8 (H2 MySQL-mode in tests), JUnit 5 + Mockito + MockMvc, Bootstrap 5.3, jQuery 4, vanilla JS (`SrmsCombobox`).

**Spec:** `docs/superpowers/specs/2026-10-01-pre-export-missing-documents-design.md` (read it first).

**Conventions for every task:**
- Work only on branch `feature/pre-export-missing-documents` (Task 0). Never commit to `main`.
- Commit messages: plain conventional commits, **no `Co-Authored-By` trailer** (the user's standing preference).
- Run Gradle from the repo root in Git Bash: `./gradlew ...`. Tests use H2/mocks only — no live DB needed until Task 8.
- `system_logs` is append-only; never edit or delete log rows.

---

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `src/main/java/com/example/springboot/service/RequiredDocumentPolicy.java` | Create | Pure rules: which required documents a student is missing |
| `src/main/java/com/example/springboot/service/DocumentService.java` | Modify | Type constants; label validation in `uploadBatch`; `getDocumentLabels`; label in `toSummary`/`save` |
| `src/main/java/com/example/springboot/dto/registrar/ExportCheckResponse.java` | Create | Check result: students in scope + flagged list |
| `src/main/java/com/example/springboot/dto/registrar/FlaggedStudent.java` | Create | One flagged student + what is missing |
| `src/main/java/com/example/springboot/repository/DocumentFolderRepository.java` | Modify | `CheckRow` + `findCheckRows(scope, key)` |
| `src/main/java/com/example/springboot/service/DocumentExportService.java` | Modify | `checkMissing`; `PreparedExport` carries counts |
| `src/main/java/com/example/springboot/controller/DocumentController.java` | Modify | `export-check` + `labels` endpoints; audit text; labels param |
| `src/main/java/com/example/springboot/model/Document.java` | Modify | `documentLabel` field |
| `src/main/java/com/example/springboot/dto/registrar/DocumentSummaryResponse.java` | Modify | `documentLabel` component + 9-arg convenience constructor |
| `src/main/java/com/example/springboot/repository/DocumentRepository.java` | Modify | Label in projections + search; `findDistinctDocumentLabels` |
| `src/main/sql/migrations/2026-10-01-add-documents-document-label.sql` | Create | Idempotent `ADD COLUMN document_label` |
| `src/main/sql/schema.sql`, `src/main/sql/AnihanSRMS.sql` | Modify | Mirror the new column |
| `src/test/resources/document-storage-h2-schema.sql` | Modify | Mirror the new column for real-H2 tests |
| `src/main/resources/static/documents.html` | Modify | `#exportCheckModal`; page CSS; script cache-bust |
| `src/main/resources/static/js/registrar-documents.js` | Modify | Pre-export check flow; label field on staged "Others" rows; label display |
| Tests (see each task) | Create/Modify | `RequiredDocumentPolicyTest` (new); extend `DocumentExportServiceTest`, `DocumentServiceTest`, `DocumentControllerWebMvcTest`, `DocumentStorageIntegrationTest` |

**Expected test count at the end:** 546 baseline + 32 new = **578**, 0 failures.

---

### Task 0: Branch and baseline

**Files:** none.

- [ ] **Step 1: Create the feature branch from an up-to-date `main`**

```bash
git checkout main
git status --short          # expect: no output (clean)
git checkout -b feature/pre-export-missing-documents
git branch --show-current   # expect: feature/pre-export-missing-documents
```

- [ ] **Step 2: Record the baseline test count**

```bash
./gradlew test
find build/test-results/test -name '*.xml' -exec grep -h -o 'tests="[0-9]*"' {} + | awk -F'"' '{s+=$2} END {print s}'
```

Expected: `BUILD SUCCESSFUL`, and the count prints `546`. If it differs, stop and report — every later count in this plan assumes 546.

---

### Task 1: `RequiredDocumentPolicy` and shared document-type constants

**Files:**
- Create: `src/main/java/com/example/springboot/service/RequiredDocumentPolicy.java`
- Modify: `src/main/java/com/example/springboot/service/DocumentService.java:30-84`
- Test: `src/test/java/com/example/springboot/service/RequiredDocumentPolicyTest.java` (create)

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/example/springboot/service/RequiredDocumentPolicyTest.java`:

```java
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.service.RequiredDocumentPolicyTest"`
Expected: FAIL — compilation error `cannot find symbol: variable RequiredDocumentPolicy`.

- [ ] **Step 3: Promote the document types to public constants in `DocumentService`**

In `src/main/java/com/example/springboot/service/DocumentService.java`, replace the block from the `ID_PICTURE_TYPE` declaration (line 30) through the end of `DOCUMENT_TYPES` (line 45) with:

```java
    /** Document type reserved for the student's 1x1 / 2x2 ID picture. */
    public static final String ID_PICTURE_TYPE = "ID Picture (1x1 / 2x2)";

    public static final String TOR_TYPE = "Transcript of Records (TOR)";
    public static final String FORM_IX_BPP_TYPE = "Form IX - Bread and Pastry Production NC II";
    public static final String FORM_IX_COOKERY_TYPE = "Form IX - Cookery NC II";
    public static final String FORM_IX_FBS_TYPE = "Form IX - Food and Beverage Services NC II";
    public static final String FORM_137_TYPE = "Form 137";
    public static final String PSA_BIRTH_CERTIFICATE_TYPE = "PSA Birth Certificate";
    public static final String OJT_REPORT_TYPE = "OJT Report";
    public static final String TVET_CERTIFICATE_TYPE = "Certificate of TVET Program";
    /** The only type that may carry a custom {@code document_label}. */
    public static final String OTHERS_TYPE = "Others";

    /** Document categories per R3.2 (AGILE-76) — the four generated templates plus common uploads. */
    private static final List<String> DOCUMENT_TYPES = List.of(
            TOR_TYPE,
            FORM_IX_BPP_TYPE,
            FORM_IX_COOKERY_TYPE,
            FORM_IX_FBS_TYPE,
            FORM_137_TYPE,
            PSA_BIRTH_CERTIFICATE_TYPE,
            OJT_REPORT_TYPE,
            TVET_CERTIFICATE_TYPE,
            OTHERS_TYPE,
            ID_PICTURE_TYPE
    );
```

Then replace the `TYPE_SHORT_NAMES` map (lines 79-84) with:

```java
    private static final Map<String, String> TYPE_SHORT_NAMES = Map.of(
            TOR_TYPE, "TOR",
            FORM_IX_BPP_TYPE, "FormIX-BPP",
            FORM_IX_COOKERY_TYPE, "FormIX-Cookery",
            FORM_IX_FBS_TYPE, "FormIX-FBS"
    );
```

The list order and strings are unchanged, so `GET /types` returns exactly the same JSON.

- [ ] **Step 4: Create the policy**

Create `src/main/java/com/example/springboot/service/RequiredDocumentPolicy.java`:

```java
package com.example.springboot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Which required documents a student is missing (spec 2026-10-01 §2).
 * Intake documents are required for every student; completion documents
 * only once the student is Graduated, so the warning stays meaningful for
 * students who are still studying. Pure and stateless — to change what is
 * required, edit this class.
 */
public final class RequiredDocumentPolicy {

    /** Label reported when a Graduated student has none of the three Form IX types. */
    public static final String FORM_IX_ANY_LABEL = "Form IX (any one)";

    private static final String GRADUATED_STATUS = "graduated";

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

        if (isGraduated(studentStatus)) {
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

    /** Anything other than "Graduated" (incl. null/unknown) gets intake requirements only. */
    private static boolean isGraduated(String studentStatus) {
        return studentStatus != null
                && GRADUATED_STATUS.equals(studentStatus.trim().toLowerCase(Locale.ROOT));
    }
}
```

- [ ] **Step 5: Run the policy test and the existing `DocumentServiceTest` to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.service.RequiredDocumentPolicyTest" --tests "com.example.springboot.service.DocumentServiceTest"`
Expected: `BUILD SUCCESSFUL`; 8 new tests pass; `DocumentServiceTest` (incl. `documentTypesIncludeAllFourTemplates`) still passes.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/springboot/service/RequiredDocumentPolicy.java \
        src/main/java/com/example/springboot/service/DocumentService.java \
        src/test/java/com/example/springboot/service/RequiredDocumentPolicyTest.java
git commit -m "feat: add RequiredDocumentPolicy and shared document type constants"
```

---

### Task 2: Check query and `DocumentExportService.checkMissing`

**Files:**
- Create: `src/main/java/com/example/springboot/dto/registrar/ExportCheckResponse.java`
- Create: `src/main/java/com/example/springboot/dto/registrar/FlaggedStudent.java`
- Modify: `src/main/java/com/example/springboot/repository/DocumentFolderRepository.java`
- Modify: `src/main/java/com/example/springboot/service/DocumentExportService.java`
- Test: `src/test/java/com/example/springboot/service/DocumentExportServiceTest.java`, `src/test/java/com/example/springboot/integration/DocumentStorageIntegrationTest.java`, `src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java` (one helper only)

- [ ] **Step 1: Write the failing unit tests**

In `src/test/java/com/example/springboot/service/DocumentExportServiceTest.java`:

Add these imports below the existing `import com.example.springboot.repository.DocumentFolderRepository.ExportRow;` line:

```java
import com.example.springboot.dto.registrar.ExportCheckResponse;
import com.example.springboot.repository.DocumentFolderRepository.CheckRow;
```

Add a new section before the `// Helpers` section:

```java
    // -------------------------------------------------------
    // Pre-export missing-documents check
    // -------------------------------------------------------

    @Test
    void checkMissingGroupsRowsPerStudentAndCountsDocuments() {
        when(sectionRepository.existsById("S1")).thenReturn(true);
        when(documentFolderRepository.findCheckRows(DocumentExportScope.SECTION, "S1")).thenReturn(List.of(
                check("SR1", "Active", 1, "PSA Birth Certificate"),
                check("SR1", "Active", 2, "Form 137"),
                check("SR2", "Graduated", 3, "PSA Birth Certificate"),
                check("SR2", "Graduated", 4, "Form 137"),
                check("SR2", "Graduated", 5, "ID Picture (1x1 / 2x2)"),
                check("SR2", "Graduated", 6, "Form IX - Cookery NC II")));

        ExportCheckResponse result = service.checkMissing(DocumentExportScope.SECTION, "S1");

        assertEquals(2, result.studentsInScope());
        assertEquals(2, result.flagged().size());
        var first = result.flagged().get(0);
        assertEquals("SR1", first.studentId());
        assertEquals(2, first.documentCount());
        assertEquals(List.of("ID Picture (1x1 / 2x2)"), first.missing());
        var second = result.flagged().get(1);
        assertEquals("SR2", second.studentId());
        assertEquals(4, second.documentCount());
        assertEquals(List.of("Transcript of Records (TOR)", "OJT Report", "Certificate of TVET Program"),
                second.missing());
    }

    @Test
    void checkMissingFlagsAStudentWithZeroDocuments() {
        when(batchRepository.existsById("B2026A")).thenReturn(true);
        when(documentFolderRepository.findCheckRows(DocumentExportScope.UNASSIGNED, "B2026A"))
                .thenReturn(List.of(check("SR9", "Enrolling", null, null)));

        ExportCheckResponse result = service.checkMissing(DocumentExportScope.UNASSIGNED, "B2026A");

        assertEquals(1, result.studentsInScope());
        assertEquals(0, result.flagged().get(0).documentCount());
        assertEquals(List.of("PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)"),
                result.flagged().get(0).missing());
    }

    @Test
    void checkMissingReturnsNoFlaggedStudentsWhenEveryoneIsComplete() {
        when(studentRecordRepository.existsByStudentId("SR1")).thenReturn(true);
        when(documentFolderRepository.findCheckRows(DocumentExportScope.STUDENT, "SR1")).thenReturn(List.of(
                check("SR1", "Active", 1, "PSA Birth Certificate"),
                check("SR1", "Active", 2, "Form 137"),
                check("SR1", "Active", 3, "ID Picture (1x1 / 2x2)")));

        ExportCheckResponse result = service.checkMissing(DocumentExportScope.STUDENT, "SR1");

        assertEquals(1, result.studentsInScope());
        assertTrue(result.flagged().isEmpty());
    }

    @Test
    void checkMissingThrowsNotFoundWhenSectionUnknown() {
        when(sectionRepository.existsById("NOPE")).thenReturn(false);
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.checkMissing(DocumentExportScope.SECTION, "NOPE"));
    }

    @Test
    void prepareExportCarriesStudentsInScopeAndFlaggedCount() {
        when(sectionRepository.existsById("S1")).thenReturn(true);
        when(documentFolderRepository.findExportRows(DocumentExportScope.SECTION, "S1"))
                .thenReturn(List.of(row(1, "SR1", null, "A", "One", "S1", "psa.pdf")));
        when(documentFolderRepository.findCheckRows(DocumentExportScope.SECTION, "S1")).thenReturn(List.of(
                check("SR1", "Active", 1, "PSA Birth Certificate"),
                check("SR2", "Active", null, null)));

        var prepared = service.prepareExport(DocumentExportScope.SECTION, "S1");

        assertEquals(2, prepared.studentsInScope());
        assertEquals(2, prepared.flaggedCount());
    }
```

Add this helper next to the existing `row(...)` helper:

```java
    private static CheckRow check(String studentId, String status, Integer documentId, String documentType) {
        return new CheckRow(studentId, null, "First", "Last" + studentId, status, null, documentId, documentType);
    }
```

Update the two existing `PreparedExport` constructions in this file (inside `writeZipStreamsEachDocumentAndProducesAValidArchiveOnSuccess` and `writeZipPropagatesFailureWithoutProducingAValidArchive`) to pass the two new counts:

```java
        var prepared = new DocumentExportService.PreparedExport("Documents_test.zip", List.of(
                new DocumentExportService.ExportEntry(1, "a.pdf"),
                new DocumentExportService.ExportEntry(2, "folder/b.pdf")), 0, 0);
```

```java
        var prepared = new DocumentExportService.PreparedExport("Documents_test.zip", List.of(
                new DocumentExportService.ExportEntry(1, "a.pdf"),
                new DocumentExportService.ExportEntry(2, "b.pdf")), 0, 0);
```

In `src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java`, update the `samplePreparedExport` helper (line ~316) so the file still compiles (Task 3 asserts on these numbers):

```java
    private DocumentExportService.PreparedExport samplePreparedExport(String zipName) {
        return new DocumentExportService.PreparedExport(zipName,
                List.of(new DocumentExportService.ExportEntry(1, "a.pdf")), 3, 1);
    }
```

- [ ] **Step 2: Write the failing real-H2 tests**

In `src/test/java/com/example/springboot/integration/DocumentStorageIntegrationTest.java`, add these two tests directly above the `private StudentRecord newStudent(...)` helper:

```java
    // -------------------------------------------------------
    // Pre-export missing-documents check (real SQL)
    // -------------------------------------------------------

    @Test
    void checkScopesMatchExportMembershipAndIncludeZeroDocumentStudents() {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        Batch otherBatch = batchRepository.save(new Batch("B2025A", (short) 2025));
        Course course = courseRepository.save(new Course("C1", "Culinary Arts and Restaurant Services"));
        Section section = new Section();
        section.setSectionCode("S1");
        section.setSection("Section 1");
        section.setBatch(batch);
        section.setCourse(course);
        sectionRepository.save(section);

        // In S1 although the student's own batch is B2025A — section membership wins, as in export.
        StudentRecord inSectionMismatched = newStudent("SR1", "Dela Cruz", "Maria", otherBatch, section);
        // Unassigned, own batch B2026A, and NO documents at all.
        StudentRecord unassignedSameBatch = newStudent("SR2", "Santos", "Juan", batch, null);
        StudentRecord unassignedOtherBatch = newStudent("SR3", "Reyes", "Ana", otherBatch, null);
        studentRecordRepository.saveAll(List.of(inSectionMismatched, unassignedSameBatch, unassignedOtherBatch));
        saveDocument(inSectionMismatched, "PSA Birth Certificate", "psa.pdf");
        saveDocument(inSectionMismatched, "Form 137", "f137.pdf");
        saveDocument(unassignedOtherBatch, "OJT Report", "ojt.pdf");

        assertEquals(java.util.Set.of("SR1"), checkStudentIds(DocumentExportScope.SECTION, "S1"));
        assertEquals(java.util.Set.of("SR2"), checkStudentIds(DocumentExportScope.UNASSIGNED, "B2026A"));
        assertEquals(java.util.Set.of("SR1", "SR2"), checkStudentIds(DocumentExportScope.BATCH, "B2026A"));
        assertEquals(java.util.Set.of("SR3"), checkStudentIds(DocumentExportScope.STUDENT, "SR3"));
    }

    @Test
    void checkMissingOnRealDatabaseCountsDocumentsAndFlagsZeroDocumentStudent() {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        StudentRecord complete = newStudent("SR1", "Abad", "Ana", batch, null);
        StudentRecord empty = newStudent("SR2", "Bautista", "Bea", batch, null);
        studentRecordRepository.saveAll(List.of(complete, empty));
        saveDocument(complete, "PSA Birth Certificate", "psa.pdf");
        saveDocument(complete, "Form 137", "f137.pdf");
        saveDocument(complete, "ID Picture (1x1 / 2x2)", "id.png");
        saveDocument(complete, "Others", "extra.pdf");

        var result = documentExportService.checkMissing(DocumentExportScope.UNASSIGNED, "B2026A");

        assertEquals(2, result.studentsInScope());
        assertEquals(1, result.flagged().size());
        var flagged = result.flagged().get(0);
        assertEquals("SR2", flagged.studentId());
        assertEquals(0, flagged.documentCount());
        assertEquals(List.of("PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)"), flagged.missing());
    }

    private java.util.Set<String> checkStudentIds(DocumentExportScope scope, String key) {
        return documentFolderRepository.findCheckRows(scope, key).stream()
                .map(DocumentFolderRepository.CheckRow::studentId)
                .collect(java.util.stream.Collectors.toSet());
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.service.DocumentExportServiceTest" --tests "com.example.springboot.integration.DocumentStorageIntegrationTest"`
Expected: FAIL — compilation errors: `cannot find symbol` for `CheckRow`, `ExportCheckResponse`, `findCheckRows`, `checkMissing`, and the 4-arg `PreparedExport` constructor.

- [ ] **Step 4: Create the response DTOs**

Create `src/main/java/com/example/springboot/dto/registrar/FlaggedStudent.java`:

```java
package com.example.springboot.dto.registrar;

import java.util.List;

/**
 * One student in an export scope who is missing required documents.
 * {@code documentCount == 0} means the student will not appear in the ZIP.
 */
public record FlaggedStudent(
        String studentId,
        String studentNumber,
        String lastName,
        String firstName,
        String studentStatus,
        String sectionCode,
        long documentCount,
        List<String> missing
) {
}
```

Create `src/main/java/com/example/springboot/dto/registrar/ExportCheckResponse.java`:

```java
package com.example.springboot.dto.registrar;

import java.util.List;

/**
 * Pre-export check result for one export scope. An empty {@code flagged}
 * list means the page can start the download immediately.
 */
public record ExportCheckResponse(int studentsInScope, List<FlaggedStudent> flagged) {
}
```

- [ ] **Step 5: Add `CheckRow` and `findCheckRows` to `DocumentFolderRepository`**

In `src/main/java/com/example/springboot/repository/DocumentFolderRepository.java`, add this record directly after the `ExportRow` record:

```java
    /** One student-document pair for the pre-export check; document fields are null for a student with none. */
    public record CheckRow(String studentId, String studentNumber, String firstName, String lastName,
                           String studentStatus, String sectionCode, Integer documentId, String documentType) {
    }
```

Add the SQL and method at the end of the class (before the final `}`):

```java
    /**
     * Starts from student_records (LEFT JOIN documents) so students with zero
     * documents are still returned. The WHERE clauses mirror the EXPORT_*_SQL
     * membership exactly, so the check and the ZIP always cover the same students.
     */
    private static final String CHECK_ROW_SELECT = """
            SELECT s.student_id, s.student_number, s.first_name, s.last_name, s.student_status,
                   s.section_code, d.document_id, d.document_type
            FROM student_records s
            LEFT JOIN documents d ON d.student_id = s.student_id
            """;

    private static final String CHECK_ORDER =
            " ORDER BY s.last_name ASC, s.first_name ASC, s.student_id ASC, d.document_id ASC";

    private static final String CHECK_STUDENT_SQL = CHECK_ROW_SELECT
            + " WHERE s.student_id = ?" + CHECK_ORDER;

    private static final String CHECK_SECTION_SQL = CHECK_ROW_SELECT
            + " WHERE s.section_code = ?" + CHECK_ORDER;

    private static final String CHECK_UNASSIGNED_SQL = CHECK_ROW_SELECT
            + " WHERE s.section_code IS NULL AND s.batch_code = ?" + CHECK_ORDER;

    private static final String CHECK_BATCH_SQL = CHECK_ROW_SELECT
            + " LEFT JOIN sections sec ON sec.section_code = s.section_code"
            + " WHERE sec.batch_code = ? OR (s.section_code IS NULL AND s.batch_code = ?)" + CHECK_ORDER;

    /** One parameterized, BLOB-free query per scope — never a per-student read. */
    public List<CheckRow> findCheckRows(DocumentExportScope scope, String key) {
        String sql = switch (scope) {
            case STUDENT -> CHECK_STUDENT_SQL;
            case SECTION -> CHECK_SECTION_SQL;
            case UNASSIGNED -> CHECK_UNASSIGNED_SQL;
            case BATCH -> CHECK_BATCH_SQL;
        };
        Object[] args = scope == DocumentExportScope.BATCH ? new Object[] { key, key } : new Object[] { key };
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CheckRow(
                rs.getString("student_id"),
                rs.getString("student_number"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("student_status"),
                rs.getString("section_code"),
                rs.getObject("document_id", Integer.class),
                rs.getString("document_type")), args);
    }
```

- [ ] **Step 6: Add `checkMissing` and the counts to `DocumentExportService`**

In `src/main/java/com/example/springboot/service/DocumentExportService.java`:

Add imports (keep the list alphabetical with the existing ones):

```java
import java.util.LinkedHashMap;

import com.example.springboot.dto.registrar.ExportCheckResponse;
import com.example.springboot.dto.registrar.FlaggedStudent;
import com.example.springboot.repository.DocumentFolderRepository.CheckRow;
```

Replace the `PreparedExport` record:

```java
    /**
     * {@code studentsInScope}/{@code flaggedCount} come from the same
     * missing-documents check the page runs, recomputed server-side for the
     * export audit row.
     */
    public record PreparedExport(String fileName, List<ExportEntry> entries,
                                 int studentsInScope, int flaggedCount) {
    }
```

Replace the body of `prepareExport` so it also runs the check (still before any response bytes are written by the controller), and add the new methods after it:

```java
    public PreparedExport prepareExport(DocumentExportScope scope, String key) {
        requireScopeExists(scope, key);

        List<ExportRow> rows = documentFolderRepository.findExportRows(scope, key);
        if (rows.isEmpty()) {
            throw new EmptyDocumentExportException("No documents to export.");
        }

        String zipFileName = buildZipFileName(scope, key, rows.get(0));
        List<ExportEntry> entries = buildEntries(scope, rows);
        ExportCheckResponse check = evaluate(documentFolderRepository.findCheckRows(scope, key));
        return new PreparedExport(zipFileName, entries, check.studentsInScope(), check.flagged().size());
    }

    /**
     * Pre-export check (spec 2026-10-01 §3): every student in the scope —
     * including students with zero documents — who is missing a required
     * document per {@link RequiredDocumentPolicy}.
     */
    public ExportCheckResponse checkMissing(DocumentExportScope scope, String key) {
        requireScopeExists(scope, key);
        return evaluate(documentFolderRepository.findCheckRows(scope, key));
    }

    /** Groups check rows per student (preserving query order) and applies the policy. */
    private static ExportCheckResponse evaluate(List<CheckRow> rows) {
        Map<String, StudentDocuments> byStudent = new LinkedHashMap<>();
        for (CheckRow row : rows) {
            StudentDocuments docs = byStudent.computeIfAbsent(row.studentId(), id -> new StudentDocuments(row));
            if (row.documentId() != null) {
                docs.documentCount++;
                docs.types.add(row.documentType());
            }
        }

        List<FlaggedStudent> flagged = new ArrayList<>();
        for (StudentDocuments docs : byStudent.values()) {
            List<String> missing = RequiredDocumentPolicy.missing(docs.student.studentStatus(), docs.types);
            if (!missing.isEmpty()) {
                CheckRow s = docs.student;
                flagged.add(new FlaggedStudent(s.studentId(), s.studentNumber(), s.lastName(), s.firstName(),
                        s.studentStatus(), s.sectionCode(), docs.documentCount, missing));
            }
        }
        return new ExportCheckResponse(byStudent.size(), flagged);
    }

    private static final class StudentDocuments {
        private final CheckRow student;
        private final Set<String> types = new HashSet<>();
        private long documentCount;

        private StudentDocuments(CheckRow student) {
            this.student = student;
        }
    }
```

(`ArrayList`, `HashSet`, `Map`, `Set` are already imported in this file.)

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.service.DocumentExportServiceTest" --tests "com.example.springboot.integration.DocumentStorageIntegrationTest" --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"`
Expected: `BUILD SUCCESSFUL`. 5 new unit tests + 2 new H2 tests pass; all existing tests in the three classes still pass (existing `prepareExport` unit tests get Mockito's default empty list from the unstubbed `findCheckRows`, and the H2 end-to-end export now also runs the real check query).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/springboot/dto/registrar/ExportCheckResponse.java \
        src/main/java/com/example/springboot/dto/registrar/FlaggedStudent.java \
        src/main/java/com/example/springboot/repository/DocumentFolderRepository.java \
        src/main/java/com/example/springboot/service/DocumentExportService.java \
        src/test/java/com/example/springboot/service/DocumentExportServiceTest.java \
        src/test/java/com/example/springboot/integration/DocumentStorageIntegrationTest.java \
        src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java
git commit -m "feat: add pre-export missing-documents check query and service"
```

---

### Task 3: `export-check` endpoint and flagged count in the export audit row

**Files:**
- Modify: `src/main/java/com/example/springboot/controller/DocumentController.java`
- Test: `src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java`

- [ ] **Step 1: Write the failing tests**

In `DocumentControllerWebMvcTest.java`, add imports next to the other `dto.registrar` imports:

```java
import com.example.springboot.dto.registrar.ExportCheckResponse;
import com.example.springboot.dto.registrar.FlaggedStudent;
```

Add after `exportBatchUnauthorizedWhenAnonymous()` (end of the ZIP export section):

```java
    // -------------------------------------------------------
    // Pre-export missing-documents check
    // -------------------------------------------------------

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void exportCheckReturnsFlaggedStudentsAndWritesNoAuditRow() throws Exception {
        when(documentExportService.checkMissing(DocumentExportScope.SECTION, "S1")).thenReturn(
                new ExportCheckResponse(3, List.of(new FlaggedStudent("SR20260001", null, "Dela Cruz", "Maria",
                        "Active", "S1", 0, List.of("PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)")))));

        mvc.perform(get("/api/registrar/documents/export-check/section/S1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentsInScope").value(3))
                .andExpect(jsonPath("$.flagged[0].studentId").value("SR20260001"))
                .andExpect(jsonPath("$.flagged[0].documentCount").value(0))
                .andExpect(jsonPath("$.flagged[0].missing[1]").value("Form 137"));

        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void exportCheckReturns400ForUnknownScopeWord() throws Exception {
        mvc.perform(get("/api/registrar/documents/export-check/classroom/S1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown export scope: classroom"));

        verify(documentExportService, never()).checkMissing(any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void exportCheckReturns404ForUnknownKey() throws Exception {
        when(documentExportService.checkMissing(DocumentExportScope.BATCH, "NOPE"))
                .thenThrow(new java.util.NoSuchElementException("batch not found: NOPE"));

        mvc.perform(get("/api/registrar/documents/export-check/batch/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("batch not found: NOPE"));
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void exportCheckForbiddenForTrainer() throws Exception {
        mvc.perform(get("/api/registrar/documents/export-check/unassigned/B2026A"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportCheckUnauthorizedWhenAnonymous() throws Exception {
        mvc.perform(get("/api/registrar/documents/export-check/student/SR20260001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void exportAuditRowIncludesTheFlaggedCount() throws Exception {
        when(documentExportService.prepareExport(DocumentExportScope.BATCH, "B2026A"))
                .thenReturn(samplePreparedExport("Documents_Batch_B2026A.zip"));

        mvc.perform(get("/api/registrar/documents/export/batch/B2026A"))
                .andExpect(status().isOk());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Requested ZIP export (batch B2026A, 1 document(s), 1 of 3 student(s) with missing documents)"),
                any());
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"`
Expected: FAIL — `exportCheck*` tests get 404/500 (no mapping) and `exportAuditRowIncludesTheFlaggedCount` fails because the logged text lacks `1 of 3 student(s) with missing documents`.

- [ ] **Step 3: Add the endpoint and update the audit text**

In `src/main/java/com/example/springboot/controller/DocumentController.java`:

Add the import next to the other `dto.registrar` imports:

```java
import com.example.springboot.dto.registrar.ExportCheckResponse;
```

Add this endpoint directly after `exportBatch(...)` (before the `streamExport` javadoc):

```java
    /**
     * Read-only pre-export check (spec 2026-10-01 §3): which students in the
     * scope are missing required documents. Writes no audit row.
     */
    @GetMapping("/export-check/{scope}/{key}")
    public ResponseEntity<ExportCheckResponse> exportCheck(@PathVariable String scope, @PathVariable String key) {
        return ResponseEntity.ok(documentExportService.checkMissing(parseExportScope(scope), key));
    }

    private static DocumentExportScope parseExportScope(String scope) {
        return switch (scope.toLowerCase(Locale.ROOT)) {
            case "student" -> DocumentExportScope.STUDENT;
            case "section" -> DocumentExportScope.SECTION;
            case "unassigned" -> DocumentExportScope.UNASSIGNED;
            case "batch" -> DocumentExportScope.BATCH;
            default -> throw new IllegalArgumentException("Unknown export scope: " + scope);
        };
    }
```

In `streamExport`, replace the `systemLogService.logAction(...)` call with:

```java
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Requested ZIP export (" + scope.name().toLowerCase(Locale.ROOT) + " " + key + ", "
                        + prepared.entries().size() + " document(s), "
                        + prepared.flaggedCount() + " of " + prepared.studentsInScope()
                        + " student(s) with missing documents)",
                httpRequest.getRemoteAddr());
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"`
Expected: `BUILD SUCCESSFUL`; 6 new tests pass; the existing `exportStudentReturnsZipWithDownloadHeadersAndWritesAuditBeforeStreaming` (`contains("Requested ZIP export")`) still passes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/springboot/controller/DocumentController.java \
        src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java
git commit -m "feat: add export-check endpoint and log flagged count on ZIP export"
```

---

### Task 4: `document_label` column, entity, summaries, search, and `GET /labels`

**Files:**
- Create: `src/main/sql/migrations/2026-10-01-add-documents-document-label.sql`
- Modify: `src/main/sql/schema.sql:1-4,282-292`, `src/main/sql/AnihanSRMS.sql:168-178`, `src/test/resources/document-storage-h2-schema.sql:71-81`
- Modify: `src/main/java/com/example/springboot/model/Document.java`
- Modify: `src/main/java/com/example/springboot/dto/registrar/DocumentSummaryResponse.java`
- Modify: `src/main/java/com/example/springboot/repository/DocumentRepository.java`
- Modify: `src/main/java/com/example/springboot/service/DocumentService.java` (`toSummary`, new `getDocumentLabels`)
- Modify: `src/main/java/com/example/springboot/controller/DocumentController.java` (new `GET /labels`)
- Test: `DocumentStorageIntegrationTest.java`, `DocumentControllerWebMvcTest.java`

- [ ] **Step 1: Write the failing tests**

In `DocumentStorageIntegrationTest.java`, add above `private StudentRecord newStudent(...)`:

```java
    // -------------------------------------------------------
    // Custom "Others" document labels (real SQL)
    // -------------------------------------------------------

    @Test
    void documentLabelRoundTripsIsSearchableAndListedDistinct() {
        StudentRecord student = newStudent("SR1", "Dela Cruz", "Maria", null, null);
        studentRecordRepository.save(student);
        saveLabelledOthers(student, "Medical Certificate", "med.pdf");
        saveLabelledOthers(student, "Medical Certificate", "med2.pdf");
        saveLabelledOthers(student, "Barangay Clearance", "brgy.pdf");
        saveDocument(student, "Others", "plain.pdf");

        var summaries = documentRepository.findSummariesByStudentId("SR1");
        assertEquals(4, summaries.size());
        assertEquals(java.util.Set.of("Medical Certificate", "Barangay Clearance"), summaries.stream()
                .map(com.example.springboot.dto.registrar.DocumentSummaryResponse::documentLabel)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet()));

        var found = documentRepository.searchSummaries("barangay", null, null, null);
        assertEquals(1, found.size());
        assertEquals("brgy.pdf", found.get(0).fileName());

        assertEquals(List.of("Barangay Clearance", "Medical Certificate"),
                documentRepository.findDistinctDocumentLabels());
    }
```

and next to the existing `saveDocument(...)` helper:

```java
    private void saveLabelledOthers(StudentRecord student, String label, String fileName) {
        Document document = new Document();
        document.setStudent(student);
        document.setDocumentType("Others");
        document.setDocumentLabel(label);
        document.setFileName(fileName);
        document.setFileType("application/pdf");
        byte[] content = "pdf-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        document.setFileSize(content.length);
        document.setContentData(content);
        documentRepository.save(document);
    }
```

In `DocumentControllerWebMvcTest.java`, add after `typesReturnsList()`:

```java
    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void labelsReturnsDistinctLabelsForRegistrar() throws Exception {
        when(documentService.getDocumentLabels()).thenReturn(List.of("Barangay Clearance", "Medical Certificate"));

        mvc.perform(get("/api/registrar/documents/labels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Barangay Clearance"))
                .andExpect(jsonPath("$[1]").value("Medical Certificate"));
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.integration.DocumentStorageIntegrationTest" --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"`
Expected: FAIL — compilation errors for `setDocumentLabel`, `documentLabel()`, `findDistinctDocumentLabels`, `getDocumentLabels`.

- [ ] **Step 3: Write the migration**

Create `src/main/sql/migrations/2026-10-01-add-documents-document-label.sql`:

```sql
-- ============================================================
-- Migration: 2026-10-01 — Add documents.document_label
-- ============================================================
-- Optional custom name for an "Others" document (e.g. "Medical
-- Certificate"), typed by the Registrar on upload. document_type stays
-- 'Others', so the strict type whitelist and the "Others" filter are
-- unchanged. NULL = a plain, unnamed "Others" document; existing rows are
-- left NULL (no backfill). The application only ever writes a label when
-- document_type = 'Others' (enforced in DocumentService.uploadBatch).
--
-- See docs/superpowers/specs/2026-10-01-pre-export-missing-documents-design.md §5.
--
-- Idempotent: safe to re-run. The guard matches on COLUMN_NAME.
-- ============================================================

USE AnihanSRMS;

SET @has_document_label = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'documents'
      AND COLUMN_NAME = 'document_label'
);
SET @sql = IF(@has_document_label = 0,
    'ALTER TABLE documents ADD COLUMN document_label VARCHAR(100) NULL AFTER document_type',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION (read-only — safe to run any time)
-- ============================================================

-- Expect: document_label VARCHAR(100), IS_NULLABLE = YES, right after document_type
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, ORDINAL_POSITION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'documents'
  AND COLUMN_NAME IN ('document_type', 'document_label')
ORDER BY ORDINAL_POSITION;

-- Expect: labelled = 0 on first run
SELECT COUNT(*) AS total_documents,
       SUM(document_label IS NOT NULL) AS labelled
FROM documents;
```

- [ ] **Step 4: Mirror the column in the three schema files**

`src/main/sql/schema.sql` — insert a header entry directly below line 2 (`-- schema.sql — Clean Schema + Default Security Questions`):

```sql
-- Updated: 2026-10-01 (added documents.document_label — optional custom name
--            for "Others" documents — see
--            migrations/2026-10-01-add-documents-document-label.sql)
```

and in the `documents` table add the column after `document_type`:

```sql
    document_type VARCHAR(255) NOT NULL,
    document_label VARCHAR(100) NULL,
    file_name VARCHAR(255) NOT NULL,
```

`src/main/sql/AnihanSRMS.sql` — same column insertion in its `documents` table (after `document_type VARCHAR(255) NOT NULL,`):

```sql
    document_label VARCHAR(100) NULL,
```

`src/test/resources/document-storage-h2-schema.sql` — same insertion in its `documents` table:

```sql
    document_label VARCHAR(100) NULL,
```

- [ ] **Step 5: Add the entity field**

In `src/main/java/com/example/springboot/model/Document.java`, add after the `documentType` field:

```java
    /** Optional custom name; only ever set when documentType is "Others". */
    @Column(name = "document_label", length = 100)
    private String documentLabel;
```

and after `setDocumentType(...)`:

```java
    public String getDocumentLabel() {
        return documentLabel;
    }

    public void setDocumentLabel(String documentLabel) {
        this.documentLabel = documentLabel;
    }
```

- [ ] **Step 6: Extend `DocumentSummaryResponse`**

Replace `src/main/java/com/example/springboot/dto/registrar/DocumentSummaryResponse.java` with:

```java
package com.example.springboot.dto.registrar;

import java.time.LocalDateTime;

/**
 * Listing row for the registrar Documents page. Deliberately excludes the
 * LONGBLOB content so table queries never load file bytes into memory.
 * {@code documentLabel} is the optional custom name of an "Others" document.
 */
public record DocumentSummaryResponse(
        Integer documentId,
        String studentId,
        String lastName,
        String firstName,
        String documentType,
        String fileName,
        String fileType,
        Integer fileSize,
        LocalDateTime uploadDate,
        String documentLabel
) {

    /** An unlabelled summary (every document except a named "Others"). */
    public DocumentSummaryResponse(Integer documentId, String studentId, String lastName, String firstName,
                                   String documentType, String fileName, String fileType, Integer fileSize,
                                   LocalDateTime uploadDate) {
        this(documentId, studentId, lastName, firstName, documentType, fileName, fileType, fileSize,
                uploadDate, null);
    }
}
```

The 9-arg constructor keeps the existing test fixtures compiling unchanged.

- [ ] **Step 7: Update `DocumentRepository`**

In `src/main/java/com/example/springboot/repository/DocumentRepository.java`, in **each of the three** constructor expressions (`searchSummaries`, `findSummariesByStudentId`, `findSummariesByIds`) change the second projection line from

```
                d.documentType, d.fileName, d.fileType, d.fileSize, d.uploadDate)
```

to

```
                d.documentType, d.fileName, d.fileType, d.fileSize, d.uploadDate, d.documentLabel)
```

In `searchSummaries`, extend the `:q` group — replace its last line

```
                   OR LOWER(CONCAT(s.lastName, ', ', s.firstName)) LIKE LOWER(CONCAT('%', :q, '%')))
```

with

```
                   OR LOWER(CONCAT(s.lastName, ', ', s.firstName)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(d.documentLabel) LIKE LOWER(CONCAT('%', :q, '%')))
```

Add at the end of the interface:

```java
    /** Distinct custom "Others" names in use, for the upload combobox suggestions. */
    @Query("""
            SELECT DISTINCT d.documentLabel
            FROM Document d
            WHERE d.documentLabel IS NOT NULL
            ORDER BY d.documentLabel
            """)
    List<String> findDistinctDocumentLabels();
```

- [ ] **Step 8: Update `DocumentService` read side**

In `DocumentService.java`, add after `getDocumentTypes()`:

```java
    public List<String> getDocumentLabels() {
        return documentRepository.findDistinctDocumentLabels();
    }
```

and in `toSummary(...)` add the label as the last constructor argument:

```java
                d.getFileSize(),
                d.getUploadDate(),
                d.getDocumentLabel());
```

- [ ] **Step 9: Add `GET /labels` to the controller**

In `DocumentController.java`, add after `types()`:

```java
    @GetMapping("/labels")
    public ResponseEntity<List<String>> labels() {
        return ResponseEntity.ok(documentService.getDocumentLabels());
    }
```

- [ ] **Step 10: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.integration.DocumentStorageIntegrationTest" --tests "com.example.springboot.controller.DocumentControllerWebMvcTest" --tests "com.example.springboot.service.DocumentServiceTest"`
Expected: `BUILD SUCCESSFUL`; the 2 new tests pass; all existing tests in these classes still pass.

- [ ] **Step 11: Commit**

```bash
git add src/main/sql/migrations/2026-10-01-add-documents-document-label.sql \
        src/main/sql/schema.sql src/main/sql/AnihanSRMS.sql \
        src/test/resources/document-storage-h2-schema.sql \
        src/main/java/com/example/springboot/model/Document.java \
        src/main/java/com/example/springboot/dto/registrar/DocumentSummaryResponse.java \
        src/main/java/com/example/springboot/repository/DocumentRepository.java \
        src/main/java/com/example/springboot/service/DocumentService.java \
        src/main/java/com/example/springboot/controller/DocumentController.java \
        src/test/java/com/example/springboot/integration/DocumentStorageIntegrationTest.java \
        src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java
git commit -m "feat: add documents.document_label with search and label suggestions"
```

---

### Task 5: Accept and validate labels on batch upload

**Files:**
- Modify: `src/main/java/com/example/springboot/service/DocumentService.java` (`uploadBatch`, `ValidatedFile`, `save`, `upload`, `saveGenerated`)
- Modify: `src/main/java/com/example/springboot/controller/DocumentController.java` (`uploadBatch`)
- Test: `DocumentServiceTest.java`, `DocumentControllerWebMvcTest.java`

> **Why `getParameterValues`:** Spring binds `@RequestParam List<String>` by **splitting a single value on commas**. With one staged file labelled `Barangay Clearance, 2026`, a `@RequestParam List<String> documentLabels` would arrive as two entries and fail the length check. `HttpServletRequest.getParameterValues` never splits. A test pins this.

- [ ] **Step 1: Write the failing service tests**

In `DocumentServiceTest.java`, add in the "Atomic bulk upload" section after `uploadBatchAcceptsExactlyTwentyFiles()`:

```java
    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf", "x".getBytes(StandardCharsets.UTF_8));
    }

    private List<Document> captureSavedDocuments() {
        List<Document> saved = new java.util.ArrayList<>();
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            saved.add(d);
            return d;
        });
        when(documentRepository.findSummariesByIds(any())).thenReturn(List.of());
        return saved;
    }

    @Test
    void uploadBatchStoresTrimmedLabelOnOthersAndNullForBlank() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        List<Document> saved = captureSavedDocuments();

        service.uploadBatch("SR20260001", List.of("Others", "Others"),
                java.util.Arrays.asList("  Medical Certificate  ", "   "),
                List.of(pdf("a.pdf"), pdf("b.pdf")), sampleAudit());

        assertEquals("Medical Certificate", saved.get(0).getDocumentLabel());
        assertNull(saved.get(1).getDocumentLabel());
    }

    @Test
    void uploadBatchWithoutLabelsStoresNullLabel() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        List<Document> saved = captureSavedDocuments();

        service.uploadBatch("SR20260001", List.of("Others"), List.of(pdf("a.pdf")), sampleAudit());

        assertNull(saved.get(0).getDocumentLabel());
    }

    @Test
    void uploadBatchRejectsLabelOnNonOthersType() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of(TOR_TYPE), List.of("My TOR"), List.of(pdf("a.pdf")), sampleAudit()));

        assertTrue(ex.getMessage().contains("'Others'"));
        verify(documentRepository, never()).save(any());
        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }

    @Test
    void uploadBatchRejectsLabelOverOneHundredCharacters() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of("Others"), List.of("x".repeat(101)), List.of(pdf("a.pdf")), sampleAudit()));

        assertTrue(ex.getMessage().contains("100 characters"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsLabelWithControlCharacters() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of("Others"), List.of("Medical\u0007Certificate"), List.of(pdf("a.pdf")), sampleAudit()));

        assertTrue(ex.getMessage().contains("invalid characters"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsLabelEqualToAKnownTypeIgnoringCase() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of("Others"), List.of("  psa birth certificate "), List.of(pdf("a.pdf")), sampleAudit()));

        assertTrue(ex.getMessage().contains("pick it from the type list"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadBatchRejectsLabelCountMismatch() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.uploadBatch("SR20260001",
                List.of("Others"), List.of("A", "B"), List.of(pdf("a.pdf")), sampleAudit()));

        assertEquals("The number of files and document labels must match.", ex.getMessage());
        verify(documentRepository, never()).save(any());
    }
```

- [ ] **Step 2: Write the failing controller tests and update the existing stubs**

In `DocumentControllerWebMvcTest.java`, the three existing `uploadBatch` stubs must use the new 5-argument overload (the controller will call it). Change:

- `uploadBatchReturns201WithOrderedSummaries`: `when(documentService.uploadBatch(eq("SR20260001"), any(), any(), any()))` → `when(documentService.uploadBatch(eq("SR20260001"), any(), any(), any(), any()))`
- `uploadBatchReturns400WhenServiceRejectsValidation`: `when(documentService.uploadBatch(any(), any(), any(), any()))` → `when(documentService.uploadBatch(any(), any(), any(), any(), any()))`
- `uploadBatchReturns404WhenStudentUnknown`: `when(documentService.uploadBatch(eq("NOPE"), any(), any(), any()))` → `when(documentService.uploadBatch(eq("NOPE"), any(), any(), any(), any()))`

Add after `uploadBatchUnauthorizedWhenAnonymous()`:

```java
    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void uploadBatchForwardsLabelsWithoutSplittingOnCommas() throws Exception {
        var f1 = new MockMultipartFile("files", "a.pdf", "application/pdf", "aaa".getBytes(StandardCharsets.UTF_8));
        when(documentService.uploadBatch(eq("SR20260001"), any(), any(), any(), any())).thenReturn(List.of());

        mvc.perform(multipart("/api/registrar/documents/batch")
                        .file(f1)
                        .param("studentId", "SR20260001")
                        .param("documentTypes", "Others")
                        .param("documentLabels", "Barangay Clearance, 2026")
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(documentService).uploadBatch(eq("SR20260001"), eq(List.of("Others")),
                eq(List.of("Barangay Clearance, 2026")), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void uploadBatchPassesNullLabelsWhenTheParameterIsAbsent() throws Exception {
        var f1 = new MockMultipartFile("files", "a.pdf", "application/pdf", "aaa".getBytes(StandardCharsets.UTF_8));
        when(documentService.uploadBatch(eq("SR20260001"), any(), any(), any(), any())).thenReturn(List.of());

        mvc.perform(multipart("/api/registrar/documents/batch")
                        .file(f1)
                        .param("studentId", "SR20260001")
                        .param("documentTypes", TOR_TYPE)
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(documentService).uploadBatch(eq("SR20260001"), eq(List.of(TOR_TYPE)), isNull(), any(), any());
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.service.DocumentServiceTest" --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"`
Expected: FAIL — compilation error: no `uploadBatch` overload taking 5 arguments.

- [ ] **Step 4: Implement label validation in `DocumentService`**

In `DocumentService.java`:

Add constants next to `MAX_BATCH_TOTAL_BYTES`:

```java
    /** Custom "Others" name limit — matches documents.document_label VARCHAR(100). */
    private static final int MAX_LABEL_LENGTH = 100;
    private static final Pattern LABEL_CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");
```

Replace the `ValidatedFile` record:

```java
    /** One validated, ready-to-persist file from a {@link #uploadBatch} request. */
    private record ValidatedFile(MultipartFile file, String documentType, String documentLabel,
                                 String fileName, String mimeType) {
    }
```

Replace the whole existing `uploadBatch(String, List<String>, List<MultipartFile>, DocumentAuditContext)` method (javadoc included) with these two methods plus the helper:

```java
    /** Batch upload without custom names — every file is stored unlabelled. */
    @Transactional
    public List<DocumentSummaryResponse> uploadBatch(String studentId, List<String> documentTypes,
                                                      List<MultipartFile> files, DocumentAuditContext audit) {
        return uploadBatch(studentId, documentTypes, null, files, audit);
    }

    /**
     * Validates and saves an entire batch of files for one student inside a
     * single database transaction: either every file and the one success
     * audit row are committed, or nothing is (spec §4). All validation runs
     * before any file is read or persisted. {@code documentLabels} is
     * optional (null = no names) and parallel to {@code files}; a label is
     * only allowed on an "Others" document (spec 2026-10-01 §5.2).
     */
    @Transactional
    public List<DocumentSummaryResponse> uploadBatch(String studentId, List<String> documentTypes,
                                                      List<String> documentLabels,
                                                      List<MultipartFile> files, DocumentAuditContext audit) {
        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("A student ID is required.");
        }
        // Unlike upload()/requireStudent() (400), an unknown-but-well-formed
        // reference here is a 404 per spec §4 — the field itself was valid.
        String trimmedStudentId = studentId.trim();
        StudentRecord student = studentRecordRepository.findByStudentId(trimmedStudentId)
                .orElseThrow(() -> new NoSuchElementException("No student record found for ID: " + trimmedStudentId));

        if (files == null || documentTypes == null) {
            throw new IllegalArgumentException("Files and document types are required.");
        }
        if (files.size() != documentTypes.size()) {
            throw new IllegalArgumentException("The number of files and document types must match.");
        }
        if (documentLabels != null && documentLabels.size() != files.size()) {
            throw new IllegalArgumentException("The number of files and document labels must match.");
        }
        int count = files.size();
        if (count == 0) {
            throw new IllegalArgumentException("At least one file is required.");
        }
        if (count > MAX_BATCH_FILES) {
            throw new IllegalArgumentException("A maximum of " + MAX_BATCH_FILES + " files may be uploaded at once.");
        }

        List<ValidatedFile> validated = new ArrayList<>(count);
        long totalSize = 0;
        for (int i = 0; i < count; i++) {
            MultipartFile file = files.get(i);
            String documentType = documentTypes.get(i);
            int position = i + 1;

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File at position " + position + " is empty.");
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("File at position " + position + " exceeds the 10MB size limit.");
            }
            totalSize += file.getSize();

            if (ID_PICTURE_TYPE.equals(documentType)) {
                throw new IllegalArgumentException(
                        "ID Picture uploads use a dedicated endpoint, not batch upload (file " + position + ").");
            }
            requireKnownType(documentType);
            String documentLabel = normalizeLabel(
                    documentLabels == null ? null : documentLabels.get(i), documentType, position);

            String originalName = requireValidFileName(file.getOriginalFilename(), position);

            String extension = extensionOf(originalName);
            String mimeType = ALLOWED_EXTENSIONS.get(extension);
            if (mimeType == null) {
                throw new IllegalArgumentException(
                        "Unsupported file type at position " + position + ". Allowed: "
                                + String.join(", ", ALLOWED_EXTENSIONS.keySet()));
            }

            validated.add(new ValidatedFile(file, documentType, documentLabel, originalName, mimeType));
        }

        if (totalSize > MAX_BATCH_TOTAL_BYTES) {
            throw new IllegalArgumentException("Combined upload size exceeds the 50MB limit.");
        }

        List<Integer> savedIds = new ArrayList<>(count);
        for (ValidatedFile vf : validated) {
            byte[] content;
            try {
                content = vf.file().getBytes();
            } catch (IOException e) {
                // Unchecked so the surrounding @Transactional method still rolls
                // back — Spring only auto-rolls-back on RuntimeException/Error.
                throw new UncheckedIOException("Could not read an uploaded file. Please try again.", e);
            }
            Document saved = save(student, vf.documentType(), vf.documentLabel(), vf.fileName(), vf.mimeType(),
                    content);
            savedIds.add(saved.getDocumentId());
        }

        systemLogService.logAction(audit.userId(), audit.username(), audit.role(),
                "Uploaded " + count + " document(s) for student " + student.getStudentId(),
                audit.ipAddress());

        documentRepository.flush();
        Map<Integer, DocumentSummaryResponse> byId = new LinkedHashMap<>();
        for (DocumentSummaryResponse summary : documentRepository.findSummariesByIds(savedIds)) {
            byId.put(summary.documentId(), summary);
        }
        return savedIds.stream().map(byId::get).toList();
    }

    /**
     * Trims a custom "Others" name; blank becomes null. Rejects a name on any
     * other type, an over-long name, control characters, and a name equal to
     * a real type — a real required document must never hide under "Others".
     */
    private static String normalizeLabel(String rawLabel, String documentType, int position) {
        if (!StringUtils.hasText(rawLabel)) {
            return null;
        }
        String label = rawLabel.trim();
        if (!OTHERS_TYPE.equals(documentType)) {
            throw new IllegalArgumentException(
                    "A document name can only be given to 'Others' documents (file " + position + ").");
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException(
                    "Document name exceeds " + MAX_LABEL_LENGTH + " characters (file " + position + ").");
        }
        if (LABEL_CONTROL_CHARS.matcher(label).find()) {
            throw new IllegalArgumentException(
                    "Document name contains invalid characters (file " + position + ").");
        }
        for (String type : DOCUMENT_TYPES) {
            if (type.equalsIgnoreCase(label)) {
                throw new IllegalArgumentException("'" + label + "' is a document type — pick it from the type "
                        + "list instead of using Others (file " + position + ").");
            }
        }
        return label;
    }
```

Replace the private `save(...)` helper with a version that takes the label:

```java
    private Document save(StudentRecord student, String documentType, String documentLabel,
                          String fileName, String mimeType, byte[] content) {
        Document document = new Document();
        document.setStudent(student);
        document.setDocumentType(documentType);
        document.setDocumentLabel(documentLabel);
        document.setFileName(fileName);
        document.setFileType(mimeType);
        document.setFileSize(content.length);
        document.setContentData(content);
        return documentRepository.save(document);
    }
```

Update its two other callers to pass `null` for the label:
- in `upload(...)`: `return toSummary(save(student, documentType, null, originalName, mimeType, content));`
- at the end of `saveGenerated(...)`: `return toSummary(save(student, documentType, null, cleanName, "text/html", content));`

- [ ] **Step 5: Read labels in the controller without comma-splitting**

In `DocumentController.java`, add `import java.util.Arrays;` (next to `import java.util.List;`) and replace `uploadBatch(...)`:

```java
    @PostMapping("/batch")
    public ResponseEntity<List<DocumentSummaryResponse>> uploadBatch(
            @RequestParam("studentId") String studentId,
            @RequestParam("documentTypes") List<String> documentTypes,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest httpRequest
    ) {
        LogContext ctx = getLogContext();
        DocumentAuditContext audit = new DocumentAuditContext(ctx.userId(), ctx.username(), ctx.role(),
                httpRequest.getRemoteAddr());

        // Read raw values: @RequestParam List<String> would split a single
        // label such as "Barangay Clearance, 2026" on its comma.
        String[] rawLabels = httpRequest.getParameterValues("documentLabels");
        List<String> documentLabels = rawLabels == null ? null : Arrays.asList(rawLabels);

        List<DocumentSummaryResponse> saved = documentService.uploadBatch(
                studentId, documentTypes, documentLabels, files, audit);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.service.DocumentServiceTest" --tests "com.example.springboot.controller.DocumentControllerWebMvcTest" --tests "com.example.springboot.integration.DocumentStorageIntegrationTest"`
Expected: `BUILD SUCCESSFUL`; 7 new service tests and 2 new controller tests pass; every existing `uploadBatch` test (4-arg service calls, H2 rollback tests) still passes.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/springboot/service/DocumentService.java \
        src/main/java/com/example/springboot/controller/DocumentController.java \
        src/test/java/com/example/springboot/service/DocumentServiceTest.java \
        src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java
git commit -m "feat: accept and validate custom names for Others documents on batch upload"
```

---

### Task 6: Pre-export dialog on the Documents page

**Files:**
- Modify: `src/main/resources/static/documents.html` (page `<style>`, new modal before `<!-- ===== EDIT ACCOUNT MODAL ===== -->`, script tag)
- Modify: `src/main/resources/static/js/registrar-documents.js`

No automated JS tests exist in this project; this task is verified live in Task 8.

- [ ] **Step 1: Add the page CSS**

In `documents.html`, inside the page `<style>` block, directly after the `.export-note { ... }` rule, add:

```css
        .missing-doc-icon {
            align-items: center;
            background: var(--bs-danger);
            border-radius: 50%;
            color: #fff;
            display: inline-flex;
            flex: 0 0 auto;
            font-size: 0.75rem;
            font-weight: 700;
            height: 1.25rem;
            justify-content: center;
            width: 1.25rem;
        }
        .export-check-row {
            border-bottom: 1px solid #f1f3f5;
            padding: 0.6rem 0;
        }
        .export-check-row:last-child {
            border-bottom: none;
        }
```

- [ ] **Step 2: Add the modal markup**

In `documents.html`, insert directly before `<!-- ===== EDIT ACCOUNT MODAL ===== -->`:

```html
    <!-- ===== PRE-EXPORT MISSING-DOCUMENTS CHECK MODAL ===== -->
    <div class="modal fade" id="exportCheckModal" tabindex="-1" aria-labelledby="exportCheckModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="exportCheckModalLabel">Missing documents</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body" id="exportCheckBody"></div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-surface-secondary" data-bs-dismiss="modal" id="exportCheckCancelBtn">Cancel</button>
                    <button type="button" class="btn btn-primary" id="exportCheckProceedBtn">Export anyway</button>
                </div>
            </div>
        </div>
    </div>

```

Change the script tag `<script src="js/registrar-documents.js?v=5"></script>` to `?v=6`.

- [ ] **Step 3: Wire the modal in `registrar-documents.js`**

Add to the "Table state" variable block (after `let documentTypeChoices = [];`):

```js
    let exportCheckModal = null;
    let exportCheckContext = null; // { url, check, label, btn } of the export being checked
```

In the `$(document).ready` handler, after `deleteDocumentModal = new bootstrap.Modal(...)`, add:

```js
        exportCheckModal = new bootstrap.Modal(document.getElementById('exportCheckModal'));
```

and after `setupSharedActions();` add:

```js
        setupExportCheck();
```

- [ ] **Step 4: Replace `buildExportButton` and its four call sites**

Replace `buildExportButton`:

```js
    /**
     * Export buttons run the pre-export missing-documents check first; the ZIP
     * itself is still the unchanged streaming GET (exportUrl).
     * check = { scope: 'student'|'section'|'unassigned'|'batch', key, scopeLabel }.
     */
    function buildExportButton(label, url, check, disabled) {
        if (disabled) {
            return el('button', { type: 'button', class: 'btn btn-surface-secondary disabled', 'aria-disabled': 'true', disabled: 'disabled', text: label });
        }
        const btn = el('button', { type: 'button', class: 'btn btn-primary', text: label });
        btn.addEventListener('click', function () {
            runExportCheck({ url: url, check: check, label: label, btn: btn });
        });
        return btn;
    }
```

In `renderContentPanel`, replace the `section` and `unassigned` branches:

```js
        else if (selection.kind === 'section') renderStudentListView(ctx.section.students,
            'Section ' + ctx.section.sectionCode, {
                url: exportUrl('section', ctx.section.sectionCode),
                label: 'Export Section',
                check: { scope: 'section', key: ctx.section.sectionCode, scopeLabel: 'Section ' + ctx.section.sectionCode }
            });
        else if (selection.kind === 'unassigned') renderStudentListView(ctx.batch.unassignedStudents,
            'Unassigned Students',
            ctx.batch.batchCode ? {
                url: exportUrl('unassignedBatch', batchNodeKey(ctx.batch)),
                label: 'Export Unassigned',
                check: { scope: 'unassigned', key: ctx.batch.batchCode, scopeLabel: 'Unassigned Students of ' + ctx.batch.batchCode }
            } : null);
```

In `renderBatchOverview`, replace the `buildExportButton(...)` line:

```js
            wrap.appendChild(buildExportButton('Export Entire Batch', exportUrl('batch', batch.batchCode),
                { scope: 'batch', key: batch.batchCode, scopeLabel: 'Batch ' + batch.batchCode },
                batch.documentCount === 0));
```

In `renderStudentListView`, replace the `buildExportButton(...)` line:

```js
            wrap.appendChild(buildExportButton(exportConfig.label, exportConfig.url, exportConfig.check, totalDocs === 0));
```

In `renderStudentDetail`, replace the `right.appendChild(buildExportButton(...))` line:

```js
        right.appendChild(buildExportButton('Export Student', exportUrl('student', student.studentId),
            { scope: 'student', key: student.studentId, scopeLabel: student.lastName + ', ' + student.firstName },
            student.documentCount === 0));
```

- [ ] **Step 5: Add the check flow**

Add a new section directly after `exportNote()`:

```js
    // -------------------------------------------------------
    // Pre-export missing-documents check (dialog only — nothing is left
    // on the page once the dialog closes)
    // -------------------------------------------------------

    function exportCheckUrl(check) {
        return '/api/registrar/documents/export-check/' + encodeURIComponent(check.scope)
            + '/' + encodeURIComponent(check.key);
    }

    function startDownload(url) {
        window.location.href = url;
    }

    function runExportCheck(ctx) {
        ctx.btn.disabled = true;
        ctx.btn.textContent = 'Checking…';
        fetchExportCheck(ctx, function () {
            ctx.btn.disabled = false;
            ctx.btn.textContent = ctx.label;
        });
    }

    function fetchExportCheck(ctx, onComplete) {
        $.ajax({
            url: exportCheckUrl(ctx.check),
            method: 'GET',
            success: function (result) {
                if (!result.flagged.length) {
                    exportCheckModal.hide();
                    startDownload(ctx.url);
                    return;
                }
                renderExportCheckResult(ctx, result);
                openExportCheck(ctx);
            },
            error: function (xhr) {
                renderExportCheckError(ctx, xhr);
                openExportCheck(ctx);
            },
            complete: function () {
                if (onComplete) onComplete();
            }
        });
    }

    function openExportCheck(ctx) {
        exportCheckContext = ctx;
        exportCheckModal.show(); // no-op when already open (Retry)
    }

    function setExportCheckFooter(proceedText) {
        const proceed = document.getElementById('exportCheckProceedBtn');
        if (proceedText) {
            proceed.textContent = proceedText;
            proceed.classList.remove('d-none');
        } else {
            proceed.classList.add('d-none');
        }
    }

    function renderExportCheckResult(ctx, result) {
        document.getElementById('exportCheckModalLabel').textContent = 'Missing documents';
        const body = document.getElementById('exportCheckBody');
        body.innerHTML = '';

        const flaggedCount = result.flagged.length;
        const summary = el('p', { class: 'mb-3' });
        summary.appendChild(el('strong', { text: flaggedCount + ' of ' + result.studentsInScope }));
        summary.appendChild(document.createTextNode(
            ' student' + (result.studentsInScope === 1 ? '' : 's') + ' in '));
        summary.appendChild(el('em', { text: ctx.check.scopeLabel }));
        summary.appendChild(document.createTextNode(
            ' ' + (flaggedCount === 1 ? 'is' : 'are') + ' missing required documents.'));
        body.appendChild(summary);

        const list = el('ul', { class: 'list-unstyled mb-0' });
        result.flagged.forEach(function (s) {
            const item = el('li', { class: 'export-check-row' });
            const head = el('div', { class: 'd-flex align-items-center flex-wrap gap-2' });
            head.appendChild(el('span', { class: 'missing-doc-icon', 'aria-hidden': 'true', text: '!' }));
            head.appendChild(el('span', { class: 'visually-hidden', text: 'Missing documents:' }));
            head.appendChild(el('strong', { text: s.lastName + ', ' + s.firstName }));
            head.appendChild(el('span', {
                class: 'text-muted small',
                text: 'Reference No. ' + s.studentId + ' · ' + (s.studentStatus || 'Unknown')
            }));
            item.appendChild(head);
            item.appendChild(el('div', { class: 'small ms-4 mt-1', text: 'Missing: ' + s.missing.join(', ') }));
            if (s.documentCount === 0) {
                item.appendChild(el('div', {
                    class: 'small text-muted ms-4',
                    text: "No documents — won't be included in the ZIP."
                }));
            }
            list.appendChild(item);
        });
        body.appendChild(list);
        setExportCheckFooter('Export anyway');
    }

    function renderExportCheckError(ctx, xhr) {
        document.getElementById('exportCheckModalLabel').textContent = "Couldn't check documents";
        const body = document.getElementById('exportCheckBody');
        body.innerHTML = '';

        if (xhr.status === 401) {
            body.appendChild(buildSessionExpiredNotice());
            setExportCheckFooter(null);
        } else if (xhr.status === 404) {
            body.appendChild(el('p', { class: 'mb-0', text: 'This item is no longer available — refresh to continue.' }));
            setExportCheckFooter(null);
        } else {
            body.appendChild(buildRetryableError("Couldn't check for missing documents.", function () {
                body.innerHTML = '';
                body.appendChild(el('p', { class: 'text-muted mb-0', text: 'Checking…' }));
                fetchExportCheck(ctx, null);
            }));
            // A failed check must never block an export.
            setExportCheckFooter('Export without checking');
        }
    }

    function setupExportCheck() {
        const modalEl = document.getElementById('exportCheckModal');

        document.getElementById('exportCheckProceedBtn').addEventListener('click', function () {
            const url = exportCheckContext ? exportCheckContext.url : null;
            exportCheckModal.hide();
            if (url) startDownload(url);
        });

        modalEl.addEventListener('shown.bs.modal', function () {
            document.getElementById('exportCheckCancelBtn').focus();
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            exportCheckContext = null;
            document.getElementById('exportCheckBody').innerHTML = '';
        });
    }
```

- [ ] **Step 6: Syntax check**

Run: `node --check src/main/resources/static/js/registrar-documents.js`
Expected: no output, exit code 0. (If `node` is not installed, skip — Task 8's live check covers it — and note it in the task report.)

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/documents.html src/main/resources/static/js/registrar-documents.js
git commit -m "feat: show missing-documents dialog before document exports"
```

---

### Task 7: Custom name field on staged "Others" files and label display

**Files:**
- Modify: `src/main/resources/static/documents.html` (page `<style>`)
- Modify: `src/main/resources/static/js/registrar-documents.js`

- [ ] **Step 1: Let staged rows wrap and size the name field**

In `documents.html`, in the existing `.staged-file-row { ... }` rule, add `flex-wrap: wrap;` after `display: flex;`. Then add directly after the `.staged-file-row select { ... }` rule:

```css
        .staged-file-row .staged-file-label {
            flex: 1 0 100%;
        }
```

- [ ] **Step 2: Track labels in staging state and load suggestions**

In `registrar-documents.js`:

Change the staging comment and add the suggestions variable:

```js
    let stagedFiles = []; // [{ file, documentType, documentLabel }]
    let documentLabelChoices = [];
```

In `addStagedFiles`, change the pushed object to:

```js
            stagedFiles.push({ file: file, documentType: 'Others', documentLabel: '' });
```

In `setupUploadModal`, inside the `show.bs.modal` handler, add at the end (after `updateUploadSubmitState();`):

```js
            loadDocumentLabelChoices();
```

Add this function directly after `setupUploadModal`:

```js
    function loadDocumentLabelChoices() {
        $.ajax({
            url: '/api/registrar/documents/labels',
            method: 'GET',
            success: function (labels) {
                documentLabelChoices = labels;
                renderStagedFiles();
            }
        });
    }
```

- [ ] **Step 3: Render the name field for "Others" rows**

In `renderStagedFiles`, replace

```js
            select.addEventListener('change', function () { entry.documentType = select.value; });
            row.appendChild(select);
```

with:

```js
            select.addEventListener('change', function () {
                entry.documentType = select.value;
                if (entry.documentType !== 'Others') entry.documentLabel = '';
                renderStagedFiles();
            });
            row.appendChild(select);
```

and insert, directly before `container.appendChild(row);`:

```js
            if (entry.documentType === 'Others') {
                const labelWrap = el('div', { class: 'staged-file-label' });
                const inputId = 'stagedLabel' + index;
                labelWrap.appendChild(el('label', {
                    for: inputId, class: 'visually-hidden', text: 'Document name for ' + entry.file.name
                }));
                const input = el('input', {
                    type: 'text', id: inputId, class: 'form-control form-control-sm', maxlength: '100',
                    placeholder: 'Specify document name (optional)'
                });
                input.value = entry.documentLabel;
                input.addEventListener('input', function () { entry.documentLabel = input.value; });
                labelWrap.appendChild(input);
                row.appendChild(labelWrap);
                SrmsCombobox.attach(input, {
                    items: documentLabelChoices.map(function (l) { return { value: l, label: l }; }),
                    emptyText: 'No saved names yet — type a new one.'
                });
            }
```

(`SrmsCombobox.attach` needs the input to already have a parent, which `labelWrap` provides; picking a suggestion fires `input`, which updates `entry.documentLabel`.)

- [ ] **Step 4: Send labels with the upload**

In `submitBatchUpload`, inside the `stagedFiles.forEach`, after `formData.append('documentTypes', entry.documentType);` add:

```js
            formData.append('documentLabels', entry.documentType === 'Others' ? entry.documentLabel.trim() : '');
```

- [ ] **Step 5: Show "Others — name" in the table and the folder view**

Add to the Helpers section (after `formatSize`):

```js
    /** "Others — Medical Certificate" for a named Others document, else the type. */
    function displayType(doc) {
        return doc.documentLabel ? doc.documentType + ' — ' + doc.documentLabel : doc.documentType;
    }
```

In `initTable`, replace the column `{ data: 'documentType', render: escapeHtml },` with:

```js
                {
                    data: null,
                    render: function (data) {
                        return escapeHtml(displayType(data));
                    }
                },
```

In `renderStudentDocList`, change the row text to:

```js
                text: doc.fileName + ' (' + displayType(doc) + ', ' + formatSize(doc.fileSize) + ')'
```

(Leave every `data-type` attribute as `documentType` — the Edit link for generated documents needs the real type. The View and Delete dialogs never show a type, so they need no change.)

- [ ] **Step 6: Syntax check**

Run: `node --check src/main/resources/static/js/registrar-documents.js`
Expected: no output, exit code 0 (skip if `node` is unavailable; note it).

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/documents.html src/main/resources/static/js/registrar-documents.js
git commit -m "feat: name Others documents on upload and show the name in listings"
```

---

### Task 8: Full verification, live check, memory bank

**Files:**
- Modify: `memory-bank/activeContext.md`, `memory-bank/progress.md`, `memory-bank/changeLog.md`, `memory-bank/testing.md`, `memory-bank/decisions.md` (only if a decision changed), `CLAUDE.md` (Key Conventions: one bullet)

- [ ] **Step 1: Full suite**

```bash
./gradlew test
find build/test-results/test -name '*.xml' -exec grep -h -o 'tests="[0-9]*"' {} + | awk -F'"' '{s+=$2} END {print s}'
grep -h -o 'failures="[0-9]*"\|errors="[0-9]*"' build/test-results/test/*.xml | sort | uniq -c
```

Expected: `BUILD SUCCESSFUL`; count `578`; only `failures="0"` / `errors="0"` lines. If the count differs, list which new tests are missing/extra before continuing.

- [ ] **Step 2: Apply the migration to the live dev DB — ask the user first**

This changes the real `AnihanSRMS` database. **Ask the user for explicit approval**, then apply `src/main/sql/migrations/2026-10-01-add-documents-document-label.sql` with their usual MySQL client (e.g. MySQL Workbench, or the `mysql` CLI against `localhost:3306` as `root`). Confirm the verification SELECTs show `document_label VARCHAR(100) YES` right after `document_type`. Running it a second time must be a no-op.

- [ ] **Step 3: Live check with Playwright (logged in as `registrar` / `password123`)**

Start the app: `./gradlew bootRun` (background). Then on `/documents.html` → Folder Explorer:

1. Pick a student who has PSA, Form 137, and ID Picture and is not Graduated → **Export Student** → ZIP downloads immediately, no dialog.
2. Pick a section containing a student with gaps → **Export Section** → dialog lists exactly the flagged students with a red "!", the right "Missing: …" items, and the zero-document note where applicable. **Cancel** → dialog closes; nothing remains in the tree or panel.
3. Same section → **Export anyway** → ZIP downloads.
4. **Export Entire Batch** on that batch → dialog includes both section and unassigned flagged students.
5. As `admin`, open `/logs.html` → the newest export row reads `Requested ZIP export (section …, N document(s), X of Y student(s) with missing documents)`.
6. Upload dialog → stage one file, keep type **Others**, type `Medical Certificate`, Upload → the table shows `Others — Medical Certificate`; the folder view's student document list shows the same.
7. Stage another file as Others → the name field suggests `Medical Certificate`. Switch that row to **Form 137** → the name field disappears.
8. Type filter **Others** → shows labelled and unlabelled Others documents. Search box `medical` → finds the labelled document.
9. Try naming an Others file `psa birth certificate` → upload rejected with "… pick it from the type list …".
10. Browser console: no new errors (pre-existing `favicon.ico` 404 and the expected 404 for a missing ID picture are fine).

Record each item as pass/fail in `memory-bank/testing.md`. Note any test data created (uploaded files) so the user can delete it.

- [ ] **Step 4: Update the memory bank and CLAUDE.md**

Add newest-first entries (top of each file):
- `activeContext.md` — branch `feature/pre-export-missing-documents`, commits, test count, live-check result, migration applied (Confirmed/Blocked), open items.
- `progress.md` — completed / deferred.
- `changeLog.md` — every file from the File Structure table and why.
- `testing.md` — suite count + the 10 live-check results.
- `CLAUDE.md` → **Key Conventions**, add one bullet:
  `**Required documents & labels:** what counts as "missing" before an export lives only in service/RequiredDocumentPolicy.java (intake docs for everyone; TOR, any one Form IX, OJT Report, TVET certificate for Graduated). "Others" documents may carry a custom documents.document_label; it never satisfies a requirement.`

```bash
git add memory-bank/ CLAUDE.md
git commit -m "docs: record pre-export missing-documents check and Others labels"
```

- [ ] **Step 5: Hand off**

Do **not** merge or push. Report the branch, commits, test count, live-check results, and test data left behind, then invoke `superpowers:finishing-a-development-branch` so the user chooses merge or PR.
