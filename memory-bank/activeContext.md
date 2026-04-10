# Active Context — Anihan SRMS

## Current Phase
**Root/Admin Merge Stabilization**

## Active Branch
`feature/fix-login-security` (Need to consider if we should create a new branch e.g., `feature/update-database-schema` or stay here. The user instructs us to stay in the stack. I should probably stay in this branch as it's already active, or ask whether to create a new one. Wait, the rule says "create a dedicated feature branch". I will inform the user that I'll use a new branch if they want, but I'm currently on `feature/fix-login-security`.)

## Status (April 2026)
- **Completed**: Fixed HTML landing routing. Admin User Management Dashboard implemented, complete with DataTables, a responsive navbar, user detail modals, and an Edit User page.
- **Security Enhancements**: Role-based access control (RBAC) securely locked down `admin.html`, `trainer.html`, etc. Added backend and frontend checks to prevent Admins from mistakenly changing their own role and losing access.
- **SQL Schema**: `AnihanSRMS.sql` features comprehensive tables. Note: file contains unresolved Git merge conflict markers.
- **UI Fixes (2026-04-10, Branch: `main-em`)**: Reverted navbar color across 5 admin pages to original `login.css` green, removing inline styles and conflicting `bg-success`. Fixed structural HTML bug in `edit-user.html` (extra `</div>` breaking card layout).
- **In-Progress**: Wiring up the remaining CRUD operations for Student Records, Subjects, and Logs.
- **Immediate Task**: Proceed with the "Student Records" tabular view and specific CRUD functionality.
## Current Task
Merge the donor admin module from `main-em/` into the tracked root app while preserving the root auth flow and current user schema.

## Previous Task (Completed)
Database Schema Refactor — Dropped `full_name` and `date_of_birth` columns, replaced with `lastname`, `firstname`, `middlename`, and `birthdate`. Adapted backend data models and updated the frontend modals.

## Open Questions & Unverified Items
1. Confirm visual behavior of the merged admin pages (`admin.html`, `edit-user.html`, `student-records.html`, `subjects.html`, `logs.html`) in the browser.
2. Decide whether registrar and trainer dashboard shells should be visually refreshed to match the newer admin shell more closely.
