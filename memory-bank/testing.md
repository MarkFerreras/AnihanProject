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

**Latest full-suite result:** `./gradlew test` → BUILD SUCCESSFUL — **299 tests, 0 failures, 0 errors** (August 30, 2026, after the student-number export/import session).

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
