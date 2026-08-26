# Active Context - Anihan SRMS

## Current Phase
**Create/Edit Subject: competency_type done; subject_code is now editable/renameable on Edit — PR to main still open**

## Active Branch
`edit_subjects`

## ⚠️ ACTION REQUIRED — All Developers (as of 2026-08-26)
Two migrations landed today and the app code depends on both. Every developer with
a local `AnihanSRMS` database **must run these, in order, before pulling this
branch**:
1. `src/main/sql/migrations/2026-08-26-subjects-competency-type.sql` — adds
   `subjects.competency_type` (NOT NULL, BASIC/COMMON/CORE), relaxes
   `subjects.qualification_code` to nullable.
2. `src/main/sql/migrations/2026-08-26-subjects-code-update-cascade.sql` — adds
   `ON UPDATE CASCADE` to `classes.subject_code` and `grades.subject_code` (both
   were MySQL-default RESTRICT, which silently blocked renaming a subject's code
   the moment it had real classes/grades).

Without both, `Subject`/`SubjectResponse` will fail to load `competency_type`,
`createSubject`/`updateSubject` will throw, and renaming a subject with live
classes/grades will hit a raw FK constraint error instead of succeeding. Fresh
installs get both automatically via `schema.sql`. See `decisions.md` (2026-08-26
entries) for the full rationale.

## Latest Session (2026-08-26 PM #2 - Subject Code Made Editable/Renameable on Edit)

### Scope
User request: allow `subjectCode` to be edited in the Edit Subject modal (it was
previously `readonly`, locked deliberately on 2026-05-10 because it's the PK and
`classes`/`grades` reference it by FK). Investigated first rather than just
unlocking the HTML field — confirmed the underlying FKs had no `ON UPDATE CASCADE`,
so a naive unlock would have let the registrar type a new code and hit an opaque
500 from MySQL the moment the subject had any real classes/grades. User chose the
"full cascading rename" option over a narrower "only rename while unreferenced"
alternative (both were proposed).

### Key Changes
- **Migration `2026-08-26-subjects-code-update-cascade.sql`**: drops and
  re-adds `fk_grades_subject`/`fk_classes_subject` with `ON DELETE RESTRICT ON
  UPDATE CASCADE` (delete-blocking behavior unchanged — only the update rule
  changed). Idempotent — checks the *current* `UPDATE_RULE` via
  `information_schema.REFERENTIAL_CONSTRAINTS`, not just whether an FK exists,
  since the old FK already exists and must be replaced, not skipped.
- **`schema.sql`**: both FKs now explicitly named + `ON DELETE RESTRICT ON UPDATE
  CASCADE` for fresh installs.
- **`SubjectRepository.renameSubjectCode(oldCode, newCode)`**: a
  `@Modifying(clearAutomatically = true) @Query(UPDATE Subject SET subjectCode = ...)`
  bulk update — NOT a load-mutate-save on the entity. `subjectCode` is `@Id`;
  JPA/Hibernate has no well-defined way to change a managed entity's own identity
  via setter + `save()`. The bulk update issues a direct SQL UPDATE, which is what
  actually triggers the DB's `ON UPDATE CASCADE`.
- **`UpdateSubjectRequest`** gained `subjectCode` (`@NotBlank`, max 20).
- **`ClassManagementService.updateSubject()`**: if the submitted code differs from
  the path code, checks for a collision (`existsById`), calls
  `renameSubjectCode()`, then **reloads the entity under the new code** before
  applying the other field changes — the old in-memory `Subject` object is stale
  the instant the rename executes (its row no longer exists under the old PK).
- **Controller**: audit log now distinguishes a plain update ("Updated subject X")
  from a rename ("Updated subject X (renamed to Y)").
- **Frontend**: `#editSubjectCode` is no longer `readonly`; a form-text hint now
  reads "Renaming updates every class and grade already linked to this subject."
  Payload/validation updated to include `subjectCode`.

### Verified
- `./gradlew test` → **230 tests, 0 failures** (was 226; +4: rename succeeds and
  reloads under the new code, rename to an already-taken code is rejected, no-op
  when the code is unchanged, controller-level blank-code rejection + rename log
  message).
- **Raw-SQL cascade proof** (before touching app code): inserted a throwaway
  `classes` row and a throwaway `grades` row referencing `COOK-101`, ran
  `UPDATE subjects SET subject_code='COOK-101-RENAMED' ...` directly, confirmed
  both child rows updated automatically, then cleaned up and renamed back to
  `COOK-101` — zero leftover test data.
- **Full live HTTP smoke test** (after the app-code changes): booted the real app
  against the live Docker MySQL, logged in as `registrar`, created a real subject
  via `POST /api/registrar/subjects`, attached a real `classes` row directly in
  MySQL, renamed the subject via the real `PUT /api/registrar/subjects/{code}`
  endpoint, confirmed the `classes` row's `subject_code` updated automatically in
  live MySQL, and confirmed `system_logs` recorded
  `"Updated subject SMOKE-101 (renamed to SMOKE-101-RENAMED)"`. Test subject/class
  rows deleted afterward; the `system_logs` entries were **left in place**
  (system_logs is append-only per this project's own convention — smoke-test log
  rows are not an exception).
- `schema.sql` rebuilt fresh into a throwaway DB — matches the migrated live DB.

### Open Items
- PR to `main` (user approval required).
- No manual browser click-through of the Edit Subject rename UX yet (the toggle
  behavior from the prior session and this session's rename input are both
  API/SQL-verified but not eyeballed in an actual browser).

---

## Previous Session (2026-08-26 PM - Subjects: competency_type Backend + Frontend, Create & Edit)

### Scope
Completed the Create/Edit Subject feature revision started earlier this session
(the DB/schema step). Added a Competency Type dropdown (Basic/Common/Core) to both
the Create Subject and Edit Subject modals on `subjects.html`; selecting Core reveals
the Qualification dropdown, selecting Basic/Common hides and clears it. Backend now
enforces the actual business rule (Core requires a qualification; Basic/Common do
not) instead of unconditionally requiring one. Both Create and Edit were updated,
per user decision — competency_type is editable on Edit, not locked after creation.

### Key Changes
- **`Subject.java`**: added `competencyType` field/accessors; `qualification`
  `@JoinColumn` no longer `nullable = false`.
- **`CreateSubjectRequest`/`UpdateSubjectRequest`**: added
  `@NotBlank @Pattern(regexp = "BASIC|COMMON|CORE") competencyType`; dropped
  `@NotNull` from `qualificationCode` (cross-field CORE-requires-qualification rule
  can't be expressed cleanly in Bean Validation, so it's enforced in the service
  layer instead, matching this codebase's existing pattern for cross-field rules
  like `deleteSection`'s FK pre-check).
- **`SubjectResponse`**: added `competencyType` and `qualificationCode` (the latter
  so the Edit modal can pre-select the qualification dropdown by code instead of
  the old fragile name-matching).
- **`ClassManagementService`**: new private `resolveQualificationForCompetencyType()`
  — CORE requires and looks up a qualification; BASIC/COMMON silently ignore any
  submitted `qualificationCode` and save with `qualification = null`. Used by both
  `createSubject()` and `updateSubject()`.
- **`subjects.html`**: Competency Type `<select>` added to both modals; Qualification
  field wrapped in a `d-none`-toggleable group; new "Competency Type" DataTable column.
  Cache-buster `?v=2` → `?v=3`.
- **`registrar-subjects.js`**: `setupCompetencyTypeToggle()` shared by Create + Edit
  — shows/hides + clears the qualification field on competency-type change;
  `formatCompetencyType()` for table display; payload/validation on both Save buttons
  now sends `competencyType` and only requires/sends `qualificationCode` when Core.

### Verified
- `./gradlew compileJava compileTestJava` → BUILD SUCCESSFUL.
- `./gradlew test` → **226 tests, 0 failures, 0 errors** (was 217; +9 new: 6 service
  tests — Core-without-qualification rejected, Basic/Common allowed without one,
  Basic silently ignores a submitted qualificationCode, Core-to-Basic conversion
  clears qualification — and 3 controller tests — missing/invalid competencyType
  rejected by Bean Validation, Basic subject with no qualificationCode accepted).
- **Live-DB compatibility (authoritative check, since the Gradle suite runs on H2):**
  booted the app with `--spring.jpa.hibernate.ddl-auto=validate` against the live
  Docker MySQL → `Started SpringbootApplication` with zero schema-validation errors,
  confirming `Subject`'s new field and relaxed FK match the migrated live table.
  Process stopped afterward (port 8080 confirmed free).

### Open Items
- PR to `main` (user approval required, per branch rule).
- Same pre-existing gap noted in the prior session: Food and Beverage Services NC II
  still has no seeded `qualifications` row.
- No manual browser smoke test yet — Create/Edit Subject flows with the new toggle
  behavior should be clicked through in-browser before merging.

---

## Previous Session (2026-08-26 AM - Subjects: competency_type Schema/DB Change)

### Scope
Step 1 of the Create/Edit Subject feature revision (adding a Basic/Common/Core
competency classification to subjects, matching the actual TESDA Form IX/TOR
document structure). This session covers only the database layer — the JPA
entity, DTOs, service validation, and frontend (subjects.html /
registrar-subjects.js) are deliberately deferred to a follow-up session.

### What Changed
- **Live Docker DB (`AnihanSRMS`):** `subjects.qualification_code` relaxed
  `NOT NULL` → `NULL`; new `subjects.competency_type VARCHAR(15) NOT NULL` added
  and backfilled to `'CORE'` for all 6 existing seeded subjects (all pre-existing
  subjects are genuinely Core/qualification-specific). Applied via
  `src/main/sql/migrations/2026-08-26-subjects-competency-type.sql` (idempotent —
  guarded via `information_schema`, matching the project's established migration
  idiom; verified safe to re-run).
- **`schema.sql`** updated to match for fresh installs: `subjects` CREATE TABLE
  block now declares `qualification_code INT NULL` and
  `competency_type VARCHAR(15) NOT NULL`; the 6-row seed INSERT now supplies
  `'CORE'` for every row. Verified by building schema.sql fresh into a throwaway
  `schema_check` database — output matched the migrated live DB exactly, then
  dropped.
- **Backup taken first:** `src/main/sql/backup-2026-08-26-pre-competency-type.sql`.

### Why
See `decisions.md` (2026-08-26). Short version: `competency_type` is a property of
the *subject*, not the qualification (Basic/Common subjects are shared across all
qualifications; only Core subjects are qualification-specific) — so it's a plain
`VARCHAR` column on `subjects` (same pattern as `role`/`student_status`), not a new
lookup table, and `qualification_code` had to become nullable rather than pointing
at a sentinel "No Qualification" row.

### Verified
- Migration applied to live DB, re-run twice → identical result (idempotent).
- Fresh `schema.sql` build into a throwaway DB → matches live DB exactly.
- Confirmed via code search (not assumption) that `SubjectResponse.java` and
  `TrainerService.java` already null-check `Subject.getQualification()`, so this
  DB-layer change doesn't break anything downstream yet — but `ClassManagementService
  .createSubject()/.updateSubject()` **will** break the moment a null
  qualificationCode is submitted, since they currently call
  `qualificationRepository.findById(request.qualificationCode()).orElseThrow(...)`
  unconditionally. That's the first thing the next session must fix.

### Open Items (next session)
1. `model/Subject.java` — add `competencyType` field/accessors; `@JoinColumn`
   `nullable = true` on `qualification`.
2. `dto/registrar/CreateSubjectRequest.java` + `UpdateSubjectRequest.java` — add
   validated `competencyType`; drop `@NotNull` on `qualificationCode`.
3. `dto/registrar/SubjectResponse.java` — add `competencyType` to output.
4. `service/ClassManagementService.java` — `createSubject()`/`updateSubject()`:
   business rule — CORE requires `qualificationCode`; BASIC/COMMON optional/ignored.
   Currently unconditional lookup will NPE/throw on null.
5. `static/subjects.html` + `static/js/registrar-subjects.js` — Create/Edit Subject
   modals: competency-type dropdown, qualification dropdown conditional on it,
   DataTable column for competency type.
6. Tests: new cases in `ClassManagementSubjectServiceTest.java` and
   `ClassManagementSubjectControllerWebMvcTest.java` (CORE without qualification →
   rejected; BASIC/COMMON without qualification → accepted).
7. Known pre-existing gap, unrelated but adjacent: Food and Beverage Services NC II
   has no seeded `qualifications` row despite having document templates and a
   `CORE_FBS` curriculum section — will need one eventually.

---

## Previous Session (2026-07-14 PM #2 - Document Management Polish: Print, Logo, DOCX, Delete/Edit)

### Scope
Six approved tasks on documents.html + generate-document.html: (1) print formatting
(no grey backdrop/chrome, 1-page templates print as 1 page); (2) school logo embedded
in the generated header; (3) auto PDF/download filenames "{ShortType}-{Last} {First}";
(4) Actions column fit; (5) generated HTML downloads as editable Word .docx;
(6) DELETE endpoint + type-to-confirm modal, view-modal Edit (re-save in place),
generate-page Cancel + post-save redirect.

### Key Decisions
- **DOCX via OOXML altChunk, server-side** (`HtmlDocxConverter`, pure `java.util.zip`,
  no new dependency, air-gap safe): a minimal docx package embeds the stored HTML as an
  altChunk that Word converts to editable content on open. Chosen over vendoring
  html-docx-js (same fidelity, +70KB unaudited JS). Uploaded pdf/docx/xlsx download
  unchanged; only `text/html` documents convert. Friendly filename via
  `TYPE_SHORT_NAMES` map (mirrors curriculum-templates.js shortName).
- **Edit re-saves update the existing row in place** (`saveGenerated` 5-arg overload with
  `documentId`; `GenerateDocumentRequest.documentId` nullable). Guards: ownership
  (studentId match) AND fileType must be text/html (code review caught that an API call
  could otherwise overwrite an uploaded PSA scan with HTML).
- **Print fix root causes**: grey backdrop = body background + sheet box-shadow not reset
  in `@media print`; page spill = Chromium cannot fragment flex items — body stays
  `display:flex` in print, so sheets jumped to page 2. Print CSS now forces
  `body { display:block; min-height:0 }` + white bg + tighter metrics + row-level
  `page-break-inside: avoid`. **Form IX variants = exactly 1 page; TOR = 2 dense pages**
  (57 subject rows ≈ 1718px vs ~1054px/page capacity — physically cannot fit 1 page).
- Logo shipped as `static/js/anihan-logo.js` (`window.AnihanLogo.DATA_URI`, base64 of
  images/logo.png, 17KB) so saved documents stay self-contained; generator degrades to
  logo-less header if the script fails to load.
- Print filename: `document.title` swapped to sanitized "{shortName}-{Last} {First}"
  around `window.print()`, restored on afterprint + 2s fallback.

### New/Changed Endpoints
`DELETE /api/registrar/documents/{id}` (204, logs "Deleted document…", 404 JSON when
missing) · `GET /{id}/download` now converts text/html→docx with friendly filename
(logs delivered name) · `POST /generate` accepts optional `documentId` → update in
place, logs "Updated generated document…".

### Verified
- `./gradlew test` → **217 tests, 0 failures** (was 200; +17).
- Headless-Edge Playwright E2E **30/30**: real `page.pdf()` (Form IX ×4 = 1 page,
  TOR = 2; no chrome in PDF), logo data-URI, title swap/restore, actions contained
  @1400/@992px, docx download (zip + altChunk + filename), edit re-arms contenteditable
  + updates in place (no duplicate row), cancel + post-save redirects, delete
  type-to-confirm removes row. `system_logs` rows confirmed in live MySQL
  (Generated/Downloaded/Updated/Deleted).
- E2E harness gotcha (documented for next time): a sticky
  `emulateMedia({media:'screen'})` overrides `page.pdf()`'s print media — clear with
  `media: null` or PDFs render with screen CSS.
- /code-review: 3 findings (uploaded-file overwrite guard, blank-name docx filename
  fallback, hard logo dereference) — all fixed + tested.

### Open Items
- PR to `main` (user approval required).
- Fidelity note: Word's HTML import linearizes flex rows (`doc-field-row`) — docx opens
  editable but not pixel-identical to print; tables/underlines survive.

---

## Previous Session (2026-07-14 PM - Student Picker Dropdown + "Failed to load" Diagnosis + Record Cleanup)

### Scope
Three user requests on the Generate Document page and data: (1) replace the native
`<datalist>` student list (ugly floating browser list) with a proper searchable dropdown;
(2) diagnose "Failed to load student data" on Load; (3) delete duplicate / incomplete
student records from the live DB.

### 1. "Failed to load student data" - root cause: stale server build, not a bug
`GET /api/registrar/documents/generate-data/SR20260016` returns **HTTP 200 with full
auto-fill payload** on the current build (verified via curl + headless browser). The
fallback alert text only appears when the error response has no JSON `message` - i.e. a
404 from a server still running a **pre-PR-#49 build** without the document endpoints.
Requirements to generate: the student ID must exist in `student_records`; everything else
(course, section, grades, TESDA, OJT, parents) is optional and simply left blank.
**Operator note: restart `./gradlew bootRun` after pulling document-management.**
Hardened `ajaxErrorMessage()` in the JS so 0/401/404/other statuses now produce
distinguishable messages instead of one generic string.

### 2. Searchable student dropdown (combobox)
- `generate-document.html`: replaced `<input list>` + `<datalist>` with a
  `#studentPicker` wrapper — text input (`role="combobox"`) + Bootstrap `.dropdown-menu`
  (`#studentPickerMenu`, 300px scroll). JS cache-buster `?v=1` → `?v=2`.
- `registrar-generate-document.js`: `loadStudentsDatalist()` → `setupStudentPicker()`.
  Opens on focus, filters as you type (matches ID + last/first name, case-insensitive),
  ArrowUp/Down + Enter keyboard nav, Escape/Tab/outside-click closes, mouse select.
  Shows "Loading students…" until the AJAX list arrives and re-renders when it does
  (race found by headless-browser test: menu rendered empty if focused before load).
- `resolveStudentId()`: exact ID match wins; else a query matching exactly one student
  resolves to it; else friendly "No student matches" alert (no wasted request).

### 3. Student record cleanup (live DB)
Backup taken to scratchpad first. Deleted via the app's own
`DELETE /api/registrar/student-records/{recordId}` (cascade-safe + `system_logs` rows):
- SR20260009 Wong, Angelica — duplicate of SR20260008 (same name/initial), all-NULL Enrolling stub
- SR20260010 Avellaneda, SR20260011 "Mark Mark", SR20260017 "test125" — abandoned all-NULL Enrolling stubs, zero child rows
**Left in place (flagged, user to decide):** SR20260005 (dwd, wdw), SR20260007 (dwadwa,
dwadad — has grade data), SR20260013 (fff, fff) — fake-name test fixtures but Active with
sections. 10 records remain.

### Verified
- Headless Edge (playwright-core) E2E: 10/10 checks pass — login → picker opens on focus
  (10 students) → filter "lipata" → keyboard select → TOR renders auto-filled ("Lipata")
  → mouse select → unknown-text friendly error.
- `./gradlew test` → **200 tests, 0 failures, 0 errors** (unchanged baseline; frontend-only).

### Open Items
- PR to `main` (user approval required).
- User decision: delete the 3 remaining fake-name Active test records?

---

## Previous Session (2026-07-14 - Live DB vs schema.sql Comparison and Sync)

### Scope
Compare the live AnihanSRMS MySQL database against src/main/sql/schema.sql, find any
discrepancies, and update the live DB so it works with the latest project schema.
User explicitly kept work on main (DB-only, no branch switch).

### Method
- Backed up the live DB first -> src/main/sql/backup-2026-07-14.sql (58 KB).
- Built a throwaway schema_check DB from schema.sql (stripped its hard-coded
  CREATE DATABASE / USE AnihanSRMS so it did not redirect into the live DB), dumped
  --no-data structures of both, normalized (dropped AUTO_INCREMENT counters + comments),
  and diffed.

### Findings - ZERO functional drift
Diff showed only cosmetic differences, all verified non-functional:
1. grades columns appear in a different physical ORDER in the dump. Sorted
   column-by-column, live and schema.sql are byte-identical (every name, type,
   nullability, default matches). Column order is irrelevant to SQL and to Hibernate.
2. grades class FK: live name fk_grades_class vs schema.sql auto-name grades_ibfk_3 -
   same relationship (class_id -> classes ON DELETE SET NULL), name only.
3. users unique key: live uq_username vs schema.sql username - same UNIQUE, name only.
- Data: courses holds all 3 (CARS, BPRO, FSERV). Live is the real working DB (14 students,
  6 classes, 264 log rows), so schema.sql's fresh-install seed blocks were NOT re-applied.

### Verification
- ddl-auto=validate boot against live MySQL -> PASS (Started in 11.256s, all 19 entities
  validated, zero HHH schema-validation errors). Authoritative check; Gradle suite runs on
  H2 and cannot catch live drift.
- FK integrity sweep -> 0 orphans (grades->classes, class_enrollments->classes,
  subjects->users trainer, classes->users trainer).

### Outcome
No update to the live database was required - it already matches schema.sql. Only
filesystem change: the new backup file (untracked).

## Latest Session (July 9, 2026 — Document Management R3.1–R3.7 + Template Generation)

### Scope
Implemented Jira AGILE-75…AGILE-81 (R3.1 Upload, R3.2 Type, R3.3 Name, R3.4 View,
R3.5 Search, R3.6 Filter, R3.7 Download) plus auto-filled, editable, print-ready
generation of the 4 templates in `document-templates/` (TOR + Form IX ×3 with
Candidate-for-Graduation / Permanent-Record variants). Plan:
`docs/superpowers/plans/2026-07-09-document-management-r3.md`.

### Key Decisions
- Uses the **existing `documents` table + `Document` entity** — no DB migration.
- Listing query is a JPQL constructor-expression projection that **never selects
  `content_data`** (LONGBLOB stays out of memory); explicit LEFT JOINs on batch/section
  so students without batch/section still appear when those filters are null.
- Generated documents are **self-contained HTML** (`text/html`, print CSS inlined)
  saved into `documents` so they flow through list/view/search/download immediately.
  Curriculum (~50 subjects with fixed hours/units) lives in
  `static/js/curriculum-templates.js`, transcribed from the PDFs — deliberately NOT
  seeded into `subjects` (would entangle class management; `subjects.units` is INT).
  Grades merge in by `subject_code`.
- The rendered document itself is the fillable form: every blank is a
  `contenteditable` span, pre-filled from `GET /api/registrar/documents/generate-data/{studentId}`.
- `X-Frame-Options` changed DENY → **SAMEORIGIN** (SecurityConfig) — required for the
  View modal's same-origin iframe preview; found via live smoke test.
- Upload whitelist: pdf/docx/xlsx, 10MB cap; view=inline only for PDF/HTML (docx/xlsx
  are download-only in the UI).

### New Endpoints (`/api/registrar/documents`)
`GET` list (q/type/batchCode/sectionCode) · `GET /types` · `POST` multipart upload ·
`GET /{id}/download` (logged) · `GET /{id}/view` (inline, not logged) ·
`GET /generate-data/{studentId}` · `POST /generate` (logged). Upload/generate logged too.

### Verified
- `./gradlew test` → **200 tests, 0 failures** (was 176; +24 new).
- Live smoke test against running app + real MySQL: login → types → upload smoke.pdf →
  list/search/filter (incl. 0-rows on wrong section) → download (bytes intact,
  attachment) → view (inline, SAMEORIGIN) → generate-save (.html appended) →
  `system_logs` rows for upload/download/generate confirmed. Smoke rows deleted after.

### Open Items
- Browser smoke test: documents.html flows + generate-document.html print fidelity
  side-by-side with the sample PDFs.
- PR to `main` (user approval required).
- Follow-up ticket: R3.8–R3.11 (group download, encode, group encode, incomplete-docs
  warning) remain unimplemented; curriculum seeding into `subjects` deferred.

---

## Previous Session (July 9, 2026 — Live DB vs SQL Files Comparison & Sync)

### What Was Checked
Compared the live `AnihanSRMS` MySQL database against the newest SQL sources
(`src/main/sql/schema.sql`, dated 2026-05-19, and all 6 files in `src/main/sql/migrations/`).

### Findings
1. **Structure: zero drift.** A `mysqldump --no-data` of the live DB matches `schema.sql`
   table-for-table — all 19 tables, every column type, nullability, index, and foreign key.
   Migrations `2026-05-05`, `2026-05-09` (×2), `2026-05-19`, and `2026-05-20` were all
   already reflected in the live schema.
2. **Data gap (the only discrepancy): `2026-05-10-seed-courses-and-batch.sql` had not been
   applied.** Live `courses` held only `CARS`; the migration seeds `BPRO` and `FSERV` too.
   Batch `B2026A` was already present.

### Action Taken
- Backed up the live DB before any write.
- Applied `2026-05-10-seed-courses-and-batch.sql` (uses `INSERT IGNORE`, so re-running is safe).
- `courses` now holds `CARS`, `BPRO`, `FSERV`. No existing row was modified; `batches` unchanged.
- Re-dumped the structure and diffed against the pre-migration dump: **identical** (data-only change).

### Compatibility Verification
| Check | Result |
|-------|--------|
| Hibernate `ddl-auto=validate` booted against **live MySQL** | **PASS** — app started in 9.0s; all 19 entities validated against live tables (column names, types, nullability) |
| `./gradlew test` (176 tests) | PASS — 0 failures, 0 errors |
| Orphaned FK sweep (grades, class_enrollments, sections, subjects) | 0 orphans |
| `classes.trainer_id` all point at `ROLE_TRAINER` users | Yes |
| Grades within the `[1.0, 5.0]` range enforced by `TrainerGradeService` | Yes |
| `Active` students all have a `section_code` | Yes |

**Note on test coverage:** the Gradle suite runs against an in-memory H2 database
(`ddl-auto=create-drop`), so it does **not** validate entities against live MySQL.
The `ddl-auto=validate` boot above is the check that actually proves live-DB compatibility.

### Live Data State
`courses` 3 · `batches` 3 · `sections` 3 · `qualifications` 2 · `subjects` 6 · `users` 3 ·
`student_records` 5 · `classes` 1 · `class_enrollments` 1 · `grades` 1 · `system_logs` 10 ·
(`parents`, `documents`, `student_education`, `student_uploads` all empty)

### Follow-up (same session) — Grades-Restructure Migration Made Real
`2026-05-19-grades-restructure.sql` was a no-op verification script (its `ALTER`s were
commented out, because the spec's `ADD COLUMN IF NOT EXISTS` is MariaDB/Postgres syntax and
is invalid in MySQL 8). It has been rewritten to actually perform the restructure, using the
guarded `information_schema` + `PREPARE` pattern already proven in the 2026-05-20 migration.

It now: adds `class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at`; relaxes
`final_grade` / `hours_studied` / `remarks` to NULL; adds the `fk_grades_class` FK and the
`uq_grade_student_class` unique key; and aborts up front (statement 1, before any change) if
the `classes` FK target is missing.

**Second bug found while testing the fix:** the FK guards keyed off the *constraint name*
(`fk_grades_class`). A database built from `schema.sql` carries that same relationship under
MySQL's auto-generated name `grades_ibfk_3`, so the name-only check saw "no FK" and added a
**duplicate** foreign key on every re-run — i.e. the migration was not idempotent. Confirmed
by running it against the live DB (it did create a duplicate; reverted immediately). Both this
file and `2026-05-20-sync-and-clear-students.sql` now match on
`KEY_COLUMN_USAGE (COLUMN_NAME + REFERENCED_TABLE_NAME)` instead of the constraint name.
The same fix was applied to the `subjects.trainer_id -> users` guard.

**Verified on three paths:**
- Legacy pre-restructure `grades` table → migration applies all columns, constraints, and
  nullability changes correctly.
- Already-migrated DB (live) → two consecutive re-runs leave the structure byte-identical;
  exactly 3 FKs, no duplicate; the existing grade row is untouched.
- `classes` missing → aborts with `ERROR 1146 ... ABORT_classes_missing_run_2026_05_09_migration_first`
  and leaves `grades` completely unmodified (no half-apply).
- Hibernate `ddl-auto=validate` against live MySQL → still PASS.

Note: `SIGNAL` cannot be used inside the prepared-statement protocol, so the precondition
abort intentionally selects from a non-existent table whose *name* is the operator instruction.

### Open Items
- None from this session. Changes are uncommitted on `main`.

---

## Previous Session (May 21, 2026 — Bugfix Audit Remediation)

### Items Completed
All 10 tasks from `2026-05-21-bugfix-audit-remediation.md` plan executed. Test suite: **176 tests, 0 failures, 0 errors** (was 166; 10 new tests added).

| Task | Description | Status |
|------|-------------|--------|
| H1 | `deleteRecord()` now calls `deleteClassEnrollmentsByStudentId` before `deleteById` | Done |
| H2 | Student ID locked `readonly` in HTML; server-side rejects changes in `updateRecord` | Done |
| M2+M3+M4 | `startOrResume` handles namesake list properly; `load` guards non-Enrolling; `generateStudentId` uses MAX+1 | Done |
| M5+M6 | Grade range [1.0–5.0] validation in `saveGrades`; finalGrade always recomputed | Done |
| L1 | `@Column` lengths/nullable fixed in 5 entities (StudentUpload, StudentEducation, StudentSchoolYear, StudentTesdaQualification, StudentOjt) | Done |
| L2 | `middleName` `nullable=false` removed from `StudentRecord`; `@NotBlank` removed from `StudentRecordUpdateRequest` | Done |
| L3 | `@EnableMethodSecurity` added to `SecurityConfig` | Done |
| L5 | Default email in `AdminService.createUser` now derives from username | Done |
| L4 | CLAUDE.md CSRF statement corrected | Done |
| L6 | `StorageService.store()` now validates studentId against `[A-Za-z0-9_-]+` whitelist | Done |

### Open Items
- Commit all changes to `main`

## Previous Session (May 21, 2026 — Database Schema Sync & Grades Restructure)

### Root Causes / Needs Identified
1. **DB Column & Constraint Mismatches:** The live MySQL database had multiple column width and type mismatches compared to `schema.sql` (e.g. `batches.batch_year` as `smallint` instead of `year`, `student_school_years` string fields as `varchar(10)` instead of `varchar(20)`, etc.).
2. **Grades Table Restructure Not Applied:** The live MySQL `grades` table was in its old structure, missing critical trainer grading columns (`class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at`), unique constraints, and foreign key relations.
3. **Empty Data State:** The database was missing seeded student records and class-scoped lookups.

### Items Completed
1. **Database Backup:** Took a full SQL dump of the live database (`AnihanSRMS`) to `src/main/sql/backup.sql`.
2. **Fresh Schema Installation:** Dropped and recreated `AnihanSRMS` and successfully imported the canonical `src/main/sql/schema.sql` (which incorporates the full restructured grades schema, all tables, and seed lookup/users data).
3. **Restructure Verification:** Ran the query portion of `src/main/sql/migrations/2026-05-19-grades-restructure.sql` to confirm that all restructured grades table columns (`class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at`), nullable column properties, foreign key links, and unique constraints are fully in place.
4. **Student Records Seeding:** Verified that the 5 sample student records and lookup data are fully seeded.
5. **Testing Suite Validation:** Ran `./gradlew test` and verified that the entire suite of 166 tests passed successfully.

### Verified via Database
```
Field            Type          Null   Key   Default  Extra
grade_id         int           NO     PRI   NULL     auto_increment
student_id       varchar(20)   NO     MUL   NULL     
subject_code     varchar(20)   NO     MUL   NULL     
class_id         int           YES    MUL   NULL     
midterm_grade    decimal(5,2)  YES          NULL     
finals_grade     decimal(5,2)  YES          NULL     
locked           tinyint(1)    NO           0        
locked_at        datetime      YES          NULL     
final_grade      decimal(5,2)  YES          NULL     
re_exam_grade    decimal(5,2)  YES          NULL     
hours_studied    decimal(5,2)  YES          NULL     
remarks          varchar(255)  YES          NULL     
```

## Previous Session (May 19, 2026 — Fix Save Grades Error + Button Styling)

### Root Causes Found
1. **`Grade.java` JoinColumn mapping** — `@JoinColumn(name = "student_id")` on the `StudentRecord` relationship was mapping to `StudentRecord.recordId` (the `@Id` auto-increment integer) instead of `StudentRecord.studentId` (the business key varchar). This caused `SQLIntegrityConstraintViolationException` on every INSERT because Hibernate wrote the integer record_id into the varchar student_id FK column.
2. **`TrainerGradeController` empty error responses** — All catch blocks returned `ResponseEntity.badRequest().build()` with NO JSON body. The JS `xhr.responseJSON.message` then failed silently because `responseJSON` was `null`.
3. **Button CSS classes** — `btn-surface-success` (Lock Grades) and `btn-save` (Save Grades) did not exist in `dashboard.css`. Only `.edit-account-modal .btn-save` was scoped to the account modal, not available globally.
4. **JS sent all students** — `collectGradeUpdates()` included every table row (even empty ones), causing unnecessary save attempts for students with all-null fields.

### Items Completed
1. **`Grade.java`** — Added `referencedColumnName = "student_id"` to `@JoinColumn` to correctly join on the varchar business key.
2. **`TrainerGradeController.java`** — All endpoints now return `Map.of("message", e.getMessage())` in error responses. Added SLF4J logging. Removed `@Valid` from save endpoint (not needed for manual grade input). Changed return types to `ResponseEntity<?>`.
3. **`trainer-classes.html`** — Changed `btn-surface-success` → `btn-surface-secondary`, `btn-save` → `btn-surface`. Bumped JS cache-buster `?v=3` → `?v=4`.
4. **`trainer-classes.js`** — `collectGradeUpdates()` now only includes rows with at least one filled grade field. `saveGrades()` shows warning when no data entered. After successful save, reloads grade data in modal instead of closing it.
5. **DB cleanup** — Cleared stale failed INSERT artifacts from `grades` table, reset AUTO_INCREMENT.
6. **Browser smoke test: PASSED** — Save → Lock → Unlock full flow verified. Grades persist in DB with correct FK values and computed final_grade.

### Verified via Database
```
grade_id=1, student_id='SR20260007', subject_code='BPP-102', midterm_grade=2.00, finals_grade=3.00, final_grade=2.60, class_id=1, locked=0
```

### Open Items
- Commit on `feature/trainer-grade-input`.
- PR to main (user approval required).

## Previous Session (May 18, 2026 — Trainer Subject & Class Views)

### Items Completed
1. **`SchoolClassRepository`** extended — `findByTrainerUserId(Integer)` and `findByTrainerUserIdAndSubjectSubjectCode(Integer, String)`.
2. **4 new DTOs** in `dto/trainer/` — `TrainerSubjectResponse`, `TrainerSubjectStudentResponse`, `TrainerClassResponse`, `TrainerClassStudentResponse`.
3. **`TrainerService`** created — `resolveCurrentTrainerId()`, `getMyAssignedSubjects()`, `getStudentsForSubject()`, `getMyClasses()`, `getStudentsForClass()`.
4. **`TrainerController`** created — 4 GET endpoints under `/api/trainer/`.
5. **`TrainerServiceTest`** — 12 Mockito tests. **`TrainerControllerWebMvcTest`** — 9 WebMvc tests (RBAC + 401/403/400 edge cases).
6. **`SecurityConfig.java`** — trainer matcher extended to include `/trainer-subjects.html` and `/trainer-classes.html`.
7. **`trainer.html`** — upgraded to full dashboard pattern: `navbar-expand-lg` with 3 nav links, welcome hero, quick-link cards, jQuery import.
8. **`trainer-subjects.html`** — new page: subjects DataTable + inline student roster panel.
9. **`trainer-subjects.js`** — subjects DataTable with click-to-load roster; `GET /api/trainer/subjects` + `GET /api/trainer/subjects/{code}/students`.
10. **`trainer-classes.html`** — new page: classes DataTable + inline student roster panel.
11. **`trainer-classes.js`** — classes DataTable with click-to-load roster; `GET /api/trainer/classes` + `GET /api/trainer/classes/{classId}/students`.
12. **Full suite: 156 tests, 0 failures, 0 errors.**

### Open Items
- Manual browser smoke test: log in as `trainer`, verify My Subjects and My Classes pages load, click a row to expand student roster.
- PR to main (user approval required).

## Previous Session (May 15, 2026 — Section CRUD + Bulk Class Enrollment)

### Items Completed
1. **6 new DTOs** — `UpdateSectionRequest`, `SectionStudentResponse`, `EligibleSectionStudentResponse`, `AssignStudentsToSectionRequest`, `SectionAssignmentResultResponse`, `BulkEnrollSectionResponse`.
2. **`StudentRecordRepository`** extended — 5 new finder methods for section filtering (null section, status, batch, course combinations).
3. **`ClassEnrollmentRepository`** extended — `deleteByStudentAndSectionCode` JPQL bulk-delete query.
4. **`ClassManagementService`** extended — 6 new methods: `updateSection`, `getStudentsInSection`, `getEligibleStudentsForSection`, `assignStudentsToSection`, `removeStudentFromSection`, `bulkEnrollSectionIntoClass`.
5. **`ClassManagementController`** extended — 6 new endpoints: `PUT /sections/{code}`, `GET /sections/eligible-students`, `GET /sections/{code}/students`, `POST /sections/{code}/students`, `DELETE /sections/{code}/students/{studentId}`, `POST /classes/{classId}/enroll-section`.
6. **`ClassManagementSectionServiceTest`** — 13 Mockito tests for all 6 service methods.
7. **`ClassManagementSectionControllerWebMvcTest`** — 7 WebMvc tests. Full suite: **135 tests, 0 failures**.
8. **`sections.html`** — added `#editSectionModal` and `#manageSectionModal` (tabbed: Current Students + Add Students with batch/course filters); cache-buster `?v=3`.
9. **`registrar-sections.js`** — rewritten Actions column (Edit / Manage Students / Delete); added `setupEditSection()`, `setupManageStudents()`, `refreshCurrentStudents()`, `refreshEligibleStudents()`, `loadFilterDropdowns()`.
10. **`classes.html`** — added Bulk Enrollment section (`#enrollWholeSectionBtn` + `#enrollSectionAlert`) in `#enrollStudentModal`; cache-buster `?v=3`.
11. **`registrar-classes.js`** — wired `#enrollWholeSectionBtn` in `setupEnrollment()`; `openEnrollmentModal()` now clears `#enrollSectionAlert` on re-open.

### Verified
- `./gradlew test` → **BUILD SUCCESSFUL — 135 tests, 0 failures, 0 errors**.
- 3 commits on `feature/section-class-enrollment`.

### Open Items
- Manual browser smoke test: sections page Edit + Manage Students flows, classes page Enroll Whole Section button.
- PR to main (user approval required).

## Previous Session (May 10, 2026 — Navbar Sync on student-records.html)

### Items Completed
1. **`student-records.html` navbar updated** — added `Classes` and `Sections` `<li>` entries to match the canonical 4-link registrar nav from `registrar.html`. Previous state had only Home + Subjects (a stale 2-link pattern from before the May 9 registrar-pages rollout).
2. Navbar markup style follows the existing pattern: `nav-link admin-nav-link` classes, no `active` modifier (the edit page is a subpage of Home, not a top-level nav target).

### Verified
- Visual diff against `registrar.html` confirms identical link order, hrefs, and Bootstrap classes.

### Open Items
- Manual browser smoke test on `/student-records.html?id={recordId}` — confirm all 4 links navigate correctly and the collapsible menu still works at mobile widths.

---

## Previous Session (May 10, 2026 — Edit Class Trainer: AGILE-93 / AGILE-95)

### Items Completed
1. **UpdateClassTrainerRequest DTO** created — single nullable `Integer trainerId`; mirrors `AssignTrainerRequest`.
2. **ClassManagementService** extended — `updateClassTrainer(Integer classId, UpdateClassTrainerRequest)` added. Validates trainer role + enabled (same as `assignTrainer`). Returns `ClassResponse` with live enrolled count.
3. **ClassManagementController** extended — `PUT /api/registrar/classes/{classId}/trainer`. Writes `system_logs` row: "Assigned trainer X to class #N" or "Unassigned trainer from class #N".
4. **classes.html** updated — `#editClassModal` inserted between Create Class and Enroll Student modals. Read-only Section/Subject/Semester display + trainer `<select>` + inline alert. Cache-buster `?v=2`.
5. **registrar-classes.js** updated — Actions column now emits `Edit Trainer` + `Manage Students` buttons. `setupEditClass()` + `openEditClassModal()` added. `loadTrainersDropdown()` now returns jQuery deferred for `.done()` chaining. `currentEditClassData` module state added.
6. **Tests** — `ClassManagementServiceTest` (6 tests) + `ClassManagementControllerWebMvcTest` (4 tests). Full suite: **115 tests, 0 failures, 0 errors**.

### Open Items / Deferred
- Manual browser smoke test: open `/classes.html`, click Edit Trainer, change trainer, verify row updates; unassign and verify "Unassigned" italic; check `/logs.html` for audit rows.
- Jira: transition AGILE-93, AGILE-95 to Done after PR merges (user will handle).
- Note for follow-up: `createClass` still missing the `enabled` trainer-account check (present in `assignTrainer` and new `updateClassTrainer`).

---

## Previous Session (May 10, 2026 — Subjects CRUD: Create / Edit / Delete)

### Items Completed
1. **QualificationRepository** created — `JpaRepository<Qualification, Integer>`.
2. **QualificationResponse DTO** created — used by `GET /api/registrar/qualifications` dropdown endpoint.
3. **CreateSubjectRequest + UpdateSubjectRequest DTOs** created — Bean Validation on all fields; subject code read-only on update.
4. **SchoolClassRepository** extended — added `existsBySubjectSubjectCode(String)` for FK pre-check.
5. **SubjectRepository** extended — added `countGradesBySubjectCode` native query against `grades` table.
6. **ClassManagementService** extended — `QualificationRepository` injected; `getAllQualifications()`, `createSubject()`, `updateSubject()`, `deleteSubject()` implemented. Delete has double FK pre-check (classes + grades).
7. **ClassManagementController** extended — `GET /qualifications`, `POST /subjects`, `PUT /subjects/{code}`, `DELETE /subjects/{code}`. Every state-changing call writes a `system_logs` row.
8. **subjects.html** updated — "Create Subject" button in page header; three new Bootstrap modals (`#createSubjectModal`, `#editSubjectModal`, `#deleteSubjectConfirmModal` with strict type-to-confirm); JS cache-buster `?v=2`.
9. **registrar-subjects.js** rewritten — Actions column now returns Edit + Assign Trainer + Delete buttons; `loadQualificationsDropdown()`, `setupCreateSubject()`, `setupEditSubject()`, `setupDeleteSubject()` added.
10. **Tests** — `ClassManagementSubjectServiceTest` (9 tests) + `ClassManagementSubjectControllerWebMvcTest` (6 tests). Full suite: **105 tests, 0 failures**.

### Open Items / Deferred
- Manual browser smoke test (Create → Edit → Assign Trainer → Delete happy path + FK-block path).
- Verify `system_logs` rows for create/update/delete via `/logs.html`.
- Jira: transition AGILE-89, AGILE-90, AGILE-91 to Done after PR merges.

---

## Previous Session (May 9, 2026 — Student-Details Wizard Trim)

### Items Completed
1. **Baptismal Certificate optional.** Removed `certStatus`/`pendingBaptCert` guard from `STEP_CUSTOM_VALIDATORS[2]`. Baptism Date + Place remain required when "Baptized" is checked. File input retained — students can still upload voluntarily.
2. **Educational Background → 4 columns.** Removed `Grade/Year` and `Semester` columns from `<thead>` and all 4 `<tbody>` rows. Renamed `Year Ended` → `School Year`. Dropped `.edu-grade`/`.edu-sem` reads from `buildPayload` and `populateForm`.
3. **School Years at Anihan removed.** Deleted the entire `#syTable`/`#addSyRow` HTML block. Removed `renderSyRow()` DOMContentLoaded call, `addSyRow` listener, 12-line `#syTableBody` forEach in `buildPayload`, `schoolYears` population block in `populateForm`, and `renderSyRow()`/`addSyRowData()` functions. `const schoolYears = []` remains in payload for DTO compatibility.
4. **JS cache-buster bumped** `?v=4` → `?v=5`.

### Verified
- `grep` for `syTable|syTableBody|addSyRow|renderSyRow|addSyRowData|edu-grade|edu-sem` → zero matches in both files.
- `./gradlew test` → **90 tests, 0 failures, 0 errors**.

### Open Items / Deferred
- Manual browser smoke test of the updated wizard (Steps 2–4, baptism opt-out, education 4-col, submit).
- Registrar regression: confirm school-year rows can still be added via the registrar edit form after a new student submits with zero `student_school_years` rows.

---

## Previous Session (May 9, 2026 — DB Sync + Error-Handler Hardening + Section FK Pre-Check)

### Audit Findings (read-only investigation phase)
1. **CRITICAL:** Live MySQL had 17 tables, code expected 19. The `2026-05-09-classes-and-trainers.sql` migration had not been applied on this machine. `GET /api/registrar/classes` → 500 `Table 'AnihanSRMS.classes' doesn't exist`. `GET /api/registrar/subjects` → 500 `Unknown column 's1_0.trainer_id'`.
2. **HIGH:** `student_records.middle_name` was still `NOT NULL` despite the JPA entity treating it as optional and the student-portal wizard allowing blank input. The 2026-05-05 drift migration relaxed 27 columns but missed this one.
3. **HIGH:** `GlobalExceptionHandler` generic 500 handler returned `"Internal server error: <ExceptionClass> - <message>"` which leaked raw SQL, table names, and column names to clients.
4. **MEDIUM:** `ClassManagementService.deleteSection` had no FK pre-check — relied on raw MySQL FK violation, which then leaked through the generic handler.
5. **MEDIUM:** `getCurrentSemester` loaded all batches into memory just to compute a max.

### Items Completed
1. **Live DB migrated.** Re-applied `2026-05-09-classes-and-trainers.sql` (added `classes`, `class_enrollments`, `subjects.trainer_id`, seeded 2 qualifications + 6 subjects). Live tables: 17 → 19.
2. **New migration `2026-05-09-relax-middle-name.sql`** — applied to live DB. `student_records.middle_name` now `NULL`.
3. **`schema.sql` updated** — fresh-install schema matches live DB on the relaxed column.
4. **`GlobalExceptionHandler` hardened** — added SLF4J logger; new `DataIntegrityViolationException` handler (HTTP 409); generic `Exception` handler logs server-side and returns sanitized `"An unexpected error occurred."`.
5. **`SchoolClassRepository`** — added `existsBySectionSectionCode(String)`.
6. **`BatchRepository`** — added `findTopByOrderByBatchYearDesc()`.
7. **`ClassManagementService.deleteSection`** — pre-checks for class references and throws `IllegalArgumentException` (→ HTTP 400) with a clear actionable message.
8. **`ClassManagementService.getCurrentSemester`** — replaced `findAll()` + in-memory max with a single repository call.

### Verified
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → **90 tests, 0 failures, 0 errors**
- `./gradlew bootRun` → started cleanly. Authenticated as `registrar` and confirmed:
  - `GET /api/registrar/subjects` → HTTP 200 with 6 seeded subjects
  - `GET /api/registrar/classes` → HTTP 200 `[]`
  - `GET /api/registrar/classes/current-semester` → HTTP 200 `{"semester":"2026"}`
- Live MySQL: 19 tables, `subjects.trainer_id` present, qualifications + subjects seeded, `student_records.middle_name` nullable.

### Open Items / Deferred
- N+1 in `getEligibleStudents()` and `getClasses()` — acceptable at the ~160-student scale; revisit if perf issues surface.
- Move `ClassEnrollmentResponse` and `StudentSummary` records from `ClassManagementService` into `dto/registrar/` (cosmetic).
- Unit/WebMvc tests for `ClassManagementService` and `ClassManagementController` — would have caught the live-DB drift earlier; remains on the roadmap.

---

## Previous Session (May 9, 2026 — Registrar Subjects/Classes/Sections + Class Enrollment)

### Items Completed
1. **DB migration `2026-05-09-classes-and-trainers.sql`** — Idempotent. Adds `subjects.trainer_id` (FK → `users.user_id`, ON DELETE SET NULL), creates `classes` + `class_enrollments`, seeds 2 qualifications (Cookery NC II, Bread and Pastry Production NC II) + 6 subjects. Applied to live MySQL via `docker exec`.

2. **JPA entities** — `SchoolClass` (named to avoid `java.lang.Class` clash), `ClassEnrollment`. `Subject.java` extended with optional `@ManyToOne User trainer`.

3. **Repositories** — `SchoolClassRepository`, `ClassEnrollmentRepository`. `SectionRepository.findByBatchBatchYear`. `UserRepository.findByRoleAndEnabledTrue`.

4. **DTOs** (in `dto/registrar/`) — `SubjectResponse`, `AssignTrainerRequest`, `ClassResponse` (+`enrolledCount`), `CreateClassRequest`, `SectionResponse`, `CreateSectionRequest`, `TrainerResponse`, `EnrollStudentRequest`.

5. **Service `ClassManagementService`** — Subjects, Trainers lookup, Classes (current-semester resolution), Class Enrollment, Sections. Validates trainer role + enabled, enforces `(section_code, subject_code, semester)` uniqueness; eligible-student filter scoped to same section, Active/Submitted only.

6. **Controller `ClassManagementController`** — Separate from `RegistrarController`, mounted under `/api/registrar/`. Endpoints: `GET /subjects`, `PUT /subjects/{code}/trainer`, `GET /trainers`, `GET /classes`, `GET /classes/current-semester`, `POST /classes`, `GET /classes/{id}/enrollments`, `GET /classes/{id}/eligible-students`, `POST /classes/enroll`, `DELETE /enrollments/{id}`, `GET /sections`, `POST /sections`, `DELETE /sections/{code}`. Every state-changing call writes a `system_logs` row.

7. **SecurityConfig** — Registrar HTML matcher extended with `/classes.html` and `/sections.html`.

8. **Frontend pages** — Rebuilt `subjects.html` (DataTable + Assign Trainer modal); new `classes.html` (DataTable + Create Class + Manage Students modals, auto-fills subject's default trainer); new `sections.html` (DataTable + Create Section + Delete confirm). `registrar.html` navbar bumped from 2-link to 4-link (Home / Subjects / Classes / Sections).

9. **Frontend JS** — `registrar-subjects.js`, `registrar-classes.js`, `registrar-sections.js`.

10. **schema.sql refresh** — `subjects.trainer_id`, `classes`, `class_enrollments`, qualifications + subjects seed data. Header → 2026-05-09; table count 17 → 19.

### Verified
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → BUILD SUCCESSFUL (full suite, no regressions)
- `./gradlew bootRun` → Tomcat on 8080, 14 JPA repositories loaded, no schema validation errors
- Live MySQL: `classes` and `class_enrollments` present; `subjects.trainer_id` present; 2 qualifications + 6 subjects seeded.

### Open Items
- E2E browser smoke test of the three new pages and create/enroll flows (deferred to user)
- Unit tests for `ClassManagementService` / `ClassManagementController` not written yet (follow-up ticket)
