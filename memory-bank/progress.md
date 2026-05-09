# Progress - Anihan SRMS

## Recent Sessions (detail)

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
