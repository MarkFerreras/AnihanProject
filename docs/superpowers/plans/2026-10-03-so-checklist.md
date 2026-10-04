# R4.2 SO Checklist Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use **superpowers:subagent-driven-development** to implement this plan task-by-task. This is the user's confirmed choice (2026-10-03): do not ask again, and do not switch to executing-plans. Dispatch a fresh subagent per task, in order (Task 0 → Task 15), with spec-compliance and code-quality reviews as described under "Execution efficiency" below. Steps use checkbox (`- [ ]`) syntax for tracking. Task 15 Steps 2–3 (backing up and migrating the live DB) must stop and ask the user. Task 9 is **Confirmed** by the user (2026-10-03), so run it.

**Execution efficiency (follow while running the plan):**
- Paste only the one task's text (plus the header's Conventions and Deviations) into each implementer subagent prompt. Never ask a subagent to read this whole plan (≈45k tokens).
- Subagents read memory-bank logs top-only, per CLAUDE.md. They do not read the spec unless the task text points to a section.
- Run per-task Gradle commands with `-q` (e.g. `./gradlew test -q --tests "..."`) so only failures print. Run the full suite only where the plan says to (Tasks 1 and 15).
- Reviews: give Tasks 1–4, 6–8, 11 and 13 their own spec + code-quality review. Review the small tasks (5, 9, 10, 12, 14) together in one batched pass after Task 14. Re-review only what a fix changed.
- Task 15 live check: prefer `browser_evaluate` / `browser_network_requests` over full `browser_snapshot`; take a snapshot only to diagnose a failure.
- Don't re-run a check that already passed unless code changed since.

**Goal:** Show the Registrar, per student, which TESDA Special Order (SO) requirements are met, met with a warning, unmet, or not yet due. The checklist is computed live, which needs a new `Completed` status, two new student fields, and an "Others"-label hint on upload.

**Architecture:** Two pure, static rule classes decide everything: `StudentStatusTransitions` (which status moves are allowed and what input each needs) and `SoReadinessPolicy` (item states from student facts, document types and grade rows). `SoChecklistService` gathers those facts with three BLOB-free `JdbcTemplate` queries. A new read-only endpoint `GET /api/registrar/student-records/{recordId}/so-checklist` feeds a new **SO Checklist** tab in the `registrar.html` details modal. The status endpoint now enforces the transition rules and records a completion date or reason. The edit form gains editable enrollment/completion dates and an Employment Status select. A third pure class, `DocumentTypeSuggester`, powers a "Did you mean …?" hint on "Others" upload labels.

**Tech Stack:** Java 25, Spring Boot 4.0.4, Spring Security 7, Spring Data JPA + `JdbcTemplate`, MySQL 8 (H2 MySQL-mode in tests), JUnit 5 + Mockito + MockMvc, Bootstrap 5.3, jQuery 4, vanilla JS.

**Spec:** `docs/superpowers/specs/2026-10-01-so-checklist-design.md`. Read it first. Section numbers (§) below refer to it.

**Deviations from the spec (decided while planning, verified against code):**
1. The checklist endpoint lives in a new `SoChecklistController` (same URL as the spec) instead of `RegistrarController`. Four existing `@WebMvcTest(RegistrarController.class)` classes would otherwise all need a new `@MockitoBean`.
2. Section-first course/batch is resolved in SQL (`COALESCE(sec.course_code, s.course_code)`), so it is pinned by the real-H2 test (Task 7), not by `SoReadinessPolicyTest`.
3. Employment Status values live as `EMPLOYMENT_STATUS_PATTERN` / `EMPLOYMENT_STATUS_DISPLAY` constants on `StudentRecordUpdateRequest` (same shape as `UpdateStudentStatusRequest`). The `<select>` in `student-records.html` is static HTML, pinned to the constant by `FrontendContractTest`.
4. For Form IX aliases the suggester returns the generic `"Form IX"`. The page then says "pick the matching Form IX type" and offers no one-click Switch, because there are three Form IX types.

**Conventions for every task:**
- Work only on branch `feature/so-checklist` (already created from `main` at `6f40bc3`). Never commit to `main`.
- Commit messages: plain conventional commits, **no `Co-Authored-By` trailer** (the user's standing preference).
- Run Gradle from the repo root in Git Bash: `./gradlew ...`. Tests use H2/mocks only; no live DB is needed until Task 15.
- `system_logs` is append-only. Never edit or delete log rows.
- Stage files by name (`git add <paths>`), never `git add -A`.

---

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `src/main/sql/migrations/2026-10-03-so-checklist.sql` | Create | Idempotent `ADD COLUMN completion_date`, `employment_status` |
| `src/main/sql/schema.sql`, `src/main/sql/AnihanSRMS.sql` | Modify | Mirror the two columns |
| `src/test/resources/document-storage-h2-schema.sql` | Modify | Mirror the two columns for real-H2 tests |
| `src/test/resources/so-checklist-h2-schema.sql` | Create | `classes`, `class_enrollments`, `grades` for the checklist H2 test |
| `src/main/java/com/example/springboot/model/StudentRecord.java` | Modify | `completionDate`, `employmentStatus` fields |
| `src/main/java/com/example/springboot/service/StudentStatusTransitions.java` | Create | Pure status-move rules (§4.2) |
| `src/main/java/com/example/springboot/dto/registrar/UpdateStudentStatusRequest.java` | Modify | `Completed` value; `completionDate`, `reason` |
| `src/main/java/com/example/springboot/service/RegistrarService.java` | Modify | Enforce transitions in `updateStatus`; dates + employment in `updateRecord` |
| `src/main/java/com/example/springboot/controller/RegistrarController.java` | Modify | Pass new status inputs; extended audit text (§10) |
| `src/main/java/com/example/springboot/dto/registrar/StudentRecordUpdateRequest.java` | Modify | `enrollmentDate`, `completionDate`, `employmentStatus` + employment constants |
| `src/main/java/com/example/springboot/dto/registrar/StudentRecordDetailsResponse.java` | Modify | `completionDate`, `employmentStatus` |
| `src/main/java/com/example/springboot/service/RequiredDocumentPolicy.java` | Modify | Completion documents apply to Completed **and** Graduated |
| `src/main/java/com/example/springboot/service/SoReadinessPolicy.java` | Create | Pure checklist rules (§6) |
| `src/main/java/com/example/springboot/dto/registrar/SoChecklistResponse.java` | Create | Checklist result |
| `src/main/java/com/example/springboot/dto/registrar/SoChecklistItem.java` | Create | One checklist row |
| `src/main/java/com/example/springboot/service/SoChecklistService.java` | Create | Three BLOB-free reads → `SoReadinessPolicy` |
| `src/main/java/com/example/springboot/controller/SoChecklistController.java` | Create | `GET /api/registrar/student-records/{id}/so-checklist` |
| `src/main/java/com/example/springboot/service/DocumentTypeSuggester.java` | Create | Pure "Others" label → document type hint (§9) |
| `src/main/java/com/example/springboot/dto/registrar/TypeSuggestionResponse.java` | Create | `{ suggestedType }` |
| `src/main/java/com/example/springboot/controller/DocumentController.java` | Modify | `GET /type-suggestion` |
| `src/main/java/com/example/springboot/controller/StudentPortalController.java` | Modify (Task 9, confirmed) | Pre-check also blocks Completed/Graduated |
| `src/main/resources/static/css/dashboard.css` | Modify | `.status-badge-completed` |
| `src/main/resources/static/registrar.html` | Modify | Completed filter; Edit Status fields; details-modal tabs; checklist CSS |
| `src/main/resources/static/js/registrar-students.js` | Modify | Completed badge; transition-aware status dialog; SO Checklist tab |
| `src/main/resources/static/student-numbers.html`, `js/registrar-student-numbers.js` | Modify | Completed filter + badge |
| `src/main/resources/static/student-records.html`, `js/registrar-student-records-edit.js` | Modify | Editable dates; Employment section |
| `src/main/resources/static/documents.html`, `js/registrar-documents.js` | Modify | "Did you mean …?" hint on staged "Others" rows |
| Tests (see each task) | Create/Modify | see each task |

**Expected test count at the end:** 578 baseline + 86 new = **664**, 0 failures.

| Task | New tests |
|---|---|
| 1 Schema | 1 |
| 2 StudentStatusTransitions | 13 |
| 3 Status endpoint | 15 (9 service + 6 controller) |
| 4 Edit-form fields | 9 (6 service + 3 controller) |
| 5 RequiredDocumentPolicy | 1 |
| 6 SoReadinessPolicy | 21 |
| 7 Checklist service + endpoint | 8 (4 real-H2 + 4 controller) |
| 8 DocumentTypeSuggester | 12 (9 unit + 3 controller) |
| 9 Portal pre-check | 3 |
| 10–12 Frontend contracts | 3 |

---

### Task 0: Confirm branch and baseline

**Files:** none.

- [ ] **Step 1: Confirm the branch**

```bash
git branch --show-current   # expect: feature/so-checklist
git status --short          # expect: nothing, or only this plan file if it is not committed yet
```

If the branch is `main`, stop. Run `git checkout feature/so-checklist`, and if that fails, run `git checkout -b feature/so-checklist`.

- [ ] **Step 2: Record the baseline test count**

```bash
./gradlew test
find build/test-results/test -name '*.xml' -exec grep -h -o 'tests="[0-9]*"' {} + | awk -F'"' '{s+=$2} END {print s}'
```

Expected: `BUILD SUCCESSFUL`, and the count prints `578`. If it differs, stop and report, because every later count in this plan assumes 578.

---

### Task 1: Schema: `completion_date` and `employment_status`

**Files:**
- Create: `src/main/sql/migrations/2026-10-03-so-checklist.sql`
- Modify: `src/main/sql/schema.sql`, `src/main/sql/AnihanSRMS.sql`, `src/test/resources/document-storage-h2-schema.sql`
- Modify: `src/main/java/com/example/springboot/model/StudentRecord.java`
- Test: `src/test/java/com/example/springboot/SchemaContractTest.java`

- [ ] **Step 1: Write the failing contract test**

Add this test to `SchemaContractTest` (after `documentFileTypeSupportsOpenXmlMimeTypesEverywhere`):

```java
    @Test
    void soChecklistColumnsExistInEverySchemaCopy() throws Exception {
        for (String file : List.of("src/main/sql/schema.sql", "src/main/sql/AnihanSRMS.sql",
                "src/test/resources/document-storage-h2-schema.sql")) {
            String sql = Files.readString(Path.of(file));
            assertTrue(sql.matches("(?s).*enrollment_date DATE NULL,\\s+completion_date DATE NULL,.*"),
                    file + ": completion_date must follow enrollment_date");
            assertTrue(sql.matches("(?s).*student_status VARCHAR\\(25\\) NOT NULL DEFAULT 'Enrolling',"
                            + "\\s+employment_status VARCHAR\\(25\\) NULL,.*"),
                    file + ": employment_status must follow student_status");
        }
        assertTrue(Files.exists(Path.of("src/main/sql/migrations/2026-10-03-so-checklist.sql")));
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.SchemaContractTest"`
Expected: FAIL with `src/main/sql/schema.sql: completion_date must follow enrollment_date`.

- [ ] **Step 3: Create the migration**

Create `src/main/sql/migrations/2026-10-03-so-checklist.sql`:

```sql
-- ============================================================
-- Migration: 2026-10-03 — R4.2 SO Checklist columns
-- ============================================================
-- completion_date: the day the student finished training and OJT. Set when
-- the Registrar changes the status to Completed (or, for a digitized archive
-- record, Active -> Graduated). NULL until then.
-- employment_status: Employed / Self-employed / Unemployed / Further studies,
-- or NULL ("Not set"). One of TESDA's SO requirements.
-- student_status VARCHAR(25) already fits the new 'Completed' value, so it is
-- not touched. Existing rows are left NULL (no backfill).
--
-- See docs/superpowers/specs/2026-10-01-so-checklist-design.md §3.
--
-- Idempotent: safe to re-run. Each guard matches on COLUMN_NAME.
-- ============================================================

USE AnihanSRMS;

SET @has_completion_date = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'student_records'
      AND COLUMN_NAME = 'completion_date'
);
SET @sql = IF(@has_completion_date = 0,
    'ALTER TABLE student_records ADD COLUMN completion_date DATE NULL AFTER enrollment_date',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_employment_status = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'student_records'
      AND COLUMN_NAME = 'employment_status'
);
SET @sql = IF(@has_employment_status = 0,
    'ALTER TABLE student_records ADD COLUMN employment_status VARCHAR(25) NULL AFTER student_status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION (read-only — safe to run any time)
-- ============================================================

-- Expect: enrollment_date, completion_date, student_status, employment_status
-- in that order; both new columns IS_NULLABLE = YES.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, ORDINAL_POSITION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'student_records'
  AND COLUMN_NAME IN ('enrollment_date', 'completion_date', 'student_status', 'employment_status')
ORDER BY ORDINAL_POSITION;

-- Expect: with_completion_date = 0 and with_employment_status = 0 on first run
SELECT COUNT(*) AS total_students,
       SUM(completion_date IS NOT NULL) AS with_completion_date,
       SUM(employment_status IS NOT NULL) AS with_employment_status
FROM student_records;
```

- [ ] **Step 4: Mirror the columns in the three schema copies**

In **each** of `src/main/sql/schema.sql`, `src/main/sql/AnihanSRMS.sql` and `src/test/resources/document-storage-h2-schema.sql`, inside `CREATE TABLE ... student_records`, replace:

```sql
    enrollment_date DATE NULL,
    student_status VARCHAR(25) NOT NULL DEFAULT 'Enrolling',
```

with:

```sql
    enrollment_date DATE NULL,
    completion_date DATE NULL,
    student_status VARCHAR(25) NOT NULL DEFAULT 'Enrolling',
    employment_status VARCHAR(25) NULL,
```

(Indentation is four spaces in all three files.)

- [ ] **Step 5: Add the entity fields**

In `StudentRecord.java`, replace:

```java
    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "student_status", nullable = false, length = 25)
    private String studentStatus = "Enrolling";
```

with:

```java
    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    /**
     * The day the student finished training and OJT. Set when the status becomes
     * Completed (see StudentStatusTransitions); null before that.
     */
    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "student_status", nullable = false, length = 25)
    private String studentStatus = "Enrolling";

    /** Employed / Self-employed / Unemployed / Further studies, or null ("Not set"). */
    @Column(name = "employment_status", length = 25)
    private String employmentStatus;
```

and append these accessors after `setStudentStatus`:

```java

    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
```

- [ ] **Step 6: Run the full suite (the entity change touches every H2 test)**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, 579 tests (578 + 1).

- [ ] **Step 7: Commit**

```bash
git add src/main/sql/migrations/2026-10-03-so-checklist.sql src/main/sql/schema.sql src/main/sql/AnihanSRMS.sql \
  src/test/resources/document-storage-h2-schema.sql src/main/java/com/example/springboot/model/StudentRecord.java \
  src/test/java/com/example/springboot/SchemaContractTest.java
git commit -m "feat: add student completion_date and employment_status columns"
```

---

### Task 2: `StudentStatusTransitions`

**Files:**
- Create: `src/main/java/com/example/springboot/service/StudentStatusTransitions.java`
- Test: `src/test/java/com/example/springboot/service/StudentStatusTransitionsTest.java`

- [ ] **Step 1: Write the failing test**

Create `StudentStatusTransitionsTest.java`:

```java
package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.springboot.service.StudentStatusTransitions.Rule;

/** Pins the status-move table in spec 2026-10-01 SO checklist §4.2. */
class StudentStatusTransitionsTest {

    private static Rule check(String from, String to) {
        return StudentStatusTransitions.check(from, to);
    }

    @Test
    void activeToCompletedNeedsACompletionDateOnly() {
        Rule rule = check("Active", "Completed");
        assertTrue(rule.allowed());
        assertTrue(rule.requiresCompletionDate());
        assertFalse(rule.requiresReason());
        assertFalse(rule.clearsCompletionDate());
        assertNull(rule.rejection());
    }

    @Test
    void onlyAnActiveStudentMayBecomeCompletedFromBelow() {
        for (String from : List.of("Enrolling", "Submitted")) {
            Rule rule = check(from, "Completed");
            assertFalse(rule.allowed(), from + " -> Completed must be rejected");
            assertEquals("Only an Active student can be marked Completed.", rule.rejection());
        }
    }

    @Test
    void completedBackToActiveClearsTheCompletionDate() {
        Rule rule = check("Completed", "Active");
        assertTrue(rule.allowed());
        assertTrue(rule.clearsCompletionDate());
        assertFalse(rule.requiresCompletionDate());
        assertFalse(rule.requiresReason());
    }

    @Test
    void completedToGraduatedNeedsNothingExtra() {
        Rule rule = check("Completed", "Graduated");
        assertTrue(rule.allowed());
        assertFalse(rule.requiresCompletionDate());
        assertFalse(rule.requiresReason());
        assertFalse(rule.clearsCompletionDate());
    }

    @Test
    void activeToGraduatedIsTheArchiveEscapeHatchAndNeedsADateAndAReason() {
        Rule rule = check("Active", "Graduated");
        assertTrue(rule.allowed());
        assertTrue(rule.requiresCompletionDate());
        assertTrue(rule.requiresReason());
        assertFalse(rule.clearsCompletionDate());
    }

    @Test
    void graduatedBackToCompletedNeedsAReasonAndKeepsTheDate() {
        Rule rule = check("Graduated", "Completed");
        assertTrue(rule.allowed());
        assertTrue(rule.requiresReason());
        assertFalse(rule.requiresCompletionDate());
        assertFalse(rule.clearsCompletionDate());
    }

    @Test
    void graduatedCannotMoveAnywhereButCompleted() {
        for (String to : List.of("Active", "Enrolling")) {
            Rule rule = check("Graduated", to);
            assertFalse(rule.allowed(), "Graduated -> " + to + " must be rejected");
            assertEquals("A Graduated student can only be moved back to Completed.", rule.rejection());
        }
    }

    @Test
    void onlyACompletedOrActiveStudentMayBecomeGraduated() {
        for (String from : List.of("Enrolling", "Submitted")) {
            Rule rule = check(from, "Graduated");
            assertFalse(rule.allowed(), from + " -> Graduated must be rejected");
            assertEquals("Only a Completed student can be marked Graduated.", rule.rejection());
        }
    }

    @Test
    void completedCannotGoBackToEnrolling() {
        Rule rule = check("Completed", "Enrolling");
        assertFalse(rule.allowed());
        assertEquals("A Completed student can only move back to Active or on to Graduated.", rule.rejection());
    }

    @Test
    void movesAmongEnrollingSubmittedAndActiveAreUnchanged() {
        List<String[]> pairs = List.of(
                new String[] { "Enrolling", "Active" }, new String[] { "Active", "Enrolling" },
                new String[] { "Submitted", "Active" }, new String[] { "Submitted", "Enrolling" });
        for (String[] pair : pairs) {
            Rule rule = check(pair[0], pair[1]);
            assertTrue(rule.allowed(), pair[0] + " -> " + pair[1]);
            assertFalse(rule.requiresCompletionDate());
            assertFalse(rule.requiresReason());
            assertFalse(rule.clearsCompletionDate());
        }
    }

    @Test
    void keepingTheSameStatusIsAllowedAndNeedsNothing() {
        for (String status : List.of("Completed", "Graduated", "Active")) {
            Rule rule = check(status, status);
            assertTrue(rule.allowed(), status);
            assertFalse(rule.requiresCompletionDate());
            assertFalse(rule.requiresReason());
            assertFalse(rule.clearsCompletionDate());
        }
    }

    @Test
    void statusesAreMatchedIgnoringCaseAndSurroundingSpaces() {
        assertTrue(check(" active ", "COMPLETED").requiresCompletionDate());
        assertFalse(check("graduated", " Active").allowed());
    }

    @Test
    void completionDateIsEditableOnlyForCompletedOrGraduated() {
        assertTrue(StudentStatusTransitions.isCompletedOrGraduated("Completed"));
        assertTrue(StudentStatusTransitions.isCompletedOrGraduated(" graduated "));
        for (String status : new String[] { "Active", "Enrolling", "Submitted", null }) {
            assertFalse(StudentStatusTransitions.isCompletedOrGraduated(status), String.valueOf(status));
        }
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.service.StudentStatusTransitionsTest"`
Expected: FAIL to compile with `cannot find symbol ... StudentStatusTransitions`.

- [ ] **Step 3: Implement**

Create `StudentStatusTransitions.java`:

```java
package com.example.springboot.service;

import java.util.Locale;

/**
 * Which status changes the Registrar may make through
 * {@code PUT /api/registrar/student-records/{id}/status}, and what extra input each
 * needs (spec 2026-10-01 SO checklist §4.2). Only moves into or out of Completed or
 * Graduated are restricted; moves among Enrolling / Submitted / Active behave as
 * before. Pure and stateless. To change a rule, edit this class and its mirror
 * {@code statusRule} in {@code js/registrar-students.js} (the server stays the authority).
 */
public final class StudentStatusTransitions {

    private static final String ACTIVE = "active";
    private static final String COMPLETED = "completed";
    private static final String GRADUATED = "graduated";

    /** The outcome for one from → to pair. {@code rejection} is null when the move is allowed. */
    public record Rule(boolean allowed, boolean requiresCompletionDate, boolean requiresReason,
                       boolean clearsCompletionDate, String rejection) {

        static Rule allow() {
            return new Rule(true, false, false, false, null);
        }

        static Rule reject(String rejection) {
            return new Rule(false, false, false, false, rejection);
        }
    }

    private StudentStatusTransitions() {
    }

    public static Rule check(String fromStatus, String toStatus) {
        String from = normalize(fromStatus);
        String to = normalize(toStatus);
        if (from.equals(to)) {
            return Rule.allow();
        }
        if (to.equals(COMPLETED)) {
            if (from.equals(ACTIVE)) {
                return new Rule(true, true, false, false, null);
            }
            if (from.equals(GRADUATED)) {
                return new Rule(true, false, true, false, null);
            }
            return Rule.reject("Only an Active student can be marked Completed.");
        }
        if (to.equals(GRADUATED)) {
            if (from.equals(COMPLETED)) {
                return Rule.allow();
            }
            if (from.equals(ACTIVE)) {
                // Archive escape hatch: digitized records of students who graduated long ago.
                return new Rule(true, true, true, false, null);
            }
            return Rule.reject("Only a Completed student can be marked Graduated.");
        }
        if (from.equals(GRADUATED)) {
            return Rule.reject("A Graduated student can only be moved back to Completed.");
        }
        if (from.equals(COMPLETED)) {
            return to.equals(ACTIVE)
                    ? new Rule(true, false, false, true, null)
                    : Rule.reject("A Completed student can only move back to Active or on to Graduated.");
        }
        return Rule.allow();
    }

    /** Statuses for which completion_date may be set or corrected: Completed and Graduated. */
    public static boolean isCompletedOrGraduated(String status) {
        String normalized = normalize(status);
        return normalized.equals(COMPLETED) || normalized.equals(GRADUATED);
    }

    private static String normalize(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }
}
```

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew test --tests "com.example.springboot.service.StudentStatusTransitionsTest"`
Expected: PASS, 13 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/springboot/service/StudentStatusTransitions.java \
  src/test/java/com/example/springboot/service/StudentStatusTransitionsTest.java
git commit -m "feat: add student status transition rules for Completed/Graduated"
```

---

### Task 3: Status endpoint enforces transitions, records date/reason, and logs them

**Files:**
- Modify: `src/main/java/com/example/springboot/dto/registrar/UpdateStudentStatusRequest.java`
- Modify: `src/main/java/com/example/springboot/dto/registrar/StudentRecordDetailsResponse.java`
- Modify: `src/main/java/com/example/springboot/service/RegistrarService.java` (`updateStatus`, lines 269-285)
- Modify: `src/main/java/com/example/springboot/controller/RegistrarController.java` (`updateStatus`, lines 151-174)
- Modify tests: `RegistrarStatusServiceTest`, `RegistrarStatusControllerWebMvcTest`, `RegistrarStudentNumberControllerWebMvcTest`, `RegistrarBatchControllerWebMvcTest`

- [ ] **Step 1: Add `completionDate` and `employmentStatus` to the details response**

They are needed now for the log text and response assertions. Task 4 wires `employmentStatus` into the edit form.

In `StudentRecordDetailsResponse.java`, replace the last record components:

```java
        GuardianDto guardian,
        BigDecimal totalGwa
) {
```

with:

```java
        GuardianDto guardian,
        BigDecimal totalGwa,
        LocalDate completionDate,
        String employmentStatus
) {
```

and in the 8-argument `from(...)`, replace:

```java
                guardian,
                totalGwa
        );
```

with:

```java
                guardian,
                totalGwa,
                r.getCompletionDate(),
                r.getEmploymentStatus()
        );
```

- [ ] **Step 2: Fix the three test helpers that build the response positionally**

In `RegistrarStatusControllerWebMvcTest`, `RegistrarStudentNumberControllerWebMvcTest` and `RegistrarBatchControllerWebMvcTest`, the `details(...)` helper ends with:

```java
                null, List.of(), List.of(), null, null, null, null);
```

Change that line in all three files to:

```java
                null, List.of(), List.of(), null, null, null, null,
                null, null);
```

(In `RegistrarStatusControllerWebMvcTest` the helper is rewritten again in Step 4. Doing it here keeps the build compiling in between.)

- [ ] **Step 3: Write the failing service tests**

In `RegistrarStatusServiceTest.java`:

(a) Add imports:

```java
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
```

(b) In the existing tests, change `registrarService.updateStatus(1, "Active")` to `registrarService.updateStatus(1, "Active", null, null)` and `registrarService.updateStatus(999, "Active")` to `registrarService.updateStatus(999, "Active", null, null)`.

(c) Add these tests before the closing brace:

```java
    // ----- Completed / Graduated transitions (spec 2026-10-01 SO checklist §4.2) -----

    private void stubSuccessfulSave(StudentRecord target) {
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildLookups();
    }

    @Test
    void activeToCompletedSetsTheCompletionDate() {
        StudentRecord target = record(1, "SR20260001", "Active");
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result =
                registrarService.updateStatus(1, "Completed", LocalDate.of(2026, 9, 30), null);

        assertEquals("Completed", result.studentStatus());
        assertEquals(LocalDate.of(2026, 9, 30), result.completionDate());
        assertEquals(LocalDate.of(2026, 9, 30), target.getCompletionDate());
    }

    @Test
    void activeToCompletedWithoutADateIsRejected() {
        StudentRecord target = record(1, "SR20260001", "Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Completed", null, null));

        assertEquals("A completion date is required for this status change.", ex.getMessage());
        assertEquals("Active", target.getStudentStatus());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aCompletionDateInTheFutureIsRejected() {
        StudentRecord target = record(1, "SR20260001", "Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Completed", LocalDate.now().plusDays(1), null));

        assertEquals("Completion date cannot be in the future.", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aCompletionDateBeforeTheEnrollmentDateIsRejected() {
        StudentRecord target = record(1, "SR20260001", "Active");
        target.setEnrollmentDate(LocalDate.of(2025, 6, 2));
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Completed", LocalDate.of(2025, 6, 1), null));

        assertEquals("Completion date cannot be before the enrollment date (2025-06-02).", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void completedBackToActiveClearsTheCompletionDate() {
        StudentRecord target = record(1, "SR20260001", "Completed");
        target.setCompletionDate(LocalDate.of(2026, 9, 30));
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result = registrarService.updateStatus(1, "Active", null, null);

        assertEquals("Active", result.studentStatus());
        assertNull(result.completionDate());
        assertNull(target.getCompletionDate());
    }

    @Test
    void activeToGraduatedWithoutAReasonIsRejected() {
        StudentRecord target = record(1, "SR20260001", "Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Graduated", LocalDate.of(2014, 3, 15), "   "));

        assertEquals("A reason is required for this status change.", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void activeToGraduatedWithADateAndAReasonSetsTheDate() {
        StudentRecord target = record(1, "SR20260001", "Active");
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result = registrarService.updateStatus(
                1, "Graduated", LocalDate.of(2014, 3, 15), "Digitized archive record");

        assertEquals("Graduated", result.studentStatus());
        assertEquals(LocalDate.of(2014, 3, 15), result.completionDate());
    }

    @Test
    void graduatedBackToCompletedKeepsTheCompletionDate() {
        StudentRecord target = record(1, "SR20260001", "Graduated");
        target.setCompletionDate(LocalDate.of(2014, 3, 15));
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result =
                registrarService.updateStatus(1, "Completed", null, "Wrong batch on the SO");

        assertEquals("Completed", result.studentStatus());
        assertEquals(LocalDate.of(2014, 3, 15), result.completionDate());
    }

    @Test
    void aDisallowedMoveIsRejectedWithTheRuleMessage() {
        StudentRecord target = record(1, "SR20260001", "Enrolling");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateStatus(1, "Completed", LocalDate.of(2026, 9, 30), null));

        assertEquals("Only an Active student can be marked Completed.", ex.getMessage());
        assertEquals("Enrolling", target.getStudentStatus());
        verify(studentRecordRepository, never()).save(any());
    }
```

- [ ] **Step 4: Write the failing controller tests**

In `RegistrarStatusControllerWebMvcTest.java`:

(a) Add imports:

```java
import static org.mockito.ArgumentMatchers.isNull;

import java.time.LocalDate;
```

(b) Replace the `details(String status)` helper with:

```java
    private StudentRecordDetailsResponse details(String status) {
        return details(status, null);
    }

    private StudentRecordDetailsResponse details(String status, LocalDate completionDate) {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", null, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null, status,
                null, List.of(), List.of(), null, null, null, null,
                completionDate, null);
    }
```

(c) In the existing tests, change `when(registrarService.updateStatus(eq(1), eq("Active")))` to `when(registrarService.updateStatus(eq(1), eq("Active"), isNull(), isNull()))`. Change every `verify(registrarService, never()).updateStatus(any(), any())` to `verify(registrarService, never()).updateStatus(any(), any(), any(), any())`.

(d) Add these tests:

```java
    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void completingAStudentForwardsTheDateAndLogsIt() throws Exception {
        LocalDate done = LocalDate.of(2026, 9, 30);
        when(registrarService.getRecordById(1)).thenReturn(details("Active"));
        when(registrarService.updateStatus(eq(1), eq("Completed"), eq(done), isNull()))
                .thenReturn(details("Completed", done));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Completed\",\"completionDate\":\"2026-09-30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionDate").value("2026-09-30"));

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Changed status of Lipata, Maria from Active to Completed (completion date 2026-09-30)"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void archiveGraduationLogsTheDateAndTheTrimmedReason() throws Exception {
        LocalDate done = LocalDate.of(2014, 3, 15);
        when(registrarService.getRecordById(1)).thenReturn(details("Active"));
        when(registrarService.updateStatus(eq(1), eq("Graduated"), eq(done), eq("  Digitized archive record  ")))
                .thenReturn(details("Graduated", done));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Graduated\",\"completionDate\":\"2014-03-15\","
                                + "\"reason\":\"  Digitized archive record  \"}"))
                .andExpect(status().isOk());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Changed status of Lipata, Maria from Active to Graduated "
                        + "(completion date 2014-03-15; reason: Digitized archive record)"), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void movingBackToActiveLogsTheClearedDate() throws Exception {
        when(registrarService.getRecordById(1)).thenReturn(details("Completed", LocalDate.of(2026, 9, 30)));
        when(registrarService.updateStatus(eq(1), eq("Active"), isNull(), isNull())).thenReturn(details("Active"));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Active\"}"))
                .andExpect(status().isOk());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Changed status of Lipata, Maria from Completed to Active (cleared completion date 2026-09-30)"),
                any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void graduatedBackToCompletedLogsTheReasonOnly() throws Exception {
        LocalDate done = LocalDate.of(2014, 3, 15);
        when(registrarService.getRecordById(1)).thenReturn(details("Graduated", done));
        when(registrarService.updateStatus(eq(1), eq("Completed"), isNull(), eq("Wrong batch on the SO")))
                .thenReturn(details("Completed", done));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Completed\",\"reason\":\"Wrong batch on the SO\"}"))
                .andExpect(status().isOk());

        verify(systemLogService).logAction(any(), eq("registrar"), eq("ROLE_REGISTRAR"),
                eq("Changed status of Lipata, Maria from Graduated to Completed (reason: Wrong batch on the SO)"),
                any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForAReasonLongerThan255Characters() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Graduated\",\"completionDate\":\"2014-03-15\",\"reason\":\""
                                + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());

        verify(registrarService, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400WithTheRuleMessageWhenTheMoveIsNotAllowed() throws Exception {
        when(registrarService.getRecordById(1)).thenReturn(details("Enrolling"));
        when(registrarService.updateStatus(eq(1), eq("Completed"), any(), any()))
                .thenThrow(new IllegalArgumentException("Only an Active student can be marked Completed."));

        mvc.perform(put("/api/registrar/student-records/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentStatus\":\"Completed\",\"completionDate\":\"2026-09-30\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only an Active student can be marked Completed."));

        verify(systemLogService, never()).logAction(any(), any(), any(), any(), any());
    }
```

- [ ] **Step 5: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.service.RegistrarStatusServiceTest" --tests "com.example.springboot.controller.RegistrarStatusControllerWebMvcTest"`
Expected: FAIL to compile, because `updateStatus` has no 4-argument form.

- [ ] **Step 6: Extend the request DTO**

Replace the record in `UpdateStudentStatusRequest.java` (keep the class Javadoc). Add `import java.time.LocalDate;` and `import jakarta.validation.constraints.Size;`:

```java
public record UpdateStudentStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = ALLOWED_VALUES_PATTERN, message = "Status must be one of: " + ALLOWED_VALUES_DISPLAY)
        String studentStatus,

        // Required for Active -> Completed and Active -> Graduated. Which moves need it is
        // decided by StudentStatusTransitions and checked in RegistrarService.updateStatus.
        LocalDate completionDate,

        // Required for Active -> Graduated and Graduated -> Completed (same checking as above).
        // Logged with the status change, so it is capped to keep system_logs.action under 500.
        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
    public static final String ALLOWED_VALUES_PATTERN = "^(Enrolling|Active|Completed|Graduated)$";
    public static final String ALLOWED_VALUES_DISPLAY = "Enrolling, Active, Completed, Graduated";
}
```

- [ ] **Step 7: Enforce the rules in `RegistrarService.updateStatus`**

Replace the whole `updateStatus` method (Javadoc included) with:

```java
    /**
     * Changes a student's enrollment status. The only write path for {@code student_status} —
     * deliberately pulled out of the general edit form so a routine field edit can never
     * silently change it, and every status change is a separately audited action.
     *
     * <p>The set of target values is enforced at the DTO level
     * ({@link com.example.springboot.dto.registrar.UpdateStudentStatusRequest}). Which moves are
     * allowed from the student's <em>current</em> status, and what extra input each needs, is
     * decided by {@link StudentStatusTransitions} (spec 2026-10-01 SO checklist §4.2). Only
     * changes made here are checked; existing rows are never re-validated.
     *
     * @param completionDate required for Active → Completed and Active → Graduated
     * @param reason         required for Active → Graduated and Graduated → Completed
     */
    @Transactional
    public StudentRecordDetailsResponse updateStatus(Integer recordId, String newStatus,
                                                     LocalDate completionDate, String reason) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

        StudentStatusTransitions.Rule rule = StudentStatusTransitions.check(record.getStudentStatus(), newStatus);
        if (!rule.allowed()) {
            throw new IllegalArgumentException(rule.rejection());
        }
        if (rule.requiresReason() && emptyToNull(reason) == null) {
            throw new IllegalArgumentException("A reason is required for this status change.");
        }
        if (rule.requiresCompletionDate()) {
            if (completionDate == null) {
                throw new IllegalArgumentException("A completion date is required for this status change.");
            }
            validateCompletionDate(completionDate, record.getEnrollmentDate());
            record.setCompletionDate(completionDate);
        }
        if (rule.clearsCompletionDate()) {
            record.setCompletionDate(null);
        }

        record.setStudentStatus(newStatus);
        return buildDetailsResponse(studentRecordRepository.save(record));
    }

    /** Spec §4.2: not in the future, and not before the enrollment date when that is set. */
    private void validateCompletionDate(LocalDate completionDate, LocalDate enrollmentDate) {
        if (completionDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Completion date cannot be in the future.");
        }
        if (enrollmentDate != null && completionDate.isBefore(enrollmentDate)) {
            throw new IllegalArgumentException(
                    "Completion date cannot be before the enrollment date (" + enrollmentDate + ").");
        }
    }
```

(`LocalDate` is already imported in `RegistrarService`.)

- [ ] **Step 8: Pass the new inputs and write the extended audit text in the controller**

In `RegistrarController.java`, add imports:

```java
import java.util.ArrayList;

import com.example.springboot.service.StudentStatusTransitions;
```

Replace the whole `updateStatus` endpoint method (Javadoc included) with:

```java
    /**
     * Changes a student's enrollment status. Pulled out of the general update endpoint so a
     * status change is always a deliberate, separately audited action — mirrors the treatment
     * given to the student number. The audit line also records a set or cleared completion
     * date and, when the move required one, the reason (spec 2026-10-01 SO checklist §10).
     */
    @PutMapping("/{recordId}/status")
    public ResponseEntity<StudentRecordDetailsResponse> updateStatus(
            @PathVariable Integer recordId,
            @Valid @RequestBody UpdateStudentStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        StudentRecordDetailsResponse before = registrarService.getRecordById(recordId);

        StudentRecordDetailsResponse updated = registrarService.updateStatus(
                recordId, request.studentStatus(), request.completionDate(), request.reason());

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                statusChangeAction(before, updated, request.reason()),
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(updated);
    }

    /** system_logs.action is VARCHAR(500). */
    private static final int MAX_ACTION_LENGTH = 500;

    /**
     * e.g. "Changed status of Dela Cruz, Ana from Active to Graduated (completion date
     * 2014-03-15; reason: Digitized archive record)". The reason is logged only when the move
     * required one (the service has already rejected a blank one).
     */
    private String statusChangeAction(StudentRecordDetailsResponse before,
                                      StudentRecordDetailsResponse after, String reason) {
        List<String> notes = new ArrayList<>();
        if (after.completionDate() != null && !after.completionDate().equals(before.completionDate())) {
            notes.add("completion date " + after.completionDate());
        } else if (after.completionDate() == null && before.completionDate() != null) {
            notes.add("cleared completion date " + before.completionDate());
        }
        if (StudentStatusTransitions.check(before.studentStatus(), after.studentStatus()).requiresReason()) {
            notes.add("reason: " + (reason == null ? "" : reason.trim()));
        }
        String action = "Changed status of " + after.lastName() + ", " + after.firstName()
                + " from " + before.studentStatus() + " to " + after.studentStatus();
        if (!notes.isEmpty()) {
            action += " (" + String.join("; ", notes) + ")";
        }
        return action.length() <= MAX_ACTION_LENGTH ? action : action.substring(0, MAX_ACTION_LENGTH - 3) + "...";
    }
```

- [ ] **Step 9: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.service.RegistrarStatusServiceTest" --tests "com.example.springboot.controller.RegistrarStatusControllerWebMvcTest" --tests "com.example.springboot.controller.RegistrarStudentNumberControllerWebMvcTest" --tests "com.example.springboot.controller.RegistrarBatchControllerWebMvcTest"`
Expected: PASS. `RegistrarStatusServiceTest` has 11 tests (2 old + 9 new) and `RegistrarStatusControllerWebMvcTest` has 12 (6 old + 6 new).

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/example/springboot/dto/registrar/UpdateStudentStatusRequest.java \
  src/main/java/com/example/springboot/dto/registrar/StudentRecordDetailsResponse.java \
  src/main/java/com/example/springboot/service/RegistrarService.java \
  src/main/java/com/example/springboot/controller/RegistrarController.java \
  src/test/java/com/example/springboot/service/RegistrarStatusServiceTest.java \
  src/test/java/com/example/springboot/controller/RegistrarStatusControllerWebMvcTest.java \
  src/test/java/com/example/springboot/controller/RegistrarStudentNumberControllerWebMvcTest.java \
  src/test/java/com/example/springboot/controller/RegistrarBatchControllerWebMvcTest.java
git commit -m "feat: enforce Completed/Graduated status transitions with completion date and reason"
```

---

### Task 4: Edit form accepts enrollment date, completion date, and employment status

**Files:**
- Modify: `src/main/java/com/example/springboot/dto/registrar/StudentRecordUpdateRequest.java`
- Modify: `src/main/java/com/example/springboot/service/RegistrarService.java` (`updateRecord`)
- Modify tests: `RegistrarStudentNumberServiceTest` (line ~215), `RegistrarBatchServiceTest` (line ~236)
- Create tests: `src/test/java/com/example/springboot/service/RegistrarRecordFieldsServiceTest.java`, `src/test/java/com/example/springboot/controller/RegistrarRecordUpdateControllerWebMvcTest.java`

- [ ] **Step 1: Extend the request record**

In `StudentRecordUpdateRequest.java`, add `import jakarta.validation.constraints.Pattern;`, then replace:

```java
        ParentDto mother,
        GuardianDto guardian
) {
}
```

with:

```java
        ParentDto mother,
        GuardianDto guardian,

        // Editable for every status (spec 2026-10-01 SO checklist §4.3). The edit form always
        // sends the loaded values back, so a routine save never wipes these three.
        @PastOrPresent(message = "Enrollment date cannot be in the future")
        LocalDate enrollmentDate,

        // Accepted only while the student is Completed or Graduated — see
        // RegistrarService.updateRecord. Normally set by the status endpoint.
        @PastOrPresent(message = "Completion date cannot be in the future")
        LocalDate completionDate,

        @Pattern(regexp = EMPLOYMENT_STATUS_PATTERN,
                message = "Employment status must be one of: " + EMPLOYMENT_STATUS_DISPLAY)
        String employmentStatus
) {
    /**
     * The four Employment Status values (spec §5); null means "Not set". The
     * #editEmploymentStatus select in student-records.html lists the same values,
     * pinned by FrontendContractTest.
     */
    public static final String EMPLOYMENT_STATUS_PATTERN = "^(Employed|Self-employed|Unemployed|Further studies)$";
    public static final String EMPLOYMENT_STATUS_DISPLAY = "Employed, Self-employed, Unemployed, Further studies";
}
```

- [ ] **Step 2: Fix the two existing positional constructor calls**

In `RegistrarStudentNumberServiceTest` and `RegistrarBatchServiceTest`, the `new StudentRecordUpdateRequest(...)` call ends with:

```java
                null, List.of(), List.of(), null, null, null);
```

Change it in both files to:

```java
                null, List.of(), List.of(), null, null, null,
                null, null, null);
```

- [ ] **Step 3: Write the failing service tests**

Create `RegistrarRecordFieldsServiceTest.java`:

```java
package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.OtherGuardianRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentOjtRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentSchoolYearRepository;
import com.example.springboot.repository.StudentTesdaQualificationRepository;

/**
 * Tests the SO-checklist fields on {@code RegistrarService.updateRecord}: enrollment date
 * (editable for every status), completion date (only for Completed/Graduated) and
 * employment status (spec 2026-10-01 SO checklist §4.3 and §5).
 */
@ExtendWith(MockitoExtension.class)
class RegistrarRecordFieldsServiceTest {

    private static final LocalDate ENROLLED = LocalDate.of(2025, 6, 2);
    private static final LocalDate COMPLETED = LocalDate.of(2026, 3, 20);

    @Mock private StudentRecordRepository studentRecordRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private StudentOjtRepository studentOjtRepository;
    @Mock private StudentTesdaQualificationRepository tesdaQualRepository;
    @Mock private StudentSchoolYearRepository schoolYearRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private OtherGuardianRepository guardianRepository;
    @Mock private StudentEducationRepository educationRepository;
    @Mock private GradeRepository gradeRepository;

    @InjectMocks
    private RegistrarService registrarService;

    private StudentRecord record(String status) {
        StudentRecord r = new StudentRecord();
        r.setRecordId(1);
        r.setStudentId("SR20260001");
        r.setLastName("Lipata");
        r.setFirstName("Maria");
        r.setStudentStatus(status);
        return r;
    }

    private static StudentRecordUpdateRequest request(LocalDate enrollmentDate, LocalDate completionDate,
                                                      String employmentStatus) {
        return new StudentRecordUpdateRequest(
                "SR20260001", "Lipata-Edited", "Maria", null, null,
                null, null, null, null, null, null, null, false, null, null,
                null, null, null, null, null, null,
                null, List.of(), List.of(), null, null, null,
                enrollmentDate, completionDate, employmentStatus);
    }

    private void stubSuccessfulSave(StudentRecord target) {
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));
        when(studentRecordRepository.save(any(StudentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentOjtRepository.findByStudentId(any())).thenReturn(Optional.empty());
        when(tesdaQualRepository.findByStudentIdOrderBySlot(any())).thenReturn(List.of());
        when(schoolYearRepository.findByStudentIdOrderByRowIndex(any())).thenReturn(List.of());
        when(parentRepository.findByStudentStudentIdAndRelation(any(), any())).thenReturn(Optional.empty());
        when(guardianRepository.findByStudentStudentId(any())).thenReturn(List.of());
        when(gradeRepository.findByStudentStudentId(any())).thenReturn(List.of());
    }

    @Test
    void aRoutineSaveKeepsTheDatesAndEmploymentStatus() {
        StudentRecord target = record("Completed");
        target.setEnrollmentDate(ENROLLED);
        target.setCompletionDate(COMPLETED);
        target.setEmploymentStatus("Employed");
        stubSuccessfulSave(target);

        StudentRecordDetailsResponse result =
                registrarService.updateRecord(1, request(ENROLLED, COMPLETED, "Employed"));

        assertEquals("Lipata-Edited", result.lastName(), "The edit itself must apply");
        assertEquals(ENROLLED, result.enrollmentDate());
        assertEquals(COMPLETED, result.completionDate());
        assertEquals("Employed", result.employmentStatus());
    }

    @Test
    void enrollmentDateIsEditableForAnActiveStudent() {
        StudentRecord target = record("Active");
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(ENROLLED, null, null));

        assertEquals(ENROLLED, target.getEnrollmentDate());
    }

    @Test
    void aCompletionDateCannotBeSetForAnActiveStudent() {
        StudentRecord target = record("Active");
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateRecord(1, request(null, COMPLETED, null)));

        assertEquals("Completion date can only be set for a Completed or Graduated student. "
                + "Change the status first.", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aCompletionDateCanBeCorrectedForAGraduatedStudent() {
        StudentRecord target = record("Graduated");
        target.setCompletionDate(COMPLETED);
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(ENROLLED, LocalDate.of(2026, 3, 18), null));

        assertEquals(LocalDate.of(2026, 3, 18), target.getCompletionDate());
    }

    @Test
    void anEnrollmentDateAfterTheCompletionDateIsRejected() {
        StudentRecord target = record("Completed");
        target.setCompletionDate(COMPLETED);
        when(studentRecordRepository.findById(1)).thenReturn(Optional.of(target));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registrarService.updateRecord(1, request(LocalDate.of(2026, 4, 1), COMPLETED, null)));

        assertEquals("Enrollment date cannot be after the completion date.", ex.getMessage());
        verify(studentRecordRepository, never()).save(any());
    }

    @Test
    void aNullEmploymentStatusMeansNotSet() {
        StudentRecord target = record("Completed");
        target.setCompletionDate(COMPLETED);
        target.setEmploymentStatus("Employed");
        stubSuccessfulSave(target);

        registrarService.updateRecord(1, request(null, COMPLETED, null));

        assertNull(target.getEmploymentStatus());
    }
}
```

- [ ] **Step 4: Write the failing controller tests**

Create `RegistrarRecordUpdateControllerWebMvcTest.java`:

```java
package com.example.springboot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.SystemLogService;

/** Validation of the SO-checklist fields on {@code PUT /api/registrar/student-records/{id}}. */
@WebMvcTest(RegistrarController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class RegistrarRecordUpdateControllerWebMvcTest {

    private static final String BODY_START =
            "{\"studentId\":\"SR20260001\",\"lastName\":\"Lipata\",\"firstName\":\"Maria\"";

    @Autowired private MockMvc mvc;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private StudentRecordDetailsResponse details() {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", null, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, LocalDate.of(2025, 6, 2), "Active",
                null, List.of(), List.of(), null, null, null, null,
                null, "Self-employed");
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void forwardsTheEnrollmentDateAndEmploymentStatus() throws Exception {
        when(registrarService.updateRecord(eq(1), any())).thenReturn(details());

        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_START + ",\"enrollmentDate\":\"2025-06-02\","
                                + "\"employmentStatus\":\"Self-employed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("Self-employed"));

        ArgumentCaptor<StudentRecordUpdateRequest> sent = ArgumentCaptor.forClass(StudentRecordUpdateRequest.class);
        verify(registrarService).updateRecord(eq(1), sent.capture());
        assertEquals(LocalDate.of(2025, 6, 2), sent.getValue().enrollmentDate());
        assertEquals("Self-employed", sent.getValue().employmentStatus());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForAnUnknownEmploymentStatus() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_START + ",\"employmentStatus\":\"Abroad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.employmentStatus").exists());

        verify(registrarService, never()).updateRecord(any(), any());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void returns400ForAnEnrollmentDateInTheFuture() throws Exception {
        mvc.perform(put("/api/registrar/student-records/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_START + ",\"enrollmentDate\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.enrollmentDate").exists());

        verify(registrarService, never()).updateRecord(any(), any());
    }
}
```

- [ ] **Step 5: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.service.RegistrarRecordFieldsServiceTest" --tests "com.example.springboot.controller.RegistrarRecordUpdateControllerWebMvcTest"`
Expected: the service tests FAIL. For example, `aRoutineSaveKeepsTheDatesAndEmploymentStatus` fails with `expected: <2025-06-02> but was: <null>`, because `updateRecord` does not write the new fields yet. The 3 controller tests already pass, because validation lives on the DTO from Step 1.

- [ ] **Step 6: Write the fields in `updateRecord`**

In `RegistrarService.updateRecord`, after `record.setSisterCount(request.sisterCount());` add:

```java
        applyDates(record, request.enrollmentDate(), request.completionDate());
        record.setEmploymentStatus(emptyToNull(request.employmentStatus()));
```

Add this private method right after `updateRecord`:

```java
    /**
     * Enrollment date is editable for every status. Completion date may be set or corrected
     * only while the student is Completed or Graduated; for anyone else a non-null value that
     * differs from the stored one is rejected and a null leaves the stored value alone, so a
     * routine save never wipes it (spec 2026-10-01 SO checklist §4.3).
     */
    private void applyDates(StudentRecord record, LocalDate enrollmentDate, LocalDate completionDate) {
        boolean completionEditable = StudentStatusTransitions.isCompletedOrGraduated(record.getStudentStatus());
        if (!completionEditable && completionDate != null && !completionDate.equals(record.getCompletionDate())) {
            throw new IllegalArgumentException(
                    "Completion date can only be set for a Completed or Graduated student. Change the status first.");
        }
        LocalDate effectiveCompletion = completionEditable ? completionDate : record.getCompletionDate();
        if (enrollmentDate != null && effectiveCompletion != null && enrollmentDate.isAfter(effectiveCompletion)) {
            throw new IllegalArgumentException("Enrollment date cannot be after the completion date.");
        }
        record.setEnrollmentDate(enrollmentDate);
        record.setCompletionDate(effectiveCompletion);
    }
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.service.*" --tests "com.example.springboot.controller.*"`
Expected: PASS. That includes `RegistrarStudentNumberServiceTest` and `RegistrarBatchServiceTest`, whose updated calls send nulls for a record that is not Completed/Graduated.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/springboot/dto/registrar/StudentRecordUpdateRequest.java \
  src/main/java/com/example/springboot/service/RegistrarService.java \
  src/test/java/com/example/springboot/service/RegistrarRecordFieldsServiceTest.java \
  src/test/java/com/example/springboot/controller/RegistrarRecordUpdateControllerWebMvcTest.java \
  src/test/java/com/example/springboot/service/RegistrarStudentNumberServiceTest.java \
  src/test/java/com/example/springboot/service/RegistrarBatchServiceTest.java
git commit -m "feat: edit enrollment date, completion date and employment status on student records"
```

---

### Task 5: `RequiredDocumentPolicy`: completion documents for Completed too

**Files:**
- Modify: `src/main/java/com/example/springboot/service/RequiredDocumentPolicy.java`
- Test: `src/test/java/com/example/springboot/service/RequiredDocumentPolicyTest.java`

- [ ] **Step 1: Write the failing test**

Add to `RequiredDocumentPolicyTest`:

```java
    @Test
    void completedStudentRequiresTheCompletionDocumentsToo() {
        assertEquals(RequiredDocumentPolicy.missing("Graduated", Set.of()),
                RequiredDocumentPolicy.missing("Completed", Set.of()));
        assertEquals(7, RequiredDocumentPolicy.missing(" completed ", Set.of()).size());
    }
```

Also rename `nonGraduatedStatusesNeverRequireCompletionDocuments` to `studentsStillStudyingNeverRequireCompletionDocuments`. Its body (Enrolling/Submitted/Active) stays unchanged.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.service.RequiredDocumentPolicyTest"`
Expected: FAIL in `completedStudentRequiresTheCompletionDocumentsToo` (Completed returns only the 3 intake documents).

- [ ] **Step 3: Implement**

In `RequiredDocumentPolicy.java`, replace the class Javadoc with:

```java
/**
 * Which required documents a student is missing (spec 2026-10-01 §2).
 * Intake documents are required for every student; completion documents
 * once the student is Completed or Graduated (spec 2026-10-01 SO checklist
 * §4.5), so the warning stays meaningful for students who are still studying.
 * Pure and stateless — to change what is required, edit this class.
 */
```

Replace `/** Label reported when a Graduated student has none of the three Form IX types. */` with `/** Label reported when a Completed/Graduated student has none of the three Form IX types. */`.

Replace:

```java
    private static final String GRADUATED_STATUS = "graduated";
```

with:

```java
    private static final Set<String> COMPLETION_STATUSES = Set.of("completed", "graduated");
```

Replace `if (isGraduated(studentStatus)) {` with `if (requiresCompletionDocuments(studentStatus)) {`.

Replace the `isGraduated` method (and its Javadoc) with:

```java
    /** Completed or Graduated; anything else (incl. null/unknown) gets intake requirements only. */
    private static boolean requiresCompletionDocuments(String studentStatus) {
        return studentStatus != null
                && COMPLETION_STATUSES.contains(studentStatus.trim().toLowerCase(Locale.ROOT));
    }
```

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew test --tests "com.example.springboot.service.RequiredDocumentPolicyTest" --tests "com.example.springboot.service.DocumentExportServiceTest"`
Expected: PASS (`RequiredDocumentPolicyTest` 9 tests; `DocumentExportServiceTest` unchanged).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/springboot/service/RequiredDocumentPolicy.java \
  src/test/java/com/example/springboot/service/RequiredDocumentPolicyTest.java
git commit -m "feat: require completion documents for Completed students as well as Graduated"
```

---

### Task 6: `SoReadinessPolicy` and the checklist DTOs

**Files:**
- Create: `src/main/java/com/example/springboot/dto/registrar/SoChecklistItem.java`
- Create: `src/main/java/com/example/springboot/dto/registrar/SoChecklistResponse.java`
- Create: `src/main/java/com/example/springboot/service/SoReadinessPolicy.java`
- Test: `src/test/java/com/example/springboot/service/SoReadinessPolicyTest.java`

- [ ] **Step 1: Create the DTOs**

`SoChecklistItem.java`:

```java
package com.example.springboot.dto.registrar;

/**
 * One SO checklist row. {@code state} is MET, WARNING, UNMET or NOT_DUE;
 * {@code detail} is a short sentence the Registrar can act on.
 */
public record SoChecklistItem(String key, String label, String state, String detail) {
}
```

`SoChecklistResponse.java`:

```java
package com.example.springboot.dto.registrar;

import java.util.List;

/**
 * Per-student SO checklist (spec 2026-10-01 SO checklist §7). {@code stage} is NOT_APPLICABLE
 * (no items), PREVIEW (Active: completion items NOT_DUE, no verdict) or FINAL
 * (Completed/Graduated). {@code complete} is null unless FINAL, and covers student
 * requirements only — batch-level SO items are not checked.
 */
public record SoChecklistResponse(String stage, Boolean complete, int warningCount, List<SoChecklistItem> items) {
}
```

- [ ] **Step 2: Write the failing test**

Create `SoReadinessPolicyTest.java`:

```java
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
```

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.service.SoReadinessPolicyTest"`
Expected: FAIL to compile with `cannot find symbol ... SoReadinessPolicy`.

- [ ] **Step 4: Implement**

Create `SoReadinessPolicy.java`:

```java
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
```

- [ ] **Step 5: Run it to verify it passes**

Run: `./gradlew test --tests "com.example.springboot.service.SoReadinessPolicyTest"`
Expected: PASS, 21 tests.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/springboot/service/SoReadinessPolicy.java \
  src/main/java/com/example/springboot/dto/registrar/SoChecklistItem.java \
  src/main/java/com/example/springboot/dto/registrar/SoChecklistResponse.java \
  src/test/java/com/example/springboot/service/SoReadinessPolicyTest.java
git commit -m "feat: add SO readiness policy for the per-student TESDA checklist"
```

---

### Task 7: `SoChecklistService` and the checklist endpoint

**Files:**
- Create: `src/main/java/com/example/springboot/service/SoChecklistService.java`
- Create: `src/main/java/com/example/springboot/controller/SoChecklistController.java`
- Create: `src/test/resources/so-checklist-h2-schema.sql`
- Test: `src/test/java/com/example/springboot/integration/SoChecklistIntegrationTest.java`, `src/test/java/com/example/springboot/controller/SoChecklistControllerWebMvcTest.java`

- [ ] **Step 1: Create the H2 grade tables**

Create `src/test/resources/so-checklist-h2-schema.sql`:

```sql
-- Grade tables for SoChecklistIntegrationTest, loaded AFTER
-- document-storage-h2-schema.sql. Only the columns SoChecklistService reads.
-- Deliberately no foreign keys to student_records, so the first script's
-- DROP TABLE student_records can never trip over these tables.

DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS class_enrollments;
DROP TABLE IF EXISTS classes;

CREATE TABLE classes (
    class_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    semester VARCHAR(20) NOT NULL
);

CREATE TABLE class_enrollments (
    enrollment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id INT NOT NULL,
    student_id VARCHAR(20) NOT NULL,
    UNIQUE (class_id, student_id)
);

CREATE TABLE grades (
    grade_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    grade_status VARCHAR(5) NULL,
    remarks VARCHAR(20) NULL,
    class_id INT NULL,
    locked TINYINT NOT NULL DEFAULT 0,
    locked_at DATETIME NULL,
    UNIQUE (class_id, student_id)
);
```

- [ ] **Step 2: Write the failing real-H2 test**

Create `SoChecklistIntegrationTest.java`:

```java
package com.example.springboot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.SpringbootApplication;
import com.example.springboot.dto.registrar.SoChecklistItem;
import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.service.SoChecklistService;

/**
 * Runs SoChecklistService's real SQL on H2 (MySQL mode): section-first course/batch,
 * newest-TOR selection, enrollment LEFT JOIN grades, and no BLOB columns.
 * Never touches the live MySQL database.
 */
@SpringBootTest(classes = SpringbootApplication.class)
@Import(SoChecklistIntegrationTest.RecordingJdbcTemplateConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:soChecklistTestDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:document-storage-h2-schema.sql,classpath:so-checklist-h2-schema.sql"
})
class SoChecklistIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private SoChecklistService soChecklistService;

    @BeforeEach
    void clearRows() {
        for (String table : List.of("grades", "class_enrollments", "classes", "documents",
                "student_records", "sections", "courses", "batches")) {
            jdbc.update("DELETE FROM " + table);
        }
        ((RecordingJdbcTemplate) jdbc).clear();

        jdbc.update("INSERT INTO batches (batch_code, batch_year) VALUES ('B2026A', 2026)");
        jdbc.update("INSERT INTO courses (course_code, course_name) VALUES ('CARS', 'Culinary Arts and Restaurant Services')");
        jdbc.update("INSERT INTO sections (section_code, section, batch_code, course_code) VALUES ('S1', 'Section 1', 'B2026A', 'CARS')");
    }

    @Test
    void aCompleteStudentReadsCourseAndBatchFromTheSectionAndTheNewestTor() {
        // Own course_code and batch_code are NULL: Student Information is only met if the
        // query resolves them through the section (section-first, spec §6.2).
        insertStudent("SR1", "S1", "Completed");
        document("SR1", "PSA Birth Certificate", "2026-03-01 08:00:00");
        document("SR1", "Form IX - Cookery NC II", "2026-03-01 08:00:00");
        document("SR1", "Transcript of Records (TOR)", "2026-03-10 08:00:00"); // older than the locks
        document("SR1", "Transcript of Records (TOR)", "2026-03-25 08:00:00"); // newest: after the locks
        document("SR1", "Certificate of TVET Program", "2026-03-26 08:00:00");
        document("SR1", "OJT Report", "2026-03-26 08:00:00");
        grade(enroll("SR1", "BPP-101"), "SR1", "BPP-101", "COMPETENT", null, true, "2026-03-20 09:00:00");
        grade(enroll("SR1", "COOK-102"), "SR1", "COOK-102", "COMPETENT", null, true, "2026-03-21 09:00:00");
        Integer recordId = recordId("SR1");
        ((RecordingJdbcTemplate) jdbc).clear(); // record only the service's own reads

        SoChecklistResponse result = soChecklistService.checklist(recordId);

        assertEquals("FINAL", result.stage());
        assertEquals(Boolean.TRUE, result.complete());
        assertEquals(0, result.warningCount());
        assertEquals("MET", item(result, "student_info").state());
        assertEquals("On file; all 2 grades final", item(result, "tor").detail());
        assertEquals("Unemployed", item(result, "employment_status").detail());

        List<String> sql = ((RecordingJdbcTemplate) jdbc).executedSql();
        assertEquals(3, sql.size(), "student, documents, enrollments — one read each");
        sql.forEach(statement -> {
            assertFalse(statement.contains("content_data"), statement);
            assertFalse(statement.contains("profile_picture"), statement);
        });
    }

    @Test
    void anEnrollmentWithoutAGradeRowMakesTheTorUnmet() {
        insertStudent("SR1", "S1", "Completed");
        document("SR1", "Transcript of Records (TOR)", "2026-03-25 08:00:00");
        grade(enroll("SR1", "BPP-101"), "SR1", "BPP-101", "COMPETENT", null, true, "2026-03-20 09:00:00");
        enroll("SR1", "FBS-103"); // enrolled, never graded

        SoChecklistItem tor = item(soChecklistService.checklist(recordId("SR1")), "tor");

        assertEquals("UNMET", tor.state());
        assertEquals("TOR on file, but FBS-103 has no grade", tor.detail());
    }

    @Test
    void theNewestTorIsComparedWithTheLatestLock() {
        insertStudent("SR1", "S1", "Graduated");
        document("SR1", "Transcript of Records (TOR)", "2026-03-20 12:00:00");
        grade(enroll("SR1", "BPP-101"), "SR1", "BPP-101", "COMPETENT", null, true, "2026-03-20 09:00:00");
        grade(enroll("SR1", "COOK-102"), "SR1", "COOK-102", "COMPETENT", null, true, "2026-03-21 09:00:00");

        SoChecklistItem tor = item(soChecklistService.checklist(recordId("SR1")), "tor");

        assertEquals("WARNING", tor.state());
    }

    @Test
    void anUnknownRecordIsNotFound() {
        assertThrows(NoSuchElementException.class, () -> soChecklistService.checklist(999_999));
    }

    // ----- fixtures -----

    private void insertStudent(String studentId, String sectionCode, String status) {
        jdbc.update("""
                INSERT INTO student_records (student_id, last_name, first_name, middle_name, birthdate, sex,
                    permanent_address, section_code, enrollment_date, completion_date, student_status,
                    employment_status, baptized)
                VALUES (?, 'Dela Cruz', 'Ana', 'Reyes', DATE '2005-02-14', 'Female', 'Quezon City',
                    ?, DATE '2025-06-02', DATE '2026-03-20', ?, 'Unemployed', 0)
                """, studentId, sectionCode, status);
    }

    private Integer recordId(String studentId) {
        return jdbc.queryForObject("SELECT record_id FROM student_records WHERE student_id = ?",
                Integer.class, studentId);
    }

    private void document(String studentId, String type, String uploadedAt) {
        jdbc.update("INSERT INTO documents (student_id, document_type, file_name, file_type, file_size,"
                        + " content_data, upload_date) VALUES (?, ?, 'f.pdf', 'application/pdf', 1, X'00', ?)",
                studentId, type, Timestamp.valueOf(uploadedAt));
    }

    private int enroll(String studentId, String subjectCode) {
        jdbc.update("INSERT INTO classes (section_code, subject_code, semester) VALUES ('S1', ?, '1st')", subjectCode);
        Integer classId = jdbc.queryForObject("SELECT class_id FROM classes WHERE subject_code = ?",
                Integer.class, subjectCode);
        jdbc.update("INSERT INTO class_enrollments (class_id, student_id) VALUES (?, ?)", classId, studentId);
        return classId;
    }

    private void grade(int classId, String studentId, String subjectCode, String remarks, String gradeStatus,
                       boolean locked, String lockedAt) {
        jdbc.update("INSERT INTO grades (student_id, subject_code, remarks, grade_status, class_id, locked, locked_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                studentId, subjectCode, remarks, gradeStatus, classId, locked ? 1 : 0,
                lockedAt == null ? null : Timestamp.valueOf(lockedAt));
    }

    private static SoChecklistItem item(SoChecklistResponse response, String key) {
        return response.items().stream().filter(i -> i.key().equals(key)).findFirst().orElseThrow();
    }

    /** Wraps the real JdbcTemplate to record each parameterized query's SQL. */
    @TestConfiguration
    static class RecordingJdbcTemplateConfig {
        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new RecordingJdbcTemplate(dataSource);
        }
    }

    static class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<String> executedSql = new ArrayList<>();

        RecordingJdbcTemplate(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            executedSql.add(sql);
            return super.query(sql, rowMapper, args);
        }

        List<String> executedSql() {
            return executedSql;
        }

        void clear() {
            executedSql.clear();
        }
    }
}
```

Note: the first test clears the recorder right before `checklist(...)`, so the fixture inserts and lookups never count toward the "3 reads" assertion. Only `SoChecklistService`'s own queries are recorded.

- [ ] **Step 3: Write the failing controller test**

Create `SoChecklistControllerWebMvcTest.java`:

```java
package com.example.springboot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.dto.registrar.SoChecklistItem;
import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SoChecklistService;

@WebMvcTest(SoChecklistController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SoChecklistControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private SoChecklistService soChecklistService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void registrarGetsTheChecklist() throws Exception {
        when(soChecklistService.checklist(1)).thenReturn(new SoChecklistResponse("FINAL", true, 1,
                List.of(new SoChecklistItem("psa", "PSA Birth Certificate", "MET", "On file"))));

        mvc.perform(get("/api/registrar/student-records/1/so-checklist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("FINAL"))
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.warningCount").value(1))
                .andExpect(jsonPath("$.items[0].key").value("psa"))
                .andExpect(jsonPath("$.items[0].state").value("MET"));
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void anUnknownRecordIs404() throws Exception {
        when(soChecklistService.checklist(999))
                .thenThrow(new NoSuchElementException("Student record not found: 999"));

        mvc.perform(get("/api/registrar/student-records/999/so-checklist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void aTrainerIsForbidden() throws Exception {
        mvc.perform(get("/api/registrar/student-records/1/so-checklist"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get("/api/registrar/student-records/1/so-checklist"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 4: Run them to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.integration.SoChecklistIntegrationTest" --tests "com.example.springboot.controller.SoChecklistControllerWebMvcTest"`
Expected: FAIL to compile with `cannot find symbol ... SoChecklistService` / `SoChecklistController`.

- [ ] **Step 5: Implement the service**

Create `SoChecklistService.java`:

```java
package com.example.springboot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.service.SoReadinessPolicy.Enrollment;
import com.example.springboot.service.SoReadinessPolicy.Student;

/**
 * Gathers the facts for one student's SO checklist (spec 2026-10-01 SO checklist §7) and
 * hands them to {@link SoReadinessPolicy}. Three flat {@link JdbcTemplate} reads — never
 * an entity load, never a BLOB column — written in plain SQL (no GROUP_CONCAT or window
 * functions) so they run identically on MySQL and the H2 tests. Read-only: writes no
 * audit row, like the pre-export check.
 */
@Service
public class SoChecklistService {

    /** Course and batch resolve section-first, matching the document folder-tree ownership rules. */
    private static final String STUDENT_SQL = """
            SELECT s.student_id, s.student_status, s.last_name, s.first_name, s.middle_name,
                   s.birthdate, s.sex, s.permanent_address,
                   COALESCE(sec.course_code, s.course_code) AS course_code,
                   COALESCE(sec.batch_code, s.batch_code) AS batch_code,
                   s.enrollment_date, s.completion_date, s.employment_status
            FROM student_records s
            LEFT JOIN sections sec ON sec.section_code = s.section_code
            WHERE s.record_id = ?
            """;

    private static final String DOCUMENT_SQL = """
            SELECT document_type, upload_date
            FROM documents
            WHERE student_id = ?
            """;

    /** One row per class enrollment; grade columns are NULL when the enrollment has no grade row. */
    private static final String ENROLLMENT_SQL = """
            SELECT c.subject_code, g.grade_id, g.locked, g.locked_at, g.remarks, g.grade_status
            FROM class_enrollments ce
            JOIN classes c ON c.class_id = ce.class_id
            LEFT JOIN grades g ON g.class_id = ce.class_id AND g.student_id = ce.student_id
            WHERE ce.student_id = ?
            ORDER BY c.subject_code ASC, ce.class_id ASC
            """;

    private static final RowMapper<Student> STUDENT_MAPPER = (rs, rowNum) -> new Student(
            rs.getString("student_id"),
            rs.getString("student_status"),
            rs.getString("last_name"),
            rs.getString("first_name"),
            rs.getString("middle_name"),
            rs.getObject("birthdate", LocalDate.class),
            rs.getString("sex"),
            rs.getString("permanent_address"),
            rs.getString("course_code"),
            rs.getString("batch_code"),
            rs.getObject("enrollment_date", LocalDate.class),
            rs.getObject("completion_date", LocalDate.class),
            rs.getString("employment_status"));

    private static final RowMapper<Enrollment> ENROLLMENT_MAPPER = (rs, rowNum) -> new Enrollment(
            rs.getString("subject_code"),
            rs.getObject("grade_id") != null,
            rs.getBoolean("locked"),
            rs.getObject("locked_at", LocalDateTime.class),
            rs.getString("remarks"),
            rs.getString("grade_status"));

    private record DocumentRow(String documentType, LocalDateTime uploadDate) {
    }

    private final JdbcTemplate jdbcTemplate;

    public SoChecklistService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SoChecklistResponse checklist(Integer recordId) {
        Student student = jdbcTemplate.query(STUDENT_SQL, STUDENT_MAPPER, recordId).stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

        Set<String> documentTypes = new HashSet<>();
        LocalDateTime newestTorUpload = null;
        List<DocumentRow> documents = jdbcTemplate.query(DOCUMENT_SQL, (rs, rowNum) -> new DocumentRow(
                rs.getString("document_type"),
                rs.getObject("upload_date", LocalDateTime.class)), student.studentId());
        for (DocumentRow document : documents) {
            documentTypes.add(document.documentType());
            if (DocumentService.TOR_TYPE.equals(document.documentType()) && document.uploadDate() != null
                    && (newestTorUpload == null || document.uploadDate().isAfter(newestTorUpload))) {
                newestTorUpload = document.uploadDate();
            }
        }

        List<Enrollment> enrollments = jdbcTemplate.query(ENROLLMENT_SQL, ENROLLMENT_MAPPER, student.studentId());
        return SoReadinessPolicy.evaluate(student, documentTypes, newestTorUpload, enrollments);
    }
}
```

- [ ] **Step 6: Implement the controller**

Create `SoChecklistController.java`:

```java
package com.example.springboot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.service.SoChecklistService;

/**
 * Per-student SO checklist behind the SO Checklist tab in registrar.html (spec 2026-10-01 SO
 * checklist §7). REGISTRAR only, via the /api/registrar/** rule. Read-only and not
 * audit-logged. Kept out of RegistrarController so that controller's tests stay untouched.
 */
@RestController
@RequestMapping("/api/registrar/student-records")
public class SoChecklistController {

    private final SoChecklistService soChecklistService;

    public SoChecklistController(SoChecklistService soChecklistService) {
        this.soChecklistService = soChecklistService;
    }

    @GetMapping("/{recordId}/so-checklist")
    public ResponseEntity<SoChecklistResponse> soChecklist(@PathVariable Integer recordId) {
        return ResponseEntity.ok(soChecklistService.checklist(recordId));
    }
}
```

- [ ] **Step 7: Run them to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.integration.SoChecklistIntegrationTest" --tests "com.example.springboot.controller.SoChecklistControllerWebMvcTest"`
Expected: PASS, 4 + 4 tests.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/springboot/service/SoChecklistService.java \
  src/main/java/com/example/springboot/controller/SoChecklistController.java \
  src/test/resources/so-checklist-h2-schema.sql \
  src/test/java/com/example/springboot/integration/SoChecklistIntegrationTest.java \
  src/test/java/com/example/springboot/controller/SoChecklistControllerWebMvcTest.java
git commit -m "feat: add read-only per-student SO checklist endpoint"
```

---

### Task 8: `DocumentTypeSuggester` and the type-suggestion endpoint

**Files:**
- Create: `src/main/java/com/example/springboot/service/DocumentTypeSuggester.java`
- Create: `src/main/java/com/example/springboot/dto/registrar/TypeSuggestionResponse.java`
- Modify: `src/main/java/com/example/springboot/controller/DocumentController.java` (after `labels()`, line ~185)
- Test: `src/test/java/com/example/springboot/service/DocumentTypeSuggesterTest.java`, `DocumentControllerWebMvcTest`

- [ ] **Step 1: Write the failing unit test**

Create `DocumentTypeSuggesterTest.java`:

```java
package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Pins the alias list and the whole-word rule (spec 2026-10-01 SO checklist §9). */
class DocumentTypeSuggesterTest {

    private static void assertSuggests(String expectedType, List<String> labels) {
        for (String label : labels) {
            assertEquals(Optional.of(expectedType), DocumentTypeSuggester.suggest(label), label);
        }
    }

    @Test
    void torAliasesSuggestTheTor() {
        assertSuggests("Transcript of Records (TOR)",
                List.of("TOR", "tor scan", "Transcript", "transcript of records (copy)"));
    }

    @Test
    void psaAliasesSuggestThePsaBirthCertificate() {
        assertSuggests("PSA Birth Certificate", List.of("PSA", "Birth Cert", "NSO birth certificate"));
    }

    @Test
    void formIxAliasesAskForAFormIxType() {
        assertSuggests(DocumentTypeSuggester.FORM_IX_GENERIC, List.of("Form IX", "form 9", "Permanent Record"));
    }

    @Test
    void ojtAliasesSuggestTheOjtReport() {
        assertSuggests("OJT Report", List.of("OJT", "ojt report", "OJT Report - Jollibee"));
    }

    @Test
    void tvetAliasesSuggestTheTvetCertificate() {
        assertSuggests("Certificate of TVET Program", List.of("TVET", "tvet certificate"));
    }

    @Test
    void form137AliasesSuggestForm137() {
        assertSuggests("Form 137", List.of("Form 137", "F137"));
    }

    @Test
    void labelsThatMerelyContainAnAliasSuggestNothing() {
        for (String label : List.of("Director's letter", "Monitoring report", "History",
                "Torres family letter", "Medical Certificate", "Form 1370")) {
            assertTrue(DocumentTypeSuggester.suggest(label).isEmpty(), label);
        }
    }

    @Test
    void aTorRequestLetterIsStillSuggestedAsTorBecauseItIsOnlyAHint() {
        assertSuggests("Transcript of Records (TOR)", List.of("TOR request letter"));
    }

    @Test
    void blankOrNullLabelsSuggestNothing() {
        assertTrue(DocumentTypeSuggester.suggest(null).isEmpty());
        assertTrue(DocumentTypeSuggester.suggest("   ").isEmpty());
    }
}
```

- [ ] **Step 2: Write the failing controller tests**

In `DocumentControllerWebMvcTest.java`, add the following after `labelsReturnsDistinctLabelsForRegistrar`. The file already imports `MockMvcRequestBuilders.*`, `MockMvcResultMatchers.*`, `ArgumentMatchers.*` and `Mockito.*`.

```java
    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void typeSuggestionReturnsTheSuggestedTypeAndWritesNoAuditRow() throws Exception {
        mvc.perform(get("/api/registrar/documents/type-suggestion").param("label", "TOR copy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedType").value(TOR_TYPE));

        verifyNoInteractions(systemLogService);
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void typeSuggestionIsNullWhenNothingMatches() throws Exception {
        mvc.perform(get("/api/registrar/documents/type-suggestion").param("label", "History"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"suggestedType\":null}"));
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void trainerCannotAskForATypeSuggestion() throws Exception {
        mvc.perform(get("/api/registrar/documents/type-suggestion").param("label", "TOR"))
                .andExpect(status().isForbidden());
    }
```

- [ ] **Step 3: Run them to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.service.DocumentTypeSuggesterTest" --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"`
Expected: FAIL to compile with `cannot find symbol ... DocumentTypeSuggester`.

- [ ] **Step 4: Implement the suggester and DTO**

Create `TypeSuggestionResponse.java`:

```java
package com.example.springboot.dto.registrar;

/** A real document type for an "Others" label, or null when the label matches none. */
public record TypeSuggestionResponse(String suggestedType) {
}
```

Create `DocumentTypeSuggester.java`:

```java
package com.example.springboot.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Suggests a real document type for an "Others" label that looks like one (spec 2026-10-01
 * SO checklist §9), e.g. "TOR copy" → Transcript of Records, so it can be refiled before it
 * silently fails to count on the SO checklist. A hint only: the upload page never
 * auto-switches and never blocks. An alias matches only as a whole word, case-insensitive
 * ("Director's letter" does not match "tor"). All alias knowledge lives here, the same idea
 * as StudentNumberImportMapping: to catch a new spelling, add it below and re-run
 * DocumentTypeSuggesterTest.
 */
public final class DocumentTypeSuggester {

    /** Returned for Form IX aliases: there are three Form IX types, so the Registrar picks one. */
    public static final String FORM_IX_GENERIC = "Form IX";

    // Checked in this order; the first alias found wins.
    private static final List<Map.Entry<String, List<String>>> ALIASES = List.of(
            Map.entry(DocumentService.TOR_TYPE, List.of("transcript of records", "transcript", "tor")),
            Map.entry(DocumentService.PSA_BIRTH_CERTIFICATE_TYPE, List.of("birth certificate", "birth cert", "psa")),
            Map.entry(FORM_IX_GENERIC, List.of("permanent record", "form ix", "form 9")),
            Map.entry(DocumentService.OJT_REPORT_TYPE, List.of("ojt report", "ojt")),
            Map.entry(DocumentService.TVET_CERTIFICATE_TYPE, List.of("tvet certificate", "tvet")),
            Map.entry(DocumentService.FORM_137_TYPE, List.of("form 137", "f137")));

    private static final List<Map.Entry<String, Pattern>> PATTERNS = compile();

    private DocumentTypeSuggester() {
    }

    public static Optional<String> suggest(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        for (Map.Entry<String, Pattern> entry : PATTERNS) {
            if (entry.getValue().matcher(label).find()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** "birth cert" → (?<![\p{L}\p{N}])\Qbirth\E\s+\Qcert\E(?![\p{L}\p{N}]) — whole words, any spacing. */
    private static List<Map.Entry<String, Pattern>> compile() {
        List<Map.Entry<String, Pattern>> patterns = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : ALIASES) {
            for (String alias : entry.getValue()) {
                String words = Arrays.stream(alias.split(" "))
                        .map(Pattern::quote)
                        .collect(Collectors.joining("\\s+"));
                patterns.add(Map.entry(entry.getKey(), Pattern.compile(
                        "(?<![\\p{L}\\p{N}])" + words + "(?![\\p{L}\\p{N}])",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
            }
        }
        return List.copyOf(patterns);
    }
}
```

- [ ] **Step 5: Add the endpoint**

In `DocumentController.java`, add imports:

```java
import com.example.springboot.dto.registrar.TypeSuggestionResponse;
import com.example.springboot.service.DocumentTypeSuggester;
```

Add the method directly after `labels()`:

```java
    /**
     * "Did you mean …?" hint for an "Others" label on the upload dialog (spec 2026-10-01 SO
     * checklist §9). Read-only and not audit-logged; the page never switches on its own.
     */
    @GetMapping("/type-suggestion")
    public ResponseEntity<TypeSuggestionResponse> typeSuggestion(
            @RequestParam(name = "label", required = false) String label) {
        return ResponseEntity.ok(new TypeSuggestionResponse(DocumentTypeSuggester.suggest(label).orElse(null)));
    }
```

(`RequestParam` is already imported in this controller. If it isn't, add `import org.springframework.web.bind.annotation.RequestParam;`.)

- [ ] **Step 6: Run them to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.service.DocumentTypeSuggesterTest" --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"`
Expected: PASS (`DocumentTypeSuggesterTest` 9 tests; 3 new controller tests).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/springboot/service/DocumentTypeSuggester.java \
  src/main/java/com/example/springboot/dto/registrar/TypeSuggestionResponse.java \
  src/main/java/com/example/springboot/controller/DocumentController.java \
  src/test/java/com/example/springboot/service/DocumentTypeSuggesterTest.java \
  src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java
git commit -m "feat: suggest a real document type for look-alike Others labels"
```

---

### Task 9: Portal duplicate pre-check also blocks Completed and Graduated

Spec §4.5 marked this "Recommended (confirm while planning)". **Confirmed by the user on 2026-10-03: do it.**

**Files:**
- Modify: `src/main/java/com/example/springboot/controller/StudentPortalController.java`
- Test: `src/test/java/com/example/springboot/controller/StudentPortalControllerWebMvcTest.java` (new)

- [ ] **Step 1: Write the failing test**

Create `StudentPortalControllerWebMvcTest.java`:

```java
package com.example.springboot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.CustomUserDetailsService;

/** The public portal's name pre-check (spec 2026-10-01 SO checklist §4.5). */
@WebMvcTest(StudentPortalController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class StudentPortalControllerWebMvcTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private StudentRecordRepository studentRecordRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private void existingStudentWithStatus(String status) {
        StudentRecord record = new StudentRecord();
        record.setStudentStatus(status);
        when(studentRecordRepository.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                "Dela Cruz", "Ana", "Reyes")).thenReturn(List.of(record));
    }

    private void expectExists(boolean exists) throws Exception {
        mvc.perform(get("/api/student-portal/check-duplicate")
                        .param("lastName", "Dela Cruz").param("firstName", "Ana").param("middleName", "Reyes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(exists));
    }

    @Test
    void aCompletedStudentsNameIsReportedAsExisting() throws Exception {
        existingStudentWithStatus("Completed");
        expectExists(true);
    }

    @Test
    void aGraduatedStudentsNameIsReportedAsExisting() throws Exception {
        existingStudentWithStatus("Graduated");
        expectExists(true);
    }

    @Test
    void anEnrollingRecordIsResumableNotADuplicate() throws Exception {
        existingStudentWithStatus("Enrolling");
        expectExists(false);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.controller.StudentPortalControllerWebMvcTest"`
Expected: 2 FAIL (`aCompletedStudentsNameIsReportedAsExisting`, `aGraduatedStudentsNameIsReportedAsExisting`: `expected true but was false`), 1 PASS.

- [ ] **Step 3: Implement**

In `StudentPortalController.java`, add `import java.util.Set;`. Then replace:

```java
    /**
     * Returns exists=true only when a Submitted or Active record already exists for this name,
     * so Enrolling/Draft records are treated as resumable rather than duplicates.
     */
```

with:

```java
    /** Statuses that make a name a duplicate. Enrolling is resumable, so it is not listed. */
    private static final Set<String> BLOCKING_STATUSES = Set.of("Submitted", "Active", "Completed", "Graduated");

    /**
     * Returns exists=true when a Submitted, Active, Completed or Graduated record already exists
     * for this name, so Enrolling/Draft records are treated as resumable rather than duplicates.
     * StudentDetailsService.startOrResume still blocks any non-Enrolling match on its own.
     */
```

and replace:

```java
                .anyMatch(r -> "Submitted".equals(r.getStudentStatus())
                        || "Active".equals(r.getStudentStatus()));
```

with:

```java
                .anyMatch(r -> BLOCKING_STATUSES.contains(r.getStudentStatus()));
```

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew test --tests "com.example.springboot.controller.StudentPortalControllerWebMvcTest"`
Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/springboot/controller/StudentPortalController.java \
  src/test/java/com/example/springboot/controller/StudentPortalControllerWebMvcTest.java
git commit -m "fix: treat Completed and Graduated names as existing in the portal pre-check"
```

---

### Task 10: Frontend: Completed in status filters and badges

**Files:**
- Modify: `src/main/resources/static/css/dashboard.css` (after `.status-badge-graduated`, line ~826)
- Modify: `src/main/resources/static/registrar.html` (filter, line ~155; CSS link, line 12; script tag, line 521)
- Modify: `src/main/resources/static/student-numbers.html` (filter, line ~247; CSS link, line 12; script tag, line 422)
- Modify: `src/main/resources/static/js/registrar-students.js` (`renderStatusBadge`), `js/registrar-student-numbers.js` (`renderStatusBadge`)
- Test: `src/test/java/com/example/springboot/FrontendContractTest.java` (new)

`js/registrar-classes.js` was checked: its `status-badge-*` classes decorate trainer names and enrolled counts, not student statuses, so it needs no change.

- [ ] **Step 1: Write the failing contract test**

Create `src/test/java/com/example/springboot/FrontendContractTest.java`:

```java
package com.example.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Pins static HTML choices that mirror server-side constants, so the two cannot drift
 * (spec 2026-10-01 SO checklist §4.4 and §5).
 */
class FrontendContractTest {

    private static final String STATIC = "src/main/resources/static/";

    /** The option values of the select with this id, in page order. */
    static List<String> optionValues(String page, String selectId) throws Exception {
        String html = Files.readString(Path.of(STATIC + page));
        Matcher select = Pattern.compile("(?s)<select[^>]*id=\"" + selectId + "\"[^>]*>(.*?)</select>").matcher(html);
        assertTrue(select.find(), page + " has no #" + selectId);
        Matcher option = Pattern.compile("<option value=\"([^\"]*)\"").matcher(select.group(1));
        List<String> values = new ArrayList<>();
        while (option.find()) {
            values.add(option.group(1));
        }
        return values;
    }

    @Test
    void statusFiltersOfferCompletedBetweenActiveAndGraduated() throws Exception {
        for (String page : List.of("registrar.html", "student-numbers.html")) {
            assertEquals(List.of("", "Enrolling", "Submitted", "Active", "Completed", "Graduated"),
                    optionValues(page, "studentStatusFilter"), page);
        }
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.FrontendContractTest"`
Expected: FAIL. `registrar.html` lists `[, Enrolling, Submitted, Active, Graduated]`.

- [ ] **Step 3: Add the filter options**

In both `registrar.html` and `student-numbers.html`, inside `#studentStatusFilter`, replace:

```html
                            <option value="Active">Active</option>
                            <option value="Graduated">Graduated</option>
```

with:

```html
                            <option value="Active">Active</option>
                            <option value="Completed">Completed</option>
                            <option value="Graduated">Graduated</option>
```

(Only the filter select. `registrar.html`'s `#editStatusSelect` is changed in Task 11.)

- [ ] **Step 4: Add the badge style**

In `dashboard.css`, after the `.status-badge-graduated { ... }` block, add:

```css

.status-badge-completed {
    background: rgba(111, 66, 193, 0.12);
    color: #59359a;
}
```

- [ ] **Step 5: Use it in both badge renderers**

In `js/registrar-students.js` **and** `js/registrar-student-numbers.js`, in `renderStatusBadge`, replace:

```js
        } else if (status === 'Graduated') {
            cls = 'status-badge-graduated';
```

with:

```js
        } else if (status === 'Completed') {
            cls = 'status-badge-completed';
        } else if (status === 'Graduated') {
            cls = 'status-badge-graduated';
```

- [ ] **Step 6: Bust the caches**

- `registrar.html`: `css/dashboard.css?v=4` → `css/dashboard.css?v=5`; `js/registrar-students.js?v=10` → `js/registrar-students.js?v=11`.
- `student-numbers.html`: `css/dashboard.css?v=3` → `css/dashboard.css?v=5`; `js/registrar-student-numbers.js?v=3` → `js/registrar-student-numbers.js?v=4`.

- [ ] **Step 7: Run it to verify it passes**

Run: `./gradlew test --tests "com.example.springboot.FrontendContractTest"`
Expected: PASS, 1 test.

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/static/css/dashboard.css src/main/resources/static/registrar.html \
  src/main/resources/static/student-numbers.html src/main/resources/static/js/registrar-students.js \
  src/main/resources/static/js/registrar-student-numbers.js \
  src/test/java/com/example/springboot/FrontendContractTest.java
git commit -m "feat: show the Completed status in filters and badges"
```

---

### Task 11: Frontend: transition-aware Edit Status dialog

**Files:**
- Modify: `src/main/resources/static/registrar.html` (`#editStatusModal` body, lines ~250-259; script tag)
- Modify: `src/main/resources/static/js/registrar-students.js` (`setupEditStatus`, lines ~643-721)
- Test: `FrontendContractTest`

- [ ] **Step 1: Write the failing contract test**

Add to `FrontendContractTest` (and add `import com.example.springboot.dto.registrar.UpdateStudentStatusRequest;`):

```java
    @Test
    void editStatusOptionsMatchTheServerAllowedValues() throws Exception {
        assertEquals(List.of(UpdateStudentStatusRequest.ALLOWED_VALUES_DISPLAY.split(", ")),
                optionValues("registrar.html", "editStatusSelect"));
    }
```

Run: `./gradlew test --tests "com.example.springboot.FrontendContractTest"`
Expected: FAIL. The page lists `[Enrolling, Active, Graduated]`.

- [ ] **Step 2: Replace the Edit Status modal body**

In `registrar.html`, replace the whole `<div class="modal-body">...</div>` inside `#editStatusModal`:

```html
                <div class="modal-body">
                    <p class="mb-3">Changing status for <strong id="editStatusStudentName">this student</strong>.</p>
                    <div class="alert d-none" id="editStatusAlert" role="alert"></div>
                    <label for="editStatusSelect" class="form-label">New Status</label>
                    <select class="form-select" id="editStatusSelect">
                        <option value="Enrolling">Enrolling</option>
                        <option value="Active">Active</option>
                        <option value="Graduated">Graduated</option>
                    </select>
                </div>
```

with:

```html
                <div class="modal-body">
                    <p class="mb-3">Changing status for <strong id="editStatusStudentName">this student</strong>.</p>
                    <div class="alert d-none" id="editStatusAlert" role="alert"></div>
                    <label for="editStatusSelect" class="form-label">New Status</label>
                    <select class="form-select" id="editStatusSelect" aria-describedby="editStatusSelectHelp">
                        <option value="Enrolling">Enrolling</option>
                        <option value="Active">Active</option>
                        <option value="Completed">Completed</option>
                        <option value="Graduated">Graduated</option>
                    </select>
                    <div class="form-text" id="editStatusSelectHelp">
                        Completed = training and OJT finished. Graduated = Special Order issued.
                        Statuses that cannot be reached from the current one are disabled.
                    </div>
                    <div class="mt-3 d-none" id="editStatusDateGroup">
                        <label for="editStatusCompletionDate" class="form-label">Completion Date</label>
                        <input type="date" class="form-control" id="editStatusCompletionDate"
                               aria-describedby="editStatusDateHelp">
                        <div class="form-text" id="editStatusDateHelp"></div>
                    </div>
                    <div class="mt-3 d-none" id="editStatusReasonGroup">
                        <label for="editStatusReason" class="form-label">Reason</label>
                        <input type="text" class="form-control" id="editStatusReason" maxlength="255"
                               aria-describedby="editStatusReasonHelp" placeholder="e.g. Digitized archive record">
                        <div class="form-text" id="editStatusReasonHelp">Saved in the system log with this change.</div>
                    </div>
                </div>
```

- [ ] **Step 3: Replace `setupEditStatus` in `registrar-students.js`**

Replace the whole `function setupEditStatus(dataTable) { ... }` (from its declaration to its closing brace, just before `})();`) with:

```js
    // Mirrors service/StudentStatusTransitions.java so the dialog only offers moves the
    // server will accept. The server stays the authority and re-checks every change.
    function statusRule(from, to) {
        if (from === to) return { allowed: true };
        if (to === 'Completed') {
            if (from === 'Active') return { allowed: true, needsDate: true };
            if (from === 'Graduated') return { allowed: true, needsReason: true };
            return { allowed: false };
        }
        if (to === 'Graduated') {
            if (from === 'Completed') return { allowed: true };
            if (from === 'Active') return { allowed: true, needsDate: true, needsReason: true, archive: true };
            return { allowed: false };
        }
        if (from === 'Graduated') return { allowed: false };
        if (from === 'Completed') return { allowed: to === 'Active' };
        return { allowed: true };
    }

    /** Today as yyyy-mm-dd in the browser's local time (toISOString would give the UTC date). */
    function todayIso() {
        const d = new Date();
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-'
            + String(d.getDate()).padStart(2, '0');
    }

    // Replaced by the real implementation in Task 13 (SO Checklist tab).
    function resetSoChecklistTab() {}

    function setupEditStatus(dataTable) {
        const editBtn = document.getElementById('editStatusBtn');
        const modalEl = document.getElementById('editStatusModal');
        const nameEl = document.getElementById('editStatusStudentName');
        const selectEl = document.getElementById('editStatusSelect');
        const saveBtn = document.getElementById('saveStatusBtn');
        const alertEl = document.getElementById('editStatusAlert');
        const dateGroup = document.getElementById('editStatusDateGroup');
        const dateInput = document.getElementById('editStatusCompletionDate');
        const dateHelp = document.getElementById('editStatusDateHelp');
        const reasonGroup = document.getElementById('editStatusReasonGroup');
        const reasonInput = document.getElementById('editStatusReason');

        if (!editBtn || !modalEl || !selectEl || !saveBtn || !editStatusModal
                || !dateGroup || !dateInput || !reasonGroup || !reasonInput) return;

        function showStatusAlert(message, type) {
            if (!alertEl) return;
            alertEl.className = 'alert alert-' + type + ' mt-0 mb-3';
            alertEl.textContent = message;
            alertEl.classList.remove('d-none');
        }

        function currentRule() {
            return statusRule(currentRecordStatus, selectEl.value);
        }

        function updateStatusFields() {
            const rule = currentRule();
            dateGroup.classList.toggle('d-none', !rule.needsDate);
            reasonGroup.classList.toggle('d-none', !rule.needsReason);
            if (rule.needsDate && !dateInput.value) {
                dateInput.value = todayIso();
            }
            if (dateHelp) {
                dateHelp.textContent = rule.archive
                    ? 'Moving an Active record straight to Graduated is for digitized archive records. '
                        + 'Enter the date this student actually finished training and OJT.'
                    : 'The day the student finished training and OJT. Defaults to today.';
            }
        }

        editBtn.addEventListener('click', function () {
            if (!currentRecordId) return;
            if (nameEl) {
                nameEl.textContent = currentRecordIdentifier || ('Record #' + currentRecordId);
            }
            Array.prototype.forEach.call(selectEl.options, function (option) {
                option.disabled = !statusRule(currentRecordStatus, option.value).allowed;
            });
            selectEl.value = currentRecordStatus || 'Enrolling';
            dateInput.value = '';
            dateInput.max = todayIso();
            reasonInput.value = '';
            updateStatusFields();
            hideAlert('editStatusAlert');
            detailsModal.hide();
            editStatusModal.show();
        });

        selectEl.addEventListener('change', updateStatusFields);

        saveBtn.addEventListener('click', async function () {
            if (!currentRecordId) return;

            const rule = currentRule();
            const payload = { studentStatus: selectEl.value };
            if (rule.needsDate) {
                if (!dateInput.value) {
                    showStatusAlert('Enter the completion date.', 'danger');
                    return;
                }
                payload.completionDate = dateInput.value;
            }
            if (rule.needsReason) {
                if (!reasonInput.value.trim()) {
                    showStatusAlert('Enter a reason for this change.', 'danger');
                    return;
                }
                payload.reason = reasonInput.value.trim();
            }

            saveBtn.disabled = true;
            const originalLabel = saveBtn.textContent;
            saveBtn.textContent = 'Saving...';
            hideAlert('editStatusAlert');

            try {
                const res = await fetch(
                    '/api/registrar/student-records/' + encodeURIComponent(currentRecordId) + '/status',
                    {
                        method: 'PUT',
                        credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    }
                );

                if (!res.ok) {
                    const body = await res.json().catch(function () { return null; });
                    const fieldErrors = body && body.errors ? Object.values(body.errors).join(' ') : '';
                    showStatusAlert(
                        fieldErrors || (body && body.message) || 'Could not save the status.',
                        'danger'
                    );
                    return;
                }

                const saved = await res.json();
                currentRecordStatus = saved.studentStatus;
                setText('detailsStudentStatus', saved.studentStatus);
                resetSoChecklistTab(saved.studentStatus);
                dataTable.ajax.reload(null, false);
                editStatusModal.hide();
            } catch (err) {
                showStatusAlert('Network error. Could not save the status.', 'danger');
            } finally {
                saveBtn.disabled = false;
                saveBtn.textContent = originalLabel;
            }
        });

        // The status modal is opened from within the details modal (details hides first
        // to avoid stacked-modal focus issues), so whenever it closes — Cancel, X, or a
        // successful save above — bring the details modal back rather than leaving the
        // registrar with nothing open.
        modalEl.addEventListener('hidden.bs.modal', function () {
            hideAlert('editStatusAlert');
            detailsModal.show();
        });
    }
```

Then bump `js/registrar-students.js?v=11` to `js/registrar-students.js?v=12` in `registrar.html`.

- [ ] **Step 4: Run the contract test**

Run: `./gradlew test --tests "com.example.springboot.FrontendContractTest"`
Expected: PASS, 2 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/registrar.html src/main/resources/static/js/registrar-students.js \
  src/test/java/com/example/springboot/FrontendContractTest.java
git commit -m "feat: guide status changes with allowed moves, completion date and reason"
```

(The dialog is checked in the browser in Task 15, after the migration. Before then the live DB has no `completion_date` column, and every student-record path would fail.)

---

### Task 12: Frontend: edit form dates and Employment section

**Files:**
- Modify: `src/main/resources/static/student-records.html` (Enrollment row, lines ~453-456; after the OJT row, line ~472; script tag, line 678)
- Modify: `src/main/resources/static/js/registrar-student-records-edit.js` (comment at lines ~229-234; `populateForm`, line ~305; `buildPayload`, lines ~457-486)
- Test: `FrontendContractTest`

- [ ] **Step 1: Write the failing contract test**

Add to `FrontendContractTest` (and add `import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;`):

```java
    @Test
    void employmentStatusOptionsMatchTheServerPattern() throws Exception {
        List<String> expected = new ArrayList<>();
        expected.add(""); // "Not set"
        expected.addAll(List.of(StudentRecordUpdateRequest.EMPLOYMENT_STATUS_DISPLAY.split(", ")));
        assertEquals(expected, optionValues("student-records.html", "editEmploymentStatus"));
    }
```

Run: `./gradlew test --tests "com.example.springboot.FrontendContractTest"`
Expected: FAIL with `student-records.html has no #editEmploymentStatus`.

- [ ] **Step 2: Make the enrollment date editable and add the completion date**

In `student-records.html`, replace:

```html
                                        <div class="col-md-3">
                                            <label for="editEnrollmentDate" class="form-label">Enrollment Date</label>
                                            <input type="text" class="form-control" id="editEnrollmentDate" disabled readonly>
                                        </div>
```

with:

```html
                                        <div class="col-md-3">
                                            <label for="editEnrollmentDate" class="form-label">Enrollment Date</label>
                                            <input type="date" class="form-control" id="editEnrollmentDate">
                                        </div>
                                        <div class="col-md-3">
                                            <label for="editCompletionDate" class="form-label">Completion Date</label>
                                            <input type="date" class="form-control" id="editCompletionDate"
                                                aria-describedby="editCompletionDateHelp" disabled>
                                            <div class="form-text" id="editCompletionDateHelp">
                                                Set by <strong>Edit Status → Completed</strong>. Editable here only for
                                                Completed or Graduated students.
                                            </div>
                                        </div>
```

- [ ] **Step 3: Add the Employment section under OJT**

In `student-records.html`, replace:

```html
                                            <input type="number" step="0.01" min="0" class="form-control" id="editOjtHoursRendered">
                                        </div>
                                    </div>

                                    <hr class="my-4">
                                    <h6 class="section-title">TESDA Qualifications</h6>
```

with:

```html
                                            <input type="number" step="0.01" min="0" class="form-control" id="editOjtHoursRendered">
                                        </div>
                                    </div>

                                    <hr class="my-4">
                                    <h6 class="section-title">Employment</h6>
                                    <div class="row g-3">
                                        <div class="col-md-4">
                                            <label for="editEmploymentStatus" class="form-label">Employment Status</label>
                                            <select class="form-select" id="editEmploymentStatus"
                                                aria-describedby="editEmploymentStatusHelp">
                                                <option value="">Not set</option>
                                                <option value="Employed">Employed</option>
                                                <option value="Self-employed">Self-employed</option>
                                                <option value="Unemployed">Unemployed</option>
                                                <option value="Further studies">Further studies</option>
                                            </select>
                                            <div class="form-text" id="editEmploymentStatusHelp">
                                                Required for the TESDA Special Order once the student is Completed.
                                            </div>
                                        </div>
                                    </div>

                                    <hr class="my-4">
                                    <h6 class="section-title">TESDA Qualifications</h6>
```

Then change `js/registrar-student-records-edit.js?v=9` to `js/registrar-student-records-edit.js?v=10` in the script tag.

- [ ] **Step 4: Update the JS comment, population, and payload**

In `registrar-student-records-edit.js`:

(a) Replace:

```js
    // that must always stay read-only (Record ID, Enrollment Date, Reference No.,
    // Student Number) keep their own explicit disabled/readonly attribute, which is
```

with:

```js
    // that must always stay read-only (Record ID, Reference No., Student Number) — and
    // Completion Date for students who are not Completed/Graduated — keep their own
    // explicit disabled/readonly attribute, which is
```

(b) In `populateForm`, replace:

```js
        setVal('editEnrollmentDate', r.enrollmentDate || 'Not set');
```

with:

```js
        setVal('editEnrollmentDate', r.enrollmentDate || '');
        setVal('editCompletionDate', r.completionDate || '');
        // Completion date is set by the Completed status change; it can be corrected here
        // only while the student is Completed or Graduated (the server enforces the same).
        const completionInput = document.getElementById('editCompletionDate');
        if (completionInput) {
            completionInput.disabled = ['Completed', 'Graduated'].indexOf(r.studentStatus) === -1;
        }
        setVal('editEmploymentStatus', r.employmentStatus);
```

(c) In `buildPayload`, replace:

```js
            guardian: buildGuardian()
        };
```

with:

```js
            guardian: buildGuardian(),
            // Always sent back as loaded (a disabled input still has its value), so a routine
            // save never wipes these.
            enrollmentDate: getNullableDate('editEnrollmentDate'),
            completionDate: getNullableDate('editCompletionDate'),
            employmentStatus: getVal('editEmploymentStatus') || null
        };
```

- [ ] **Step 5: Run the contract test**

Run: `./gradlew test --tests "com.example.springboot.FrontendContractTest"`
Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/student-records.html \
  src/main/resources/static/js/registrar-student-records-edit.js \
  src/test/java/com/example/springboot/FrontendContractTest.java
git commit -m "feat: edit enrollment/completion dates and employment status on the student form"
```

---

### Task 13: Frontend: SO Checklist tab in the details modal

**Files:**
- Modify: `src/main/resources/static/registrar.html` (`<style>`, lines 13-45; `#studentRecordDetailsModal` body, lines ~204-228; script tag)
- Modify: `src/main/resources/static/js/registrar-students.js`

No automated test (the repo has no JS test runner). Verified live in Task 15.

- [ ] **Step 1: Add the checklist CSS**

In `registrar.html`, inside `<style>`, after the `.id-picture-preview { ... }` block, add:

```css

        /* SO Checklist tab: icon + visually-hidden state text, never colour alone. */
        .so-checklist {
            list-style: none;
            margin: 0;
            padding: 0;
        }
        .so-checklist-row {
            align-items: flex-start;
            border-bottom: 1px solid #e9ecef;
            display: flex;
            gap: 0.75rem;
            padding: 0.6rem 0;
        }
        .so-checklist-row:last-child {
            border-bottom: none;
        }
        .so-state-icon {
            flex: 0 0 1.5rem;
            font-weight: 700;
            text-align: center;
        }
        .so-state-met { color: var(--color-green-dark); }
        .so-state-warning { color: #856404; }
        .so-state-unmet { color: #b02a37; }
        .so-state-not_due { color: #6c757d; }
        .so-checklist-detail {
            color: #555;
            font-size: 0.85rem;
            margin: 0;
        }
```

- [ ] **Step 2: Wrap the details body in tabs**

In `registrar.html`, the details modal body is currently:

```html
                <div class="modal-body">
                    <div class="alert d-none" id="studentDetailsAlert" role="alert"></div>
                    <div class="text-center mb-3">
                        <img id="detailsIdPicture" class="id-picture-preview"
                             src="images/TempProfile%201.webp" alt="No ID picture on file">
                    </div>
                    <div class="detail-grid">
                        ...16 <article class="detail-card"> lines (Record ID ... Permanent Address)...
                    </div>
                </div>
```

Restructure it to the following. Move the 16 existing `<article class="detail-card">…</article>` lines into the new `.detail-grid` unchanged, where the comment marks them:

```html
                <div class="modal-body">
                    <div class="alert d-none" id="studentDetailsAlert" role="alert"></div>
                    <ul class="nav nav-tabs mb-3" id="studentDetailsTabs" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link active" id="tab-details-info" data-bs-toggle="tab"
                                data-bs-target="#pane-details-info" type="button" role="tab"
                                aria-controls="pane-details-info" aria-selected="true">Details</button>
                        </li>
                        <li class="nav-item d-none" role="presentation" id="soChecklistTabItem">
                            <button class="nav-link" id="tab-details-so" data-bs-toggle="tab"
                                data-bs-target="#pane-details-so" type="button" role="tab"
                                aria-controls="pane-details-so" aria-selected="false">SO Checklist</button>
                        </li>
                    </ul>
                    <div class="tab-content">
                        <div class="tab-pane fade show active" id="pane-details-info" role="tabpanel"
                             aria-labelledby="tab-details-info">
                            <div class="text-center mb-3">
                                <img id="detailsIdPicture" class="id-picture-preview"
                                     src="images/TempProfile%201.webp" alt="No ID picture on file">
                            </div>
                            <div class="detail-grid">
                                <!-- the 16 existing <article class="detail-card"> lines go here, unchanged -->
                            </div>
                        </div>
                        <div class="tab-pane fade" id="pane-details-so" role="tabpanel"
                             aria-labelledby="tab-details-so">
                            <div id="soChecklistContent" aria-live="polite"></div>
                        </div>
                    </div>
                </div>
```

Delete the placeholder comment once the 16 lines are in place.

- [ ] **Step 3: Add the tab logic in `registrar-students.js`**

(a) Delete the temporary no-op added in Task 11:

```js
    // Replaced by the real implementation in Task 13 (SO Checklist tab).
    function resetSoChecklistTab() {}
```

(b) Below `let assignBatchTargetRecordId = null;` at the top of the IIFE, add:

```js
    let soChecklistLoadedFor = null;

    const SO_CHECKLIST_STATUSES = ['Active', 'Completed', 'Graduated'];
    const SO_STATE_DISPLAY = {
        MET: { icon: '✓', text: 'Met' },
        WARNING: { icon: '⚠', text: 'Met with a warning' },
        UNMET: { icon: '✗', text: 'Not met' },
        NOT_DUE: { icon: '—', text: 'Not yet due' }
    };
```

(c) Below `function hideAlert(id) { ... }`, add:

```js
    function makeEl(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    /**
     * Shows the SO Checklist tab only for statuses that have a checklist, always reopens
     * on the Details tab, and forgets any loaded checklist (it reloads on first view).
     */
    function resetSoChecklistTab(status) {
        soChecklistLoadedFor = null;
        const content = document.getElementById('soChecklistContent');
        if (content) content.replaceChildren();
        const tabItem = document.getElementById('soChecklistTabItem');
        if (tabItem) tabItem.classList.toggle('d-none', SO_CHECKLIST_STATUSES.indexOf(status) === -1);
        const detailsTab = document.getElementById('tab-details-info');
        if (detailsTab) bootstrap.Tab.getOrCreateInstance(detailsTab).show();
    }

    async function loadSoChecklist(recordId) {
        const content = document.getElementById('soChecklistContent');
        if (!content) return;
        content.replaceChildren(makeEl('p', 'text-muted mb-0', 'Loading checklist…'));

        let res;
        try {
            res = await fetch('/api/registrar/student-records/' + encodeURIComponent(recordId) + '/so-checklist', {
                credentials: 'same-origin'
            });
        } catch (err) {
            if (recordId !== currentRecordId) return;
            content.replaceChildren(makeEl('div', 'alert alert-danger mb-0',
                'Could not load the SO checklist. Check the connection and try again.'));
            return;
        }
        if (recordId !== currentRecordId) return; // the Registrar opened another student meanwhile

        if (res.status === 401) {
            const box = makeEl('div', 'alert alert-warning mb-0');
            box.appendChild(document.createTextNode('Your session has expired. '));
            const link = makeEl('a', '', 'Log in again');
            link.href = 'index.html';
            box.appendChild(link);
            content.replaceChildren(box);
            return;
        }
        if (res.status === 404) {
            content.replaceChildren(makeEl('div', 'alert alert-warning mb-0',
                'This student is no longer available — refresh.'));
            return;
        }
        if (!res.ok) {
            content.replaceChildren(makeEl('div', 'alert alert-danger mb-0', 'Could not load the SO checklist.'));
            return;
        }

        const data = await res.json();
        soChecklistLoadedFor = recordId;
        renderSoChecklist(content, data);
    }

    function renderSoChecklist(container, data) {
        container.replaceChildren();
        if (data.stage === 'NOT_APPLICABLE') {
            container.appendChild(makeEl('p', 'text-muted mb-0', 'There is no SO checklist for this status.'));
            return;
        }

        if (data.stage === 'PREVIEW') {
            container.appendChild(makeEl('div', 'alert alert-info',
                'Preview — the full checklist applies once the student is Completed.'));
        } else {
            const unmet = data.items.filter(function (i) { return i.state === 'UNMET'; }).length;
            const warnings = data.warningCount
                ? ' — ' + data.warningCount + ' warning' + (data.warningCount === 1 ? '' : 's')
                : '';
            // Never "Ready to file": batch-level SO items are not checked here.
            const summary = data.complete
                ? 'Student requirements: Complete' + warnings
                : 'Student requirements: ' + unmet + ' of ' + data.items.length + ' unmet' + warnings;
            const tone = data.complete ? (data.warningCount ? 'alert-warning' : 'alert-success') : 'alert-danger';
            container.appendChild(makeEl('div', 'alert ' + tone, summary));
        }

        const list = makeEl('ul', 'so-checklist');
        data.items.forEach(function (item) {
            const display = SO_STATE_DISPLAY[item.state] || { icon: '?', text: item.state };
            const row = makeEl('li', 'so-checklist-row');
            const icon = makeEl('span', 'so-state-icon so-state-' + String(item.state).toLowerCase(), display.icon);
            icon.setAttribute('aria-hidden', 'true');
            row.appendChild(icon);
            const body = makeEl('div', '');
            body.appendChild(makeEl('span', 'visually-hidden', display.text + ': '));
            body.appendChild(makeEl('strong', '', item.label));
            body.appendChild(makeEl('p', 'so-checklist-detail', item.detail));
            row.appendChild(body);
            list.appendChild(row);
        });
        container.appendChild(list);
        container.appendChild(makeEl('p', 'text-muted small mt-3 mb-0',
            'Batch-level SO items (List of Students, Attendance Sheet, Registry of Workers, '
            + 'Registry of Workers Assessed) are not checked here.'));
    }
```

(d) In `loadRecordDetails`, directly before `detailsModal.show();`, add:

```js
        resetSoChecklistTab(r.studentStatus);
```

(e) In the `DOMContentLoaded` handler, directly after the `if (editStatusEl) { editStatusModal = new bootstrap.Modal(editStatusEl); }` block, add:

```js
        const soTab = document.getElementById('tab-details-so');
        if (soTab) {
            // Load on first view of the tab, not on modal open (spec §8).
            soTab.addEventListener('shown.bs.tab', function () {
                if (currentRecordId && soChecklistLoadedFor !== currentRecordId) {
                    loadSoChecklist(currentRecordId);
                }
            });
        }
```

(f) In `registrar.html`, bump `js/registrar-students.js?v=12` to `js/registrar-students.js?v=13`.

- [ ] **Step 4: Run the frontend contract tests (sanity, the markup changed)**

Run: `./gradlew test --tests "com.example.springboot.FrontendContractTest"`
Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/registrar.html src/main/resources/static/js/registrar-students.js
git commit -m "feat: add SO Checklist tab to the student record details modal"
```

---

### Task 14: Frontend: "Did you mean …?" hint on staged "Others" rows

**Files:**
- Modify: `src/main/resources/static/js/registrar-documents.js` (`stagedFiles` declaration line ~34; `addStagedFiles` line ~1183; `renderStagedFiles` lines ~1215-1247)
- Modify: `src/main/resources/static/documents.html` (`<style>` near `.staged-file-label`, line ~158; script tag, line 611)

No automated test (no JS runner). Verified live in Task 15.

- [ ] **Step 1: Track the suggestion on each staged entry**

In `addStagedFiles`, replace:

```js
            stagedFiles.push({ file: file, documentType: 'Others', documentLabel: '' });
```

with:

```js
            stagedFiles.push({ file: file, documentType: 'Others', documentLabel: '', suggestion: null });
```

Change the `stagedFiles` declaration comment at line ~34 from `// [{ file, documentType, documentLabel }]` to `// [{ file, documentType, documentLabel, suggestion }]`.

- [ ] **Step 2: Clear the suggestion when the type changes**

In `renderStagedFiles`, in the select's `change` listener, replace:

```js
                if (entry.documentType !== 'Others') entry.documentLabel = '';
```

with:

```js
                if (entry.documentType !== 'Others') {
                    entry.documentLabel = '';
                    entry.suggestion = null;
                }
```

- [ ] **Step 3: Render the hint under the label input**

In `renderStagedFiles`, replace:

```js
                input.value = entry.documentLabel;
                input.addEventListener('input', function () { entry.documentLabel = input.value; });
                labelWrap.appendChild(input);
                row.appendChild(labelWrap);
```

with:

```js
                input.value = entry.documentLabel;
                const hint = el('div', { class: 'staged-file-hint small mt-1', 'aria-live': 'polite' });
                input.addEventListener('input', function () {
                    entry.documentLabel = input.value;
                    scheduleTypeSuggestion(entry, hint);
                });
                labelWrap.appendChild(input);
                labelWrap.appendChild(hint);
                renderTypeSuggestion(hint, entry);
                row.appendChild(labelWrap);
```

- [ ] **Step 4: Add the two helpers**

Directly after the `renderStagedFiles` function, add:

```js
    // "Did you mean …?" for "Others" labels (spec 2026-10-01 SO checklist §9). The alias list
    // lives on the server (DocumentTypeSuggester); this only asks and shows the answer. It
    // never switches the type on its own and never blocks the upload.
    const SUGGESTION_DELAY_MS = 300;
    const FORM_IX_GENERIC = 'Form IX';

    function scheduleTypeSuggestion(entry, hint) {
        clearTimeout(entry.suggestionTimer);
        entry.suggestion = null;
        renderTypeSuggestion(hint, entry);
        const label = entry.documentLabel.trim();
        if (!label) return;
        entry.suggestionTimer = setTimeout(function () {
            $.ajax({
                url: '/api/registrar/documents/type-suggestion',
                data: { label: label },
                success: function (res) {
                    // Ignore a reply for a label the Registrar has since changed.
                    if (entry.documentType !== 'Others' || entry.documentLabel.trim() !== label) return;
                    entry.suggestion = res && res.suggestedType ? res.suggestedType : null;
                    renderTypeSuggestion(hint, entry);
                }
                // No error handler on purpose: the hint is optional, so a failed lookup shows nothing.
            });
        }, SUGGESTION_DELAY_MS);
    }

    function renderTypeSuggestion(hint, entry) {
        hint.replaceChildren();
        if (!entry.suggestion) return;
        if (entry.suggestion === FORM_IX_GENERIC) {
            hint.appendChild(document.createTextNode(
                'Did you mean a Form IX? Pick the matching Form IX type from the list.'));
            return;
        }
        hint.appendChild(document.createTextNode('Did you mean '));
        hint.appendChild(el('em', { text: entry.suggestion }));
        hint.appendChild(document.createTextNode('? '));
        const switchBtn = el('button', {
            type: 'button', class: 'btn btn-link btn-sm p-0 align-baseline', text: 'Switch'
        });
        switchBtn.addEventListener('click', function () {
            entry.documentType = entry.suggestion;
            entry.documentLabel = '';
            entry.suggestion = null;
            renderStagedFiles();
        });
        hint.appendChild(switchBtn);
    }
```

- [ ] **Step 5: Style the hint and bust the cache**

In `documents.html`, after the `.staged-file-row .staged-file-label { ... }` block, add:

```css
        .staged-file-row .staged-file-hint {
            color: #856404;
        }
```

Then change `js/registrar-documents.js?v=7` to `js/registrar-documents.js?v=8`.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/js/registrar-documents.js src/main/resources/static/documents.html
git commit -m "feat: hint a real document type for look-alike Others labels on upload"
```

---

### Task 15: Full regression, migration, live check, memory bank

**Files:**
- Modify: `memory-bank/activeContext.md`, `progress.md`, `changeLog.md`, `decisions.md`, `testing.md` (newest entry at the **top** of each)
- Modify: `CLAUDE.md` ("Key Conventions")

- [ ] **Step 1: Run the full suite**

```bash
./gradlew test --rerun-tasks
find build/test-results/test -name '*.xml' -exec grep -h -o 'tests="[0-9]*"' {} + | awk -F'"' '{s+=$2} END {print s}'
grep -l '<failure\|<error' build/test-results/test/*.xml || echo "no failures"
```

Expected: `BUILD SUCCESSFUL`, **664**, `no failures`. If the count differs, find which task's count is off before going further.

- [ ] **Step 2: Back up the live DB (ask the user first)**

**Stop and ask the user** for approval to back up and migrate the live `AnihanSRMS` DB (Docker container `mysql-server`). Once approved, run:

```bash
docker exec mysql-server mysqldump -uroot -pmy_password --routines AnihanSRMS > src/main/sql/backup-2026-10-03-pre-so-checklist.sql
ls -la src/main/sql/backup-2026-10-03-pre-so-checklist.sql   # expect a non-empty file
```

- [ ] **Step 3: Apply the migration (only after approval)**

```bash
docker exec -i mysql-server mysql -uroot -pmy_password < src/main/sql/migrations/2026-10-03-so-checklist.sql
```

Expected verification output: `enrollment_date`, `completion_date` (`date`, `YES`), `student_status`, `employment_status` (`varchar(25)`, `YES`) in that order, and `with_completion_date = 0`, `with_employment_status = 0`. Run it a second time to confirm it is a no-op (same output, no error).

- [ ] **Step 4: Live check with Playwright (real app + DB)**

Run `./gradlew bootRun` (background) and log in as `registrar` / `password123`. Use developer test students only. Create any test data with a `ZZ_TEST_` prefix and remove it afterwards. Never edit or delete `system_logs` rows. Check each item and record PASS/FAIL:

1. **Tab visibility:** an Enrolling or Submitted student's details modal has no SO Checklist tab. An Active student has the tab, and it shows the Preview banner plus `—` "Due once the student is Completed" rows.
2. **Lazy load:** opening the modal does not call `/so-checklist`. The first click on the tab calls it once, and switching tabs back and forth does not call it again.
3. **Edit Status:** for an Active student, Completed and Graduated are enabled and nothing else is disabled. Picking Completed shows the date (pre-filled today). Picking Graduated shows date + reason + the archive explanation. Saving Completed without a date shows "Enter the completion date." A future date is rejected by the server. A valid save updates the badge, the list (purple Completed badge) and the checklist (FINAL).
4. **Disabled moves:** for a Completed student, only Active, Completed and Graduated are enabled. For a Graduated student, only Completed and Graduated are enabled, and Completed asks for a reason.
5. **Audit rows:** `/logs.html` as admin shows `Changed status of … from Active to Completed (completion date …)` and the cleared/reason variants for the moves made.
6. **Edit form:** the enrollment date is editable. The completion date is disabled for Active and enabled for Completed. The Employment select sits under OJT. Saving without touching these fields keeps them (reload and compare). An enrollment date after the completion date shows the server error.
7. **Checklist content:** for a Completed test student with a TOR uploaded before the last grade lock, the TOR row shows ⚠ and the summary reads "Student requirements: Complete — 1 warning" (or "N of 9 unmet"). Screen-reader text is present (inspect the `.visually-hidden` spans).
8. **Upload hint:** on `documents.html`, stage a file as Others with the name "TOR copy". After ~300 ms "Did you mean *Transcript of Records (TOR)*? Switch" appears, and Switch changes the type and clears the name. "Form 9" shows the Form IX sentence without Switch. "History" shows nothing. Upload still works without switching.
9. **Status filter:** filtering by Completed on `registrar.html` and `student-numbers.html` returns only Completed students.
10. **Pre-export check:** a Completed student missing the TOR is flagged in the Documents export dialog (RequiredDocumentPolicy change).

Afterwards, remove every `ZZ_TEST_` document/record created and restore any test student whose status you changed (through the UI, so the changes are logged). Then stop the app and confirm port 8080 is free.

- [ ] **Step 5: Update the memory bank (newest entry at the top of each file)**

- `activeContext.md`: new "Latest Session (2026-10-03 — R4.2 SO Checklist: Implemented)" with branch `feature/so-checklist`, the commit list, test count, migration status, live-check result, open items (spec §13 assumptions, follow-up cards §12).
- `progress.md`: completed items per task; deferred items (follow-up cards); any review findings left open.
- `changeLog.md`: every file from the File Structure table, with the reason.
- `decisions.md`: the implementation outcome of the decisions already recorded on 2026-10-03 (four planning deviations, Task 9 confirmed, subagent-driven execution); add any new decision made during execution.
- `testing.md`: suite count, the new test classes, and the live-check table from Step 4.

- [ ] **Step 6: Add the conventions to `CLAUDE.md`**

Under "Key Conventions", after the "Required documents & labels" paragraph, add:

```markdown
**Student lifecycle & SO checklist:** statuses run `Enrolling → Submitted → Active → Completed → Graduated` (Completed = training + OJT done, Graduated = SO issued). Which status moves are allowed, and which need a completion date or a reason, lives only in `service/StudentStatusTransitions.java` (mirrored, not owned, by `statusRule` in `js/registrar-students.js`). The per-student SO checklist rules live only in `service/SoReadinessPolicy.java`; `SoChecklistService` feeds it from three BLOB-free queries and `GET /api/registrar/student-records/{id}/so-checklist` is read-only. "Others" label → document-type hints live only in `service/DocumentTypeSuggester.java`. `student_records.completion_date` and `employment_status` need migration `2026-10-03-so-checklist.sql`.
```

- [ ] **Step 7: Commit the docs**

```bash
git add memory-bank/activeContext.md memory-bank/progress.md memory-bank/changeLog.md \
  memory-bank/decisions.md memory-bank/testing.md CLAUDE.md
git commit -m "docs: record R4.2 SO checklist implementation, migration and live check"
```

- [ ] **Step 8: Finish the branch**

Use `superpowers:finishing-a-development-branch`. The user previously chose "push and create a PR", but ask again rather than assume. Never push to or merge into `main` directly.
