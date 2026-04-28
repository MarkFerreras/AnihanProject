# CLAUDE.md
@.agents/rules/full-stack-anihan.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Anihan Technical School Student Records Management System (SRMS) — a Spring Boot + vanilla JS web app for managing student enrollment, grades, and audit logs. Deployed on-premise on a Windows Server 2025 air-gapped network.

## Tech Stack

- **Backend:** Java 25, Spring Boot 4.0.4, Spring Security 7, Spring Data JPA, MySQL 8
- **Build:** Gradle 9.4.1 (Kotlin DSL)
- **Frontend:** HTML5, Bootstrap 5.3, DataTables 2, jQuery 4.0 — all libraries served locally (no CDN)
- **DB:** MySQL 8 in Docker, schema managed manually (JPA DDL is `none`)

## Project Memory

@memory-bank/projectbrief.md
@memory-bank/productContext.md
@memory-bank/activeContext.md
@memory-bank/techContext.md
@memory-bank/systemPatterns.md
@memory-bank/decisions.md
@memory-bank/progress.md
@memory-bank/changeLog.md
@memory-bank/testing.md
@memory-bank/add-account.md

## Memory Update Protocol

After completing any task:
1. Identify what changed (code, architecture, decisions, progress)
2. Update the relevant memory-bank file(s):
   - `progress.md` — what was completed, current blockers
   - `changeLog.md` — what files were modified and why
   - `decisions.md` — if a design choice was made
   - `activeContext.md` — if the current focus area changed
   - `techContext.md` — if tech approach shifted
3. Commit memory updates alongside code changes

**This is not optional** — memory files are the source of truth for project state.

## Common Commands

```bash
# Build
./gradlew build

# Run dev server
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.springboot.SomeTest"
```

**Database prerequisite:** MySQL must be running on `localhost:3306`, database `AnihanSRMS`, credentials `root / my_password` (see `application.properties`). Schema must be applied manually from `src/main/sql/schema.sql`.

**Seed accounts** (password: `password123`): `admin`, `registrar`, `trainer`.

## Architecture

Strict MVC layering — every feature touches all layers in order:

```
Controller → Service → Repository → Model (JPA Entity)
                ↕
              DTO  (request/response shapes)
```

**Key packages** under `src/main/java/com/example/springboot/`:
- `controller/` — REST endpoints + HTML page routing (Auth, Admin, Account, Lookup, SystemLog controllers)
- `service/` — business logic including `AgeCalculator` (auto-computes age from birthdate on save and read)
- `repository/` — Spring Data JPA interfaces
- `model/` — 11 JPA entities (User, StudentRecord, Batch, Course, Section, Subject, Grade, Parent, OtherGuardian, Document, SystemLog)
- `dto/` — 8 DTOs for login, admin ops, log queries
- `exception/` — `GlobalExceptionHandler` for centralized error responses
- `config/SecurityConfig.java` — RBAC, session config, CSRF rules

**Frontend** lives in `src/main/resources/static/`:
- `*.html` — one page per role dashboard (admin.html, registrar.html, trainer.html, index.html)
- `js/` — `auth-guard.js` (session protection included on every page), `admin-*.js`, `system-logs.js`
- `css/` — Bootstrap, DataTables, `dashboard.css`

## Role-Based Access

| Role | Access | Key Routes |
|------|--------|-----------|
| ADMIN | User account management, system logs export | `/admin.html`, `/api/admin/**`, `/api/logs/**` |
| REGISTRAR | Student records, enrollment, documents | `/registrar.html`, `/api/registrar/**` |
| TRAINER | View assigned subjects, input/lock grades | `/trainer.html`, `/api/trainer/**` |

## Key Conventions

**Schema changes:** JPA DDL is `none` — all DB changes must be written as SQL and applied manually. Never rely on Hibernate to auto-migrate.

**Age field:** Never expose `age` as an editable field in forms. It is auto-calculated from `birthdate` by `AgeCalculator` on every save and read. The `age` column in `users` is kept in sync silently.

**Frontend page checklist:** Every dashboard page must include the dashboard navbar, account dropdown, edit modal (personal + account tabs), and `auth-guard.js` + jQuery imports.

**System logs:** Every significant action (account creation, grade update, document upload) must be written to `system_logs` via `SystemLogService`. The table is append-only — never update or delete log rows. Logs include `user_id`, `username`, `role`, `action`, `ip_address`, and `timestamp`.

**Security:** CSRF is disabled for `/api/**` endpoints and enabled for form submissions. Session timeout is 30 minutes; cookies are HTTP-only, SameSite=Lax.

**Name fields:** `users` table stores `last_name` and `first_name` as separate columns — do not collapse them into a single `full_name`.

## Database Schema

Schema source of truth: `src/main/sql/schema.sql` (and `AnihanSRMS.sql` at root for device transfer).

Core tables: `users`, `student_records`, `batches`, `courses`, `sections`, `subjects`, `qualifications`, `parents`, `other_guardians`, `documents` (BLOB), `grades`, `system_logs`.

`system_logs` has an index on `timestamp` — keep log queries range-filtered by timestamp when possible.