# Progress - Anihan SRMS

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

## Remainder Requirements / Roadmap
- [ ] Create `StudentUser` Enrollment Portal logic
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
