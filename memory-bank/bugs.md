# Known Bugs & Technical Debt — Anihan SRMS

> **Last updated:** April 30, 2026
> **Updated by:** Antigravity (automated audit)

This file tracks all known bugs and technical debt discovered during code audits.
It is intended for all developers on the project. Items marked **Open** have not been fixed yet.

---

## Fixed Bugs (Resolved)

### Bug 1 — `student_records.age` NOT NULL Blocks Enrollment ✅
- **Fixed:** April 30, 2026 | **Branch:** `feature/student-details`
- **What:** DB column `age` was `NOT NULL` with no default, but `startOrResume()` never sets `age` on creation → `DataIntegrityViolationException` on INSERT.
- **Fix:** `ALTER TABLE student_records MODIFY COLUMN age INT NULL` + updated `AnihanSRMS.sql` and `schema.sql`.

### Bug 2 — `StudentRecord` JPA `@Id` pointed to `student_id` instead of `record_id` ✅
- **Fixed:** April 30, 2026 | **Branch:** `feature/student-details`
- **What:** `@Id` was on `studentId` (String, manually assigned) instead of `recordId` (Integer, auto-increment PK). JPA called `merge()` instead of `persist()`.
- **Fix:** Moved `@Id @GeneratedValue(IDENTITY)` to `recordId`, demoted `studentId` to `@Column(unique=true)`, updated `StudentRecordRepository` generic from `String` to `Integer`, added `findByStudentId()`.

### Bug 3 — `Parent` / `OtherGuardian` `@JoinColumn` FK mismatch after Bug 2 fix ✅
- **Fixed:** April 30, 2026 | **Branch:** `fix/db-sync-username-unique`
- **What:** After Bug 2 moved `@Id` to `record_id`, the `@ManyToOne @JoinColumn(name = "student_id")` in `Parent.java` and `OtherGuardian.java` defaulted to joining `record_id` (INT) instead of `student_id` (VARCHAR). This caused a type mismatch with the DB FK which references `student_records.student_id`.
- **Fix:** Added `referencedColumnName = "student_id"` to both entities.

### Bug 6 — Duplicate security authorization rules in `SecurityConfig` ✅
- **Fixed:** April 30, 2026 | **Branch:** `fix/db-sync-username-unique`
- **What:** `/admin.html`, `/registrar.html`, `/trainer.html` had duplicate `requestMatchers` rules. The first match wins in Spring Security, so the second set was dead code.
- **Fix:** Consolidated into a single clean block with HTML pages and API endpoints clearly separated.

### Bug 7 — `saveDraft()` failure silently swallowed before submit ✅
- **Fixed:** April 30, 2026 | **Branch:** `fix/db-sync-username-unique`
- **What:** On submit, `saveDraft()` was called in a try-catch that swallowed errors. If the save failed, submission proceeded with stale server-side data, potentially losing the student's last edits.
- **Context:** Students only have one session to enter enrollment data — they cannot return unless granted access by the registrar.
- **Fix (Option A — keep saveDraft, block submit on failure):**
  1. `saveDraft()` refactored to return `true`/`false` instead of swallowing errors internally
  2. Submit handler now calls `saveDraft()` **after** validation and **blocks** submission if it returns `false`, showing a clear error: *"Your data could not be saved. Please check your connection and try again."*
  3. "Next" button still uses best-effort `saveDraft()` (ignores return value) for browser crash recovery
- **Files changed:** `student-details.js` (submit handler + saveDraft function)

---

## Open Bugs (Not Yet Fixed)
- **Severity:** Medium
- **Status:** Open — needs product decision
- **Location:** `StudentDetailsRequest.java`, `StudentDetailsResponse.java`, `student-details.html`, `student-details.js`
- **What:** The `StudentRecord` entity has an `email` field (mapped to `student_records.email VARCHAR(255) NULL` in the DB), but:
  - `StudentDetailsRequest` does not include an `email` field
  - `StudentDetailsResponse` does not include an `email` field
  - `student-details.html` has no email input for the student
  - `student-details.js` does not read or write the student's email
- **Impact:** The student's personal email is never collected during enrollment. It will always be `NULL` in the DB.
- **Decision needed:** Is this intentional (to be filled by registrar post-enrollment)? Or should it be added to the enrollment wizard?
- **Suggested fix (if needed):**
  1. Add `String email` to `StudentDetailsRequest` and `StudentDetailsResponse`
  2. Add an email input to Step 1 of `student-details.html`
  3. Add `email` to `buildPayload()` and `populateForm()` in `student-details.js`
  4. Add `if (req.email() != null) r.setEmail(req.email().trim());` to `applyPersonal()` in `StudentDetailsService`

### Bug 5 — `AgeCalculator.calculateAge()` Returns 0 Instead of Null for Missing Birthdate 🟡
- **Severity:** Medium
- **Status:** Open — needs code fix
- **Location:** `com.example.springboot.service.AgeCalculator` (line 22–24)
- **What:** The method returns `int` (primitive), so when birthdate is `null` it returns `0` instead of `null`. This means a student whose birthdate hasn't been entered will have `age = 0` stored in the DB.
- **Impact:** Misleading data — a student with no birthdate shows as "0 years old" rather than having no age value.
- **Current code:**
  ```java
  public static int calculateAge(LocalDate birthdate) {
      if (birthdate == null) {
          return 0;  // ← Should be null
      }
      return Period.between(birthdate, LocalDate.now()).getYears();
  }
  ```
- **Suggested fix:**
  ```java
  public static Integer calculateAge(LocalDate birthdate) {
      if (birthdate == null) {
          return null;
      }
      return Period.between(birthdate, LocalDate.now()).getYears();
  }
  ```

---

## Technical Debt

### TD-1 — No Unit/Integration Tests for Student Enrollment Module
- **Status:** Open
- **What:** The `StudentDetailsService`, `StudentDetailsController`, `StudentPortalController`, and `StorageService` have zero test coverage. All current JUnit tests cover only the Account and System Log modules.
- **Risk:** Regressions in enrollment logic (like Bugs 1–3) won't be caught automatically.

### TD-2 — Stale Navbars on `student-records.html` and `subjects.html`
- **Status:** Open (deferred)
- **What:** These pages still use the old pre-standardization navbar. They need to be updated to the new standard (Home | Logs) pattern when the pages are re-enabled.

### TD-3 — Orphan Legacy Tables in Live DB
- **Status:** Open (low priority)
- **What:** `classess`, `log`, `previous_school`, `qualification_assessment` exist in the live DB but are not referenced by any JPA entity. They are remnants from an earlier schema version.
- **Risk:** None currently — they just take up space.
