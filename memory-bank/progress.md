# Progress - Anihan SRMS

## Strict Type-to-Confirm Delete Modals (Completed — May 7, 2026)
- [x] **Registrar — `deleteRecordConfirmModal`**: New Bootstrap modal in `registrar.html` requiring the user to type `delete` before the Permanently Delete button is enabled; replaces `window.confirm()` and `window.alert()` in `registrar-students.js`.
- [x] **Registrar — modal lifecycle**: Reset input + button state on `hidden.bs.modal`; show identifier (Student ID + last/first name) inside the modal.
- [x] **Admin — `permanentDeleteConfirmModal`**: New typing-confirm modal in `admin.html` alongside the existing soft/hard chooser; opens when **Permanently Delete** is clicked.
- [x] **Admin — wiring**: `admin-users.js` now hides `deleteConfirmModal` and opens `permanentDeleteConfirmModal` instead of using `window.confirm()`. Soft delete (deactivate) flow unchanged.
- [x] **Admin — modal lifecycle**: Reset input + button state on `hidden.bs.modal`; show username inside the modal.
- [x] `./gradlew build -x test` → BUILD SUCCESSFUL
- [x] Branch: `feature/registrar-fix`

## Bugs & Registrar Features — Parents/Guardian, Delete, Deferred Uploads (Completed — May 7, 2026)
- [x] **Feature 2**: "Not Available" replaces literal "null" in registrar table and modal (`renderNullable`, `renderStatusBadge`, `setText`)
- [x] **Bug 3**: ID Photo no longer a required field — asterisk removed from label, validator removed
- [x] **Bug 2 Backend**: `StudentRecordDetailsResponse` + `StudentRecordUpdateRequest` gained `father`, `mother`, `guardian` fields; `RegistrarService` loads/saves parents and guardian
- [x] **Bug 2 Frontend View**: Father/Mother/Guardian detail-grid sections in `registrar.html` modal; `registrar-students.js` populates all sub-fields
- [x] **Bug 2 Frontend Edit**: Father/Mother/Guardian form sections in `student-records.html`; `registrar-student-records-edit.js` populates and saves them
- [x] **Feature 1 Backend**: `RegistrarService.deleteRecord()` deletes child rows in FK order + physical uploads; `RegistrarController` exposes `DELETE /{recordId}`
- [x] **Feature 1 Frontend**: Delete button in modal footer; `registrar-students.js` confirm → DELETE API → reload table
- [x] **Feature 3**: `BatchRepository.findFirstByBatchYear(Short)` added; `StudentDetailsService.submitEnrollment()` auto-assigns current-year batch
- [x] **Bug 1**: `setupFileInput()` defers file selection to pending JS variables; `submitForm()` uploads after JSON submit succeeds; baptism cert validator accepts pending file
- [x] `./gradlew test` → BUILD SUCCESSFUL — all tests pass, no regressions
- [x] Branch: `feature/registrar-fix`

## Registrar Enhancements — Status Filter + OJT/TESDA/SchoolYears Edit Form (Completed — May 6, 2026)
- [x] Status filter (`<select>`) added to registrar home filter bar — All / Enrolling / Submitted / Active / Graduated
- [x] `RegistrarService.getAllRecords` 4-arg overload with case-insensitive status filter; older overloads delegate to it
- [x] `RegistrarController` forwards `?status=` param to service
- [x] `StudentRecordDetailsResponse` + `StudentRecordUpdateRequest` each gained `ojt`, `tesdaQualifications`, `schoolYears` fields
- [x] `RegistrarService.updateRecord()` made `@Transactional`; persists OJT (upsert/delete), TESDA (delete+flush+insert), SchoolYears (delete+flush+insert with reassigned rowIndex)
- [x] `getRecordById()` now loads OJT, TESDA, SchoolYears and returns them in the response
- [x] `student-records.html` edit form: OJT section, 3-slot TESDA fieldsets, dynamic SchoolYears table with Add/Remove row
- [x] `registrar-student-records-edit.js`: extended `populateForm`, `buildPayload`, school year row handlers, form-level dirty delegation for dynamic inputs
- [x] `RegistrarBulkLoadTest`: 3 new `@Mock` repos, new `statusFilterRestrictsResultsByStudentStatus` test
- [x] `RegistrarBulkLoadWebMvcTest`: all 3 stubs updated from 3-arg to 4-arg `getAllRecords`
- [x] `./gradlew test` → BUILD SUCCESSFUL (all tests pass, no regressions)
- [x] Branch: `feature/registrar-fixes`

## Student Portal Enrollment Flow Fix (Completed — May 5, 2026)
- [x] **RC-1 (CRITICAL):** Fixed premature DB persistence — `startOrResume()` now creates name+status only (minimal record for upload FK)
- [x] **RC-2 (CRITICAL):** Removed `saveDraft()` on every Next click — data stays in browser until final submit
- [x] **RC-3 (MEDIUM):** New `submitEnrollment()` persists student record + parents + guardian + education + school years in one `@Transactional` block
- [x] **RC-4:** Removed OJT/TESDA from student-facing flow (HTML, JS, request DTO, response DTO, service layer); entities/repos kept for Registrar
- [x] **RC-5 (LOW):** `AgeCalculator` returns `Integer null` instead of `int 0` for null birthdate
- [x] Created `StudentDetailsServiceTest.java` — 7 tests covering start, resume, submit, double-submit guard, load, invalid ID
- [x] Updated `AgeCalculatorTest.java` — null birthdate assertion changed from `assertEquals(0)` to `assertNull`
- [x] `./gradlew test` → BUILD SUCCESSFUL (all tests pass, no regressions)
- [x] Branch: `fix/student-portal-flow`

## Schema Drift Remediation + DataSeeder Removal (Completed — May 5, 2026)
- [x] Diagnosed live `AnihanSRMS` DB against `schema.sql` / `AnihanSRMS.sql` and JPA entities — found `civil_status` missing on `student_records` and 27 columns over three tables locked as `NOT NULL` instead of `NULL`
- [x] Reproduced single test failure: `SpringbootApplicationTests > contextLoads()` — "Unknown column 'sr1_0.civil_status' in 'field list'"
- [x] Created `src/main/sql/migrations/2026-05-05-fix-schema-drift.sql` (idempotent for `civil_status` via `information_schema` guard)
- [x] Applied migration to live DB:
  - `student_records`: added `civil_status`, relaxed 12 columns to NULL
  - `parents`: relaxed 9 columns to NULL, dropped `est_income DEFAULT 0.00`
  - `other_guardians`: relaxed 6 columns to NULL
- [x] Deleted `src/main/java/com/example/springboot/config/DataSeeder.java` — application data must come from the database, not from a `CommandLineRunner` seeding fake students every startup
- [x] Refreshed headers on `schema.sql` and `AnihanSRMS.sql` so other developers know the migration file exists for legacy DBs
- [x] `./gradlew test` → BUILD SUCCESSFUL — 82 tests, 0 failures (was 81/82 before)
- [x] Working uncommitted on `main` per user instruction (no branch, no commit)

## Student Portal — Mandatory Field Validation (Completed — May 3, 2026)
- [x] Step 1: Added Civil Status as required field
- [x] Step 2: Religion already required; added ID Photo upload as always-required
- [x] Step 2: Baptism Date, Baptism Place, and Baptismal Certificate conditionally required when "Baptized" checkbox is checked
- [x] Step 3: Father core fields required (Family Name, First Name, Occupation, Contact No., Address)
- [x] Step 3: Mother core fields required (same 5 fields)
- [x] Step 3: Guardian fields remain optional
- [x] Added `STEP_CUSTOM_VALIDATORS` pattern for runtime-dependent validation (file uploads, conditional fields)
- [x] Updated `validateStep()`, `validateAll()`, `clearValidation()` to merge custom validators
- [x] Added `*` asterisk markers to all newly-required labels in HTML
- [x] Added "Fields marked * are required" note to Steps 2 and 3
- [x] Bumped JS cache version v=2 → v=3
- [x] `./gradlew test` → BUILD SUCCESSFUL
- [x] Branch: `feature/student-field-validation`

## Database Sync Migration (Completed — May 2, 2026)
- [x] Full cross-check of live DB against `AnihanSRMS.sql` + `schema.sql`
- [x] Dropped 4 legacy tables: `classess`, `log`, `previous_school`, `qualification_assessment`
- [x] Changed `student_records` PK from `student_id` → `record_id`
- [x] Added missing `civil_status` column to `student_records`
- [x] Fixed nullability on `student_records` (12 columns), `parents` (9 columns), `other_guardians` (6 columns)
- [x] Fixed `documents.upload_date` type (TIMESTAMP → DATETIME)
- [x] Inserted seed data: CARS course, 3 batches, 3 sections
- [x] Kept existing user accounts intact
- [x] `./gradlew test` → BUILD SUCCESSFUL — all tests pass
- [x] Database now has exactly 17 tables matching both SQL files
## Student Status Dropdown + Badge Colors (Completed — May 4, 2026)
- [x] Replaced `<input list>` + `<datalist>` for Status field on `student-records.html` with `<select>` (Enrolling, Active, Graduated only — "Submitted" removed as registrar-facing option)
- [x] Rewrote `renderStatusBadge()` in `registrar-students.js` — Active=green, Enrolling/Submitted=grey, Graduated=blue, unknown=red
- [x] Added `.status-badge-enrolling` (grey) and `.status-badge-graduated` (blue) to `dashboard.css`
- [x] Chapter 3 FR 3.1 verified: 3 official statuses; "Submitted" is portal-only transitional state

## Fix Search Bar Selector + Filter Input Width + DataSeeder (Completed — May 2, 2026)
- [x] Fixed search bar CSS: dual selector covers `.dataTables_filter input` (DataTables 1) and `.dt-search input` (DataTables 2)
- [x] Fixed batch year filter inputs: `width: 90px; max-width: 90px; flex: 0 0 auto` on `#batchFromYear, #batchToYear`
- [x] Created `DataSeeder.java` — `@Component CommandLineRunner`, idempotent, seeds CARS + 3 batches + 3 sections + 5 student records on startup
- [x] `./gradlew test` → BUILD SUCCESSFUL — 82 tests, 0 failures

## Registrar Search Bar Width + Batch Year Filter + Dummy Seed Data (Completed — May 2, 2026)
- [x] Wider DataTables search input on the registrar student-records table (320px min-width, scoped to `#studentRecordsTable_wrapper`)
- [x] Batch Year filter UI: From/To number inputs + Apply + Reset buttons mirroring the system-logs filter pattern
- [x] Server-side: `getAllRecords(query, fromYear, toYear)` overload + new `?fromYear=&toYear=` query params on `GET /api/registrar/student-records`
- [x] Validation: `fromYear > toYear` → HTTP 400
- [x] schema.sql now self-contained: 1 course + 3 batches + 3 sections + 5 fully-populated dummy student records (batch years 2024–2026)
- [x] `profile_picture` uses `X''` placeholder BLOB (user requested empty for now; images folder reminded: `static/images/`)
- [x] 4 new tests (1 service + 3 WebMvc); existing tests updated for the new 3-arg signature
- [x] `./gradlew test` → BUILD SUCCESSFUL — 82 tests across 15 classes, 0 failures

## Registrar Bulk Load Tests + H2 Isolation + Server-Side Search (Completed — May 1, 2026)
- [x] Added `getAllRecords(String query)` overload to `RegistrarService` with case-insensitive in-memory filter
- [x] `GET /api/registrar/student-records?q=<query>` now supported (optional param)
- [x] Created `RegistrarBulkLoadTest` (3 Mockito service tests for 200 records: correctness, performance, search across 9 fields)
- [x] Created `RegistrarBulkLoadWebMvcTest` (2 WebMvc tests: 200-record JSON serialization, ?q= query forwarding)
- [x] Created `StudentRecordH2LoadTest` (H2 in-memory integration test, 100 records via real JPA, fully isolated from live MySQL)
- [x] Added `com.h2database:h2` as `testRuntimeOnly` in build.gradle.kts
- [x] Documented Spring Boot 4.0 test-autoconfigure package reorganization (DataJpaTest moved to `org.springframework.boot.data.jpa.test.autoconfigure`; AutoConfigureTestDatabase moved to `org.springframework.boot.jdbc.test.autoconfigure`)
- [x] `./gradlew test` → BUILD SUCCESSFUL — 79 tests across 15 classes, 0 failures, 0 skipped

## Registrar Edit Student Record + Search + Unsaved Notifications (Completed — May 1, 2026)
- [x] Search bar: DataTables 2 default search input is already rendered on the registrar home table — searches across all 8 columns
- [x] Created `BatchRepository` and `CourseRepository` (previously missing)
- [x] Extended `LookupController` with `GET /api/lookup/batches` and `GET /api/lookup/courses`
- [x] Created `StudentRecordUpdateRequest` DTO with `@NotBlank`/`@PastOrPresent` validation
- [x] Added `RegistrarService.updateRecord()` — FK resolution, studentId-uniqueness, age recalc via `AgeCalculator`, blank-to-null mapping
- [x] Added `PUT /api/registrar/student-records/{recordId}` to `RegistrarController` with `SystemLogService.logAction()`
- [x] Built full edit form in `student-records.html` (recordId + enrollmentDate read-only; rest editable)
- [x] Batch/Course/Section/Status/Sex/Civil Status use `<input list>` + `<datalist>` for combined dropdown + free-text
- [x] Created `js/registrar-student-records-edit.js` with dirty-tracking, `beforeunload` + nav-intercept confirm dialog, lookup loading, save handler
- [x] `./gradlew compileJava` → BUILD SUCCESSFUL

## Registrar Home: Student Records Table & Detail Modal (Completed — May 1, 2026)
- [x] Created `RegistrarController` with `GET /api/registrar/student-records` (list) and `GET /api/registrar/student-records/{recordId}` (full details)
- [x] Created `RegistrarService` reusing `StudentRecordRepository.findAll()` and `findById(Integer)`
- [x] Created two DTOs in new `dto/registrar/` package: `StudentRecordSummaryResponse` (8 fields) and `StudentRecordDetailsResponse` (25 fields, FK relations flattened to codes, BLOB excluded)
- [x] Built `registrar.html` student records dashboard: page-hero, surface-card with 9-column DataTable, full detail modal (all fields), Edit link goes to `student-records.html?id={recordId}`
- [x] Created `js/registrar-students.js` (DataTables init + modal AJAX + null-rendering logic)
- [x] Repurposed `student-records.html` for the registrar (REGISTRAR role, registrar navbar, placeholder content kept)
- [x] `SecurityConfig`: moved `/student-records.html` from ADMIN to REGISTRAR
- [x] `./gradlew compileJava` → BUILD SUCCESSFUL

## Live Age Recalculation in Edit Account Modal (Completed — May 1, 2026)
- [x] Added `calculateAgeFromBirthdate()` and `updateAgeDisplay()` helpers to `auth-guard.js`
- [x] Birthdate `<input>` now triggers live recalculation of `#ageDisplay` on `change`
- [x] Personal-details save success now refreshes `#ageDisplay` and `#birthdate` from the server response
- [x] All three role pages (admin, registrar, trainer) benefit since they share `auth-guard.js`
- [x] Confirmed registrar.html and trainer.html already had the correct read-only `<p id="ageDisplay">` markup (no editable age input)

## Registrar Navbar Standardization (Completed — May 1, 2026)
- [x] `registrar.html` — replaced simple placeholder navbar with full Bootstrap-collapsible navbar matching admin pattern
- [x] Navbar has two links: **Home** (active, → `registrar.html`) and **Subjects** (→ `subjects.html`)
- [x] Added `class="dashboard-page"` to `<body>` to align with the dashboard page convention
- [x] Reused `admin-navbar`, `admin-nav-link`, `portal-label` CSS — no new styles required
- [x] `subjects.html` rebranded to registrar:
  - `data-required-role` changed `ROLE_ADMIN` → `ROLE_REGISTRAR`
  - `<title>` changed `Subjects — Admin` → `Subjects — Registrar`
  - Old 4-link admin navbar replaced with 2-link registrar navbar (Home, Subjects active)
  - Brand link (`navbar-brand`) now points to `registrar.html`
  - Portal label changed `Admin Portal` → `Registrar Portal`
  - Main content kept as placeholder (heading + muted text) per user request
- [x] `SecurityConfig.java` — `subjects.html` moved from ADMIN matcher to REGISTRAR matcher
- [x] Resolved one of the stale-navbar items flagged in `systemPatterns.md` (`subjects.html`)

## Current Status
- **Tasks in Progress**: Browser-level revalidation of the repaired admin dashboard and edit-user flow after the front-end shell rebuild.
- **Completed**: G2.1 Edit Personal Details (Sprint 4 completed). Database Schema migration to support distinct name fields (lastName, firstName, middleName) and birthdate.

## Research & Documentation (Completed)
- [x] Extracted Project Scope, Problem Statements, and Limitations from `Chapter-1.pdf`
- [x] Defined Actor roles (Registrar, Trainer, Admin, Student) and Use Cases from `Chapter-3.pdf`
- [x] Defined System Workflows via BPMNs and DFDs (Enrollment, Special Order, Grading)
- [x] Established Data Dictionaries & Base ERD Entities
- [x] Recorded Deployment Constraints (Local Network, Windows Server 2025)

## Backend & Infrastructure (Completed)
- [x] Project folders created (`controller`, `model`, `repository`, `service`)
- [x] Memory bank initialized & populated with Capstone constraints
- [x] Gradle upgraded to 9.4.1
- [x] Existing MySQL 8 via Docker integration
- [x] `AnihanSRMS.sql` ERD mapped into 11 JPA Entities
- [x] Initial Spring Security 7 RBAC & Authentication logic (LoginRequest, AuthController)
- [x] Global Exception Handler established
- [x] Data Seeder executed for Admins, Registrars, and Trainers
- [x] Removed DataSeeder - accounts managed directly in database
- [x] Added unique constraint on `users.username` column

## Login Security & Account Management (Completed - AGILE-142)
- [x] Server-side role-based page matchers for dashboard HTML pages
- [x] Dual-mode authentication entry point (redirect browser / JSON API)
- [x] Dual-mode access denied handler (wrong-role -> own dashboard / API -> 403 JSON)
- [x] AccountService with username change and password change logic
- [x] AccountController with PUT /api/account/profile and PUT /api/account/password
- [x] UpdateProfileRequest and UpdatePasswordRequest DTOs with validation
- [x] IllegalArgumentException handler in GlobalExceptionHandler
- [x] Shared auth-guard.js for all authenticated pages
- [x] Account dropdown (icon + menu) on all dashboard navbars
- [x] Edit Account modal (username change + password change)
- [x] Session check on login page to redirect already-authenticated users
- [x] Cache-control headers disabled for authenticated pages
- [x] Password change forces session invalidation and re-login
- [x] Enforce strict case-sensitivity for username login (email remains case-insensitive)

## Frontend & UI (Completed)
- [x] Login page front-end UI (`index.html`, `css/login.css`)
- [x] Dashboard empty templates built (`admin.html`, `registrar.html`, `trainer.html`, `student-records.html`, `subjects.html`, `logs.html`)
- [x] Custom JS fetch logic for form interactions
- [x] SQL database schema created and updated (`AnihanSRMS.sql`)
- [x] Admin User Management Dashboard (`admin.html`) built with DataTables and Bootstrap Navbar.
- [x] User Details Modal & Edit User Page (`edit-user.html`) with self-role lock protections.
- [x] Admin UI Layout Polish (Navbar color standardization & `edit-user.html` structural cleanup).
- [x] Dashboard templates built (`admin.html`, `registrar.html`, `trainer.html`) 
- [x] Custom JS fetch logic for form interactions
- [x] Dashboard CSS for account dropdown and modal styling
- [x] Shared auth-guard.js for session protection and account UI
- [x] Merged admin user-management dashboard into root `admin.html`
- [x] Added root admin pages: `edit-user.html`, `student-records.html`, `subjects.html`, `logs.html`
- [x] Unified merged admin pages under the root green/white/blue shell and shared account modal behavior
- [x] Rebuilt `admin.html` after a malformed donor merge left duplicate markup and caused a blank admin page
- [x] Rebuilt `edit-user.html` to match the current `admin-edit-user.js` contract and shared admin shell
- [x] Split admin Users table "Name" column into separate "Last Name" and "First Name" columns (`admin.html` + `admin-users.js`)

## Admin Merge (Completed)
- [x] Added `/api/admin/users` and `/api/admin/users/{id}` in the root app using DTO responses
- [x] Added root-side admin update flow with self-role-lock protection
- [x] Extended root RBAC for admin-only HTML pages introduced from `main-em`
- [x] Added automated tests for admin service and admin controller behavior
- [x] Normalized `AnihanSRMS.sql` ordering and root-aligned users/student records keys
- [x] Removed unresolved merge markers and donor regressions that broke commit safety
- [x] Re-verified the branch with `./gradlew test` and `./gradlew build`
- [x] Restored `AdminController` to the service/DTO implementation after a repository-based regression broke `AdminControllerWebMvcTest`

## Admin Password, Delete & Hover Fix (Completed — April 2026)
- [x] Added optional password field to admin edit-user form + backend DTO (`AdminUpdateUserRequest`)
- [x] Admin can reset any user's password (BCrypt-hashed, 8-char minimum)
- [x] Added `@Size(min=8)` validation to self-service `UpdatePasswordRequest` DTO
- [x] Added `enabled` column to `users` table schema (soft delete support)
- [x] Added `enabled` field to `User.java` entity + `AdminUserResponse` DTO
- [x] `CustomUserDetailsService` blocks disabled users from logging in via Spring Security `enabled` flag
- [x] Added `DELETE /api/admin/users/{id}` (soft delete — deactivate) endpoint
- [x] Added `DELETE /api/admin/users/{id}/permanent` (hard delete — permanent removal) endpoint
- [x] Self-deletion prevention in `AdminService` (backend) and `admin-users.js` (frontend — hides button)
- [x] Added delete confirmation modal (`deleteConfirmModal`) with Deactivate + Permanently Delete options
- [x] Hard delete requires extra browser `confirm()` dialog
- [x] Added Status column (Active/Disabled badge) to admin DataTable
- [x] Fixed `.btn-surface:hover` / `:focus` CSS — explicitly sets green background so Bootstrap doesn't override to white
- [x] Added client-side 8-char password validation in `auth-guard.js` and `admin-edit-user.js`
- [x] Added `minlength="8"` to all password input fields in Edit Account modals
- [x] Updated all test files (`AdminServiceTest`, `AdminControllerWebMvcTest`) for new constructor signatures
- [x] `./gradlew build` passes (7/7 tasks, all tests green)

## Admin Enhancements Phase 2 (Completed — April 2026)
- [x] Added `password_changed_at DATETIME NULL` column to `users` table + `User.java` entity
- [x] Added `passwordChangedAt` to `AdminUserResponse` DTO
- [x] Admin Service sets `passwordChangedAt` on admin password reset
- [x] Account Service sets `passwordChangedAt` on self-service password change
- [x] "Password Last Changed" displayed in User Details modal (formatted date or "Never")
- [x] Added `PUT /api/admin/users/{id}/enable` endpoint for re-enabling soft-deleted users
- [x] Added `reEnableUser()` method to `AdminService`
- [x] Added "Re-enable Account" button in User Details modal (green, shown only for disabled users)
- [x] Added password visibility toggle (eye icon SVG) — auto-injected on ALL `input[type=password]` via `auth-guard.js`
- [x] Toggle resets correctly on modal close (input type reverted, icons reset)
- [x] Added strong password validation — `@Pattern` regex on `UpdatePasswordRequest.newPassword`
- [x] Requires: 1 uppercase, 1 lowercase, 1 number, 1 special character, min 8 chars
- [x] Client-side strong password check in `auth-guard.js` `setupPasswordChange()`
- [x] Admin password resets remain simple (8-char minimum only — temporary passwords allowed)
- [x] Added `minlength="8"` + strong password help text to `registrar.html` and `trainer.html`
- [x] Added CSS for `.btn-reenable` and `.password-toggle-btn`
- [x] Updated test files for new `passwordChangedAt` field
- [x] DB migration executed: `ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL`
- [x] `./gradlew build` passes (7/7 tasks, all tests green)

## Admin Enhancements Phase 3 — Username Edit & Hover Fixes (Completed — April 2026)
- [x] Fixed `.btn-reenable:hover` → `.btn.btn-reenable:hover` (Bootstrap specificity override)
- [x] Fixed `.btn-danger-surface`, `.btn-deactivate`, `.btn-permanent-delete` hover selectors (same pattern)
- [x] Added `username` field to `AdminUpdateUserRequest` DTO with `@Pattern(^\S+$)` no-spaces validation
- [x] Updated `AdminService.updateUser()` with duplicate-username check via `findByUsername()`
- [x] Made username field editable in `edit-user.html` (was `disabled`), added `pattern` + help text
- [x] Added `username` to `buildPayload()` in `admin-edit-user.js`
- [x] Added client-side no-spaces validation in `saveUser()` in `admin-edit-user.js`
- [x] Updated contract note in `edit-user.html`
- [x] Updated `AdminServiceTest` constructors for new `username` parameter (null)
- [x] `./gradlew build` passes (7/7 tasks, all tests green)

## Admin System Logs (Completed — April 14, 2026)
- [x] Created `SystemLog.java` entity mapped to `system_logs` table
- [x] Created `SystemLogRepository.java` with `findAllByOrderByTimestampDesc()`
- [x] Created `SystemLogService.java` with `logAction()` and `getAllLogs()` methods
- [x] Created `SystemLogResponse.java` DTO record with `from()` factory
- [x] Created `SystemLogController.java` — `GET /api/logs` (ADMIN-only)
- [x] Added `/api/logs/**` ADMIN matcher to `SecurityConfig.java`
- [x] Integrated login/logout logging in `AuthController.java` (captures identity before session cleanup)
- [x] Integrated admin action logging in `AdminController.java` (update, soft delete, hard delete, re-enable)
- [x] Integrated self-service logging in `AccountController.java` (password, username, personal details)
- [x] Added `system_logs` table to `AnihanSRMS.sql`
- [x] Rebuilt `logs.html` with unified color scheme, DataTables 2, and auth-guard.js
- [x] Created `system-logs.js` for AJAX fetch and DataTables initialization
- [x] Updated `AdminControllerWebMvcTest.java` with new mock beans
- [x] `./gradlew build` passes (7/7 tasks, all tests green)

## Navbar Logo UI Standardization (Completed — April 16, 2026)
- [x] Removed `<span class="brand-title">Anihan SRMS</span>` from all dashboard navbars to declutter the UI.
- [x] Standardized logo scaling across `.navbar-logo` in login and dashboard pages.
- [x] Enlarged the logo (height set to `85px`) to maximize readability of "ANIHAN TECHNICAL SCHOOL".
- [x] Applied negative vertical margins (`-22px`) to `.navbar-logo` to collapse its layout footprint, keeping the navbar height unchanged.
- [x] Removed the restrictive inline `max-height: 40px` from `logs.html`.
- [x] Removed unused `.brand-title` CSS block.
- [x] `./gradlew build -x test` passes (all HTML/CSS changes verified).

## Admin Navbar Cleanup (Completed — April 17, 2026)
- [x] Removed "Student Records" and "Subjects" nav links from `admin.html`, `edit-user.html`, `add-user.html`, `logs.html`
- [x] Admin navbar now shows: **Home | Logs**
- [x] `student-records.html` and `subjects.html` pages preserved (NOT deleted) for future use
- [x] ⚠️ `student-records.html` and `subjects.html` internal navbars intentionally NOT updated — must be synced when re-adding
- [x] Updated `systemPatterns.md` with current navbar pattern and stale navbar warning
- [x] No CSS changes required — purely HTML `<li>` removal

## Unit Test Coverage Expansion (Completed — April 17, 2026)
- [x] Created `AccountServiceTest.java` — 10 unit tests covering `updateUsername`, `updatePassword`, `updatePersonalDetails`
- [x] Created `SystemLogServiceTest.java` — 4 unit tests covering `logAction`, `getAllLogs`
- [x] Created `AccountControllerWebMvcTest.java` — 7 WebMvc tests for `/api/account/**` endpoints
- [x] Created `SystemLogControllerWebMvcTest.java` — 5 WebMvc tests for `/api/logs` endpoint
- [x] All tests follow established patterns (Mockito + `@ExtendWith(MockitoExtension.class)` for services, `@WebMvcTest` + `@Import(SecurityConfig.class)` for controllers)
- [x] `./gradlew test` → BUILD SUCCESSFUL (all tests green across all modules)

## Admin Bulk Load Tests (Completed — April 18, 2026)
- [x] Created `AdminBulkLoadTest.java` — 3 unit tests verifying `AdminService.getAllUsers()` handles 100 users (count, DTO mapping, performance)
- [x] Created `AdminBulkLoadWebMvcTest.java` — 2 WebMvc tests verifying `GET /api/admin/users` serializes 100 users as JSON
- [x] All 100 test users generated programmatically in-memory via Mockito — no database touched, no hard-coded dummy data
- [x] Performance: service layer 0.008s, HTTP layer 0.663s — well within 5-second non-functional requirement
- [x] No existing test files modified, no new dependencies added
- [x] `./gradlew test` → BUILD SUCCESSFUL (42 tests, all green)

## System Logs Date Filtering (Completed — April 18, 2026)
- [x] Extended `GET /api/logs` with optional `rangeDays`, `startDate`, `endDate` query parameters
- [x] Default behavior changed from full-table fetch to last 7 days only
- [x] Quick filter presets: 7 days, 14 days, 30 days via `rangeDays` query param
- [x] Custom inclusive date range via `startDate` + `endDate` (start of day to end of day)
- [x] Custom range takes precedence over `rangeDays` when both are provided
- [x] Invalid date ranges (startDate > endDate) return HTTP 400
- [x] Added `findByTimestampBetweenOrderByTimestampDesc()` to `SystemLogRepository`
- [x] Replaced `getAllLogs()` with `getLogs(rangeDays, startDate, endDate)` in `SystemLogService`
- [x] Added filter toolbar UI to `logs.html` (preset pills + From/To inputs + Apply/Reset)
- [x] Refactored `system-logs.js` with parameterized `loadLogs()` and filter event handlers
- [x] Updated `SystemLogServiceTest` — 10 tests (2 logAction + 8 getLogs)
- [x] Updated `SystemLogControllerWebMvcTest` — 9 tests (6 filtered + 3 security)
- [x] `./gradlew test` → BUILD SUCCESSFUL (52 tests, 0 failures)

## System Logs Export UI Cleanup (Completed - April 18, 2026)
- [x] Added `GET /api/logs/export` with `format=csv|xlsx|docx`
- [x] Added `SystemLogExportFormat`, `SystemLogQueryResult`, and `SystemLogExportService`
- [x] Reused existing log filter precedence for both page data and exported files
- [x] Added CSV export with summary rows and escaped values
- [x] Added XLSX export with summary header and single-sheet table output
- [x] Added DOCX export with summary header and table output
- [x] Removed the redundant extra date-selection controls from `logs.html`
- [x] Refactored `system-logs.js` to keep one shared filter state for presets, exact dates, and export
- [x] Added export format selector and export button to the logs toolbar
- [x] Added `SystemLogExportServiceTest` - 3 tests covering CSV/XLSX/DOCX generation
- [x] Expanded `SystemLogControllerWebMvcTest` to cover export success, validation, and security cases
- [x] `./gradlew test` -> BUILD SUCCESSFUL (63 tests, 0 failures, 0 skipped)

## Age Auto-Calculation from Birthdate (Completed — April 19, 2026)
- [x] Created `AgeCalculator.java` utility — centralised `Period.between(birthdate, now).getYears()` logic
- [x] Removed `age` field from `AdminUpdateUserRequest`, `AdminCreateUserRequest`, `UpdatePersonalDetailsRequest` DTOs
- [x] Made `birthdate` `@NotNull` (mandatory) in `AdminCreateUserRequest`; removed default value `2000-01-01`
- [x] `AdminService.createUser()` and `updateUser()` auto-calculate age from birthdate via `AgeCalculator`
- [x] `AdminService.getUserById()` silently recalculates + persists age on every detail view (NOT logged)
- [x] `AuthController.currentUser()` (`GET /api/auth/me`) silently recalculates + persists age (NOT logged)
- [x] `AccountService.updatePersonalDetails()` auto-calculates age from birthdate
- [x] Admin user table (`GET /api/admin/users`) does NOT recalculate — returns stored age (avoids bulk writes)
- [x] Replaced age `<input>` with read-only `<p id="ageDisplay">` in 6 HTML pages: `admin.html`, `edit-user.html`, `add-user.html`, `registrar.html`, `trainer.html`, `logs.html`
- [x] Removed `age` from JS payloads in `admin-edit-user.js`, `admin-add-user.js`, `auth-guard.js`
- [x] Added client-side birthdate validation in `admin-add-user.js`
- [x] Updated test constructors/assertions in `AdminServiceTest`, `AccountServiceTest`, `AdminControllerWebMvcTest`, `AccountControllerWebMvcTest`
- [x] ⚠️ `student-records.html` and `subjects.html` intentionally NOT updated — deferred to student-focused session
- [x] `./gradlew test` → BUILD SUCCESSFUL (63 tests, 0 failures, 0 skipped)
- [/] Refactoring logic to align heavily with newly integrated Capstone Requirements

## Student Portal — Welcome Page (Completed — April 26, 2026)
- [x] Created `StudentRecordRepository.java` with case-insensitive name duplicate check
- [x] Created `StudentPortalController.java` with `GET /api/student-portal/check-duplicate` (public, no auth)
- [x] Created `student-portal.html` — welcome page mirroring login page design (name form instead of credentials)
- [x] Created `student-portal.js` — duplicate check on submit, warning alert or redirect
- [x] Updated `SecurityConfig.java` — student portal pages and API added to `permitAll()`
- [x] Names passed to next page via URL query params (no DB insert on welcome page)
- [x] `./gradlew build` → BUILD SUCCESSFUL

## Student Details Enrollment Wizard (Completed — April 29, 2026)
- [x] Created 5 new JPA entities: `StudentEducation`, `StudentSchoolYear`, `StudentOjt`, `StudentTesdaQualification`, `StudentUpload`
- [x] Created 7 new repositories: `ParentRepository`, `OtherGuardianRepository`, `StudentEducationRepository`, `StudentSchoolYearRepository`, `StudentOjtRepository`, `StudentTesdaQualificationRepository`, `StudentUploadRepository`
- [x] Updated `StudentRecordRepository` with `findByLastName...` and `countByStudentIdStartingWith`
- [x] Created 9 DTOs in `dto/student/`: `ParentDto`, `GuardianDto`, `EducationItemDto`, `SchoolYearDto`, `OjtDto`, `TesdaQualDto`, `UploadRefDto`, `StudentDetailsRequest`, `StudentDetailsResponse`
- [x] Created `StorageService` — local disk storage under `./uploads/students/{studentId}/`, validates MIME/size
- [x] Created `StudentDetailsService` — `startOrResume`, `load`, `saveDraft`, `submit`, `saveUpload`
- [x] Created `StudentDetailsController` — 6 REST endpoints under `/api/student/**`
- [x] Updated `StudentRecord.java` — made most fields nullable, added `civilStatus`, `age`; batch/course/section now `optional=true`
- [x] Updated `Parent.java` and `OtherGuardian.java` — made all non-required fields nullable
- [x] Updated `StudentPortalController` — `check-duplicate` now only blocks Submitted/Active (allows Enrolling/Draft as resumable)
- [x] Updated `SecurityConfig.java` — `/api/student/**` added to `permitAll()`
- [x] Updated `application.properties` — added `app.storage.root`, multipart max sizes
- [x] Replaced `student-details.html` placeholder with full 4-step Bootstrap wizard
- [x] Created `js/student-details.js` — full wizard logic (init, step nav, draft save, file upload, submit, populate form)
- [x] Updated `schema.sql` — corrected CREATE TABLE definitions + 5 new student tables + migration section
- [x] Applied live DB migration via docker exec (ALTER TABLE + 5 CREATE TABLEs)
- [x] `./gradlew build -x test` → BUILD SUCCESSFUL

## Bug 1 Fix — student_records.age NOT NULL (Completed — April 30, 2026)
- [x] Identified `student_records.age` as `NOT NULL` with no default in live DB
- [x] Root cause: `startOrResume()` inserts a new record before birthdate is known, so `age = null` — MySQL rejects it
- [x] Applied `ALTER TABLE student_records MODIFY COLUMN age INT NULL` to live MySQL
- [x] Updated `AnihanSRMS.sql` with clarifying comment on the `age` line
- [x] No Java changes needed — `StudentRecord.java` already declared `age` as `Integer` (nullable)
- [x] Verified `IS_NULLABLE: YES` via `information_schema.COLUMNS`
- [x] Enrollment wizard `POST /api/student/start` is now unblocked

## StudentRecord JPA @Id Fix (Completed — April 30, 2026)
- [x] Identified that `StudentRecord.java` incorrectly mapped `@Id` to `student_id` (UNIQUE VARCHAR) instead of `record_id` (INT AUTO_INCREMENT PRIMARY KEY)
- [x] Added `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` on new `recordId` field mapped to `record_id` column
- [x] Demoted `studentId` to `@Column(unique = true, nullable = false, length = 20)` (business key, not JPA identity)
- [x] Updated `StudentRecordRepository` generic type from `String` → `Integer`
- [x] Added `findByStudentId(String)` method to repository for service-layer lookups by business key
- [x] Updated both `findById(studentId)` call sites in `StudentDetailsService` to `findByStudentId(studentId)`
- [x] `./gradlew build -x test` → BUILD SUCCESSFUL

## DB Schema Audit & Migration (Completed — April 30, 2026)
- [x] Compared live MySQL against `AnihanSRMS.sql` — identified 6 discrepancy categories
- [x] Created 5 missing student tables: `student_education`, `student_school_years`, `student_ojt`, `student_tesda_qualifications`, `student_uploads`
- [x] Added missing `civil_status` column to `student_records`
- [x] Relaxed 11 NOT NULL → NULL on `student_records`
- [x] Relaxed 9 NOT NULL → NULL on `parents`
- [x] Relaxed 6 NOT NULL → NULL on `other_guardians`
- [x] Added UNIQUE index on `users.username`
- [x] Legacy orphan tables left intact (no JPA entities reference them)

## Remainder Requirements / Roadmap
- [/] Create `StudentUser` Enrollment Portal logic (welcome page done, details page next)
- [ ] Implement `updateGrade()` logic for Trainers 
- [ ] Implement BLOB Database encoding via `encodeStudentDocsPerBatch()`
- [ ] Implement rigorous Unit Testing as required by Agile sprints
- [ ] Prepare User Acceptance Testing tools to execute the Time and Motion Study
- [ ] Build KPI Evaluation Dashboards (Processing efficiency, SO prep time, data accuracy) against ISO/IEC 25010 standards

## Validation Metrics (From Non-Functional Requirements)
- Document Upload Response: < 3 seconds (< 5MB files)
- Student Record Retrieval: < 5 seconds
- Concurrent User Limit testing: 50+ users
- Hardware RAM footprint: < 6GB RAM required on runtime environment
