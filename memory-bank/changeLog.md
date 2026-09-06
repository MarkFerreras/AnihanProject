# Change Log - Anihan SRMS

## 2026-08-30 - Student Number Export / Import / Report Page
**Branch:** `fix/student-ID-number`

### Task
Make bulk entry of student numbers practical: a report page listing all students with filters
to isolate those still missing a number, export of the filtered set to CSV/Excel for encoding,
and import of the completed sheet back. Editability (single assignment) already shipped on
2026-08-27 and is reused here.

### Design constraint (from the user)
The school's real record format is not yet known, so the file-reading rules must be **easily
editable later**. Everything about how a sheet is recognised lives in ONE file —
`StudentNumberImportMapping` — and the parser/service/controller/UI carry no format knowledge.

### Files Created
| File | Purpose |
|------|---------|
| `service/StudentNumberImportMapping.java` | **The file to edit when the real format arrives.** Header alias lists per column, the match key, `normaliseHeader` (case/punctuation-insensitive), `normaliseValue` (trims, strips Excel's `.0` tail, preserves leading zeros), `HEADER_SCAN_ROWS`, `MAX_DATA_ROWS`, and the canonical export column names. |
| `service/StudentNumberSheetParser.java` | Format mechanics only, no DB access: RFC-4180 CSV splitter (read side of `SystemLogExportService.csvEscape`), XLSX via POI `DataFormatter` (so `2026001` does not come back as `2026001.0`), UTF-8 BOM strip, and header-row auto-detection over the first 10 rows — school files carry title blocks above the real header. |
| `service/StudentNumberExportService.java` | Builds the encoding sheet. Headers on row 1 using the canonical aliases so an export re-imports untouched; Student Number last and blank; written as a **text** cell so `0012` is not eaten by Excel. Modelled on `SystemLogExportService`. |
| `service/StudentNumberExportFormat.java` | CSV/XLSX. Separate from `SystemLogExportFormat`, which is log-scoped and includes non-round-trippable DOCX. |
| `service/StudentNumberImportService.java` | One `classify()` pass shared by preview and apply, so the two cannot diverge. Guard rails follow `DocumentService` (extension whitelist, 5MB cap, empty/invalid-name rejection). |
| `dto/registrar/StudentNumberImportOutcome.java` | Ten outcomes; `applicable()` is the single definition of "this row writes", used by both the preview counts and the apply loop. |
| `dto/registrar/StudentNumberImportRowResult.java`, `StudentNumberImportReport.java` | Per-row detail and the counts/summary. |
| `controller/StudentNumberController.java` | `GET /export`, `POST /import/preview`, `POST /import/apply` under `/api/registrar/student-numbers`. Apply logs one row per assignment plus a summary; preview logs nothing. |
| `static/student-numbers.html` + `static/js/registrar-student-numbers.js` | The report page: filters, "N of M students still need a student number", export, import preview→apply with outcome badges, and the existing Assign Number modal for singles. |
| `test/.../StudentNumberSheetParserTest.java` | 18 tests — the regression net for future mapping edits. |
| `test/.../StudentNumberExportServiceTest.java` | 8 tests incl. CSV/XLSX round-trip back through the parser and leading-zero survival. |
| `test/.../StudentNumberImportServiceTest.java` | 22 tests — every outcome, overwrite on/off, apply writes only applicable rows. |
| `test/.../StudentNumberControllerWebMvcTest.java` | 13 tests — export headers per format, multipart preview/apply, RBAC, logging on apply and **not** on preview. |

### Files Modified
| File | Change |
|------|--------|
| `dto/registrar/AssignStudentNumberRequest.java` | Validation hoisted to `MAX_LENGTH` / `PATTERN` / `ALLOWED_CHARS_MESSAGE` constants used by both its own annotations and the bulk import, so a value the Assign action accepts is exactly one the import accepts. |
| `config/SecurityConfig.java` | `/student-numbers.html` added to the REGISTRAR matcher. (`/api/registrar/**` was already REGISTRAR-only, so the new endpoints needed no change.) |
| `static/registrar.html`, `subjects.html`, `classes.html`, `sections.html`, `documents.html`, `student-records.html`, `generate-document.html` | Registrar navbar 5 → 6 links (Student Numbers). |

### Key behaviours
- **Preview writes nothing** — verified against the live DB and `system_logs`.
- **Apply** writes only applicable rows; the rest are reported. With the preview in front, that
  beats failing 200 good rows over one typo.
- **Never silently overwrite** — an existing *different* number is `CONFLICT_EXISTING` unless
  "Allow overwriting existing numbers" is ticked.
- **A number repeated within one file blocks both rows** — we cannot know which was intended.
- **Name columns warn, never match** — a `NAME_MISMATCH` still applies but is surfaced, which
  catches rows slipping out of alignment in a hand-edited sheet.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 299 tests, 0 failures, 0 errors** (was 238).
- Live round trip against real MySQL: exported the 6 unnumbered students (the already-numbered
  one correctly excluded), encoded a sheet containing every failure mode at once
  (duplicate-in-file pair, number already in use, unknown reference, blank, invalid characters),
  previewed → nothing written, applied → exactly the 2 valid rows written.
- **Excel fidelity via a POI-authored file:** `0012` (leading zero), `2025-777` and `A/2026/03`
  all survived export → edit → import. Header auto-detection found the header under two pasted
  title rows.
- `system_logs`: per-assignment rows + `"Imported student numbers from encoded.csv: 2 assigned,
  5 skipped"` + the export row. No rows from a preview.
- Playwright headless-Edge E2E **22/22**, run twice.
- Live DB restored to its pre-session state; the pre-existing `231472` on record 5 preserved
  throughout. Backup: `src/main/sql/backup-2026-08-30-pre-import-test.sql`.
- **No schema change** — `student_number` already existed.

---

## 2026-08-27 - Registrar-Controlled Student Number (no auto-generation)
**Branch:** `fix/student-ID-number`

### Task
Per the stakeholder meeting: the system must stop auto-generating student numbers. Add a
student number that is nullable, is never invented by the system, can be assigned later by
the archive import, and whose absence is visible to the Registrar.

### Design decision (see decisions.md)
`student_records.student_id` is `NOT NULL UNIQUE` and the **FK target of 10 child tables**;
`record_id` is the PK. Making `student_id` nullable would orphan child rows written before a
number exists. So `student_id` is kept untouched as an internal **Reference No.**, and a new
nullable `student_number` column carries the registrar-controlled value. The meeting note's
"should remain the primary key" is not literally satisfiable (a nullable column cannot be a
SQL PK, and it was never the PK) — a UNIQUE index provides "unique when present".

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/migrations/2026-08-27-add-student-number.sql` | Adds `student_number VARCHAR(20) NULL` + `uq_student_number`. Idempotent; guards match on `COLUMN_NAME`/`NON_UNIQUE`, never a constraint name (the name-based guard caused the 2026-05-19 duplicate-FK bug). Ends with read-only verification queries. |
| `dto/registrar/AssignStudentNumberRequest.java` | Deliberately NOT `@NotBlank` — blank/null means "clear". `@Size(max=20)` + `@Pattern` allowing letters, digits, `-`, `/` (the shapes real archive numbers take). |
| `test/.../RegistrarStudentNumberServiceTest.java` | 11 tests: assign, trim, overwrite, same-number-same-record, blank/null clear, duplicate rejection (target untouched, no save), unknown record, **edit-form update preserves the number**, filter partitioning, search-by-number. |
| `test/.../RegistrarStudentNumberControllerWebMvcTest.java` | 9 tests: assign 200 + log, clear 200 + log, null accepted, duplicate 400, bad chars 400 (field error), too long 400, 404, 403 trainer, 401 anonymous. |

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/schema.sql` | `student_number` column + `uq_student_number` on `student_records`; header dated; comment explains the two-identifier split. Seed INSERT uses an explicit column list, so sample students correctly start with NULL. |
| `model/StudentRecord.java` | `studentNumber` field + accessors, with Javadoc distinguishing it from the internal `studentId`. |
| `repository/StudentRecordRepository.java` | `findByStudentNumber` for the uniqueness pre-check. |
| `dto/registrar/StudentRecordSummaryResponse.java`, `StudentRecordDetailsResponse.java` | `studentNumber` added and mapped. |
| `service/RegistrarService.java` | `assignStudentNumber()` (trim via existing `emptyToNull`, blank→clear, uniqueness pre-check throwing an actionable `IllegalArgumentException` → 400 rather than a generic 409 from the index); 5-arg `getAllRecords` with `hasStudentNumber` (4-arg delegates, mirroring the `status` filter); `matchesQuery` now also matches the number. |
| `controller/RegistrarController.java` | `PUT /{recordId}/student-number` (logs "Assigned student number X to: …" / "Cleared student number for: …"); `list()` accepts `hasStudentNumber`. |
| `static/registrar.html` | "Student ID" → "Reference No."; new "Student Number" column + detail card; Student No. filter select; `#assignStudentNumberModal`; page-scoped `.record-actions .btn` compact sizing and a `flex-wrap` override for the filter bar; JS `?v=4`. |
| `static/js/registrar-students.js` | `renderStudentNumber` (warning badge when absent — a missing number is an action item, not merely absent data); Assign Number button in a `record-actions` group; details handler narrowed to `.js-open-details`; `hasStudentNumber` in `buildAjaxUrl` + Reset; `openAssignNumberModal` / `setupAssignStudentNumber` (PUT, inline errors, Enter-to-save, table reload). |
| `static/student-records.html` + `js/registrar-student-records-edit.js` | Identifiers row now 4 columns: Record ID, Reference No., **read-only** Student Number (with a pointer to the Assign action), Status. Populated but never sent in `buildPayload` — the edit form must not be a second write path. JS `?v=4`. |
| `static/student-details.html` | Submitted banner relabelled "Reference No." with a line telling the student the Registrar assigns their student number. |
| `test/.../RegistrarBulkLoadWebMvcTest.java` | Stubs updated to the 5-arg `getAllRecords`; fixture gives every third student a null number; new test asserting `?hasStudentNumber=false` is forwarded. |

**Deliberately unchanged:** `StudentDetailsService.generateStudentId()` and the
"Student ID cannot be changed." guard. The internal reference is still generated and still
immutable; only the new column is registrar-owned.

### Two frontend bugs found by the browser E2E
1. **86px of horizontal table scroll at 1280px.** The table itself fit (1045px in 1069px) —
   the culprit was `dashboard.css:683` pinning
   `#studentRecordsTable_wrapper #batchFilterBar .logs-filter-section` to `flex-wrap: nowrap`;
   the added dropdown pushed that bar to 1155px. Fixed with a page-scoped `flex-wrap: wrap`
   below 1400px. Now 0px overflow at 1280/1440/1920.
2. The existing details handler bound to `button[data-record-id]` — which the new Assign
   button also matched. Narrowed to `button.js-open-details`.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 238 tests, 0 failures, 0 errors** (was 217).
- Migration applied to live MySQL, then re-run: `SHOW CREATE TABLE student_records`
  byte-identical, exactly one unique index on the column. Two NULL rows coexist; a duplicate
  real value is rejected with `ERROR 1062`.
- `ddl-auto=validate` boot against live MySQL → **PASS** (started in 8.86s, all 19 entities).
- Playwright headless-Edge E2E **18/18**, run twice.
- Live API smoke: assign, duplicate → 400 with the naming message, invalid chars → field-level
  400, clear, 404, both filters, combined filter, search-by-number. `system_logs` rows present;
  a rejected duplicate writes no log row.
- Live DB restored to its pre-session state (all 7 students NULL). Backup:
  `src/main/sql/backup-2026-08-27-pre-student-number.sql`.

---

## 2026-07-14 PM #2 - Document Management Polish: Print, Logo, Filenames, DOCX, Delete/Edit
**Branch:** `fix/generate-document-student-picker`

### Task
Six approved polish items on the Document Management module: (1) print formatting —
no grey backdrop/chrome, 1-page templates print as exactly 1 page; (2) school logo in
the generated document header; (3) auto PDF/download filenames
"{ShortType}-{LastName} {FirstName}"; (4) documents.html Actions column fit; (5)
generated HTML documents download as editable Word .docx; (6) DELETE endpoint +
type-to-confirm modal, view-modal Edit that re-opens a saved document for re-editing,
generate-page Cancel + post-save redirect.

### Files Created
| File | Purpose |
|------|---------|
| `service/HtmlDocxConverter.java` | Wraps stored self-contained HTML into a minimal OOXML package whose document.xml references the HTML as an altChunk — Word converts it to editable content on open. Pure `java.util.zip`, no new dependency, air-gap safe. A4 sectPr matches the print CSS margins. |
| `static/js/anihan-logo.js` | `window.AnihanLogo.DATA_URI` — base64 data URI of images/logo.png (17KB) so generated/saved documents stay fully self-contained. |
| `test/.../HtmlDocxConverterTest.java` | 4 tests: OOXML parts present, original bytes preserved as the chunk part, altChunk references wired, empty-content rejection. |

### Files Modified
| File | Change |
|------|--------|
| `service/DocumentService.java` | New `delete(id)` (fetch → summary → delete, 404 via `NoSuchElementException`); new `prepareDownload(id)` + `DownloadPayload` record — `text/html` documents convert to docx named "{ShortType}-{Last} {First}.docx" (`TYPE_SHORT_NAMES` mirrors curriculum-templates.js; blank-name fallback to studentId), uploads pass through unchanged; `saveGenerated` gained a 5-arg overload with `documentId` for update-in-place, guarded by student ownership **and** text/html fileType (an API call must not overwrite an uploaded PSA scan — code-review finding). |
| `controller/DocumentController.java` | `DELETE /{documentId}` → 204 + "Deleted document…" system_logs row; download uses `prepareDownload` and logs the delivered filename; `generate` passes `documentId` and logs "Updated generated document…" on edits; `fileResponse` refactored to (name, type, bytes, inline). |
| `dto/registrar/GenerateDocumentRequest.java` | Optional `Integer documentId` (null = create, present = update in place). |
| `css/document-print.css` | Header now flex with `.doc-school-logo` (62px). Print rules rewritten: white body, chrome hidden, no sheet shadow, `body { display:block; min-height:0 }` (**root cause of the page-2 spill: Chromium cannot fragment flex items**), tighter margins/fonts, `page-break-inside: avoid` on table rows/signatories/certification/footer, `@page 9mm 11mm`. |
| `static/js/registrar-generate-document.js` | Logo in `SCHOOL_HEADER` (guarded `LOGO_URI` — degrades logo-less if the script fails); `printDocument()` swaps `document.title` to the sanitized filename around `window.print()` (afterprint + 2s fallback restore); edit mode (`?documentId=`) fetches the saved HTML, extracts `.document-sheet`, re-arms contenteditable, locks setup controls; save passes `documentId`, redirects to documents.html on success; Cancel button handler. |
| `static/js/registrar-documents.js` | Actions render: flex-nowrap group with View/Download/Delete (btn-sm, data-student/-type attrs); view modal shows Edit only for `text/html` docs, linking to the generate page edit mode; `setupDelete()` — type-"delete"-to-confirm modal flow, DELETE call, table refresh. |
| `static/documents.html` | Page-scoped `.document-actions .btn` size fix (dashboard.css `.btn-surface` padding overrides Bootstrap `.btn-sm`); `#viewDocumentEditBtn`; `#deleteDocumentConfirmModal` (registrar.html pattern); JS `?v=2`. |
| `static/generate-document.html` | `anihan-logo.js` include; Cancel button beside Save; JS `?v=3`. |
| `test/.../DocumentServiceTest.java` | +7 tests: delete (success/missing), prepareDownload (docx conversion + friendly name, uploads unchanged), update-in-place (success, wrong student, uploaded-file rejection). |
| `test/.../DocumentControllerWebMvcTest.java` | +6 tests: DELETE 204+log / 404 / 403 trainer / 401 anon; docx download headers; generate-with-documentId logs "Updated generated document". |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md` | Session notes. |

### Print Verification Result (real print-to-PDF, headless Edge `page.pdf()`)
All four Form IX variants print as **exactly 1 page**; the TOR prints as **2 dense
pages** — its 57 fixed subject rows measure ~1718px against ~1054px of usable A4, so
one page is physically impossible; breaks now fall cleanly between table rows with no
near-empty trailing page. No grey backdrop, no app chrome, logo present.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 217 tests, 0 failures, 0 errors** (was 200).
- Playwright headless-Edge E2E **30/30**: print-media assertions, PDFs (page counts +
  visual check of the rendered PDF), title swap/restore ("TOR-Ferreras Mark"), actions
  contained at 1400px and 992px, docx download (zip magic + `word/afchunk.html` +
  Content-Disposition), edit → re-save in place (marker persisted, no duplicate row),
  cancel + post-save redirects, type-to-confirm delete removes the row.
- `system_logs` (live MySQL): Generated / Downloaded ('TOR-Ferreras Mark.docx') /
  Updated generated / Deleted rows present. E2E's own test document deleted itself.
- /code-review on the diff: 3 findings (uploaded-file overwrite guard, blank-name docx
  filename, hard `window.AnihanLogo` dereference) — all fixed with tests.
- E2E harness note: `emulateMedia({media:'screen'})` is sticky and overrides
  `page.pdf()`'s default print media — reset with `media: null`.

---

## 2026-07-14 PM - Generate-Document Student Picker + Load-Failure Diagnosis + Record Cleanup
**Branch:** `fix/generate-document-student-picker`

### Task
(1) Replace the native `<datalist>` student list on the Generate Document page with a
dropdown that can also be typed into to search; (2) explain the "Failed to load student
data" error and the requirements for generating a document; (3) clean up duplicate and
incomplete student records in the live database.

### Files Modified
| File | Change |
|------|--------|
| `static/generate-document.html` | `<input list>` + `<datalist id="studentsDatalist">` replaced with `#studentPicker` combobox (input `role="combobox"` + Bootstrap `.dropdown-menu` `#studentPickerMenu`, 300px scrollable). JS cache-buster `?v=1` → `?v=2`. Follow-up fix: `.surface-card` has `overflow: hidden` in dashboard.css, which clipped the open picker menu at the card edge - added a page-scoped `overflow: visible` override plus `min-width: max-content` on the menu so long names are not truncated (verified by browser hit-test past the card boundary). Second follow-up (user feedback: full list too tall): menu capped at exactly 5 visible rows (items fixed at 2.5rem, menu max-height 13.5rem = 5 rows + padding) with overflow-y scroll for the rest; flex layout + 0.3rem gap keeps the "ID - Name" spacing. Browser-verified: 5/10 items visible, last item reachable by scroll. |
| `static/js/registrar-generate-document.js` | `loadStudentsDatalist()` → `setupStudentPicker()` (open on focus, live filter on ID/last/first name, ArrowUp/Down + Enter + Escape keyboard nav, mouse select, outside-click close, "Loading students…" placeholder + re-render when the student list arrives). New `resolveStudentId()` (exact-ID match, else unique search match, else friendly alert). New `ajaxErrorMessage()` used by load + save error paths: prefers server JSON message, else distinguishes network (status 0), 401 session-expired, 404 stale-build, and other HTTP statuses. |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md` | Session notes. |

### Diagnosis - "Failed to load student data"
The endpoint works: `GET /api/registrar/documents/generate-data/SR20260016` → HTTP 200
with the full auto-fill payload (student, parents, education, TESDA, OJT, grades). The
only hard requirement is that the student ID exists in `student_records`; missing
course/section/grades just render as editable blanks. The generic alert appears only when
the error response carries no JSON `message` — a 404 from a server still running a build
that predates the document-management merge (PR #49). Fix for operators: restart the app
after pulling. The hardened error handler now names that condition explicitly.

### Database Changes (live `AnihanSRMS`, backup taken first)
Deleted 4 student records via `DELETE /api/registrar/student-records/{recordId}`
(cascade-safe service path; each write logged to `system_logs`):
| Record | Reason |
|--------|--------|
| SR20260009 Wong, Angelica | Duplicate of SR20260008 (same name + middle initial); all-NULL `Enrolling` stub |
| SR20260010 Avellaneda, Keith | Abandoned `Enrolling` stub — no birthdate/sex/contact/batch/course/section, zero child rows |
| SR20260011 Mark, Mark | Same |
| SR20260017 test125, test125 | Same |

Left in place pending user decision: SR20260005 (dwd, wdw), SR20260007 (dwadwa, dwadad —
has grade data), SR20260013 (fff, fff) — fake-name test fixtures but `Active` with sections.

### Verification
- Headless Edge E2E (playwright-core driving system Edge): 10/10 checks pass — login,
  picker opens on focus (10 students), filter "lipata" → 1 match, keyboard select fills
  SR20260016, TOR renders with auto-filled data, mouse select works, unknown text shows
  friendly error. The E2E run caught and led to fixing a focus-before-load race.
- `./gradlew test` → **200 tests, 0 failures, 0 errors** (frontend-only change).

---

## 2026-07-14 - Live DB vs schema.sql Comparison and Sync
**Branch:** `main` (user explicitly kept work on main - DB-only task)

### Task
Compare the live `AnihanSRMS` MySQL database against `src/main/sql/schema.sql`, find any
discrepancies, and update the live DB so it works with the latest project schema.

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/backup-2026-07-14.sql` | New full `mysqldump` backup taken before any inspection (untracked). |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md` | Session notes. |

No application code, entity, or SQL source file was changed. **No change was applied to the
live database** - it already matched `schema.sql`.

### Comparison Result - no functional drift
Built a throwaway `schema_check` DB from `schema.sql`, dumped `--no-data` structures of both,
normalized (stripped `AUTO_INCREMENT=` + comments), and diffed. Only cosmetic differences,
all verified non-functional:
- **`grades` column order** differs in the raw dump; sorted column-by-column the two are
  byte-identical (name/type/nullability/default all match). Order is irrelevant to SQL/Hibernate.
- **`grades` class FK name:** live `fk_grades_class` vs schema.sql auto-name `grades_ibfk_3` -
  same `class_id -> classes ON DELETE SET NULL` relationship, name only.
- **`users` unique key name:** live `uq_username` vs schema.sql `username` - same UNIQUE, name only.
- **`courses`** already holds `CARS`, `BPRO`, `FSERV` (the 2026-07-09 seed gap remains closed).

### Compatibility Verification
- **`ddl-auto=validate` boot against live MySQL -> PASS** (`Started SpringbootApplication in
  11.256s`, all 19 entities validated, zero `HHH` schema-validation errors). Authoritative check;
  the Gradle test suite runs on in-memory H2 and cannot detect live-DB drift.
- FK integrity sweep -> 0 orphaned rows across `grades`, `class_enrollments`, `subjects`,
  `classes`.
- Live schema unchanged (19 tables). Throwaway `schema_check` DB dropped afterward.

### Note - schema.sql seed blocks intentionally not re-applied
Live is the real working DB (14 students, 6 classes, 264 `system_logs` rows). `schema.sql`'s
fresh-install seeds (3 test accounts, 5 sample students) were deliberately skipped - re-running
them would duplicate/corrupt live data (the load also errored on a duplicate `admin` username,
confirming the guard).


## 2026-07-09 - Document Management (R3.1–R3.7) + TOR/Form IX Generation
**Branch:** `feature/document-management`

### Task
Implement the registrar Document Management module — Jira AGILE-75…AGILE-81 (R3.1 Upload,
R3.2 Type, R3.3 Name, R3.4 View, R3.5 Search, R3.6 Filter, R3.7 Download) — plus
auto-filled, editable, print-ready generation of the four official templates in
`document-templates/` (TOR; Form IX for BPP / Cookery / FBS, each with Records-of-
Candidate-for-Graduation and Student's-Permanent-Record variants). Confirmed decisions:
print-ready HTML output, auto-fill + editable review, generated documents persisted to
the `documents` BLOB table with `system_logs` entries. No DB migration required.

### Files Created
| File | Purpose |
|------|---------|
| `repository/DocumentRepository.java` | `searchSummaries` JPQL constructor-expression projection (never selects `content_data`); explicit LEFT JOINs so null batch/section students survive the optional filters |
| `dto/registrar/DocumentSummaryResponse.java` | BLOB-free listing row |
| `dto/registrar/DocumentGenerateDataResponse.java` | Aggregated auto-fill payload (student + parents + education + TESDA + OJT + grades) |
| `dto/registrar/GenerateDocumentRequest.java` | `@NotBlank` studentId/documentType/fileName/html |
| `service/DocumentService.java` | Upload (extension+MIME whitelist pdf/docx/xlsx, 10MB cap, student + type validation), generated-HTML save (`text/html`, `.html` appended), type list, BLOB fetch for view/download |
| `service/DocumentGenerationService.java` | Builds the generate-data payload from 6 repositories |
| `controller/DocumentController.java` | `GET /api/registrar/documents` (q/type/batchCode/sectionCode), `GET /types`, `POST` multipart, `GET /{id}/download` + `GET /{id}/view`, `GET /generate-data/{studentId}`, `POST /generate`; upload/download/generate write `system_logs` |
| `static/documents.html` | Documents page — DataTable, upload modal (student datalist), iframe view modal, filter bar |
| `static/js/registrar-documents.js` | Table + debounced search, filters, FormData upload, PDF/HTML inline preview (docx/xlsx download-only) |
| `static/generate-document.html` | Template picker (TOR / Form IX × variant) + editable in-document form |
| `static/js/registrar-generate-document.js` | Renders the document as the fillable form (contenteditable spans), auto-fills from generate-data, Print (window.print), Save (self-contained HTML with inlined CSS → POST /generate) |
| `static/js/curriculum-templates.js` | Full curriculum transcription from the PDFs (~50 subjects with fixed hours/units, grading legend); grades merged by subject_code at render time |
| `static/css/document-print.css` | Document layout + `@page`/`@media print` rules; embedded into saved HTML |
| `test/.../DocumentServiceTest.java` | 10 Mockito tests (whitelist, size, unknown student/type, generated save, filter normalization) |
| `test/.../DocumentGenerationServiceTest.java` | 3 Mockito tests (aggregation, null OJT, missing student) |
| `test/.../DocumentControllerWebMvcTest.java` | 11 WebMvc tests (RBAC 401/403, multipart 201+log, 400 paths, download/view headers, generate) |
| `docs/superpowers/plans/2026-07-09-document-management-r3.md` | Approved implementation plan |

### Files Modified
| File | Change |
|------|--------|
| `config/SecurityConfig.java` | Registrar matcher += `/documents.html`, `/generate-document.html`; `X-Frame-Options` DENY → **SAMEORIGIN** (View modal iframe was blocked by the default — found via live smoke test) |
| `exception/GlobalExceptionHandler.java` | `MaxUploadSizeExceededException` → 400 with friendly message |
| `static/registrar.html`, `subjects.html`, `classes.html`, `sections.html`, `student-records.html` | Registrar navbar 4 → 5 links (Documents added) |

### Design Decisions
- **Curriculum is static template data, not `subjects` rows.** The printed documents carry
  ~50 fixed subjects with decimal units; `subjects.units` is INT and seeding would entangle
  class management. Deferred as a follow-up (units DECIMAL migration + seed).
- **The document is the form.** Instead of a separate form panel bound to a preview, every
  blank is a contenteditable span inside the print-faithful layout — what you edit is what
  prints and what gets saved.
- **Saved documents are self-contained HTML** so `GET /{id}/view` renders them in the iframe
  with zero extra dependencies and reprints keep their styling.
- **View is not logged; upload/download/generate are** — mirrors the trainer read-only
  no-logging precedent.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 200 tests, 0 failures, 0 errors** (was 176).
- Live smoke test (running app + real MySQL): login → types → upload → list → `?q=` search →
  type+batch filter → wrong-section filter (0 rows) → download (attachment, bytes intact) →
  view (inline, SAMEORIGIN) → generate-save (`.html` appended, `text/html`) → `system_logs`
  rows for upload/download/generate confirmed. Smoke-test document rows deleted afterwards.

---

## 2026-07-09 - Live DB vs SQL Files Comparison & Sync
**Branch:** `main` (user explicitly approved working on `main`)

### Task
Compare the live `AnihanSRMS` MySQL database against the most recent SQL files in
`src/main/sql/`, apply whatever the live DB was missing, then check for compatibility
issues and discrepancies between the database and the application code.

### Files Modified
| File | Change |
|------|--------|
| `memory-bank/activeContext.md` | New session entry: comparison findings, action taken, verification matrix |
| `memory-bank/progress.md` | Added this session under Recent Sessions |
| `memory-bank/changeLog.md` | This entry |

No application code, entity, or SQL source file was changed — the live database was the
only thing brought into sync.

### Database Changes Applied
| Statement | Purpose |
|-----------|---------|
| `src/main/sql/migrations/2026-05-10-seed-courses-and-batch.sql` | Seeded the two missing courses `BPRO` (Bread and Pastry Production) and `FSERV` (Food and Beverage Services). `CARS` and batch `B2026A` already existed and were left untouched by `INSERT IGNORE`. |

A full `mysqldump` backup was taken before applying anything.

### Comparison Result
- **Structure: no drift.** `mysqldump --no-data` of the live DB matches `schema.sql` exactly —
  19 tables, identical columns, types, nullability, indexes, and foreign keys. Every prior
  migration (`2026-05-05`, both `2026-05-09` files, `2026-05-19`, `2026-05-20`) was already
  reflected in the live schema.
- **Data: one gap**, the 2026-05-10 course seed, now applied.
- Structure re-dumped after the migration and diffed against the pre-migration dump: identical,
  confirming the change was data-only.

### Compatibility Verification
- **`ddl-auto=validate` boot against live MySQL → PASS.** Hibernate compared all 19 entities to
  the live tables and started cleanly in 9.0s. This is the authoritative compatibility check;
  the Gradle test suite runs on in-memory H2 (`ddl-auto=create-drop`) and therefore cannot
  catch live-DB drift.
- `./gradlew test` → 176 tests, 0 failures, 0 errors.
- Referential-integrity sweep → 0 orphaned rows across `grades`, `class_enrollments`,
  `sections`, and `subjects`.
- Domain-invariant sweep → every `classes.trainer_id` references a `ROLE_TRAINER` user;
  all grades sit inside the `[1.0, 5.0]` range that `TrainerGradeService` enforces;
  every `Active` student has a `section_code`.

### Discrepancy Noted → Fixed in the same session (see next entry)
`src/main/sql/migrations/2026-05-19-grades-restructure.sql` contained only `DESCRIBE`/`SHOW`
verification queries — its `ALTER TABLE` statements were commented out. It documented the
grades restructure rather than applying it.

---

## 2026-07-09 - Grades-Restructure Migration Made Functional + FK Idempotency Fix
**Branch:** `main`

### Task
Fix the flag raised above: make `2026-05-19-grades-restructure.sql` actually perform the
restructure instead of only verifying it, without breaking already-migrated databases.

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/migrations/2026-05-19-grades-restructure.sql` | Rewritten. Commented-out `ALTER`s replaced with real, guarded statements. Adds `class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at`; relaxes `final_grade`/`hours_studied`/`remarks` to NULL; adds `fk_grades_class` + `uq_grade_student_class`. Aborts before any change if the `classes` FK target is missing. Verification queries retained at the end. |
| `src/main/sql/migrations/2026-05-20-sync-and-clear-students.sql` | Fixed the same latent FK-guard bug in section A4 (`subjects.trainer_id`) and A5 (`grades.class_id`). |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md` | Session notes. |

No application code, entity, or `schema.sql` change. The live database ends the session
structurally identical to how it started.

### Why the ALTERs were commented out
The design spec (`docs/superpowers/specs/2026-05-19-trainer-grading-design.md`) wrote them as
`ALTER TABLE grades ADD COLUMN IF NOT EXISTS ...` — valid in MariaDB and Postgres, **not** in
MySQL 8. Rather than translate, they were disabled. The rewrite uses the guarded
`information_schema` + `PREPARE`/`EXECUTE` pattern already used by the 2026-05-20 migration,
which is the project's established idiom for idempotent DDL on MySQL 8.

### Second Bug Found While Testing (the important one)
The FK guards keyed off the **constraint name** `fk_grades_class`. But a database created from
`schema.sql` declares that FK inline and unnamed, so MySQL auto-names it `grades_ibfk_3`.
The name-only check therefore concluded "no FK present" and issued `ADD CONSTRAINT` — creating
a **duplicate foreign key on `grades.class_id` on every single re-run**. The migration was not
idempotent despite claiming to be.

Reproduced against the live database (a duplicate `fk_grades_class` really did appear; it was
dropped immediately). Fixed by matching on what the constraint *does* rather than what it is
called:

```sql
SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
  AND COLUMN_NAME = 'class_id' AND REFERENCED_TABLE_NAME = 'classes';
```

The identical name-based guard in `2026-05-20-sync-and-clear-students.sql` (for both
`grades.class_id` and `subjects.trainer_id`) had the same defect and was corrected the same way.

### Design Note — why not `SIGNAL` for the precondition
`SIGNAL` is not supported inside the prepared-statement protocol (`ERROR 1295`). The
precondition check instead selects from a deliberately non-existent table whose name carries
the operator instruction, so a missing `classes` table aborts the script on its first statement
with `ERROR 1146: Table '...ABORT_classes_missing_run_2026_05_09_migration_first' doesn't exist`.
MySQL identifiers cap at 64 characters, which is why the name is terse.

### Verification
Tested on all three paths a migration can meet:

| Scenario | Result |
|----------|--------|
| Legacy pre-restructure `grades` (no `class_id`, legacy `NOT NULL` cols) | Applies all 5 columns, relaxes 3 columns, creates FK + unique key |
| Re-run on the migrated legacy DB | Structure byte-identical — idempotent, no duplicate FK |
| Re-run twice on the live DB | Structure byte-identical to the pre-fix baseline; exactly 3 FKs; grade row intact |
| `classes` table absent | Aborts on statement 1; `grades` completely unmodified (no half-apply) |
| Hibernate `ddl-auto=validate` vs live MySQL | PASS |

Throwaway schemas `legacy_test` and `noclasses_test` were used for the first four and dropped
afterwards; only `AnihanSRMS` remains.

---

## 2026-05-21 - Bugfix Audit Remediation
**Branch:** `main`

### Task
Execute all 10 items from `docs/superpowers/plans/2026-05-21-bugfix-audit-remediation.md`.

### Files Modified
| File | Change |
|------|--------|
| `repository/StudentRecordRepository.java` | List return on name-lookup; `deleteClassEnrollmentsByStudentId` native query; `findMaxStudentIdWithPrefix` |
| `service/RegistrarService.java` | Delete cascades class enrollments; Student ID change rejected; middleName uses emptyToNull |
| `service/StudentDetailsService.java` | startOrResume list+filter+throws; load guards non-Enrolling; generateStudentId uses MAX+1 |
| `controller/StudentDetailsController.java` | start returns 409 on duplicate name |
| `controller/StudentPortalController.java` | checkDuplicate uses stream on List |
| `service/TrainerGradeService.java` | Grade range validation [1.0–5.0]; unconditional finalGrade |
| `service/StorageService.java` | studentId whitelist guard before file I/O |
| `service/AdminService.java` | Default email derives from username |
| `config/SecurityConfig.java` | @EnableMethodSecurity added |
| `model/StudentRecord.java` | middleName nullable=false removed |
| `model/StudentUpload.java` | kind 20→30, file_path 512→500, optional columns relaxed |
| `model/StudentEducation.java` | level/grade_year/semester/ended_year lengths corrected |
| `model/StudentSchoolYear.java` | sy_start/sem_start/sy_end/sem_end 10→20 |
| `model/StudentTesdaQualification.java` | result 25→50 |
| `model/StudentOjt.java` | hours_rendered precision 6→8 |
| `dto/registrar/StudentRecordUpdateRequest.java` | @NotBlank removed from middleName |
| `static/student-records.html` | editStudentId readonly; editMiddleName required removed |
| `CLAUDE.md` | CSRF statement corrected |
| `test/.../RegistrarBulkLoadTest.java` | 2 new tests |
| `test/.../StudentDetailsServiceTest.java` | 3 new tests + stub updates |
| `test/.../TrainerGradeServiceTest.java` | 2 new tests |
| `test/.../AdminServiceTest.java` | 1 new test + import |

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 176 tests, 0 failures, 0 errors**

---

## 2026-05-21 - Database Schema Sync & Grades Restructure
**Branch:** `main` (stayed on `main` per user's instruction)

### Task
Synchronize the live MySQL database (`AnihanSRMS`) running in Docker with the canonical `schema.sql` to resolve structural column mismatches, apply the `2026-05-19-grades-restructure.sql` schema verification, seed 5 dummy student records, and verify backend test suite integrity.

### Files Modified
| File | Change |
|------|--------|
| `memory-bank/activeContext.md` | Documented database sync items, verified grades table schema layout, and current branch state. |
| `memory-bank/progress.md` | Added database sync details under Recent Sessions. |
| `memory-bank/changeLog.md` | This entry. |

### Database Schema Updates
- **`AnihanSRMS` database dropped and recreated** to perform a clean, error-free reinstall of the schema.
- **Imported `schema.sql`**, resolving all structural column mismatches:
  - `batches.batch_year` changed from `smallint` to `year`.
  - `student_school_years.sy_start`, `.sem_start`, `.sy_end`, `.sem_end` expanded from `varchar(10)` to `varchar(20)`.
  - `student_tesda_qualifications.result` expanded from `varchar(25)` to `varchar(50)`.
  - `student_uploads.kind` expanded from `varchar(20)` to `varchar(30)`, and `.file_path` shortened from `varchar(512)` to `varchar(500)`.
  - `student_education.level` expanded from `varchar(10)` to `varchar(50)`, `.grade_year` shortened from `varchar(255)` to `varchar(50)`, `.semester` shortened from `varchar(255)` to `varchar(20)`, and `.ended_year` expanded from `varchar(7)` to `varchar(20)`.
  - `student_ojt.hours_rendered` expanded from `decimal(6,2)` to `decimal(8,2)`.
  - **`grades` restructured:** Incorporated `class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at` columns, changed `hours_studied` to `decimal(5,2)` and `remarks` to nullable, and added the unique constraint `uq_grade_student_class` and foreign key constraint `fk_grades_class`.
- **Seeded 5 dummy student records** and lookup/user data successfully.

### Verification
- Ran the query checks in `2026-05-19-grades-restructure.sql` to confirm that all restructured grades table columns, indexes, and constraints are in place.
- `./gradlew test` → **BUILD SUCCESSFUL — 166 tests, 0 failures, 0 errors**.

---

## 2026-05-18 - Trainer Read-Only Views (AGILE-123 / AGILE-124)
**Branch:** `feature/trainer-view-subjects-classes`

### Task
Implement the trainer-facing read-only subject and class views. Trainers can see which subjects they are assigned to teach (with enrolled counts and section names), drill into the student roster per subject, view their class list, and drill into the roster per class. No DB schema changes — trainer assignment already exists on `classes.trainer_id`. No `system_logs` writes — all endpoints are read-only GETs.

### Files Created
| File | Purpose |
|------|---------|
| `dto/trainer/TrainerSubjectResponse.java` | Subject summary: code, name, qualification, units, enrolledCount, sectionNames[], courseNames[] |
| `dto/trainer/TrainerSubjectStudentResponse.java` | Per-subject student: studentId, lastName, firstName, middleName, sectionCode, sectionName |
| `dto/trainer/TrainerClassResponse.java` | Class summary: classId, sectionCode, sectionName, subjectCode, subjectName, courseName, semester, enrolledCount |
| `dto/trainer/TrainerClassStudentResponse.java` | Per-class student: studentId, lastName, firstName, middleName |
| `service/TrainerService.java` | Business logic — resolveCurrentTrainerId(), getMyAssignedSubjects(), getStudentsForSubject(), getMyClasses(), getStudentsForClass() |
| `controller/TrainerController.java` | 4 GET endpoints under `/api/trainer/` |
| `test/.../TrainerServiceTest.java` | 12 Mockito service tests |
| `test/.../TrainerControllerWebMvcTest.java` | 9 WebMvc controller tests |
| `static/trainer-subjects.html` | Subjects DataTable page with inline student roster panel |
| `static/js/trainer-subjects.js` | Subjects DataTable + click-to-load roster AJAX |
| `static/trainer-classes.html` | Classes DataTable page with inline student roster panel |
| `static/js/trainer-classes.js` | Classes DataTable + click-to-load roster AJAX |

### Files Modified
| File | Change |
|------|--------|
| `repository/SchoolClassRepository.java` | Added `findByTrainerUserId(Integer)` and `findByTrainerUserIdAndSubjectSubjectCode(Integer, String)` |
| `config/SecurityConfig.java` | Trainer HTML matcher extended: `/trainer-subjects.html` and `/trainer-classes.html` added |
| `static/trainer.html` | Upgraded to full dashboard pattern: `navbar-expand-lg` with 3-link nav (Home / My Subjects / My Classes), welcome hero section, quick-link cards, jQuery script import |

### Design Decisions
- **Trainer assignment is class-level only.** `Subject.trainer_id` is a registrar-side default and is not used to filter trainer views. The trainer sees only classes where `classes.trainer_id = currentUserId`.
- **`resolveCurrentTrainerId()` is package-private** to allow `@WithMockUser` stubbing in WebMvc tests without exposing it as a public API.
- **Subjects view groups by subjectCode** using `LinkedHashMap` to preserve insertion order, sums enrolled counts across all classes for that subject, and collects distinct section/course names.
- **Ownership guard in `getStudentsForClass()`** throws `IllegalArgumentException` (→ HTTP 400) rather than returning an empty list silently.
- **No `system_logs` writes** — all 4 endpoints are read-only views.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 156 tests, 0 failures, 0 errors**.

---

## 2026-05-15 - Section Student Management + Bulk Class Enrollment (AGILE-164 / AGILE-165)
**Branch:** `feature/section-class-enrollment`

### Task
Implement section-level student management (assign/remove students from sections) and bulk enrollment of a whole section into a class. No schema changes required — uses existing `student_records.section_code` FK and `student_status` column.

### Files Created
| File | Purpose |
|------|---------|
| `dto/registrar/UpdateSectionRequest.java` | `@NotBlank @Size(max=25) String sectionName` — PUT body for section rename |
| `dto/registrar/SectionStudentResponse.java` | Roster DTO: `studentId`, `lastName`, `firstName`, `middleName`, `studentStatus` |
| `dto/registrar/EligibleSectionStudentResponse.java` | Picker DTO: `studentId`, `lastName`, `firstName`, `batchCode`, `batchYear`, `courseCode`, `courseName` |
| `dto/registrar/AssignStudentsToSectionRequest.java` | `@NotEmpty List<@NotBlank String> studentIds` |
| `dto/registrar/SectionAssignmentResultResponse.java` | `int assignedCount`, `List<String> skippedStudentIds`, `List<String> reasons` |
| `dto/registrar/BulkEnrollSectionResponse.java` | `int enrolledCount`, `int skippedAlreadyEnrolled`, `int skippedIneligible`, `int totalConsidered` |
| `test/.../ClassManagementSectionServiceTest.java` | 13 Mockito service tests for all 6 new service methods |
| `test/.../ClassManagementSectionControllerWebMvcTest.java` | 7 WebMvc controller tests |

### Files Modified
| File | Change |
|------|--------|
| `repository/StudentRecordRepository.java` | Added 5 derived finders: `findBySectionSectionCode`, `findBySectionIsNullAndStudentStatusIgnoreCase`, and 3 variants adding batch/course filters |
| `repository/ClassEnrollmentRepository.java` | Added `deleteByStudentAndSectionCode` JPQL `@Modifying @Transactional @Query` |
| `service/ClassManagementService.java` | Added 6 methods: `updateSection`, `getStudentsInSection`, `getEligibleStudentsForSection`, `assignStudentsToSection`, `removeStudentFromSection`, `bulkEnrollSectionIntoClass` |
| `controller/ClassManagementController.java` | Added 6 endpoints: `PUT /sections/{code}`, `GET /sections/eligible-students`, `GET /sections/{code}/students`, `POST /sections/{code}/students`, `DELETE /sections/{code}/students/{studentId}`, `POST /classes/{classId}/enroll-section` — all write `system_logs` |
| `static/sections.html` | Added `#editSectionModal` and `#manageSectionModal` (tabbed: current roster + eligible picker with batch/course filters); cache-buster `?v=3` |
| `static/js/registrar-sections.js` | Full rewrite: 3-button Actions column; `setupEditSection()`, `setupManageStudents()`, `refreshCurrentStudents()`, `refreshEligibleStudents()`, `loadFilterDropdowns()` |
| `static/classes.html` | Added Bulk Enrollment block (`#enrollWholeSectionBtn` + `#enrollSectionAlert`) in `#enrollStudentModal`; cache-buster `?v=3` |
| `static/js/registrar-classes.js` | `setupEnrollment()` wires `#enrollWholeSectionBtn`; `openEnrollmentModal()` clears `#enrollSectionAlert` |

### Design Decisions
- `assignStudentsToSection` promotes `Submitted` → `Active`; skips students already in any section.
- `removeStudentFromSection` cascades via `deleteByStudentAndSectionCode`, reverts status to `Submitted`, clears section FK.
- `bulkEnrollSectionIntoClass` is idempotent: skips already-enrolled and non-Active students; returns counts for UI feedback.
- `GET /sections/eligible-students` registered before `GET /sections/{sectionCode}/students` to avoid path-variable capture.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 135 tests, 0 failures, 0 errors**.

## 2026-05-10 - Navbar Sync on student-records.html
**Branch:** `feature/edit-class-trainer`

### Task
Update the registrar's edit student record page navbar to expose the recently added registrar pages (`classes.html`, `sections.html`). The page had been stuck on the pre-May-9 2-link layout (Home + Subjects).

### Files Modified
| File | Change |
|------|--------|
| `src/main/resources/static/student-records.html` | Added two `<li class="nav-item">` entries linking to `classes.html` and `sections.html` after the existing `Subjects` link. Link order, classes, and styling mirror `registrar.html` exactly. |

### Design Decisions
- No `active` class on any link — the edit page is a subpage of Home (reached via "Open Details → Edit" on the home table), not itself a top-level nav target.
- Pure HTML change; no JS, CSS, or backend update needed.
- Re-used existing `nav-link admin-nav-link` classes — no new styles.

### Verification
- Visual diff against `registrar.html` confirms identical 4-link structure.
- No build needed; static asset change only.

### Open Items
- Manual browser check: open `/student-records.html?id={recordId}`, click each nav link, verify routing + Bootstrap collapse at mobile widths.

---

## 2026-05-10 - Edit Class Trainer (AGILE-93 / AGILE-95)
**Branch:** `feature/edit-class-trainer`

### Task
Allow a registrar to reassign (or unassign) the trainer on an existing class via a new `PUT /api/registrar/classes/{classId}/trainer` endpoint and an Edit Trainer modal on `classes.html`. Edit scope is trainer-only — section, subject, and semester remain immutable after class creation. No DB schema change needed (`classes.trainer_id` is already nullable with `ON DELETE SET NULL`).

### Files Created
| File | Purpose |
|------|---------|
| `src/main/java/com/example/springboot/dto/registrar/UpdateClassTrainerRequest.java` | Request DTO — single nullable `Integer trainerId` |
| `src/test/java/com/example/springboot/service/ClassManagementServiceTest.java` | 6 Mockito service tests for `updateClassTrainer` |
| `src/test/java/com/example/springboot/controller/ClassManagementControllerWebMvcTest.java` | 4 WebMvc controller tests for `PUT /classes/{id}/trainer` |

### Files Modified
| File | Change |
|------|--------|
| `src/main/java/com/example/springboot/service/ClassManagementService.java` | Added `UpdateClassTrainerRequest` import + `updateClassTrainer()` method (~25 LOC) |
| `src/main/java/com/example/springboot/controller/ClassManagementController.java` | Added `UpdateClassTrainerRequest` import + `PUT /classes/{classId}/trainer` endpoint with `system_logs` |
| `src/main/resources/static/classes.html` | Added `#editClassModal` (read-only context + trainer select + inline alert); cache-buster `?v=2` |
| `src/main/resources/static/js/registrar-classes.js` | Actions column now emits two buttons; `currentEditClassData` state + `editClassModal` init; `setupEditClass()` + `openEditClassModal()`; `loadTrainersDropdown()` returns jQuery deferred |

### Design Decisions
- Trainer-only edit scope per user decision — changing section/subject/semester would break the `(section_code, subject_code, semester)` unique key and leave enrollments in invalid states.
- `loadTrainersDropdown()` now returns the `$.ajax` deferred so `openEditClassModal()` can call `.done()` to pre-select the current trainer after the dropdown populates.
- Validation mirrors `assignTrainer` exactly: role must be `ROLE_TRAINER`, `enabled` must be `true`.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 115 tests, 0 failures, 0 errors**.

---

## 2026-05-10 - Subjects CRUD (Create / Edit / Delete)
**Branch:** `feature/subjects-crud`

### Task
Add Create, Edit, and Delete capabilities to the existing Subjects page (closes AGILE-89, AGILE-90, AGILE-91). Delete uses strict type-to-confirm modal. Existing Assign Trainer flow preserved unchanged. No schema changes — `subjects` table already supports all operations.

### Files Created
| File | Purpose |
|------|---------|
| `src/main/java/com/example/springboot/repository/QualificationRepository.java` | `JpaRepository<Qualification, Integer>` |
| `src/main/java/com/example/springboot/dto/registrar/QualificationResponse.java` | Response DTO for qualification dropdown |
| `src/main/java/com/example/springboot/dto/registrar/CreateSubjectRequest.java` | POST body with Bean Validation |
| `src/main/java/com/example/springboot/dto/registrar/UpdateSubjectRequest.java` | PUT body (code read-only) |
| `src/test/java/com/example/springboot/service/ClassManagementSubjectServiceTest.java` | 9 Mockito service tests |
| `src/test/java/com/example/springboot/controller/ClassManagementSubjectControllerWebMvcTest.java` | 6 WebMvc controller tests |

### Files Modified
| File | Change |
|------|--------|
| `src/main/java/com/example/springboot/repository/SchoolClassRepository.java` | Added `existsBySubjectSubjectCode(String)` FK pre-check |
| `src/main/java/com/example/springboot/repository/SubjectRepository.java` | Added `countGradesBySubjectCode` native query |
| `src/main/java/com/example/springboot/service/ClassManagementService.java` | Added `QualificationRepository` + 4 new methods: `getAllQualifications`, `createSubject`, `updateSubject`, `deleteSubject` |
| `src/main/java/com/example/springboot/controller/ClassManagementController.java` | Added `GET /qualifications`, `POST /subjects`, `PUT /subjects/{code}`, `DELETE /subjects/{code}` with system_logs |
| `src/main/resources/static/subjects.html` | Create button + 3 modals (Create, Edit, Delete strict-confirm); JS cache-buster `?v=2` |
| `src/main/resources/static/js/registrar-subjects.js` | Full rewrite — 3-button Actions column; CRUD setup functions added |

### Design Decisions
- Subject code is read-only after creation (PK cascades across `classes`/`grades`).
- Double FK pre-check on delete (classes then grades) yields actionable 400 messages instead of generic 409.
- Assign Trainer modal kept separate per user preference; Edit modal handles only name, qualification, units.
- `loadQualificationsDropdown()` returns a jQuery deferred so Edit can chain `.done()` to pre-select current qualification.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 105 tests, 0 failures, 0 errors**.

---

## 2026-05-09 - Student-Details Wizard Trim
**Branch:** `fix/student-details-trim`

### Task
Shorten the student enrollment wizard: (1) make the Baptismal Certificate upload optional, (2) reduce the Educational Background table from 6 to 4 columns, (3) remove the "School Years at Anihan" section. Pure frontend change — no backend, DTO, entity, or DB migration required.

### Files Modified
| File | Change |
|------|--------|
| `src/main/resources/static/js/student-details.js` | Dropped `certStatus`/`pendingBaptCert` check from `STEP_CUSTOM_VALIDATORS[2]` (Baptism Date + Place still required when Baptized is checked; cert is now optional). Removed `renderSyRow()` DOMContentLoaded call and `addSyRow` click listener. Dropped `gradeYear`/`semester` from `educationHistory` map in `buildPayload`. Replaced 12-line `#syTableBody` forEach with `const schoolYears = []`. Removed `edu-grade`/`edu-sem` reads from `populateForm`. Removed `schoolYears` population block from `populateForm`. Deleted `renderSyRow()` and `addSyRowData()` functions. |
| `src/main/resources/static/student-details.html` | Replaced 6-column `<thead>` with 4-column (`Grade/Year`/`Semester` removed; `Year Ended` → `School Year`). Removed `.edu-grade`/`.edu-sem` `<td>` cells from all 4 education rows. Deleted entire "School Years at Anihan" section (`#syTable`, `#syTableBody`, `#addSyRow`). Bumped JS cache-buster `?v=4` → `?v=5`. |

### Design Decisions
- Baptism cert file input stays in the UI — students can still upload voluntarily. Only the wizard-block is removed.
- `EducationItemDto.gradeYear`/`.semester` and `student_education` columns kept — new submissions write `NULL`. No migration needed.
- Registrar continues to own school-year history via `RegistrarService.saveSchoolYears`; newly enrolled students arrive with zero `student_school_years` rows.

### Verification
- `grep -nE "syTable|syTableBody|addSyRow|renderSyRow|addSyRowData|edu-grade|edu-sem"` on both files → empty.
- `./gradlew test` → **BUILD SUCCESSFUL — 90 tests, 0 failures, 0 errors**.

---

## 2026-05-09 - DB Sync + Error-Handler Hardening + Section FK Pre-Check
**Branch:** `fix/db-sync-and-bugs`

### Task
Audit-driven bugfix sweep. Five issues addressed: (1) live MySQL was missing the May 9 migration so the registrar Classes/Subjects/Sections pages threw 500s; (2) `student_records.middle_name` was still `NOT NULL` despite the JPA entity treating it as optional; (3) `GlobalExceptionHandler` leaked SQL/exception internals to API clients; (4) `ClassManagementService.deleteSection` had no pre-check for FK references; (5) `getCurrentSemester` loaded all batches into memory just to find the max year.

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/migrations/2026-05-09-relax-middle-name.sql` | Idempotent migration relaxing `student_records.middle_name` to `NULL`. Companion fix to the 2026-05-05 drift migration which missed this column. |

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/schema.sql` | `student_records.middle_name` declared `NULL` (was `NOT NULL`). Aligns fresh-install schema with the JPA entity and student-portal wizard which both treat middle name as optional. |
| `src/main/java/com/example/springboot/exception/GlobalExceptionHandler.java` | Added SLF4J logger. New `@ExceptionHandler(DataIntegrityViolationException)` returns HTTP 409 with a generic conflict message. Generic `Exception` handler now logs server-side via `log.error` and returns a sanitized `"An unexpected error occurred."` instead of echoing `ex.getClass().getSimpleName() + " - " + ex.getMessage()` (which previously leaked raw SQL, table names, and column names to clients). |
| `src/main/java/com/example/springboot/repository/SchoolClassRepository.java` | Added `boolean existsBySectionSectionCode(String sectionCode)` for the new pre-check. |
| `src/main/java/com/example/springboot/repository/BatchRepository.java` | Added `Optional<Batch> findTopByOrderByBatchYearDesc()` so `getCurrentSemester()` can resolve the latest batch year in a single SQL query instead of `findAll()` + in-memory max. |
| `src/main/java/com/example/springboot/service/ClassManagementService.java` | `getCurrentSemester()` now calls `batchRepository.findTopByOrderByBatchYearDesc()`. `deleteSection()` now calls `classRepository.existsBySectionSectionCode()` and throws `IllegalArgumentException("Cannot delete section: one or more classes still reference it. Remove those classes first.")` (mapped to HTTP 400 by `GlobalExceptionHandler`) instead of letting the FK violation bubble up as a generic 500. |

### Database Migrations Applied (live `AnihanSRMS` on this machine)
| Statement | Purpose |
|-----------|---------|
| `2026-05-09-classes-and-trainers.sql` | Re-applied — was previously not present on this machine despite memory-bank claim. Created `classes`, `class_enrollments`, added `subjects.trainer_id` FK, seeded 2 qualifications + 6 subjects. |
| `ALTER TABLE student_records MODIFY COLUMN middle_name VARCHAR(255) NULL` | Relaxed the column to allow students with no middle name to save without a SQL constraint violation. |

### Design Decisions
- **Generic 500 message instead of exception details.** The previous `"Internal server error: <ExceptionClass> - <message>"` body leaked SQL queries, table names, and JPA internals — confirmed during the audit by hitting `/api/registrar/classes` and seeing the full SELECT echoed back. The new behavior logs the full stack trace via SLF4J (so it appears in operator logs) but returns only a generic message to the client. This matches the project's on-premise deployment context where stack traces should never leave the server.
- **Pre-check, don't retry.** `deleteSection()` checks for class references *before* attempting the delete. The FK constraint stays as a defense in depth, but the pre-check converts a leaky 500 into a clean 400 with an actionable message.
- **`findTopByOrderByBatchYearDesc()` over a custom `@Query`.** Spring Data derived queries are preferred for simple cases; the runtime SQL is `SELECT ... FROM batches ORDER BY batch_year DESC LIMIT 1`, which is what we want. Avoids the maintenance cost of an explicit JPQL string.

### Verification
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → BUILD SUCCESSFUL — 90 tests, 0 failures, 0 errors
- `./gradlew bootRun` → started cleanly. Hit each previously-broken endpoint while authenticated as `registrar`:
  - `GET /api/registrar/subjects` → HTTP 200, returns 6 seeded subjects with qualifications + trainer fields
  - `GET /api/registrar/classes` → HTTP 200, `[]` (no classes yet — expected, no batches/courses/sections seeded; that's data the registrar enters via the UI)
  - `GET /api/registrar/classes/current-semester` → HTTP 200, `{"semester":"2026"}`
- Live MySQL `SHOW TABLES` → 19 tables (was 17). `subjects.trainer_id` present. 2 qualifications + 6 subjects rows present. `student_records.middle_name` `IS_NULLABLE = YES`.

### Out of Scope (deferred to follow-up tickets)
- N+1 in `getEligibleStudents()` and `getClasses()` — both still use `findAll()` + filter/count per row. Acceptable at the school's scale (~160 students); revisit if perf issues surface.
- Move `ClassEnrollmentResponse` and `StudentSummary` records out of `ClassManagementService` into `dto/registrar/`. Cosmetic; doesn't affect behavior.
- Unit/WebMvc tests for `ClassManagementService` and `ClassManagementController`. Existing audit caught the live-DB drift via integration probing rather than tests; tests would have caught a regression earlier and remain on the roadmap.

---

## 2026-05-09 - Registrar Subjects / Classes / Sections + Class Enrollment
**Branch:** `feature/class-assignment`

### Task
Implement three registrar-facing features: (1) per-subject default trainer assignment, (2) per-class scheduling with optional trainer + student enrollment, (3) section creation/listing scoped to the current semester. Includes a brand-new `classes` and `class_enrollments` table, seeded qualifications + subjects, and three new dashboard pages.

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/migrations/2026-05-09-classes-and-trainers.sql` | Idempotent migration: adds `subjects.trainer_id` + FK, creates `classes` and `class_enrollments`, seeds 2 qualifications + 6 subjects |
| `model/SchoolClass.java` | JPA entity for `classes` (named `SchoolClass` to avoid clash with `java.lang.Class`) |
| `model/ClassEnrollment.java` | JPA entity for `class_enrollments` |
| `repository/SchoolClassRepository.java` | `findBySemester`, `existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester` |
| `repository/ClassEnrollmentRepository.java` | `findBySchoolClassClassId`, `existsBySchoolClassClassIdAndStudentStudentId`, `countBySchoolClassClassId` |
| `dto/registrar/SubjectResponse.java` | Subject DTO with flattened qualification name + trainer info |
| `dto/registrar/AssignTrainerRequest.java` | Single optional `trainerId` (null to unassign) |
| `dto/registrar/ClassResponse.java` | Class DTO including `enrolledCount` |
| `dto/registrar/CreateClassRequest.java` | Validated create-class payload |
| `dto/registrar/SectionResponse.java` | Section DTO with batch + course info |
| `dto/registrar/CreateSectionRequest.java` | Validated create-section payload |
| `dto/registrar/TrainerResponse.java` | Lightweight trainer-dropdown DTO |
| `dto/registrar/EnrollStudentRequest.java` | Validated enrollment payload |
| `service/ClassManagementService.java` | Subjects + Trainers + Classes + Class Enrollment + Sections business logic |
| `controller/ClassManagementController.java` | New controller under `/api/registrar/...` (separate from `RegistrarController`); every state-changing call writes a `system_logs` row |
| `static/classes.html` | Classes dashboard page with create + enrollment modals; 4-link registrar navbar |
| `static/sections.html` | Sections dashboard page with create + delete modals; 4-link registrar navbar |
| `static/js/registrar-subjects.js` | Subjects DataTable + Assign Trainer modal logic |
| `static/js/registrar-classes.js` | Classes DataTable + create-class + enrollment management logic |
| `static/js/registrar-sections.js` | Sections DataTable + create + delete logic |

### Files Modified
| File | Change |
|------|--------|
| `model/Subject.java` | Added `@ManyToOne User trainer` mapped to `trainer_id` (optional) |
| `repository/SectionRepository.java` | Added `List<Section> findByBatchBatchYear(Short batchYear)` |
| `repository/UserRepository.java` | Added `List<User> findByRoleAndEnabledTrue(String role)` |
| `config/SecurityConfig.java` | REGISTRAR HTML matcher extended with `/classes.html` and `/sections.html` |
| `static/registrar.html` | Navbar bumped from 2-link (Home / Subjects) to 4-link (Home / Subjects / Classes / Sections) |
| `static/subjects.html` | Replaced placeholder with full DataTable + Assign Trainer modal; navbar bumped to 4 links |
| `src/main/sql/schema.sql` | Added `subjects.trainer_id` column + FK, added `classes` + `class_enrollments` CREATE TABLE, added qualifications + subjects seed data; header bumped to 2026-05-09 with table count 17 → 19 |

### Database Migrations Applied
| Statement | Purpose |
|-----------|---------|
| `ALTER TABLE subjects ADD COLUMN trainer_id INT NULL` | Optional default trainer per subject |
| `ALTER TABLE subjects ADD CONSTRAINT fk_subjects_trainer FOREIGN KEY (trainer_id) REFERENCES users(user_id) ON DELETE SET NULL` | FK keeps subjects intact when a trainer account is deleted |
| `CREATE TABLE classes` | New table — section + subject + trainer + semester with `(section_code, subject_code, semester)` unique key |
| `CREATE TABLE class_enrollments` | New table — student-to-class link with `(class_id, student_id)` unique key, `ON DELETE CASCADE` from classes |
| `INSERT INTO qualifications` | Seeded `Cookery NC II` and `Bread and Pastry Production NC II` |
| `INSERT INTO subjects` | Seeded 6 subjects (4 cookery + 2 bread/pastry) |

### Design Decisions
- **Two trainer touchpoints (subject and class).** A trainer can be assigned as a *default* on the Subject (via Subjects page) and again on a specific Class (per-section, per-semester). The class-level trainer is the authoritative teacher; the subject-level trainer is a convenience pre-fill on the Create Class modal (auto-selected when the subject changes).
- **`SchoolClass` instead of `Class`.** `Class` would collide with `java.lang.Class`, so the JPA entity uses `SchoolClass` while the DB table is `classes`.
- **Separate controller `ClassManagementController` instead of bloating `RegistrarController`.** Keeps endpoints organized and limits the cross-cutting impact on existing tests.
- **Semester = batch year (string).** `getCurrentSemester()` reads `MAX(batch.batchYear)` and falls back to `Year.now()` if no batches exist. Filtering classes/sections uses this same value.
- **Eligible-student filter** restricts to `Active` or `Submitted` students in the same `section_code` as the class who aren't already enrolled. Avoids enrolling students from a different section by accident.
- **Delete a section** requires no linked classes — FK on `classes.section_code` blocks the delete and the frontend surfaces the DB error.

### Verification
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → BUILD SUCCESSFUL (no regressions)
- `./gradlew bootRun` → Tomcat started on port 8080, Spring context loaded with no schema validation errors
- Live MySQL: `SHOW TABLES` reports `classes` + `class_enrollments`; `DESCRIBE subjects` shows new `trainer_id` column; `SELECT COUNT(*)` returns 2 qualifications + 6 subjects.

---

## 2026-05-07 - Emoji Cleanup Across Static Frontend
**Branch:** `feature/registrar-fix`

### Task
Remove visible emoji glyphs from user-facing pages and JS, keeping the third-party DataTables library (`datatables.min.js`, `datatables.min.css`) untouched.

### Files Modified
| File | Change |
|------|--------|
| `static/registrar.html` | Removed `<span class="dropdown-icon">✏️</span>` and `<span class="dropdown-icon">🚪</span>` from the Edit Account and Log Out dropdown items. |
| `static/trainer.html` | Same removals as registrar.html. |
| `static/index.html` | Dropped the `✅ ` prefix from the logout notification text. |
| `static/student-portal.html` | Dropped the `⚠️ ` prefix from the duplicate-record alert title. |
| `static/js/student-details.js` | Replaced 3 occurrences of `` `✓ ${...}` `` with `` `Uploaded: ${...}` ``; updated the baptismal-cert validator's `startsWith('✓')` check to `startsWith('Uploaded:')` so the logic still detects an already-uploaded file. |

### Notes
- Confirmed via `grep` over `static/css/` and `static/js/` that no CSS or JS code referenced the removed `dropdown-icon` span class — it was decorative only.
- The character `—` (em dash) used in headers/labels is **not** an emoji and was kept where present.
- Vendored DataTables library files (`datatables.min.js`, `datatables.min.css`) contain a few non-ASCII chars used internally and were intentionally left as-is.

### Verification
- `./gradlew build -x test` → BUILD SUCCESSFUL
- No backend, DTO, or DB changes.

---

## 2026-05-07 - Strict Type-to-Confirm Delete Modals (Registrar + Admin)
**Branch:** `feature/registrar-fix`

### Task
Replace the lightweight `window.confirm()` / `window.alert()` dialogs used for destructive deletes with stricter Bootstrap modals that require the user to type the literal word `delete` before the action is enabled. Apply on the registrar student-record delete and on the admin permanent (hard) account delete. Soft-delete (Deactivate Account) is unchanged because it is reversible.

### Files Modified
| File | Change |
|------|--------|
| `static/registrar.html` | New `#deleteRecordConfirmModal` (typing-confirm) above the footer. Reuses existing `delete-confirm-modal`, `warning-text`, `danger-zone`, and `btn-permanent-delete` styles in `dashboard.css` — no CSS changes. Modal echoes the student identifier in `#deleteRecordIdentifier` and shows inline result feedback in `#deleteRecordResultAlert`. |
| `static/js/registrar-students.js` | Added `deleteConfirmModal` and `currentRecordIdentifier` module state. `loadRecordDetails()` now stores a human-readable identifier (Student ID + last/first name). The old `deleteRecordBtn` inline handler that called `window.confirm()` was replaced with `setupDeleteRecordFlow(dataTable)`, which: (1) on click, hides the details modal, populates the identifier, clears the input, disables the confirm button, and shows the typing-confirm modal; (2) wires an `input` listener that enables the confirm button only when the input matches `delete` after `trim().toLowerCase()`; (3) on confirm, performs the existing `DELETE /api/registrar/student-records/{id}` call and surfaces success/error inline; (4) on `hidden.bs.modal`, resets input/button/alert state. All `window.confirm()` and `window.alert()` calls in the delete path are gone. |
| `static/admin.html` | Added a second modal `#permanentDeleteConfirmModal` next to the existing `#deleteConfirmModal`. The existing soft/hard chooser (`#deleteConfirmModal`) is kept as-is for the Deactivate flow; the new modal handles the Permanently Delete flow with typing confirmation. Echoes the username in `#permanentDeleteUserName` and shows inline result feedback in `#permanentDeleteResultAlert`. |
| `static/js/admin-users.js` | Added `currentDeleteUserName` and `permanentDeleteConfirmModal` module state. `loadUserDetails()` now also captures `user.username` into `currentDeleteUserName`. `confirmHardDeleteBtn` click handler simplified — it no longer performs the delete itself; it hides `deleteConfirmModal` and calls `openPermanentDeleteModal()` to open the typing-confirm modal. New `setupPermanentDeleteFlow(dataTable)` mirrors the registrar pattern: input gating, click handler that calls `deleteUser(currentDeleteUserId, true)` via the existing helper, success alert + `dataTable.ajax.reload(null, false)` after a short delay, and modal-state reset on `hidden.bs.modal`. The previous `window.confirm('This action is PERMANENT...')` has been removed. |

### Design Decisions
- **Soft delete (Deactivate) keeps its existing single-modal flow.** It is reversible (re-enable is supported), so a typing confirmation would be friction without a safety benefit.
- **Two-step admin flow preserved.** The new modal opens *from* the existing soft/hard chooser rather than replacing it, so admins still get the explicit Deactivate vs. Permanently Delete choice. The chooser closes before the typing-confirm modal opens to avoid Bootstrap stacking-modal issues.
- **Comparison is `value.trim().toLowerCase() === 'delete'`** so leading/trailing whitespace and capitalization (`DELETE`, `Delete`) are accepted. The placeholder shows the literal lowercase word `delete` so the contract is unambiguous.
- **Reused existing CSS classes** (`delete-confirm-modal`, `warning-text`, `danger-zone`, `btn-permanent-delete`, `btn-surface-secondary`) — no `dashboard.css` changes were needed.

### Verification
- `./gradlew build -x test` → BUILD SUCCESSFUL
- No backend, DTO, repository, or DB changes — purely a frontend UX guardrail in front of the existing DELETE endpoints (`/api/registrar/student-records/{id}` and `/api/admin/users/{id}/permanent`).

---

## 2026-05-07 - Bugs & Registrar Features: Parents/Guardian, Delete, Deferred Uploads, Not Available
**Branch:** `feature/registrar-fix`

### Task
6 items: Feature 2 (Not Available), Bug 3 (ID Photo not required), Bug 2 (Parents/Guardian in view+edit), Feature 1 (Delete record), Feature 3 (Auto-assign batch), Bug 1 (Defer file uploads).

### Files Modified
| File | Change |
|------|--------|
| `dto/registrar/StudentRecordDetailsResponse.java` | Added `father`, `mother`, `guardian` (ParentDto/GuardianDto) fields. New 7-arg `from()` factory; 4-arg and 1-arg factories delegate to it. |
| `dto/registrar/StudentRecordUpdateRequest.java` | Added `father`, `mother`, `guardian` optional fields (no validation constraints). |
| `service/RegistrarService.java` | Constructor expanded to 12-arg (added `ParentRepository`, `OtherGuardianRepository`, `StudentEducationRepository`, `StudentUploadRepository`, `StorageService`). `buildDetailsResponse()` loads parent/guardian from repos and passes to `from()`. `updateRecord()` calls `saveParents()` and `saveGuardian()` after `saveSchoolYears()`. New `deleteRecord()` deletes physical uploads, all child rows in FK order (uploads → parents → guardian → education → school years → TESDA → OJT → documents → grades), then parent row. New helpers: `saveParents()`, `upsertParent()`, `saveGuardian()`, `toParentDto()`, `toGuardianDto()`. |
| `controller/RegistrarController.java` | Added `DELETE /{recordId}` with `@ResponseStatus(NO_CONTENT)` and system log entry. |
| `repository/StudentUploadRepository.java` | Added `List<StudentUpload> findByStudentId(String)` and `void deleteByStudentId(String)`. |
| `repository/StudentRecordRepository.java` | Added `@Modifying @Query(nativeQuery=true)` `deleteDocumentsByStudentId()` and `deleteGradesByStudentId()` (no JPA repositories exist for `documents`/`grades` tables). |
| `repository/BatchRepository.java` | Added `Optional<Batch> findFirstByBatchYear(Short batchYear)`. |
| `service/StudentDetailsService.java` | Constructor +`BatchRepository batchRepo`. `submitEnrollment()`: if no batch set, auto-assigns via `batchRepo.findFirstByBatchYear(currentYear)`. |
| `test/service/StudentDetailsServiceTest.java` | Added `@Mock private BatchRepository batchRepo`. |
| `test/service/RegistrarBulkLoadTest.java` | Added 5 `@Mock` fields needed by expanded 12-arg constructor: `parentRepository`, `guardianRepository`, `educationRepository`, `uploadRepository`, `storageService`. |
| `static/registrar.html` | Added Father/Mother/Guardian detail-grid sections in modal body. Added Delete button (danger-surface, margin-right:auto) in modal footer. |
| `static/js/registrar-students.js` | Added `currentRecordId` state variable. `loadRecordDetails()` sets `currentRecordId` and populates all 24 parent/guardian sub-fields. Delete button handler: confirm → `DELETE /api/registrar/student-records/{id}` → hide modal → `dataTable.ajax.reload()`. |
| `static/student-records.html` | Added Father, Mother, Guardian form sections (each with full set of inputs) between Family/Religion and Enrollment sections. JS cache bumped `?v=2` → `?v=3`. |
| `static/js/registrar-student-records-edit.js` | `populateForm()` fills Father/Mother/Guardian fields. New `buildParent(prefix)` and `buildGuardian()` helpers. `buildPayload()` includes `father`, `mother`, `guardian`. |
| `static/js/student-details.js` | Added `pendingIdPhoto`/`pendingBaptCert` state. `setupFileInput()` rewritten: stores File in pending var, shows local FileReader preview, sets "Selected: filename" status (no network). `submitForm()` uploads pending files after JSON submit succeeds via `uploadPendingFile()`. Baptism cert validator: `!certStatus.startsWith('✓') && pendingBaptCert === null` (pending file satisfies requirement). |
| `static/student-details.html` | Removed `*` from ID Photo label (no longer required). |

### Design Decisions
- `deleteRecord()` uses explicit ordered deletes (not JPA cascade) because `documents` and `grades` have no JPA repositories and FK constraints must be cleared before the parent row can be removed.
- `findFirstByBatchYear` (not `findByBatchYear`) used for batch auto-assignment because `batch_year` has no UNIQUE constraint; `findFirst` safely handles multiple batches per year.
- Deferred upload approach (store File in JS var, upload after JSON submit) ensures a student record exists in the DB before the upload FK constraint fires, and avoids wasted uploads if the student abandons the form before submitting.

### Verification
- `./gradlew test` → BUILD SUCCESSFUL — all tests pass, no regressions

---

## 2026-05-06 - Registrar Enhancements: Status Filter + OJT/TESDA/SchoolYears on Edit Form
**Branch:** `feature/registrar-fixes`

### Task
Feature 1: Add a student status filter dropdown to the registrar home page (`registrar.html`).
Feature 2: Restore OJT, TESDA Qualifications, and School Years sections to the registrar edit form (`student-records.html`) so the registrar can manage the full training record alongside basic student details.

### Files Modified
| File | Change |
|------|--------|
| `dto/registrar/StudentRecordDetailsResponse.java` | Added `OjtDto ojt`, `List<TesdaQualDto> tesdaQualifications`, `List<SchoolYearDto> schoolYears` fields. New 4-arg `from(record, ojt, tesda, sy)` factory. Old 1-arg `from(record)` delegates to new factory with empty collections. |
| `dto/registrar/StudentRecordUpdateRequest.java` | Added same 3 optional fields with no validation constraints (empty allowed). |
| `service/RegistrarService.java` | Constructor extended with `StudentOjtRepository`, `StudentTesdaQualificationRepository`, `StudentSchoolYearRepository`. Added `getAllRecords(query, fromYear, toYear, status)` 4-arg overload; older overloads delegate to it. Added `matchesStatus()`. `updateRecord()` annotated `@Transactional`; after saving basic record, calls `saveOjt()`, `saveTesda()`, `saveSchoolYears()` helpers. Each helper deletes by old studentId (with explicit `flush()` to prevent unique-constraint race before inserts) then inserts new rows with new studentId. `getRecordById()` now calls `buildDetailsResponse()` which loads all 3 collections. |
| `controller/RegistrarController.java` | Added `@RequestParam(value = "status", required = false) String status` to `list()` and forwarded to service. |
| `test/service/RegistrarBulkLoadTest.java` | Added `@Mock StudentOjtRepository`, `@Mock StudentTesdaQualificationRepository`, `@Mock StudentSchoolYearRepository` so `@InjectMocks` can build the new 7-arg constructor. Added `statusFilterRestrictsResultsByStudentStatus` test (Active filter, case-insensitive, null/blank returns all, unknown returns empty). |
| `test/controller/RegistrarBulkLoadWebMvcTest.java` | Updated all 3 `when(registrarService.getAllRecords(...))` stubs from 3-arg to 4-arg signature (added trailing `isNull()` for status). |
| `static/registrar.html` | Added `<label>` + `<select id="studentStatusFilter">` with options All/Enrolling/Submitted/Active/Graduated inside the existing `#batchFilterBar`. |
| `static/js/registrar-students.js` | `buildAjaxUrl()` now reads `#studentStatusFilter` and appends `?status=` when non-empty. Reset handler clears the select alongside year inputs. |
| `static/student-records.html` | Added 3 new form sections before `</form>`: OJT (company name, address, hours), TESDA (3 fixed `<fieldset>` slots with title, center address, assessment date, result), School Years (dynamic `<table>` with `<tbody id="schoolYearsBody">` + "Add Row" button). Bumped JS cache to `?v=2`. |
| `static/js/registrar-student-records-edit.js` | Added `esc()`, `createSchoolYearRow()`, `setupSchoolYearHandlers()`. Extended `populateForm()` to fill OJT inputs, TESDA slots 1–3 (keyed by `slot` number), and render School Year rows. Extended `buildPayload()` with `buildOjt()`, `buildTesdaSlot()`, `buildSchoolYearRows()` helpers. Added form-level `input`/`change` event delegation in `setupDirtyTracking()` to cover dynamically-added rows. `setupSchoolYearHandlers()` called before `setupDirtyTracking()` in boot sequence. |

### Design Decisions
- **Delete-all-then-insert-new** for TESDA and SchoolYears: simpler than diffing; "edit form replaces state" matches the registrar's mental model. See `decisions.md`.
- **Explicit `flush()` after delete**: prevents Hibernate from buffering the DELETE past the INSERT within the same `@Transactional`, which would violate the `(student_id, slot)` unique constraint on TESDA.
- **OJT upsert** (find existing row, update or create): OJT is a 1:1 relationship per student; upsert avoids losing the `ojt_id` PK unnecessarily.
- **Form-level dirty delegation**: `form.addEventListener('input', markDirty)` is added alongside per-element listeners so dynamically-added School Year row inputs are covered without re-running `setupDirtyTracking`.

### Verification
- `./gradlew test` → BUILD SUCCESSFUL — all tests pass, no regressions
- New test: `statusFilterRestrictsResultsByStudentStatus` — passes

---

## 2026-05-05 - Student Portal Enrollment Flow Fix
**Branch:** `fix/student-portal-flow`

### Task
Fix 5 root-cause bugs in the student enrollment flow (student-portal → student-details wizard → submit). Scope: student-facing code only — no Registrar/Trainer changes.

### Root Causes Fixed
| RC | Severity | Description |
|----|----------|-------------|
| RC-1 | 🔴 Critical | `startOrResume()` created full student record on portal start. Now creates name+status only (minimal for upload FK). |
| RC-2 | 🔴 Critical | `saveDraft()` persisted to DB on every "Next" click. Removed — data stays in browser until final submit. |
| RC-3 | 🟡 Medium | `submitEnrollment()` replaces old two-step saveDraft/submit with a single `@Transactional` block that persists record + parents + guardian + education + school years atomically. |
| RC-4 | 🟡 Medium | OJT/TESDA removed from student-facing flow (HTML, JS, DTOs, service). Entities/repos kept for Registrar. |
| RC-5 | 🟢 Low | `AgeCalculator` returns `Integer null` instead of `int 0` for null birthdate. |

### Files Created
| File | Purpose |
|------|---------|
| `test/service/StudentDetailsServiceTest.java` | 7 Mockito unit tests: start (minimal record), resume, submit (all data), double-submit guard, load, invalid ID |

### Files Modified
| File | Change |
|------|--------|
| `service/StudentDetailsService.java` | Rewrote: removed `saveDraft()` and old `submit()`; added `submitEnrollment()` with full `@Transactional` persistence; removed OJT/TESDA handling |
| `controller/StudentDetailsController.java` | Removed PUT saveDraft endpoint; new `POST /{studentId}/submit` accepts full `StudentDetailsRequest` body |
| `dto/student/StudentDetailsRequest.java` | Added `lastName`, `firstName`, `middleName` fields; removed `ojt` and `tesdaQualifications` |
| `dto/student/StudentDetailsResponse.java` | Removed `ojt` and `tesdaQualifications` |
| `static/js/student-details.js` | Removed `saveDraft()` function and all calls; removed OJT/TESDA from `buildPayload()`/`populateForm()`; submit sends full payload as POST body |
| `static/student-details.html` | Removed OJT/TESDA HTML sections (20 lines); bumped JS cache `v=3` → `v=4` |
| `service/AgeCalculator.java` | Return type `int` → `Integer`; null birthdate → `null` instead of `0` |
| `test/service/AgeCalculatorTest.java` | Updated: `assertEquals(0)` → `assertNull`; added `assertNull` import |
| `memory-bank/activeContext.md` | Updated with enrollment flow fix session |
| `memory-bank/progress.md` | Added completed enrollment flow fix section |
| `memory-bank/changeLog.md` | This entry |

### Scope Boundary (NOT Changed)
- All Registrar/Trainer controllers, services, HTML, JS — untouched
- `StudentOjt.java`, `StudentTesdaQualification.java` entities — kept for Registrar use
- `StudentOjtRepository.java`, `StudentTesdaQualificationRepository.java` — kept
- `AdminService.java`, `RegistrarService.java`, `AccountService.java` — untouched

### Verification
- `./gradlew test` → BUILD SUCCESSFUL — all tests pass, no regressions
- New `StudentDetailsServiceTest`: 7/7 pass
- Updated `AgeCalculatorTest`: 6/6 pass
- No lint warnings

---

## 2026-05-05 - Schema Drift Remediation + DataSeeder Removal
**Branch:** `main` (uncommitted, per user instruction — no feature branch, no commit)

### Task
Diagnose live `AnihanSRMS` database against `schema.sql` / `AnihanSRMS.sql` and JPA entities. Apply migrations to fix the drift, delete the redundant `DataSeeder` so application data lives in the database only, and update SQL files for other developers.

### Files Created
| File | Purpose |
|---|---|
| `src/main/sql/migrations/2026-05-05-fix-schema-drift.sql` | One-shot migration to bring legacy databases in line with `schema.sql`. Adds `student_records.civil_status` (idempotent guard via `information_schema`), relaxes 27 columns from `NOT NULL` to `NULL` across `student_records`, `parents`, and `other_guardians`, drops `parents.est_income DEFAULT 0.00`. |

### Files Deleted
| File | Reason |
|---|---|
| `src/main/java/com/example/springboot/config/DataSeeder.java` | Application data must come from the database. The seeder duplicated `schema.sql` seed inserts and was the proximate cause of the `contextLoads()` test failure when the live DB drifted. |

### Files Modified
| File | Change |
|---|---|
| `src/main/sql/schema.sql` | Header updated to 2026-05-05; description corrected to acknowledge the 5 sample student records that were already being inserted; added cross-reference to the new migration file |
| `src/main/sql/AnihanSRMS.sql` | Header updated to 2026-05-05; added cross-reference to `schema.sql` (for fresh installs) and to the new migration file (for existing DBs predating the drift fix) |
| `memory-bank/activeContext.md` | New "Schema Drift Remediation + DataSeeder Removal" session entry; current branch refreshed to `main`; removed stale "Active Branch: registrar-retry" duplicate |
| `memory-bank/progress.md` | New completed section at the top |
| `memory-bank/decisions.md` | Two new decision records (live-DB ALTER over schema rewrite; DataSeeder removal) |

### Database Migrations Applied (Docker `mysql-server` container)
| Statement | Purpose |
|---|---|
| `ALTER TABLE student_records ADD COLUMN civil_status VARCHAR(50) NULL AFTER sex` | Restore missing column referenced by `StudentRecord.java` and the enrollment wizard |
| `ALTER TABLE student_records MODIFY COLUMN ... NULL` (×12) | birthdate, age, sex, permanent_address, email, contact_no, religion, baptism_place, sibling_count, batch_code, course_code, section_code |
| `ALTER TABLE parents MODIFY COLUMN ... NULL` (×9) | family_name, first_name, middle_name, birthdate, occupation, est_income, contact_no, email, address |
| `ALTER TABLE parents ALTER COLUMN est_income DROP DEFAULT` | Removed leftover `DEFAULT 0.00` |
| `ALTER TABLE other_guardians MODIFY COLUMN ... NULL` (×6) | relation, last_name, first_name, middle_name, birthdate, address |

### Why Drift Returned After the May 2 Fix
The May 2 session corrected the same drift on a different live DB. The DB on this machine on 2026-05-05 either predated that fix or was rebuilt from an older dump. Both `schema.sql` and `AnihanSRMS.sql` use `CREATE TABLE IF NOT EXISTS`, so re-running them against an existing legacy DB silently keeps the old column definitions. The new migration file is the durable answer for that scenario.

### Verification
- `SHOW CREATE TABLE student_records` → `civil_status` present, 12 fixed columns nullable
- `SHOW CREATE TABLE parents` / `other_guardians` → all expected columns nullable, `est_income` no default
- `./gradlew test` → BUILD SUCCESSFUL — **82 tests, 0 failures, 0 skipped** (was 81/82 with `contextLoads` failing)
- `Grep DataSeeder` → no remaining production references; only historical mentions in memory-bank files

---

## Historical Summary (pre-2026-05-05)

Older sessions condensed to one line each. For full file-change tables, rationale, and verification details, recover from `git log` on the branch named in each entry.

### May 2026

- **2026-05-04 — Student Status Dropdown + Badge Colors:** Status `<select>` on `student-records.html` (Enrolling/Active/Graduated only); `renderStatusBadge()` colors — Active=green, Enrolling/Submitted=grey, Graduated=blue.
- **2026-05-03 — Student Portal Mandatory Field Validation** (`feature/student-field-validation`): Civil Status, ID Photo, conditional baptism fields, Father/Mother core fields. Added `STEP_CUSTOM_VALIDATORS` pattern.
- **2026-05-02 — Database Sync Migration:** Dropped 4 legacy tables; `student_records` PK changed to `record_id`; nullability + type fixes; 17 canonical tables.
- **2026-05-02 — Search Bar Selector + Filter Width + DataSeeder:** Dual selector for DataTables 1/2 search input; batch-year filter input width; (interim) DataSeeder added — later removed 2026-05-05.
- **2026-05-02 — Registrar Search Bar + Batch Year Filter + Dummy Seed Data:** 320px search input, From/To year inputs, `?fromYear=&toYear=` server-side, 5 dummy student records in `schema.sql`.
- **2026-05-01 — Registrar Bulk Load Tests + H2 Isolation + Server-Side Search:** `?q=` query, 200-record perf test, `StudentRecordH2LoadTest` with isolated H2 (MODE=MySQL).
- **2026-05-01 — Registrar Edit Student Record + Search + Unsaved Notifications:** Full edit form with FK resolution, dirty-tracking + `beforeunload`.
- **2026-05-01 — Registrar Home: Student Records Table & Detail Modal:** `RegistrarController`, `RegistrarService`, summary + details DTOs (BLOB excluded), 9-col DataTable.
- **2026-05-01 — Live Age Recalculation in Edit Account Modal:** `auth-guard.js` recalculates `#ageDisplay` on birthdate change.
- **2026-05-01 — Registrar Navbar Standardization:** `subjects.html` rebranded from admin to registrar (REGISTRAR role, 2-link navbar, security matcher updated).

### April 2026

- **2026-04-30 — Enrollment Flow Bug Audit & Fixes** (`fix/db-sync-username-unique`): Bug 3 (Parent/OtherGuardian FK), Bug 6 (duplicate security matchers), Bug 7 (`saveDraft()` failure handling).
- **2026-04-30 — Database Sync from `AnihanSRMS.sql`:** Added missing UNIQUE index on `users.username`.
- **2026-04-30 — Bug 1 Fix: `student_records.age` NOT NULL** (`feature/student-details`): `ALTER TABLE ... MODIFY COLUMN age INT NULL`.
- **2026-04-30 — Bug 2 Fix: `StudentRecord` `@Id` mismatch:** Moved `@Id @GeneratedValue(IDENTITY)` to `recordId`; repo generic `String → Integer`; `findByStudentId()`.
- **2026-04-30 — Database Schema Audit & Migration:** 5 missing student tables created; `civil_status` added; 26 NOT NULL → NULL relaxations; UNIQUE index on username.
- **2026-04-29 — SQL Files Synced with Live Database:** `AnihanSRMS.sql` + `schema.sql` rewritten — 17 tables + 3 user accounts.
- **2026-04-29 — Submit Button Fix:** JS cache-busting (`?v=2`) + removed `throw e` in `saveDraft()` catch block.
- **2026-04-29 — Student Details Enrollment Wizard:** 5 new entities, 7 new repos, 9 DTOs, `StorageService`, `StudentDetailsService`+Controller, 4-step Bootstrap wizard, live DB migrated.
- **2026-04-27 — Admin Users Table Column Split** (`test-user-table`): Single Name column → Last Name + First Name.
- **2026-04-26 — Database Schema Sync & SQL Export Files:** Created 6 missing tables in live DB; rewrote `AnihanSRMS.sql` + `schema.sql`.
- **2026-04-19 — Age Auto-Calculation from Birthdate:** `AgeCalculator` utility; age removed from input DTOs/forms; silent recalc on individual view; `birthdate` `@NotNull` on create.
- **2026-04-18 — System Logs Export UI Cleanup** (`feature/export-logs`): Server-side CSV/XLSX/DOCX via `GET /api/logs/export`; Apache POI dependency added.
- **2026-04-18 — System Logs Date Filtering** (`feature/logs-date-filter`): Optional `rangeDays`/`startDate`/`endDate` params; default 7 days; preset pills UI.
- **2026-04-18 — Admin Bulk Load Tests:** 100-user perf tests at service (0.008s) and HTTP layer (0.663s).
- **2026-04-17 — Admin Navbar Cleanup:** Removed Student Records + Subjects from active admin navbar; pages preserved with stale internal navbars (TD-2).
- **2026-04-17 — Database Migration Fix & SQL Cleanup:** Re-added `enabled`, `password_changed_at`, `system_logs` after live DB rebuild; removed merge-conflict remnants from `AnihanSRMS.sql`.
- **2026-04-17 — Unit Test Coverage Expansion:** Added Account + SystemLog service/controller test suites.
- **2026-04-16 — Navbar Logo UI Standardization** (`ui-style/fix`): Removed `.brand-title`, logo enlarged to 85px with `-22px` margin compensation.
- **2026-04-14 — Account Icon Dropdown on All Admin Pages:** Brand-mark navbar + Edit Account modal added to `student-records.html`, `subjects.html`, `logs.html`.
- **2026-04-14 — Admin System Logs:** `system_logs` table + entity/repo/service/controller/DTO + `logs.html` + `system-logs.js`; integrated in Auth/Admin/Account controllers.
- **2026-04-11 — Admin Username Edit & Hover Fixes:** `.btn.btn-reenable:hover` Bootstrap-specificity fixes; editable username with no-spaces pattern.
- **2026-04-11 — Re-enable, Password Toggle, Strong Validation, `passwordChangedAt`:** New column; `PUT /api/admin/users/{id}/enable`; eye-icon toggle auto-injected; `@Pattern` strong-password regex (self-service only).
- **2026-04-11 — Admin Password Reset, Delete Account & Hover Fix:** Optional password in admin update DTO; `enabled` column + Spring Security flag; soft + hard delete endpoints; status badge column.
- **2026-04-11 — Admin Dashboard Front-End Repair** (`feature/fix-login-security`): Rebuilt malformed `admin.html` and `edit-user.html` after donor merge artifacts.
- **2026-04-11 — Build Repair for Admin Controller Regression:** Restored DTO/service-based `AdminController` (had drifted back to repository-only).
- **2026-04-11 — Conflict Cleanup and Commit-Safety Recheck:** Removed unresolved merge markers across HTML/SQL/config.
- **2026-04-11 — Root/Admin Merge from `main-em`:** `AdminController`, `AdminService`, sanitized DTOs, edit-user/student-records/subjects/logs HTML, admin DataTable JS, automated tests.
- **2026-04-10 — Admin Dashboard UI & Logic** (`feature/admin-dashboard-ui`): MySQL session config, login error UI, `AnihanSRMS.sql` reordered for FK creation order.
- **2026-04-07 — Database Schema Development:** Defined complete schema with 15 tables.
- **2026-04-06 — AGILE-100 G2.1 Edit Personal Details:** Tabbed Edit Account modal; `LookupController` (subjects/sections); trainer dropdowns; users table extended with `lastName/firstName/middleName/birthdate`.
- **2026-04-05 — AGILE-142 G1.R Fix Login:** Role-specific page matchers, dual-mode entry/denied handlers, `AccountController`/`AccountService`, account dropdown, `auth-guard.js`, unique username index, removed `DataSeeder`.

### March 2026

- **2026-03-24 — Backend Auth Setup** (`feature/backend-auth-setup`): `User` entity + repo, `AuthController` login/logout/me, dashboard HTML templates.
- **2026-03-24 — Troubleshooting IDE Syntax Errors** (`feature/fix-src-errors`): JDTLS Java 25 JDK reconnection; `build.gradle.kts` updated to Java 25.
- **2026-03-21 — Login Page Design:** Card-based centered form with gradient header/button.

