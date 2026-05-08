# Active Context - Anihan SRMS

## Current Phase
**Registrar Subjects, Classes & Sections Management (2026-05-09)**

## Active Branch
`feature/class-assignment`

## Latest Session (May 9, 2026 — Registrar Subjects/Classes/Sections + Class Enrollment)

### Items Completed
1. **DB migration `2026-05-09-classes-and-trainers.sql`** — Idempotent migration that adds `subjects.trainer_id` (FK to `users.user_id`, ON DELETE SET NULL), creates the `classes` and `class_enrollments` tables, and seeds 2 qualifications (Cookery NC II, Bread and Pastry Production NC II) + 6 subjects (4 cookery + 2 bread/pastry). Applied to live MySQL via `docker exec`.

2. **JPA entities** — `SchoolClass` (entity name avoids conflict with `java.lang.Class`) for the `classes` table, and `ClassEnrollment` for the `class_enrollments` table. `Subject.java` extended with optional `@ManyToOne User trainer` mapped to `trainer_id`.

3. **Repositories** — `SchoolClassRepository` (with `findBySemester`, `existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester`), `ClassEnrollmentRepository` (with `findBySchoolClassClassId`, `existsBySchoolClassClassIdAndStudentStudentId`, `countBySchoolClassClassId`). `SectionRepository` extended with `findByBatchBatchYear`. `UserRepository` extended with `findByRoleAndEnabledTrue`.

4. **DTOs** — In `dto/registrar/`: `SubjectResponse`, `AssignTrainerRequest`, `ClassResponse` (includes `enrolledCount`), `CreateClassRequest`, `SectionResponse`, `CreateSectionRequest`, `TrainerResponse`, `EnrollStudentRequest`.

5. **Service `ClassManagementService`** — Wraps Subjects (`getAllSubjects`, `assignTrainer`), Trainers lookup (`getActiveTrainers`), Classes (`getCurrentSemester`, `getClasses`, `createClass`), Class Enrollment (`getClassEnrollments`, `enrollStudent`, `unenrollStudent`, `getEligibleStudents`), and Sections (`getSections`, `createSection`, `deleteSection`). Validates trainer role + enabled flag, enforces `(section_code, subject_code, semester)` uniqueness on classes, eligible-student filter restricts to Active/Submitted students in the same section who aren't already enrolled.

6. **Controller `ClassManagementController`** — Separate controller (instead of modifying `RegistrarController`) under `/api/registrar/`. Endpoints: `GET /subjects`, `PUT /subjects/{code}/trainer`, `GET /trainers`, `GET /classes`, `GET /classes/current-semester`, `POST /classes`, `GET /classes/{id}/enrollments`, `GET /classes/{id}/eligible-students`, `POST /classes/enroll`, `DELETE /enrollments/{id}`, `GET /sections`, `POST /sections`, `DELETE /sections/{code}`. Every state-changing call writes a `system_logs` row with the actor + IP.

7. **SecurityConfig** — Registrar HTML matcher extended to include `/classes.html` and `/sections.html`.

8. **Frontend pages**:
   - `subjects.html` — DataTable of all subjects with "Assigned Trainer" column + "Assign Trainer" modal. 4-link registrar navbar.
   - `classes.html` — DataTable of classes filtered by current semester (toggle to show all). "Create Class" modal (section/subject/trainer/semester). "Manage Students" modal opens per-class enrollment screen with currently-enrolled list and eligible-student dropdown.
   - `sections.html` — DataTable of sections filtered by current semester. "Create Section" modal (code/name/batch/course). "Delete Section" confirm modal.
   - `registrar.html` — navbar updated from 2-link to 4-link (Home / Subjects / Classes / Sections).

9. **Frontend JS** — `registrar-subjects.js`, `registrar-classes.js` (drives both create-class and enrollment flows + auto-selects subject's default trainer when subject is picked), `registrar-sections.js`.

10. **schema.sql refresh** — Added `subjects.trainer_id` column + FK, added `classes` and `class_enrollments` CREATE TABLE, added qualifications + subjects seed data so fresh installs match the migrated state. Header bumped to 2026-05-09 with table count 17 → 19.

### Verified
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → BUILD SUCCESSFUL (full suite)
- `./gradlew bootRun` → Tomcat started on port 8080, Spring context loaded with 14 JPA repositories, no schema validation errors after live-DB migration
- Live MySQL: `SHOW TABLES` reports `classes`, `class_enrollments`. `subjects.trainer_id` column present. Seeded 2 qualifications + 6 subjects.

### Open Items
- E2E browser smoke test of the three new pages and the create/enroll flows (deferred to user)
- No unit tests written for `ClassManagementService` / `ClassManagementController` yet — follow-up ticket

## Previous Session (May 7, 2026 — Strict Type-to-Confirm Delete Modals)

### Items Completed
1. **Registrar — Strict delete confirmation modal** — Replaced the `window.confirm()` alert in the student record delete flow with a Bootstrap modal that requires the user to type the literal word `delete` (case-insensitive, trimmed) before the **Permanently Delete** button becomes enabled. The modal opens in place of the details modal, shows the student's identifier (Student ID + name), and surfaces success/error feedback inline instead of via `window.alert()`.

2. **Admin — Strict permanent-delete confirmation modal** — Added a second confirmation modal (`#permanentDeleteConfirmModal`) on `admin.html` and rewired `admin-users.js` so that clicking **Permanently Delete** in the existing soft/hard chooser opens the new typing-confirm modal instead of the old `window.confirm()` JS alert. Soft-delete (Deactivate Account) is unchanged because it is reversible. The new modal echoes the username being removed, requires typing `delete`, and shows result feedback inline.

### Files Changed (this session)
| File | Change |
|------|--------|
| `static/registrar.html` | Added `#deleteRecordConfirmModal` typing-confirm modal above the footer. |
| `static/js/registrar-students.js` | Added `deleteConfirmModal`, `currentRecordIdentifier` module state. Replaced inline delete handler with `setupDeleteRecordFlow()` that opens the new modal, gates the confirm button on `value.trim().toLowerCase() === 'delete'`, and calls the existing DELETE endpoint. Removed all `window.confirm()` / `window.alert()` calls. |
| `static/admin.html` | Added `#permanentDeleteConfirmModal` typing-confirm modal alongside the existing `#deleteConfirmModal`. |
| `static/js/admin-users.js` | Added `currentDeleteUserName`, `permanentDeleteConfirmModal` module state. `confirmHardDeleteBtn` click now hides the chooser and calls `openPermanentDeleteModal()`. New `setupPermanentDeleteFlow()` validates the typed input, calls `deleteUser(id, true)`, and reloads the table on success. Removed the `window.confirm()` extra prompt. |

### Verified
- `./gradlew build -x test` → BUILD SUCCESSFUL
- Branch: `feature/registrar-fix`
- No backend or DB changes; this is purely a stricter UX guardrail in front of the existing DELETE endpoints.

### Items Completed (continued)
3. **Emoji cleanup across the static frontend** — Removed visible emoji glyphs from user-facing pages and JS, leaving only the third-party `datatables.min.js` / `datatables.min.css` files (which are vendored libraries) untouched. Specifically:
   - `registrar.html` and `trainer.html`: removed the `✏️` and `🚪` glyphs (and their wrapping `<span class="dropdown-icon">`) from the **Edit Account** and **Log Out** dropdown items.
   - `index.html`: removed the `✅` from the post-logout notification banner.
   - `student-portal.html`: removed the `⚠️` from the "Existing Record Found" alert title.
   - `student-details.js`: replaced all `✓` checkmark prefixes used as upload-status markers with the literal word `Uploaded:`. The `startsWith('✓')` validator check on `baptCertStatus` was updated to `startsWith('Uploaded:')` so the gating logic is preserved.

### Files Changed (continued)
| File | Change |
|------|--------|
| `static/registrar.html` | Removed `<span class="dropdown-icon">✏️</span>` and `<span class="dropdown-icon">🚪</span>` from the account dropdown items. |
| `static/trainer.html` | Same removals as registrar.html. |
| `static/index.html` | Dropped `✅ ` prefix from the logout notification text. |
| `static/student-portal.html` | Dropped `⚠️ ` prefix from the duplicate-record alert. |
| `static/js/student-details.js` | Replaced 3 occurrences of `` `✓ ${...}` `` with `` `Uploaded: ${...}` ``; updated the `startsWith('✓')` check in the baptismal-cert validator to `startsWith('Uploaded:')` so the logic still detects an already-uploaded file. |

## Previous Session (May 7, 2026 — Bugs & Registrar Features)

### Items Completed
1. **Feature 2 — "Not Available" instead of literal "null"** — `renderNullable()`, `renderStatusBadge()` null case, and `setText()` in `registrar-students.js` all show the styled italic "Not Available" span.

2. **Bug 3 — Remove ID Photo as required field** — Removed asterisk from label in `student-details.html`; removed the ID photo required check from `STEP_CUSTOM_VALIDATORS`.

3. **Bug 2 — Parents/Guardian in Registrar view and edit** — Backend: `StudentRecordDetailsResponse` and `StudentRecordUpdateRequest` each gained `father`, `mother`, `guardian` fields. `RegistrarService.buildDetailsResponse()` loads parent/guardian rows from repos. `updateRecord()` calls `saveParents()` and `saveGuardian()`. Frontend view: Father/Mother/Guardian detail-grid sections added to `registrar.html` modal; `registrar-students.js` populates all 24 sub-fields in `loadRecordDetails()`. Frontend edit: Father/Mother/Guardian form sections added to `student-records.html`; `registrar-student-records-edit.js` `populateForm()` and `buildPayload()` updated.

4. **Feature 1 — Delete student record** — Backend: `RegistrarService.deleteRecord()` deletes uploads (physical + DB), all child rows in FK order, then the parent row. `RegistrarController` exposes `DELETE /{recordId}`. Frontend: Delete button added to modal footer in `registrar.html`; `registrar-students.js` shows `window.confirm()`, calls DELETE API, hides modal, reloads table.

5. **Feature 3 — Auto-assign batch on submit** — `BatchRepository.findFirstByBatchYear(Short)` added. `StudentDetailsService.submitEnrollment()` auto-assigns the current-year batch if none is set.

6. **Bug 1 — Defer file uploads to submit** — `setupFileInput()` rewritten to store `File` in `pendingIdPhoto`/`pendingBaptCert`, show local FileReader preview, and display "Selected: filename" status without any network request. `submitForm()` uploads pending files after the JSON submit succeeds. Baptism cert validator updated: accepts `pendingBaptCert !== null` as sufficient.

### Files Changed (this session)
| File | Change |
|------|--------|
| `dto/registrar/StudentRecordDetailsResponse.java` | Added `father`, `mother`, `guardian` fields; new 7-arg `from()` factory; 4-arg and 1-arg delegates |
| `dto/registrar/StudentRecordUpdateRequest.java` | Added `father`, `mother`, `guardian` optional fields |
| `service/RegistrarService.java` | Constructor expanded to 12-arg (+parent/guardian/education/upload repos + StorageService); `buildDetailsResponse()` loads parents/guardian; `updateRecord()` calls `saveParents`/`saveGuardian`; new `deleteRecord()` with full FK cascade deletion |
| `controller/RegistrarController.java` | Added `DELETE /{recordId}` endpoint with system log |
| `repository/StudentUploadRepository.java` | Added `findByStudentId()` and `deleteByStudentId()` |
| `repository/StudentRecordRepository.java` | Added native `deleteDocumentsByStudentId()` and `deleteGradesByStudentId()` |
| `repository/BatchRepository.java` | Added `findFirstByBatchYear(Short)` |
| `service/StudentDetailsService.java` | Constructor +`BatchRepository`; `submitEnrollment()` auto-assigns current-year batch |
| `test/service/StudentDetailsServiceTest.java` | Added `@Mock BatchRepository batchRepo` |
| `test/service/RegistrarBulkLoadTest.java` | Added 5 new `@Mock` fields for expanded 12-arg constructor |
| `static/registrar.html` | Father/Mother/Guardian detail-grid sections in modal; Delete button in footer |
| `static/js/registrar-students.js` | `currentRecordId` state; `loadRecordDetails()` populates 24 parent/guardian fields; delete button handler |
| `static/student-records.html` | Father/Mother/Guardian form sections added; JS cache bumped to `?v=3` |
| `static/js/registrar-student-records-edit.js` | `populateForm()` fills parent/guardian fields; `buildParent()`/`buildGuardian()` helpers; `buildPayload()` includes them |
| `static/js/student-details.js` | `setupFileInput()` defers to pending vars; `submitForm()` uploads after JSON submit; baptism cert validator accepts `pendingBaptCert` |
| `static/student-details.html` | Removed `*` from ID Photo label |

### Verified
- `./gradlew test` → BUILD SUCCESSFUL (all tests pass, no regressions)
- Branch: `feature/registrar-fix`

## Previous Session (May 6, 2026 — Registrar Enhancements)

### Features Implemented
1. **Status filter on `registrar.html`** — new `<select id="studentStatusFilter">` in the filter bar (All / Enrolling / Submitted / Active / Graduated). `buildAjaxUrl()` appends `?status=` when non-empty; Reset clears the select. Backend: `RegistrarController` forwards `status` param; `RegistrarService.getAllRecords` gains a 4-arg overload with case-insensitive status filter. Older 2-arg and 3-arg overloads delegate to the new one.

2. **OJT, TESDA, SchoolYears on `student-records.html` edit form** — three new sections added to the form. `RegistrarService.updateRecord()` is now `@Transactional` and persists OJT (upsert/delete), TESDA (delete-all-insert-new, with flush to avoid unique-constraint race), and SchoolYears (delete-all-insert-new with reassigned rowIndex). `getRecordById()` loads all three collections and passes them to the response factory. `StudentRecordDetailsResponse` and `StudentRecordUpdateRequest` each gained three new fields (`ojt`, `tesdaQualifications`, `schoolYears`).

### Files Changed
| File | Change |
|------|--------|
| `dto/registrar/StudentRecordDetailsResponse.java` | Added 3 new fields; new 4-arg `from()` factory; old 1-arg `from()` delegates |
| `dto/registrar/StudentRecordUpdateRequest.java` | Added `ojt`, `tesdaQualifications`, `schoolYears` fields (all optional) |
| `service/RegistrarService.java` | New 7-arg constructor (+3 repos); 4-arg `getAllRecords`; `matchesStatus()`; `@Transactional` `updateRecord` with OJT/TESDA/SchoolYear helpers; `buildDetailsResponse()` helper |
| `controller/RegistrarController.java` | Added `status` `@RequestParam` forwarded to service |
| `test/service/RegistrarBulkLoadTest.java` | Added 3 new `@Mock` repos; added `statusFilterRestrictsResultsByStudentStatus` test |
| `test/controller/RegistrarBulkLoadWebMvcTest.java` | Updated all 3 mock stubs from 3-arg to 4-arg `getAllRecords` |
| `static/registrar.html` | Added status `<select>` to filter bar |
| `static/js/registrar-students.js` | Extended `buildAjaxUrl()` + Reset handler for status |
| `static/student-records.html` | Added OJT section, 3-slot TESDA fieldsets, SchoolYears dynamic table; bumped JS to `?v=2` |
| `static/js/registrar-student-records-edit.js` | Added OJT/TESDA/SchoolYear `populateForm`, `buildPayload`, row handlers, `esc()`, `createSchoolYearRow()`, form-level dirty delegation |

### Verified
- `./gradlew test` → BUILD SUCCESSFUL (all tests pass, no regressions)

## Previous Session (May 5, 2026 — Enrollment Flow Fix)
Fixed 5 root-cause bugs in the student enrollment flow. All changes scoped strictly
to student-portal and student-details (no Registrar/Trainer code touched).

### Root Causes Fixed
1. **RC-1 (CRITICAL): Premature DB persistence** — `startOrResume()` was saving full
   student data on portal start. Now only name + status are persisted (minimal record
   needed for upload FK constraints).
2. **RC-2 (CRITICAL): saveDraft on every Next click** — Removed. Data stays in browser
   sessionStorage until final submit. No intermediate DB writes.
3. **RC-3 (MEDIUM): Single transactional submit** — New `submitEnrollment()` method
   persists student record + parents + guardian + education + school years atomically in
   one `@Transactional` block. Old two-step saveDraft/submit flow is gone.
4. **RC-4: OJT/TESDA removal from student flow** — Removed from HTML, JS (buildPayload,
   populateForm), request DTO, response DTO, and service layer. Entities/repos kept
   for Registrar/Trainer use.
5. **RC-5 (LOW): AgeCalculator null handling** — Returns `Integer null` instead of
   `int 0` when birthdate is null. Prevents misleading `age=0` in DB.

### Files Changed
| File | Change |
|------|--------|
| `StudentDetailsService.java` | Rewrote: removed saveDraft/old submit; added submitEnrollment; removed OJT/TESDA handling |
| `StudentDetailsController.java` | Removed PUT saveDraft endpoint; new POST submit with full payload |
| `StudentDetailsRequest.java` | Added name fields; removed OJT/TESDA |
| `StudentDetailsResponse.java` | Removed OJT/TESDA |
| `student-details.js` | Removed saveDraft calls; removed OJT/TESDA from payload/populate; submit sends full body |
| `student-details.html` | Removed OJT/TESDA HTML sections; bumped JS cache to v=4 |
| `AgeCalculator.java` | Return type `int` → `Integer`, null → null |
| `AgeCalculatorTest.java` | Updated: assertEquals(0) → assertNull |
| `StudentDetailsServiceTest.java` | **NEW** — 7 tests covering start, resume, submit, double-submit, load |

### Files NOT Changed (scope boundary)
- All Registrar/Trainer controllers, services, HTML, JS
- OJT/TESDA entities + repositories (kept for Registrar use)
- AdminService, RegistrarService, AccountService

## Verified
- Full test suite: BUILD SUCCESSFUL (all tests pass)
- New StudentDetailsServiceTest: 7 tests pass
- AgeCalculatorTest: 6 tests pass (updated for null return)
- No regressions in existing AdminServiceTest, AccountServiceTest, etc.

## Previous Session (May 5, 2026 — Schema Drift)
- Applied migration `2026-05-05-fix-schema-drift.sql`
- Deleted DataSeeder.java
- 82/82 tests passed

## Open Items
- **Data cleanup consideration:** Existing "Enrolling" records in production DB from
  the old premature-create behavior may need review/cleanup
- **Upload flow:** Uploads still require a minimal DB record to exist (FK constraint).
  The current approach creates a name-only record on start, which is the minimum viable.
- **E2E browser testing:** Not yet done. Consider testing the full portal → details →
  submit flow in a browser.
