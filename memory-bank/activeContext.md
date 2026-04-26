# Active Context - Anihan SRMS

## Current Phase
**Student Portal — Welcome Page**

## Active Branch
`test-user-table`

## Status (April 26, 2026)

### Student Portal Welcome Page (April 26, 2026)
Created a public student portal where students can begin enrollment without a user account:
- Created `student-portal.html` — welcome page with Last Name, First Name, Middle Name form
- Created `student-details.html` — placeholder page for future student details input
- Created `StudentRecordRepository.java` with case-insensitive name duplicate check
- Created `StudentPortalController.java` with `GET /api/student-portal/check-duplicate` (public endpoint)
- Created `student-portal.js` — form logic, duplicate check, redirect to details page
- Updated `SecurityConfig.java` — student portal pages and API are publicly accessible (no auth)
- Names are passed to the next page via URL params (no database insert on welcome page)
- Duplicate check compares all 3 names case-insensitively against `student_records` table

**Student Portal URL:** `/student-portal.html`

### Previous Sessions
- Database Schema Sync & SQL Export (April 26, 2026)
- Age Auto-Calculation from Birthdate (April 19, 2026)
- System Logs Export UI Cleanup (April 18, 2026)
- Admin Bulk Load Tests (April 18, 2026)
- Database Migration Fix (April 17, 2026)
- Admin Navbar Cleanup (April 17, 2026)

## Verified
- `./gradlew build` → BUILD SUCCESSFUL (7 tasks, all tests green)

## Known Notes
- `student-records.html` and `subjects.html` still have old editable age input in their Edit Account modals (future task)
- `spring.jpa.hibernate.ddl-auto=none` — all schema changes must be done via SQL manually
- `student-details.html` is a placeholder — actual enrollment fields to be added in a future session
