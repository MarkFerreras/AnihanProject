# Change Log - Anihan SRMS

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

