# Active Context - Anihan SRMS

## Current Phase
**Admin Bulk Load Testing & Capstone Alignment**

## Active Branch
`feature/unit-tests-coverage`

## Status (April 18, 2026)

### Admin Bulk Load Tests (April 18, 2026)
Created 2 new standalone test files to verify the Admin "View All Users" table can handle 100 users:
1. `test/service/AdminBulkLoadTest.java` — 3 tests (count, DTO field mapping, performance)
2. `test/controller/AdminBulkLoadWebMvcTest.java` — 2 tests (JSON serialization, performance)

All 100 test users generated programmatically via Mockito mocks — no database touched, no existing files modified, no new dependencies.

**Results:** 42 total tests, 0 failures, 100% success rate. Service layer: 0.008s, HTTP layer: 0.663s — well within the 5-second non-functional requirement.

### Previous Sessions
- Database Migration Fix (April 17, 2026) — applied missing `enabled`, `password_changed_at`, `system_logs` schema changes to Docker MySQL
- Admin Navbar Cleanup (April 17, 2026) — removed "Student Records" and "Subjects" nav links
- Unit Test Coverage Expansion (April 17, 2026) — AccountServiceTest, SystemLogServiceTest, AccountControllerWebMvcTest, SystemLogControllerWebMvcTest

> **⚠️ WARNING — STALE NAVBARS IN `student-records.html` AND `subjects.html`:**
> These two pages still contain the OLD 4-link navbar (Home | Student Records | Subjects | Logs). Their navbars were intentionally NOT updated during this cleanup. When re-adding these pages to the admin navbar, you MUST first update their internal navbars to match the current admin navbar pattern. See `systemPatterns.md` for the current standard.

## Verified
- Docker MySQL schema now matches `AnihanSRMS.sql` and `User.java` entity
- `./gradlew test` → BUILD SUCCESSFUL (42 tests, all green)
- All new test files follow existing project patterns (Mockito + `@ExtendWith(MockitoExtension.class)` for services, `@WebMvcTest` + `@Import(SecurityConfig.class)` for controllers)

