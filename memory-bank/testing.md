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
| `DocumentServiceTest` | Mockito | 24 | Upload whitelist (extension/size/empty), unknown student/type, generated-HTML save (.html appended, text/html), blank-filter normalization, missing document, delete (success/missing), prepareDownload (html→docx + friendly name, uploads unchanged), update-in-place (success / wrong student / uploaded-file rejected), **ID picture** (findIdPicture empty, known-type registration, upload saves + returns summary, replace-in-place reuses documentId, reject non-image, reject over 2MB, reject unknown student) |
| `DocumentGenerationServiceTest` | Mockito | 3 | Aggregated generate-data payload, null OJT, missing student throws |
| `DocumentControllerWebMvcTest` | WebMvc | 21 | List (200/403/401), types, multipart upload 201+log, 400 on service reject, download attachment+log, docx download headers, view inline no-log, generate 201+log, generate-update log, blank-fields 400, generate-data, DELETE (204+log / 404 / 403 / 401), **ID picture** (upload 201+log, forbidden for trainer, get 404 when none, delete 204+log) |
| `HtmlDocxConverterTest` | Pure unit | 4 | OOXML parts present, original HTML preserved as altChunk part, altChunk references wired, empty-content rejection |
| `RegistrarStudentNumberServiceTest` | Mockito | 11 | assignStudentNumber — assign to a numberless record, trim, overwrite, same-number-same-record, blank clears, null clears, duplicate on another record throws (target untouched, no save), unknown record; **edit-form update preserves the number**; `hasStudentNumber` filter partitioning (blank counts as missing); free-text search by number |
| `RegistrarStudentNumberControllerWebMvcTest` | WebMvc | 9 | PUT student-number — 200 + "Assigned…" log, 200 + "Cleared…" log, null body accepted, 400 duplicate (message passthrough, no log written), 400 invalid characters (field-level error), 400 too long, 404 unknown record, 403 trainer, 401 anonymous |

| `StudentNumberSheetParserTest` | Pure unit | 18 | **The suite to re-run after editing `StudentNumberImportMapping`.** Header detection with and without title rows, alias spellings (`"Student No."` / `"student_no"` / `"STUDENT ID"`), column-order independence, missing/unrecognisable header → clear message, optional name columns, trailing blank rows, blank cell → null, quoted CSV with embedded commas, UTF-8 BOM, XLSX numeric cells (no `.0` tail), **leading zeros preserved**, mid-sheet empty rows, empty file, bad extension |
| `StudentNumberExportServiceTest` | Pure unit | 8 | Canonical header row, blank Student Number cell for unassigned, CSV escaping, filename extension, **CSV and XLSX round-trip back through the parser**, leading-zero survival, header-only export |
| `StudentNumberImportServiceTest` | Mockito | 22 | Every outcome (assign, overwrite on/off, in-use conflict, unchanged, unknown reference, duplicate-in-file, invalid format, too long, blank, name mismatch); preview writes nothing; apply writes only applicable rows; trimming; counts; file guards; xlsx upload |
| `StudentNumberControllerWebMvcTest` | WebMvc | 13 | Export per format + attachment header + log, unsupported format 400, inverted year range 400, preview 200 with **no log written**, overwrite flag forwarded, parse failure → 400 with message, apply logs per-row + summary, no per-row log for skipped rows, RBAC (403 trainer / 401 anonymous on both export and apply) |
| `SecurityQuestionServiceTest` | Mockito | 21 | setup (happy path 2 defaults, already-complete throws, duplicate default rejected, custom matching a default word-for-word rejected regardless of case/punctuation, two custom questions allowed, both-fields-set-on-one-slot rejected), replaceAnswers (wrong current password throws with no writes, correct password deletes-then-inserts), lookupByEmail (not found / disabled / locked / setup-incomplete all throw, happy path returns question texts in slot order), verifyAnswers lockout state machine (already-locked rejects immediately without querying answers, correct answer resets the counter, wrong answer increments, 3rd wrong answer locks, 15-minute decay from the first failure in a stale streak), resetPassword (mismatch throws, same-as-current throws, happy path updates password + timestamp) |
| `AuthControllerWebMvcTest` | WebMvc | 4 | Pins the 2026-09-19 decision that login/logout are NOT audited — `loginWritesNoSystemLogRow` and `logoutWritesNoSystemLogRow` both assert `verifyNoInteractions(systemLogService)`; the logout test deliberately stubs `userRepository.findByUsername("admin")` even though the new code never calls it, so it's a genuine regression pin against the *old* code (which resolved the user via that call before logging), not a vacuous pass; `loginStillReturnsUsernameAndRole` confirms the login response contract survived the removal; **+2026-09-22:** `missingPermittedStaticResourceReturnsJson404` pins Bug 13's fix — confirmed RED (500) before the `NoResourceFoundException` handler was added, GREEN after |
| `TrainerControllerWebMvcTest` (2026-09-22 additions) | WebMvc | +3 | `getAvailableSemestersReturnsAssignedYears`, `getMyClassesPassesExplicitSemesterToService`, `getMyClassesPassesBlankSemesterToService` — characterization tests pinning the already-merged semester-filter backend contract, added ahead of the frontend idempotency fix in `trainer-classes.js` |
| `ClassManagementControllerWebMvcTest` (2026-09-22 addition) | WebMvc | +1 | `getAvailableSemestersReturnsYears` — same characterization purpose, registrar side |
| `TrainerServiceTest` (2026-09-22 addition) | Mockito | +1 | `getMyClassesFiltersByExplicitSemester` — confirms `getMyClasses(String semester)` genuinely filters by year (not a tautology); zero production code touched |
| `SchemaContractTest` | Pure unit | 2 | `documentFileTypeSupportsOpenXmlMimeTypesEverywhere` (Bug 12 fix: pins `Document.fileType`'s `@Column(length=100)` + both SQL schema files' `file_type VARCHAR(100) NOT NULL` text + the migration file's existence); `demoAccountSeedOnlyInsertsMissingUsers` (pins `seed-accounts.sql`'s insert-only contract via text-substring checks — present: `WHERE NOT EXISTS`, `TIMESTAMPDIFF(YEAR`, all 3 usernames; absent: `UPDATE users`, `DELETE FROM users`, `DELETE FROM user_security_answers`, `ON DUPLICATE KEY UPDATE`) |
| `DocumentFolderServiceTest` | Mockito | 9 | Batch year-desc/code-asc sort + No Batch last, empty batch/section still appear, assigned student uses section's batch over its own mismatched batch, unassigned-with-null-batch → No Batch, No Batch omitted when empty, student sort (last/first/reference), aggregate count summation |
| `DocumentExportServiceTest` | Mockito | 19 | 404 per scope (student/section/unassigned-batch/batch), 409 empty, filename per scope (studentNumber-or-id fallback), entry layout per scope (flat/student-folder/Sections-or-Unassigned), duplicate-filename `(1)`/`(2)` suffixing incl. case-insensitive, truncated-to-identical folder names disambiguated, unsafe/control-char stripping, Windows reserved-name rename, 120-char cap preserving extension, blank-name → `document-{id}` fallback, `writeZip` produces a valid parseable archive on success / no EOCD trailer on failure |
| `DocumentStorageIntegrationTest` (extended) | `@SpringBootTest` real H2 (MySQL mode, real DDL script — not `create-drop`) | 10 | 3 scalar reads regardless of student count + no BLOB/`SELECT *`; exact `SR20260001` vs `SR202600010` lookup; folder-hierarchy real aggregation; **real transaction commit/rollback**: success commits N documents + exactly 1 audit row, a second-file `IOException` rolls back everything (0 documents, 0 audit rows), an audit-persistence failure (`@MockitoSpyBean` on `SystemLogRepository`) rolls back the documents too; section-scope includes an assigned student despite mismatched own batch; unassigned-scope uses the student's own batch, not any section's; batch-scope combines section-assigned + same-batch-unassigned only; `DocumentContentRepository` copies exact stored bytes; end-to-end `prepareExport`+`writeZip` produces a real parseable ZIP with the stored bytes |
| `DocumentServiceTest` (extended) | Mockito | +16 | Exact student listing (200/`[]`/404/blank/no-prefix-collision); `uploadBatch` — in-order save + reorder-by-input via mocked `findSummariesByIds`, 0/21/exactly-20 files, >10MiB file, >50MiB combined (mocked `getSize()`, no giant byte arrays), any-file-empty, mismatched list sizes, unknown type, reserved ID-picture category, disallowed extension, unknown student → 404 (not 400, unlike single upload), file-read `IOException` → `UncheckedIOException` |
| `DocumentControllerWebMvcTest` (extended) | WebMvc | +27 | `/folders/tree` (200/`[]`/403×2/401), `/student/{id}` (200/`[]`/404/403/401), `/batch` (201 ordered, 400 service-reject, 400 missing param, 400 missing part, 404 unknown student, 403×2/401), 4× `/export/...` (200+headers, 400 validation passthrough, 404, 409, 403×2/401 **per route**) |

**Latest full-suite result:** `./gradlew test` → BUILD SUCCESSFUL — **484 tests, 0
failures, 0 errors, 0 skipped** (2026-09-27, `feature/document-folder-management` branch,
after all 4 implementation tasks of the document-folder-management plan; 393 baseline + new
tests added across the plan's 4 tasks; recount via the XML test reports, not a hand tally).

## Live Verification — 2026-09-27 (Document Folder Management, Disposable DB)

Real end-to-end verification against a **disposable** MySQL 8 Docker container (port 3307,
`anihan-task5-test-mysql`, `schema.sql` applied fresh) and a locally-run `./gradlew bootRun`
instance on port 8081 — the live `mysql-server` container (port 3306, real `AnihanSRMS`) was
never connected to, migrated, or modified. Container and all temp files removed after.

**Browser (Playwright), logged in as `registrar`:**
- Folder tree renders real batches/sections/students with correct counts from the seeded
  schema data; drill-down batch → section → student with correct breadcrumb at each level.
- Batch overview: section/unassigned cards, "Export Entire Batch" correctly `disabled` at
  zero documents.
- Student detail: Reference No./Student Number line, status badge, "No documents uploaded
  yet." (real async fetch to `/student/{id}`).
- Locked upload (from student context): student field pre-filled+readonly, help text
  correct; drag-and-drop staging (simulated via a real `DragEvent`/`DataTransfer`) added a
  row with filename/size/category-select/Remove; Upload button transitioned
  disabled→enabled once a valid file was staged.
- Upload succeeded (real `POST /batch` against the real DB): tree count, Export Student
  (disabled→enabled link), and the student's document list all refreshed in place.
- Shared delegated View (iframe pointed at `/api/registrar/documents/{id}/view`) and Delete
  (type-"delete"-to-confirm) both worked from the explorer-rendered row, with the same
  handlers the table uses; after delete, counts and Export Student's disabled state
  reverted correctly.
- Explorer filter ("Lopez"): only the matching batch/section/student stayed visible,
  ancestors retained, non-matching batches hidden; selection preserved across a tab switch
  to "All Documents Table" and back.
- Non-locked upload (Table tab entry point): student combobox required an **exact** label
  match — typing a partial name (`"Lopez"`) left the Upload button `disabled` (no
  auto-selection of the first match).
- Table tab: DataTables renders with real batch/section/type filter options populated from
  the live data.

**Real HTTP (curl, session cookie from the same login) — confirms behavior MockMvc cannot,
since MockMvc never enforces servlet-container multipart limits:**
| Case | Combined size | Per-file | Result |
|---|---|---|---|
| 2 files | 16MiB | 8MiB each | **201**, above the *old* 15MiB cap |
| 1 file | 11MiB | — | **400** `"...10MB per file..."` |
| 6 files | 51MiB | 8.5MiB each | **400** `"Combined upload size exceeds the 50MB limit."` (business check; container's 52MiB cap alone would have let this through) |
| 6 files | 54MiB | 9MiB each | **400** `"...maximum allowed size..."` (container-level, over the 52MiB request cap) |
| 21 files | tiny | — | **400** `"A maximum of 20 files..."` |

- `SELECT COUNT(*) FROM documents WHERE student_id=...` confirmed **0 new rows** after each
  of the 4 rejected requests (still 8 from the 2 successful ones).
- `system_logs` confirmed **exactly 2 rows** total for this student (`"Uploaded 2
  document(s)..."`, `"Uploaded 6 document(s)..."`) — one per successful batch, zero for any
  rejected attempt.
- `GET /export/student/{id}` on the 8-document student: `200`, `Content-Type: application/zip`,
  `Content-Disposition: attachment; filename="Documents_SR-CURL-001_Curl.zip"`,
  `Cache-Control: no-store`; the downloaded ZIP parsed with Python's `zipfile` — 8 entries,
  every filename and byte size matched exactly what was uploaded, `testzip()` returned
  `None` (valid archive, no CRC errors).
- `GET /export/section/{empty}` → `409 {"message":"No documents to export."}`;
  `GET /export/section/{unknown}` → `404 {"message":"section not found: ..."}`;
  `GET /export/section/{populated}` → `200 application/zip`.

**Not verified this session (explicit gaps, not silently skipped):** true concurrent-request
abort/disconnect recovery mid-stream on a live export; mobile-viewport rendering; a full
screen-reader pass over the explorer's tree/breadcrumb.

## Independent Final Review — 2026-09-27 (GO-WITH-FIXES) and Post-Review Fixes

A fresh subagent with no prior context on this work reviewed the spec, the plan, and the
full `main...feature/document-folder-management` diff, and independently re-ran
`./gradlew test`, parsing the JUnit XML reports directly (confirmed 484/0 exactly). Verdict:
**GO-WITH-FIXES** — scoping correctness (§2), transactional atomicity (§4), and ZIP
safety/memory (§5) were all confirmed genuinely correct against real (non-mocked-persistence)
tests. Two concrete gaps were found and are now fixed:

1. **Upload filename validation didn't implement spec §4's stated rules** — the pre-existing
   `isBlank() || contains("..")` check (shared with the pre-branch single-upload endpoint)
   had no 255-char cap, no control-character/reserved-Windows-character rejection, and no
   dot-only-name rejection. **Fixed:** a shared `DocumentService.requireValidFileName(...)`
   now enforces all of spec §4's rules for both `upload()` and `uploadBatch()`; 8 new unit
   tests added (`DocumentServiceTest`) covering >255 chars, control chars, reserved chars,
   dot-only names (`"."`, not just `".."`, which the old check already caught), and
   browser path-prefix stripping. `uploadIdPicture()`/`saveGenerated()` were correctly left
   untouched (different spec section / server-generated name respectively).
2. **The Folder Explorer's tree contradicted the spec's own explicit warning** ("Do not
   claim an ARIA tree without its keyboard behavior") — rows were `<div role="button">` with
   no `role="treeitem"`/`aria-selected` and no arrow-key navigation, which the reviewer
   correctly flagged as inconsistent with this file's own prior claim of "real `<button>`s
   per spec." **Fixed:** rows now carry `role="treeitem"`/`aria-selected`; nested `<ul>`s
   carry `role="group"`; a roving-tabindex implementation (`applyRovingTabindex`,
   `setupTreeKeyboardNav`) handles ArrowUp/ArrowDown/ArrowRight/ArrowLeft/Home/End per the
   WAI-ARIA tree pattern, including re-focusing the same logical node across the
   expand/collapse re-render. **Live-reverified** (second disposable-MySQL round, Playwright):
   `role="treeitem"`/correct initial roving `tabindex` confirmed via DOM query; ArrowDown
   moved focus+tabindex to the next batch row; ArrowRight on a collapsed batch expanded it
   and kept focus on that same row; a second ArrowRight moved focus into its first child
   section row (`role="treeitem"` confirmed); ArrowLeft moved focus back to the parent batch
   row. All four moves worked exactly as the WAI-ARIA tree pattern requires.
3. **[Low, also fixed]** `DocumentExportService.writeZip`'s inner `try/finally { closeEntry(); }`
   could let a `closeEntry()` failure mask the real root-cause exception in server logs.
   Removed the inner try/finally — `completed` still correctly stays `false` on any failure,
   so the abort-without-trailer guarantee is unaffected; only log fidelity improves.

Full suite after fixes: **492 tests, 0 failures** (484 + 8 new filename-validation tests).
Commit: see `changeLog.md`'s next entry for the exact hash.

## Live Verification — 2026-09-22 (Interim Client Demo Stability Plan Execution)

Full backup-first, migrate, verify-live-MySQL, role-based-browser-smoke cycle for Tasks 1–5
of `docs/superpowers/plans/2026-09-22-client-presentation-readiness.md`, executed against the
Docker `mysql-server` container (`AnihanSRMS`) and a locally-run `./gradlew bootRun` instance.

**Commands run, in order:**
```bash
docker exec mysql-server mysqldump -uroot -pmy_password --databases AnihanSRMS --result-file=/tmp/pre-stability.sql
docker cp mysql-server:/tmp/pre-stability.sql C:\tmp\anihan-client-demo\pre-stability.sql
docker cp src/main/sql/migrations/2026-09-22-widen-documents-file-type.sql mysql-server:/tmp/widen-documents-file-type.sql
docker exec mysql-server sh -c "mysql -uroot -pmy_password < /tmp/widen-documents-file-type.sql"
docker exec mysql-server mysql -uroot -pmy_password AnihanSRMS -e "SELECT username, role, enabled FROM users WHERE username IN ('admin','registrar','trainer') ORDER BY username;"
./gradlew clean test
./gradlew bootRun   # backgrounded; stopped after verification
```

**Backup:** `C:\tmp\anihan-client-demo\pre-stability.sql`, 101,436 bytes, 21 `CREATE TABLE`
statements confirmed present before any mutation.

**Migration verification (live MySQL, `information_schema.COLUMNS`):**
```
COLUMN_TYPE   IS_NULLABLE
varchar(100)  NO
```

**Account check (live MySQL):** all three demo accounts already existed
(`admin`/`ROLE_ADMIN`, `registrar`/`ROLE_REGISTRAR`, `trainer`/`ROLE_TRAINER`, all
`enabled=1`) — `seed-accounts.sql` correctly **not** executed, per the plan's decision to
only run it when an account is missing.

**Automated suite:** `./gradlew clean test` → **BUILD SUCCESSFUL, 393 tests, 0 skipped, 0
failures, 0 errors** (summed from the generated `build/test-results/test/TEST-*.xml`
reports, not read off console text alone).

**Role-based browser/API stability smoke matrix (Playwright MCP browser bridge + curl for
the one endpoint the browser sandbox couldn't drive):**

| Role/area | Result |
|---|---|
| Public | `GET /js/does-not-exist.js` → `404 {"message":"Resource not found: js/does-not-exist.js"}` (was a 500 before this session's Bug 13 fix). |
| Admin | `admin`/`password123` → `admin.html`; User Directory DataTable shows all 5 accounts ("Showing 1 to 5 of 5 entries"); `logs.html` → `GET /api/logs?rangeDays=7` → 200. Console clean except the expected pre-login 401 on `/api/auth/me` and `favicon.ico`, which now correctly 404s (previously a documented 500 — direct live confirmation the Bug 13 fix also closed that separately-known issue). |
| Registrar/Classes | Login succeeds; semester `<select>` shows exactly one `2026` option (no duplicates); switching to "All Semesters" then explicitly back to "2026" fired **exactly one** new `/api/registrar/classes` request per change, confirmed via Playwright's network-request log (`browser_network_requests`), not just visual inspection — proves Task 1's idempotency fix holds in a real browser, not only in the unit tests. |
| Registrar/Documents | One real `.docx` (`application/vnd.openxmlformats-officedocument.wordprocessingml.document`, 71 chars) and one real `.xlsx` (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, 65 chars) uploaded to `SR20260002` via `POST /api/registrar/documents` → both **HTTP 201**, no truncation error. Verified via direct `SELECT document_id, file_type, LENGTH(file_type) FROM documents WHERE document_id IN (11,12)` that both MIME strings persisted at full, untruncated length. Generation page was not opened, per the plan's exclusion. **Cleanup:** both deleted via `DELETE /api/registrar/documents/{id}` → 204 each; re-queried, `COUNT(*) = 0` for those IDs — confirmed removed, DB back to pre-test document state. |
| Trainer | `trainer`/`password123` → redirected to the (pre-existing, unrelated) mandatory first-login security-question setup page, since this seed account had never completed it; completed with 2 real Q&A pairs, then reached `trainer-classes.html`. Semester dropdown deduplicated correctly; filter change fired exactly one new `/api/trainer/classes` request. "Input Grades" opened the grade modal with the real enrolled roster for `COOK-101`/section `TEST-T2`. **No grades were entered or saved** — modal closed without submitting, per the plan's explicit instruction. |

**Console check (final):** no unexpected errors across the whole session — only the expected
pre-login 401s on `/api/auth/me`, the now-correctly-404ing `favicon.ico`, and one unrelated
browser-extension (Grammarly) permissions-policy warning.

**Two environment gotchas worth remembering for future sessions:**
1. Playwright MCP's native file chooser (`DOM.setFileInputFiles`) returned a CDP "Not
   allowed" error in this environment — worked around by uploading via `curl` multipart
   directly against the real `POST /api/registrar/documents` endpoint using a fresh
   curl-based login as `registrar`, which exercises the identical `DocumentService.upload()`
   code path Bug 12 affects, so the verification is still against real backend behavior.
2. Stopping the `./gradlew bootRun` background shell task via `TaskStop` reported success
   but did **not** actually release port 8080 (a lingering JVM process). Had to find the PID
   via `netstat -ano | grep :8080` and force-stop it directly; a follow-up `curl` timeout
   confirmed the port was genuinely free afterward. Worth checking port liveness after
   stopping `bootRun`, not just trusting the stop confirmation.

**Not verified this session (explicitly out of scope per the plan):** document generation,
generated-document DOCX conversion, and export behavior. Bug 10 (grades don't appear on
generated documents) remains open and unrelated to this session's fixes.

## Live Verification — 2026-09-19 (Login Auditing Removed)

No browser automation was available this session (no Playwright browser bridge extension),
so verification was done via `curl` against the running app + real MySQL, the same pattern
used in several prior sessions in this file.

- **Purge counts (live `AnihanSRMS`, backup taken first):** pre-purge **334 total
  `system_logs` rows, 197 of them login/logout**; post-purge **137 total rows, 0
  login/logout rows** (334 − 197 = 137, exact match). Re-run of the migration a second time
  deleted 0 further rows — idempotent.
- **A real login** (`admin`/`password123`, succeeded, returned `ROLE_ADMIN`) left
  `system_logs` unchanged at **137** rows.
- **A real logout** also left `system_logs` unchanged at **137** rows.
- **A real, still-audited control action** — `PUT /api/account/details`, a safe no-op
  re-save of admin's own existing personal details — DID write a fresh row (**137 → 138**,
  action "Updated own personal details"), proving the removal only touched login/logout and
  did not accidentally disable auditing more broadly.
- `GET /api/logs?rangeDays=3000` returns all 138 surviving rows with zero containing "logged
  in"/"logged out"; `admin.html`/`dashboard.css`/`admin-users.js` confirmed clean of every
  stat-related identifier via static inspection; `logs.html` returns HTTP 200.
- **Not completed in this session** — no Playwright browser bridge was available. **Closed
  in a same-day follow-up session (2026-09-21, after PR #59 merged)** — see below.

## Live Verification — 2026-09-21 (admin.html Browser Walkthrough, follow-up)

Ran `./gradlew bootRun` against live MySQL and drove `admin.html` via the Playwright MCP
browser bridge (`mcp__playwright__*` tools, now available), logged in as `admin`.

- **False alarm, not a regression:** the first page load rendered the old stat-card hero
  ("Total Users"/"Admins"/"Registrars"/"Trainers"). A `fetch(url, {cache: 'no-store'})`
  against the live server confirmed the real response is clean, and `grep` across all three
  on-disk copies of `admin.html` (`src/`, `build/resources/main`, `bin/main`) confirmed none
  contain that markup. Root cause: a stale browser HTTP cache from earlier testing in the
  same browser profile, not a code issue. A cache-busted navigation (`admin.html?cb=1`)
  rendered the correct page — **lesson for next time: if a browser session shows content
  that doesn't match `grep` on the actual served files, suspect the browser cache before
  suspecting the server.**
- **Confirmed clean (cache-busted load):** full-width hero, no stat cards, no leftover
  layout gap; User Directory DataTable renders all 5 seed accounts with correct role badges
  ("Showing 1 to 5 of 5 entries"); search filters correctly (`registrar` → 1/5 filtered,
  cleared → 5/5); the `dataSrc: ''` fix from the 2026-09-20 session still works; details
  modal opens with all 10 fields populated (User ID, Username, Email, Role, Last/First/
  Middle Name, Age, Birthdate, Password Last Changed) plus working View Logs / Edit User
  links; zero horizontal overflow at 1280px and 992px.
- **Console:** clean apart from one pre-existing, unrelated `favicon.ico` 500 (Bug 13 in
  `bugs.md` — `GlobalExceptionHandler`'s missing-route handling; not part of this branch).
- Test server stopped after verification.

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

## Live Verification — 2026-09-19 (ID Photo Moved to Registrar)

No browser automation was available this session (Playwright's browser extension was not
installed, and `playwright-core` was not present locally to drive headless Edge the way
prior sessions did). Verification was done at the API level instead: `curl` against the
running app + real MySQL, with a `registrar`-role session cookie.

- `ddl-auto=validate` boot against live MySQL → **PASS** (started in 12.727s). Confirms the
  now-unmapped `student_uploads` table (still present, per decision) does not break startup.
- **Old endpoints confirmed dead:** `POST /api/student/{id}/upload` and
  `GET /api/student/files/{id}` both return an error (not 200) — see the Bug 13 note below
  for why it's a 500 rather than the expected 404.
- **ID picture full round trip:** upload a JPEG → 201 with the right name/type/size →
  `HEAD`/`GET` returns it (`image/jpeg`) → DB shows exactly 1 `documents` row of that type →
  upload a second picture for the same student → same `documentId` returned (replace in
  place, not a second row) → DB count stays at 1.
- **Rejection paths:** a `.pdf` → 400 "Unsupported picture type. Allowed: jpg, jpeg, png,
  webp"; a 2MB+1-byte file → 400 "ID picture exceeds the 2MB size limit"; neither wrote a
  row. Delete → 204, DB count back to 0, subsequent `GET` → 404.
- **Audit trail:** `system_logs` carries "Uploaded ID picture '…' for student …" (×2, one
  per upload) and "Removed ID picture for student …", all under the `registrar` username.
- **Existing document features confirmed unaffected:** PDF upload against a normal type
  still succeeds; `GET /types` still lists `ID Picture (1x1 / 2x2)` (for the Documents page
  filter); `/{id}/view` and `/{id}/download` on the PDF both still work; `/{id}/view` on the
  ID picture itself also works (backing the widened inline-image preview);
  generate-data + TOR generation both succeed.
- **docx/xlsx upload could not be verified** — both fail with a 409 against real MySQL. Root
  cause traced to a pre-existing, unrelated defect: `documents.file_type VARCHAR(50)` is too
  short for the MIME strings `DocumentService.ALLOWED_EXTENSIONS` maps those extensions to
  (73/65 characters). Logged as **Bug 12** in `bugs.md` — not fixed, out of scope.
- **The dead-endpoint 404 check actually returns 500** — traced to a second pre-existing,
  unrelated defect: `GlobalExceptionHandler` has no handler for `NoResourceFoundException`,
  so any genuinely-missing route under a `permitAll()` prefix 500s instead of 404ing.
  Reproduced with an unrelated nonsense path under the same prefix to confirm it predates
  this session. Logged as **Bug 13** — not fixed, out of scope.
- Test student (`SR20260017`) and its 3 documents (ID picture + PDF + generated TOR) were
  all deleted afterward via the app's own cascade-safe delete; live DB confirmed back to its
  pre-session state (10 students).
- The `registrar` seed account's mandatory security-question setup was completed as part of
  this verification (needed to get a REGISTRAR-role session at all — `ROLE_PENDING_SETUP`
  otherwise blocks every `/api/registrar/**` call). This is real onboarding, not test
  pollution, and was left in place rather than reverted.

## Live Verification — 2026-09-19 (schema.sql vs live DB sync)

- Live DB was missing the entire security-questions delta (2 tables, 3 `users` columns, the
  `users.email` UNIQUE index) — the merged migration had never been applied. Applied it;
  live DB 19 → **21 tables**.
- **`ddl-auto=validate` boot against live MySQL caught two bugs the 363-test suite could
  not**: `user_security_answers.slot` and `users.failed_security_attempts` were `TINYINT`
  in the SQL while their JPA fields are `Integer`
  (`found [tinyint], but expecting [integer]`). The Gradle suite runs on H2 with
  `create-drop`, so Hibernate generates the columns itself and never compares them to the
  checked-in SQL. **Lesson: a green suite does not prove `schema.sql` is correct — only a
  `ddl-auto=validate` boot does.** Both fixed to `INT` in the schema, the migration, and via
  two guarded `MODIFY COLUMN` steps for already-migrated databases.
- After the fixes: boot → **PASS** (started in 9.368s, zero schema-validation errors).
- Migration proven idempotent on a throwaway restore of the live backup **before** being run
  against live: byte-identical structure on re-run, `security_questions` stayed at 6 rows.
- Final structural diff (live vs a DB built from the corrected `schema.sql`) → cosmetic only
  (FK/index auto-names, index order, `grades` column order).

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

## Manual Thread Testing Cases (capstone paper)

Two workbooks in `capstonepaper/`, both in the same 9-column format with a
`Pass/Fail/Blocked/Not Tested` dropdown on the Results column:

| Workbook | Cases | Covers |
|---|---|---|
| `OLD ANIHAN Thread Testing Cases.xlsx` | TC-001 … TC-082 | Auth/RBAC, admin user CRUD, activity logs + export, self-service account, registrar student records, subjects, classes, sections, trainer views + the *old* midterm/finals grade input, student portal, error handling, load/performance. Partly filled in by testers on 2026-05-20. |
| `NEW ANIHAN Thread Testing Cases.xlsx` | TC-083 … TC-134 (52 cases) | Student number (single assign + bulk export/import), the TESDA grading overhaul, subject competency type + code rename, trainer account lifecycle guards, access/navigation for the newer pages. Blank and ready for testers. |

Numbering is continuous across the two, so the pair reads as one suite of 134 cases.

**Not covered anywhere yet — document management and document generation.** Cases for the
whole documents module (upload/search/filter/view/download/delete) and template generation
(TOR/Form IX, print fidelity, save & edit-in-place, .docx download) were written on
2026-09-16 and then **removed at the user's request** — out of testing scope for now.
Recoverable from git history on `fix/student-ID-number`. This is the largest known gap in
manual coverage; the automated suite does cover the backend (`DocumentServiceTest`,
`DocumentControllerWebMvcTest`, `HtmlDocxConverterTest`).

**Note:** TC-062–066 in the old sheet describe the pre-2026-08-29 midterm/finals grade
input, which no longer exists — TC-109–121 in the new sheet supersede them.

**When adding a third batch:** continue from TC-135, re-run the duplicate sweep against
both existing workbooks, and generate with
`capstonepaper/generate_thread_tests_part2.py` as the styling reference.

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
- [x] Browser retest: admin login → admin dashboard renders (hero, DataTable, details
      modal, clean console) — confirmed (2026-09-21). Edit-user flow itself (opening
      `edit-user.html` and saving) was not exercised this pass — only the "Edit User" link
      from the details modal was confirmed present and correctly targeted.
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
