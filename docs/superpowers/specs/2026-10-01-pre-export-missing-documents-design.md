# Pre-Export Missing-Documents Check & Custom "Others" Document Names — Design

**Date:** 2026-10-01 | **Status:** Approved design (brainstormed with the user section by section), ready for plan drafting.
**Intent:** (1) Before the Registrar exports documents (single student or a whole section/batch), warn which students in that scope are missing required documents, using a dialog with a red "!" per flagged student. (2) Let the Registrar give "Others" documents a custom name so miscellaneous documents stay organized.
**Stack:** Java 25, Spring Boot 4.0.4, Spring Security 7, Spring Data JPA + `JdbcTemplate`, MySQL 8, Bootstrap 5.3, local jQuery 4 / vanilla JS. No new libraries.
**Related:** this is "Sub-project 2: Group Document Download Verification & Flagging" in `memory-bank/activeContext.md`. Builds on `docs/superpowers/specs/2026-09-27-document-folder-management-design.md` (folder tree + the four ZIP export scopes).

---

## 1. Confirmed Decisions (user-approved)

1. **Required set is tiered by student status.**
   - Every status: PSA Birth Certificate, Form 137, ID Picture (1x1 / 2x2).
   - **Graduated only:** Transcript of Records (TOR), Form IX (any one of the three), OJT Report, Certificate of TVET Program.
2. **Form IX:** any one of the three Form IX types satisfies the requirement (no per-course mapping).
3. **The warning appears only when exporting.** Clicking Export runs a live check. If nobody is missing anything the ZIP downloads immediately; otherwise a dialog lists flagged students with a red "!" and **Cancel / Export anyway**. Closing the dialog leaves nothing behind.
4. **Dialog only.** No persistent or temporary marks in the folder tree or student lists.
5. **Approach A:** a separate read-only pre-export check endpoint; the existing streaming ZIP endpoints are unchanged except for the audit text (§3.5).
6. **The export audit row records the flagged count**, computed server-side.
7. **Custom names for "Others"** are stored in a new nullable `documents.document_label` column; `document_type` stays `"Others"`.

## 2. Required-Documents Policy — `service/RequiredDocumentPolicy.java` (new)

A `final` class with static methods (same style as `CourseCodeGenerator`): no Spring, no database access.

```java
public static List<String> missing(String studentStatus, Set<String> presentDocumentTypes)
```

Returns display labels of unmet requirements in this fixed order, or an empty list when complete:

| # | Label returned | Applies to | Satisfied by (`document_type`, exact match) |
|---|---|---|---|
| 1 | `PSA Birth Certificate` | all | `PSA Birth Certificate` |
| 2 | `Form 137` | all | `Form 137` |
| 3 | `ID Picture (1x1 / 2x2)` | all | `ID Picture (1x1 / 2x2)` |
| 4 | `Transcript of Records (TOR)` | Graduated | `Transcript of Records (TOR)` |
| 5 | `Form IX (any one)` | Graduated | any of the three `Form IX - …` types |
| 6 | `OJT Report` | Graduated | `OJT Report` |
| 7 | `Certificate of TVET Program` | Graduated | `Certificate of TVET Program` |

Rules:
- "Graduated" is matched case-insensitively against `student_status`. Every other value (`Enrolling`, `Submitted`, `Active`, `null`, unknown) → rows 1–3 only.
- `Others` (labelled or not) never satisfies anything. Duplicate types don't matter (input is a set).
- **Type strings are shared, not copied:** promote the relevant entries of `DocumentService.DOCUMENT_TYPES` to `public static final String` constants (like the existing `ID_PICTURE_TYPE`, plus `OTHERS_TYPE`) and build `DOCUMENT_TYPES` and the policy from them.

## 3. Pre-Export Check — Backend

### 3.1 Endpoint
`GET /api/registrar/documents/export-check/{scope}/{key}` in `DocumentController`.

- `{scope}` ∈ `student | section | unassigned | batch` (case-insensitive) → `DocumentExportScope.STUDENT | SECTION | UNASSIGNED | BATCH`. Any other word → **400** (`IllegalArgumentException` → existing handler).
- `{key}` = student ID / section code / batch code (batch code for both `unassigned` and `batch`, as the export endpoints).
- Unknown key → **404** (`NoSuchElementException`, same as export).
- Covered by the existing `/api/registrar/**` → REGISTRAR rule in `SecurityConfig`. Read-only; **writes no audit row** (same as the folder-tree read).

### 3.2 Response DTOs (new, `dto/registrar/`)
```java
public record ExportCheckResponse(int studentsInScope, List<FlaggedStudent> flagged) { }

public record FlaggedStudent(String studentId, String studentNumber, String lastName, String firstName,
                             String studentStatus, String sectionCode, long documentCount,
                             List<String> missing) { }
```
- `flagged` holds only students with a non-empty `missing`, ordered last name, first name, student ID. Empty list = "export immediately".
- `documentCount = 0` lets the dialog say the student won't appear in the ZIP.

### 3.3 Query — `DocumentFolderRepository.findCheckRows(DocumentExportScope scope, String key)`
- Returns `List<CheckRow>` with `CheckRow(studentId, studentNumber, firstName, lastName, studentStatus, sectionCode, Integer documentId /* nullable */, String documentType /* nullable */)`.
- One parameterised `SELECT` per call, **starting from `student_records` with `LEFT JOIN documents`**, so a student with zero documents still yields one row (with `document_id` / `document_type` = `NULL`). Ordered by last name, first name, student ID.
- Scope `WHERE` clauses **identical in membership to the four existing `EXPORT_*_SQL` queries**:
  - STUDENT: `s.student_id = ?`
  - SECTION: `s.section_code = ?`
  - UNASSIGNED: `s.section_code IS NULL AND s.batch_code = ?`
  - BATCH: `LEFT JOIN sections sec ON sec.section_code = s.section_code WHERE sec.batch_code = ? OR (s.section_code IS NULL AND s.batch_code = ?)`
- No BLOB columns; no `GROUP_CONCAT` / window functions (must run identically on MySQL and the real-H2 tests). Grouping happens in Java.

### 3.4 Service — `DocumentExportService.checkMissing(DocumentExportScope scope, String key)` (new, public)
1. `requireScopeExists(scope, key)` (existing private method).
2. `findCheckRows(scope, key)`.
3. Group rows per student in query order: `documentCount` = number of non-null `documentId`s; `presentTypes` = set of non-null `documentType`s.
4. `RequiredDocumentPolicy.missing(status, presentTypes)` per student; keep only non-empty results.
5. Return `ExportCheckResponse(studentCount, flagged)`. An existing key with no students → `ExportCheckResponse(0, [])`.

### 3.5 Audit row includes the flagged count
- `PreparedExport` gains `int studentsInScope` and `int flaggedCount`; `prepareExport` calls `checkMissing` after its existing validation, **before any response headers are written** (failures still become normal JSON errors).
- `DocumentController.streamExport` audit text becomes (always, including zero):
  ```
  Requested ZIP export (batch B2026A, 142 document(s), 3 of 20 student(s) with missing documents)
  ```
- The count is always computed server-side, never taken from the browser.
- The only existing assertion on this text is `contains("Requested ZIP export")` in `DocumentControllerWebMvcTest` — unaffected.

## 4. Pre-Export Dialog — Frontend (`documents.html`, `js/registrar-documents.js`, `css/dashboard.css`)

### 4.1 Export buttons
- `buildExportButton(label, url, disabled)` → `buildExportButton(label, url, checkScope, checkKey, disabled)`. Enabled buttons become `<button type="button">` (instead of `<a href>`) carrying the export URL and check scope/key. Disabled behaviour (no documents in scope) unchanged.
- Call sites: Entire Batch (`renderBatchOverview`), Section / Unassigned (`renderStudentListView` via `exportConfig`), Student (`renderStudentDetail`).

### 4.2 Click flow
1. Disable the button, text "Checking…".
2. `GET /api/registrar/documents/export-check/{scope}/{key}`.
3. `flagged.length === 0` → `window.location.href = exportUrl` (the same streaming GET as today; nothing buffered in the browser).
4. Otherwise open `#exportCheckModal`.
5. Restore the button in all cases.

### 4.3 `#exportCheckModal` (static Bootstrap modal in `documents.html`, `modal-dialog-scrollable`)
- Title: "Missing documents".
- Summary: "**{flagged} of {studentsInScope}** students in *{scope label}* are missing required documents." (Scope label: "Section CARS-A", "Batch B2026A", "Unassigned Students of B2026A", or the student's name.)
- One row per flagged student:
  - red `!` badge (`.missing-doc-icon`, `aria-hidden="true"`) + visually-hidden text "Missing documents";
  - **Last, First** · Reference No. `{studentId}` · `{studentStatus}`;
  - missing items, comma-separated;
  - if `documentCount === 0`: muted note "No documents — won't be included in the ZIP."
- Footer: **Cancel** (secondary, receives initial focus) · **Export anyway** (primary → hide modal, then `window.location.href = exportUrl`).
- Nothing persists after close; the tree is not marked.

### 4.4 Errors (shown inside `#exportCheckModal` — the folder content panel is re-rendered too often to host an alert)
- 401 → existing `buildSessionExpiredNotice()` in the modal body; no export button.
- 404 → "This item is no longer available — refresh to continue."; no export button.
- Any other failure → "Couldn't check for missing documents." with **Retry** (re-runs the check in place) and an **Export without checking** footer button. A failed check must never prevent an export.

### 4.5 Safety & style
- All server text via the existing `el()` helper / `textContent` — never `innerHTML`.
- `.missing-doc-icon` in the page-scoped `<style>` block of `documents.html` (where the other folder-explorer styles live): small round badge, white bold "!" on `var(--bs-danger)`. No icon library (air-gapped; everything local).

## 5. Custom Names for "Others" Documents

### 5.1 Schema
- New `src/main/sql/migrations/2026-10-01-add-documents-document-label.sql`: adds `documents.document_label VARCHAR(100) NULL` **idempotently** (check `information_schema.COLUMNS` first, then `ALTER TABLE` via prepared statement, matching earlier migrations' style).
- Mirror in `src/main/sql/schema.sql` (+ header "Updated:" line), `src/main/sql/AnihanSRMS.sql`, and `src/test/resources/document-storage-h2-schema.sql`.
- Existing rows get `NULL` = plain "Others"; no backfill. Applied manually (JPA DDL is `none`).
- `model/Document.java`: `@Column(name = "document_label", length = 100) private String documentLabel;` + accessors.

### 5.2 Write rules — `DocumentService.uploadBatch` (validate-before-write, as today)
- `POST /api/registrar/documents/batch` accepts optional `documentLabels` (parallel to `files` / `documentTypes`). Absent → all `null` (old callers keep working). Present with a different length → 400.
- The controller reads it with `HttpServletRequest.getParameterValues("documentLabels")`, **not** `@RequestParam List<String>` — Spring splits a single list value on commas, which would break a one-file upload labelled e.g. "Barangay Clearance, 2026".
- Each label: trimmed; blank → `null`.
- A non-null label is allowed **only when the type is `Others`**; otherwise 400.
- Max 100 characters; control characters rejected → 400.
- A label equal (case-insensitive, after trim) to any known document type → 400 with "'{label}' is a document type — pick it from the type list instead of using Others."
- Any validation failure → nothing written (existing all-or-nothing behaviour).
- The batch upload audit row stays unchanged (`"Uploaded N document(s) for student X"` — it has never listed per-file types, so labels are not added to it).
- Out of scope: the unused single-file `POST /api/registrar/documents`, generated documents, and ID pictures stay unlabelled.

### 5.3 Read side
- `DocumentSummaryResponse` gains `String documentLabel`; the three JPQL constructor projections in `DocumentRepository` and `DocumentService.toSummary` include it.
- The `q` search in the Documents list also matches `document_label`.
- New `GET /api/registrar/documents/labels` → sorted distinct non-null labels (`DocumentRepository` query); used for combobox suggestions.
- Display text where a type is shown (Documents DataTable type column and the folder-view student document list — the View and Delete dialogs never show a type): `Others — {label}` when labelled, else the type.
- Document Type filter unchanged: "Others" returns labelled and unlabelled "Others" documents.
- ZIP entry names still come from `file_name` — export unaffected.

### 5.4 Upload UI (staged-file rows in `registrar-documents.js`)
- When a row's type is `Others`, show a `SrmsCombobox` text field "Specify document name (optional)" (max 100) under the type select, suggestions from `/labels` (fetched once when the upload modal opens).
- Changing the type away from `Others` hides and clears the field.
- `submitBatchUpload` appends `documentLabels` for every staged file (empty string when none).

## 6. Testing & Verification

**`RequiredDocumentPolicyTest`** (new, pure):
- Active + nothing → exactly the 3 intake labels, in order.
- Active + all intake → empty; completion docs never required for Enrolling / Submitted / Active.
- Graduated + nothing → all 7; each single Form IX variant satisfies "Form IX (any one)".
- "graduated" (any case) treated as Graduated; `null` / unknown → intake only.
- `Others` and duplicates don't affect the result.

**`DocumentServiceTest`** (extend): label stored trimmed; blank → null; label on non-Others → 400; >100 chars / control chars → 400; label equal to a known type (case-insensitive) → 400; `documentLabels` length mismatch → 400; omitted `documentLabels` accepted; nothing saved on any failure.

**`DocumentExportServiceTest`** (extend): `checkMissing` groups rows per student, counts documents, includes zero-document students, empty `flagged` when all complete, unknown key → `NoSuchElementException`; `prepareExport` populates `studentsInScope` / `flaggedCount`.

**`DocumentControllerWebMvcTest`** (extend): `export-check` 200 shape / 400 bad scope / 404 unknown key; export audit text contains `"of N student(s) with missing documents"`; `GET /labels`; batch upload forwards `documentLabels`.

**`DocumentStorageIntegrationTest`** (real H2, extend): for all four scopes the check covers exactly the export membership (incl. section student with mismatched own batch, unassigned matched by own batch); zero-document student flagged; `document_label` round-trips; `/labels` distinct.

**Full suite:** baseline 546 tests, must stay green.

**Live check (Playwright, real `AnihanSRMS`, after applying the migration):** complete student → immediate download; section with gaps → dialog with correct students/items, Cancel leaves no trace, Export anyway downloads; system log row shows the flagged count; "Others" upload labelled "Medical Certificate" displays as `Others — Medical Certificate` and is suggested on the next upload; "Others" filter shows labelled and unlabelled.

## 7. Out of Scope

- Per-course Form IX mapping; configurable required sets (edit the policy class to change).
- Missing-document marks in the folder tree, student lists, or other pages.
- Renaming/merging labels after upload; a separate label filter.
- Blocking exports — the check only warns.
