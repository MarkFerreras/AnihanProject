# Decisions - Anihan SRMS

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
