# Known Bugs & Technical Debt — Anihan SRMS

> **Last updated:** September 6, 2026

## Fixed Bugs (one-line summary)

- **Bug 1** ✅ `student_records.age` NOT NULL blocked enrollment — `ALTER TABLE ... MODIFY COLUMN age INT NULL`. (2026-04-30)
- **Bug 2** ✅ `StudentRecord` `@Id` mapped to `student_id` instead of `record_id` — moved `@Id @GeneratedValue(IDENTITY)` to `recordId`; repo generic `String → Integer`; added `findByStudentId()`. (2026-04-30)
- **Bug 3** ✅ `Parent` / `OtherGuardian` `@JoinColumn` FK mismatch after Bug 2 — added `referencedColumnName = "student_id"` to both. (2026-04-30)
- **Bug 6** ✅ Duplicate `requestMatchers` rules in `SecurityConfig` — consolidated into one block. (2026-04-30)
- **Bug 7** ✅ `saveDraft()` failure swallowed before submit — refactored to return `boolean`; submit blocks on failure. (2026-04-30) — *Note: `saveDraft()` was later removed entirely on 2026-05-05; see RC-2 in changeLog.*
- **Bug 8** ✅ Subjects page 500 (`GET /api/registrar/subjects` → `Unknown column 'trainer_id'`, DataTables "Ajax error"). The `Subject` entity mapped `subjects.trainer_id` but the drifted local DB had no such column (rebuilt from a pre-2026-05-09 snapshot). **Fixed by choosing Model 1** (trainer assignment is class-level only) rather than restoring the column: dropped `subjects.trainer_id` + the whole subject-trainer feature; the Subjects page now shows a derived read-only "Trainer(s)" column. Migration `2026-08-29-drop-subjects-trainer-id.sql`. See `changeLog.md` / `decisions.md` (2026-08-29). (2026-08-29)
- **Bug 9** ✅ Trainer hard-delete was unguarded — could null `classes.trainer_id` and orphan classes holding locked grades (no trainer could ever unlock them). `AdminService.hardDeleteUser` now **blocks** (400) a `ROLE_TRAINER` delete when they hold locked grades; otherwise it proceeds and the count of unassigned classes is logged. `softDeleteUser` returns a non-blocking "still assigned to N class(es)" warning. Edit Trainer dropdown shows deactivated trainers (`/trainers?includeDisabled=true`). `AdminServiceTest` +5, `AdminControllerWebMvcTest` +3; suite 340/0. See full Resolution note under Open Bugs. (2026-09-06)
- **Merge integration (2026-09-06)** ✅ After merging grade_input_fix + student-ID-number: StudentRecordDetailsResponse arity mismatch in RegistrarStudentNumberControllerWebMvcTest, and a missing @Mock GradeRepository in RegistrarStudentNumberServiceTest (7 NPEs). Both fixed test-only (1916904, fec8004). Suite: 332 tests, 0 failures.

## Open Bugs

### Bug 10 — Generated documents can't show grades: `subjects` codes ≠ curriculum module codes 🟡
- **Severity:** Medium · **Status:** Open (known, deferred by the user during the 2026-08-29 grading overhaul) · **Logged:** 2026-08-29
- **What:** Grades are keyed to the `subjects` table (`COOK-101`, `BPP-101`, …).
  The TOR / Form IX / Student Permanent Record templates carry the ~50 fixed TESDA
  modules with entirely different codes (`TRS512328`, `400311210`, …), transcribed
  separately in `static/js/curriculum-templates.js` (deliberately decoupled from
  `subjects`). `DocumentGenerationService` merges grades into the document tables
  **by subject code** (`gradesByCode[subj.code]` in `registrar-generate-document.js`
  `subjectsTable()`), so no row ever matches.
- **Effect:** the Scope-B document wiring from the grading overhaul is *correct*
  (FINAL = equivalent or status code, RE-EXAM = equivalent, Remarks = derived
  label) but **dormant** — a generated TOR/Form IX shows blank grade cells for a
  real student until this mapping exists.
- **Options (not chosen yet):** (a) create classes against real TESDA module codes
  so grades key straight to the curriculum; (b) a `subjects` ↔ curriculum-module
  mapping table; (c) accept documents stay manually filled for grades.
- Raised again by the assistant during the grading overhaul, per the user's request
  to "remind about it later / log it as a potential bug".

### Bug 11 — "Total GWA" is computed and shown to the registrar but appears on no official Anihan document 🟢
- **Severity:** Low (possible business-process contradiction, not a defect) · **Status:** Open — flagged per the user · **Logged:** 2026-08-29
- **What:** The 2026-08-29 grading overhaul added a units-weighted **Total GWA**
  (`GradeEquivalent.gwa`), surfaced in the registrar's student-record detail modal
  (`#detailsTotalGwa`). None of the client's documents — TOR, Form IX, Student
  Permanent Record — contain a GWA / GPA / grade total (Form IX totals only HOURS
  and UNITS). The user chose to **keep it for now** as a possible "nice to have"
  for internal use (honors/ranking), but asked that the mismatch with the actual
  business process be recorded.
- **If it turns out unused:** remove `totalGwa` from `StudentRecordDetailsResponse`
  + `RegistrarService` + the detail card, and `GradeEquivalent.gwa` becomes dead
  code. No migration needed (GWA is computed on read, never stored).

### Bug 9 — Trainer (user) hard-delete is silent, unguarded, and can orphan classes/locked grades ✅ RESOLVED
- **Severity:** was Medium→High · **Status:** ✅ Resolved 2026-09-06 (kept here for
  traceability; see Resolution) · **Logged:** 2026-08-29
- **Resolution (2026-09-06) — proposal points 1, 2, 3 & 5; point 4 out of scope:**
  - `AdminService.hardDeleteUser` **blocks** with `IllegalArgumentException` → HTTP
    400 when a `ROLE_TRAINER` has locked grades in ≥1 of their classes
    (*"This trainer has locked grades in N class(es). Deactivate the account
    instead, or reassign those classes first."*) — nothing is deleted and **no
    `system_logs` row is written**. New native query
    `GradeRepository.countLockedGradeClassesByTrainerId`.
  - With no locked grades the hard-delete proceeds and returns the number of
    classes the DB `ON DELETE SET NULL` just unassigned; `AdminController` appends
    *"(unassigned from N class(es))"* to the audit message and the response tells
    the admin those classes now have no trainer.
  - `AdminService.softDeleteUser` stays **non-blocking** but returns how many
    classes the deactivated trainer remains trainer-of-record for; the endpoint
    logs *"(still trainer-of-record on N class(es))"* and the JSON response
    carries a `warning` flag that `admin-users.js` renders as a held warning
    banner.
  - Edit Trainer dropdown: `GET /api/registrar/trainers?includeDisabled=true`
    (`ClassManagementService.getAllTrainers`, `TrainerResponse.enabled`) so a
    class whose trainer was deactivated still shows its current assignment,
    labelled "(deactivated)". Create Class dropdown is unchanged (active only).
  - Tests: `AdminServiceTest` +5, `AdminControllerWebMvcTest` +3 — full suite
    **340 tests, 0 failures**. The guard's native query was run against the live
    MySQL schema to confirm it is valid.
  - **Follow-ups deliberately NOT done:** (a) proposal point 4 — a registrar
    view/filter for "classes with no trainer" so orphaned classes are
    discoverable; (b) mark deactivated `<option>`s `disabled` (except the
    currently-selected one) so re-saving a class without changing the trainer
    can't hit `updateClassTrainer`'s existing "trainer account is disabled" 400.
- *Original analysis retained below for reference.*
- **Where:** `AdminService.hardDeleteUser()` / `softDeleteUser()`,
  `AdminController` DELETE endpoints. DB FKs: `classes.trainer_id`
  (`classes_ibfk_3`, `ON DELETE SET NULL`) and `subjects.trainer_id`
  (`fk_subjects_trainer`, `ON DELETE SET NULL` — currently absent on this machine,
  see Bug 8). `grades` has **no** trainer FK.
- **What happens today:**
  - **Hard delete** (`DELETE /api/admin/users/{id}/permanent` →
    `userRepository.delete(user)`): **no pre-check of any kind.** The DB
    `ON DELETE SET NULL` rules make the delete always succeed; every
    `classes.trainer_id` / `subjects.trainer_id` that pointed at the deleted
    trainer is silently set to `NULL`. Grade rows (including **locked** grades the
    deleted trainer entered) are untouched — `grades` has no trainer FK.
  - The `system_logs` row says only `"Permanently deleted account: X"` — nothing
    about "N classes / M subjects were unassigned."
  - **Consequence:** a class with `trainer_id = NULL` that still holds locked
    grades is orphaned. Every ownership check in `TrainerGradeService` /
    `TrainerService` is `getTrainer() == null || !...equals(trainerId)`, so **no
    trainer can ever unlock or edit those grades again.** Only recovery is the
    registrar manually reassigning a trainer via Edit Trainer on `classes.html`,
    and nothing signals that this is needed.
  - **Soft delete** (`enabled = false`, the default path) does **not** unassign
    anything — the deactivated trainer stays trainer-of-record on subjects/classes,
    yet `GET /api/registrar/trainers` is enabled-only, so the Edit Trainer dropdown
    can't offer them and the modal's "current trainer" can render blank/stale.
  - `system_logs.user_id` becomes a dangling integer (no FK — audit trail is
    intentionally preserved; this part is by design and fine).
- **Not the bug:** a `NULL`/unassigned trainer never NPEs — every `.getTrainer()`
  call site is null-guarded (`SubjectResponse`, `ClassResponse`, `TrainerService`,
  `TrainerGradeService`). The gap is the missing guard/warning around the delete.
- **Contrast:** `deleteSection()` already pre-checks (`existsBySectionSectionCode`
  → 400 with an actionable message). User deletion has no equivalent.

**Proposed solution:**
1. **Pre-check in `AdminService.hardDeleteUser()`** when `user.getRole()` is
   `ROLE_TRAINER`:
   - New `SchoolClassRepository.countByTrainerUserId(Integer)` and
     `SubjectRepository.countByTrainerUserId(Integer)` (skip the subjects one if
     Bug 8 is resolved by dropping `subjects.trainer_id`).
   - New count of **locked** grades owned via the trainer's classes — native query
     joining `grades` → `classes` on `class_id` where `classes.trainer_id = :id
     AND grades.locked = 1`.
   - **Locked grades exist → block the hard delete**, `IllegalArgumentException`
     → 400: *"This trainer has locked grades in N class(es). Deactivate the account
     instead, or reassign those classes first."*
   - **Otherwise assigned to classes/subjects → allow, but** return the affected
     counts and write them into the `system_logs` message: *"Permanently deleted
     account: X (unassigned from N class(es), M subject(s))"*.
2. Keep the DB `ON DELETE SET NULL` as defence-in-depth — the pre-check just turns
   a silent cascade into an informed one.
3. **Soft delete of a `ROLE_TRAINER`:** surface a non-blocking warning listing the
   N classes / M subjects they remain assigned to. Optionally include disabled
   trainers in the Edit Trainer dropdown labelled "(deactivated)" so a registrar
   can see and replace the current assignment.
4. **Visibility:** add a registrar view / filter for "classes with no trainer" so
   orphaned classes are discoverable.
5. Add service + WebMvc tests mirroring the existing `deleteSection` FK-guard tests.

### Bug 4 — Student email not collected during enrollment 🟡
- **Severity:** Medium · **Status:** Open (needs product decision)
- **Where:** `StudentDetailsRequest`, `StudentDetailsResponse`, `student-details.html`, `student-details.js`
- **What:** `student_records.email` exists in DB and entity, but the enrollment wizard never reads or writes it — always `NULL` after enrollment.
- **Decision needed:** registrar fills it in post-enrollment, or add email input to Step 1?

### Bug 5 — `AgeCalculator` returns 0 instead of null for missing birthdate 🟢 (resolved 2026-05-05)
- **Status:** ✅ Fixed in the May 5 enrollment flow fix (RC-5). `AgeCalculator.calculateAge` now returns `Integer` and returns `null` for null birthdate. Kept here for traceability.

## Technical Debt

- **TD-1** — No unit/integration tests for `StudentDetailsService`, `StudentDetailsController`, `StudentPortalController`, `StorageService`, `ClassManagementService`, `ClassManagementController`. *(Partially addressed 2026-05-05: `StudentDetailsServiceTest` added with 7 tests.)*
- **TD-2** — `student-records.html` still has the old admin 4-link navbar. Update internal navbar before re-enabling. *(`subjects.html` was rebranded to registrar on 2026-05-01.)*
- **TD-3** — Orphan legacy tables in live DB (`classess`, `log`, `previous_school`, `qualification_assessment`) — no JPA entities reference them; harmless but unused. *(Dropped on 2026-05-02.)*
