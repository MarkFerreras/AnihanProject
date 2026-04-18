# Decisions - Anihan SRMS

## 2026-04-18 - Reuse Existing Date Filter Contract for Export

**Decision**: Keep the export API on `GET /api/logs/export` aligned with the existing `rangeDays`, `startDate`, and `endDate` query parameters only.

**Alternatives considered**:
1. Add another custom-range workflow in the UI for the same export use case - more controls without adding capability
2. Keep one custom date filter path and reuse it for both table loading and export

**Why chosen**: Option 2 keeps the backend contract and frontend UX smaller. The exact-date filter already covers the export use case, so removing extra controls reduces clutter without reducing functionality.

## 2026-04-18 - Server-Side Export Generation for CSV, XLSX, and DOCX

**Decision**: Generate all log exports on the server and return them from `GET /api/logs/export` as attachment downloads.

**Alternatives considered**:
1. Build exports in the browser from DataTables rows - simpler for CSV only, but unreliable for true XLSX/DOCX generation and tied to client-side pagination/search state
2. Create a separate export screen - more UI overhead for a workflow that belongs on the existing logs page
3. Generate files server-side from the selected filter - consistent output, real XLSX/DOCX support, ignores client-side table state

**Why chosen**: Option 3 gives the admin real downloadable files, keeps export independent from DataTables paging/search, and preserves the source-of-truth filter on the backend.

## 2026-04-18 - System Logs Filter Precedence and Default

**Decision**: When `GET /api/logs` is called, apply this filter precedence:
1. If both `startDate` and `endDate` are present → use inclusive custom range (ignore `rangeDays`)
2. Else if `rangeDays` is present → use rolling window ending at current time
3. Else → default to 7 days

Default to the last 7 days instead of fetching all logs. No "all logs" mode is exposed.

**Alternatives considered**:
1. Default to all logs (current behavior) — simple but becomes expensive as the table grows
2. Default to 7 days with an "All" option — adds risk of slow queries on large datasets
3. Default to 7 days, no "All" option — safe, performant, sufficient for typical admin workflows

**Why chosen**: Option 3. The `system_logs` table grows indefinitely with every login, logout, and admin action. Defaulting to a bounded window prevents page load degradation. Preset options (7/14/30 days) cover most audit review scenarios. Custom date ranges handle edge cases. No "all logs" mode avoids accidental full-table scans.

## 2026-04-14 - Separate system_logs Table (No FK to users)

**Decision**: Create a standalone `system_logs` table with `user_id INT NULL` and no foreign key to the `users` table, despite the existing `log` table having an FK.

**Alternatives considered**:
1. Use the existing `log` table — has FK to `users`, limited columns (only `event`, `user_id`, `log_time`, `log_date`)
2. New `system_logs` table with FK to `users.user_id` — normalized but breaks when users are hard-deleted
3. New `system_logs` table with nullable `user_id`, no FK — logs survive hard deletes

**Why chosen**: Option 3. The admin can permanently delete user accounts (hard delete). If `system_logs` had an FK, those deletes would either cascade (losing audit trail) or be blocked. A nullable `user_id` with no FK preserves the complete audit history regardless of user lifecycle.

## 2026-04-14 - Manual Service Calls Over AOP for System Logging

**Decision**: Inject `SystemLogService` directly into controllers and call `logAction()` explicitly at each integration point, rather than using Spring AOP to auto-intercept controller methods.

**Alternatives considered**:
1. Spring AOP with `@Around` on controller methods — automatic but produces generic log messages like "Called PUT /api/admin/users/3"
2. Manual `systemLogService.logAction()` calls — more code, but produces human-readable messages like "Deactivated account: Mark"

**Why chosen**: Option 2. System logs are read by humans (admins). Clean, descriptive action strings like "Reset password for: registrar" are far more useful than auto-generated AOP messages. The small extra code is worth the readability gain.


## 2026-04-11 - Split Password Policy (Admin vs Self-Service)

**Decision**: Apply strong password requirements (uppercase, lowercase, number, special character, min 8 chars) only to self-service password changes. Admin password resets only require 8-character minimum.

**Alternatives considered**:
1. Same strong rules for both — secure but prevents admins from setting simple temporary passwords for handoffs
2. No strong rules at all — too weak for production
3. Split: strong for self-service, simple for admin reset — gives admins flexibility while users must choose secure passwords

**Why chosen**: Option 3 matches real-world workflow. Admins often set an initial temporary password like `Welcome1!` when onboarding a user, then the user is expected to change it. Forcing full complexity on admin resets adds friction without security benefit (the user changes it immediately).

## 2026-04-11 - Programmatic Password Toggle Injection

**Decision**: Dynamically inject eye-icon toggle buttons on all `input[type="password"]` via JavaScript in `auth-guard.js`, rather than manually adding HTML to each password field.

**Alternatives considered**:
1. Manually add `<button>` toggle HTML to every password field in every dashboard (17 inputs across 4 files) — high maintenance, easy to miss one
2. Programmatic injection via JS — single function, zero HTML changes, automatically covers current and future password fields

**Why chosen**: Option 2 is DRY, zero-maintenance, and guarantees consistent behavior across all pages sharing `auth-guard.js`.

## 2026-04-11 - passwordChangedAt Tracking

**Decision**: Add a `password_changed_at DATETIME NULL` column to track when each user's password was last changed. Display it in the admin User Details modal.

**Alternatives considered**:
1. Don't track — simpler, but admin has no visibility into password age
2. Track in a separate audit table — more normalized but overkill for this use case
3. Track directly on the `users` table — simple, one column, updated by both admin reset and self-service change

**Why chosen**: Option 3 is the simplest approach. A `NULL` value means "never changed" (original seeded password), which is useful information for the admin.

## 2026-04-11 - Soft Delete with Hard Delete Option for User Accounts

**Decision**: Implement user deletion with two tiers — soft delete (sets `enabled = false`) and hard delete (permanent removal from DB). Soft delete is the default action; hard delete requires an extra browser `confirm()` dialog.

**Alternatives considered**:
1. Hard delete only — simpler, but loses audit trail and makes mistakes irreversible
2. Soft delete only — safer, but admin may want to fully purge accounts
3. Both tiers with UI separation — gives admin control while defaulting to the safer option

**Why chosen**: Option 3 provides maximum flexibility. The soft delete preserves data for auditing (log table has FK to `user_id`), while hard delete covers cleanup needs. The extra confirmation on permanent delete reduces accidental data loss.

## 2026-04-11 - Optional Password in Admin Update DTO

**Decision**: Add an optional (nullable) `password` field to `AdminUpdateUserRequest`. When null or blank, the existing password is preserved. When provided, it is BCrypt-hashed and saved.

**Alternatives considered**:
1. Separate endpoint `PUT /api/admin/users/{id}/password` — cleaner API separation, but adds an extra round-trip for a combined save
2. Add password to the existing update DTO — simpler frontend integration, single save operation
3. Force password on every edit — bad UX, admin shouldn't need to know/set the current password

**Why chosen**: Option 2 keeps the edit flow as a single form submission. The `@Size(min = 8)` annotation only fires when the field is non-null, so leaving it blank skips validation and preserves the existing password.

## 2026-04-11 - Spring Security `enabled` Flag for Soft Delete

**Decision**: Use Spring Security's built-in `enabled` parameter in the `UserDetails` constructor to block disabled users from authenticating, rather than adding manual checks in the auth flow.

**Alternatives considered**:
1. Manual check in `CustomUserDetailsService.loadUserByUsername()` — throw `UsernameNotFoundException` for disabled users
2. Use Spring Security's `enabled` flag in the 7-arg `User` constructor

**Why chosen**: Option 2 is idiomatic Spring Security. It automatically throws `DisabledException` with a clear message, and Spring already has infrastructure to handle it. Less custom code, more framework alignment.

## 2026-04-11 - Root Project as Merge Base for Admin Module

**Decision**: Keep the tracked repo root as the source-of-truth app and merge donor features from `main-em/` into root only.

**Alternatives considered**:
1. Promote `main-em/` as the new base - risky because it is untracked, has malformed donor files, and drifts from the newer root schema/auth flow
2. Perform a symmetric merge between both trees - too ambiguous and would leave more reconciliation decisions during implementation

**Why chosen**: The root project already contains the newer auth/session behavior and updated user schema. Using root as the implementation target reduced regression risk and kept the final runnable app in the tracked codebase.

## 2026-04-11 - DTO-Based Admin User API

**Decision**: Return DTOs from `/api/admin/users` and `/api/admin/users/{id}` instead of exposing the JPA `User` entity directly.

**Alternatives considered**:
1. Return `User` entities directly - simpler initially, but risks exposing password hashes and couples the admin API tightly to persistence structure
2. Add a sanitized response DTO - slightly more code, but safer and clearer

**Why chosen**: The admin merge needed user-management endpoints, but the API should never leak password data. DTOs keep the contract explicit and safe while still supporting the merged front end.

## 2026-04-05 - Remove DataSeeder, Use Direct SQL for Accounts (AGILE-142)

**Decision**: Delete `DataSeeder.java` entirely. Manage test accounts directly in the database via SQL.

**Alternatives considered**:
1. Keep DataSeeder with "only seed if missing" logic - unnecessary overhead on every startup
2. Keep DataSeeder with "reset on every restart" - destroys username/password changes made via the UI

**Why chosen**: User confirmed that accounts should be inserted directly in the database. This avoids startup overhead, prevents accidental data reset, and keeps `ddl-auto=none` clean.

## 2026-04-05 - Unique Index on username via ALTER TABLE (AGILE-142)

**Decision**: Run `ALTER TABLE AnihanSRMS.users ADD UNIQUE INDEX idx_username (username);` via MCP instead of using `ddl-auto=update`.

**Alternatives considered**:
1. Temporarily set `ddl-auto=update` to let Hibernate create the index, then revert - risky, could alter other tables
2. Manual SQL via MCP - precise, no side effects

**Why chosen**: Option 2 is safer and keeps the production-safe `ddl-auto=none` setting untouched.

## 2026-04-05 - Dual-Mode Auth Entry Point (AGILE-142)

**Decision**: Use request URI prefix (`/api/`) to distinguish API vs browser requests when handling 401/403.

**Alternatives considered**:
1. Check `Accept` header for `application/json` - unreliable, browsers sometimes send JSON accept
2. Check URI prefix `/api/` - simple, deterministic, matches our routing convention

**Why chosen**: URI-based detection is deterministic and matches the established `/api/**` convention throughout the codebase.

## 2026-03-21 - Login Page Design

**Decision**: Use a card-based centered login form with gradient header/button.

**Alternatives considered**:
1. Full-page background image with overlay - heavier, requires image asset
2. Sidebar layout - less conventional for a login page

**Why chosen**: Card-based design is clean, modern, lightweight, and easy to extend. Works well with Bootstrap 5.3 components.
