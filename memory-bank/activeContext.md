# Active Context - Anihan SRMS

## Current Phase
**Admin Navbar Cleanup & Capstone Alignment**

## Active Branch
`feature/unit-tests-coverage`

## Status (April 17, 2026)

### Database Migration Fix (April 17, 2026)
The Docker MySQL container was missing 3 schema changes that the Java codebase expected, preventing login. Applied the following migrations directly to the running `mysql-server` container:
1. `ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;`
2. `ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL;`
3. `CREATE TABLE IF NOT EXISTS system_logs (...)` — full schema per `AnihanSRMS.sql` lines 172–181

All 3 statements match exactly what is defined in `AnihanSRMS.sql`. Login now works with the existing seeded accounts (`Ado`, `registrar`, `trainer`).

### Admin Navbar Cleanup (April 17, 2026)
Removed "Student Records" and "Subjects" nav links from the admin navbar in 4 files:
- `admin.html`, `edit-user.html`, `add-user.html`, `logs.html`

The navbar now shows only: **Home | Logs**

The HTML pages `student-records.html` and `subjects.html` were **NOT deleted** — they still exist and are still protected by `SecurityConfig.java`. They will be re-added to the navbar when their features are implemented.

> **⚠️ WARNING — STALE NAVBARS IN `student-records.html` AND `subjects.html`:**
> These two pages still contain the OLD 4-link navbar (Home | Student Records | Subjects | Logs). Their navbars were intentionally NOT updated during this cleanup. When re-adding these pages to the admin navbar, you MUST first update their internal navbars to match the current admin navbar pattern. See `systemPatterns.md` for the current standard.

### SQL File Cleanup (April 17, 2026)
Removed leftover merge-conflict artifacts from `AnihanSRMS.sql`: dangling `ALTER TABLE` statements, a bare `main` text, and a stray closing parenthesis.

### Previous Tasks (Completed)
- Unit Test Coverage Expansion — all tests green across Admin, Account, SystemLog modules
- UI Styling Adjustments on `ui-style/fix` branch — navbar logo standardization at 85px, brand-title text removal

## Verified
- Docker MySQL schema now matches `AnihanSRMS.sql` and `User.java` entity
- `./gradlew test` → BUILD SUCCESSFUL (all tests green)
- All new test files follow existing project patterns (Mockito + `@WebMvcTest`)
