# Testing - Anihan SRMS

## Login Page - Visual Checklist
- [x] Header visible at top with "Anihan SRMS" brand
- [x] Login card centered vertically and horizontally
- [x] Username and password fields present and functional
- [x] Login button displays with gradient styling
- [x] Footer visible at bottom with copyright
- [x] Green/white/blue palette applied throughout
- [x] Responsive - card stays centered on mobile
- [x] Hover effects work on button and inputs
- [x] Session check on load redirects already-authenticated users

## Login Security (AGILE-142) - API Tests
- [x] Unauthenticated GET `/admin.html` -> 302 redirect to `/index.html`
- [x] Unauthenticated GET `/registrar.html` -> 302 redirect to `/index.html`
- [x] Unauthenticated GET `/trainer.html` -> 302 redirect to `/index.html`
- [x] Unauthenticated GET `/api/auth/me` -> 401 JSON
- [x] POST `/api/auth/login` (Mark/password123) -> 200 + `ROLE_ADMIN`
- [x] POST `/api/auth/login` (registrar/password123) -> 200 + `ROLE_REGISTRAR`
- [x] POST `/api/auth/login` (trainer/password123) -> 200 + `ROLE_TRAINER`
- [x] Authenticated admin GET `/admin.html` -> 200
- [x] Authenticated admin GET `/registrar.html` -> 302 (wrong role blocked)
- [x] Authenticated registrar GET `/registrar.html` -> 200

## Account Management (AGILE-142) - API Tests
- [x] PUT `/api/account/profile` (valid) -> 200 + username updated
- [x] PUT `/api/account/profile` (wrong password) -> 400 "Current password is incorrect"
- [x] PUT `/api/account/profile` (duplicate username) -> 400 "Username is already taken"
- [x] PUT `/api/account/password` (valid) -> 200 + session invalidated
- [x] Login with new password -> 200 (confirms password persisted)
- [x] Logout -> subsequent dashboard access returns 302

## Admin Merge - Automated Tests
- [x] GET `/api/admin/users` as admin -> 200 with sanitized DTO response (no password field)
- [x] GET `/api/admin/users` as non-admin -> 403
- [x] PUT `/api/admin/users/{id}` with invalid payload -> 400 validation response
- [x] Admin service blocks self-role change
- [x] Admin service returns updated sanitized user response
- [x] `./gradlew test` -> BUILD SUCCESSFUL
- [x] `./gradlew build` -> BUILD SUCCESSFUL

## Account Module - Automated Tests (April 17, 2026)
### AccountServiceTest (10 tests)
- [x] `updateUsername` succeeds with valid input
- [x] `updateUsername` throws when user not found
- [x] `updateUsername` throws when password is incorrect
- [x] `updateUsername` throws when new username is same as current
- [x] `updateUsername` throws when username is already taken
- [x] `updatePassword` succeeds with valid input
- [x] `updatePassword` throws when user not found
- [x] `updatePassword` throws when current password is incorrect
- [x] `updatePassword` throws when new passwords do not match
- [x] `updatePassword` throws when new password same as current
- [x] `updatePersonalDetails` succeeds with valid input
- [x] `updatePersonalDetails` throws when user not found

### AccountControllerWebMvcTest (7 tests)
- [x] PUT `/api/account/profile` (valid) -> 200 + username updated
- [x] PUT `/api/account/profile` (blank username) -> 400 validation error
- [x] PUT `/api/account/profile` (wrong password) -> 400 "Current password is incorrect"
- [x] PUT `/api/account/profile` (unauthenticated) -> 401
- [x] PUT `/api/account/password` (valid) -> 200 + session invalidated
- [x] PUT `/api/account/password` (weak password) -> 400 validation error
- [x] PUT `/api/account/password` (unauthenticated) -> 401
- [x] PUT `/api/account/details` (valid) -> 200 + details returned
- [x] PUT `/api/account/details` (unauthenticated) -> 401

## SystemLog Module - Automated Tests (April 17, 2026)
### SystemLogServiceTest (4 tests)
- [x] `logAction` saves system log with all fields
- [x] `logAction` handles null userId gracefully
- [x] `getAllLogs` returns mapped SystemLogResponse list
- [x] `getAllLogs` returns empty list when no logs exist

### SystemLogControllerWebMvcTest (5 tests)
- [x] GET `/api/logs` as ADMIN -> 200 with log entries
- [x] GET `/api/logs` as ADMIN (empty) -> 200 with empty array
- [x] GET `/api/logs` as REGISTRAR -> 403
- [x] GET `/api/logs` as TRAINER -> 403
- [x] GET `/api/logs` (unauthenticated) -> 401

## Commit-Safety Recheck
- [x] `git diff --check` -> no tracked whitespace or conflict-marker errors
- [x] `rg -n "^(<<<<<<<|=======|>>>>>>>)" -S .` -> no unresolved merge markers in the workspace
- [x] `./gradlew test` -> BUILD SUCCESSFUL after the conflict cleanup pass
- [x] `./gradlew build` -> BUILD SUCCESSFUL after the conflict cleanup pass

## Build Repair - Admin Controller
- [x] Restored `AdminController` to constructor-injected `AdminService` usage
- [x] Reinstated validated `AdminUpdateUserRequest` handling on `PUT /api/admin/users/{id}`
- [x] Reinstated sanitized `AdminUserResponse` payloads on admin user endpoints
- [x] `./gradlew test` -> BUILD SUCCESSFUL after the controller repair
- [x] `./gradlew build` -> BUILD SUCCESSFUL after the controller repair

## Admin Front-End Repair
- [x] Rebuilt `admin.html` so it now has one valid document structure and matches `admin-users.js`
- [x] Rebuilt `edit-user.html` so it matches `admin-edit-user.js`
- [x] Confirmed required static assets exist for the repaired admin flow (`dashboard.css`, `auth-guard.js`, `admin-users.js`, `admin-edit-user.js`, `datatables.min.js`, `jquery-4.0.0.min.js`)
- [x] `./gradlew build` -> BUILD SUCCESSFUL after the front-end shell rebuild
- [ ] Browser retest: confirm admin login opens the rebuilt dashboard instead of a blank page
- [ ] Browser retest: confirm the user detail modal and edit-user flow work end-to-end

## Dashboard UI - Visual Checklist
- [ ] Account icon visible top-right on admin dashboard
- [ ] Account icon visible top-right on registrar dashboard
- [ ] Account icon visible top-right on trainer dashboard
- [ ] Dropdown shows username, role, Edit Account, Log Out
- [ ] Edit Account modal opens with username + password forms
- [ ] Username change via modal works end-to-end
- [ ] Password change via modal triggers redirect to login
- [ ] Modal styling matches green/white/blue theme

## Admin Merge - Manual Visual Checklist
- [ ] Admin dashboard shows merged user table and summary cards cleanly
- [ ] User details modal opens from the admin table
- [ ] Edit User page loads existing user details and saves successfully
- [ ] `student-records.html`, `subjects.html`, and `logs.html` share the same admin shell and theme
- [ ] Merged admin pages render correctly on mobile widths

## Admin Users Table — Bulk Load Tests (April 18, 2026)
Tests verify that the Admin "View All Users" table can handle 100 users simultaneously.
Created in **new standalone files** — no existing test files were modified.

### AdminBulkLoadTest (3 tests) — `service/AdminBulkLoadTest.java`
- [x] `getAllUsersReturnsOneHundredUsers` — mocks 100 `User` entities, verifies `AdminService.getAllUsers()` returns exactly 100 DTOs with correct first/last usernames
- [x] `getAllUsersMapsAllDtoFieldsCorrectly` — spot-checks 5 entries (indices 0, 25, 50, 75, 99) for correct userId, username, email, lastName, firstName, middleName, age, enabled fields
- [x] `getAllUsersCompletesWithinPerformanceBound` — verifies 100-user retrieval completes in < 5 seconds (actual: **0.008s**)

### AdminBulkLoadWebMvcTest (2 tests) — `controller/AdminBulkLoadWebMvcTest.java`
- [x] `getUsersReturnsOneHundredUsersAsJson` — mocks 100 DTOs, performs `GET /api/admin/users`, verifies HTTP 200, JSON array length = 100, correct username fields, password field absent
- [x] `getUsersOneHundredCompletesWithinPerformanceBound` — same endpoint timed, verifies < 5 seconds (actual: **0.663s**)

### Test Results Summary
| Test Class | Tests | Failures | Skipped | Duration | Success Rate |
|---|---|---|---|---|---|
| AdminBulkLoadTest | 3 | 0 | 0 | 0.008s | 100% |
| AdminBulkLoadWebMvcTest | 2 | 0 | 0 | 0.663s | 100% |
| **Full Suite (42 tests)** | **42** | **0** | **0** | **12.897s** | **100%** |

### Environment
- All 100 test users generated programmatically in a loop — no hard-coded dummy users
- All data exists only in JVM memory via Mockito mocks — no database touched
- No existing test files were modified
- No new dependencies added — uses only JUnit 5, Mockito, Spring Test (already in `build.gradle.kts`)

## SystemLog Date Filtering — Automated Tests (April 18, 2026)
Tests verify the new server-side date filtering for Admin System Logs.

### SystemLogServiceTest (10 tests) — `service/SystemLogServiceTest.java`
- [x] `logActionSavesSystemLog` — unchanged
- [x] `logActionHandlesNullUserId` — unchanged
- [x] `getLogsDefaultsToSevenDays` — no params → 7-day window applied
- [x] `getLogsFourteenDayFilter` — `rangeDays=14` → 14-day window
- [x] `getLogsThirtyDayFilter` — `rangeDays=30` → 30-day window
- [x] `getLogsCustomDateRange` — `startDate + endDate` → inclusive boundaries verified
- [x] `getLogsCustomDateRangeIgnoresRangeDays` — custom range takes precedence over rangeDays
- [x] `getLogsInvalidDateRangeThrows` — startDate > endDate → `IllegalArgumentException`
- [x] `getLogsReturnsEmptyListWhenNoLogsInRange` — valid range, no data → empty list
- [x] `getLogsReturnsMappedResponses` — DTO mapping verified (logId, username, role, action)

### SystemLogControllerWebMvcTest (9 tests) — `controller/SystemLogControllerWebMvcTest.java`
- [x] `getLogsDefaultsToSevenDays` — `GET /api/logs` (no params) → 200 with filtered data
- [x] `getLogsWithRangeDays14` — `GET /api/logs?rangeDays=14` → 200
- [x] `getLogsWithRangeDays30` — `GET /api/logs?rangeDays=30` → 200
- [x] `getLogsWithCustomDateRange` — `GET /api/logs?startDate=2026-04-01&endDate=2026-04-18` → 200
- [x] `getLogsInvalidRangeReturns400` — invalid range → 400
- [x] `getLogsReturnsEmptyListWhenNoLogs` — no data → 200 with empty array
- [x] `getLogsRejectNonAdminUser` — REGISTRAR → 403 (unchanged)
- [x] `getLogsRejectTrainerUser` — TRAINER → 403 (unchanged)
- [x] `getLogsRejectUnauthenticatedUser` — unauthenticated → 401 (unchanged)

### Test Results Summary
| Test Class | Tests | Failures | Skipped | Success Rate |
|---|---|---|---|---|
| SystemLogServiceTest | 10 | 0 | 0 | 100% |
| SystemLogControllerWebMvcTest | 9 | 0 | 0 | 100% |
| **Full Suite** | **52** | **0** | **0** | **100%** |

## SystemLog Export - Automated Tests (April 18, 2026)
Tests verify backend export generation and the new download endpoint.

### SystemLogExportServiceTest (3 tests) - `service/SystemLogExportServiceTest.java`
- [x] `exportCsvIncludesSummaryAndHeaders` - verifies summary rows, table headers, filename, and CSV escaping for action text
- [x] `exportXlsxIncludesSummaryAndTableData` - verifies workbook title, selected range summary, headers, and mapped row values
- [x] `exportDocxIncludesSummaryAndTableData` - verifies document title, selected range paragraph, table headers, and mapped row values

### SystemLogControllerWebMvcTest (17 tests total) - `controller/SystemLogControllerWebMvcTest.java`
- [x] Existing `GET /api/logs` filter tests remain green
- [x] `exportLogsDefaultsToSevenDaysCsv` - no filter params + `format=csv` -> 200 with attachment response
- [x] `exportLogsWithRangeDays14AsXlsx` - preset range export -> 200 with XLSX content type
- [x] `exportLogsWithCustomDateRangeAsDocx` - custom inclusive date range export -> 200 with DOCX content type
- [x] `exportLogsInvalidFormatReturns400` - unsupported format -> 400
- [x] `exportLogsInvalidRangeReturns400` - invalid date range -> 400
- [x] `exportLogsRejectNonAdminUser` - REGISTRAR -> 403
- [x] `exportLogsRejectTrainerUser` - TRAINER -> 403
- [x] `exportLogsRejectUnauthenticatedUser` - unauthenticated -> 401

### Full Suite Verification
- [x] `./gradlew test` -> BUILD SUCCESSFUL
- [x] 63 tests, 0 failures, 0 skipped
- [x] `git diff --check` -> no tracked whitespace or conflict-marker errors

## Age Auto-Calculation from Birthdate (April 19, 2026)

### Changes to Existing Tests
The following tests were updated to remove `age` from DTO constructors/payloads and use `AgeCalculator` for assertions:

#### AdminServiceTest.java
- [x] `updateUserBlocksSelfRoleChange` — removed `age` param from `AdminUpdateUserRequest` constructor (8 args instead of 9)
- [x] `updateUserReturnsSanitizedResponse` — removed `age` param; age assertion now uses `AgeCalculator.calculateAge()`

#### AccountServiceTest.java
- [x] `updatePersonalDetailsSucceeds` — removed `age` param from `UpdatePersonalDetailsRequest` (4 args instead of 5); age assertion uses `AgeCalculator.calculateAge()`
- [x] `updatePersonalDetailsThrowsWhenUserNotFound` — removed `age` param from constructor

#### AdminControllerWebMvcTest.java
- [x] `updateUserRequiresValidPayload` — removed `"age": 0` from invalid JSON payload

#### AccountControllerWebMvcTest.java
- [x] `updateDetailsSucceeds` — removed `"age": 25` from JSON payload; age assertion changed to `.isNumber()` (dynamic calculation); `updatedUser` setup uses `AgeCalculator`

### Key Verification Points
- [x] `AdminCreateUserRequest` rejects null birthdate (`@NotNull`)
- [x] `AdminUpdateUserRequest` no longer has an `age` field
- [x] `UpdatePersonalDetailsRequest` no longer has an `age` field
- [x] Age is correctly calculated from birthdate in service-layer tests
- [x] All 63 tests pass with 0 failures after the refactor

### Full Suite Verification
- [x] `./gradlew test` → BUILD SUCCESSFUL
- [x] 73 tests, 0 failures, 0 skipped

## Age Calculation & DB Persistence Tests (April 19, 2026 — Round 2)

### New Test File: AgeCalculatorTest.java
Pure unit tests for the `AgeCalculator.calculateAge()` utility:
- [x] `calculateAgeReturnsZeroForNullBirthdate` — null → 0
- [x] `calculateAgeReturnsZeroForTodaysBirthdate` — birthdate = today → 0
- [x] `calculateAgeReturnsCorrectYearsForKnownDate` — fixed past date → correct years (range assertion)
- [x] `calculateAgeReturnsNegativeForFutureBirthdate` — future date → negative
- [x] `calculateAgeHandlesBirthdayNotYetReachedThisYear` — birthday next month, 20 years ago → 19
- [x] `calculateAgeHandlesBirthdayAlreadyPassedThisYear` — birthday last month, 20 years ago → 20

### New Tests: AdminServiceTest.java
- [x] `getUserByIdRecalculatesAgeFromBirthdate` — stale age=99 → recalculated from birthdate → save verified
- [x] `getUserByIdSkipsRecalculationWhenBirthdateIsNull` — null birthdate → age preserved → no save called

### New Tests: AccountServiceTest.java
- [x] `updatePersonalDetailsRecalculatesAgeFromNewBirthdate` — new birthdate provided → age calculated from it
- [x] `updatePersonalDetailsPreservesExistingBirthdateWhenRequestBirthdateIsNull` — null birthdate in request → existing birthdate preserved → age recalculated from existing

### Full Suite Verification
- [x] `./gradlew test` → BUILD SUCCESSFUL
- [x] 73 tests, 0 failures, 0 skipped

