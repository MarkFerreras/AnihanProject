# Active Context - Anihan SRMS

## Current Phase
**Subjects CRUD (AGILE-89, AGILE-90, AGILE-91) — Create / Edit / Delete on the Subjects page**

## Active Branch
`feature/subjects-crud`

## Latest Session (May 10, 2026 — Subjects CRUD: Create / Edit / Delete)

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
