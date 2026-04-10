# Progress — Anihan SRMS

## Current Status
- **Tasks in Progress**: Updating database schema and aligning backend Entities, DTOs, Controllers, and Frontend views (AGILE-XXX).
- **Completed**: G2.1 Edit Personal Details (Sprint 4 completed). Database Schema migration to support distinct name fields (lastName, firstName, middleName) and birthdate.

## Research & Documentation (Completed)
- [x] Extracted Project Scope, Problem Statements, and Limitations from `Chapter-1.pdf`
- [x] Defined Actor roles (Registrar, Trainer, Admin, Student) and Use Cases from `Chapter-3.pdf`
- [x] Defined System Workflows via BPMNs and DFDs (Enrollment, Special Order, Grading)
- [x] Established Data Dictionaries & Base ERD Entities
- [x] Recorded Deployment Constraints (Local Network, Windows Server 2025)

## Backend & Infrastructure (Completed)
- [x] Project folders created (`controller`, `model`, `repository`, `service`)
- [x] Memory bank initialized & populated with Capstone constraints
- [x] Gradle upgraded to 9.4.1
- [x] Existing MySQL 8 via Docker integration
- [x] `AnihanSRMS.sql` ERD mapped into 11 JPA Entities
- [x] Initial Spring Security 7 RBAC & Authentication logic (LoginRequest, AuthController)
- [x] Global Exception Handler established
- [x] Data Seeder executed for Admins, Registrars, and Trainers
- [x] Removed DataSeeder — accounts managed directly in database
- [x] Added unique constraint on `users.username` column

## Login Security & Account Management (Completed — AGILE-142)
- [x] Server-side role-based page matchers for dashboard HTML pages
- [x] Dual-mode authentication entry point (redirect browser / JSON API)
- [x] Dual-mode access denied handler (wrong-role → own dashboard / API → 403 JSON)
- [x] AccountService with username change and password change logic
- [x] AccountController with PUT /api/account/profile and PUT /api/account/password
- [x] UpdateProfileRequest and UpdatePasswordRequest DTOs with validation
- [x] IllegalArgumentException handler in GlobalExceptionHandler
- [x] Shared auth-guard.js for all authenticated pages
- [x] Account dropdown (icon + menu) on all dashboard navbars
- [x] Edit Account modal (username change + password change)
- [x] Session check on login page to redirect already-authenticated users
- [x] Cache-control headers disabled for authenticated pages
- [x] Password change forces session invalidation and re-login
- [x] Enforce strict case-sensitivity for username login (email remains case-insensitive)

## Frontend & UI (Completed)
- [x] Login page front-end UI (`index.html`, `css/login.css`)
- [x] Dashboard templates built (`admin.html`, `registrar.html`, `trainer.html`) 
- [x] Custom JS fetch logic for form interactions
- [x] Dashboard CSS for account dropdown and modal styling
- [x] Shared auth-guard.js for session protection and account UI

## In Progress
- [/] Refactoring logic to align heavily with newly integrated Capstone Requirements

## Remainder Requirements / Roadmap
- [ ] Create `StudentUser` Enrollment Portal logic
- [ ] Implement `updateGrade()` logic for Trainers 
- [ ] Implement BLOB Database encoding via `encodeStudentDocsPerBatch()`
- [ ] Implement rigorous Unit Testing as required by Agile sprints
- [ ] Prepare User Acceptance Testing tools to execute the Time and Motion Study
- [ ] Build KPI Evaluation Dashboards (Processing efficiency, SO prep time, data accuracy) against ISO/IEC 25010 standards

## Validation Metrics (From Non-Functional Requirements)
- Document Upload Response: < 3 seconds (< 5MB files)
- Student Record Retrieval: < 5 seconds
- Concurrent User Limit testing: 50+ users
- Hardware RAM footprint: < 6GB RAM required on runtime environment
