# System Patterns — Anihan SRMS

## Architecture
- **Model-View-Controller (MVC)** is strictly adhered to, enforcing role-specific access by isolating business logic from user interfaces.
- **Application Layers**: `controller`, `model`, `repository`, `service`.

## Core Entities & Data Structures (Based on ERD)
- **StudentRecord**: Central entity holding demographic data, foreign keys to parents, batch, section, and course.
- **Batch & Section**: Groups students to facilitate bulk processing of Special Orders (SO) and grade input.
- **EncodableDocument**: Entity holding binary large objects (BLOBs) for digitized docs (e.g., PSA, Form 137).
- **Subject, Qualification, Class**: Ties Trainers to specific academic components for grade inputting.
- **TransactionLog**: Immutable records of every update (grades, documents) to guarantee accountability.

## Workflow Patterns
- **Enrollment Flow**: Student submits personal details via Portal -> Temporary "Pending Docs" Status -> Physical doc submission -> Registrar validates & finalizes enrollment (Status: Active).
- **Records Management Flow**: Trainer invokes `updateGrade()` for subject -> Registrar uses `encodeStudentDocsPerBatch()` pushing BLOBs -> Registrar monitors checklist -> SO requested from TESDA -> SO number returned -> Student cleared.

## User Access Patterns (RBAC)
- **Admin**: Full authority to create accounts and assign Trainers to Classes. View logs and manage overarching tables.
- **Registrar**: High clearance for data maintenance. Approves enrollment, edits biographical data, batch uploads documents, and generates Special Orders. 
- **Trainer**: Restricted to viewing assigned classes (`viewAssignedSubjects()`) and updating numerical grades only. Once locked, grades cannot be altered.
- **Student**: Self-enrollment initialization, followed exclusively by read-only portal access (view grades, view missing documents). 

## Front-End Patterns
- **Styling**: Bootstrap 5.3 + custom CSS (`kebab-case` classes). Semantic HTML IDs (`camelCase`).
- **Data Display**: DataTables 2 for high-efficiency tabular search, filtering, and sorting of student and grade data.
- **JavaScript**: jQuery 4.0 handling AJAX requests and DOM manipulation.

## Admin Page Template Pattern (Mandatory for All Admin Pages)
Every admin page **must** include:
1. `<body class="dashboard-page" data-required-role="ROLE_ADMIN">`
2. `css/dashboard.css` — shared color scheme and component styles
3. **Brand-mark navbar** with `<span class="brand-mark">` (logo + "Anihan SRMS" title)
4. **Account dropdown** — `.account-icon-btn` with user initial, dropdown menu with "Edit Account" and "Log Out"
5. **Edit Account modal** (`#editAccountModal`) — tabbed (Personal Details + Account Settings) with username change, password change forms
6. `js/auth-guard.js` — session protection + account dropdown logic
7. `js/jquery-4.0.0.min.js` — required for auth-guard.js and AJAX
8. Nav links: **Home | Logs** — with `active` class on current page

> **⚠️ CRITICAL WARNING — STALE NAVBARS:**
> `student-records.html` and `subjects.html` still contain the OLD 4-link navbar (Home | Student Records | Subjects | Logs).
> These pages were intentionally left unchanged when Student Records and Subjects were removed from the active admin navbar.
> **Before re-adding these pages to the admin navbar, you MUST update their internal navbars to match the current pattern above.**
> Failure to do so will create an inconsistent navigation experience across admin pages.
