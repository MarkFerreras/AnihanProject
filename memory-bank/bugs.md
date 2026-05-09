# Known Bugs & Technical Debt — Anihan SRMS

> **Last updated:** April 30, 2026

## Fixed Bugs (one-line summary)

- **Bug 1** ✅ `student_records.age` NOT NULL blocked enrollment — `ALTER TABLE ... MODIFY COLUMN age INT NULL`. (2026-04-30)
- **Bug 2** ✅ `StudentRecord` `@Id` mapped to `student_id` instead of `record_id` — moved `@Id @GeneratedValue(IDENTITY)` to `recordId`; repo generic `String → Integer`; added `findByStudentId()`. (2026-04-30)
- **Bug 3** ✅ `Parent` / `OtherGuardian` `@JoinColumn` FK mismatch after Bug 2 — added `referencedColumnName = "student_id"` to both. (2026-04-30)
- **Bug 6** ✅ Duplicate `requestMatchers` rules in `SecurityConfig` — consolidated into one block. (2026-04-30)
- **Bug 7** ✅ `saveDraft()` failure swallowed before submit — refactored to return `boolean`; submit blocks on failure. (2026-04-30) — *Note: `saveDraft()` was later removed entirely on 2026-05-05; see RC-2 in changeLog.*

## Open Bugs

### Bug 4 — Student email not collected during enrollment 🟡
- **Severity:** Medium · **Status:** Open (needs product decision)
- **Where:** `StudentDetailsRequest`, `StudentDetailsResponse`, `student-details.html`, `student-details.js`
- **What:** `student_records.email` exists in DB and entity, but the enrollment wizard never reads or writes it — always `NULL` after enrollment.
- **Decision needed:** registrar fills it in post-enrollment, or add email input to Step 1?

### Bug 5 — `AgeCalculator` returns 0 instead of null for missing birthdate 🟢 (resolved 2026-05-05)
- **Status:** ✅ Fixed in the May 5 enrollment flow fix (RC-5). `AgeCalculator.calculateAge` now returns `Integer` and returns `null` for null birthdate. Kept here for traceability.

## Technical Debt

- **TD-1** — No unit/integration tests for `StudentDetailsService`, `StudentDetailsController`, `StudentPortalController`, `StorageService`, `ClassManagementService`, `ClassManagementController`. *(Partially addressed 2026-05-05: `StudentDetailsServiceTest` added with 7 tests.)*
- **TD-2** — `student-records.html` still has the old admin 4-link navbar. Update internal navbar before re-enabling. *(`subjects.html` was rebranded to registrar on 2026-05-01.)*
- **TD-3** — Orphan legacy tables in live DB (`classess`, `log`, `previous_school`, `qualification_assessment`) — no JPA entities reference them; harmless but unused. *(Dropped on 2026-05-02.)*
