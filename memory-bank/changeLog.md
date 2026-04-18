# Change Log - Anihan SRMS
## 2026-04-18 - System Logs Date Filtering Enhancement
**Branch:** `feature/logs-date-filter`

### Files Modified
| File | Change |
|---|---|
| `repository/SystemLogRepository.java` | Added `findByTimestampBetweenOrderByTimestampDesc()` range query method |
| `service/SystemLogService.java` | Replaced `getAllLogs()` with `getLogs(rangeDays, startDate, endDate)` implementing filter precedence: custom range > preset days > default 7 days |
| `controller/SystemLogController.java` | Added optional `rangeDays`, `startDate`, `endDate` query params with `@DateTimeFormat`; catches `IllegalArgumentException` → 400 |
| `static/logs.html` | Added date filter toolbar (preset 7/14/30-day pills + custom From/To date inputs + Apply/Reset buttons) with matching CSS |
| `static/js/system-logs.js` | Refactored `loadLogs()` to accept filter params and build query URL; added preset, apply, and reset event handlers; default load is 7 days |
| `test/service/SystemLogServiceTest.java` | Rewrote: 10 tests (2 logAction unchanged + 8 new getLogs tests covering default, 14d, 30d, custom range, precedence, invalid range, empty result, DTO mapping) |
| `test/controller/SystemLogControllerWebMvcTest.java` | Rewrote: 9 tests (6 new filter tests + 3 unchanged security tests) |
| `memory-bank/activeContext.md` | Updated phase, branch, status, and verified section |
| `memory-bank/progress.md` | Added "System Logs Date Filtering" completed section |
| `memory-bank/testing.md` | Added date filtering test results section |
| `memory-bank/changeLog.md` | This entry |
| `memory-bank/decisions.md` | Added filter precedence decision record |

### Verification
- `./gradlew test` → BUILD SUCCESSFUL (52 tests, 0 failures, 19.998s)
- No references to removed `getAllLogs()` method remain
- Existing security tests (403/401) pass unchanged

---

## 2026-04-18 - Admin Bulk Load Tests (100 Users)
**Branch:** `feature/unit-tests-coverage`

### Files Created
| File | Purpose |
|---|---|
| `test/service/AdminBulkLoadTest.java` | 3 unit tests verifying `AdminService.getAllUsers()` handles 100 users (count, DTO mapping, performance) |
| `test/controller/AdminBulkLoadWebMvcTest.java` | 2 WebMvc tests verifying `GET /api/admin/users` serializes 100 users as JSON with correct content and timing |

### Files Modified
| File | Change |
|---|---|
| `memory-bank/testing.md` | Added "Admin Users Table — Bulk Load Tests" section with detailed results |
| `memory-bank/progress.md` | Added "Admin Bulk Load Tests" completed section |
| `memory-bank/activeContext.md` | Updated current phase and status for April 18 session |

### Not Modified (Intentional)
| File | Reason |
|---|---|
| `test/service/AdminServiceTest.java` | Existing tests preserved — bulk tests placed in a separate file |
| `test/controller/AdminControllerWebMvcTest.java` | Existing tests preserved — bulk tests placed in a separate file |
| `build.gradle.kts` | No new dependencies needed — bulk tests use only existing JUnit 5, Mockito, Spring Test |

### Verification
- `./gradlew test` → BUILD SUCCESSFUL (42 tests, 0 failures, 12.897s total)
- AdminBulkLoadTest: 3/3 passed in 0.008s
- AdminBulkLoadWebMvcTest: 2/2 passed in 0.663s
- Both well within 5-second non-functional requirement

---

## 2026-04-17 - Admin Navbar Cleanup (Student Records & Subjects Removed)
**Branch:** `feature/unit-tests-coverage`

### Files Modified
| File | Change |
|---|---|
| `static/admin.html` | Removed "Student Records" and "Subjects" `<li>` nav items from navbar |
| `static/edit-user.html` | Same — removed "Student Records" and "Subjects" nav items |
| `static/add-user.html` | Same — removed "Student Records" and "Subjects" nav items |
| `static/logs.html` | Same — removed "Student Records" and "Subjects" nav items |
| `memory-bank/systemPatterns.md` | Updated nav links pattern to "Home | Logs", added stale navbar warning |
| `memory-bank/activeContext.md` | Recorded navbar cleanup and stale navbar warning |

### Not Modified (Intentional)
| File | Reason |
|---|---|
| `static/student-records.html` | Page kept for future use — internal navbar NOT updated (still has old 4-link nav) |
| `static/subjects.html` | Page kept for future use — internal navbar NOT updated (still has old 4-link nav) |

### Verification
- Admin navbar now shows: **Home | Logs** across all active admin pages
- No CSS changes needed — nav items are standard Bootstrap `<li>` elements
- `student-records.html` and `subjects.html` remain accessible via direct URL and `SecurityConfig.java` route protection

---

## 2026-04-17 - Database Migration Fix & SQL Cleanup
**Branch:** `feature/unit-tests-coverage`

### Database Migrations Applied (Docker `mysql-server` container)
| Statement | Purpose |
|---|---|
| `ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;` | Adds soft-delete support column expected by `User.java` and `CustomUserDetailsService.java` |
| `ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL;` | Adds password tracking column expected by `User.java` and `AdminUserResponse.java` |
| `CREATE TABLE IF NOT EXISTS system_logs (...)` | Creates audit log table expected by `SystemLogService.java` and `AuthController.java` |

### SQL File Cleanup — `AnihanSRMS.sql`
| Change | Description |
|---|---|
| Removed dangling `ALTER TABLE users ADD COLUMN ...` block (lines 40–49) | Leftover merge-conflict artifact that would error on fresh import |
| Removed bare `main` text (line 53) | Git merge conflict remnant |
| Removed stray closing `)` near EOF | Syntax error from merge conflict |
| Commented out inline `ALTER TABLE student_records` | Converted to comment to prevent accidental execution |

### Root Cause
The Docker MySQL database had been recreated or reverted since the last session where these migrations were applied. The seeded accounts existed but the schema was outdated, causing Hibernate SQL errors on login.

### Verification
- `DESCRIBE AnihanSRMS.users` → 12 columns including `enabled` and `password_changed_at`
- `DESCRIBE AnihanSRMS.system_logs` → 7 columns with timestamp index
- All 3 existing accounts (`Ado`, `registrar`, `trainer`) preserved with `enabled=1`

---

## 2026-04-17 - Unit Test Coverage Expansion
**Branch:** `feature/unit-tests-coverage`

### Files Created
| File | Purpose |
|---|---|
| `test/service/AccountServiceTest.java` | 10 unit tests for `updateUsername`, `updatePassword`, `updatePersonalDetails` |
| `test/service/SystemLogServiceTest.java` | 4 unit tests for `logAction`, `getAllLogs` |
| `test/controller/AccountControllerWebMvcTest.java` | 7 WebMvc tests for `/api/account/**` endpoints |
| `test/controller/SystemLogControllerWebMvcTest.java` | 5 WebMvc tests for `/api/logs` endpoint |

### Verification
- `./gradlew test` → BUILD SUCCESSFUL (all tests green)
- Total test coverage now spans: Admin, Account, SystemLog modules

---

## 2026-04-16 - Navbar Logo UI Standardization
**Branch:** `ui-style/fix`

### Files Modified
| File | Change |
|---|---|
| `static/admin.html` | Removed `.brand-title` span ("Anihan SRMS") from navbar |
| `static/add-user.html` | Removed `.brand-title` span from navbar |
| `static/edit-user.html` | Removed `.brand-title` span from navbar |
| `static/logs.html` | Removed `.brand-title` span from navbar, removed inline `max-height` restriction from logo |
| `static/student-records.html` | Removed `.brand-title` span from navbar |
| `static/subjects.html` | Removed `.brand-title` span from navbar |
| `static/css/login.css` | Enlarged `.navbar-logo` height to `85px` and applied `-22px` vertical margins to keep navbar size intact |
| `static/css/dashboard.css` | Removed unused `.brand-title` CSS rules |

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (html/css changes verified)
- Checked layouts for uniformity across all referenced HTML files.

---

## 2026-04-14 - Account Icon Dropdown on All Admin Pages
**Branch:** `feature/admin-system-logs`

### Files Modified
| File | Change |
|---|---|
| `static/student-records.html` | Added brand-mark navbar, account dropdown, Edit Account modal, `dashboard.css`, `auth-guard.js`, `data-required-role` |
| `static/subjects.html` | Same as above |
| `static/logs.html` | Same as above (already had `auth-guard.js` but lacked account dropdown/modal) |
| `memory-bank/systemPatterns.md` | Added "Admin Page Template Pattern" section as mandatory standard for future pages |

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-14 - Admin System Logs
**Branch:** `feature/admin-system-logs`

### Files Created
| File | Purpose |
|---|---|
| `model/SystemLog.java` | JPA entity for `system_logs` table |
| `repository/SystemLogRepository.java` | Data access with `findAllByOrderByTimestampDesc()` |
| `service/SystemLogService.java` | `logAction()` and `getAllLogs()` business logic |
| `dto/SystemLogResponse.java` | API response DTO record with `from()` factory |
| `controller/SystemLogController.java` | `GET /api/logs` REST endpoint (ADMIN-only) |
| `static/js/system-logs.js` | DataTables 2 AJAX fetch and table rendering |

### Files Modified
| File | Change |
|---|---|
| `config/SecurityConfig.java` | Added `/api/logs/**` ADMIN role matcher |
| `controller/AuthController.java` | Injected `SystemLogService`, logs login/logout (captures identity before session cleanup) |
| `controller/AdminController.java` | Injected `SystemLogService` + `UserRepository`, logs update/delete/re-enable actions with `LogContext` helper |
| `controller/AccountController.java` | Injected `SystemLogService` + `UserRepository`, logs self-service password/username/details changes |
| `static/logs.html` | Rebuilt with unified color scheme (`dashboard.css`), DataTables 2, `auth-guard.js`, loading spinner |
| `src/main/sql/AnihanSRMS.sql` | Added `system_logs` table with timestamp index |
| `test/controller/AdminControllerWebMvcTest.java` | Added `@MockitoBean` for `SystemLogService` and `UserRepository` |

### Database Migration Required
```sql
CREATE TABLE system_logs (
    log_id      INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id     INT NULL,
    username    VARCHAR(255) NOT NULL,
    role        VARCHAR(15) NOT NULL,
    action      VARCHAR(500) NOT NULL,
    ip_address  VARCHAR(45) NULL,
    timestamp   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_system_logs_timestamp (timestamp DESC)
);
```

### Tracked Actions
- Login / Logout
- Admin: Update user details, Reset password, Deactivate account, Permanently delete account, Re-enable account
- Self-service: Change own password, Change own username, Update own personal details

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-11 - Admin Username Edit & Button Hover Fixes
**Branch:** `feature/admin-password-delete-hover-fix`

### Files Modified
| File | Change |
|---|---|
| `css/dashboard.css` | Fixed hover selectors for `.btn-reenable`, `.btn-danger-surface`, `.btn-deactivate`, `.btn-permanent-delete` (added `.btn.` prefix for Bootstrap specificity) |
| `dto/AdminUpdateUserRequest.java` | Added optional `username` field with `@Pattern(^\S+$)` no-spaces validation |
| `service/AdminService.java` | Added duplicate-username check in `updateUser()` |
| `edit-user.html` | Made username field editable (removed `disabled`), added `pattern` + help text, updated contract note |
| `js/admin-edit-user.js` | Added `username` to payload, added client-side no-spaces validation |
| `test/service/AdminServiceTest.java` | Updated constructors for new `username` parameter |

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-11 - Re-enable, Password Toggle, Strong Validation & Timestamp
**Branch:** `feature/admin-password-delete-hover-fix`

### Files Modified
| File | Change |
|---|---|
| `model/User.java` | Added `passwordChangedAt` (LocalDateTime) field + getter/setter |
| `dto/AdminUserResponse.java` | Added `LocalDateTime passwordChangedAt` field + updated `from()` |
| `dto/UpdatePasswordRequest.java` | Added `@Pattern` regex for strong password enforcement |
| `service/AdminService.java` | Sets `passwordChangedAt` on admin password reset; added `reEnableUser()` method |
| `service/AccountService.java` | Sets `passwordChangedAt` on self-service password change |
| `controller/AdminController.java` | Added `PUT /api/admin/users/{id}/enable` endpoint |
| `static/js/auth-guard.js` | Added `isStrongPassword()` validation, `setupPasswordToggles()` function (eye icon SVG auto-injection), modal reset logic for toggle state |
| `static/js/admin-users.js` | Added `passwordChangedAt` display, re-enable button toggle logic, `setupReEnableHandler()` function |
| `static/admin.html` | Added "Password Last Changed" detail card, "Re-enable Account" button, updated password help text |
| `static/edit-user.html` | Updated self-service password help text for strong requirements |
| `static/registrar.html` | Added `minlength="8"` to all password inputs, added strong password help text |
| `static/trainer.html` | Added `minlength="8"` to all password inputs, added strong password help text |
| `static/css/dashboard.css` | Added `.btn-reenable` and `.password-toggle-btn` styles |
| `src/main/sql/AnihanSRMS.sql` | Added `password_changed_at DATETIME NULL` column |
| `test/controller/AdminControllerWebMvcTest.java` | Updated constructor with `null` for passwordChangedAt |

### Database Migration
```sql
ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL;
```

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-11 - Admin Password Reset, Delete Account & Button Hover Fix
**Branch:** `feature/admin-password-delete-hover-fix`

### Files Modified
| File | Change |
|---|---|
| `model/User.java` | Added `enabled` field (Boolean, default true) with JPA column mapping + getter/setter |
| `dto/AdminUpdateUserRequest.java` | Added optional `password` field with `@Size(min=8)` validation |
| `dto/UpdatePasswordRequest.java` | Added `@Size(min=8)` to `newPassword` and `confirmNewPassword` |
| `dto/AdminUserResponse.java` | Added `Boolean enabled` field and updated `from()` factory |
| `service/AdminService.java` | Injected `PasswordEncoder`, added password hashing in `updateUser()`, added `softDeleteUser()` and `hardDeleteUser()` with self-deletion prevention |
| `controller/AdminController.java` | Added `DELETE /api/admin/users/{id}` (soft) and `DELETE /api/admin/users/{id}/permanent` (hard) |
| `service/CustomUserDetailsService.java` | Uses Spring Security 7-arg `User` constructor to block disabled users via `enabled` flag |
| `static/admin.html` | Added "Delete User" button in details modal, added delete confirmation modal with soft/hard options, added Status column header, added `minlength="8"` to password fields |
| `static/edit-user.html` | Added "New Password" input field with `minlength="8"`, added `minlength="8"` to self-service password fields |
| `static/js/admin-users.js` | Rewrote: added delete flow, status badge rendering, self-delete prevention (hides button), delete confirmation modal handlers |
| `static/js/admin-edit-user.js` | Updated `buildPayload()` to include optional password, added client-side 8-char validation |
| `static/js/auth-guard.js` | Added 8-char minimum validation in `setupPasswordChange()` |
| `static/css/dashboard.css` | Fixed `.btn-surface:hover`/`:focus` to explicitly set green background; added `.btn-danger-surface`, `.status-badge`, `.delete-confirm-modal`, `.btn-deactivate`, `.btn-permanent-delete` styles |
| `src/main/sql/AnihanSRMS.sql` | Added `enabled TINYINT(1) NOT NULL DEFAULT 1` column to users table |
| `test/service/AdminServiceTest.java` | Added `PasswordEncoder` mock, updated constructor calls with null password, set `enabled=true` in test builder |
| `test/controller/AdminControllerWebMvcTest.java` | Updated `AdminUserResponse` constructor to include `enabled=true` |

### Database Migration Required
```sql
ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;
```

### Verification
- `./gradlew build` → BUILD SUCCESSFUL (7/7 tasks, all tests green)

---

## 2026-04-11 - Admin Dashboard Front-End Repair
**Branch:** `feature/fix-login-security`

### Files Modified
| File | Change |
|---|---|
| `static/admin.html` | Rebuilt the admin dashboard HTML to remove duplicate donor markup, restore a valid document structure, and match the current `admin-users.js` IDs |
| `static/edit-user.html` | Rebuilt the admin edit-user page to match `admin-edit-user.js` and the shared account modal shell |
| `memory-bank/activeContext.md` | Recorded the front-end regression cause and current browser retest task |
| `memory-bank/progress.md` | Added the admin shell and edit-flow rebuild work to the UI progress notes |
| `memory-bank/testing.md` | Recorded the post-repair verification steps for the admin front-end fix |

### Root Cause
- `admin.html` had regressed into a partially merged donor version with overlapping navbars, misplaced closing tags, inline scripts duplicated in the page body, and mismatched IDs compared to `admin-users.js`.
- `edit-user.html` had also drifted back to older markup that no longer matched the current `admin-edit-user.js` field names.
- That combination could leave the admin experience blank or broken immediately after login and during the follow-up edit flow.

### Verification
- `./gradlew build` -> BUILD SUCCESSFUL
- `git diff --check` -> no tracked whitespace errors after the HTML rebuild
- Structural spot-check confirmed single `head`, `body`, and `html` blocks in both repaired pages

## 2026-04-11 - Build Repair for Admin Controller Regression
**Branch:** `feature/fix-login-security`

### Files Modified
| File | Change |
|---|---|
| `controller/AdminController.java` | Replaced the regressed repository-backed controller with the service/DTO-based admin API expected by the current tests |
| `memory-bank/activeContext.md` | Updated active task and recorded the build-failure root cause |
| `memory-bank/progress.md` | Recorded the controller regression fix in the admin merge progress notes |
| `memory-bank/testing.md` | Added fresh automated verification results for the controller repair |

### Root Cause
- `AdminController` had drifted back to an older `UserRepository` implementation.
- The current `AdminControllerWebMvcTest` slice expects `AdminService` injection, validated `AdminUpdateUserRequest`, and sanitized `AdminUserResponse` DTO output.
- That mismatch caused the Spring test context to fail before the controller tests could run.

### Verification
- `./gradlew test` -> BUILD SUCCESSFUL
- `./gradlew build` -> BUILD SUCCESSFUL
- `git diff --check` -> no tracked whitespace errors after the controller repair

## 2026-04-11 - Conflict Cleanup and Commit-Safety Recheck
**Branch:** `feature/fix-login-security`

### Files Modified
| File | Change |
|---|---|
| `memory-bank/projectbrief.md` | Removed unresolved merge markers and rewrote current-status note |
| `memory-bank/activeContext.md` | Replaced stale donor-status notes with current repaired branch state |
| `memory-bank/progress.md` | Recorded commit-safety cleanup and fresh verification results |
| `config/SecurityConfig.java` | Restored clean root RBAC configuration for merged admin routes |
| `controller/AdminController.java` | Restored DTO/service-based admin API contract |
| `static/admin.html` | Removed partially merged donor markup and restored clean root dashboard shell |
| `static/edit-user.html` | Restored root edit-user flow and shared account modal integration |
| `static/student-records.html` | Restored root placeholder shell with proper admin session guard |
| `static/subjects.html` | Restored root placeholder shell with proper admin session guard |
| `static/logs.html` | Restored root placeholder shell with proper admin session guard |
| `src/main/sql/AnihanSRMS.sql` | Removed donor corruption and restored root-aligned schema ordering |

### Verification
- `./gradlew test` -> BUILD SUCCESSFUL
- `./gradlew build` -> BUILD SUCCESSFUL
- `git diff --check` -> no conflict markers or whitespace errors in tracked files
- `rg -n "^(<<<<<<<|=======|>>>>>>>)" -S .` -> no matches in tracked project files

## 2026-04-11 - Root/Admin Merge from `main-em`
**Branch:** `feature/fix-login-security`

### Files Created
| File | Purpose |
|---|---|
| `controller/AdminController.java` | Root admin API for listing, viewing, and updating users |
| `service/AdminService.java` | Admin-side user-management business logic |
| `dto/AdminUserResponse.java` | Sanitized admin API response DTO |
| `dto/AdminUpdateUserRequest.java` | Validated admin update request DTO |
| `static/edit-user.html` | Root admin edit-user page |
| `static/student-records.html` | Root admin student-records placeholder page |
| `static/subjects.html` | Root admin subjects placeholder page |
| `static/logs.html` | Root admin logs placeholder page |
| `static/js/admin-users.js` | Admin dashboard DataTables and detail modal logic |
| `static/js/admin-edit-user.js` | Edit-user page fetch/save logic and unsaved-change guard |
| `test/controller/AdminControllerWebMvcTest.java` | Admin controller security and validation coverage |
| `test/service/AdminServiceTest.java` | Admin service self-role-lock and sanitized-response coverage |

### Files Modified
| File | Change |
|---|---|
| `config/SecurityConfig.java` | Added admin-only route protection for merged HTML pages |
| `exception/GlobalExceptionHandler.java` | Added 404 handling for missing admin resources |
| `build.gradle.kts` | Added Spring Boot starter test dependency required by the new admin tests |
| `static/admin.html` | Rebuilt the admin dashboard shell around merged user-management UI |
| `static/css/dashboard.css` | Added shared admin shell, modal, table, placeholder-page, and responsive styles |
| `static/js/auth-guard.js` | Cleaned shared session/account modal logic for merged pages |
| `src/main/sql/AnihanSRMS.sql` | Reordered schema definitions and aligned users/student records keys with the root app |
| `memory-bank/activeContext.md` | Updated current phase/task for the root/admin merge |
| `memory-bank/progress.md` | Recorded merged admin module completion details |

### Merge Notes
- Kept the repo root as the source-of-truth application.
- Treated `main-em/` as a read-only donor/reference only.
- Preserved the newer root user schema: `lastname`, `firstname`, `middlename`, `birthdate`.
- Used DTOs for `/api/admin/users` responses so password hashes are never exposed.
- Preserved self-role-lock protection so an admin cannot demote their own role.

### Verification
- `./gradlew test` -> BUILD SUCCESSFUL
- `./gradlew build` -> BUILD SUCCESSFUL
- `rg -n "^(<<<<<<<|=======|>>>>>>>)" src memory-bank build.gradle.kts` -> no matches

## 2026-04-06 - AGILE-100: G2.1 Edit Personal Details
**Branch:** `feature/fix-login-security`

### Files Created
| File | Purpose |
|---|---|
| `dto/UpdatePersonalDetailsRequest.java` | Personal details update DTO (record) |
| `repository/SubjectRepository.java` | JPA repo for subjects dropdown |
| `repository/SectionRepository.java` | JPA repo for sections dropdown |
| `controller/LookupController.java` | GET /api/lookup/subjects + /sections |

### Files Modified
| File | Change |
|---|---|
| `service/CustomUserDetailsService.java` | Enforced strict case match on username login; fallback to case-insensitive email |
| `model/User.java` | Added fullName, age, dateOfBirth, subjectCode, sectionCode fields |
| `service/AccountService.java` | Added updatePersonalDetails() with role-based field enforcement |
| `controller/AccountController.java` | Added PUT /api/account/details |
| `controller/AuthController.java` | Injected UserRepository, expanded /api/auth/me to include personal details |
| `static/css/dashboard.css` | Added nav-tab styles, form-select, trainer-only-fields, logout notification |
| `static/js/auth-guard.js` | Added personal details form, dropdown loading, trainer field visibility, logout notification redirect |
| `static/admin.html` | Tabbed Edit Account modal (Personal Details + Account Settings) |
| `static/registrar.html` | Same tabbed modal |
| `static/trainer.html` | Same tabbed modal (trainer sees Subject/Section dropdowns) |
| `static/index.html` | Added logout notification banner + JS trigger |

### Database Changes
| Change | Command |
|---|---|
| Added columns to users | `ALTER TABLE users ADD full_name, age, date_of_birth, subject_code (FK), section_code (FK)` |
| Seeded courses | `INSERT INTO courses VALUES ('CARS', 'Culinary Arts and Restaurant Services')` |
| Seeded batches | `INSERT INTO batches VALUES ('B2026A', 2026), ('B2026B', 2026)` |
| Seeded qualifications | NC II - Cookery, NC II - Food and Beverage Services |
| Seeded subjects | COOK101, COOK102, FBS101 |
| Seeded sections | SEC-A, SEC-B, SEC-C |

### Verification
- `./gradlew clean build -x test` -> BUILD SUCCESSFUL
- `./gradlew bootRun` -> Started in ~10s
- PUT `/api/account/details` (trainer) -> 200 + all fields saved
- GET `/api/lookup/subjects` -> 3 subjects returned
- GET `/api/lookup/sections` -> 3 sections returned
- GET `/api/auth/me` -> includes personal details
- Browser: tabbed modal works, dropdowns populated
- Browser: logout notification banner slides in

## 2026-04-05 - AGILE-142: G1.R Fix Login
**Branch:** `feature/fix-login-security`

### Files Created
| File | Purpose |
|---|---|
| `controller/AccountController.java` | PUT /api/account/profile + /api/account/password |
| `service/AccountService.java` | Username/password change business logic |
| `dto/UpdateProfileRequest.java` | Username change DTO with validation |
| `dto/UpdatePasswordRequest.java` | Password change DTO with validation |
| `static/js/auth-guard.js` | Shared session guard + account UI logic |
| `static/css/dashboard.css` | Account dropdown + modal styles |

### Files Modified
| File | Change |
|---|---|
| `config/SecurityConfig.java` | Rewritten: role-specific HTML matchers, dual-mode entry/denied handlers, cache-control |
| `model/User.java` | Added `@Column(unique = true)` on username |
| `exception/GlobalExceptionHandler.java` | Added IllegalArgumentException handler |
| `static/admin.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/registrar.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/trainer.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/index.html` | Added session check to redirect authenticated users |

### Files Deleted
| File | Reason |
|---|---|
| `config/DataSeeder.java` | Removed - accounts managed directly in database per user decision |

### Database Changes
| Change | Command |
|---|---|
| Added unique index on `users.username` | `ALTER TABLE AnihanSRMS.users ADD UNIQUE INDEX idx_username (username);` |

### Verification
- `./gradlew clean build -x test` -> BUILD SUCCESSFUL
- `./gradlew bootRun` -> Started successfully (no errors, no DataSeeder output)
- Unauthenticated `/admin.html` -> 302 redirect to `/index.html`
- API `/api/auth/me` without session -> 401 JSON
- Admin login -> 200 + `admin.html`
- Admin accessing `/registrar.html` -> 302 redirect (wrong role blocked)
- Registrar login -> 200 + `registrar.html`
- Username change -> 200 + session updated
- Password change -> 200 + session invalidated (401 on next request)
- Login with new password -> 200
- Logout -> subsequent access returns 302
- Wrong password on profile update -> 400 "Current password is incorrect"

## 2026-03-24 - Phases 1-4: Backend Auth Setup
**Branch:** `feature/backend-auth-setup`

### Files Created
| File | Purpose |
|---|---|
| `model/User.java` | Users table entity |
| `repository/UserRepository.java` | JPA repo with username/email queries |
| `controller/AuthController.java` | Login/logout/me endpoints |
| `src/main/resources/static/admin.html` | Empty admin template with header/footer |
| `src/main/resources/static/registrar.html` | Empty registrar template with header/footer |
| `src/main/resources/static/trainer.html` | Empty trainer template with header/footer |

## 2026-04-07 — Database Schema Development
| File | Action | Description |
|------|--------|-------------|
| `AnihanSRMS.sql` | Created | Defined complete database schema with 15 tables for student records, enrollment, and results |

## 2026-04-10 — Admin Dashboard UI & Logic (Current)
**Branch:** `feature/admin-dashboard-ui`

### Files Created/Modifed
| File | Change |
|---|---|
| `application.properties` | Added ddl-auto=none, MySQL dialect, session config |
| `index.html` | Added error alert div + login fetch JS |
| `src/main/sql/AnihanSRMS.sql` | Reordered tables to respect foreign key creation order |

### Files Deleted
| File | Reason |
|---|---|
| `src/main/java/controller/` | Empty folder outside Spring Boot package |
| `src/main/java/model/` | Empty folder outside Spring Boot package |
| `src/main/java/repository/` | Empty folder outside Spring Boot package |
| `src/main/java/service/` | Empty folder outside Spring Boot package |
| `docker-compose.yml` | Removed per user request, using existing `mysql-server` container |

### Verification
- Re-injected corrected SQL script into `mysql-server`
- Gradle `clean build -x test` -> BUILD SUCCESSFUL
- `./gradlew bootRun` started successfully
- `Invoke-RestMethod` to `/api/auth/login` returned 200 OK + `ROLE_ADMIN`

## 2026-03-24 - Troubleshooting IDE Syntax Errors
**Branch:** `feature/fix-src-errors`

### Verification
- Tested DataTables rendering users.
- Confirmed Modal successfully fetches via Ajax.
- Confirmed `edit-user.html` correctly displays "Unsaved changes" warnings visually.
- Verified backend rejects self-demoting role assignments.
- Checked user IDE error: `String cannot be resolved to a type` inside `LoginRequest.java`.
- Ran `./gradlew clean compileJava` locally. Confirmed `BUILD SUCCESSFUL`.
- Diagnosed issue as a false-positive caused by IDE Java Language Server (JDTLS) losing connection to the Java 25 JDK.
- Updated `activeContext.md` and `progress.md`.
- Updated `build.gradle.kts` to use Java 25.
- Updated `src/main/resources/static/index.html` to add error alert div + login fetch JS.
- Updated `src/main/resources/static/admin.html` to add header/footer + empty main.
- Updated `src/main/resources/static/registrar.html` to add header/footer + empty main.
- Updated `src/main/resources/static/trainer.html` to add header/footer + empty main.

### 2026-04-10
- Refactored DB schema to drop `full_name` in favor of `lastname`, `firstname`, `middlename`.
- Renamed `date_of_birth` to `birthdate`.
- Removed inline `subject_code` and `section_code` from users table and corresponding Java entities.
- Updated `AnihanSRMS.sql` to remove Git conflict marks and use clean `CREATE TABLE`.
- Updated `User.java`, `UpdatePersonalDetailsRequest.java` for the entity structural changes.
- Updated `AccountService.java`, `AuthController.java`, `AccountController.java` for new personal detail endpoints.
- Updated `auth-guard.js` and admin, trainer, registrar HTML pages to render distinct inputs for names.
- Re-seeded 3 standard test accounts into the new `users` table schema.
