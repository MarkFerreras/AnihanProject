# Active Context - Anihan SRMS

## Current Phase
**Admin Users Table Column Split & Student Portal Re-implementation**

## Active Branch
`test-user-table`

## Status (April 27, 2026)

### Admin Users Table — Column Split (April 27, 2026)
Split the combined "Name" column in the admin Users table into separate "Last Name" and "First Name" columns:
- Replaced single `<th>Name</th>` with `<th>Last Name</th>` + `<th>First Name</th>` in `admin.html`
- Updated DataTables column config in `admin-users.js`: replaced `formatName(row)` with two separate `data: 'lastName'` and `data: 'firstName'` columns
- No backend or DTO changes needed — `AdminUserResponse` already has separate `lastName` and `firstName` fields
- No test changes needed — existing tests don't assert on HTML table column structure
- `./gradlew build` → BUILD SUCCESSFUL (all tests green)

### ⚠️ Student Portal — Code Lost from April 26 Session
The student portal code created in the previous session (April 26) was **not found on disk**. All files are missing:
- `StudentRecordRepository.java`, `StudentPortalController.java`
- `student-portal.html`, `student-details.html`, `js/student-portal.js`
- `SecurityConfig.java` portal `permitAll()` entries
- Memory bank files also did not retain the student portal updates
- Evidence: stale `StudentRecordRepository.class.uniqueId2` exists in build cache

### Previous Sessions
- Database Schema Sync & SQL Export (April 26, 2026)
- Age Auto-Calculation from Birthdate (April 19, 2026)
- System Logs Export UI Cleanup (April 18, 2026)
- Admin Bulk Load Tests (April 18, 2026)
- Database Migration Fix (April 17, 2026)
- Admin Navbar Cleanup (April 17, 2026)

## Verified
- `./gradlew build` → BUILD SUCCESSFUL (7 tasks, all tests green) — April 27
- Admin Users table now has 7 columns: User ID, Role, Last Name, First Name, Email, Status, Details
- DataTables JS column count matches HTML `<th>` count (7)

## Known Notes
- `student-records.html` and `subjects.html` still have old editable age input in their Edit Account modals (future task)
- `spring.jpa.hibernate.ddl-auto=none` — all schema changes must be done via SQL manually
- Student portal needs to be re-created (design decisions documented in conversation `abd39c2f`)
