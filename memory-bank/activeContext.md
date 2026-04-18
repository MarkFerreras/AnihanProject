# Active Context - Anihan SRMS

## Current Phase
**System Logs Date Filtering Enhancement**

## Active Branch
`feature/logs-date-filter`

## Status (April 18, 2026)

### System Logs Date Filtering (April 18, 2026)
Implemented server-side date filtering for the Admin System Logs page:
- Extended `GET /api/logs` with optional `rangeDays`, `startDate`, `endDate` query parameters
- Default view shows only the last 7 days of logs (no more full-table fetch)
- Quick filter presets: 7 days, 14 days, 30 days
- Custom inclusive From/To date range with Apply/Reset actions
- Invalid date ranges (startDate > endDate) return HTTP 400
- All filtering happens in the database via `findByTimestampBetweenOrderByTimestampDesc()`
- Frontend filter toolbar with preset pills and custom date inputs

**Results:** 52 total tests, 0 failures, 100% success rate. BUILD SUCCESSFUL in 19.998s.

### Previous Sessions
- Admin Bulk Load Tests (April 18, 2026) — 5 new standalone tests verifying 100-user table handling
- Database Migration Fix (April 17, 2026) — applied missing schema changes to Docker MySQL
- Admin Navbar Cleanup (April 17, 2026) — removed "Student Records" and "Subjects" nav links
- Unit Test Coverage Expansion (April 17, 2026) — AccountServiceTest, SystemLogServiceTest, AccountControllerWebMvcTest, SystemLogControllerWebMvcTest

> **⚠️ WARNING — STALE NAVBARS IN `student-records.html` AND `subjects.html`:**
> These two pages still contain the OLD 4-link navbar (Home | Student Records | Subjects | Logs). Their navbars were intentionally NOT updated during this cleanup. When re-adding these pages to the admin navbar, you MUST first update their internal navbars to match the current admin navbar pattern. See `systemPatterns.md` for the current standard.

## Verified
- `./gradlew test` → BUILD SUCCESSFUL (52 tests, all green)
- No references to removed `getAllLogs()` method remain in the codebase
- All new test files follow existing project patterns (Mockito + `@ExtendWith(MockitoExtension.class)` for services, `@WebMvcTest` + `@Import(SecurityConfig.class)` for controllers)
