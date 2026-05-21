# Progress - Anihan SRMS

## Recent Sessions (detail)

### Bugfix Audit Remediation (Completed — May 21, 2026)
- **Task:** Execute all 10 items from `docs/superpowers/plans/2026-05-21-bugfix-audit-remediation.md`.
- **Result:** `./gradlew test` → **176 tests, 0 failures, 0 errors** (was 166; 10 new tests).
- **Key fixes:** FK cascade on student delete (H1); Student ID locked readonly (H2); duplicate-name portal flow (M2-M4); grade range validation (M5-M6); entity `@Column` metadata sync (L1); middleName nullable (L2); `@EnableMethodSecurity` (L3); username-derived default email (L5); CSRF docs (L4); StorageService path-traversal guard (L6).
- **Branch:** `main`.

### Database Schema Sync & Grades Restructure (Completed — May 21, 2026)
- **Task:** Resolve all column type/width and constraint mismatches between the live MySQL database and the canonical `schema.sql`, apply the grades restructure migration `2026-05-19-grades-restructure.sql`, seed 5 test student records, and verify that the Spring Boot test suite compiles and runs cleanly against the refreshed schema.
- **Action:**
  - Backed up the live database to `src/main/sql/backup.sql`.
  - Dropped and recreated the `AnihanSRMS` database, and imported the updated `schema.sql`.
  - Verified that all tables and columns match the latest schema specifications, resolving all 7 structural table column type/width mismatches.
  - Ran the query checks in `2026-05-19-grades-restructure.sql` to verify the class-scoped grading columns, indexes, and constraints.
  - Ran `./gradlew test` and successfully passed the full suite of 166 backend tests.
- **Active Branch:** `main` (stayed on `main` per user's instruction).

### Save Grades Error + Button Styling Fix — AGILE-126 / AGILE-127 (Completed — May 19, 2026)
- **Root cause 1:** `Grade.java` `@JoinColumn(name = "student_id")` mapped to `StudentRecord.recordId` (integer PK) instead of `StudentRecord.studentId` (varchar business key). Hibernate wrote integer record_id into varchar student_id FK → `SQLIntegrityConstraintViolationException`.
- **Root cause 2:** `TrainerGradeController` catch blocks returned `ResponseEntity.badRequest().build()` with no JSON body → JS `xhr.responseJSON.message` failed silently.
- **Root cause 3:** Button CSS classes `btn-surface-success` and `btn-save` did not exist in `dashboard.css` (only scoped to `.edit-account-modal`).
- **Root cause 4:** JS `collectGradeUpdates()` sent all 5 student rows even when empty.
- **Fix:** Added `referencedColumnName = "student_id"` to `Grade.java` JoinColumn. Controller now returns `Map.of("message", ...)` in all error paths with SLF4J logging. HTML buttons switched to `btn-surface` / `btn-surface-secondary`. JS filters empty rows and shows warning.
- Browser smoke test: Save → Lock → Unlock full flow verified. DB row: `student_id='SR20260007', midterm=2.00, finals=3.00, final_grade=2.60`.
- Branch: `feature/trainer-grade-input`. Open: commit + PR to main.

### Grade Input Modal Fix — AGILE-126 / AGILE-127 (Completed — May 19, 2026)
- **Root cause 1:** `TrainerGradeService.resolveCurrentTrainerId()` hardcoded to return `100` instead of DB lookup via `UserRepository`. Trainer user_id is `12`, so ownership checks always failed → HTTP 400 → "Failed to load grades."
- **Root cause 2:** `getGradesForClass()` only queried the empty `grades` table, never consulted `class_enrollments` for enrolled students.
- **Root cause 3:** `saveGrades()` expected pre-existing grade rows (`orElseThrow`), preventing grade creation for new students.
- **Fix:** Injected `ClassEnrollmentRepository` + `UserRepository` into `TrainerGradeService`. `resolveCurrentTrainerId()` now does `UserRepository.findByUsername()` (same as `TrainerService`). `getGradesForClass()` now merges enrollments with existing grades. `saveGrades()` now does upsert.
- `TrainerGradeServiceTest`: updated to 12 tests with new mocks + 2 new test cases.
- Full suite: **166 tests, 0 failures, 0 errors**.
- Browser smoke test: trainer logged in, Pastry Arts modal shows 5 enrolled students with editable grade inputs.
- Branch: `feature/trainer-grade-input`. Open: commit + PR to main.

### Trainer Read-Only Views — AGILE-123 / AGILE-124 (Completed — May 18, 2026)
- `SchoolClassRepository`: added `findByTrainerUserId(Integer)` and `findByTrainerUserIdAndSubjectSubjectCode(Integer, String)`.
- 4 new DTOs in `dto/trainer/`: `TrainerSubjectResponse`, `TrainerSubjectStudentResponse`, `TrainerClassResponse`, `TrainerClassStudentResponse`.
- `TrainerService` created: `resolveCurrentTrainerId()` (reads SecurityContext → UserRepository), `getMyAssignedSubjects()` (groups classes by subjectCode, sums enrolled counts, collects distinct section/course names), `getStudentsForSubject()` (throws if trainer not assigned), `getMyClasses()`, `getStudentsForClass()` (ownership guard).
- `TrainerController` created: 4 GET endpoints under `/api/trainer/` — no `system_logs` writes (read-only).
- `TrainerServiceTest`: 12 Mockito tests. `TrainerControllerWebMvcTest`: 9 WebMvc tests (RBAC: 401 anon, 403 non-trainer, 400 on service throws).
- `SecurityConfig.java`: trainer HTML matcher extended to include `/trainer-subjects.html` and `/trainer-classes.html`.
- `trainer.html`: upgraded to full dashboard pattern with 3-link navbar, welcome hero, quick-link cards, jQuery import.
- `trainer-subjects.html` + `trainer-subjects.js`: subjects DataTable, click-to-expand student roster panel.
- `trainer-classes.html` + `trainer-classes.js`: classes DataTable, click-to-expand student roster panel.
- Full suite: **156 tests, 0 failures, 0 errors**.
- Branch: `feature/trainer-view-subjects-classes`. Open: browser smoke test; PR to main (user approval required).

### Section Student Management + Bulk Class Enrollment — AGILE-164 / AGILE-165 (Completed — May 15, 2026)
- 6 new DTOs in `dto/registrar/`: `UpdateSectionRequest`, `SectionStudentResponse`, `EligibleSectionStudentResponse`, `AssignStudentsToSectionRequest`, `SectionAssignmentResultResponse`, `BulkEnrollSectionResponse`.
- `StudentRecordRepository`: 5 new derived finders for null-section + status + batch/course filter combinations.
- `ClassEnrollmentRepository`: `deleteByStudentAndSectionCode` JPQL bulk-delete with `@Modifying @Transactional`.
- `ClassManagementService`: 6 new methods — section rename, roster listing, eligible-student query (4 repo variants), assign (Submitted→Active promotion), remove (cascade class enrollments + revert to Submitted), bulk-enroll whole section into class.
- `ClassManagementController`: 6 new endpoints under `/api/registrar/` — all write `system_logs`.
- `ClassManagementSectionServiceTest`: 13 Mockito tests. `ClassManagementSectionControllerWebMvcTest`: 7 WebMvc tests.
- Full suite: **135 tests, 0 failures, 0 errors**.
- `sections.html`: `#editSectionModal` + `#manageSectionModal` (tabbed); cache-buster `?v=3`.
- `registrar-sections.js`: full rewrite — 3-button Actions column; Edit, Manage Students (current roster + assign eligible), Delete flows.
- `classes.html`: Bulk Enrollment block (`#enrollWholeSectionBtn` + `#enrollSectionAlert`) in `#enrollStudentModal`; cache-buster `?v=3`.
- `registrar-classes.js`: `#enrollWholeSectionBtn` handler in `setupEnrollment()`; alert cleared on modal re-open.
- Branch: `feature/section-class-enrollment`. Open: browser smoke test; PR to main (user approval required).

### Navbar Sync on student-records.html (Completed — May 10, 2026)
- `student-records.html` registrar edit page navbar updated from 2-link (Home, Subjects) to canonical 4-link (Home, Subjects, Classes, Sections) to match `registrar.html`.
- Pure HTML change — two `<li class="nav-item">` blocks appended; no JS, CSS, or backend touched.
- No `active` class applied since the edit page is a Home subpage, not a top-level nav target.
- Branch: `feature/edit-class-trainer` (no rebranch — trivial UI sync).
- Open: manual browser smoke (4 links navigate; mobile collapse still works).

### Edit Class Trainer — AGILE-93 / AGILE-95 (Completed — May 10, 2026)
- `UpdateClassTrainerRequest` DTO created — nullable `Integer trainerId`; mirrors `AssignTrainerRequest`.
- `ClassManagementService.updateClassTrainer()` added — validates trainer role + enabled; returns `ClassResponse` with live enrolled count.
- `ClassManagementController`: `PUT /api/registrar/classes/{classId}/trainer` added. Writes `system_logs` assign/unassign messages.
- `classes.html`: `#editClassModal` added (read-only Section/Subject/Semester + trainer select + inline alert). Cache-buster `?v=2`.
- `registrar-classes.js`: Actions column updated to emit `Edit Trainer` + `Manage Students` buttons; `setupEditClass()` + `openEditClassModal()` added; `loadTrainersDropdown()` returns jQuery deferred for `.done()` chaining.
- Tests: `ClassManagementServiceTest` (6) + `ClassManagementControllerWebMvcTest` (4). Full suite: **115 tests, 0 failures**.
- Branch: `feature/edit-class-trainer`. Open: browser smoke test; Jira transitions for AGILE-93/95 (user handles).

### Subjects CRUD — Create / Edit / Delete (Completed — May 10, 2026)
- `QualificationRepository`, `QualificationResponse`, `CreateSubjectRequest`, `UpdateSubjectRequest` created.
- `SchoolClassRepository.existsBySubjectSubjectCode()` and `SubjectRepository.countGradesBySubjectCode()` added for FK pre-checks before delete.
- `ClassManagementService`: `getAllQualifications()`, `createSubject()`, `updateSubject()`, `deleteSubject()` implemented. Delete blocked if classes or grades reference the subject.
- `ClassManagementController`: `GET /qualifications`, `POST /subjects`, `PUT /subjects/{code}`, `DELETE /subjects/{code}` added. All state-changing calls write `system_logs`.
- `subjects.html`: "Create Subject" button + three modals (Create, Edit, Delete strict-confirm). JS cache-buster `?v=2`.
- `registrar-subjects.js`: full rewrite — Actions column has Edit + Assign Trainer + Delete; `setupCreateSubject`, `setupEditSubject`, `setupDeleteSubject`, `loadQualificationsDropdown` added.
- Tests: `ClassManagementSubjectServiceTest` (9) + `ClassManagementSubjectControllerWebMvcTest` (6). Full suite: **105 tests, 0 failures**.
- Branch: `feature/subjects-crud`. Open: browser smoke test, Jira transitions for AGILE-89/90/91.

### Student-Details Wizard Trim (Completed — May 9, 2026)
- Baptismal Certificate upload made optional (Baptism Date + Place still required when Baptized is checked).
- Educational Background table trimmed from 6 to 4 columns: removed `Grade/Year` and `Semester`; renamed `Year Ended` → `School Year`. JS payload/populateForm updated to match.
- "School Years at Anihan" section removed from wizard entirely. `renderSyRow`/`addSyRowData` functions deleted; all DOM references (`syTable`, `syTableBody`, `addSyRow`) gone.
- No backend/DTO/DB changes. `EducationItemDto.gradeYear`/`.semester` and DB columns retained; new submissions write `NULL`. Registrar manages school years on edit form independently.
- JS cache-buster bumped `?v=4` → `?v=5`. `./gradlew test` → **90 tests, 0 failures, 0 errors**.
- Branch: `fix/student-details-trim`.

### DB Sync + Error-Handler Hardening + Section FK Pre-Check (Completed — May 9, 2026)
- Audit revealed: live DB missing the May 9 migration (17/19 tables), `student_records.middle_name` still NOT NULL, `GlobalExceptionHandler` leaking SQL/exception internals, `deleteSection` had no FK pre-check, `getCurrentSemester` did in-memory max.
- Re-applied `2026-05-09-classes-and-trainers.sql` — `classes` + `class_enrollments` created, `subjects.trainer_id` added, 2 qualifications + 6 subjects seeded.
- New migration `2026-05-09-relax-middle-name.sql` applied — `student_records.middle_name` now nullable. `schema.sql` updated to match.
- `GlobalExceptionHandler`: SLF4J logger added; new `DataIntegrityViolationException` handler (409); generic 500 returns sanitized `"An unexpected error occurred."`; full stack logged server-side.
- `SchoolClassRepository.existsBySectionSectionCode()` and `BatchRepository.findTopByOrderByBatchYearDesc()` added.
- `ClassManagementService.deleteSection()` pre-checks for class references → 400 with actionable message.
- `ClassManagementService.getCurrentSemester()` now a single SQL query.
- `./gradlew test` → **90 tests, 0 failures, 0 errors**. Live API smoke-tested: `/api/registrar/subjects` 200 (6 rows), `/api/registrar/classes` 200 `[]`, `/api/registrar/classes/current-semester` 200 `{"semester":"2026"}`.
- Branch: `fix/db-sync-and-bugs`. Open: ClassManagement test coverage, N+1 in eligible-students/getClasses, move inner DTOs out.

### Registrar Subjects / Classes / Sections + Class Enrollment (Completed — May 9, 2026)
- Migration `2026-05-09-classes-and-trainers.sql`: `subjects.trainer_id` + FK, `classes` + `class_enrollments` tables, 2 qualifications + 6 subjects seeded. Applied to live MySQL.
- Entities `SchoolClass`, `ClassEnrollment`; `Subject` extended with `@ManyToOne User trainer`.
- Repos `SchoolClassRepository`, `ClassEnrollmentRepository`; `SectionRepository.findByBatchBatchYear`; `UserRepository.findByRoleAndEnabledTrue`.
- DTOs in `dto/registrar/`: SubjectResponse, AssignTrainerRequest, ClassResponse, CreateClassRequest, SectionResponse, CreateSectionRequest, TrainerResponse, EnrollStudentRequest.
- `ClassManagementService` + `ClassManagementController` (separate from `RegistrarController`); every state-changing call writes a `system_logs` row.
- `SecurityConfig` registrar matcher extended with `/classes.html` + `/sections.html`.
- Frontend: rebuilt `subjects.html`, new `classes.html` + `sections.html`; registrar navbar 2-link → 4-link.
- `schema.sql` refreshed (header → 2026-05-09; tables 17 → 19).
- `./gradlew test` + `bootRun` → green; live MySQL verified.
- Branch: `feature/class-assignment`. Open: E2E browser smoke + unit tests for the new service/controller.

### Strict Type-to-Confirm Delete Modals (May 7, 2026)
- Registrar (`#deleteRecordConfirmModal`) + Admin permanent-delete (`#permanentDeleteConfirmModal`): user must type `delete` (case-insensitive) before the destructive button enables. Replaces all `window.confirm()`/`window.alert()` in those flows. Soft-delete unchanged.
- Branch: `feature/registrar-fix`.

### Emoji Cleanup (May 7, 2026)
- Removed visible emoji glyphs from `registrar.html`, `trainer.html`, `index.html`, `student-portal.html`. `student-details.js` `✓` upload-status prefix replaced with literal `Uploaded:` (validator updated). DataTables vendored library left as-is.
- Branch: `feature/registrar-fix`.

### Bugs & Registrar Features (May 7, 2026)
- "Not Available" replaces literal `null` in registrar table + modal.
- ID Photo no longer required (asterisk + validator removed).
- Father/Mother/Guardian sections on registrar view + edit (DTO/service/HTML/JS all updated).
- `RegistrarService.deleteRecord()` deletes child rows in FK order + physical uploads; `DELETE /api/registrar/student-records/{id}` exposed.
- Auto-assign current-year batch on submit via `BatchRepository.findFirstByBatchYear`.
- File uploads deferred until after JSON submit succeeds (pending File state in JS).
- Branch: `feature/registrar-fix`.

### Registrar Enhancements — Status Filter + OJT/TESDA/SchoolYears (May 6, 2026)
- Status `<select>` (All/Enrolling/Submitted/Active/Graduated) on registrar home; `getAllRecords` 4-arg overload.
- OJT (upsert), TESDA (delete-all-flush-insert), SchoolYears (delete-all-flush-insert with reassigned rowIndex) added to `student-records.html` edit form.
- `RegistrarService.updateRecord()` is `@Transactional`.
- Tests: `RegistrarBulkLoadTest` adds 3 mocks + new `statusFilterRestrictsResultsByStudentStatus`; WebMvc test stubs updated to 4-arg.
- Branch: `feature/registrar-fixes`.

### Student Portal Enrollment Flow Fix (May 5, 2026)
- **RC-1:** `startOrResume()` creates name+status only (minimal record for upload FK).
- **RC-2:** Removed `saveDraft()` — data stays in browser until final submit.
- **RC-3:** New `submitEnrollment()` persists everything atomically in one `@Transactional` block.
- **RC-4:** OJT/TESDA removed from student-facing flow (entities/repos kept for registrar).
- **RC-5:** `AgeCalculator` returns `Integer null` instead of `int 0`.
- New `StudentDetailsServiceTest` (7 tests). Branch: `fix/student-portal-flow`.

### Schema Drift Remediation + DataSeeder Removal (May 5, 2026)
- `2026-05-05-fix-schema-drift.sql` applied: `civil_status` added; 27 columns relaxed to NULL across `student_records`/`parents`/`other_guardians`; `parents.est_income` default dropped.
- `DataSeeder.java` deleted — application data now comes from DB only.
- 82/82 tests pass (was 81/82 with `contextLoads` failing).

## Roadmap

- [/] Student enrollment portal — welcome + details wizard done; awaiting product decisions on Bug 4 (email)
- [ ] Trainer `updateGrade()` flow (next major workstream)
- [ ] BLOB document encoding via `encodeStudentDocsPerBatch()`
- [ ] Unit tests for `ClassManagementService`/`Controller`, `StudentDetailsController`, `StudentPortalController`, `StorageService`
- [ ] User Acceptance Testing tools for Time and Motion Study
- [ ] KPI Evaluation Dashboards (ISO/IEC 25010)

## Non-Functional Targets
- Document upload (<5MB): < 3s
- Student record retrieval: < 5s
- Concurrent users: 50+
- Server RAM at runtime: < 6GB

## Completed Foundations (one-line summary)

- **2026-05-04 — Student status dropdown + badge colors:** Active=green, Enrolling/Submitted=grey, Graduated=blue. "Submitted" is portal-only.
- **2026-05-03 — Student portal mandatory field validation:** Civil Status, ID Photo, conditional baptism fields, Father/Mother core fields. `STEP_CUSTOM_VALIDATORS` pattern added.
- **2026-05-02 — Database sync migration:** Dropped 4 legacy tables; PK change `student_records.student_id → record_id`; nullability + type fixes; 17 canonical tables.
- **2026-05-02 — Search bar + batch year filter + dummy seed data:** DataTables 2 selector fix, `?fromYear=&toYear=` server-side, schema.sql self-contained with seed data.
- **2026-05-01 — Registrar bulk load tests + H2 isolation + server-side search:** `?q=` query, 200-record perf tests, `StudentRecordH2LoadTest` (real JPA, isolated H2).
- **2026-05-01 — Registrar edit student record + unsaved-changes guard:** Full edit form with FK resolution, `<input list>` dropdowns, dirty-tracking + `beforeunload`.
- **2026-05-01 — Registrar home: student records table + detail modal:** `RegistrarController`, `RegistrarService`, summary + details DTOs, 9-col DataTable.
- **2026-05-01 — Live age recalculation in Edit Account modal.**
- **2026-05-01 — Registrar navbar standardization; `subjects.html` rebranded to registrar.**
- **2026-04-29 — Student details enrollment wizard:** 5 new entities, 7 new repos, 9 DTOs, `StorageService`, `StudentDetailsService` + Controller, full 4-step Bootstrap wizard. Live DB migrated.
- **2026-04-26 — Student portal welcome page:** `StudentPortalController`, public duplicate-name check, names passed via URL params (no premature DB insert).
- **2026-04-19 — Age auto-calculation from birthdate:** `AgeCalculator` utility; age removed from all input DTOs/forms; silent recalc on view (not on bulk table load).
- **2026-04-18 — System logs export (CSV/XLSX/DOCX) + date filtering:** Server-side generation; filter precedence (custom range > rangeDays > default 7d); preset pills UI.
- **2026-04-18 — Admin bulk load tests:** 100-user fetch in <5s service / <5s HTTP.
- **2026-04-17 — Admin navbar cleanup:** Home | Logs only; `student-records.html` + `subjects.html` preserved with stale internal navbars (TD-2).
- **2026-04-17 — Unit test coverage expansion:** Account + SystemLog service/controller suites added.
- **2026-04-16 — Navbar logo UI standardization:** `.brand-title` removed, logo sized to 85px with negative margin.
- **2026-04-14 — Admin system logs:** `system_logs` table, `SystemLog` entity/repo/service/controller/DTO; integrated in Auth/Admin/Account controllers; `logs.html` rebuilt.
- **2026-04-14 — Account-icon dropdown standardized across admin pages.**
- **2026-04-11 — Admin password reset, soft/hard delete, hover fixes, password toggle, strong validation, `passwordChangedAt`, username edit.**
- **2026-04-11 — Root/admin merge from `main-em`:** Sanitized DTO admin API; donor pages reshelled; commit-safety cleanup.
- **2026-04-06 — AGILE-100 G2.1 Edit Personal Details:** Tabbed Edit Account modal; trainer subject/section dropdowns; `LookupController`.
- **2026-04-05 — AGILE-142 Login security fix:** Role-specific HTML matchers, dual-mode entry/denied handlers, account modal, unique-username index, DataSeeder removed.
- **2026-04-07 — Database schema established (`AnihanSRMS.sql`).**
- **2026-03-24 — Backend auth setup:** User entity/repo, AuthController login/logout/me, dashboard templates, Gradle 9.4.1, `ddl-auto=none`.
- **2026-03-21 — Login page design and login page front-end UI built.**
