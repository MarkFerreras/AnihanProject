# Active Context - Anihan SRMS

## Current Phase
**Database Schema Sync & SQL Export Files**

## Active Branch
`test-user-table`

## Status (April 26, 2026)

### Database Schema Sync & SQL Export (April 26, 2026)
Synchronized the live database with all JPA entity models and created export SQL files:
- Created 6 missing tables in live Docker MySQL: `qualifications`, `subjects`, `parents`, `other_guardians`, `documents`, `grades`
- Added `UNIQUE INDEX idx_student_id` on `student_records` (required for FK references)
- Rewrote `src/main/sql/AnihanSRMS.sql` — full clone dump (12 tables DDL + 3 users + 79 system logs)
- Created `src/main/sql/schema.sql` — clean schema + 3 dummy seed accounts (no system logs)
- Removed obsolete tables from SQL: `log`, `classess`, `qualification_assessment`, `previous_school`
- Both SQL files include `CREATE DATABASE` + `USE` for standalone execution on a new device

**Database now has 12 tables:**
`batches`, `courses`, `qualifications`, `sections`, `subjects`, `users`, `student_records`, `parents`, `other_guardians`, `documents`, `grades`, `system_logs`

### Previous Sessions
- Age Auto-Calculation from Birthdate (April 19, 2026)
- System Logs Export UI Cleanup (April 18, 2026)
- Admin Bulk Load Tests (April 18, 2026)
- Database Migration Fix (April 17, 2026)
- Admin Navbar Cleanup (April 17, 2026)

## Verified
- `SHOW TABLES` → 12 tables confirmed in live database
- All 6 new tables created successfully with correct FK constraints
- Existing data preserved (3 users, 79 system logs)
- `AnihanSRMS.sql` contains full DDL + all current data
- `schema.sql` contains full DDL + 3 dummy accounts, no system log data
- `student_records.record_id` remains PK (matching live DB)

## Known Notes
- `student-records.html` and `subjects.html` still have old editable age input in their Edit Account modals (future task)
- `spring.jpa.hibernate.ddl-auto=none` — all schema changes must be done via SQL manually
