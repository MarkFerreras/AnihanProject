# Active Context - Anihan SRMS

## Current Phase
**Root/Admin Merge Stabilization**

## Active Branch
`feature/fix-login-security`

## Status (April 2026)
- **Completed**: Root admin module merged from `main-em` into the tracked repo root.
- **Security**: Admin-only routes are enforced for `admin.html`, `edit-user.html`, `student-records.html`, `subjects.html`, `logs.html`, and `/api/admin/**`.
- **Admin User Management**: Root admin dashboard includes DataTables user listing, detail modal, and dedicated edit-user flow.
- **Schema**: `AnihanSRMS.sql` has been cleaned and aligned with the root user schema and student record keys.
- **Verification**: `./gradlew test` and `./gradlew build` both pass after conflict cleanup.
## Current Task
Double-check the branch for commit safety after conflicting local changes and keep the memory-bank aligned with the repaired repo state.

## Previous Task (Completed)
Database Schema Refactor - Dropped `full_name` and `date_of_birth` columns, replaced with `lastname`, `firstname`, `middlename`, and `birthdate`. Adapted backend data models and updated the frontend modals.

## Open Questions & Unverified Items
1. Confirm visual behavior of the merged admin pages (`admin.html`, `edit-user.html`, `student-records.html`, `subjects.html`, `logs.html`) in the browser.
2. Decide whether registrar and trainer dashboard shells should be visually refreshed to match the newer admin shell more closely.
