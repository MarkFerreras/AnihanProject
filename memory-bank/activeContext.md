# Active Context - Anihan SRMS

## Current Phase
**Registrar Enhancements — Status Filter + OJT/TESDA/SchoolYears on Edit Form (2026-05-06)**

## Active Branch
`feature/registrar-fixes`

## Latest Session (May 6, 2026 — Registrar Enhancements)

### Features Implemented
1. **Status filter on `registrar.html`** — new `<select id="studentStatusFilter">` in the filter bar (All / Enrolling / Submitted / Active / Graduated). `buildAjaxUrl()` appends `?status=` when non-empty; Reset clears the select. Backend: `RegistrarController` forwards `status` param; `RegistrarService.getAllRecords` gains a 4-arg overload with case-insensitive status filter. Older 2-arg and 3-arg overloads delegate to the new one.

2. **OJT, TESDA, SchoolYears on `student-records.html` edit form** — three new sections added to the form. `RegistrarService.updateRecord()` is now `@Transactional` and persists OJT (upsert/delete), TESDA (delete-all-insert-new, with flush to avoid unique-constraint race), and SchoolYears (delete-all-insert-new with reassigned rowIndex). `getRecordById()` loads all three collections and passes them to the response factory. `StudentRecordDetailsResponse` and `StudentRecordUpdateRequest` each gained three new fields (`ojt`, `tesdaQualifications`, `schoolYears`).

### Files Changed
| File | Change |
|------|--------|
| `dto/registrar/StudentRecordDetailsResponse.java` | Added 3 new fields; new 4-arg `from()` factory; old 1-arg `from()` delegates |
| `dto/registrar/StudentRecordUpdateRequest.java` | Added `ojt`, `tesdaQualifications`, `schoolYears` fields (all optional) |
| `service/RegistrarService.java` | New 7-arg constructor (+3 repos); 4-arg `getAllRecords`; `matchesStatus()`; `@Transactional` `updateRecord` with OJT/TESDA/SchoolYear helpers; `buildDetailsResponse()` helper |
| `controller/RegistrarController.java` | Added `status` `@RequestParam` forwarded to service |
| `test/service/RegistrarBulkLoadTest.java` | Added 3 new `@Mock` repos; added `statusFilterRestrictsResultsByStudentStatus` test |
| `test/controller/RegistrarBulkLoadWebMvcTest.java` | Updated all 3 mock stubs from 3-arg to 4-arg `getAllRecords` |
| `static/registrar.html` | Added status `<select>` to filter bar |
| `static/js/registrar-students.js` | Extended `buildAjaxUrl()` + Reset handler for status |
| `static/student-records.html` | Added OJT section, 3-slot TESDA fieldsets, SchoolYears dynamic table; bumped JS to `?v=2` |
| `static/js/registrar-student-records-edit.js` | Added OJT/TESDA/SchoolYear `populateForm`, `buildPayload`, row handlers, `esc()`, `createSchoolYearRow()`, form-level dirty delegation |

### Verified
- `./gradlew test` → BUILD SUCCESSFUL (all tests pass, no regressions)

## Previous Session (May 5, 2026 — Enrollment Flow Fix)
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
