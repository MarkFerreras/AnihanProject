# Active Context - Anihan SRMS

## Current Phase
**Age Auto-Calculation from Birthdate**

## Active Branch
`test-user-table`

## Status (April 19, 2026)

### Age Auto-Calculation Refactor (April 19, 2026)
Transitioned age from a manually-entered field to a computed value derived from birthdate:
- Created `AgeCalculator.java` utility using `java.time.Period`
- Removed `age` field from `AdminUpdateUserRequest`, `AdminCreateUserRequest`, and `UpdatePersonalDetailsRequest` DTOs
- Made `birthdate` mandatory (`@NotNull`) for user creation; removed default value `2000-01-01`
- `AdminService.createUser()` and `updateUser()` auto-calculate age from birthdate
- `AdminService.getUserById()` silently recalculates and persists age on every view (NOT logged)
- `AuthController.currentUser()` (`GET /api/auth/me`) silently recalculates and persists age (NOT logged)
- `AccountService.updatePersonalDetails()` auto-calculates age from birthdate
- Admin user table (`GET /api/admin/users`) does NOT recalculate — returns stored age to avoid bulk writes
- Replaced age `<input>` with read-only `<p id="ageDisplay">` in: `admin.html`, `edit-user.html`, `add-user.html`, `registrar.html`, `trainer.html`, `logs.html`
- Removed `age` from JavaScript payloads in `admin-edit-user.js`, `admin-add-user.js`, `auth-guard.js`
- Added client-side birthdate validation in `admin-add-user.js`
- Updated all test files: `AdminServiceTest`, `AccountServiceTest`, `AdminControllerWebMvcTest`, `AccountControllerWebMvcTest`

**Results:** `./gradlew test` → BUILD SUCCESSFUL (63 tests, 0 failures, 0 skipped).

> **Note — Intentionally NOT updated:**
> `student-records.html` and `subjects.html` still have the old editable age input in their Edit Account modals.
> These will be updated in a future student-focused session.

### Previous Sessions
- System Logs Export UI Cleanup (April 18, 2026) - CSV/XLSX/DOCX export via `/api/logs/export`
- Admin Bulk Load Tests (April 18, 2026) - 5 new standalone tests verifying 100-user table handling
- Database Migration Fix (April 17, 2026) - applied missing schema changes to Docker MySQL
- Admin Navbar Cleanup (April 17, 2026) - removed "Student Records" and "Subjects" nav links

## Verified
- `./gradlew test` → BUILD SUCCESSFUL (63 tests, all green)
- Age field removed from all edit/create forms (except student pages)
- Age displayed as read-only text in self-service modals
- Birthdate is mandatory for user creation
- Age recalculated silently on user detail views without system log entries

