# Active Context - Anihan SRMS

## Current Phase
**Registrar Home — Student Records Dashboard (2026-05-01) + Live Age Recalc (2026-05-01) + Registrar Navbar (2026-05-01)**

## Session Summary (May 1, 2026)
- New API endpoints: `GET /api/registrar/student-records`, `GET /api/registrar/student-records/{recordId}` (REGISTRAR-only)
- New DTO package: `dto/registrar/` with `StudentRecordSummaryResponse` and `StudentRecordDetailsResponse`
- New service: `RegistrarService` (reuses existing `StudentRecordRepository`)
- Registrar home now displays a DataTables-powered student records table with detail modal
- `student-records.html` repurposed for registrar (placeholder page reachable from modal Edit button)

## Active Branch
`fix/db-sync-username-unique`

## Status (May 1, 2026)

### Registrar Navbar Standardization (May 1, 2026)
- **Task**: Add a navbar to all registrar pages matching the admin navbar pattern, with two links: Home and Subjects
- **Files Changed**:
  - `static/registrar.html` — replaced simple placeholder navbar with full Bootstrap-collapsible navbar (Home active, Subjects); added `class="dashboard-page"` to body
  - `static/subjects.html` — rebranded from admin to registrar (role, title, navbar links, portal label, brand href to `registrar.html`)
  - `config/SecurityConfig.java` — moved `subjects.html` from ADMIN matcher to REGISTRAR matcher
- **CSS**: Reused existing `admin-navbar`, `admin-nav-link`, `portal-label` classes — no new CSS
- **Subjects page content**: intentionally left as placeholder per user request (just heading + muted text)
- **Decision recorded**: Subjects page is now REGISTRAR-only; Admin no longer has access

## Status (April 30, 2026)

### Enrollment Flow Bug Audit & Fixes (April 30, 2026)
- **Task**: Audit enrollment flow for potential bugs, run JUnit tests, fix critical issues
- **Bugs Found**: 5 (see `/memory-bank/bugs.md` for full tracker)
- **Bugs Fixed This Session**:
  - Bug 3 (Critical): `Parent.java` and `OtherGuardian.java` `@JoinColumn` FK mismatch → added `referencedColumnName = "student_id"`
  - Bug 6 (Low): Duplicate security authorization rules in `SecurityConfig.java` → consolidated into single clean block
- **Bugs Deferred (Open)**:
  - Bug 4 (Medium): `student_records.email` not mapped in enrollment flow — awaiting product decision
  - Bug 5 (Medium): `AgeCalculator` returns `0` instead of `null` for missing birthdate — needs code fix
  - Bug 7 (Medium): `saveDraft()` failure silently swallowed before submit — user questions whether saveDraft is needed at all since students only have one session
- **JUnit Tests**: `./gradlew test` → BUILD SUCCESSFUL (30s, all tests pass)

### Previous Sessions
- DB Sync from AnihanSRMS.sql (April 30, 2026) — UNIQUE index on users.username
- Bug Fixes (student_records.age, StudentRecord @Id) — April 30, 2026
- Submit Button Fix — April 29, 2026
- Student Details Enrollment Wizard — April 29, 2026

## Verified
- `./gradlew test` → BUILD SUCCESSFUL — April 30
- Parent.java FK now correctly joins to `student_records.student_id`
- OtherGuardian.java FK now correctly joins to `student_records.student_id`
- SecurityConfig has no duplicate matchers
- All 9 child table FKs reference `student_records.student_id` (verified via information_schema)

## Known Issues (Open)
- See `/memory-bank/bugs.md` for full bug tracker
- Bug 4: Student email not in enrollment flow (needs product decision)
- Bug 5: AgeCalculator returns 0 instead of null
- Bug 7: saveDraft architecture question (one-session design vs. crash recovery)
- Tests not yet updated for student enrollment module (TD-1)
- `student-records.html` still has old admin navbar (TD-2 — `subjects.html` resolved 2026-05-01 via registrar rebrand)
- `spring.jpa.hibernate.ddl-auto=none` — all schema changes must be done via SQL manually
