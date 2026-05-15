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

**Latest full-suite result:** `./gradlew test` → BUILD SUCCESSFUL — **135 tests, 0 failures, 0 errors** (May 15, 2026, after Section Student Management + Bulk Enrollment session).

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
