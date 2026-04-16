# Active Context - Anihan SRMS

## Current Phase
**Unit Test Coverage Expansion — Complete, Verified**

## Active Branch
`feature/unit-tests-coverage`

## Status (April 17, 2026)
All previously untested modules (Account, SystemLog) now have comprehensive JUnit 5 tests.
`./gradlew test` passes with all tests green across Admin, Account, and SystemLog modules.

### What Was Built
- **AccountServiceTest** (10 tests): Covers `updateUsername` (5 edge cases), `updatePassword` (5 edge cases), `updatePersonalDetails` (2 cases)
- **SystemLogServiceTest** (4 tests): Covers `logAction` (including null userId), `getAllLogs` (including empty list)
- **AccountControllerWebMvcTest** (7 tests): Covers `/api/account/profile`, `/api/account/password`, `/api/account/details` endpoints — validation, wrong password, unauthenticated access
- **SystemLogControllerWebMvcTest** (5 tests): Covers `/api/logs` — ADMIN-only access (403 for registrar/trainer, 401 for unauthenticated)

### Previous Task (Completed)
UI Styling Adjustments on `ui-style/fix` branch — navbar logo standardization at 85px, brand-title text removal.

## Verified
- `./gradlew test` → BUILD SUCCESSFUL (all tests green)
- All new test files follow existing project patterns (Mockito + `@WebMvcTest`)
