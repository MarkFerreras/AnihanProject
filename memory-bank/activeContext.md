# Active Context - Anihan SRMS

## Current Phase
**Student Portal Enrollment Flow Fix (2026-05-05)**

## Active Branch
`fix/student-portal-flow` (created from `main`)

## Latest Session (May 5, 2026 — Enrollment Flow Fix)
Fixed 5 root-cause bugs in the student enrollment flow. All changes scoped strictly
to student-portal and student-details (no Registrar/Trainer code touched).

### Root Causes Fixed
1. **RC-1 (CRITICAL): Premature DB persistence** — `startOrResume()` was saving full
   student data on portal start. Now only name + status are persisted (minimal record
   needed for upload FK constraints).
2. **RC-2 (CRITICAL): saveDraft on every Next click** — Removed. Data stays in browser
   sessionStorage until final submit. No intermediate DB writes.
3. **RC-3 (MEDIUM): Single transactional submit** — New `submitEnrollment()` method
   persists student record + parents + guardian + education + school years atomically in
   one `@Transactional` block. Old two-step saveDraft/submit flow is gone.
4. **RC-4: OJT/TESDA removal from student flow** — Removed from HTML, JS (buildPayload,
   populateForm), request DTO, response DTO, and service layer. Entities/repos kept
   for Registrar/Trainer use.
5. **RC-5 (LOW): AgeCalculator null handling** — Returns `Integer null` instead of
   `int 0` when birthdate is null. Prevents misleading `age=0` in DB.

### Files Changed
| File | Change |
|------|--------|
| `StudentDetailsService.java` | Rewrote: removed saveDraft/old submit; added submitEnrollment; removed OJT/TESDA handling |
| `StudentDetailsController.java` | Removed PUT saveDraft endpoint; new POST submit with full payload |
| `StudentDetailsRequest.java` | Added name fields; removed OJT/TESDA |
| `StudentDetailsResponse.java` | Removed OJT/TESDA |
| `student-details.js` | Removed saveDraft calls; removed OJT/TESDA from payload/populate; submit sends full body |
| `student-details.html` | Removed OJT/TESDA HTML sections; bumped JS cache to v=4 |
| `AgeCalculator.java` | Return type `int` → `Integer`, null → null |
| `AgeCalculatorTest.java` | Updated: assertEquals(0) → assertNull |
| `StudentDetailsServiceTest.java` | **NEW** — 7 tests covering start, resume, submit, double-submit, load |

### Files NOT Changed (scope boundary)
- All Registrar/Trainer controllers, services, HTML, JS
- OJT/TESDA entities + repositories (kept for Registrar use)
- AdminService, RegistrarService, AccountService

## Verified
- Full test suite: BUILD SUCCESSFUL (all tests pass)
- New StudentDetailsServiceTest: 7 tests pass
- AgeCalculatorTest: 6 tests pass (updated for null return)
- No regressions in existing AdminServiceTest, AccountServiceTest, etc.

## Previous Session (May 5, 2026 — Schema Drift)
- Applied migration `2026-05-05-fix-schema-drift.sql`
- Deleted DataSeeder.java
- 82/82 tests passed

## Open Items
- **Data cleanup consideration:** Existing "Enrolling" records in production DB from
  the old premature-create behavior may need review/cleanup
- **Upload flow:** Uploads still require a minimal DB record to exist (FK constraint).
  The current approach creates a name-only record on start, which is the minimum viable.
- **E2E browser testing:** Not yet done. Consider testing the full portal → details →
  submit flow in a browser.
