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
- **Verification**: `./gradlew test` and `./gradlew build` both pass after the controller repair.
## Current Task
Resume manual browser validation of the merged admin pages now that the automated build and test flow is green again.

## Previous Task (Completed)
Database Schema Refactor - Dropped `full_name` and `date_of_birth` columns, replaced with `lastname`, `firstname`, `middlename`, and `birthdate`. Adapted backend data models and updated the frontend modals.

## Open Questions & Unverified Items
1. Confirm visual behavior of the merged admin pages (`admin.html`, `edit-user.html`, `student-records.html`, `subjects.html`, `logs.html`) in the browser.
2. Decide whether registrar and trainer dashboard shells should be visually refreshed to match the newer admin shell more closely.
