# Testing - Anihan SRMS

## Current Test Suite

| Module / Class | Type | Tests | Notes |
|---|---|---|---|
| `AdminServiceTest` | Mockito | ~10 | Update, self-role-lock, sanitized response, age recalc on view |
| `AdminControllerWebMvcTest` | WebMvc | ~7 | RBAC + validation + sanitized DTO output |
| `AdminBulkLoadTest` | Mockito | 3 | 100-user table fetch + DTO mapping + perf bound (<5s) |
| `AdminBulkLoadWebMvcTest` | WebMvc | 2 | 100-user JSON serialization + perf bound |
| `AccountServiceTest` | Mockito | 12 | Username/password change, personal details, age recalc |
| `AccountControllerWebMvcTest` | WebMvc | 9 | `/api/account/profile,password,details` + auth checks |
| `SystemLogServiceTest` | Mockito | 10 | logAction + getLogs filter precedence (default 7d, presets, custom range, invalid range) |
| `SystemLogControllerWebMvcTest` | WebMvc | 17 | List + export (CSV/XLSX/DOCX), filter validation, RBAC |
| `SystemLogExportServiceTest` | Mockito | 3 | CSV/XLSX/DOCX content + headers |
| `RegistrarBulkLoadTest` | Mockito | 4 | 200-record list, search across 9 fields, status filter, perf <5s |
| `RegistrarBulkLoadWebMvcTest` | WebMvc | 2 | 200-record JSON + `?q=` forwarding |
| `StudentRecordH2LoadTest` | `@DataJpaTest` (H2 in MySQL mode) | 1 | 100 records persist+load via real JPA, isolated from live MySQL |
| `StudentDetailsServiceTest` | Mockito | 7 | start (minimal record), resume, submit, double-submit guard, load |
| `AgeCalculatorTest` | Pure unit | 6 | null/today/past/future/birthday-edge cases (returns `Integer null` for null birthdate) |
| `ClassManagementSubjectServiceTest` | Mockito | 9 | createSubject/updateSubject/deleteSubject — FK pre-checks, duplicate code, unknown qualification |
| `ClassManagementSubjectControllerWebMvcTest` | WebMvc | 6 | POST/PUT/DELETE subjects, GET qualifications, TRAINER forbidden |
| `ClassManagementServiceTest` | Mockito | 6 | updateClassTrainer — assign, unassign, class not found, trainer not found, not-a-trainer, disabled |
| `ClassManagementControllerWebMvcTest` | WebMvc | 4 | PUT /classes/{id}/trainer — 200 assign+log, 200 unassign+log, 403 TRAINER, 400 service throws |
| `ClassManagementSectionServiceTest` | Mockito | 13 | updateSection, getStudentsInSection, getEligibleStudentsForSection, assignStudentsToSection, removeStudentFromSection, bulkEnrollSectionIntoClass |
| `ClassManagementSectionControllerWebMvcTest` | WebMvc | 7 | PUT/GET/POST/DELETE section-student endpoints, POST enroll-section — RBAC + log verify |
| `TrainerServiceTest` | Mockito | 12 | getMyAssignedSubjects, getStudentsForSubject (not-assigned throws), getMyClasses, getStudentsForClass (ownership guard, class-not-found, null-trainer) |
| `TrainerControllerWebMvcTest` | WebMvc | 9 | All 4 GET endpoints — 200 happy path, 403 non-trainer, 401 anonymous, 400 on service throws |
| `DocumentServiceTest` | Mockito | 17 | Upload whitelist (extension/size/empty), unknown student/type, generated-HTML save (.html appended, text/html), blank-filter normalization, missing document, delete (success/missing), prepareDownload (html→docx + friendly name, uploads unchanged), update-in-place (success / wrong student / uploaded-file rejected) |
| `DocumentGenerationServiceTest` | Mockito | 3 | Aggregated generate-data payload, null OJT, missing student throws |
| `DocumentControllerWebMvcTest` | WebMvc | 17 | List (200/403/401), types, multipart upload 201+log, 400 on service reject, download attachment+log, docx download headers, view inline no-log, generate 201+log, generate-update log, blank-fields 400, generate-data, DELETE (204+log / 404 / 403 / 401) |
| `HtmlDocxConverterTest` | Pure unit | 4 | OOXML parts present, original HTML preserved as altChunk part, altChunk references wired, empty-content rejection |
| `RegistrarStudentNumberServiceTest` | Mockito | 11 | assignStudentNumber — assign to a numberless record, trim, overwrite, same-number-same-record, blank clears, null clears, duplicate on another record throws (target untouched, no save), unknown record; **edit-form update preserves the number**; `hasStudentNumber` filter partitioning (blank counts as missing); free-text search by number |
| `RegistrarStudentNumberControllerWebMvcTest` | WebMvc | 9 | PUT student-number — 200 + "Assigned…" log, 200 + "Cleared…" log, null body accepted, 400 duplicate (message passthrough, no log written), 400 invalid characters (field-level error), 400 too long, 404 unknown record, 403 trainer, 401 anonymous |

| `StudentNumberSheetParserTest` | Pure unit | 18 | **The suite to re-run after editing `StudentNumberImportMapping`.** Header detection with and without title rows, alias spellings (`"Student No."` / `"student_no"` / `"STUDENT ID"`), column-order independence, missing/unrecognisable header → clear message, optional name columns, trailing blank rows, blank cell → null, quoted CSV with embedded commas, UTF-8 BOM, XLSX numeric cells (no `.0` tail), **leading zeros preserved**, mid-sheet empty rows, empty file, bad extension |
| `StudentNumberExportServiceTest` | Pure unit | 8 | Canonical header row, blank Student Number cell for unassigned, CSV escaping, filename extension, **CSV and XLSX round-trip back through the parser**, leading-zero survival, header-only export |
| `StudentNumberImportServiceTest` | Mockito | 22 | Every outcome (assign, overwrite on/off, in-use conflict, unchanged, unknown reference, duplicate-in-file, invalid format, too long, blank, name mismatch); preview writes nothing; apply writes only applicable rows; trimming; counts; file guards; xlsx upload |
| `StudentNumberControllerWebMvcTest` | WebMvc | 13 | Export per format + attachment header + log, unsupported format 400, inverted year range 400, preview 200 with **no log written**, overwrite flag forwarded, parse failure → 400 with message, apply logs per-row + summary, no per-row log for skipped rows, RBAC (403 trainer / 401 anonymous on both export and apply) |
| `SecurityQuestionServiceTest` | Mockito | 21 | setup (happy path 2 defaults, already-complete throws, duplicate default rejected, custom matching a default word-for-word rejected regardless of case/punctuation, two custom questions allowed, both-fields-set-on-one-slot rejected), replaceAnswers (wrong current password throws with no writes, correct password deletes-then-inserts), lookupByEmail (not found / disabled / locked / setup-incomplete all throw, happy path returns question texts in slot order), verifyAnswers lockout state machine (already-locked rejects immediately without querying answers, correct answer resets the counter, wrong answer increments, 3rd wrong answer locks, 15-minute decay from the first failure in a stale streak), resetPassword (mismatch throws, same-as-current throws, happy path updates password + timestamp) |

**Latest full-suite result:** `./gradlew test` → BUILD SUCCESSFUL — **363 tests, 0 failures, 0 errors** (2026-09-19, after the security-questions feature; +2 more in `AdminServiceTest` for `unlockUser`, listed above).

## Live Verification — 2026-09-19 (Security Questions / Forgot Password)

Two real bugs were found and fixed only by testing against the live app + live MySQL, not by
the automated suite (which runs against a throwaway H2 database built fresh from the JPA
entities, so it can't catch "the migration was never applied to the real database" — the
Gradle suite passing is not, by itself, evidence the live app works after a schema change):

1. **Migration never applied to live MySQL.** Login failed for every account and the
   forgot-password email lookup failed for all 3 seed accounts immediately after
   implementation, because the new columns/tables and the seed-email fix existed only in the
   migration *file*, never actually run against the real database. Fixed by backing up
   (`backup-2026-09-19-pre-security-questions.sql`), applying the migration via
   `docker exec -i mysql-server mysql -u root -p... < migrations/2026-09-19-security-questions.sql`,
   and confirming idempotency by re-running it (still exactly 6 default-question rows, same
   column set, no duplicates).
2. **A CSS fix appeared to have zero effect through repeated hard refreshes / private-window
   testing.** Root cause was not a stylesheet conflict — `./gradlew bootRun` only copies
   `src/main/resources` into `build/resources/main` at startup and does not watch for edits, so
   the browser was correctly asking for a fresh file while the *server process* kept serving a
   stale compiled one. Confirmed two ways: diffing `build/resources/main/static/css/dashboard.css`
   against the edited source (0 matches for the new rule before a restart), and an isolated
   headless-Edge screenshot (`msedge.exe --headless --disable-gpu --screenshot=... file:///repro.html`
   against a minimal reproduction using the real served stylesheets) that proved the CSS itself
   rendered correctly, which is what narrowed the search away from "wrong CSS" and toward
   "stale server" as the real cause. **Technique worth reusing**: when a static asset change
   "isn't showing up" no matter what the browser does, compare `build/resources/main` against
   `src/main/resources` directly before assuming a CSS/cache problem — and remember the app
   process itself needs restarting for static file changes under plain `bootRun`, not
   `--continuous` and not Spring DevTools.

Live round trip performed after the migration was applied (curl-based, not yet a browser
E2E pass): `admin`/`password123` login → 200 `ROLE_PENDING_SETUP` → `GET default-questions` →
`POST setup` (2 default questions) → session upgraded to `ROLE_ADMIN` → `GET /api/auth/me`
shows `securityQuestionsSetUp: true` → `POST /api/password-recovery/lookup` with
`admin@anihan.local` returns the 2 question texts. The test security-question answers created
during this check were deleted from the live database afterward (`DELETE ... FROM
user_security_answers ... WHERE username = 'admin'`) so the account is back to "setup not yet
done" for the user to complete themselves. The live password-reset step (`verify` → `reset`)
was **not** exercised against production data, to avoid changing the real admin password —
that logic is covered instead by `SecurityQuestionServiceTest`'s `resetPassword*` tests.

## Live Verification — 2026-09-06 (post-merge DB sync)

- 5 migrations applied to live MySQL in date order, then re-run once more — idempotent
  (byte-identical, no duplicate columns/FKs): `2026-08-26-subjects-competency-type`,
  `2026-08-26-subjects-code-update-cascade`, `2026-08-27-add-student-number`,
  `2026-08-29-drop-subjects-trainer-id`, `2026-08-29-grades-overhaul`.
- Structural diff (live `--no-data` dump vs a throwaway DB built from `schema.sql`) →
  **cosmetic only**: FK / unique-index auto-names, secondary-index listing order, `grades`
  physical column order. Name-stripped column definition set byte-identical — no real drift.
- Hibernate `ddl-auto=validate` boot against live MySQL → **PASS** (started in 10.192s,
  19 JPA repositories, zero schema-validation errors).

## Browser E2E — 2026-08-30 (Student Numbers Report Page)

Playwright headless Edge against the running app + live MySQL. **22/22 passed, run twice**
(self-cleaning: clears every number it creates, leaves pre-existing ones alone).

Covered: navbar link → page loads all students → summary line counts who still needs a number →
zero horizontal overflow → Not Assigned filter → **real file download** (dated filename, header
matching the import aliases, only the filtered students) → encode the downloaded CSV → preview
(labelled a preview, correct counts, duplicate pair and unknown student flagged, **nothing
written to the DB**) → Apply enabled only after a preview → apply reports what it wrote →
summary updates → Apply re-disables → choosing a different file clears the stale preview →
single Assign Number still works → cleanup.

**Harness notes:** browsers do not fire `change` when the *same* file is re-selected, so testing
"a new file resets the preview" needs a genuinely different path. DataTables row counts must be
polled (see the 2026-08-27 note).

## Live Verification — 2026-08-30 (Export/Import Round Trip)

Against real MySQL, with a backup taken first and the DB restored afterwards:
- Exported the 6 unnumbered students; the already-numbered student was correctly excluded.
- One sheet exercising every failure mode at once: 2 valid rows, a duplicate-in-file pair, a
  number already held by another student, an unknown reference, and a blank row → preview
  classified all 7 correctly and **wrote nothing** (DB and `system_logs` verified unchanged) →
  apply wrote exactly the 2 valid rows.
- **Excel fidelity** via a POI-authored workbook simulating the registrar typing into Excel:
  `0012` (leading zero), `2025-777`, and `A/2026/03` all imported intact.
- **Header auto-detection**: the same workbook with two title rows pasted above the header
  parsed correctly, reporting the true spreadsheet row numbers.
- Overwrite guard refused by default with an actionable message and applied with the flag set.
- `system_logs`: per-assignment rows + import summary + export row; none from a preview.

## Browser E2E — 2026-08-27 (Registrar-Controlled Student Number)

Playwright `playwright-core` driving headless system Edge against the running app + live
MySQL. **18/18 checks passed, run twice** (self-cleaning: the run clears the number it
assigned, leaving the DB as it found it).

Covered: login → "Reference No." + "Student Number" headers → every student shows a
`Not Assigned` badge → both action buttons per row → **zero horizontal overflow** (also
measured at 1280/1440/1920) → assign via modal → number appears in the table → duplicate shows
"already assigned to …" inline → invalid characters show the field-level message (not the
generic "Validation failed") → Not Assigned / Assigned filters + Reset → details modal shows
the number → edit form shows it read-only with the relabelled Reference No. → cleanup clears it.

**Harness note for next time:** DataTables renders a placeholder row before the AJAX rows
arrive, so `waitForSelector('tbody tr')` is a racy anchor — three checks failed spuriously
against a correct app. Wait for a *data-row* selector and for the row count to settle, and
wait on `.modal-backdrop` detaching before the next click.

**Two real bugs this E2E caught** (both fixed): the filter bar's `flex-wrap: nowrap` in
dashboard.css pushed the table into 86px of horizontal scroll once the Student No. dropdown
was added; and the details-modal click handler, bound to `button[data-record-id]`, would have
fired on the new Assign button too.

## Manual Smoke Test — 2026-05-10 (Subjects CRUD)

User-verified in browser after Subjects CRUD implementation:
- Create Subject → qualification dropdown loads, form saves, row appears in DataTable — **PASS**
- Edit Subject → pre-populates name/qualification/units, saves correctly — **PASS**
- Assign Trainer → trainer dropdown loads, assignment saves, badge updates — **PASS**
- Delete Subject (type-to-confirm) → "delete" input gates button, row removed on confirm — **PASS**
- `system_logs` rows for create/update/delete visible in `/logs.html` — **PASS**
- No regressions observed.

## Manual Smoke Test — 2026-05-09 (post-fix)

After re-applying the 2026-05-09 migration to the live MySQL DB:
- `POST /api/auth/login` as `registrar` → 200
- `GET /api/registrar/subjects` → 200 with 6 seeded subjects (was 500: "Unknown column 's1_0.trainer_id'")
- `GET /api/registrar/classes` → 200 `[]` (was 500: "Table 'AnihanSRMS.classes' doesn't exist")
- `GET /api/registrar/classes/current-semester` → 200 `{"semester":"2026"}`
- Live MySQL: 19 tables, `subjects.trainer_id` present, qualifications + subjects seeded, `student_records.middle_name` `IS_NULLABLE = YES`.

## Coverage Gaps (open)

- No WebMvc tests yet for `SecurityQuestionController`, `PasswordRecoveryController`, or the
  new `PUT /api/admin/users/{id}/unlock` endpoint — only service-level Mockito coverage exists
  for the security-questions feature so far.
- No tests for `StudentDetailsController`, `StudentPortalController`, `StorageService`
- `ClassManagementService`/`ClassManagementController` subject CRUD covered (May 10). Classes, sections, trainer-assign, and enrollment endpoints still untested.
- E2E browser smoke tests not yet executed for the May 9/10 Subjects/Classes/Sections pages

## Manual Verification Performed (historical)

- **Login security (AGILE-142):** Unauthenticated dashboard access → 302; cross-role access → 302; `/api/auth/me` w/o session → 401; all 3 roles login → 200 with correct `ROLE_*`.
- **Account management:** Username/password change works end-to-end including session invalidation and "wrong password" path.
- **Admin merge:** DTO responses verified to omit password field; self-role-change blocked; validation errors return 400.
- **Schema-drift fix (2026-05-05):** Suite went from 81/82 (failing on `contextLoads`) to 82/82 after applying `2026-05-05-fix-schema-drift.sql`.
- **DataTables 2 search bar + filter inputs:** Width fixes verified in browser on registrar home page.
- **Live age recalculation in Edit Account modal:** Birthdate change updates `#ageDisplay` immediately on the client; persists correctly on save.

## Pending Manual Checks

- [ ] Full browser/Playwright pass of the security-questions feature: mandatory setup on first
      login (including attempting a direct API call while still `ROLE_PENDING_SETUP`, to prove
      the server-side gate works and not just the frontend redirect); edit security questions
      from the account settings modal; forgot-password happy path end-to-end; 3 wrong answers
      → confirm lockout also blocks *normal* password login, not just forgot-password; admin
      "Unlock Account" clears it; reset password → lands on dashboard without a second login.
      Only curl-based spot checks have been done so far (see `activeContext.md` /
      `changeLog.md` 2026-09-19 entries).
- [ ] Browser retest: admin login → admin dashboard renders; user-detail modal + edit-user flow work end-to-end.
- [x] Browser smoke: Subjects CRUD — Create → Edit → Assign Trainer → Delete happy path — all passed (2026-05-10).
- [x] Verify `system_logs` rows for subject create/update/delete via `/logs.html` — confirmed (2026-05-10).
- [ ] Browser smoke: Edit Class Trainer — click Edit Trainer on a class row, change trainer, verify row updates; unassign, verify "Unassigned" italic; confirm `/logs.html` has audit rows.
- [ ] Browser retest: registrar Subjects / Classes / Sections pages — assign trainer, create class, enroll/unenroll student, create/delete section.
- [ ] Verify `(section_code, subject_code, semester)` uniqueness on classes via UI.
- [ ] Verify section delete is blocked when classes reference it.
- [ ] Browser smoke: sections.html — Edit section name, Manage Students (assign eligible + remove), verify status transitions (Submitted→Active on assign, Active→Submitted on remove).
- [ ] Browser smoke: classes.html — Enroll Whole Section button, verify counts in success alert, confirm `system_logs` bulk-enroll row.

## Test Environment Notes

- All Mockito-based bulk tests generate data programmatically — no hard-coded dummy rows.
- H2 integration tests use `jdbc:h2:mem:...;MODE=MySQL` with `ddl-auto=create-drop`; live MySQL never touched.
- Spring Boot 4.0 paths: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` and `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`.
