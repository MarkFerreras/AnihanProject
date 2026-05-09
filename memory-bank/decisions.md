# Decisions - Anihan SRMS

Each entry: decision + brief rationale. Older entries (pre-2026-04-26) are summarized to one line; if you need full alternatives-considered for a historical decision, consult `changeLog.md` for the matching session.

---

## 2026-05-09 - Generic 500 Body, Internals via SLF4J Only

**Decision:** `GlobalExceptionHandler`'s catch-all `Exception` handler returns `"An unexpected error occurred. Please contact the administrator."` with HTTP 500. The exception class name, message, and stack trace are written via `log.error("Unhandled exception", ex)` — never serialized to the response body. A new `DataIntegrityViolationException` handler returns HTTP 409 with a generic conflict message.

**Why:** The previous body `"Internal server error: <ExceptionClass> - <message>"` echoed raw SQL queries, table names, and column names to clients (audit caught this on `/api/registrar/classes` returning `select sc1_0.class_id,...`). On an on-premise LAN system handling student PII, that's an information-disclosure risk. Operators should look at server logs, not parse stack traces from the browser console. Pre-checks at the service layer (e.g., `deleteSection` FK guard) are the right place to surface friendly messages; the generic handler is for truly unexpected failures.

## 2026-05-09 - Pre-Check FK References Before Delete (deleteSection)

**Decision:** `ClassManagementService.deleteSection()` calls `classRepository.existsBySectionSectionCode()` before `deleteById()` and throws `IllegalArgumentException` if any class still references the section.

**Why:** Letting MySQL's FK constraint reject the delete works, but the resulting exception now produces a vague generic 409 (after the handler hardening). A pre-check yields a 400 with an actionable message ("Cannot delete section: one or more classes still reference it. Remove those classes first.") which the registrar UI can render directly. The DB constraint stays as defense in depth.

## 2026-05-09 - SchoolClass Entity + Separate ClassManagementController

**Decision:** Use the entity name `SchoolClass` for the `classes` table (avoids clash with `java.lang.Class`). Introduce a separate `ClassManagementController` under `/api/registrar/...` instead of expanding `RegistrarController`.

**Why:** Keeps endpoints organized and limits cross-cutting impact on existing tests. Subject and class both expose a "trainer" — the class-level trainer is authoritative; the subject-level trainer is a default that auto-fills the Create Class modal.

## 2026-05-06 - Delete-All-Then-Insert-New for Registrar Collection Fields (TESDA, SchoolYears)

**Decision:** When the registrar saves OJT, TESDA qualifications, or School Years on the edit form, TESDA and SchoolYears use delete-all-then-insert-new (not diff/merge).

**Why:** Registrar's mental model is "what I see on screen is what gets saved." Collections are small (max 3 TESDA slots, typically <10 SchoolYear rows). Explicit `flush()` after delete prevents Hibernate from buffering DELETE past INSERT in the same `@Transactional`, which would violate the `(student_id, slot)` unique constraint.

## 2026-05-06 - OJT Upsert (Not Delete-All-Insert-New)

**Decision:** OJT uses upsert — find existing row by studentId and update in place; create if none; delete if all fields blank.

**Why:** OJT is 1:1. Upsert preserves the `ojt_id` PK and avoids unnecessary DELETE+INSERT. No unique-constraint concern.

## 2026-05-05 - Application Data Lives in the Database, Not in `DataSeeder`

**Decision:** Delete `DataSeeder.java`. Use `schema.sql` for one-shot fresh-install seeding (lookup data + 3 user accounts + sample students); rely on the live database thereafter.

**Why:** The runtime seeder duplicated `schema.sql` seeds and was the proximate cause of the `contextLoads()` failure when the live DB drifted. Removing it eliminates a class of "JPA entity vs. live DB out of sync" bugs.

## 2026-05-05 - Drift Migration as a Separate File, Not a Schema Rewrite

**Decision:** Apply schema-drift fixes as `src/main/sql/migrations/2026-05-05-fix-schema-drift.sql` rather than editing `schema.sql`.

**Why:** `schema.sql` should describe the target state for fresh installs. The live-DB drift was an artifact of `CREATE TABLE IF NOT EXISTS` running over older tables — the canonical schema file shouldn't carry one-off remediation forever.

## 2026-04-26 - No Database Insert on Student Portal Welcome Page

**Decision:** Pass the 3 names to the next page via URL query parameters; do not insert a row into `student_records` until full details are collected.

**Why:** Avoids incomplete rows and preserves NOT NULL constraints. The welcome page is purely name-collection + duplicate-check.

## 2026-04-26 - Public Student Portal (No Authentication)

**Decision:** `student-portal.html`, `student-details.html`, and `/api/student-portal/**` are `permitAll()`. Students don't have user accounts.

**Why:** Aligns with project scope (students are not system users). System is LAN-only, so public access on the local network is acceptable.

## 2026-04-19 - Age as a Computed Field (Not User Input)

**Decision:** Remove `age` from all input DTOs and forms. Compute server-side from `birthdate` via `AgeCalculator` (`Period.between(birthdate, now).getYears()`).

**Why:** Single source of truth — eliminates age/birthdate mismatch.

## 2026-04-19 - Silent Age Recalculation on View (Not on Table Load)

**Decision:** `GET /api/admin/users/{id}` and `GET /api/auth/me` silently recalculate age from birthdate and persist it. The bulk admin user table does NOT recalculate. Silent writes are not logged.

**Why:** Balances accuracy and performance. Avoids N writes on every admin page load while keeping individual views correct.

## 2026-04-18 - System Logs: Server-Side Export + Filter Precedence

**Decisions (combined):**
- `GET /api/logs/export` generates CSV/XLSX/DOCX server-side (not client) and reuses the same `rangeDays`/`startDate`/`endDate` contract as the page-load query.
- `GET /api/logs` filter precedence: custom range (`startDate`+`endDate`) > `rangeDays` > default 7 days. No "all logs" mode is exposed.

**Why:** Server-side export gives real downloadable files independent of DataTables paging. Defaulting to 7 days prevents full-table scans as `system_logs` grows indefinitely.

## 2026-04-14 - Separate `system_logs` Table (No FK to users)

**Decision:** Standalone `system_logs` table with `user_id INT NULL` and no foreign key to `users`.

**Why:** Admin can hard-delete user accounts. An FK would either cascade (losing audit trail) or block the delete. Nullable user_id with no FK preserves the audit history.

## 2026-04-14 - Manual Service Calls Over AOP for System Logging

**Decision:** Inject `SystemLogService` into controllers and call `logAction()` explicitly. No `@Around` AOP.

**Why:** Logs are read by humans. Descriptive strings ("Reset password for: registrar") beat auto-generated AOP messages ("Called PUT /api/admin/users/3").

---

## Pre-2026-04-14 (one-line summaries)

- **2026-04-11 — Split password policy:** Strong rules (upper/lower/digit/special, min 8) only on self-service changes. Admin resets require min 8 only — admins need to issue temporary passwords like `Welcome1!`.
- **2026-04-11 — Programmatic password toggle injection:** Eye-icon buttons auto-injected on every `input[type=password]` via `auth-guard.js`. DRY across all 17 inputs.
- **2026-04-11 — `passwordChangedAt` on users table:** One nullable DATETIME column. NULL means "never changed."
- **2026-04-11 — Soft delete + hard delete tiers:** Soft (sets `enabled=false`) is default; hard delete requires extra confirmation.
- **2026-04-11 — Optional password in `AdminUpdateUserRequest`:** Null/blank preserves existing; provided values are BCrypt-hashed.
- **2026-04-11 — Spring Security `enabled` flag for soft delete:** Use the 7-arg `User` constructor; throws `DisabledException` automatically.
- **2026-04-11 — Root project as merge base for admin module:** Tracked repo root is source-of-truth; `main-em/` is donor/reference only.
- **2026-04-11 — DTO-based admin user API:** `AdminUserResponse` instead of returning the JPA entity, so password hashes never leak.
- **2026-04-05 — Remove DataSeeder, manage accounts via SQL:** Avoids startup overhead and accidental data resets. (Re-affirmed 2026-05-05.)
- **2026-04-05 — Unique index on username via ALTER TABLE:** Manual SQL keeps `ddl-auto=none` clean.
- **2026-04-05 — Dual-mode auth entry point:** Distinguish API vs browser by `/api/` URI prefix (deterministic).
- **2026-03-21 — Login page design:** Card-based centered form with gradient header/button.
