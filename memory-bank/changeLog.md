# Change Log - Anihan SRMS

## 2026-05-02 - Fix Search Bar Selector, Filter Input Width, DataSeeder
**Branch:** `registrar-retry`

### Task
1. DataTables search input was not widening — CSS selector `.dataTables_filter input` does not match DataTables 2's `.dt-search` class.
2. Batch year From/To filter inputs appeared too wide — Bootstrap `form-control` defaults to `width: 100%` in a flex container.
3. Seed data from `schema.sql` did not auto-apply to the running Docker MySQL — replaced with a Spring `CommandLineRunner` component.

### Files Modified / Created
| File | Change |
|---|---|
| `static/css/dashboard.css` | Added `.dt-search input` to the search bar selector (dual selector covers DataTables 1 + 2). Added `#batchFromYear, #batchToYear { width: 90px; max-width: 90px; flex: 0 0 auto; }` to constrain the year filter inputs. |
| `config/DataSeeder.java` | **Created** — `@Component CommandLineRunner` that idempotently seeds: CARS course, B2024A/B2025A/B2026A batches, SEC-A24/SEC-A25/SEC-A26 sections, and 5 student records (STU-2024-001 through STU-2026-001). Uses `existsById` / `findByStudentId().isEmpty()` before each insert. Runs on every app startup; safe to re-run. |

### Verification
- `./gradlew test` → BUILD SUCCESSFUL — 82 tests, 0 failures (DataSeeder not triggered by any test slice)

---

## 2026-05-02 - Registrar Search Bar Width + Batch Year Filter + Dummy Seed Data
**Branch:** `fix/db-sync-username-unique`

### Task
1. Widen the DataTables search input on the registrar student-records table.
2. Add a Batch Year From/To filter above the table (mirrors the system logs filter pattern).
3. Add 5 fully-populated dummy student records to `schema.sql` (with seeded lookup rows for batch/course/section).

### Files Modified
| File | Change |
|---|---|
| `service/RegistrarService.java` | Added `getAllRecords(String query, Integer fromYear, Integer toYear)` overload. Existing `getAllRecords(String)` and `getAllRecords()` now delegate to the new method. Year filter uses `record.batch.batchYear` and excludes records with no batch when a year is supplied. |
| `controller/RegistrarController.java` | `GET /api/registrar/student-records` now accepts `fromYear` and `toYear` query params. Validates `fromYear <= toYear` and throws `IllegalArgumentException` (→ 400) otherwise. |
| `static/css/dashboard.css` | Added scoped `#studentRecordsTable_wrapper .dataTables_filter input { min-width: 320px; padding: 0.45rem 1rem; }` so the registrar's search input is wider without affecting other tables. |
| `static/registrar.html` | Added a `.logs-filter-bar` block above the table with "Batch Year" From/To number inputs, Apply, and Reset buttons (re-uses the existing system-logs filter CSS classes). |
| `static/js/registrar-students.js` | Stored the DataTable instance, added `buildAjaxUrl()` to assemble `?fromYear/&toYear` params, attached Apply/Reset click handlers that call `dataTable.ajax.url(...).load(...)` and update a feedback span. Also validates From ≤ To client-side. |
| `src/main/sql/schema.sql` | Added seed rows: 1 course (CARS), 3 batches (B2024A/B2025A/B2026A), 3 sections (one per batch), and 5 fully-populated student records spanning batch years 2024–2026. All non-null columns populated; `profile_picture` uses `X''` placeholder per user request (real images to be uploaded later). |
| `test/service/RegistrarBulkLoadTest.java` | Added `batchYearRangeFilterRestrictsResults` test covering closed range, open-ended range, no-match range, and combined query+year filter. |
| `test/controller/RegistrarBulkLoadWebMvcTest.java` | Updated existing mocks to the new 3-arg `getAllRecords` signature; added two tests: filter forwards `fromYear/toYear` params, and `fromYear > toYear` returns HTTP 400. |

### Image Folder Reminder
The user asked where logos/images live so they can upload a real student profile image to replace the placeholder BLOBs. Image assets are at:
`src/main/resources/static/images/`
The placeholder seed data uses `X''` (zero-byte BLOB) which the user explicitly chose to leave empty for now.

### Verification
- `./gradlew test` → BUILD SUCCESSFUL.
- 4 new tests added: 1 service + 3 WebMvc (year filter forwarding, validation rejection).
- Full suite: 15 test classes, 82 tests, 0 failures, 0 skipped.

| Test Class | Tests | Failures |
|---|---|---|
| RegistrarBulkLoadTest | 4 | 0 |
| RegistrarBulkLoadWebMvcTest | 4 | 0 |
| StudentRecordH2LoadTest | 1 | 0 |
| Full suite | 82 | 0 |

---

## 2026-05-01 - Registrar Bulk Load Tests + Server-Side Search + H2 Isolation
**Branch:** `fix/db-sync-username-unique`

### Task
1. Cover the registrar student-records list and search functionality with JUnit tests using 200 dummy records.
2. Add an H2-isolated integration test that persists 100 dummy student records through the real JPA stack — completely separated from the live MySQL.
3. Add a backend search endpoint so the search functionality is testable as a unit.

### Files Created
| File | Purpose |
|---|---|
| `test/service/RegistrarBulkLoadTest.java` | 3 Mockito tests on `RegistrarService`: returns 200 records correctly, < 5s performance, search filters across all 9 searchable fields |
| `test/controller/RegistrarBulkLoadWebMvcTest.java` | 2 WebMvc tests: GET `/api/registrar/student-records` serializes 200 records as JSON in < 5s; `?q=` query param is forwarded to service correctly |
| `test/integration/StudentRecordH2LoadTest.java` | 1 H2-isolated integration test (`@DataJpaTest`) persisting 100 unique `StudentRecord` rows via real Hibernate/JPA in an in-memory H2 database (MySQL compatibility mode). Verifies count, auto-generated IDs, studentId uniqueness, and 5s performance bound |

### Files Modified
| File | Change |
|---|---|
| `service/RegistrarService.java` | Added `getAllRecords(String query)` overload with case-insensitive in-memory filtering across recordId, studentId, lastName, firstName, middleName, studentStatus, batchCode, courseCode, sectionCode |
| `controller/RegistrarController.java` | `GET /api/registrar/student-records` now accepts optional `?q=<query>` parameter (forwarded to the new service method) |
| `build.gradle.kts` | Added `testRuntimeOnly("com.h2database:h2")` for the isolated integration test |

### Spring Boot 4.0 Test Annotation Paths
Spring Boot 4.0 reorganized the test autoconfigure packages. Confirmed for future reference:
- `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
- `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`

(Older Spring Boot 3.x paths `org.springframework.boot.test.autoconfigure.orm.jpa.*` and `org.springframework.boot.autoconfigure.jdbc.*` no longer resolve.)

### Frontend
No frontend changes. The DataTables built-in client-side search is unchanged — the new server-side endpoint is parallel infrastructure to support unit testing and potential future use.

### H2 Isolation Strategy
- `@DataJpaTest` slices the Spring context to JPA components only.
- `@AutoConfigureTestDatabase(replace = Replace.ANY)` forces the test database, ignoring any production datasource.
- `@TestPropertySource` overrides the JDBC URL to `jdbc:h2:mem:studentRecordsTestDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`. The `MODE=MySQL` URL parameter makes H2 accept MySQL-style column types (such as `MEDIUMBLOB` on `StudentRecord.profilePicture`). Hibernate uses the H2Dialect for query generation.
- Hibernate runs `ddl-auto=create-drop`, so the schema is built from entity metadata and dropped at the end. The live MySQL database is never connected to.

### Verification
- `./gradlew test` → BUILD SUCCESSFUL.
- 6 new tests across 3 files, all passing.
- Full suite: 15 test classes, 79 tests, 0 failures, 0 skipped.

| Test Class | Tests | Time | Result |
|---|---|---|---|
| RegistrarBulkLoadTest | 3 | 0.255s | ✅ |
| RegistrarBulkLoadWebMvcTest | 2 | 0.506s | ✅ |
| StudentRecordH2LoadTest | 1 | 1.504s | ✅ |
| Full suite | 79 | ~18s | ✅ |

---

## 2026-05-01 - Registrar Edit Student Record + Search Bar + Unsaved-Changes Notification
**Branch:** `fix/db-sync-username-unique`

### Task
Three additions to the registrar workflow:
1. Searchable registrar home table (across name, record ID, student ID, batch, course, section, status)
2. `student-records.html` becomes a fully-editable form for the record selected via the modal Edit button
3. Notifications when the registrar tries to leave with unsaved changes (admin pattern)

### Files Created
| File | Purpose |
|---|---|
| `repository/BatchRepository.java` | New `JpaRepository<Batch, String>` for batch dropdown lookups |
| `repository/CourseRepository.java` | New `JpaRepository<Course, String>` for course dropdown lookups |
| `dto/registrar/StudentRecordUpdateRequest.java` | Validated DTO for the `PUT /api/registrar/student-records/{recordId}` payload |
| `static/js/registrar-student-records-edit.js` | Loads record by `?id=`, populates form, attaches dirty-tracking + `beforeunload` + nav-intercept confirm dialog, saves via PUT |

### Files Modified
| File | Change |
|---|---|
| `controller/LookupController.java` | Added `GET /api/lookup/batches` and `GET /api/lookup/courses` (any authenticated user). Constructor extended with `BatchRepository` and `CourseRepository`. |
| `service/RegistrarService.java` | Added `updateRecord(Integer, StudentRecordUpdateRequest)` with FK resolution (batch/course/section), studentId-uniqueness check, age recalculation via `AgeCalculator`, and null-safe blank-to-null mapping. Constructor extended with the three lookup repositories. |
| `controller/RegistrarController.java` | Added `PUT /{recordId}` endpoint with `@Valid` body and `SystemLogService.logAction()` call. Constructor extended with `SystemLogService` and `UserRepository`. |
| `static/student-records.html` | Replaced placeholder content with a full edit form (Identifiers, Personal Details, Contact, Family/Religion, Enrollment sections). Record ID and Enrollment Date are read-only; Batch/Course/Section/Status/Sex/Civil Status use `<input list>` + `<datalist>` so users can either pick a code or type free text. Added unsaved-changes alert banner and inline result alert. New script tag for `registrar-student-records-edit.js`. |

### Search Bar (Task 1)
DataTables 2 already renders a built-in search input by default (top-right of the table) since neither admin-users.js nor registrar-students.js sets a custom `dom` option. The input filters across all visible columns — meeting the requirement (name, record ID, student ID, batch, course, section, status). No code change required for this task.

### Unsaved-Changes Notifications (Task 3)
Mirrors the admin `edit-user.html` pattern:
- Any input/change to the form triggers `markDirty()` → shows the yellow warning banner.
- `beforeunload` event handler triggers the browser's native "Leave site?" prompt on refresh/close/external nav.
- Click handlers on `.admin-nav-link`, `.navbar-brand`, `#cancelEditLink`, `#logoutBtn`, `#editAccountBtn` show a JS `confirm('You have unsaved changes. Leave this page?')` and abort navigation if cancelled.
- Successful save clears the dirty state and hides the banner.

### Datalist Pattern (Combined Free-Text + Dropdown)
Per user request, Batch, Course, Section, Status, Sex, and Civil Status use `<input list="...">` paired with a `<datalist>`. This produces a free-text input that also offers an autocomplete dropdown — users can type a value not in the list, or pick from the list.

### Backend Validation
- `studentId`, `lastName`, `firstName`, `middleName`, `studentStatus` are `@NotBlank`.
- `birthdate` and `baptismDate` allow null but must be `@PastOrPresent`.
- Service layer: studentId uniqueness checked when changed; FK codes (batch/course/section), if non-blank, must exist or `IllegalArgumentException` is thrown (caught by `GlobalExceptionHandler` → 400).
- Age is recalculated from birthdate via `AgeCalculator` (existing utility).

### Verification
- `./gradlew compileJava` → BUILD SUCCESSFUL.
- Manual browser testing deferred to user (login as registrar → use search → click Open Details → click Edit → form populates → modify a field → confirm warning banner appears → try to leave → confirm prompt shows → save → success alert).

---

## 2026-05-01 - Registrar Home: Student Records Table & Detail Modal
**Branch:** `fix/db-sync-username-unique`

### Task
Build the registrar home page as a student-records dashboard mirroring the admin user dashboard. Add a DataTables table, a detail modal showing the full record, and an Edit button that links to `student-records.html` (now repurposed as a registrar-side placeholder).

### Files Created
| File | Purpose |
|---|---|
| `dto/registrar/StudentRecordSummaryResponse.java` | 8-field record DTO for table rows (recordId, studentId, lastName, firstName, batchCode, courseCode, sectionCode, studentStatus) — flattens FK relations to codes |
| `dto/registrar/StudentRecordDetailsResponse.java` | Full-record DTO (excludes BLOB profilePicture) — 25 fields, FK relations flattened to codes |
| `service/RegistrarService.java` | `getAllRecords()` and `getRecordById(Integer)` — uses `StudentRecordRepository.findAll()` / `findById(Integer)` (already existing). Throws `NoSuchElementException` on miss (handled by `GlobalExceptionHandler` → 404). |
| `controller/RegistrarController.java` | `GET /api/registrar/student-records` and `GET /api/registrar/student-records/{recordId}` |
| `static/js/registrar-students.js` | DataTables 2 init, AJAX from `/api/registrar/student-records`, click-to-load detail modal, renders `'null'` for empty/null values |

### Files Modified
| File | Change |
|---|---|
| `static/registrar.html` | Added `datatables.min.css`. Replaced empty `<main>` with `page-hero` + `surface-card` containing `#studentRecordsTable` (9 columns: Record ID, Student ID, Last Name, First Name, Batch, Course, Section, Status, Details button). Added `#studentRecordDetailsModal` with all 25 fields + Edit/Close buttons. Added jQuery, DataTables JS, and `registrar-students.js` script tags. |
| `static/student-records.html` | Repurposed for registrar: title → "Student Records — Registrar"; `data-required-role` ADMIN → REGISTRAR; navbar swapped from 4-link admin nav to 2-link registrar nav (Home, Subjects); brand href → `registrar.html`; portal label → "Registrar Portal". Main content kept as placeholder per user instruction. |
| `config/SecurityConfig.java` | Moved `/student-records.html` from ADMIN to REGISTRAR matcher. `/api/registrar/**` already covers the new endpoints. |

### Behavior
- Null/empty fields render the literal text `"null"` in the table and detail modal (per user instruction).
- Status badge maps "Active"/"Submitted" → green active badge; everything else → grey/disabled badge.
- Edit button in modal links to `student-records.html?id={recordId}` (placeholder page reachable; no edit logic yet).

### Verification
- `./gradlew compileJava` → BUILD SUCCESSFUL.
- Manual verification deferred to user (login as registrar → table populates, click Open Details → modal opens, click Edit → navigates to `student-records.html?id=...`).

---

## 2026-05-01 - Live Age Recalculation in Edit Account Modal
**Branch:** `fix/db-sync-username-unique`

### Task
Ensure the Age field in the Edit Account modal is non-editable and auto-calculated from the Birthdate field on the registrar and trainer sides, matching admin behavior.

### Audit Result
- `registrar.html`, `trainer.html`, and `admin.html` all already render Age as a read-only `<p id="ageDisplay">` (no editable input) and Birthdate as `<input type="date" id="birthdate">`. All three load the shared `auth-guard.js`.
- However, `auth-guard.js` only set `ageDisplay` once on initial load. If the user changed the birthdate, the age stayed stale until page reload. After a successful save, the form did not refresh either.

### Files Modified
| File | Change |
|---|---|
| `static/js/auth-guard.js` | Added `calculateAgeFromBirthdate()` and `updateAgeDisplay()` helpers. Attached a `change` listener to `#birthdate` (idempotent via `dataset.ageListenerAttached`) so `#ageDisplay` updates live. Added explicit refresh of `#ageDisplay` and `#birthdate` from the server response in the personal-details save success branch. |

### Effect
- Admin, registrar, and trainer all benefit from the same live recalculation behavior since they share `auth-guard.js`.
- Age remains server-authoritative on persistence (the backend `AgeCalculator` still runs on save and read); the client-side calc is purely a UI preview.

### Verification
- Manual test plan: open Edit Account modal → change birthdate → age display updates immediately → click Save → success alert + age remains consistent with server.

---

## 2026-05-01 - Registrar Navbar Standardization
**Branch:** `fix/db-sync-username-unique`

### Task
Add a navbar to all registrar pages matching the admin navbar pattern, with two links: Home and Subjects. Subjects page becomes REGISTRAR-only (Admin loses access).

### Files Modified
| File | Change |
|---|---|
| `static/registrar.html` | Replaced simple placeholder navbar (logo + label + dropdown) with full `navbar-expand-lg login-navbar admin-navbar` collapsible navbar containing Home (active) and Subjects nav links + Registrar Portal label + account dropdown. Added `class="dashboard-page"` to `<body>`. |
| `static/subjects.html` | Rebranded from admin to registrar: `<title>` → `Subjects — Registrar`; `data-required-role` → `ROLE_REGISTRAR`; navbar reduced from 4 admin links to 2 registrar links (Home, Subjects active); `navbar-brand` href → `registrar.html`; collapse target id → `registrarNavbar`; portal label → `Registrar Portal`. Main content kept as placeholder. |
| `config/SecurityConfig.java` | Moved `/subjects.html` out of the ADMIN `requestMatchers` and into the REGISTRAR matcher alongside `/registrar.html`. |

### CSS / JS
- No CSS or JS changes — reused `admin-navbar`, `admin-nav-link`, `portal-label`, `account-dropdown` from `dashboard.css`.

### Verification
- Code review: navbar markup matches admin pattern; nav links resolve to existing pages.
- `SecurityConfig` change keeps role isolation intact (admin → admin pages; registrar → registrar.html + subjects.html).

---

## 2026-04-30 - Enrollment Flow Bug Audit & Fixes
**Branch:** `fix/db-sync-username-unique`

### Task
Full audit of the enrollment flow (portal → wizard → service → DB) for potential bugs, then fix critical and cleanup issues.

### Bugs Found: 5

| Bug | Severity | Status |
|---|---|---|
| Bug 3 — `Parent`/`OtherGuardian` `@JoinColumn` FK mismatch | 🔴 Critical | **Fixed** |
| Bug 4 — `student_records.email` not in enrollment flow | 🟡 Medium | Open (needs product decision) |
| Bug 5 — `AgeCalculator` returns 0 instead of null | 🟡 Medium | Open |
| Bug 6 — Duplicate security authorization rules | 🟢 Low | **Fixed** |
| Bug 7 — `saveDraft()` failure swallowed before submit | 🟡 Medium | **Fixed (Option A)** |

### Files Changed

| File | Change |
|---|---|
| `Parent.java` | Added `referencedColumnName = "student_id"` to `@JoinColumn` |
| `OtherGuardian.java` | Added `referencedColumnName = "student_id"` to `@JoinColumn` |
| `SecurityConfig.java` | Consolidated duplicate `requestMatchers` into single clean block |
| `student-details.js` | `saveDraft()` returns boolean; submit handler blocks on failure |
| `memory-bank/bugs.md` | **Created** — full bug tracker for team documentation |

### Bug 3 Fix Detail
After Bug 2 moved `@Id` from `studentId` (String) to `recordId` (Integer), the `@ManyToOne @JoinColumn(name = "student_id")` in `Parent` and `OtherGuardian` defaulted to joining `student_records.record_id` (INT) instead of `student_records.student_id` (VARCHAR). All 9 child table FKs in the DB point to `student_records.student_id`. Fix: `referencedColumnName = "student_id"`.

### Bug 6 Fix Detail
`/admin.html`, `/registrar.html`, `/trainer.html` had duplicate `requestMatchers` rules at lines 57-59 and 65-67. Consolidated into a single set with HTML pages and API endpoints clearly separated.

### Bug 7 Fix Detail (Option A)
`saveDraft()` refactored to return `true`/`false` instead of swallowing errors. Submit handler now validates first, then saves — if save fails, submission is **blocked** with error: *"Your data could not be saved. Please check your connection and try again."* The "Next" button retains best-effort save behavior (ignores return value) to provide browser-crash recovery.

### Validation
- `./gradlew build -x test` → BUILD SUCCESSFUL (4s)

---

## 2026-04-30 - Database Sync from AnihanSRMS.sql (No Data Drop)
**Branch:** `fix/db-sync-username-unique`

### Task
Update the live AnihanSRMS database to match `src/main/sql/AnihanSRMS.sql` without dropping any existing data.

### Audit Summary
Full column-by-column comparison of all 17 canonical tables between the live MySQL database and `AnihanSRMS.sql`.

| Table | Column Match | Nullability Match | Index Match | Result |
|---|---|---|---|---|
| batches | ✅ | ✅ | ✅ | OK |
| courses | ✅ | ✅ | ✅ | OK |
| qualifications | ✅ | ✅ | ✅ | OK |
| sections | ✅ | ✅ | ✅ | OK |
| subjects | ✅ | ✅ | ✅ | OK |
| **users** | ✅ | ✅ | **❌ Missing UNIQUE on username** | **Fixed** |
| student_records | ✅ | ✅ | ✅ | OK |
| parents | ✅ | ✅ | ✅ | OK |
| other_guardians | ✅ | ✅ | ✅ | OK |
| documents | ✅ | ✅ | ✅ | OK |
| grades | ✅ | ✅ | ✅ | OK |
| system_logs | ✅ | ✅ | ✅ | OK |
| student_education | ✅ | ✅ | ✅ | OK |
| student_school_years | ✅ | ✅ | ✅ | OK |
| student_ojt | ✅ | ✅ | ✅ | OK |
| student_tesda_qualifications | ✅ | ✅ | ✅ | OK |
| student_uploads | ✅ | ✅ | ✅ | OK |

### Fix Applied
| Change | Detail |
|---|---|
| `ALTER TABLE users ADD UNIQUE INDEX uq_username (username)` | `AnihanSRMS.sql` declares `username VARCHAR(255) NOT NULL UNIQUE` and `User.java` has `@Column(unique=true)`. The live DB was missing this constraint. |

### Pre-Check
- Verified no duplicate usernames exist before adding UNIQUE index

### Data Preserved
| Table | Row Count |
|---|---|
| users | 3 |
| student_records | 7 |
| system_logs | 85 |
| parents | 7 |
| student_education | 24 |

### Backend Validation
- `./gradlew build -x test` → BUILD SUCCESSFUL
- `./gradlew bootRun` → Started in 9.3s, no errors
- Hibernate 7.2.7 connected to MySQL 8.0.45, initialized 12 JPA repositories
- No schema validation warnings

### JPA Entity Cross-Reference
All 17 JPA entity files verified against live DB columns — full alignment confirmed.

---

## 2026-04-30 - Bug Fix: student_records.age NOT NULL Blocks Enrollment (Bug 1)
**Branch:** `feature/student-details`

### Root Cause
`student_records.age` was `NOT NULL` with no default value in the live MySQL database. `StudentDetailsService.startOrResume()` creates a new `StudentRecord` with only the student's name and status set — `age` is intentionally left null because birthdate hasn't been provided yet (it's entered in Step 1 of the wizard). MySQL rejected the INSERT with a `Column 'age' cannot be null` error → Spring threw `DataIntegrityViolationException` → controller returned HTTP 500 → JavaScript caught it and displayed "Failed to start enrollment. Please go back and try again."

### Fix Applied
| Change | Detail |
|---|---|
| **DB migration** | `ALTER TABLE student_records MODIFY COLUMN age INT NULL` — applied to live MySQL |
| **`AnihanSRMS.sql`** | Added clarifying comment to `age INT NULL` line: `-- calculated from birthdate; null until Step 1 is saved` |

### No Java Code Changes Required
`StudentRecord.java` already declared `age` as `Integer` (nullable in Java). The bug was purely a DB constraint mismatch.

### Verification
- `information_schema.COLUMNS` → `IS_NULLABLE: YES` for `student_records.age` ✅
- Enrollment wizard `POST /api/student/start` should now succeed for new students

---

## 2026-04-30 - Bug Fix: StudentRecord JPA @Id Mismatch (Bug 2)
**Branch:** `feature/student-details`

### Root Cause
`StudentRecord.java` mapped `@Id` to `student_id` (VARCHAR UNIQUE KEY), not `record_id` (INT AUTO_INCREMENT PRIMARY KEY). JPA identity resolution was technically incorrect — JPA was calling `merge()` on new records instead of `persist()` because the manually-set String ID appeared non-null.

### Files Modified
| File | Change |
|---|---|
| `model/StudentRecord.java` | Added `@Id @GeneratedValue(IDENTITY)` on new `recordId` field mapped to `record_id`. Demoted `studentId` to `@Column(unique=true, nullable=false)`. Added `getRecordId()`/`setRecordId()` accessors. Added `GeneratedValue` and `GenerationType` imports. |
| `repository/StudentRecordRepository.java` | Changed `JpaRepository<StudentRecord, String>` → `JpaRepository<StudentRecord, Integer>`. Added `Optional<StudentRecord> findByStudentId(String studentId)` for service lookups. |
| `service/StudentDetailsService.java` | Replaced both `studentRecordRepo.findById(studentId)` call sites with `studentRecordRepo.findByStudentId(studentId)` (the old `findById` now expects Integer, not String). |

### Not Changed
- No database schema changes — `record_id` and `student_id` were already correct in the DB
- `StudentPortalController.java` — unaffected (only uses name-based finder methods)

### Known Open Issue
- ⚠️ Bug 1 still open: `student_records.age` is `NOT NULL` in the live DB with no default. `startOrResume()` inserts with `age = null`, causing `DataIntegrityViolationException` → HTTP 500 → "Failed to start enrollment" alert. Fix: `ALTER TABLE student_records MODIFY COLUMN age INT NULL;` (pending user approval)

### Verification
- `./gradlew build -x test` → BUILD SUCCESSFUL

---

## 2026-04-30 - Database Schema Audit & Migration
**Branch:** `feature/student-details`

### Audit Summary
Full comparison of `AnihanSRMS.sql` against the live local MySQL database. Found 4 missing tables, 1 missing column, multiple NOT NULL mismatches, and 1 missing unique index.

### Migrations Applied (Live MySQL — 6 Steps)
| Step | SQL Applied | Reason |
|---|---|---|
| 1a | `CREATE TABLE student_education` | Missing from live DB; required by wizard |
| 1b | `CREATE TABLE student_school_years` | Missing from live DB; required by wizard |
| 1c | `CREATE TABLE student_ojt` | Missing from live DB; required by wizard |
| 1d | `CREATE TABLE student_tesda_qualifications` | Missing from live DB; required by wizard |
| 1e | `CREATE TABLE student_uploads` | Missing from live DB; required by wizard |
| 2 | `ALTER TABLE student_records ADD COLUMN civil_status VARCHAR(50) NULL AFTER sex` | Missing column; service sets it |
| 3 | `ALTER TABLE student_records MODIFY COLUMN` (11 columns → NULL) | birthdate, sex, permanent_address, email, contact_no, religion, baptism_place, sibling_count, batch_code, course_code, section_code |
| 4 | `ALTER TABLE parents MODIFY COLUMN` (9 columns → NULL) | family_name, first_name, middle_name, birthdate, occupation, est_income, contact_no, email, address |
| 5 | `ALTER TABLE other_guardians MODIFY COLUMN` (6 columns → NULL) | relation, last_name, first_name, middle_name, birthdate, address |
| 6 | `ALTER TABLE users ADD UNIQUE INDEX idx_users_username (username)` | Missing unique constraint |

### Legacy Tables (Left Untouched)
`classess`, `log`, `previous_school`, `qualification_assessment` — present in live DB only; no JPA entities reference them.

### Verification
- All 5 new student tables confirmed via `SHOW TABLES`
- `civil_status` column confirmed in `student_records`
- All nullability changes confirmed via `information_schema.COLUMNS`
- `idx_users_username` confirmed with `NON_UNIQUE = 0`

---

## 2026-04-29 - SQL Files Synced with Live Database
**Branch:** `feature/student-details`

### Files Modified
| File | Change |
|---|---|
| `src/main/sql/AnihanSRMS.sql` | Rewritten: 17 table DDLs (was 12) + 3 live user accounts. Removed old system_logs data dump. Added 5 new student tables, nullable columns on student_records/parents/other_guardians, civil_status column. |
| `src/main/sql/schema.sql` | Rewritten: 17 table DDLs + 3 dummy seed accounts. Removed stale migration comments. |

### Verification
- All 17 tables confirmed in live MySQL
- DDL matches live INFORMATION_SCHEMA column definitions
- User data matches live `users` table (admin age=22, trainer password_changed_at set)

---

## 2026-04-29 - Submit Button Fix (Cache-Busting + Error Handling)
**Branch:** `feature/student-details`

### Files Modified
| File | Change |
|---|---|
| `static/student-details.html` | Added `?v=2` cache-busting query string to `student-details.js` script tag |
| `static/js/student-details.js` | Removed `throw e` from `saveDraft()` catch block; wrapped `saveDraft()` in Submit handler with try/catch |

### Root Cause
Browser caching a stale `student-details.js` that still referenced a removed `btnSaveDraft` button. The `getElementById('btnSaveDraft')` returned null, `.addEventListener()` on null threw a TypeError, and `setupNavButtons()` aborted before the Submit listener was attached.

Secondary issue: `saveDraft()` rethrowing errors would crash the Submit handler (and Next handler) if a draft-save PUT request failed.

### Verification
- Confirmed `btnSaveDraft` does not exist in current HTML or JS source
- `saveDraft()` no longer rethrows — shows user-facing alert instead
- Submit handler is resilient to draft-save failures

---

## 2026-04-29 - Student Details Enrollment Wizard
**Branch:** `feature/student-details`

### Files Created
| File | Purpose |
|---|---|
| `model/StudentEducation.java` | JPA entity for `student_education` (prior school history) |
| `model/StudentSchoolYear.java` | JPA entity for `student_school_years` (semesters at Anihan) |
| `model/StudentOjt.java` | JPA entity for `student_ojt` (1:1 OJT record) |
| `model/StudentTesdaQualification.java` | JPA entity for `student_tesda_qualifications` (up to 3 slots) |
| `model/StudentUpload.java` | JPA entity for `student_uploads` (file metadata, disk storage) |
| `repository/ParentRepository.java` | Parent JPA repo with relation-based finders |
| `repository/OtherGuardianRepository.java` | OtherGuardian JPA repo |
| `repository/StudentEducationRepository.java` | findByStudentIdAndLevel, findByStudentIdOrderByLevel |
| `repository/StudentSchoolYearRepository.java` | findByStudentIdOrderByRowIndex |
| `repository/StudentOjtRepository.java` | findByStudentId |
| `repository/StudentTesdaQualificationRepository.java` | findByStudentIdOrderBySlot |
| `repository/StudentUploadRepository.java` | findByStudentIdAndKind |
| `dto/student/ParentDto.java` | Family name, contact, occupation, income record |
| `dto/student/GuardianDto.java` | Guardian info record |
| `dto/student/EducationItemDto.java` | Prior school history row record |
| `dto/student/SchoolYearDto.java` | Anihan semester row record |
| `dto/student/OjtDto.java` | OJT company/hours record |
| `dto/student/TesdaQualDto.java` | TESDA qualification slot record |
| `dto/student/UploadRefDto.java` | Upload metadata reference record |
| `dto/student/StudentDetailsRequest.java` | Full wizard save request DTO |
| `dto/student/StudentDetailsResponse.java` | Full wizard load response DTO |
| `service/StorageService.java` | Local disk file storage (validate, store, load, delete) |
| `service/StudentDetailsService.java` | startOrResume, load, saveDraft, submit, saveUpload |
| `controller/StudentDetailsController.java` | 6 REST endpoints under `/api/student/**` |
| `static/js/student-details.js` | Wizard logic: init, step nav, draft save, file upload, submit |

### Files Modified
| File | Change |
|---|---|
| `model/StudentRecord.java` | Made most fields nullable; added `civilStatus`, `age`; batch/course/section `optional=true` |
| `model/Parent.java` | Made all non-required fields nullable |
| `model/OtherGuardian.java` | Made all non-required fields nullable |
| `repository/StudentRecordRepository.java` | Added `findByLastName...` and `countByStudentIdStartingWith` |
| `controller/StudentPortalController.java` | check-duplicate now only blocks Submitted/Active (Enrolling/Draft = resumable) |
| `config/SecurityConfig.java` | Added `/api/student/**` to `permitAll()` |
| `resources/application.properties` | Added `app.storage.root`, multipart max file/request sizes |
| `static/student-details.html` | Replaced placeholder with full 4-step Bootstrap wizard |
| `src/main/sql/schema.sql` | Corrected CREATE TABLE definitions + 5 new tables + migration section |
| `memory-bank/activeContext.md` | Updated phase, branch, status |
| `memory-bank/progress.md` | Added student details wizard completed section |
| `memory-bank/changeLog.md` | This entry |

### Database Migrations Applied (live Docker MySQL)
```sql
-- Made student_records columns nullable (birthdate, age, sex, permanent_address,
--   email, contact_no, religion, baptism_place, sibling_count, batch_code,
--   course_code, section_code)
-- Added civil_status VARCHAR(50) NULL to student_records
-- Made parents fields nullable (family_name, first_name, middle_name, birthdate,
--   occupation, est_income, contact_no, email, address)
-- Made other_guardians fields nullable (relation, last_name, first_name,
--   middle_name, birthdate, address)
-- CREATE TABLE student_education
-- CREATE TABLE student_school_years
-- CREATE TABLE student_ojt
-- CREATE TABLE student_tesda_qualifications
-- CREATE TABLE student_uploads
```

### Verification
- `./gradlew build -x test` → BUILD SUCCESSFUL
- `SHOW TABLES` → 17 tables confirmed in live MySQL

---

## 2026-04-27 - Admin Users Table Column Split (Name → Last Name + First Name)
**Branch:** `test-user-table`

### Files Modified
| File | Change |
|---|---|
| `static/admin.html` | Replaced single `<th>Name</th>` with `<th>Last Name</th>` + `<th>First Name</th>` in Users table header (6 → 7 columns) |
| `static/js/admin-users.js` | Replaced single combined `formatName(row)` DataTables column with two separate `data: 'lastName'` and `data: 'firstName'` columns |

### Not Modified (Intentional)
| File | Reason |
|---|---|
| `dto/AdminUserResponse.java` | Already has separate `lastName` and `firstName` fields — no backend changes needed |
| All test files | No test asserts on HTML table column structure — all 63 tests pass unchanged |

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7 tasks, all tests green)

---
## 2026-04-26 - Database Schema Sync & SQL Export Files
**Branch:** `test-user-table` (no branch change per user request)

### Database Tables Created (Live Docker MySQL)
| Table | Purpose |
|---|---|
| `qualifications` | Qualification codes for subjects |
| `subjects` | Subject catalog linked to qualifications |
| `parents` | Parent/guardian info linked to student records |
| `other_guardians` | Additional guardian info linked to student records |
| `documents` | Uploaded student documents (LONGBLOB) |
| `grades` | Student grades per subject |

### Database Index Added
| Change | Purpose |
|---|---|
| `UNIQUE INDEX idx_student_id ON student_records (student_id)` | Required for FK references from parents, other_guardians, documents, grades |

### Files Created/Updated
| File | Action | Purpose |
|---|---|---|
| `src/main/sql/AnihanSRMS.sql` | **Rewritten** | Full database dump (12 tables DDL + 3 user accounts + 79 system log entries) for cloning to a new device |
| `src/main/sql/schema.sql` | **Created** | Clean schema (12 tables DDL) + 3 dummy seed accounts (Juan Dela Cruz/admin, Maria Reyes/registrar, Carlos Santos/trainer). No system log data. |

### Key Changes from Old AnihanSRMS.sql
- Removed stray `main` text (Git merge conflict remnant)
- Removed duplicate/broken `ALTER TABLE` blocks
- Removed stray closing `);` near EOF
- Removed obsolete `log` table (replaced by `system_logs`)
- Removed obsolete `classess` and `qualification_assessment` tables (no JPA entities)
- Removed obsolete `previous_school` table (no JPA entity)
- Added 6 missing table DDLs matching JPA entities
- Added `CREATE DATABASE` + `USE` header for standalone execution
- Added `UNIQUE KEY idx_student_id` on `student_records` for FK support
- All timestamps converted to PHT (UTC+8) for consistency

### Verification
- `SHOW TABLES` → 12 tables confirmed in live database
- All 6 new tables created successfully
- Existing data (3 users, 79 system logs) preserved

---
## 2026-04-19 - Age Auto-Calculation from Birthdate
**Branch:** `test-user-table`

### Files Created
| File | Purpose |
|---|---|
| `service/AgeCalculator.java` | Static utility for computing age from birthdate via `java.time.Period` |

### Files Modified
| File | Change |
|---|---|
| `dto/AdminUpdateUserRequest.java` | Removed `age` field and `@Min`/`@Max` annotations |
| `dto/AdminCreateUserRequest.java` | Removed `age` field; added `@NotNull` to `birthdate` |
| `dto/UpdatePersonalDetailsRequest.java` | Removed `age` field from record |
| `service/AdminService.java` | Auto-calculate age in `createUser()`, `updateUser()`, `getUserById()` via `AgeCalculator` |
| `service/AccountService.java` | Auto-calculate age in `updatePersonalDetails()` via `AgeCalculator` |
| `controller/AuthController.java` | Silently recalculate + persist age on `GET /api/auth/me` (no system log) |
| `static/admin.html` | Self-service modal: age `<input>` → read-only `<p>` display |
| `static/edit-user.html` | Admin form: removed age input (birthdate full-width); modal: age display-only |
| `static/add-user.html` | Removed age input; birthdate required with no default; sidebar defaults updated |
| `static/registrar.html` | Modal: age `<input>` → read-only display |
| `static/trainer.html` | Modal: age `<input>` → read-only display |
| `static/logs.html` | Modal: age `<input>` → read-only display |
| `static/js/admin-edit-user.js` | Removed `age` from `buildPayload()` and `populateForm()` |
| `static/js/admin-add-user.js` | Removed `age` from `buildPayload()`; added birthdate validation |
| `static/js/auth-guard.js` | Age displayed via `#ageDisplay` text; removed `age` from save payload |
| `test/service/AdminServiceTest.java` | Updated `AdminUpdateUserRequest` constructors (removed age param); age assertion uses `AgeCalculator` |
| `test/service/AccountServiceTest.java` | Updated `UpdatePersonalDetailsRequest` constructors (removed age param); age assertion uses `AgeCalculator` |
| `test/controller/AdminControllerWebMvcTest.java` | Removed `age` from invalid payload JSON |
| `test/controller/AccountControllerWebMvcTest.java` | Removed `age` from JSON payload; age assertion uses `.isNumber()` |
| `memory-bank/activeContext.md` | Updated to reflect age auto-calculation phase |
| `memory-bank/progress.md` | Added age auto-calculation completed section |
| `memory-bank/changeLog.md` | This entry |
| `memory-bank/decisions.md` | Added age calculation design decisions |
| `memory-bank/testing.md` | Added age-related test verification notes |

### Verification
- `./gradlew test` → BUILD SUCCESSFUL (63 tests, 0 failures, 0 skipped)

---
## 2026-04-18 - System Logs Export UI Cleanup
**Branch:** `feature/export-logs`

### Files Created
| File | Purpose |
|---|---|
| `service/SystemLogExportFormat.java` | Enum for supported export formats and media types |
| `service/SystemLogExportFile.java` | Record carrying exported bytes, media type, and filename |
| `service/SystemLogQueryResult.java` | Shared filtered log payload with resolved date window |
| `service/SystemLogExportService.java` | Generates CSV, XLSX, and DOCX exports |
| `test/service/SystemLogExportServiceTest.java` | Verifies export file generation for all supported formats |

### Files Modified
| File | Change |
|---|---|
| `build.gradle.kts` | Added Apache POI dependency for XLSX and DOCX generation |
| `service/SystemLogService.java` | Added `queryLogs()` so page rendering and export share the same resolved filter window |
| `controller/SystemLogController.java` | Added `GET /api/logs/export` with attachment response handling |
| `static/logs.html` | Removed redundant extra date-selection controls and kept the export toolbar |
| `static/js/system-logs.js` | Reworked filter state handling so presets, exact dates, and export share one flow |
| `test/controller/SystemLogControllerWebMvcTest.java` | Added export endpoint success, validation, and security coverage |
| `memory-bank/activeContext.md` | Corrected active branch and recorded the export enhancement |
| `memory-bank/progress.md` | Added completed export cleanup work |
| `memory-bank/testing.md` | Added export-related automated verification results |
| `memory-bank/decisions.md` | Added API-contract and server-side export decisions |
| `memory-bank/changeLog.md` | This entry |

### Verification
- `./gradlew test` -> BUILD SUCCESSFUL (63 tests, 0 failures, 0 skipped)
- `git diff --check` -> no tracked whitespace or conflict-marker errors
- Logs page now exports the selected range as `.csv`, `.xlsx`, or `.docx`

## 2026-04-18 - System Logs Date Filtering Enhancement
**Branch:** `feature/logs-date-filter`

### Files Modified
| File | Change |
|---|---|
| `repository/SystemLogRepository.java` | Added `findByTimestampBetweenOrderByTimestampDesc()` range query method |
| `service/SystemLogService.java` | Replaced `getAllLogs()` with `getLogs(rangeDays, startDate, endDate)` implementing filter precedence: custom range > preset days > default 7 days |
| `controller/SystemLogController.java` | Added optional `rangeDays`, `startDate`, `endDate` query params with `@DateTimeFormat`; catches `IllegalArgumentException` → 400 |
| `static/logs.html` | Added date filter toolbar (preset 7/14/30-day pills + custom From/To date inputs + Apply/Reset buttons) with matching CSS |
| `static/js/system-logs.js` | Refactored `loadLogs()` to accept filter params and build query URL; added preset, apply, and reset event handlers; default load is 7 days |
| `test/service/SystemLogServiceTest.java` | Rewrote: 10 tests (2 logAction unchanged + 8 new getLogs tests covering default, 14d, 30d, custom range, precedence, invalid range, empty result, DTO mapping) |
| `test/controller/SystemLogControllerWebMvcTest.java` | Rewrote: 9 tests (6 new filter tests + 3 unchanged security tests) |
| `memory-bank/activeContext.md` | Updated phase, branch, status, and verified section |
| `memory-bank/progress.md` | Added "System Logs Date Filtering" completed section |
| `memory-bank/testing.md` | Added date filtering test results section |
| `memory-bank/changeLog.md` | This entry |
| `memory-bank/decisions.md` | Added filter precedence decision record |

### Verification
- `./gradlew test` → BUILD SUCCESSFUL (52 tests, 0 failures, 19.998s)
- No references to removed `getAllLogs()` method remain
- Existing security tests (403/401) pass unchanged

---

## 2026-04-18 - Admin Bulk Load Tests (100 Users)
**Branch:** `feature/unit-tests-coverage`

### Files Created
| File | Purpose |
|---|---|
| `test/service/AdminBulkLoadTest.java` | 3 unit tests verifying `AdminService.getAllUsers()` handles 100 users (count, DTO mapping, performance) |
| `test/controller/AdminBulkLoadWebMvcTest.java` | 2 WebMvc tests verifying `GET /api/admin/users` serializes 100 users as JSON with correct content and timing |

### Files Modified
| File | Change |
|---|---|
| `memory-bank/testing.md` | Added "Admin Users Table — Bulk Load Tests" section with detailed results |
| `memory-bank/progress.md` | Added "Admin Bulk Load Tests" completed section |
| `memory-bank/activeContext.md` | Updated current phase and status for April 18 session |

### Not Modified (Intentional)
| File | Reason |
|---|---|
| `test/service/AdminServiceTest.java` | Existing tests preserved — bulk tests placed in a separate file |
| `test/controller/AdminControllerWebMvcTest.java` | Existing tests preserved — bulk tests placed in a separate file |
| `build.gradle.kts` | No new dependencies needed — bulk tests use only existing JUnit 5, Mockito, Spring Test |

### Verification
- `./gradlew test` → BUILD SUCCESSFUL (42 tests, 0 failures, 12.897s total)
- AdminBulkLoadTest: 3/3 passed in 0.008s
- AdminBulkLoadWebMvcTest: 2/2 passed in 0.663s
- Both well within 5-second non-functional requirement

---

## 2026-04-17 - Admin Navbar Cleanup (Student Records & Subjects Removed)
**Branch:** `feature/unit-tests-coverage`

### Files Modified
| File | Change |
|---|---|
| `static/admin.html` | Removed "Student Records" and "Subjects" `<li>` nav items from navbar |
| `static/edit-user.html` | Same — removed "Student Records" and "Subjects" nav items |
| `static/add-user.html` | Same — removed "Student Records" and "Subjects" nav items |
| `static/logs.html` | Same — removed "Student Records" and "Subjects" nav items |
| `memory-bank/systemPatterns.md` | Updated nav links pattern to "Home | Logs", added stale navbar warning |
| `memory-bank/activeContext.md` | Recorded navbar cleanup and stale navbar warning |

### Not Modified (Intentional)
| File | Reason |
|---|---|
| `static/student-records.html` | Page kept for future use — internal navbar NOT updated (still has old 4-link nav) |
| `static/subjects.html` | Page kept for future use — internal navbar NOT updated (still has old 4-link nav) |

### Verification
- Admin navbar now shows: **Home | Logs** across all active admin pages
- No CSS changes needed — nav items are standard Bootstrap `<li>` elements
- `student-records.html` and `subjects.html` remain accessible via direct URL and `SecurityConfig.java` route protection

---

## 2026-04-17 - Database Migration Fix & SQL Cleanup
**Branch:** `feature/unit-tests-coverage`

### Database Migrations Applied (Docker `mysql-server` container)
| Statement | Purpose |
|---|---|
| `ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;` | Adds soft-delete support column expected by `User.java` and `CustomUserDetailsService.java` |
| `ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL;` | Adds password tracking column expected by `User.java` and `AdminUserResponse.java` |
| `CREATE TABLE IF NOT EXISTS system_logs (...)` | Creates audit log table expected by `SystemLogService.java` and `AuthController.java` |

### SQL File Cleanup — `AnihanSRMS.sql`
| Change | Description |
|---|---|
| Removed dangling `ALTER TABLE users ADD COLUMN ...` block (lines 40–49) | Leftover merge-conflict artifact that would error on fresh import |
| Removed bare `main` text (line 53) | Git merge conflict remnant |
| Removed stray closing `)` near EOF | Syntax error from merge conflict |
| Commented out inline `ALTER TABLE student_records` | Converted to comment to prevent accidental execution |

### Root Cause
The Docker MySQL database had been recreated or reverted since the last session where these migrations were applied. The seeded accounts existed but the schema was outdated, causing Hibernate SQL errors on login.

### Verification
- `DESCRIBE AnihanSRMS.users` → 12 columns including `enabled` and `password_changed_at`
- `DESCRIBE AnihanSRMS.system_logs` → 7 columns with timestamp index
- All 3 existing accounts (`Ado`, `registrar`, `trainer`) preserved with `enabled=1`

---

## 2026-04-17 - Unit Test Coverage Expansion
**Branch:** `feature/unit-tests-coverage`

### Files Created
| File | Purpose |
|---|---|
| `test/service/AccountServiceTest.java` | 10 unit tests for `updateUsername`, `updatePassword`, `updatePersonalDetails` |
| `test/service/SystemLogServiceTest.java` | 4 unit tests for `logAction`, `getAllLogs` |
| `test/controller/AccountControllerWebMvcTest.java` | 7 WebMvc tests for `/api/account/**` endpoints |
| `test/controller/SystemLogControllerWebMvcTest.java` | 5 WebMvc tests for `/api/logs` endpoint |

### Verification
- `./gradlew test` → BUILD SUCCESSFUL (all tests green)
- Total test coverage now spans: Admin, Account, SystemLog modules

---

## 2026-04-16 - Navbar Logo UI Standardization
**Branch:** `ui-style/fix`

### Files Modified
| File | Change |
|---|---|
| `static/admin.html` | Removed `.brand-title` span ("Anihan SRMS") from navbar |
| `static/add-user.html` | Removed `.brand-title` span from navbar |
| `static/edit-user.html` | Removed `.brand-title` span from navbar |
| `static/logs.html` | Removed `.brand-title` span from navbar, removed inline `max-height` restriction from logo |
| `static/student-records.html` | Removed `.brand-title` span from navbar |
| `static/subjects.html` | Removed `.brand-title` span from navbar |
| `static/css/login.css` | Enlarged `.navbar-logo` height to `85px` and applied `-22px` vertical margins to keep navbar size intact |
| `static/css/dashboard.css` | Removed unused `.brand-title` CSS rules |

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (html/css changes verified)
- Checked layouts for uniformity across all referenced HTML files.

---

## 2026-04-14 - Account Icon Dropdown on All Admin Pages
**Branch:** `feature/admin-system-logs`

### Files Modified
| File | Change |
|---|---|
| `static/student-records.html` | Added brand-mark navbar, account dropdown, Edit Account modal, `dashboard.css`, `auth-guard.js`, `data-required-role` |
| `static/subjects.html` | Same as above |
| `static/logs.html` | Same as above (already had `auth-guard.js` but lacked account dropdown/modal) |
| `memory-bank/systemPatterns.md` | Added "Admin Page Template Pattern" section as mandatory standard for future pages |

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-14 - Admin System Logs
**Branch:** `feature/admin-system-logs`

### Files Created
| File | Purpose |
|---|---|
| `model/SystemLog.java` | JPA entity for `system_logs` table |
| `repository/SystemLogRepository.java` | Data access with `findAllByOrderByTimestampDesc()` |
| `service/SystemLogService.java` | `logAction()` and `getAllLogs()` business logic |
| `dto/SystemLogResponse.java` | API response DTO record with `from()` factory |
| `controller/SystemLogController.java` | `GET /api/logs` REST endpoint (ADMIN-only) |
| `static/js/system-logs.js` | DataTables 2 AJAX fetch and table rendering |

### Files Modified
| File | Change |
|---|---|
| `config/SecurityConfig.java` | Added `/api/logs/**` ADMIN role matcher |
| `controller/AuthController.java` | Injected `SystemLogService`, logs login/logout (captures identity before session cleanup) |
| `controller/AdminController.java` | Injected `SystemLogService` + `UserRepository`, logs update/delete/re-enable actions with `LogContext` helper |
| `controller/AccountController.java` | Injected `SystemLogService` + `UserRepository`, logs self-service password/username/details changes |
| `static/logs.html` | Rebuilt with unified color scheme (`dashboard.css`), DataTables 2, `auth-guard.js`, loading spinner |
| `src/main/sql/AnihanSRMS.sql` | Added `system_logs` table with timestamp index |
| `test/controller/AdminControllerWebMvcTest.java` | Added `@MockitoBean` for `SystemLogService` and `UserRepository` |

### Database Migration Required
```sql
CREATE TABLE system_logs (
    log_id      INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id     INT NULL,
    username    VARCHAR(255) NOT NULL,
    role        VARCHAR(15) NOT NULL,
    action      VARCHAR(500) NOT NULL,
    ip_address  VARCHAR(45) NULL,
    timestamp   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_system_logs_timestamp (timestamp DESC)
);
```

### Tracked Actions
- Login / Logout
- Admin: Update user details, Reset password, Deactivate account, Permanently delete account, Re-enable account
- Self-service: Change own password, Change own username, Update own personal details

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-11 - Admin Username Edit & Button Hover Fixes
**Branch:** `feature/admin-password-delete-hover-fix`

### Files Modified
| File | Change |
|---|---|
| `css/dashboard.css` | Fixed hover selectors for `.btn-reenable`, `.btn-danger-surface`, `.btn-deactivate`, `.btn-permanent-delete` (added `.btn.` prefix for Bootstrap specificity) |
| `dto/AdminUpdateUserRequest.java` | Added optional `username` field with `@Pattern(^\S+$)` no-spaces validation |
| `service/AdminService.java` | Added duplicate-username check in `updateUser()` |
| `edit-user.html` | Made username field editable (removed `disabled`), added `pattern` + help text, updated contract note |
| `js/admin-edit-user.js` | Added `username` to payload, added client-side no-spaces validation |
| `test/service/AdminServiceTest.java` | Updated constructors for new `username` parameter |

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-11 - Re-enable, Password Toggle, Strong Validation & Timestamp
**Branch:** `feature/admin-password-delete-hover-fix`

### Files Modified
| File | Change |
|---|---|
| `model/User.java` | Added `passwordChangedAt` (LocalDateTime) field + getter/setter |
| `dto/AdminUserResponse.java` | Added `LocalDateTime passwordChangedAt` field + updated `from()` |
| `dto/UpdatePasswordRequest.java` | Added `@Pattern` regex for strong password enforcement |
| `service/AdminService.java` | Sets `passwordChangedAt` on admin password reset; added `reEnableUser()` method |
| `service/AccountService.java` | Sets `passwordChangedAt` on self-service password change |
| `controller/AdminController.java` | Added `PUT /api/admin/users/{id}/enable` endpoint |
| `static/js/auth-guard.js` | Added `isStrongPassword()` validation, `setupPasswordToggles()` function (eye icon SVG auto-injection), modal reset logic for toggle state |
| `static/js/admin-users.js` | Added `passwordChangedAt` display, re-enable button toggle logic, `setupReEnableHandler()` function |
| `static/admin.html` | Added "Password Last Changed" detail card, "Re-enable Account" button, updated password help text |
| `static/edit-user.html` | Updated self-service password help text for strong requirements |
| `static/registrar.html` | Added `minlength="8"` to all password inputs, added strong password help text |
| `static/trainer.html` | Added `minlength="8"` to all password inputs, added strong password help text |
| `static/css/dashboard.css` | Added `.btn-reenable` and `.password-toggle-btn` styles |
| `src/main/sql/AnihanSRMS.sql` | Added `password_changed_at DATETIME NULL` column |
| `test/controller/AdminControllerWebMvcTest.java` | Updated constructor with `null` for passwordChangedAt |

### Database Migration
```sql
ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL;
```

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-11 - Admin Password Reset, Delete Account & Button Hover Fix
**Branch:** `feature/admin-password-delete-hover-fix`

### Files Modified
| File | Change |
|---|---|
| `model/User.java` | Added `enabled` field (Boolean, default true) with JPA column mapping + getter/setter |
| `dto/AdminUpdateUserRequest.java` | Added optional `password` field with `@Size(min=8)` validation |
| `dto/UpdatePasswordRequest.java` | Added `@Size(min=8)` to `newPassword` and `confirmNewPassword` |
| `dto/AdminUserResponse.java` | Added `Boolean enabled` field and updated `from()` factory |
| `service/AdminService.java` | Injected `PasswordEncoder`, added password hashing in `updateUser()`, added `softDeleteUser()` and `hardDeleteUser()` with self-deletion prevention |
| `controller/AdminController.java` | Added `DELETE /api/admin/users/{id}` (soft) and `DELETE /api/admin/users/{id}/permanent` (hard) |
| `service/CustomUserDetailsService.java` | Uses Spring Security 7-arg `User` constructor to block disabled users via `enabled` flag |
| `static/admin.html` | Added "Delete User" button in details modal, added delete confirmation modal with soft/hard options, added Status column header, added `minlength="8"` to password fields |
| `static/edit-user.html` | Added "New Password" input field with `minlength="8"`, added `minlength="8"` to self-service password fields |
| `static/js/admin-users.js` | Rewrote: added delete flow, status badge rendering, self-delete prevention (hides button), delete confirmation modal handlers |
| `static/js/admin-edit-user.js` | Updated `buildPayload()` to include optional password, added client-side 8-char validation |
| `static/js/auth-guard.js` | Added 8-char minimum validation in `setupPasswordChange()` |
| `static/css/dashboard.css` | Fixed `.btn-surface:hover`/`:focus` to explicitly set green background; added `.btn-danger-surface`, `.status-badge`, `.delete-confirm-modal`, `.btn-deactivate`, `.btn-permanent-delete` styles |
| `src/main/sql/AnihanSRMS.sql` | Added `enabled TINYINT(1) NOT NULL DEFAULT 1` column to users table |
| `test/service/AdminServiceTest.java` | Added `PasswordEncoder` mock, updated constructor calls with null password, set `enabled=true` in test builder |
| `test/controller/AdminControllerWebMvcTest.java` | Updated `AdminUserResponse` constructor to include `enabled=true` |

### Database Migration Required
```sql
ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;
```

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-11 - Admin Dashboard Front-End Repair
**Branch:** `feature/fix-login-security`

### Files Modified
| File | Change |
|---|---|
| `static/admin.html` | Rebuilt the admin dashboard HTML to remove duplicate donor markup, restore a valid document structure, and match the current `admin-users.js` IDs |
| `static/edit-user.html` | Rebuilt the admin edit-user page to match `admin-edit-user.js` and the shared account modal shell |
| `memory-bank/activeContext.md` | Recorded the front-end regression cause and current browser retest task |
| `memory-bank/progress.md` | Added the admin shell and edit-flow rebuild work to the UI progress notes |
| `memory-bank/testing.md` | Recorded the post-repair verification steps for the admin front-end fix |

### Root Cause
- `admin.html` had regressed into a partially merged donor version with overlapping navbars, misplaced closing tags, inline scripts duplicated in the page body, and mismatched IDs compared to `admin-users.js`.
- `edit-user.html` had also drifted back to older markup that no longer matched the current `admin-edit-user.js` field names.
- That combination could leave the admin experience blank or broken immediately after login and during the follow-up edit flow.

### Verification
- `./gradlew build` -> BUILD SUCCESSFUL
- `git diff --check` -> no tracked whitespace errors after the HTML rebuild
- Structural spot-check confirmed single `head`, `body`, and `html` blocks in both repaired pages

## 2026-04-11 - Build Repair for Admin Controller Regression
**Branch:** `feature/fix-login-security`

### Files Modified
| File | Change |
|---|---|
| `controller/AdminController.java` | Replaced the regressed repository-backed controller with the service/DTO-based admin API expected by the current tests |
| `memory-bank/activeContext.md` | Updated active task and recorded the build-failure root cause |
| `memory-bank/progress.md` | Recorded the controller regression fix in the admin merge progress notes |
| `memory-bank/testing.md` | Added fresh automated verification results for the controller repair |

### Root Cause
- `AdminController` had drifted back to an older `UserRepository` implementation.
- The current `AdminControllerWebMvcTest` slice expects `AdminService` injection, validated `AdminUpdateUserRequest`, and sanitized `AdminUserResponse` DTO output.
- That mismatch caused the Spring test context to fail before the controller tests could run.

### Verification
- `./gradlew test` -> BUILD SUCCESSFUL
- `./gradlew build` -> BUILD SUCCESSFUL
- `git diff --check` -> no tracked whitespace errors after the controller repair

## 2026-04-11 - Conflict Cleanup and Commit-Safety Recheck
**Branch:** `feature/fix-login-security`

### Files Modified
| File | Change |
|---|---|
| `memory-bank/projectbrief.md` | Removed unresolved merge markers and rewrote current-status note |
| `memory-bank/activeContext.md` | Replaced stale donor-status notes with current repaired branch state |
| `memory-bank/progress.md` | Recorded commit-safety cleanup and fresh verification results |
| `config/SecurityConfig.java` | Restored clean root RBAC configuration for merged admin routes |
| `controller/AdminController.java` | Restored DTO/service-based admin API contract |
| `static/admin.html` | Removed partially merged donor markup and restored clean root dashboard shell |
| `static/edit-user.html` | Restored root edit-user flow and shared account modal integration |
| `static/student-records.html` | Restored root placeholder shell with proper admin session guard |
| `static/subjects.html` | Restored root placeholder shell with proper admin session guard |
| `static/logs.html` | Restored root placeholder shell with proper admin session guard |
| `src/main/sql/AnihanSRMS.sql` | Removed donor corruption and restored root-aligned schema ordering |

### Verification
- `./gradlew test` -> BUILD SUCCESSFUL
- `./gradlew build` -> BUILD SUCCESSFUL
- `git diff --check` -> no conflict markers or whitespace errors in tracked files
- `rg -n "^(<<<<<<<|=======|>>>>>>>)" -S .` -> no matches in tracked project files

## 2026-04-11 - Root/Admin Merge from `main-em`
**Branch:** `feature/fix-login-security`

### Files Created
| File | Purpose |
|---|---|
| `controller/AdminController.java` | Root admin API for listing, viewing, and updating users |
| `service/AdminService.java` | Admin-side user-management business logic |
| `dto/AdminUserResponse.java` | Sanitized admin API response DTO |
| `dto/AdminUpdateUserRequest.java` | Validated admin update request DTO |
| `static/edit-user.html` | Root admin edit-user page |
| `static/student-records.html` | Root admin student-records placeholder page |
| `static/subjects.html` | Root admin subjects placeholder page |
| `static/logs.html` | Root admin logs placeholder page |
| `static/js/admin-users.js` | Admin dashboard DataTables and detail modal logic |
| `static/js/admin-edit-user.js` | Edit-user page fetch/save logic and unsaved-change guard |
| `test/controller/AdminControllerWebMvcTest.java` | Admin controller security and validation coverage |
| `test/service/AdminServiceTest.java` | Admin service self-role-lock and sanitized-response coverage |

### Files Modified
| File | Change |
|---|---|
| `config/SecurityConfig.java` | Added admin-only route protection for merged HTML pages |
| `exception/GlobalExceptionHandler.java` | Added 404 handling for missing admin resources |
| `build.gradle.kts` | Added Spring Boot starter test dependency required by the new admin tests |
| `static/admin.html` | Rebuilt the admin dashboard shell around merged user-management UI |
| `static/css/dashboard.css` | Added shared admin shell, modal, table, placeholder-page, and responsive styles |
| `static/js/auth-guard.js` | Cleaned shared session/account modal logic for merged pages |
| `src/main/sql/AnihanSRMS.sql` | Reordered schema definitions and aligned users/student records keys with the root app |
| `memory-bank/activeContext.md` | Updated current phase/task for the root/admin merge |
| `memory-bank/progress.md` | Recorded merged admin module completion details |

### Merge Notes
- Kept the repo root as the source-of-truth application.
- Treated `main-em/` as a read-only donor/reference only.
- Preserved the newer root user schema: `lastname`, `firstname`, `middlename`, `birthdate`.
- Used DTOs for `/api/admin/users` responses so password hashes are never exposed.
- Preserved self-role-lock protection so an admin cannot demote their own role.

### Verification
- `./gradlew test` -> BUILD SUCCESSFUL
- `./gradlew build` -> BUILD SUCCESSFUL
- `rg -n "^(<<<<<<<|=======|>>>>>>>)" src memory-bank build.gradle.kts` -> no matches

## 2026-04-06 - AGILE-100: G2.1 Edit Personal Details
**Branch:** `feature/fix-login-security`

### Files Created
| File | Purpose |
|---|---|
| `dto/UpdatePersonalDetailsRequest.java` | Personal details update DTO (record) |
| `repository/SubjectRepository.java` | JPA repo for subjects dropdown |
| `repository/SectionRepository.java` | JPA repo for sections dropdown |
| `controller/LookupController.java` | GET /api/lookup/subjects + /sections |

### Files Modified
| File | Change |
|---|---|
| `service/CustomUserDetailsService.java` | Enforced strict case match on username login; fallback to case-insensitive email |
| `model/User.java` | Added fullName, age, dateOfBirth, subjectCode, sectionCode fields |
| `service/AccountService.java` | Added updatePersonalDetails() with role-based field enforcement |
| `controller/AccountController.java` | Added PUT /api/account/details |
| `controller/AuthController.java` | Injected UserRepository, expanded /api/auth/me to include personal details |
| `static/css/dashboard.css` | Added nav-tab styles, form-select, trainer-only-fields, logout notification |
| `static/js/auth-guard.js` | Added personal details form, dropdown loading, trainer field visibility, logout notification redirect |
| `static/admin.html` | Tabbed Edit Account modal (Personal Details + Account Settings) |
| `static/registrar.html` | Same tabbed modal |
| `static/trainer.html` | Same tabbed modal (trainer sees Subject/Section dropdowns) |
| `static/index.html` | Added logout notification banner + JS trigger |

### Database Changes
| Change | Command |
|---|---|
| Added columns to users | `ALTER TABLE users ADD full_name, age, date_of_birth, subject_code (FK), section_code (FK)` |
| Seeded courses | `INSERT INTO courses VALUES ('CARS', 'Culinary Arts and Restaurant Services')` |
| Seeded batches | `INSERT INTO batches VALUES ('B2026A', 2026), ('B2026B', 2026)` |
| Seeded qualifications | NC II - Cookery, NC II - Food and Beverage Services |
| Seeded subjects | COOK101, COOK102, FBS101 |
| Seeded sections | SEC-A, SEC-B, SEC-C |

### Verification
- `./gradlew clean build -x test` -> BUILD SUCCESSFUL
- `./gradlew bootRun` -> Started in ~10s
- PUT `/api/account/details` (trainer) -> 200 + all fields saved
- GET `/api/lookup/subjects` -> 3 subjects returned
- GET `/api/lookup/sections` -> 3 sections returned
- GET `/api/auth/me` -> includes personal details
- Browser: tabbed modal works, dropdowns populated
- Browser: logout notification banner slides in

## 2026-04-05 - AGILE-142: G1.R Fix Login
**Branch:** `feature/fix-login-security`

### Files Created
| File | Purpose |
|---|---|
| `controller/AccountController.java` | PUT /api/account/profile + /api/account/password |
| `service/AccountService.java` | Username/password change business logic |
| `dto/UpdateProfileRequest.java` | Username change DTO with validation |
| `dto/UpdatePasswordRequest.java` | Password change DTO with validation |
| `static/js/auth-guard.js` | Shared session guard + account UI logic |
| `static/css/dashboard.css` | Account dropdown + modal styles |

### Files Modified
| File | Change |
|---|---|
| `config/SecurityConfig.java` | Rewritten: role-specific HTML matchers, dual-mode entry/denied handlers, cache-control |
| `model/User.java` | Added `@Column(unique = true)` on username |
| `exception/GlobalExceptionHandler.java` | Added IllegalArgumentException handler |
| `static/admin.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/registrar.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/trainer.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/index.html` | Added session check to redirect authenticated users |

### Files Deleted
| File | Reason |
|---|---|
| `config/DataSeeder.java` | Removed - accounts managed directly in database per user decision |

### Database Changes
| Change | Command |
|---|---|
| Added unique index on `users.username` | `ALTER TABLE AnihanSRMS.users ADD UNIQUE INDEX idx_username (username);` |

### Verification
- `./gradlew clean build -x test` -> BUILD SUCCESSFUL
- `./gradlew bootRun` -> Started successfully (no errors, no DataSeeder output)
- Unauthenticated `/admin.html` -> 302 redirect to `/index.html`
- API `/api/auth/me` without session -> 401 JSON
- Admin login -> 200 + `admin.html`
- Admin accessing `/registrar.html` -> 302 redirect (wrong role blocked)
- Registrar login -> 200 + `registrar.html`
- Username change -> 200 + session updated
- Password change -> 200 + session invalidated (401 on next request)
- Login with new password -> 200
- Logout -> subsequent access returns 302
- Wrong password on profile update -> 400 "Current password is incorrect"

## 2026-03-24 - Phases 1-4: Backend Auth Setup
**Branch:** `feature/backend-auth-setup`

### Files Created
| File | Purpose |
|---|---|
| `model/User.java` | Users table entity |
| `repository/UserRepository.java` | JPA repo with username/email queries |
| `controller/AuthController.java` | Login/logout/me endpoints |
| `src/main/resources/static/admin.html` | Empty admin template with header/footer |
| `src/main/resources/static/registrar.html` | Empty registrar template with header/footer |
| `src/main/resources/static/trainer.html` | Empty trainer template with header/footer |

## 2026-04-07 — Database Schema Development
| File | Action | Description |
|------|--------|-------------|
| `AnihanSRMS.sql` | Created | Defined complete database schema with 15 tables for student records, enrollment, and results |

## 2026-04-10 — Admin Dashboard UI & Logic (Current)
**Branch:** `feature/admin-dashboard-ui`

### Files Created/Modifed
| File | Change |
|---|---|
| `application.properties` | Added ddl-auto=none, MySQL dialect, session config |
| `index.html` | Added error alert div + login fetch JS |
| `src/main/sql/AnihanSRMS.sql` | Reordered tables to respect foreign key creation order |

### Files Deleted
| File | Reason |
|---|---|
| `src/main/java/controller/` | Empty folder outside Spring Boot package |
| `src/main/java/model/` | Empty folder outside Spring Boot package |
| `src/main/java/repository/` | Empty folder outside Spring Boot package |
| `src/main/java/service/` | Empty folder outside Spring Boot package |
| `docker-compose.yml` | Removed per user request, using existing `mysql-server` container |

### Verification
- Re-injected corrected SQL script into `mysql-server`
- Gradle `clean build -x test` -> BUILD SUCCESSFUL
- `./gradlew bootRun` started successfully
- `Invoke-RestMethod` to `/api/auth/login` returned 200 OK + `ROLE_ADMIN`

## 2026-03-24 - Troubleshooting IDE Syntax Errors
**Branch:** `feature/fix-src-errors`

### Verification
- Tested DataTables rendering users.
- Confirmed Modal successfully fetches via Ajax.
- Confirmed `edit-user.html` correctly displays "Unsaved changes" warnings visually.
- Verified backend rejects self-demoting role assignments.
- Checked user IDE error: `String cannot be resolved to a type` inside `LoginRequest.java`.
- Ran `./gradlew clean compileJava` locally. Confirmed `BUILD SUCCESSFUL`.
- Diagnosed issue as a false-positive caused by IDE Java Language Server (JDTLS) losing connection to the Java 25 JDK.
- Updated `activeContext.md` and `progress.md`.
- Updated `build.gradle.kts` to use Java 25.
- Updated `src/main/resources/static/index.html` to add error alert div + login fetch JS.
- Updated `src/main/resources/static/admin.html` to add header/footer + empty main.
- Updated `src/main/resources/static/registrar.html` to add header/footer + empty main.
- Updated `src/main/resources/static/trainer.html` to add header/footer + empty main.

### 2026-04-10
- Refactored DB schema to drop `full_name` in favor of `lastname`, `firstname`, `middlename`.
- Renamed `date_of_birth` to `birthdate`.
- Removed inline `subject_code` and `section_code` from users table and corresponding Java entities.
- Updated `AnihanSRMS.sql` to remove Git conflict marks and use clean `CREATE TABLE`.
- Updated `User.java`, `UpdatePersonalDetailsRequest.java` for the entity structural changes.
- Updated `AccountService.java`, `AuthController.java`, `AccountController.java` for new personal detail endpoints.
- Updated `auth-guard.js` and admin, trainer, registrar HTML pages to render distinct inputs for names.
- Re-seeded 3 standard test accounts into the new `users` table schema.
