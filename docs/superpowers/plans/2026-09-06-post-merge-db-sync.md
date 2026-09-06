# Post-Merge Bug Fix + Live DB Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Get `main` compiling and its full test suite green again after the `grade_input_fix` + `student-ID-number` merges, then bring the live `AnihanSRMS` MySQL database into line with the updated `src/main/sql/schema.sql` by applying the five outstanding migrations.

**Architecture:** `main` currently fails `compileTestJava` because one WebMvc test from the `student-ID-number` branch builds `StudentRecordDetailsResponse` with the pre-overhaul field count — the `grade_input_fix` merge added a trailing `BigDecimal totalGwa` component. Fix that one test, confirm the suite, then apply the migrations `2026-08-26-subjects-competency-type.sql`, `2026-08-26-subjects-code-update-cascade.sql`, `2026-08-27-add-student-number.sql`, `2026-08-29-drop-subjects-trainer-id.sql`, `2026-08-29-grades-overhaul.sql` to the live DB (which is still on the pre-2026-08-26 schema). The four existing test grade rows are deleted first (user decision) so the grades overhaul runs against an empty table, matching its own header assumption. Finish with a structural diff of live vs `schema.sql` and a Hibernate `ddl-auto=validate` boot.

**Tech Stack:** Java 25, Spring Boot 4.0, Gradle 9.4.1 (Kotlin DSL), MySQL 8 in Docker (container `mysql-server`, `root` / `my_password`), JPA DDL `none`.

**Branch:** Work stays on `main` (user decision — DB-sync task, one-line test fix). Do **not** create a feature branch. Commit code + memory-bank changes together per the project Memory Update Protocol.

---

## Context the implementer needs

- **DB access:** `docker exec -i mysql-server mysql -uroot -pmy_password AnihanSRMS < file.sql` to apply a script; `docker exec mysql-server mysql -uroot -pmy_password -e "SQL"` for one-off queries. Every migration file contains its own `USE AnihanSRMS;` and an idempotent `information_schema` guard block — re-running one is a no-op.
- **Live DB state right now (verified read-only):**
  - `grades`: has `midterm_grade`, `finals_grade`, `hours_studied`; **4 rows** (`grade_id` 1–4, none `locked`). Missing `final_percentage`, `re_exam_percentage`, `grade_status`, `hours_rendered`.
  - `subjects`: has `trainer_id`; `qualification_code` is `NOT NULL`; no `competency_type` column.
  - `student_records`: no `student_number` column.
  - `grades`→`subjects` FK is `grades_ibfk_2` (`ON UPDATE NO ACTION`); `classes`→`subjects` FK is `classes_ibfk_2` (`ON UPDATE NO ACTION`).
  - 19 tables total; `student_records` 10 rows, `classes` 8, `subjects` 6, `system_logs` 323.
- **Why the migrations are safe to apply in date order:** they touch disjoint objects (`subjects` columns / `subjects` FK rule / `student_records` column / `subjects.trainer_id` drop / `grades` columns). The only ordering that matters is *delete the 4 grade rows before the grades overhaul*.
- **`schema.sql` already reflects the target state** in its table bodies. Only its header changelog and its "existing databases should also run…" notes are missing the two 2026-08-29 migrations — a doc fix in Task 9.

---

## File Structure

| File | Responsibility | Touched in |
|---|---|---|
| `src/test/java/com/example/springboot/controller/RegistrarStudentNumberControllerWebMvcTest.java` | WebMvc test for `PUT …/student-number`. Its `details(...)` helper builds a `StudentRecordDetailsResponse` and is one arg short after the merge. | Task 1 |
| `src/main/sql/migrations/2026-08-29-grades-overhaul.sql` | Grades table structural overhaul. Header comment wrongly claims "0 rows"; does not shrink `remarks` to match `schema.sql`. | Task 5 |
| Live `AnihanSRMS` database (Docker) | The thing being synced. | Tasks 3, 4, 6, 7, 8 |
| `src/main/sql/backup-2026-09-06-pre-merge-sync.sql` | Pre-change full dump (new, committed in Task 10). | Task 3 |
| `src/main/sql/schema.sql` | Fresh-install schema. Header changelog + migration notes only. | Task 9 |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md`, `testing.md`, `bugs.md` | Project state of record. | Task 10 |

No production Java, entity, controller, service, or frontend file changes in this plan.

---

## Task 1: Fix the test-compile break on `main`

**Files:**
- Modify: `src/test/java/com/example/springboot/controller/RegistrarStudentNumberControllerWebMvcTest.java:49-55`

- [ ] **Step 1: Confirm the failure**

Run: `./gradlew compileTestJava -q`
Expected: FAIL —
```
RegistrarStudentNumberControllerWebMvcTest.java:50: error: constructor StudentRecordDetailsResponse in record StudentRecordDetailsResponse cannot be applied to given types;
  required: ...,ParentDto,ParentDto,GuardianDto,BigDecimal
  found:    ...,<null>,<null>,<null>
  reason: actual and formal argument lists differ in length
```

- [ ] **Step 2: Add the missing trailing `totalGwa` argument**

The `details(...)` helper currently passes 32 arguments; the record has 33 components (a trailing `BigDecimal totalGwa` added by the `grade_input_fix` merge). Change the final line of the helper.

Find (lines 49–55):
```java
    private StudentRecordDetailsResponse details(String studentNumber) {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", studentNumber, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null, "Active",
                null, List.of(), List.of(), null, null, null);
    }
```

Replace the last statement line with (one extra `null` for `totalGwa`):
```java
                null, List.of(), List.of(), null, null, null, null);
```

So the helper becomes:
```java
    private StudentRecordDetailsResponse details(String studentNumber) {
        return new StudentRecordDetailsResponse(
                1, "SR20260001", studentNumber, "Lipata", "Maria", null,
                null, null, null, null, null, null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null, "Active",
                null, List.of(), List.of(), null, null, null, null);
    }
```

No new import is needed — the helper passes `null` for `totalGwa`, not a `BigDecimal` literal.

- [ ] **Step 3: Verify test sources compile**

Run: `./gradlew compileTestJava -q`
Expected: no output, exit 0.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/example/springboot/controller/RegistrarStudentNumberControllerWebMvcTest.java
git commit -m "fix: reconcile StudentRecordDetailsResponse arity in student-number WebMvc test after grade_input_fix merge"
```

---

## Task 2: Establish the test-suite baseline

**Files:** none (verification only).

- [ ] **Step 1: Run the full suite**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, `0 failures, 0 errors`. Record the exact test count from the output (memory-bank `testing.md` last recorded 299 on the `student-ID-number` branch; the `grade_input_fix` branch added `GradeEquivalentTest` and rewrote `TrainerGradeServiceTest` / `TrainerGradeControllerWebMvcTest`, so the merged count will be higher).

- [ ] **Step 2: If there are failures, triage before proceeding**

For each failing test, decide:
- **Merge-integration failure** (a stub, arg list, or import that the merge left stale, same class of bug as Task 1) → fix it minimally, re-run, commit with a `fix:` message.
- **Anything larger** (a real behavioural regression, a whole test class failing) → **stop and report to the user** with the failing class name and the assertion message. Do not expand scope to a redesign; this plan's remit is "green CI + DB sync".

- [ ] **Step 3: Record the green baseline**

Run: `./gradlew test` (final confirmation)
Expected: `BUILD SUCCESSFUL — N tests, 0 failures, 0 errors`. Note `N` for Task 10.

---

## Task 3: Back up the live database

**Files:**
- Create: `src/main/sql/backup-2026-09-06-pre-merge-sync.sql`

- [ ] **Step 1: Dump the live DB**

Run:
```bash
docker exec mysql-server mysqldump -uroot -pmy_password --databases AnihanSRMS --routines --triggers > src/main/sql/backup-2026-09-06-pre-merge-sync.sql
```

- [ ] **Step 2: Sanity-check the dump**

Run: `grep -c 'CREATE TABLE' src/main/sql/backup-2026-09-06-pre-merge-sync.sql`
Expected: `19`.

Run: `tail -1 src/main/sql/backup-2026-09-06-pre-merge-sync.sql`
Expected: a line containing `Dump completed`.

- [ ] **Step 3: Do NOT commit yet** — it is committed together with the memory-bank updates in Task 10, so a single commit captures the whole sync.

---

## Task 4: Delete the four existing grade rows from the live DB

Per the user decision: the four rows (`grade_id` 1–4, `SR20260007`/`BPP-102`, `SR20260004`/`COOK-101`, `SR20260014` & `SR20260015`/`CAP-111`) are test data. Removing them lets the grades overhaul run against an empty table, exactly as its header assumes. No table references `grades`, so there are no child rows to clear.

**Files:** live `AnihanSRMS` only.

- [ ] **Step 1: Confirm the row set before deleting**

Run:
```bash
docker exec mysql-server mysql -uroot -pmy_password -e "SELECT grade_id, student_id, subject_code, locked FROM AnihanSRMS.grades ORDER BY grade_id;"
```
Expected: exactly 4 rows, `locked` = 0 for all. **If any row has `locked = 1`, stop and report** — a locked grade is real data, not a fixture.

- [ ] **Step 2: Delete**

Run:
```bash
docker exec mysql-server mysql -uroot -pmy_password -e "DELETE FROM AnihanSRMS.grades; SELECT ROW_COUNT() AS deleted;"
```
Expected: `deleted` = 4.

- [ ] **Step 3: Verify empty**

Run: `docker exec mysql-server mysql -uroot -pmy_password -e "SELECT COUNT(*) AS grades_rows FROM AnihanSRMS.grades;"`
Expected: `grades_rows` = 0.

---

## Task 5: Correct two defects in `2026-08-29-grades-overhaul.sql`

The migration's header claims the live grades table has 0 rows (it had 4 until Task 4 — the comment is simply wrong and will mislead the next reader), and it never shrinks `grades.remarks` from `VARCHAR(255)` down to the `VARCHAR(20)` that `schema.sql` declares, so live and fresh installs would diverge on that column. Both are safe to fix in place: the live DB has not run this migration, and every other environment rebuilds from `schema.sql`.

**Files:**
- Modify: `src/main/sql/migrations/2026-08-29-grades-overhaul.sql`

- [ ] **Step 1: Fix the stale header comment**

Find:
```
--   Idempotent — guarded via information_schema. The live grades table has
--   0 rows, so this is a structural change only (no data migration).
```

Replace with:
```
--   Idempotent — guarded via information_schema. Structural change only —
--   any pre-existing grade rows must be dealt with before running this
--   (the 2026-09-06 post-merge sync deleted the 4 live test rows first).
```

- [ ] **Step 2: Add a `remarks` length fix as step 6 of the migration**

After the "Fallback: if neither column exists yet" block and before the `-- Verification` line, insert:

```sql
-- 6. Shrink remarks to the derived-token width used by schema.sql
--    (COMPETENT / NOT_COMPETENT — never free text under the new model).
--    MODIFY COLUMN is naturally idempotent.
SET @sql = IF(
    (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'remarks') <> 20,
    'ALTER TABLE grades MODIFY COLUMN remarks VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

- [ ] **Step 3: Lint the SQL mentally** — confirm the inserted block matches the file's existing `SET @sql = IF(... ); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;` idiom exactly (it does).

---

## Task 6: Apply the five migrations to the live DB, in date order

Each sub-step applies one file, then runs a focused check. All five files are idempotent.

**Files:** live `AnihanSRMS` only.

- [ ] **Step 1: `2026-08-26-subjects-competency-type.sql`**

Run:
```bash
docker exec -i mysql-server mysql -uroot -pmy_password AnihanSRMS < src/main/sql/migrations/2026-08-26-subjects-competency-type.sql
```
Then:
```bash
docker exec mysql-server mysql -uroot -pmy_password -e "SELECT COLUMN_NAME, IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='subjects' AND COLUMN_NAME IN ('competency_type','qualification_code'); SELECT subject_code, competency_type FROM AnihanSRMS.subjects;"
```
Expected: `competency_type` `NOT NULL` present; `qualification_code` `IS_NULLABLE = YES`; all 6 subjects show `competency_type = CORE`.

- [ ] **Step 2: `2026-08-26-subjects-code-update-cascade.sql`**

Run:
```bash
docker exec -i mysql-server mysql -uroot -pmy_password AnihanSRMS < src/main/sql/migrations/2026-08-26-subjects-code-update-cascade.sql
```
Then:
```bash
docker exec mysql-server mysql -uroot -pmy_password -e "SELECT TABLE_NAME, CONSTRAINT_NAME, UPDATE_RULE, DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='AnihanSRMS' AND REFERENCED_TABLE_NAME='subjects';"
```
Expected: two rows — `grades` → `fk_grades_subject` and `classes` → `fk_classes_subject`, both `UPDATE_RULE = CASCADE`, `DELETE_RULE = RESTRICT` / `NO ACTION`.

- [ ] **Step 3: `2026-08-27-add-student-number.sql`**

Run:
```bash
docker exec -i mysql-server mysql -uroot -pmy_password AnihanSRMS < src/main/sql/migrations/2026-08-27-add-student-number.sql
```
Then:
```bash
docker exec mysql-server mysql -uroot -pmy_password -e "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, ORDINAL_POSITION FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='student_records' AND COLUMN_NAME IN ('student_id','student_number') ORDER BY ORDINAL_POSITION; SELECT INDEX_NAME, NON_UNIQUE FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='student_records' AND COLUMN_NAME='student_number';"
```
Expected: `student_number VARCHAR(20)`, `IS_NULLABLE = YES`, immediately after `student_id`; exactly one index row `uq_student_number`, `NON_UNIQUE = 0`.

- [ ] **Step 4: `2026-08-29-drop-subjects-trainer-id.sql`**

Run:
```bash
docker exec -i mysql-server mysql -uroot -pmy_password AnihanSRMS < src/main/sql/migrations/2026-08-29-drop-subjects-trainer-id.sql
```
Then:
```bash
docker exec mysql-server mysql -uroot -pmy_password -e "SELECT COUNT(*) AS trainer_id_should_be_zero FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='subjects' AND COLUMN_NAME='trainer_id';"
```
Expected: `trainer_id_should_be_zero` = 0.

- [ ] **Step 5: `2026-08-29-grades-overhaul.sql`** (the version edited in Task 5)

Run:
```bash
docker exec -i mysql-server mysql -uroot -pmy_password AnihanSRMS < src/main/sql/migrations/2026-08-29-grades-overhaul.sql
```
Then:
```bash
docker exec mysql-server mysql -uroot -pmy_password -e "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='grades' ORDER BY ORDINAL_POSITION;"
```
Expected column set: `grade_id, student_id, subject_code, class_id, final_percentage, re_exam_percentage, final_grade, re_exam_grade, grade_status, remarks (VARCHAR(20)), hours_rendered, locked, locked_at`. **No** `midterm_grade`, `finals_grade`, `hours_studied`.

- [ ] **Step 6: Re-run every migration once more (idempotency proof)**

Run all five `docker exec -i … < …` commands again in the same order.
Expected: each completes without error; a re-run of the Step 5 verification query returns the identical column set (no duplicate columns, no duplicate FKs).

---

## Task 7: Structural diff — live DB vs `schema.sql`

Proves the sync is complete and surfaces any residual drift.

**Files:** temporary files under the session scratchpad dir only.

- [ ] **Step 1: Build a throwaway reference DB from `schema.sql`**

`schema.sql` hard-codes `CREATE DATABASE IF NOT EXISTS AnihanSRMS` (line 46) and `USE AnihanSRMS;` (line 50) — strip those so it does not write into the live DB, then load into a scratch schema. Use the session scratchpad directory for the temp files.

```bash
SCRATCH="<session scratchpad dir>"
sed 's/^CREATE DATABASE IF NOT EXISTS AnihanSRMS.*$//; s/^USE AnihanSRMS;.*$//' src/main/sql/schema.sql > "$SCRATCH/schema-noheader.sql"
docker exec mysql-server mysql -uroot -pmy_password -e "DROP DATABASE IF EXISTS schema_check; CREATE DATABASE schema_check;"
docker exec -i mysql-server mysql -uroot -pmy_password schema_check < "$SCRATCH/schema-noheader.sql"
```
Expected: no errors. (If `schema.sql` errors on a duplicate seed such as the `admin` user, that is the seed block, not structure — safe to ignore for a structural diff.)

- [ ] **Step 2: Dump both structures, normalized**

```bash
for DB in AnihanSRMS schema_check; do
  docker exec mysql-server mysqldump -uroot -pmy_password --no-data --skip-comments --skip-dump-date "$DB" \
    | sed 's/ AUTO_INCREMENT=[0-9]*//' \
    | sed "s/\`$DB\`/\`DB\`/g" > "$SCRATCH/struct-$DB.sql"
done
diff "$SCRATCH/struct-schema_check.sql" "$SCRATCH/struct-AnihanSRMS.sql"
```
Expected: no differences, **or** only cosmetic ones (FK auto-names such as `grades_ibfk_1` vs a named constraint; index-name-only differences). Any column/type/nullability/constraint-*semantics* difference is real drift — list it and fix it before continuing.

- [ ] **Step 3: Drop the throwaway DB**

```bash
docker exec mysql-server mysql -uroot -pmy_password -e "DROP DATABASE schema_check;"
```

---

## Task 8: Hibernate `ddl-auto=validate` boot against the live DB

The Gradle suite runs on in-memory H2 and cannot catch live-DB drift. This is the authoritative check that the merged entities match the synced live schema.

**Files:** none (verification only).

- [ ] **Step 1: Start the app with validation on, in the background**

```bash
./gradlew bootRun --args='--spring.jpa.hibernate.ddl-auto=validate --server.port=8089' > "$SCRATCH/validate-boot.log" 2>&1 &
```

- [ ] **Step 2: Wait for the outcome, then stop the app**

Poll `"$SCRATCH/validate-boot.log"` until it contains either `Started SpringbootApplication in` (success) or `Schema-validation:` / `SchemaManagementException` (failure). Then stop the process (kill the `bootRun` / Gradle process).

- [ ] **Step 3: Assert success**

Run: `grep -E 'Started SpringbootApplication in|Schema-validation|SchemaManagementException' "$SCRATCH/validate-boot.log"`
Expected: exactly one line — `Started SpringbootApplication in <n>s`. **Zero** `Schema-validation` / `SchemaManagementException` lines. If a validation error appears, it names the offending table/column — that is drift Task 6/7 missed; fix and re-run from Task 7.

---

## Task 9: Update `schema.sql` documentation

`schema.sql`'s table bodies are already correct; only its header changelog and "existing databases should also run…" notes omit the two 2026-08-29 migrations.

**Files:**
- Modify: `src/main/sql/schema.sql` (header comment block, lines ~1–45)

- [ ] **Step 1: Add the 2026-08-29 lines to the "Updated:" changelog**

At the top of the header block, add above the existing `-- Updated: 2026-08-27 …` line:
```
-- Updated: 2026-08-29 (dropped subjects.trainer_id; grades overhauled to the
--            TESDA model: final_percentage / re_exam_percentage / grade_status /
--            hours_rendered replace midterm_grade / finals_grade / hours_studied)
```

- [ ] **Step 2: Add the grades-overhaul migration to the "existing databases" notes**

Near the existing note about `2026-08-26-subjects-competency-type.sql`, add:
```
-- Existing databases that predate 2026-08-29 should also run
-- src/main/sql/migrations/2026-08-29-drop-subjects-trainer-id.sql and
-- src/main/sql/migrations/2026-08-29-grades-overhaul.sql (delete any existing
-- grade rows first — the overhaul drops the old component-grade columns).
```

- [ ] **Step 3: Verify no structural lines were touched**

Run: `git diff --stat src/main/sql/schema.sql`
Expected: only the header comment lines changed (a dozen or so `+` lines, near-zero `-`).

---

## Task 10: Update the memory bank and commit

**Files:**
- Modify: `memory-bank/activeContext.md`, `memory-bank/progress.md`, `memory-bank/changeLog.md`, `memory-bank/testing.md`, `memory-bank/bugs.md`

- [ ] **Step 1: `changeLog.md` — new dated entry at the top**

Add a `## 2026-09-06 - Post-Merge Bug Fix + Live DB Sync` section on `main`, with:
- The one test-compile fix (`RegistrarStudentNumberControllerWebMvcTest` arity) and why the merge caused it.
- The two edits to `2026-08-29-grades-overhaul.sql` (stale "0 rows" comment; added `remarks VARCHAR(20)` step).
- The 4 deleted live grade rows (`grade_id` 1–4, listed) — user-approved as test data.
- The five migrations applied to live, in order, each idempotency-checked twice.
- `schema.sql` header/notes doc update.
- Verification: full `./gradlew test` count, structural diff result, `ddl-auto=validate` PASS.
- New file `src/main/sql/backup-2026-09-06-pre-merge-sync.sql`.

- [ ] **Step 2: `progress.md` — new "Recent Sessions (detail)" entry** mirroring the changeLog summary in the house style (Task / Result / Verified / Branch: `main`).

- [ ] **Step 3: `activeContext.md`** — replace the "Current Phase" / "Latest Session" block with this session; move the previous latest session into "Previous Session". Record: branch `main`, live DB now matches `schema.sql`, suite green at count `N`.

- [ ] **Step 4: `testing.md`** — update the "Latest full-suite result" line to `N tests, 0 failures` dated 2026-09-06; add a one-line "Live Verification — 2026-09-06 (post-merge DB sync)" note (migrations applied + `ddl-auto=validate` PASS + structural diff clean).

- [ ] **Step 5: `bugs.md`** — under "Fixed Bugs", add a one-liner: `Merge integration: StudentRecordDetailsResponse arity mismatch in RegistrarStudentNumberControllerWebMvcTest after the grade_input_fix + student-ID-number merges — fixed 2026-09-06.` Leave Bug 9 / 10 / 11 open and untouched (out of scope this session).

- [ ] **Step 6: Commit everything together**

```bash
git add src/test/java/com/example/springboot/controller/RegistrarStudentNumberControllerWebMvcTest.java \
        src/main/sql/migrations/2026-08-29-grades-overhaul.sql \
        src/main/sql/schema.sql \
        src/main/sql/backup-2026-09-06-pre-merge-sync.sql \
        memory-bank/
git commit -m "fix: post-merge test arity + sync live DB to schema.sql (5 migrations, grades overhaul)"
```

Note: the Task 1 commit is already in history — this `git add` picks up the remaining files. Do not squash.

- [ ] **Step 7: Final verification pass**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL — N tests, 0 failures, 0 errors`.

Run: `git status --porcelain`
Expected: empty (all changes committed).

---

## Self-Review

**1. Spec coverage**

| User requirement | Task |
|---|---|
| Check `main` for bugs/errors from the two merges | Task 1 (compile break, found), Task 2 (full suite triage) |
| Compare live DB with updated `schema.sql` | Task 7 (structural diff), plus the state table in Context |
| Update the live DB accordingly | Task 6 (5 migrations), Task 4 (grade rows), Task 8 (validate) |
| Check for potential bugs before implementing the update | Investigation already done and reported: the "0 rows" data-loss trap (Task 5), the un-migrated live DB, the test-compile break. Task 2 gates on a green suite *before* Task 3 touches the DB. |
| Ask for clarifications first | Done — scope, grade-row handling, and branch were confirmed by the user before this plan was written. |

**2. Placeholder scan** — no "TBD"/"handle edge cases"/"similar to Task N". Every SQL block and the one Java edit are shown in full. The only deferred judgement is Task 2 Step 2 (triage of unknown suite failures), which has explicit decision criteria and a stop-and-report branch.

**3. Type / name consistency** — `totalGwa` is the record's 33rd component (`BigDecimal`), matching `StudentRecordDetailsResponse.from(...)`'s 8th param. Migration filenames match `src/main/sql/migrations/`. FK names `fk_grades_subject` / `fk_classes_subject` match `2026-08-26-subjects-code-update-cascade.sql`. Container `mysql-server`, creds `root`/`my_password`, DB `AnihanSRMS` match `application.properties` and the running container.

**4. Ordering invariant** — the four grade rows are deleted (Task 4) before the grades overhaul (Task 6 Step 5). The migrations otherwise touch disjoint objects and run in date order.

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-09-06-post-merge-db-sync.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
