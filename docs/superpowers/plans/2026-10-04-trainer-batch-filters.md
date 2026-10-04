# Trainer Class List — Batch Year & Batch Filters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Batch Year and Batch filters (ANDed with the existing Semester filter) to the trainer's Class List page.

**Architecture:** `classes → sections → batches` already links every class to a batch, so no schema change. The service filters the trainer's own classes in memory (same as the semester filter today); a new `/batches` endpoint feeds the dropdowns, derived from those same classes (no new repository query).

**Tech Stack:** Spring Boot 4 / JUnit 5 / Mockito / MockMvc; jQuery 4 + DataTables 2 (static HTML/JS).

**Spec:** Approved in-chat design (2026-10-04, branch `batch-filters`); no spec file. Decisions: Class List page only; "year" = `batches.batch_year`; server-side, all filters ANDed; year and batch dropdowns are independent (no cascade); `trainer-subjects.html` untouched.

## Global Constraints

- Work only on branch `batch-filters`; never commit to `main`.
- No schema/migration changes; no `system_logs` writes (read-only filters).
- Every query is scoped to the current trainer (`resolveCurrentTrainerId()`); a trainer must never see another trainer's batches/classes.
- Blank/absent filter param = no filter (same as `semester` today).
- `batchYear` is `Short` (matches `Batch.batchYear`).
- Existing tests build `Section` objects with **no `Batch`** — `buildClassRow` and the batches list must be null-safe.
- Suite baseline: 578 tests, 0 failures (`./gradlew test`).

## Review Focus

- Class whose section has no batch → row returns with null `batchCode`/`batchYear`, no NPE (Task 1).
- Another trainer's batch never appears in `/batches` (Task 1).
- Blank `batchYear`/`batchCode` params behave as "all" (Task 2).
- Two batches sharing a code is impossible (PK), but one batch with many classes must appear once in `/batches` (Task 1).
- Selected filter values survive a dropdown repopulate (Task 3).

## Files

- Modify `src/main/java/com/example/springboot/dto/trainer/TrainerClassResponse.java` — add `batchCode`, `batchYear`.
- Create `src/main/java/com/example/springboot/dto/trainer/TrainerBatchResponse.java` — `record TrainerBatchResponse(String batchCode, Short batchYear)`.
- Modify `src/main/java/com/example/springboot/service/TrainerService.java` — filtering + `getMyBatches()`.
- Modify `src/main/java/com/example/springboot/controller/TrainerController.java` — new params + endpoint.
- Modify `src/main/resources/static/trainer-classes.html`, `src/main/resources/static/js/trainer-classes.js` — dropdowns, column, unified reload.
- Test: `service/TrainerServiceTest.java`, `controller/TrainerControllerWebMvcTest.java`.
- Memory bank: `progress.md`, `changeLog.md`, `activeContext.md`, `testing.md` (top entries only).

---

### Task 1: Service — batch fields, filters, batches list

**Files:** Modify `TrainerClassResponse.java`, create `TrainerBatchResponse.java`, modify `TrainerService.java`; test `TrainerServiceTest.java`.

**Interfaces:**
- Produces: `TrainerClassResponse(Integer classId, String sectionCode, String sectionName, String subjectCode, String subjectName, String courseName, String semester, String batchCode, Short batchYear, long enrolledCount)` (batch fields inserted before `enrolledCount`).
- Produces: `List<TrainerClassResponse> TrainerService.getMyClasses(String semester, Short batchYear, String batchCode)`; existing `getMyClasses(String semester)` stays and delegates with `(semester, null, null)`.
- Produces: `List<TrainerBatchResponse> TrainerService.getMyBatches()` — distinct by `batchCode`, sorted `batchYear` desc then `batchCode` asc; classes whose section has no batch are skipped.

- [ ] **Step 1: Write failing tests** in `TrainerServiceTest` (add helper `classInBatch(int id, String semester, String batchCode, Short year)` next to `classForYear`, setting a `Batch` on the section). Tests:
  - `getMyClassesFiltersByBatchYear` — classes in batches (B1,2025),(B2,2026); `getMyClasses(null, (short) 2026, null)` → 1 row, `batchCode()=="B2"`, `batchYear()==2026`.
  - `getMyClassesFiltersByBatchCode` — `getMyClasses(null, null, "B1")` → 1 row `"B1"`.
  - `getMyClassesCombinesSemesterYearAndBatch` — semester "2026" + year 2026 + code "B2" returns only the class matching all three; a class matching two of three is excluded.
  - `getMyClassesBlankBatchCodeMeansNoFilter` — `getMyClasses(null, null, "  ")` → all rows.
  - `getMyClassesToleratesSectionWithoutBatch` — use existing `classForYear` (no batch); result row has `batchCode()==null`, `batchYear()==null`.
  - `getMyBatchesReturnsDistinctSortedBatchesForTrainerOnly` — stub `classRepository.findByTrainerUserId(42)` with 3 classes in (B1,2025),(B2,2026),(B2,2026) plus one with no batch; expect `[B2/2026, B1/2025]`; `verify(classRepository, never()).findAll()`.
- [ ] **Step 2: Run** `./gradlew test --tests "*TrainerServiceTest"` — Expected: compile FAIL (new signatures missing).
- [ ] **Step 3: Implement** the interfaces above. `buildClassRow` reads batch via `c.getSection().getBatch()` null-guarded. Filter with stream predicates after the existing semester filter; compare `batchYear` with `Objects.equals`, `batchCode` with `equals` after `isBlank()` check. `getMyBatches` maps `findByTrainerUserId` → `Section.getBatch()` non-null → `TrainerBatchResponse`, `distinct()` (records have value equality), sort.
- [ ] **Step 4: Fix existing call sites in this file** — constructor calls of `TrainerClassResponse` in tests (if any) get the two new args; run `./gradlew test --tests "*TrainerServiceTest"` — Expected: PASS.
- [ ] **Step 5: Commit** — `git add -A src && git commit -m "feat: trainer class batch year/code filters and batches list (service)"`

### Task 2: Controller — params and `/batches` endpoint

**Files:** Modify `TrainerController.java`; test `TrainerControllerWebMvcTest.java`.

**Interfaces:**
- Consumes: Task 1 `getMyClasses(String, Short, String)`, `getMyBatches()`.
- Produces: `GET /api/trainer/classes?semester=&batchYear=&batchCode=` (all optional); `GET /api/trainer/classes/batches` → `List<TrainerBatchResponse>`. Controller always calls the 3-arg `getMyClasses`.

- [ ] **Step 1: Update + add failing tests.** The 3 existing tests that stub/verify `getMyClasses(x)` (`getMyClassesReturnsList`, `...PassesExplicitSemesterToService`, `...PassesBlankSemesterToService`) change to the 3-arg form `(x, null, null)`; also fix the `TrainerClassResponse` constructor in `getMyClassesReturnsList` for the two new args. Add:
  - `getMyClassesPassesBatchFiltersToService` — GET with `batchYear=2026&batchCode=B2` → `verify(service).getMyClasses(null, (short) 2026, "B2")`.
  - `getMyClassesTreatsBlankBatchYearAsAbsent` — `batchYear=` (empty) → status 200 and `getMyClasses(null, null, null)` (Spring binds empty `Short` param to null).
  - `getMyBatchesReturnsBatches` — stub `getMyBatches()` → `[{"batchCode":"B2","batchYear":2026}]`; assert `$[0].batchCode`, `$[0].batchYear`.
  - `getMyBatchesReturns403ForNonTrainer` — mirror `getMyClassesReturns403ForNonTrainer`.
- [ ] **Step 2: Run** `./gradlew test --tests "*TrainerControllerWebMvcTest"` — Expected: FAIL.
- [ ] **Step 3: Implement** — add `@RequestParam(value="batchYear", required=false) Short batchYear` and `batchCode` to `getMyClasses`; add `@GetMapping("/classes/batches")` method. Place it before `/classes/{classId}/students` (no path clash, but keep grouped with `/classes/semesters`).
- [ ] **Step 4: Run** the same command — Expected: PASS. Then full `./gradlew test` — Expected: 578 + new tests, 0 failures.
- [ ] **Step 5: Commit** — `git commit -am "feat: expose batch filters and /classes/batches on trainer API"`

### Task 3: Frontend — dropdowns, column, unified reload; memory bank

**Files:** Modify `trainer-classes.html` (filter header at lines ~93-98, table header ~104), `trainer-classes.js` (`loadAvailableSemesters`, `bindSemesterFilter`, `columns`, `order`).

**Interfaces:**
- Consumes: Task 2 endpoints. Element ids: `#batchYearFilterSelect` ("All Years"), `#batchFilterSelect` ("All Batches"); existing `#semesterFilterSelect`.
- Produces: `reloadClasses()` — builds the query string from the three selects (omit empty values) and calls `classesTable.ajax.url('/api/trainer/classes' + qs).load()`.

- [ ] **Step 1: HTML** — beside the semester select, add the two `form-select form-select-sm w-auto` selects (labels "Batch Year:", "Batch:", same `filter-label me-2` style; wrap the three label+select pairs with `gap-3 flex-wrap`). Add `<th>Batch</th>` after Semester.
- [ ] **Step 2: JS** — add `{ data: 'batchCode', render: v => v || '<em class="text-muted">—</em>' }` after `semester` in `columns` and shift `order` indexes (`[[0,'desc'],[3,'asc'],[4,'asc']]`). Replace `bindSemesterFilter` with a single handler on all three selects (`change.classFilter`) calling `reloadClasses()`. Add `loadAvailableBatches()` populating years (distinct `batchYear`, desc) and codes from `/api/trainer/classes/batches`, preserving the current selection like `loadAvailableSemesters` does.
- [ ] **Step 3: Manual check** — `./gradlew bootRun`, log in as `trainer`: each filter narrows the table alone; two/three together AND; "All" resets; Batch column shows; a trainer with no assigned classes sees empty dropdowns and "No classes assigned." (Verify against the real DB if the migration/data exists; otherwise say "unverified in browser".)
- [ ] **Step 4: Memory bank** — add top entries to `progress.md`, `changeLog.md`, `activeContext.md` (branch `batch-filters`, files above), and `testing.md` (new test count). Add to `decisions.md` only: "`/batches` derived from the trainer's classes, not a new repository query."
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: batch year and batch filters on trainer class list; docs"`

---

## Self-review notes

- Coverage: every approved-design item maps to a task (DTO fields, params, `/batches`, UI, tests, memory bank). Deviation: no new repository query — batches derive from `findByTrainerUserId`, since the service already loads those classes; same trainer scoping, one fewer moving part.
- Signature consistency: `getMyClasses(String, Short, String)` and `getMyBatches()` used identically in Tasks 1–2; `TrainerClassResponse` field order is defined once in Task 1.
- Risk: existing tests construct sections without a `Batch` — null-safety is pinned by `getMyClassesToleratesSectionWithoutBatch`.
