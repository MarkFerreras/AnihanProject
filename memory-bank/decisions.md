# Decisions - Anihan SRMS

Each entry: decision + brief rationale. Older entries (pre-2026-04-26) are summarized to one line; if you need full alternatives-considered for a historical decision, consult `changeLog.md` for the matching session.

---

## 2026-08-30 - All Sheet-Format Knowledge Isolated in One Editable File

**Decision:** Every rule about how an imported sheet is recognised — header aliases per column,
which column is the match key, header/value normalisation, how far down to scan for the header
row — lives in `service/StudentNumberImportMapping.java`. `StudentNumberSheetParser` and
`StudentNumberImportService` hold none of it.

**Why:** The school's real record format is not yet known. Adapting to it should be an edit to
string lists in one commented file, verified by re-running `StudentNumberSheetParserTest`, not a
rewrite of the parsing or classification logic.

**Limit of the abstraction (stated so it is not discovered the hard way):** this covers *naming
and formatting* differences. If the archive identifies a student by something the system does
not store — an old ledger number, or a name + birthdate pair — that is a change of matching
*strategy* and needs real design work, not a new alias.

## 2026-08-30 - Import Previews Before It Writes

**Decision:** `POST /import/preview` parses, validates, and classifies every row while writing
nothing; only `POST /import/apply` writes. Apply re-reads and re-classifies the uploaded file
rather than trusting any plan the client sends back. Both share one `classify()` method.

**Why:** A bulk write to student identity should not be discovered to be wrong afterwards. The
shared classification means what the preview promises and what the apply does cannot drift.
Re-uploading on apply keeps the server stateless between the two calls, which is cheap at
~160 rows.

**Related:** apply writes the applicable rows and reports the rest rather than failing the whole
file. With a mandatory preview in front, that is safer than rejecting 200 good rows over one
typo — and each write is individually logged.

## 2026-08-30 - A Bulk File Never Silently Replaces an Existing Student Number

**Decision:** A row supplying a number for a student who already has a *different* one is
reported as `CONFLICT_EXISTING` and skipped, unless the Registrar ticks "Allow overwriting
existing numbers". A number appearing twice within one file blocks both of its rows.

**Why:** A stale spreadsheet must not be able to rewrite identities across the archive as a side
effect of a routine import. Duplicates within a file are an encoding mistake where neither
intent is knowable, so applying either would be a guess.

## 2026-08-27 - Student Number as a Separate Nullable Column, Not a Nullable `student_id`

**Decision:** The registrar-controlled student number lives in a NEW nullable
`student_records.student_number VARCHAR(20) UNIQUE` column. The existing `student_id` is kept
exactly as it is — `NOT NULL UNIQUE`, auto-generated, immutable — and demoted in the UI to an
internal "Reference No.". Nothing auto-generates `student_number`.

**Why:** `student_id` is the foreign-key target of **10 child tables** (parents,
other_guardians, documents, grades, student_education, student_school_years, student_ojt,
student_tesda_qualifications, student_uploads, class_enrollments). Making it nullable would
orphan every child row created before a number is assigned — starting with the ID-photo
upload in wizard step 2, which is the very reason `startOrResume` creates the record so early.

**Alternative rejected:** repointing all 10 child FKs to `record_id` (the actual PK) and
letting `student_id` become the nullable student number. That is the cleaner end state — one
identifier instead of two — but costs ~40 files (10 entities, ~20 repository methods, 7
services, portal URLs) plus a 10-table data migration, with corresponding risk to the 217-test
baseline. Deferred, not discarded: if the two-identifier split proves confusing in practice,
this is the migration to do.

**Trade-off accepted:** a student record now carries two identifiers. Mitigated by labelling —
"Reference No." (internal) vs "Student Number" (real) — everywhere both appear.

## 2026-08-27 - "Primary Key" in the Requirement Read as "Unique Business Key"

**Decision:** The meeting note "the student number should remain the primary key" is
implemented as a UNIQUE index, not a PRIMARY KEY.

**Why:** Two facts make the literal reading impossible. `student_id` has not been the primary
key since 2026-05-02 (`record_id` is), so nothing "remains" a PK. And a column cannot be both
nullable and a SQL primary key — the same requirement asks for nullable. A UNIQUE index is the
faithful reading: unique when present, absent until assigned. MySQL permits multiple NULLs in
a unique index, which is exactly the needed semantics (verified live: two NULL rows coexist;
a duplicate real value raises ERROR 1062).

## 2026-08-27 - Student Number Written Only Through a Dedicated Assign Action

**Decision:** `student_number` is written solely by
`PUT /api/registrar/student-records/{id}/student-number`. The registrar edit form displays it
read-only and never sends it in `buildPayload`. Uniqueness is pre-checked in the service so a
clash returns a 400 naming the student who already holds the number, rather than the generic
409 the unique index would produce.

**Why:** Assigning a student number is a records-integrity event that should be deliberate and
individually auditable in `system_logs`, not a side effect of editing an address. Keeping it
out of the edit payload also means a routine edit can never silently wipe it — an invariant
now pinned by a unit test.

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
