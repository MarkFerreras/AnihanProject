# Active Context - Anihan SRMS

## Current Phase
**Student Portal Bug Fixes — All Enrollment Blockers Resolved**

## Active Branch
`feature/student-details`

## Status (April 30, 2026)

### Bug 1 Fix — `student_records.age` NOT NULL (April 30, 2026)
- **Root cause**: `student_records.age` was `NOT NULL` with no default. `startOrResume()` inserts a new record without an age (birthdate not yet provided), causing `DataIntegrityViolationException` → HTTP 500 → "Failed to start enrollment" alert.
- **Fix**: `ALTER TABLE student_records MODIFY COLUMN age INT NULL` applied to live MySQL.
- **Also updated**: `AnihanSRMS.sql` — added clarifying comment to `age INT NULL` line.
- **Enrollment wizard is now unblocked end-to-end.**

### Bug 2 Fix — StudentRecord JPA @Id Mismatch (April 30, 2026)
- **Root cause**: `StudentRecord.java` had `@Id` mapped to `student_id` (VARCHAR UNIQUE KEY), not `record_id` (INT AUTO_INCREMENT PRIMARY KEY). JPA's identity resolution was technically incorrect.
- **Fix**: Added `recordId` as `@Id @GeneratedValue(IDENTITY)` → `record_id`. Demoted `studentId` to `@Column(unique=true)`. Changed `StudentRecordRepository` generic type from `String` → `Integer`. Added `findByStudentId(String)` method. Updated two `findById` call sites in `StudentDetailsService` to `findByStudentId`.
- **Files changed**: `StudentRecord.java`, `StudentRecordRepository.java`, `StudentDetailsService.java`
- `./gradlew build -x test` → BUILD SUCCESSFUL

### Bug 1 — ⚠️ STILL OPEN — `student_records.age` is NOT NULL
- **Root cause**: `student_records.age` column is `NOT NULL` (no default) in the live DB. `startOrResume()` inserts a new record with `age = null` (birthdate is not known yet at enrollment start), causing a `DataIntegrityViolationException`.
- **Effect**: `POST /api/student/start` returns HTTP 500 → JS shows "Failed to start enrollment. Please go back and try again."
- **Fix needed**: `ALTER TABLE student_records MODIFY COLUMN age INT NULL;`
- **Status**: NOT YET APPLIED — awaiting user confirmation.

### DB Schema Audit & Migration (April 30, 2026)
- Compared live DB against `AnihanSRMS.sql` — found 6 categories of discrepancies
- Applied 6-step forward-only migration:
  1. Created 5 missing student tables (`student_education`, `student_school_years`, `student_ojt`, `student_tesda_qualifications`, `student_uploads`)
  2. Added `civil_status VARCHAR(50) NULL` to `student_records`
  3. Relaxed 11 NOT NULL → NULL on `student_records`
  4. Relaxed 9 NOT NULL → NULL on `parents`
  5. Relaxed 6 NOT NULL → NULL on `other_guardians`
  6. Added UNIQUE index on `users.username`
- Legacy orphan tables (`classess`, `log`, `previous_school`, `qualification_assessment`) left untouched — not referenced by any Java code

### Previous Sessions
- Submit Button Fix (April 29, 2026)
- Student Details Enrollment Wizard (April 29, 2026)
- Admin Users Table Column Split (April 27, 2026)
- Database Schema Sync & SQL Export (April 26, 2026)

## Verified
- `./gradlew build -x test` → BUILD SUCCESSFUL — April 30
- 21 tables in live MySQL (17 canonical + 4 legacy orphans)
- `StudentRecord.java` now correctly maps `@Id` → `record_id` (PK), `studentId` → UNIQUE business key

## Known Issues (Open)
- Tests not yet updated for new student module
- `student-records.html` and `subjects.html` still have old navbars (pre-existing deferred task)
- `spring.jpa.hibernate.ddl-auto=none` — all schema changes must be done via SQL manually
