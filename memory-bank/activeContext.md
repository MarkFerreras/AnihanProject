# Active Context - Anihan SRMS

## Current Phase
**Enrollment Flow Bug Fixes — Completed**

## Active Branch
`fix/db-sync-username-unique`

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
- `student-records.html` and `subjects.html` still have old navbars (TD-2)
- `spring.jpa.hibernate.ddl-auto=none` — all schema changes must be done via SQL manually
