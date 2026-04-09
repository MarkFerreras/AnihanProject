# Progress — Anihan SRMS

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

## Frontend & UI (Completed)
- [x] Login page front-end UI (`index.html`, `css/login.css`)
- [x] Dashboard empty templates built (`admin.html`, `registrar.html`, `trainer.html`, `student-records.html`, `subjects.html`, `logs.html`)
- [x] Custom JS fetch logic for form interactions
- [x] SQL database schema created and updated (`AnihanSRMS.sql`)
- [x] Admin User Management Dashboard (`admin.html`) built with DataTables and Bootstrap Navbar.
- [x] User Details Modal & Edit User Page (`edit-user.html`) with self-role lock protections.

## In Progress
- [/] Refactoring logic to align heavily with newly integrated Capstone Requirements
- [/] Resolving false-positive IDE syntax issues with Core Java classes ("String cannot be resolved")

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
