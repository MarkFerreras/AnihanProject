# Active Context - Anihan SRMS

## Current Phase
**Student Details Enrollment Wizard — Fully Implemented**

## Active Branch
`feature/student-details`

## Status (April 29, 2026)

### Submit Button Fix (April 29, 2026 — Late)
- **Root cause**: Browser caching a stale `student-details.js` that referenced a removed `btnSaveDraft` element, causing a TypeError that aborted `setupNavButtons()` before the Submit listener was attached.
- **Fix 1**: Added cache-busting `?v=2` to the `<script>` tag in `student-details.html`.
- **Fix 2**: Removed `throw e` from `saveDraft()` — the rethrow would crash both Next and Submit handlers if a draft-save failed.
- **Fix 3**: Wrapped `saveDraft()` call in Submit handler with try/catch so submission isn't blocked by a draft-save failure.

### Student Details Enrollment Wizard (April 29, 2026)
Full 4-step enrollment wizard implemented end-to-end:
- **Step 1** — Personal Details (contact, birthdate/age, sex, civil status, addresses, sibling count)
- **Step 2** — Religion & Documents (religion, baptism fields, ID photo upload, baptismal cert upload)
- **Step 3** — Family (father, mother, guardian)
- **Step 4** — Education & Training (education history table, school years, OJT, TESDA qualifications)
- Resumable: `startOrResume()` finds existing Enrolling/Draft records by name
- Auto-save draft on each step transition
- File uploads stored on disk under `./uploads/students/{studentId}/`
- `./gradlew build -x test` → BUILD SUCCESSFUL

### Previous Sessions
- Admin Users Table Column Split (April 27, 2026)
- Database Schema Sync & SQL Export (April 26, 2026)
- Age Auto-Calculation from Birthdate (April 19, 2026)
- System Logs Export UI Cleanup (April 18, 2026)

## Verified
- `./gradlew build -x test` → BUILD SUCCESSFUL — April 29
- 17 tables confirmed in live MySQL: all 5 new student tables created
- student_records columns made nullable; civil_status added
- parents and other_guardians columns made nullable

## Known Notes
- Tests not yet updated for new student module (no tests written for StudentDetailsService/Controller)
- `student-records.html` and `subjects.html` still have old navbars (pre-existing deferred task)
- `spring.jpa.hibernate.ddl-auto=none` — all schema changes applied manually via docker exec

## Known Notes
- `student-records.html` and `subjects.html` still have old editable age input in their Edit Account modals (future task)
- `spring.jpa.hibernate.ddl-auto=none` — all schema changes must be done via SQL manually
- Student portal needs to be re-created (design decisions documented in conversation `abd39c2f`)
