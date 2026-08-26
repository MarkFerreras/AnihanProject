# Decisions - Anihan SRMS

Each entry: decision + brief rationale. Older entries (pre-2026-04-26) are summarized to one line; if you need full alternatives-considered for a historical decision, consult `changeLog.md` for the matching session.

---

## 2026-08-26 PM #2 - subject_code Rename Uses ON UPDATE CASCADE + a JPQL Bulk Update, Not a Guard-and-Block

**Decision:** `subjectCode` (the PK) is now editable in the Edit Subject modal
at any time, including when the subject has live `classes`/`grades`.
`classes.subject_code` and `grades.subject_code` were altered to `ON UPDATE
CASCADE` (previously MySQL-default `RESTRICT`) so a rename ripples into every
referencing row automatically. Renames are executed via a JPQL
`@Modifying` bulk `UPDATE`
(`SubjectRepository.renameSubjectCode`), not a normal `load → setSubjectCode →
save()` on the entity. Rejected alternative: leave the FKs as `RESTRICT` and
only allow the rename while `existsBySubjectSubjectCode`/`countGradesBySubjectCode`
both report zero (mirroring the existing Delete Subject guard) — offered to the
user as a narrower, no-migration option; they chose the cascading approach
instead.

**Why:** `subjectCode` is a JPA `@Id`. Hibernate has no well-defined way to
change a *managed* entity's own identity through a setter followed by
`save()` — the safe, standard pattern for a real PK rename is a direct SQL
`UPDATE` that bypasses entity-identity tracking entirely, which is also the
only way to actually trigger the database's `ON UPDATE CASCADE` (an
INSERT-a-new-row-then-delete-the-old-row approach would not cascade — MySQL's
FK cascade fires on the `UPDATE` statement itself). `ON DELETE` was left
`RESTRICT` — deleting a subject that still has classes/grades stays blocked,
exactly as before; only the update rule changed. Verified twice before
shipping: a raw-SQL test proved the cascade at the database level, then a live
HTTP smoke test proved the full stack (API → service → bulk update → cascade →
audit log).

## 2026-08-26 - competency_type as a Plain Column on subjects, Not a New Table or a Field on qualifications

**Decision:** `subjects.competency_type VARCHAR(15) NOT NULL` (values `BASIC`,
`COMMON`, `CORE`), validated at the service layer — same pattern as `role` and
`student_status`. Rejected alternatives: a dedicated `competencies` lookup table
with `subjects.competency_type_code` as an FK (mirrors how `qualifications` works),
and putting the classification on `qualifications` itself.

**Why:** Competency type is a property of the *subject*, not the qualification — a
qualification (e.g. "Cookery NC II") doesn't have one competency type, it's an
aggregate of subjects spanning all three. Confirmed from the actual TESDA documents
(`document-templates/Blank Form/FORM IX - BPP.docx`) and the existing
`curriculum-templates.js`: Basic and Common competency subjects are identical/shared
across all three qualifications (Cookery, BPP, FBS); only Core subjects are
qualification-specific. A separate lookup table was rejected because Basic/Common/
Core is a fixed, TESDA-defined set of exactly 3 values that will never be
admin-managed via UI — unlike `qualifications`, which genuinely needs to support new
rows as the school adds NC programs.

## 2026-08-26 - qualification_code Made Nullable, No "No Qualification" Sentinel Row

**Decision:** `subjects.qualification_code` relaxed from `NOT NULL` to `NULL`.
Basic/Common subjects leave it `NULL`; only Core subjects are required to set it
(enforced in `ClassManagementService`, not at the SQL level). Rejected alternative:
seed a sentinel `qualifications` row named "No Qualification" and point Basic/Common
subjects at it, keeping the FK `NOT NULL`.

**Why:** `qualifications` holds real TESDA-recognized NC programs that feed directly
into generated official documents (TOR, Form IX, TESDA Special Orders). A sentinel
row risks silently appearing on generated documents anywhere the app lists
qualifications, and depends on that placeholder row never being deleted. NULL
matches this schema's existing precedent for "not assigned" (`subjects.trainer_id`,
`classes.trainer_id` are both nullable FKs with the same meaning) and matches the
actual domain fact: these subjects don't belong to a qualification at all.

## 2026-07-14 - Generated-Document DOCX via Server-Side OOXML altChunk

**Decision:** Generated `text/html` documents download as .docx built by `HtmlDocxConverter`:
a minimal OOXML package (pure `java.util.zip`) whose `document.xml` references the stored
HTML as an altChunk; Word converts it to editable content on open. Uploaded pdf/docx/xlsx
keep their original bytes. Rejected alternative: vendoring `html-docx-js` (~70KB unaudited
JS doing the same altChunk trick client-side, with identical fidelity limits).

**Why:** Zero new dependencies on an air-gapped system, server-side and unit-testable, and
the stored HTML is already self-contained. Known trade-off: Word's HTML import linearizes
flex rows, so the docx is editable but not pixel-identical to the printed PDF.

## 2026-07-14 - Edit of a Generated Document Updates the Row In Place

**Decision:** `POST /generate` with an optional `documentId` updates the existing
`documents` row (same PK) instead of inserting a new copy. Guards: the document must
belong to the submitted studentId AND must be `text/html` — uploaded files can never be
overwritten through this path. Deletes are hard deletes via
`DELETE /api/registrar/documents/{id}`; only `system_logs` is append-only.

**Why:** Re-saving after an edit must not accumulate duplicate rows with the same
filename; the audit trail lives in `system_logs` ("Updated generated document…",
"Deleted document…"), not in row copies.

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
