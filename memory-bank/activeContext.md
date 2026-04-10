# Active Context — Anihan SRMS

## Current Phase 
**Research Integration & Backend Auth Refinement** 

## Development Priorities Backed by Capstone
1. **User Authentication Module**: Resolving role-based login (Admin, Registrar, Trainer, Student).
2. **Student Onboarding**: Establishing a form for students to input personal info and trigger a "Pending Registration" state.
3. **Trainer Grading Module**: Providing a filtered view of assigned classes and enforcing numerical grade submission.
4. **Registrar Batch Document Module**: Implementation of the document upload system ensuring files are stored efficiently in the database as BLOBs to prevent physical green folder accumulation.

## Status (April 2026)
- **Completed**: Fixed HTML landing routing. Admin User Management Dashboard implemented, complete with DataTables, a responsive navbar, user detail modals, and an Edit User page.
- **Security Enhancements**: Role-based access control (RBAC) securely locked down `admin.html`, `trainer.html`, etc. Added backend and frontend checks to prevent Admins from mistakenly changing their own role and losing access.
- **SQL Schema**: `AnihanSRMS.sql` features comprehensive tables. Note: file contains unresolved Git merge conflict markers.
- **UI Fixes (2026-04-10, Branch: `main-em`)**: Reverted navbar color across 5 admin pages to original `login.css` green, removing inline styles and conflicting `bg-success`. Fixed structural HTML bug in `edit-user.html` (extra `</div>` breaking card layout).
- **In-Progress**: Wiring up the remaining CRUD operations for Student Records, Subjects, and Logs.
- **Immediate Task**: Proceed with the "Student Records" tabular view and specific CRUD functionality.
## Open Questions & Unverified Items
1. **Tech Stack Adherence**: `Chapter 3` emphasizes the use of Python, but the user has explicitly confirmed continuing with **Java 25 and Spring Boot 4.0**. *Conflict resolved.*
2. **BLOB Storage**: `techContext` indicates document uploads are converted to BLOBs inside the DB. If documents become massive (many MBs each over 40 years), we may want to ask if filesystem secure storage + DB paths is preferred, though diagrams indicate direct database storing. *Currently marked as Confirmed to be encoded into the Database per `Chapter 3` diagrams, but poses a scaling risk.*
