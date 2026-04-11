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
- **Build Repair**: `AdminController` was restored to the service/DTO contract after regressing to a direct `UserRepository` implementation that broke the WebMvc admin tests.
- **Front-End Repair**: `admin.html` was rebuilt after a donor regression left duplicate navbars, malformed head/body structure, and mismatched modal/table markup that could render the admin page blank.
- **Edit Flow Repair**: `edit-user.html` was rebuilt to match the current `admin-edit-user.js` field IDs and shared admin shell.
- **Verification**: `./gradlew build` passes after the admin front-end repair.
## Current Task
Re-test the repaired admin shell in the browser and confirm the dashboard, detail modal, and edit-user flow behave correctly end-to-end.

## Previous Task (Completed)
Database Schema Refactor - Dropped `full_name` and `date_of_birth` columns, replaced with `lastname`, `firstname`, `middlename`, and `birthdate`. Adapted backend data models and updated the frontend modals.

## Open Questions & Unverified Items
1. Confirm the rebuilt `admin.html` no longer renders as a blank white page after admin login.
2. Decide whether registrar and trainer dashboard shells should be visually refreshed to match the newer admin shell more closely.
