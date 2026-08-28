# Known Bugs & Technical Debt — Anihan SRMS

> **Last updated:** August 29, 2026

## Fixed Bugs (one-line summary)

- **Bug 1** ✅ `student_records.age` NOT NULL blocked enrollment — `ALTER TABLE ... MODIFY COLUMN age INT NULL`. (2026-04-30)
- **Bug 2** ✅ `StudentRecord` `@Id` mapped to `student_id` instead of `record_id` — moved `@Id @GeneratedValue(IDENTITY)` to `recordId`; repo generic `String → Integer`; added `findByStudentId()`. (2026-04-30)
- **Bug 3** ✅ `Parent` / `OtherGuardian` `@JoinColumn` FK mismatch after Bug 2 — added `referencedColumnName = "student_id"` to both. (2026-04-30)
- **Bug 6** ✅ Duplicate `requestMatchers` rules in `SecurityConfig` — consolidated into one block. (2026-04-30)
- **Bug 7** ✅ `saveDraft()` failure swallowed before submit — refactored to return `boolean`; submit blocks on failure. (2026-04-30) — *Note: `saveDraft()` was later removed entirely on 2026-05-05; see RC-2 in changeLog.*
- **Bug 8** ✅ Subjects page 500 (`GET /api/registrar/subjects` → `Unknown column 'trainer_id'`, DataTables "Ajax error"). The `Subject` entity mapped `subjects.trainer_id` but the drifted local DB had no such column (rebuilt from a pre-2026-05-09 snapshot). **Fixed by choosing Model 1** (trainer assignment is class-level only) rather than restoring the column: dropped `subjects.trainer_id` + the whole subject-trainer feature; the Subjects page now shows a derived read-only "Trainer(s)" column. Migration `2026-08-29-drop-subjects-trainer-id.sql`. See `changeLog.md` / `decisions.md` (2026-08-29). (2026-08-29)

## Open Bugs

### Bug 9 — Trainer (user) hard-delete is silent, unguarded, and can orphan classes/locked grades 🟡→🔴
- **Severity:** Medium today (0 classes/grades), High once real classes exist ·
  **Status:** Open — analysis complete, solution proposed below, not implemented ·
  **Logged:** 2026-08-29
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
