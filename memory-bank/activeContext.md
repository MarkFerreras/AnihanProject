# Active Context - Anihan SRMS

## Latest Session (2026-09-21 - Merge `main` into `feature/class-year-filter`)

### Scope
Unblock work on the new class-year-filter branch by finishing an in-progress merge of
`main` that had stalled on conflicts. **Documentation-only resolution** — no application
code, schema, or test code was touched.

### The conflict, and why it happened
Only two files conflicted: `memory-bank/changeLog.md` and `memory-bank/progress.md`.
Every Java/HTML/JS/SQL file merged automatically. Both sides had appended a new session
entry at the **top** of each file, and because those entries share identical sub-headings
(`### Files Modified`, the `| File | Change |` table header, `|------|--------|`), Git
matched those shared lines as common context and **interleaved the two entries** into
several small hunks rather than presenting them as one clean either/or block. Hand-editing
the hunks would have risked splicing half of one entry onto half of the other.

### Resolution method (worth reusing — this file pair will conflict again)
Instead of editing the marked-up worktree file, both clean sides were extracted from the
index (`git show :2:<path>` = ours, `git show :3:<path>` = theirs) and re-spliced:

    header + theirs' new entries + ours' new entry + shared tail

Both files are strictly newest-first, so ordering is just date order: main's 2026-09-19/20
entries, then this branch's 2026-09-16 Thread Testing Cases entry, then the shared history
from 2026-09-06 back. The shared tail was confirmed **byte-identical** on both sides before
splicing, which is what makes the splice safe rather than a guess.

### Verified
- Zero conflict markers anywhere in the repo afterwards (not just in the two files).
- Every `##`/`###` heading present on either side is present in the merged file — checked
  with a `comm` set-difference both ways, so nothing was silently dropped.
- Line accounting reconciles exactly (1245 + 1617 − 1178 shared − 2 shared header lines =
  1682; 426 + 581 − 396 shared − 4 shared header lines = 607).
- `./gradlew compileJava compileTestJava` → **BUILD SUCCESSFUL**. Checked explicitly rather
  than assumed: main brought a large amount of Java into this branch (security questions,
  the ID-photo removal, the `AdminUserResponse` arity change), and this project's own
  2026-09-19 merge session already recorded that a zero-conflict text merge does not by
  itself prove the combined code compiles.
- `./gradlew test` was **not** run this session — see Open Items.

### Open Items
- **Run `./gradlew test` before relying on this branch.** The expected baseline is 374.
  Compilation passing does not rule out the cross-branch *test* breakages this project has
  hit twice before (`1916904`, `fec8004` — both were record-arity / missing-mock failures in
  tests that compiled fine on each branch separately).
- The local MySQL schema drift noted in the 2026-09-20 session (missing
  `student_records.student_number`) is unrelated to this merge and still unresolved; it will
  block any live click-through on this branch too until the outstanding migrations are
  applied.
- Class year filter implementation has not been started — this session only cleared the way.

---

## Previous Session (2026-09-16 - Thread Testing Cases, Batch 2)

### Scope
Documentation only — **explicitly instructed not to edit any code**. Read the manual
thread-testing cases in `capstonepaper/OLD ANIHAN Thread Testing Cases.xlsx`
(82 cases, TC-001…TC-082, testers filled some in on 2026-05-20) and author a second
workbook covering everything built since, in the same format, with no duplicates.

### Delivered
`capstonepaper/NEW ANIHAN Thread Testing Cases.xlsx` — **52 cases, TC-083…TC-134**,
sheets `Thread Testing 2` + `Template`. Generator kept at
`capstonepaper/generate_thread_tests_part2.py` beside the two existing generators.

Coverage: student number, single (083–093) · student numbers export/import (094–108) ·
TESDA grading (109–121) · subjects competency type + code rename (122–127) · trainer
account lifecycle guards (128–131) · access/navigation for the newer pages (132–134).

**Document management + generation cases were written, then removed on the user's
instruction** ("for now") — 24 cases, originally TC-083–TC-106. The remainder was
renumbered so the suite stays gap-free. To restore: recover the two `add(...)` blocks
from this branch's git history and append them, renumbering from TC-135.

### Notes for next time
- Cases were written from the **current source**, not from the memory bank, so the
  thresholds and labels in them are real: 10 MB upload cap, 20-char/`[A-Za-z0-9/-]`
  student number, 0–100 percentage and hours, the 75 → 3.00 / 74.99 → 4.00 boundary,
  status codes C / FA / INC / D, and the import outcome names.
- **TC-062–066 in the OLD sheet are now obsolete** — they describe the midterm/finals
  grade input that `grade_input_fix` replaced on 2026-08-29. TC-109–121 supersede them.
  Worth telling the testers so they do not raise those as failures.
- Two document-*page* references were kept on purpose because they test the menu and
  access control, not the documents feature: TC-132 (registrar navbar really has six
  links, Documents among them) and TC-133 (a trainer cannot open `/documents.html`,
  `/generate-document.html`, `/student-numbers.html`).
- **Still open from this session:** the user asked to merge the old and new workbooks
  into one file; that request was interrupted by the removal instruction and has not
  been done.
- Branch `fix/student-ID-number`; no application code, schema, or test code touched.

---

## Current Phase
**Login and logout are no longer written to `system_logs` — `AuthController` no longer
depends on `SystemLogService` at all — and the 197 historical login/logout rows already in
the live database were purged via a one-time, idempotent migration (334 → 137 total rows).
A full pre-purge backup was taken and is being kept
(`src/main/sql/backup-2026-09-19-pre-log-purge.sql`) until this branch is accepted into use.
Separately, a leftover empty `.page-hero-grid` wrapper was cleaned out of `admin.html`'s
hero — the stat cards it used to hold were already removed in an earlier, separately-merged
session (2026-09-20 / PR #58), so this branch's own contribution there is small. Branch was
green at 385 tests and **PR #59 merged this branch into `main` on 2026-09-21T08:03:37Z.**
A follow-up session the same day completed the one item PR #59 shipped without: a rendered-
browser walkthrough of `admin.html` via the Playwright MCP browser bridge (not available in
the PR #59 session). Result: clean pass — see "Browser Walkthrough" below.**

## Active Branch
`feature/remove-login-audit-and-admin-stats` — **merged to `main` via PR #59
(2026-09-21T08:03:37Z).** This follow-up doc-only session continues on the same branch name
per the user's instruction; nothing under `src/` changed, only memory-bank verification
records.

## Browser Walkthrough — 2026-09-21 (follow-up, post-merge)
Ran `./gradlew bootRun` against live MySQL and drove `admin.html` with the Playwright MCP
browser bridge (`mcp__playwright__*` tools), logged in as `admin`.
- **False alarm caught and resolved, not a regression:** the first page load rendered the
  OLD "Total Users / Admins / Registrars / Trainers" stat-card hero. Traced to a **stale
  browser HTTP cache** from earlier testing in this browser profile — a `fetch(url, {cache:
  'no-store'})` against the live server confirmed the real response has zero stat-card
  markup, and `grep` across `src/`, `build/resources/main`, and `bin/main` confirmed none of
  the three copies of `admin.html` on disk contain `hero-stats`/`stat-card`/"Total Users".
  A cache-busted navigation (`admin.html?cb=1`) rendered the clean page. Recorded here so a
  future session isn't fooled by the same stale-cache artifact.
- **Confirmed clean on the cache-busted load:** full-width hero (eyebrow/title/subtitle
  only, no stat cards, no leftover gap); User Directory DataTable renders all 5 seed
  accounts with correct role badges and "Showing 1 to 5 of 5 entries"; search filters
  correctly (tested `registrar` → 1/5, cleared back to 5/5); the `dataSrc: ''` fix from the
  2026-09-20 stat-removal session still works correctly; the details modal opens with all
  10 fields populated (User ID, Username, Email, Role, Last/First/Middle Name, Age,
  Birthdate, Password Last Changed) plus working View Logs / Edit User links; zero
  horizontal overflow at both 1280px and 992px; console clean except one **pre-existing,
  unrelated** `favicon.ico` 500 (already documented as Bug 13 in `bugs.md` —
  `GlobalExceptionHandler`'s missing-route handling, not something this branch touches).
- Test server stopped after verification (it was started solely for this check).

## Open Items
- **The purge is irreversible once the backup is discarded.** Keep
  `src/main/sql/backup-2026-09-19-pre-log-purge.sql` (122,172 bytes, taken before the
  migration ran, 334 `system_logs` rows) until this change has been in live use long enough
  to be confident in it — it is the only way to recover the 197 purged login/logout rows if
  this decision is ever revisited.
- **Test count is 385, not the plan's originally-guessed 377** — this is baseline drift
  from other work merged into `main` before this branch was created (a known, recurring
  pattern in this project's history, e.g. the 2026-09-06 and 2026-09-19 sessions above),
  not a regression introduced by this branch. Noted here explicitly so a future session
  isn't confused by the mismatch against the original plan text.

### Task
User: stop treating routine login/logout as an audit-worthy event (it was the dominant
volume contributor to `system_logs` with the least investigative value of anything logged),
purge the historical rows already sitting in the live database, and finish clearing out the
admin dashboard statistics panel removal from an earlier session (a leftover empty wrapper
div remained in `admin.html`). Plan:
`docs/superpowers/plans/2026-09-19-remove-login-audit-and-admin-stats.md`.

### What changed
- **`AuthController.java`** — both `systemLogService.logAction(...)` calls removed (end of
  `login()`, inside `logout()`). `logout()` no longer resolves the acting user's identity at
  all — that lookup existed solely to have something to log, so once the log call was gone
  the lookup was dead code and was deleted with it; `logout()` is back to "invalidate the
  session, clear the security context." `SystemLogService` field/import/constructor
  dependency removed entirely — constructor arity 5 → 4
  (`AuthenticationManager, UserRepository, UserSecurityAnswerRepository,
  SessionAuthenticationHelper`).
- **`AuthControllerWebMvcTest.java`** (new, 3 tests) — pins the removal:
  `loginWritesNoSystemLogRow` and `logoutWritesNoSystemLogRow` both assert
  `verifyNoInteractions(systemLogService)`; `loginStillReturnsUsernameAndRole` confirms the
  removal didn't collaterally break the login response contract. The logout test
  deliberately stubs `userRepository.findByUsername("admin")` even though the new code
  never calls it — done specifically so the test is a genuine regression pin against the
  *old* code (which did call it before logging), not a vacuous pass that would succeed
  against either version.
- **`SystemLogServiceTest.java`** — `logActionSavesSystemLog`'s sample action string changed
  from the now-nonexistent `"User logged in"` to `"Reset password for: registrar"`, a real
  action `AdminController` still writes, so the test still exercises genuine, current
  behavior instead of a string nothing produces anymore.
- **`admin.html`** — removed a leftover empty `.page-hero-grid` wrapper `<div>` (and its
  inner div) from the hero section. The stat cards this wrapper used to hold, `updateStats()`
  in `admin-users.js`, and the five related CSS rules in `dashboard.css` were **already
  removed in an earlier, separately-merged session** (2026-09-20, PR #58, branch
  `admin_stats_and_SR_overhaul`) — confirmed via git log and via a repo-wide grep for
  `hero-stats`/`stat-card`/`stat-label`/`stat-value`/`stat-caption`/`updateStats` returning
  zero matches *before* this branch's own work began. This branch's only job here was the
  orphaned wrapper cleanup, not the removal itself.
- **`src/main/sql/migrations/2026-09-19-purge-login-logout-logs.sql`** (new) — idempotent,
  data-only migration: `DELETE FROM system_logs WHERE action IN ('User logged in', 'User
  logged out')`, with pre-flight/post-verification SELECT counts and a sanity GROUP BY.
- **`src/main/sql/backup-2026-09-19-pre-log-purge.sql`** (new, untracked per this project's
  convention for backup files) — full `mysqldump` of the live `AnihanSRMS` database, taken
  immediately before the purge migration ran (122,172 bytes, 21 `CREATE TABLE`, 1
  `system_logs` INSERT — contents verified before proceeding).

### Live database purge (real, applied to the live `AnihanSRMS` MySQL database)
Pre-purge counts recorded before touching anything: **334 total `system_logs` rows, 197 of
them login/logout**. Migration applied: **after = 137 total rows, 0 login/logout rows**
(334 − 197 = 137, exact match). Re-ran the migration a second time to prove idempotency: 0
further rows deleted, total stayed at 137. Confirmed via the post-purge action breakdown
that every remaining row is a genuine audited action (e.g. "Updated student record: …",
"Created new account: …") with zero "User logged in"/"User logged out" entries left.

### Verified (Task 8 of the plan)
- `./gradlew test` → **BUILD SUCCESSFUL — 385 tests, 0 failures, 0 errors.** (Higher than
  the plan's originally-guessed 377 — baseline drift from other work merged into `main`
  before this branch was created, a known pattern in this project's history; not a
  regression. See Open Items above.)
- App booted cleanly against live MySQL (`Started SpringbootApplication in 11.65 seconds`,
  zero schema-validation errors).
- Live curl verification: a real login (`admin`/`password123`, succeeded, returned
  `ROLE_ADMIN`) left `system_logs` unchanged at 137; a real logout also left it unchanged at
  137; then a real still-audited action (`PUT /api/account/details`, a safe no-op re-save
  of admin's own existing personal details) DID write a fresh row (137 → 138, action
  "Updated own personal details") — proving the removal was surgical and the rest of
  auditing survives intact.
- Static/API-level inspection confirmed: `admin.html` has zero stat-related markup and a
  full-width hero; `dashboard.css` has zero stat rules; `admin-users.js` has zero
  `updateStats` references; `logs.html` returns HTTP 200; `GET /api/logs?rangeDays=3000`
  returns all 138 rows with zero containing "logged in"/"logged out".
- **Gap, not silently omitted:** an actual rendered-browser walkthrough (visual hero
  layout, DataTable rendering, details modal, console cleanliness) could NOT be completed —
  no Playwright browser bridge extension was available in this environment, a known
  recurring limitation in this project's history (see Open Items above).

---

## Previous Session (2026-09-20 - Student Record Edit Form: Category Tabs + Per-Section Edit Lock)

### Task
User: the registrar's Student Record edit page (`student-records.html`) is one long,
cluttered scroll (12 sections, 65+ fields) and needs to be organized per detail category
the way the student portal wizard already does, with the registrar clicking a category to
view/edit it in place — no page navigation, no modal-in-a-modal. Follow-up clarification:
each category should also start **read-only**, requiring an explicit "Edit" click per
category before its fields become editable (not just visual regrouping).

### Research first
Delegated an Explore agent to map the current structure before touching anything: the
registrar view-details modal (51+ fields, 4 loose unlabeled blocks, `registrar.html`), the
edit form itself (12 flat `<hr>`-divided sections, no tabs/accordion, `student-records.html`
+ `registrar-student-records-edit.js`), and the student portal wizard (`student-details.js`)
— which turned out to be a **3-step** wizard (Personal / Family / Education), not 4-step as
prior memory-bank entries stated; corrected here. Also confirmed via DTO inspection that
`StudentRecordUpdateRequest`/`StudentRecordDetailsResponse` have no `education` field at
all — the registrar edit form has never had an Educational Background section, so "mirror
the portal" could not extend that far without adding new functionality outside this task's
scope.

### Scope decision (per user's answers)
Only the **edit form** was reorganized — the registrar's answer specifically asked for
"click Edit before editing," which only makes sense for an editable form, not the read-only
view-details modal (left untouched). UI pattern: Bootstrap tabs (the exact mechanism already
used by this page's own "Edit Account" modal), not an accordion. Categories: portal's
Personal/Family naming reused where it lines up 1:1; registrar-only sections with no portal
equivalent (Student Number, ID Picture, OJT, TESDA, School Years) folded into a third
"Enrollment & Academics" tab rather than invented portal steps.

### What was built
- **3 tabs**, each wrapping its sub-sections in a `<fieldset data-edit-section="..." disabled>`
  and preceded by a small toolbar with an "Edit Section" button:
  - **Personal Information** — Identifiers, ID Picture, Personal Details, Contact, and a
    renamed "Religion & Siblings" section (was ambiguously "Family / Religion" — renamed to
    avoid colliding in meaning with the new Family Background tab, since it's baptism/sibling
    data, not parent/guardian data).
  - **Family Background** — Father, Mother, Guardian (unchanged content, matches the portal's
    Step 2 naming exactly).
  - **Enrollment & Academics** — Enrollment, OJT, TESDA Qualifications (3 slots), School
    Years at Anihan. No portal equivalent; this is where every registrar-only academic/
    training record landed.
- **Per-section lock via native `<fieldset disabled>`**, not manual per-field tracking: since
  fieldset-disabled cascades live to every descendant control — including ones added later,
  like a new School Year row — locking/unlocking a whole tab is one attribute toggle
  (`registrar-student-records-edit.js`, `setSectionEditable()`/`setupSectionEditToggles()`).
  Fields that must always stay non-editable (Record ID, Enrollment Date, and the
  readonly-but-not-disabled Reference No. / Student Number) keep their own explicit
  disabled/readonly attribute, which fieldset re-enabling does not override — verified this
  is correct HTML5 semantics, not just assumed.
- **`setupDirtyTracking()`** no longer skips attaching listeners to fields that start
  disabled (it used to, since previously almost nothing was disabled at load) — disabled
  fields never fire input/change events regardless, so this is a safe simplification, not a
  behavior change.
- Save mechanics **unchanged on purpose**: one global "Save Changes" button still PUTs the
  full record (the backend has no per-category save endpoint, and adding one was out of
  scope for a display reorg). Locking only gates *typing*, not *what gets saved* — a value
  left in a locked field is exactly what the server already had, so it round-trips correctly
  either way.
- CSS added, carefully scoped to avoid affecting other pages: `#editRecordForm .section-title`
  (this page's `<h6 class="section-title">` headers previously had **zero** styling outside
  the unrelated `.edit-account-modal` scope — confirmed via a 15-file repo-wide grep before
  adding anything unscoped, since a global `.section-title` rule would have silently
  reskinned headers on 14 other pages), `.record-edit-tabs`, `.tab-pane-toolbar`,
  `.edit-section-fieldset` (resets the browser's default fieldset border/padding chrome so
  it's layout-invisible).

### A real bug caught before it shipped
The new tab/pane IDs were first written as `tab-personal`/`pane-personal` etc. — but this
same page already has an "Edit Account" modal (present on every dashboard page) using those
exact IDs for its own Personal Details tab. Duplicate IDs on one page mean
`getElementById`/Bootstrap's `data-bs-target` resolution become ambiguous, which would have
silently broken the *Edit Account* modal's tab switching (an unrelated, pre-existing
feature) the moment this page loaded. Caught by re-reading the full file before considering
this done, not by a test. Renamed to `tab-record-personal`/`pane-record-personal` etc.;
confirmed via grep that only the original Edit Account modal's IDs remain afterward.

### Verified
- Repo-wide grep confirmed `.section-title` usage across 15 pages before scoping the new
  CSS rule to `#editRecordForm` only, to avoid a blast-radius regression on unrelated pages.
- Bumped `dashboard.css?v=2` → `?v=3` on **all 16 pages** that load it (this project's
  established convention — an edited CSS/JS file's cache-buster must move in the same
  change or browsers keep serving the stale copy under `bootRun`, a lesson this project has
  hit and documented twice before). `registrar-student-records-edit.js?v=5` → `?v=6`.
- Started the real app (`./gradlew bootRun`) against the live local MySQL (Docker
  `mysql-server`) and fetched the page over HTTP: confirmed via grep on the raw response that
  all 3 `pane-record-*`/`tab-record-*` IDs, 3 `js-toggle-edit` buttons, and the expected
  fieldset count (3 outer + 3 TESDA slot fieldsets × open/close = 12 `fieldset` lines) are
  present and correctly served — not a stale cached copy. Same check on the JS file confirmed
  `setSectionEditable`/`setupSectionEditToggles` are present in the served bytes.
- **Full interactive click-through (open a real record, click Edit Section, confirm fields
  unlock, save) was not completed** — blocked by a pre-existing, unrelated problem: this
  machine's local MySQL `student_records` table is missing the `student_number` column (and
  likely other columns from later migrations), so every `/api/registrar/student-records/**`
  call 500s with `Unknown column 'sr1_0.student_number'`. Confirmed via `DESCRIBE
  student_records` that the column is genuinely absent, and confirmed via `git log`/this
  session's own diff that no SQL or Java file was touched this session — this is local dev
  environment drift, not a regression from this change. No browser automation
  (`chromium-cli`/`playwright-core`) was available in this environment either, matching the
  same gap noted in the 2026-09-19 ID-photo session.
- No Java/backend/DTO files were touched — `./gradlew test` was not re-run since nothing it
  covers changed.

### Open Items
- **User decision needed:** apply the outstanding local-DB migrations (with a backup first,
  same pattern as every prior DB-sync session) so a full live click-through of the new tabs
  can actually be done, or accept the code-level verification above as sufficient for this
  purely-frontend change.
- A full interactive pass (open a record, unlock each tab independently, confirm the other
  two stay locked, add/remove a School Year row while locked vs. unlocked, Save round-trip)
  is still recommended once the local DB is usable.
- PR to `main` (user approval required).

---

## Previous Session (2026-09-20 - Remove Admin Statistics Panel)

### Task
User asked to remove the admin statistics panel (Total Users / Admins / Registrars /
Trainers stat cards) from the admin dashboard hero section, without affecting any other
functionality.

### What was removed
- `static/admin.html` — the `.hero-stats` block (4 `.stat-card` articles) inside the page
  hero, matching the plain single-column hero pattern already used by every other dashboard
  page (e.g. `registrar.html`).
- `static/js/admin-users.js` — the `updateStats()` helper (computed counts from the loaded
  user list) and its call site inside the DataTable's `ajax.dataSrc`.
- `static/css/dashboard.css` — `.hero-stats`, `.stat-card`, `.stat-label`, `.stat-value`,
  `.stat-caption`, and their two responsive (`@media`) overrides. Confirmed via repo-wide
  grep that no other page referenced these classes before deleting them.

### One behavior-preserving fix made during removal
The DataTable's `ajax.dataSrc` callback was doing double duty: computing the stats AND
telling DataTables where to find the row array in the response. `/api/admin/users` returns
a bare JSON array (not `{data: [...]}`), and DataTables' default `dataSrc` is `"data"` — so
simply deleting the callback would have broken the table (DataTables would look for
`json.data`, find nothing, and render empty). Replaced it with `dataSrc: ''`, which is the
documented way to tell DataTables "the response root IS the array," preserving the exact
same table behavior with the stats computation gone.

### Verified
- Repo-wide grep for `hero-stats`, `stat-card`, `stat-label`, `stat-value`, `stat-caption`,
  `totalUsersStat`, `adminUsersStat`, `registrarUsersStat`, `trainerUsersStat`, and
  `updateStats` → zero remaining references anywhere in `src/`.
- No backend/DTO/test code ever referenced these identifiers — the stats were purely
  client-side arithmetic over the same `/api/admin/users` payload the table already used,
  so no `/api/admin/**` endpoint or test needed touching.
- `git status` confirms exactly 3 files changed: `admin.html`, `dashboard.css`,
  `admin-users.js`. No Java/Gradle build needed (frontend-only change).

### Open Items
- Manual browser smoke test: load `/admin.html`, confirm the hero now shows only the title
  block (no stat cards, no layout gap), and the User Directory DataTable still populates,
  paginates, searches, and opens the details modal correctly.
- PR to `main` (user approval required).

---

## Previous Session (2026-09-19 - Merging `main` into `security_questions`)

### Scope
Brought `origin/main`'s 13 commits into this branch ahead of the final PR back into `main`:
the ID-photo-to-Registrar feature (a separate branch, merged into `main` after this branch
had already split off) and a live-DB sync session that caught and fixed two real SQL type
mismatches in the security-questions migration (`user_security_answers.slot` and
`users.failed_security_attempts` were declared `TINYINT`, but their JPA entity fields are
`Integer` — `ddl-auto=validate` would fail to boot against a database built from the
original migration file). Conflicts appeared in exactly 5 memory-bank files; the actual
application code — including both branches' independent edits to the very same SQL
migration file — merged automatically with zero conflicts.

### Conflict resolution
- `changeLog.md`, `decisions.md`, `progress.md` — both sides had simply added their own
  dated, self-contained entry to the same spot; kept both, no rewrite needed.
- `activeContext.md` (this file) — both sides had written a different "Current Phase" /
  "Active Branch" statement, which can't both be true at once; rewrote the top section into
  one statement reflecting the actual combined state, and relabeled both prior "Latest
  Session" write-ups below as "Previous Session".
- `testing.md` — both sides had a "Latest full-suite result" line with a different test
  count (363 vs. 374); neither number is the real one once the code is actually combined,
  so that line was left until the full suite was run fresh against the merged code — see
  Verified below.

### Verified
- `./gradlew compileJava compileTestJava` → BUILD SUCCESSFUL — the combined code compiles;
  Git reporting zero text conflicts didn't by itself guarantee this (two branches can
  interleave cleanly at the line level while still breaking a cross-file dependency), so this
  was checked explicitly rather than assumed.
- `./gradlew test` → BUILD SUCCESSFUL — **374 tests, 0 failures, 0 errors**. See
  `testing.md` for why this lands exactly on the ID-photo branch's own pre-merge number.

---

## Previous Session (2026-09-19 - Security Questions / Forgot Password Feature)

### Scope
Full implementation of the 10-point security-questions/forgot-password plan agreed with the
user over an extended design discussion (see `decisions.md` for the resulting design calls).
Covers: mandatory first-login setup of 2 security questions (6 fixed defaults or a custom
question per slot), editing them later from the account settings modal, a public
forgot-password flow (email → answer both questions → reset password), a 3-strike lockout
with 15-minute decay that only an admin can clear, and an admin "Unlock Account" action.

### Backend
- **DB**: two new tables — `security_questions` (6 fixed default questions, seeded, wording
  owned by the user, not to be altered) and `user_security_answers` (row-per-slot: a nullable
  FK to a default question XOR a plaintext `custom_question`, plus a BCrypt `answer_hash`;
  `UNIQUE(user_id, question_id)` stops picking the same default twice, MySQL's multi-NULL
  unique-index behavior already relied on elsewhere in this schema). Three new lockout columns
  on `users` (`security_locked`, `failed_security_attempts`, `security_lockout_started_at`),
  kept deliberately separate from `enabled`. Closed a real pre-existing gap: `users.email` had
  no DB-level uniqueness — added `uq_email`, and fixed the 3 seed accounts' placeholder
  `@example.com` addresses to `admin@anihan.local` / `registrar@anihan.local` /
  `trainer@anihan.local` (this feature depends on email being real and looked-up-by).
  Migration: `src/main/sql/migrations/2026-09-19-security-questions.sql`.
- **Entities/repos**: `SecurityQuestion`, `UserSecurityAnswer` + repositories.
- **`User.java`**: 3 new lockout fields.
- **`CustomUserDetailsService`**: the `accountNonLocked` parameter (previously hardcoded
  `true`) now reads `!user.getSecurityLocked()` — this alone makes Spring Security's own
  `LockedException` enforce the lockout on every login attempt, not just the forgot-password
  path, with no custom filter needed.
- **`GlobalExceptionHandler`**: added distinct `LockedException` / `DisabledException`
  handlers — previously both silently fell through to one generic message.
- **`SecurityQuestionService`**: setup/edit validation (exactly one of default-question-id or
  custom-question per slot; no duplicate default question; a custom question that word-for-word
  matches a default, case/whitespace/punctuation-insensitive, is rejected), the lockout state
  machine (3 wrong answers locks the account; the failed-attempt counter decays to 0 after 15
  minutes measured from the *first* failure in the streak, but only while not yet locked; a
  correct answer resets it immediately), and the email lookup / password reset logic.
- **Session mechanism**: three restricted, single-purpose session states — `ROLE_PENDING_SETUP`
  (after login, before the mandatory setup is done), `ROLE_PENDING_VERIFICATION` (after a
  successful forgot-password email lookup), `ROLE_PENDING_RESET` (after answering both
  questions correctly, expires after 10 minutes) — implemented as one shared mechanism
  (`SessionAuthenticationHelper`) reused three times, gated by ordinary `SecurityConfig`
  `hasRole(...)` matchers rather than new filter infrastructure.
- **Controllers**: `SecurityQuestionController` (setup/edit, under `/api/account/security-questions`),
  `PasswordRecoveryController` (`lookup`/`verify`/`reset`, under `/api/password-recovery`).
  `AuthController.login()` now checks setup status and issues the restricted session instead of
  a full one when incomplete; `/api/auth/me` gained `securityQuestionsSetUp`.
- **Admin**: `AdminService.unlockUser()` + `PUT /api/admin/users/{id}/unlock` — clears both
  `security_locked` and `enabled` at once (a single admin action regardless of which
  condition(s) actually apply); `AdminUserResponse` gained `securityLocked`.

### Frontend
- 4 new standalone pages (each with its own dedicated JS, matching the rest of the app's
  per-page-JS convention): `security-question-setup.html` (mandatory, non-skippable),
  `forgot-password.html` (email entry), `forgot-password-questions.html` (answer both),
  `reset-password.html` (new password twice).
- `index.html`: "Forgot Password?" link; role-routing extended for `ROLE_PENDING_SETUP`.
- `auth-guard.js`: the mandatory-setup client-side redirect gate (server-side enforcement is
  the `SecurityConfig` matcher, not this); the "Edit 'Forgot Password' Security Questions"
  modal wiring, added to the Account Settings tab on all 3 dashboards.
- `admin.html` / `admin-users.js`: "Unlock Account" button in the user-details modal, shown
  when `securityLocked` is true.
- `js/password-toggle.js`: extracted the password reveal-toggle logic (previously only in
  `auth-guard.js`, so only dashboard pages had it) into its own small shared file, so
  `index.html`'s login field and `reset-password.html`'s two password fields get the same
  eye-icon toggle without pulling in `auth-guard.js`'s session-guard machinery.

### Two real bugs found during live verification (both fixed)
1. **Login broke for every account** right after implementation — the migration file existed
   but had never actually been *run* against the live MySQL database (only the throwaway test
   DB reflected it), so `users` was missing the 3 new columns the entity now expects on every
   query, and still had the old placeholder emails. Fixed by backing up
   (`backup-2026-09-19-pre-security-questions.sql`), applying the migration, and verifying
   idempotency by re-running it. **Lesson: applying the migration to the live DB is part of
   finishing the feature, not an optional follow-up — this is the same mistake this project's
   own history has flagged repeatedly (see the 2026-07-09 and 2026-09-06 changeLog entries).**
2. **A CSS fix appeared not to take effect no matter how the browser was refreshed or
   cache-cleared.** Root cause was not a stylesheet conflict at all: `./gradlew bootRun` copies
   `src/main/resources` into `build/resources/main` once at startup and does not watch for
   live edits, so the already-running server kept serving a stale compiled copy of
   `dashboard.css` regardless of what the browser did. Confirmed by diffing
   `build/resources/main/static/css/dashboard.css` against the source file. **Lesson: a static
   frontend file edit needs the app process restarted (not just the browser refreshed) to take
   effect under `bootRun` — flag this to the user whenever diagnosing "my CSS/JS change isn't
   showing up."** Also added `?v=2` cache-busting to `dashboard.css`'s `<link>` tag across all
   16 pages that load it (it never had one before) as a belt-and-suspenders fix, and a
   diagnostic technique worth remembering: an isolated headless-Edge screenshot of just the
   affected markup + real stylesheets, run via `msedge.exe --headless --screenshot=...`,
   conclusively proved the CSS itself was correct before the real cause was found.

### Verified
- `./gradlew test` → **363 tests, 0 failures, 0 errors** (was 340 immediately pre-feature;
  +21 `SecurityQuestionServiceTest`, +2 `AdminServiceTest` for `unlockUser`). 3 existing tests
  fixed for `AdminUserResponse`'s new `securityLocked` component (arity bump 11→12, same class
  of fix as this project's earlier `StudentRecordDetailsResponse` arity incidents).
- Live round trip against real MySQL after the migration: `admin`/`password123` login → 200
  `ROLE_PENDING_SETUP` → fetched default questions → completed setup → session upgraded to
  `ROLE_ADMIN`, `/api/auth/me` shows `securityQuestionsSetUp: true` → forgot-password lookup by
  `admin@anihan.local` now returns the 2 question texts. Test data (the security-question
  answers created during this check) was deleted afterward so the user could go through the
  real setup flow themselves rather than inherit throwaway test answers. Stopped short of
  testing the live password-reset step to avoid changing the real admin password.

### Follow-up fix (same day) — Lockout Counter Wasn't Actually Persisting
User reported 3 wrong answers didn't lock the account. Root cause: `verifyAnswers()` is
`@Transactional`, and Spring rolls back the whole transaction by default on any unchecked
exception — the method deliberately throws `IllegalArgumentException` after saving the
incremented counter, so that save was silently undone every time. All 21 unit tests passed
regardless because they mock the repository and never exercise real transaction rollback.
Fixed with `@Transactional(noRollbackFor = IllegalArgumentException.class)`. Verified live
end-to-end this time (not just unit tests): 3 real wrong attempts via the actual HTTP
endpoints correctly reached `security_locked=1` on the 3rd, a 4th was rejected immediately,
and a subsequent login with the right password was also blocked. `admin` locked itself out in
the process (sole admin) — used `BREAKGLASS-account-unlock.md` for real for the first time,
confirming it works. See `changeLog.md` for full detail. **Open follow-up: add a real
transactional (`@DataJpaTest` or full-context) test for this method — Mockito-based service
tests structurally can't catch this class of bug.**

### Open Items
- WebMvc tests for `SecurityQuestionController`, `PasswordRecoveryController`, and the new
  `PUT /api/admin/users/{id}/unlock` endpoint are not yet written (only service-level Mockito
  tests exist so far).
- A transactional/integration test for `verifyAnswers()`'s rollback behavior (see above) is
  needed to actually guard against the bug just fixed — service-level mocked tests can't.
- A full Playwright/manual browser pass of the end-to-end journeys (mandatory setup, edit from
  account settings, forgot-password happy path, 3-strike lockout blocking normal login, admin
  unlock, reset → dashboard) has not been run — only spot-checked via curl.
- PR to `main` (user approval required).
- The `admin` account's live security-question answers were deliberately left unset after this
  session's verification (see above) — first real login will hit the mandatory setup page.

---

## Previous Session (2026-09-19 - ID Photo Upload Moved from Student Portal to Registrar)

Branch was `feature/move-id-photo-to-registrar` (user-approved, branched from `main`),
merged into `main` before this session. Branch was green at 374 tests (was 363),
`ddl-auto=validate` PASS, live-API verification complete.

### Open Items (as of the ID-photo-to-registrar session)
- PR to `main` — user approval required.
- `student_uploads` still sits in live MySQL, empty and unmapped — drop it in a future
  routine schema-sync session (see `decisions.md`).
- No browser automation was available this session (Playwright extension not installed,
  `playwright-core` not present locally); Task 14 was verified at the API level instead.
  A full rendered-DOM walkthrough of the new ID Picture UI (edit form section, details
  modal card, console cleanliness) is still recommended before merge.
- Two pre-existing, unrelated bugs surfaced during live verification and logged in
  `bugs.md` as Bug 12 (`documents.file_type VARCHAR(50)` too short for docx/xlsx MIME
  strings — docx/xlsx upload has always failed against real MySQL) and Bug 13
  (`GlobalExceptionHandler` 500s on genuinely-missing routes instead of 404ing). Neither
  was fixed — both are out of scope for this plan.
- The `registrar` seed account's mandatory security-question setup was completed during
  this session's live verification (it was the only way to get a REGISTRAR-role session);
  this is real onboarding progress, not test data, and was left in place.

## Previous Session (2026-09-19 - schema.sql vs Live DB Comparison + Sync)

### Scope
User asked to compare `src/main/sql/schema.sql` against the live `AnihanSRMS` database
and, if they differed, update the live DB to match. The `security_questions` branch had
been merged into `main` (commits `b17c031`, `64be20e`) but its migration had never been
applied to the live database.

### Drift found (real)
`schema.sql` declared **21 tables**, live DB had **19**. Missing entirely:
- `security_questions` and `user_security_answers` tables
- `users.security_locked`, `users.failed_security_attempts`,
  `users.security_lockout_started_at`
- the `users.email` UNIQUE index

Everything else diffed **cosmetic only** — FK/unique-index auto-names
(`classes_ibfk_3` vs `_ibfk_2`, `fk_grades_class` vs `grades_ibfk_2`, `uq_username` vs
`username`), secondary-index listing order, and `grades` physical column order. Same
four categories as the 2026-07-14 and 2026-09-06 comparisons. No real drift beyond the
security-questions delta.

### Two genuine bugs in the merged SQL, caught by `ddl-auto=validate`
The migration applied cleanly, but the app then **failed to boot** — so the
security-questions feature could not have run against any DB built from these files.
Both were type mismatches between the SQL and the JPA entities:

| Column | Was | Entity field | Fixed to |
|--------|-----|--------------|----------|
| `user_security_answers.slot` | `TINYINT` | `UserSecurityAnswer.slot` (`Integer`) | `INT` |
| `users.failed_security_attempts` | `TINYINT` | `User.failedSecurityAttempts` (`Integer`) | `INT` |

`INT` also matches the pre-existing `student_tesda_qualifications.slot INT` convention.
`users.security_locked` correctly stays `TINYINT(1)` (maps to `Boolean`) and
`security_lockout_started_at` stays `DATETIME` (`LocalDateTime`) — both already right.

Fixed in **`schema.sql`, the migration's CREATE/ADD statements, and two new guarded
`MODIFY COLUMN` steps** (2b and 3b) so databases that already ran the old revision are
repaired on re-run rather than left broken.

### Verified
- Backup taken before any write: `src/main/sql/backup-2026-09-19-pre-schema-sync.sql`
  (116,733 bytes, 19 `CREATE TABLE`).
- **Dry run first:** restored the live backup into a throwaway `schema_check` DB and
  applied the migration there before touching live. Re-ran it — structure byte-identical,
  `security_questions` still 6 rows (not 12). Idempotent.
- Pre-flight data check: no duplicate or NULL emails, so `uq_email` could not fail.
- `ddl-auto=validate` boot against live MySQL → **PASS**
  (`Started SpringbootApplication in 9.368 seconds`, zero `Schema-validation` /
  `SchemaManagementException` lines). This is the check that caught both bugs — the
  Gradle suite runs on H2 and cannot detect live-DB or SQL-file type drift.
- `./gradlew test` → **363 tests, 0 failures, 0 errors**.
- Final structural diff (live vs a fresh DB built from the corrected `schema.sql`) →
  cosmetic only. Live DB now **21 tables**.
- Data preserved: 10 students, 5 users, 323 system_logs, 8 classes.

### Note on live data
The 3 seed accounts' emails were rewritten by the migration from `@example.com`
placeholders to `admin@anihan.local` / `registrar@anihan.local` /
`trainer@anihan.local` — intended, since the forgot-password flow looks accounts up by
email. The 2 real accounts (`trainer2`, `wilkins`) were untouched.

### Open Items
- The two SQL type fixes have since been committed to `main` and are now part of this merge.
- `user_security_answers` is still empty (0 rows) as of that session — no user had set
  security questions yet at that point.

---

## Previous Session (2026-09-06 - Post-Merge Bug Fix + Live DB Sync)

### Scope
Two branches had been merged into `main` before this session — `grade_input_fix` (TESDA
grading overhaul: `GradeEquivalent`, changed `Grade` entity, a trailing `BigDecimal totalGwa`
component on the `StudentRecordDetailsResponse` record, a new `GradeRepository` dependency in
`RegistrarService.buildDetailsResponse`) and `student-ID-number` (registrar-controlled
`student_number`). Task: get the full test suite green again, then bring the live `AnihanSRMS`
MySQL database into line with the updated `src/main/sql/schema.sql` (still on the pre-2026-08-26
schema — 5 migrations outstanding).

### Test fixes (already committed this session)
- `1916904` — `RegistrarStudentNumberControllerWebMvcTest.details(...)` passed 32 args to the
  now-33-component `StudentRecordDetailsResponse` record. The merge reconciled the production
  `StudentRecordDetailsResponse.from(...)` factory but not this cross-branch test. Added a
  trailing `null` for `totalGwa`. Test-only.
- `fec8004` — `RegistrarStudentNumberServiceTest` had no `@Mock GradeRepository`, so
  `@InjectMocks` left it null → 7 NPEs at `RegistrarService.buildDetailsResponse` (~line 355,
  computes `totalGwa` via `GradeEquivalent.gwa(gradeRepository.findByStudentStudentId(...))`).
  Added the mock + `thenReturn(List.of())` in the existing `stubEmptyChildLookups()` helper.
  Test-only; no production code changed.

### Live DB sync (Docker `mysql-server`, DB `AnihanSRMS`)
- Backup first: `src/main/sql/backup-2026-09-06-pre-merge-sync.sql` (116,859 bytes,
  `mysqldump --databases AnihanSRMS --routines --triggers`, 19 `CREATE TABLE`).
- Deleted the 4 disposable test rows from `grades` (`grade_id` 1–4, all `locked=0`) — user
  approved — so the overhaul runs against an empty table. `grades` now 0 rows.
- Edited `src/main/sql/migrations/2026-08-29-grades-overhaul.sql`: corrected the stale header
  comment that claimed 0 live rows; added a guarded/idempotent step 6 shrinking `grades.remarks`
  to `VARCHAR(20)` to match `schema.sql`.
- Applied all 5 outstanding migrations in date order, verified each, then re-ran all 5
  (idempotent — byte-identical, no duplicate columns/FKs): `2026-08-26-subjects-competency-type`
  (`competency_type VARCHAR(15) NOT NULL`, 6 subjects backfilled `CORE`; `qualification_code`
  nullable) · `2026-08-26-subjects-code-update-cascade` (subject-code FKs recreated
  `ON UPDATE CASCADE`, `ON DELETE RESTRICT`) · `2026-08-27-add-student-number`
  (`student_records.student_number VARCHAR(20) NULL` + `uq_student_number`; all 10 students NULL)
  · `2026-08-29-drop-subjects-trainer-id` (column + FK dropped) · `2026-08-29-grades-overhaul`
  (drop `midterm_grade`/`finals_grade`; add `final_percentage`, `re_exam_percentage`,
  `grade_status`; rename `hours_studied` → `hours_rendered`; `remarks` → `VARCHAR(20)`;
  `grades` now 13 columns).
- `schema.sql` — header comment only: added the `2026-08-29` "Updated:" line and an "existing
  databases should also run…" note for the two 2026-08-29 migrations. No table body touched.

### Verified
- `./gradlew test` → **332 tests, 0 failures, 0 errors, 0 skipped** (was 299 on
  `student-ID-number`; `grade_input_fix` added `GradeEquivalentTest` and rewrote the trainer
  grade tests).
- Structural diff (live `--no-data` dump vs a throwaway DB built from `schema.sql`) → cosmetic
  only: FK auto-names (`classes_ibfk_2` vs `classes_ibfk_3`; `grades_ibfk_2` vs
  `fk_grades_class`), unique-index name (`username` vs `uq_username`), secondary-index listing
  order, `grades` physical column order. Name-stripped column definition set byte-identical.
  **No real structural drift** — same conclusion as 2026-07-14.
- `ddl-auto=validate` boot against live MySQL → **PASS** (`Started SpringbootApplication in
  10.192 seconds`, 19 JPA repositories, zero `Schema-validation` / `SchemaManagementException`).
- Live DB still 19 tables. `schema.sql` table bodies already described the target state.

### Open Items
- `student-ID-number` / `grade_input_fix` follow-ups tracked elsewhere (Bug 10, curriculum vs
  `subjects` codes) remain open — out of scope this session.
- The 3 session commits (`1916904`, `fec8004`, the doc commit) are unpushed on `main`.

---

## Previous Session (2026-08-30 - Student Number Export / Import / Report Page)

### Scope
Follow-up to the 2026-08-27 session, which made the student number registrar-controlled but
only assignable one at a time. Per the meeting, the Registrar needs a report page listing all
students with filters to isolate those still missing a number, **export** of that filtered set
to CSV/Excel, and **import** of the encoded sheet back. Sir Ng's summary: Import, Export,
Editability. (Editability already existed — the assign endpoint + modal — and is reused here
rather than duplicated.)

### The constraint that shaped the design
**The school's real record format is not yet known.** So all knowledge of *how a file is read*
is isolated in ONE file, `service/StudentNumberImportMapping.java`: header aliases per column,
the match key, header normalisation, value normalisation, and how far to scan for the header
row. Supporting a new layout should mean adding strings to lists there — the parser, service,
controller, and UI carry no format knowledge. `StudentNumberSheetParserTest` (18 tests) is the
safety net for those edits.

### Decisions (user declined the question prompt; these were taken and flagged in the plan)
- **Match key = Reference No.** (`student_id`) — unique, immutable, on every student, and
  meaningful to a human reading the sheet. Record ID is exported for context, not matched on.
- **Preview then Apply** — the upload is parsed and classified with nothing written; only
  Apply writes. A bulk write to student identity should not be discovered wrong afterwards.
- **Never silently overwrite** — a row renumbering a student who already has a *different*
  number is a `CONFLICT_EXISTING` unless "Allow overwriting existing numbers" is ticked.
- **New page `student-numbers.html`** (6th registrar nav link), keeping archive-migration
  tooling off the day-to-day dashboard.
- **CSV + XLSX both ways** — `poi-ooxml:5.4.1` already present and reads as well as writes,
  so no new dependency on an air-gapped box. DOCX deliberately not offered (not round-trippable).

### What was built
- `StudentNumberImportMapping` (the editable one) + `StudentNumberSheetParser` (CSV RFC-4180
  splitter, XLSX via POI `DataFormatter`, header auto-detection over the first 10 rows).
- `StudentNumberExportService` — headers on row 1 using the canonical aliases so an exported
  file re-imports untouched; Student Number last and blank; **written as a text cell so leading
  zeros survive**. `StudentNumberExportFormat` (CSV/XLSX).
- `StudentNumberImportService` — one `classify()` pass shared by preview and apply, so the two
  cannot diverge. Ten outcomes (WILL_ASSIGN, WILL_OVERWRITE, NAME_MISMATCH, CONFLICT_IN_USE,
  CONFLICT_EXISTING, UNCHANGED, UNKNOWN_REFERENCE, DUPLICATE_IN_FILE, INVALID_FORMAT, BLANK);
  `applicable()` on the enum is the single definition of "this row writes".
- `StudentNumberController` — `GET /export`, `POST /import/preview`, `POST /import/apply`
  under `/api/registrar/student-numbers`. Apply writes one `system_logs` row per assignment
  plus a summary; preview writes none.
- `student-numbers.html` + `registrar-student-numbers.js` — filters, "N of M students still
  need a student number", export (blob download reused from system-logs.js), import
  preview→apply with per-row outcome badges, and the existing Assign Number modal for singles.
- Validation constants moved onto `AssignStudentNumberRequest` (`MAX_LENGTH`, `PATTERN`,
  `ALLOWED_CHARS_MESSAGE`) so single-assign and bulk import cannot drift apart.

### Verified
- `./gradlew test` → **299 tests, 0 failures** (was 238; +61).
- Live round trip against real MySQL: export CSV of the 6 unnumbered students (the one already
  numbered correctly excluded) → encode → preview → apply. Every failure mode exercised in one
  sheet: duplicate-in-file pair, number already in use, unknown reference, blank, invalid chars.
- **Preview proven read-only** — DB and `system_logs` unchanged after a preview.
- **Excel round trip via POI-authored file:** `0012` (leading zero), `2025-777`, `A/2026/03`
  all imported intact. Header auto-detection found the header under two pasted title rows.
- Overwrite guard: refused by default with an actionable message, applied with the flag.
- Playwright headless-Edge E2E **22/22**, run twice (self-cleaning).
- Live DB restored to exactly its pre-session state. Backup:
  `src/main/sql/backup-2026-08-30-pre-import-test.sql`.

### Note on live data
Record 5 (Lopez, Elise) carries `student_number = 231472`, assigned outside these sessions.
It was preserved throughout and is still present.

### Open Items
- PR to `main` (user approval required).
- **When the real archive format arrives:** edit `StudentNumberImportMapping` alias lists and
  re-run `StudentNumberSheetParserTest`. If the archive identifies students by something the
  system does not store (old ledger number, name + birthdate), that is a change of matching
  *strategy*, not aliases — needs a design conversation.
- Student numbers are still capped at 20 chars / `[A-Za-z0-9/-]`. If the archive uses spaces or
  dots, widen `AssignStudentNumberRequest.PATTERN` and the `VARCHAR(20)` column together.

---

## Previous Session (2026-08-27 - Registrar-Controlled Student Number)

### Scope
Stakeholder decision: the system must STOP auto-assigning student numbers. The Registrar
wants control, because the real numbers come from the 40-year paper archive and from TESDA —
a number the system invents is wrong more often than right. Requirements: nullable student
number, students may exist without one, the (future) archive import assigns them, and the
Registrar must be able to see who is still missing one.

### The structural problem, and the decision
`student_records.student_id` is NOT just a label — it is `NOT NULL UNIQUE` and the
**FK target of 10 child tables** (parents, other_guardians, documents, grades,
student_education, student_school_years, student_ojt, student_tesda_qualifications,
student_uploads, class_enrollments). The PK is `record_id`. Making `student_id` nullable
would orphan every child row written before a number is assigned — including the ID-photo
upload in wizard step 2, which is precisely why `startOrResume` creates the record so early.

**Chosen (user-confirmed): add a separate nullable `student_number` column.** `student_id`
stays untouched, demoted to an internal "Reference No."; the registrar-controlled number
lives in the new column. Zero FK churn, no child-table data migration.
Alternative rejected: repointing all 10 child FKs to `record_id` (correct end state, one
identifier, but ~40 files and a 10-table migration).

**On "the student number should remain the primary key"** (from the meeting note): it is not
the PK today (`record_id` is), and a nullable column cannot be a SQL PK. Resolved as a
UNIQUE index — MySQL allows many NULLs in one, giving "unique when present". Verified live:
two NULL rows coexist; a duplicate real value is rejected (ERROR 1062).

**Out of scope (confirmed):** the bulk import itself. Model/API/UI are ready for it; numbers
are entered manually via the new Assign Number action meanwhile.

### What was built
- Migration `2026-08-27-add-student-number.sql` (idempotent; guards match on COLUMN_NAME,
  not constraint name — the 2026-05-19 duplicate-FK bug). `schema.sql` updated to match.
- `student_number VARCHAR(20) NULL` + `uq_student_number`. Nothing auto-generates it;
  `StudentDetailsService.generateStudentId()` is deliberately unchanged.
- `PUT /api/registrar/student-records/{id}/student-number` — assign / change / clear
  (blank = clear). Uniqueness pre-checked in the service so a clash is an actionable 400,
  not a generic 409. Writes "Assigned…"/"Cleared…" to `system_logs`.
- List filter `?hasStudentNumber=true|false` (5-arg `getAllRecords` overload, mirroring how
  the `status` filter was added); free-text search now matches the number too.
- Registrar table: "Reference No." + new "Student Number" column with a
  `Not Assigned` warning badge; Assign Number action + modal; All/Assigned/Not Assigned filter.
- Edit form shows the number **read-only** — the assign action is the single write path,
  so every change is deliberate and separately audited.

### Verified
- `./gradlew test` → **238 tests, 0 failures** (was 217; +21).
- Migration applied to live MySQL, then re-run: `SHOW CREATE TABLE` byte-identical,
  exactly one unique index. `ddl-auto=validate` boot against live MySQL → **PASS** (8.86s).
- Headless-Edge Playwright E2E **18/18**, twice: badges, both action buttons, zero table
  overflow at 1280/1440/1920, assign, duplicate rejected inline, field-level validation
  message, both filters + Reset, details modal, edit form read-only + relabelled, cleanup.
- Live API smoke: assign / duplicate 400 / invalid-chars 400 / clear / 404 / filters /
  search; `system_logs` rows confirmed; a failed duplicate writes NO log row.
- **Regression pinned:** a full edit-form update (payload carries no student number) leaves
  the number intact — checked live and locked down by a unit test.
- DB restored to its pre-session state (all 7 students NULL); backup at
  `src/main/sql/backup-2026-08-27-pre-student-number.sql`.

### Two frontend bugs found by the E2E and fixed
1. **Table overflowed horizontally at 1280px (86px).** Not the new column — `dashboard.css`
   pins `#studentRecordsTable_wrapper #batchFilterBar .logs-filter-section` to
   `flex-wrap: nowrap`, and the added Student No. dropdown pushed that bar past the
   container. Fixed with a page-scoped `flex-wrap: wrap` under 1400px.
2. The existing details-modal handler bound to `button[data-record-id]`, which would have
   caught the new Assign button too. Narrowed to `.js-open-details`.

### Open Items
- PR to `main` (user approval required).
- Follow-up ticket: the bulk archive import that assigns numbers en masse.
- Note: the `student_id` / `student_number` pair is two identifiers on one record. If that
  proves confusing in use, the clean end state is repointing child FKs to `record_id` and
  dropping `student_id` — deliberately deferred, not forgotten.

---

## Previous Session (2026-07-14 PM #2 - Document Management Polish: Print, Logo, DOCX, Delete/Edit)

### Scope
Six approved tasks on documents.html + generate-document.html: (1) print formatting
(no grey backdrop/chrome, 1-page templates print as 1 page); (2) school logo embedded
in the generated header; (3) auto PDF/download filenames "{ShortType}-{Last} {First}";
(4) Actions column fit; (5) generated HTML downloads as editable Word .docx;
(6) DELETE endpoint + type-to-confirm modal, view-modal Edit (re-save in place),
generate-page Cancel + post-save redirect.

### Key Decisions
- **DOCX via OOXML altChunk, server-side** (`HtmlDocxConverter`, pure `java.util.zip`,
  no new dependency, air-gap safe): a minimal docx package embeds the stored HTML as an
  altChunk that Word converts to editable content on open. Chosen over vendoring
  html-docx-js (same fidelity, +70KB unaudited JS). Uploaded pdf/docx/xlsx download
  unchanged; only `text/html` documents convert. Friendly filename via
  `TYPE_SHORT_NAMES` map (mirrors curriculum-templates.js shortName).
- **Edit re-saves update the existing row in place** (`saveGenerated` 5-arg overload with
  `documentId`; `GenerateDocumentRequest.documentId` nullable). Guards: ownership
  (studentId match) AND fileType must be text/html (code review caught that an API call
  could otherwise overwrite an uploaded PSA scan with HTML).
- **Print fix root causes**: grey backdrop = body background + sheet box-shadow not reset
  in `@media print`; page spill = Chromium cannot fragment flex items — body stays
  `display:flex` in print, so sheets jumped to page 2. Print CSS now forces
  `body { display:block; min-height:0 }` + white bg + tighter metrics + row-level
  `page-break-inside: avoid`. **Form IX variants = exactly 1 page; TOR = 2 dense pages**
  (57 subject rows ≈ 1718px vs ~1054px/page capacity — physically cannot fit 1 page).
- Logo shipped as `static/js/anihan-logo.js` (`window.AnihanLogo.DATA_URI`, base64 of
  images/logo.png, 17KB) so saved documents stay self-contained; generator degrades to
  logo-less header if the script fails to load.
- Print filename: `document.title` swapped to sanitized "{shortName}-{Last} {First}"
  around `window.print()`, restored on afterprint + 2s fallback.

### New/Changed Endpoints
`DELETE /api/registrar/documents/{id}` (204, logs "Deleted document…", 404 JSON when
missing) · `GET /{id}/download` now converts text/html→docx with friendly filename
(logs delivered name) · `POST /generate` accepts optional `documentId` → update in
place, logs "Updated generated document…".

### Verified
- `./gradlew test` → **217 tests, 0 failures** (was 200; +17).
- Headless-Edge Playwright E2E **30/30**: real `page.pdf()` (Form IX ×4 = 1 page,
  TOR = 2; no chrome in PDF), logo data-URI, title swap/restore, actions contained
  @1400/@992px, docx download (zip + altChunk + filename), edit re-arms contenteditable
  + updates in place (no duplicate row), cancel + post-save redirects, delete
  type-to-confirm removes row. `system_logs` rows confirmed in live MySQL
  (Generated/Downloaded/Updated/Deleted).
- E2E harness gotcha (documented for next time): a sticky
  `emulateMedia({media:'screen'})` overrides `page.pdf()`'s print media — clear with
  `media: null` or PDFs render with screen CSS.
- /code-review: 3 findings (uploaded-file overwrite guard, blank-name docx filename
  fallback, hard logo dereference) — all fixed + tested.

### Open Items
- PR to `main` (user approval required).
- Fidelity note: Word's HTML import linearizes flex rows (`doc-field-row`) — docx opens
  editable but not pixel-identical to print; tables/underlines survive.

---

## Previous Session (2026-07-14 PM - Student Picker Dropdown + "Failed to load" Diagnosis + Record Cleanup)

### Scope
Three user requests on the Generate Document page and data: (1) replace the native
`<datalist>` student list (ugly floating browser list) with a proper searchable dropdown;
(2) diagnose "Failed to load student data" on Load; (3) delete duplicate / incomplete
student records from the live DB.

### 1. "Failed to load student data" - root cause: stale server build, not a bug
`GET /api/registrar/documents/generate-data/SR20260016` returns **HTTP 200 with full
auto-fill payload** on the current build (verified via curl + headless browser). The
fallback alert text only appears when the error response has no JSON `message` - i.e. a
404 from a server still running a **pre-PR-#49 build** without the document endpoints.
Requirements to generate: the student ID must exist in `student_records`; everything else
(course, section, grades, TESDA, OJT, parents) is optional and simply left blank.
**Operator note: restart `./gradlew bootRun` after pulling document-management.**
Hardened `ajaxErrorMessage()` in the JS so 0/401/404/other statuses now produce
distinguishable messages instead of one generic string.

### 2. Searchable student dropdown (combobox)
- `generate-document.html`: replaced `<input list>` + `<datalist>` with a
  `#studentPicker` wrapper — text input (`role="combobox"`) + Bootstrap `.dropdown-menu`
  (`#studentPickerMenu`, 300px scroll). JS cache-buster `?v=1` → `?v=2`.
- `registrar-generate-document.js`: `loadStudentsDatalist()` → `setupStudentPicker()`.
  Opens on focus, filters as you type (matches ID + last/first name, case-insensitive),
  ArrowUp/Down + Enter keyboard nav, Escape/Tab/outside-click closes, mouse select.
  Shows "Loading students…" until the AJAX list arrives and re-renders when it does
  (race found by headless-browser test: menu rendered empty if focused before load).
- `resolveStudentId()`: exact ID match wins; else a query matching exactly one student
  resolves to it; else friendly "No student matches" alert (no wasted request).

### 3. Student record cleanup (live DB)
Backup taken to scratchpad first. Deleted via the app's own
`DELETE /api/registrar/student-records/{recordId}` (cascade-safe + `system_logs` rows):
- SR20260009 Wong, Angelica — duplicate of SR20260008 (same name/initial), all-NULL Enrolling stub
- SR20260010 Avellaneda, SR20260011 "Mark Mark", SR20260017 "test125" — abandoned all-NULL Enrolling stubs, zero child rows
**Left in place (flagged, user to decide):** SR20260005 (dwd, wdw), SR20260007 (dwadwa,
dwadad — has grade data), SR20260013 (fff, fff) — fake-name test fixtures but Active with
sections. 10 records remain.

### Verified
- Headless Edge (playwright-core) E2E: 10/10 checks pass — login → picker opens on focus
  (10 students) → filter "lipata" → keyboard select → TOR renders auto-filled ("Lipata")
  → mouse select → unknown-text friendly error.
- `./gradlew test` → **200 tests, 0 failures, 0 errors** (unchanged baseline; frontend-only).

### Open Items
- PR to `main` (user approval required).
- User decision: delete the 3 remaining fake-name Active test records?

---

## Previous Session (2026-07-14 - Live DB vs schema.sql Comparison and Sync)

### Scope
Compare the live AnihanSRMS MySQL database against src/main/sql/schema.sql, find any
discrepancies, and update the live DB so it works with the latest project schema.
User explicitly kept work on main (DB-only, no branch switch).

### Method
- Backed up the live DB first -> src/main/sql/backup-2026-07-14.sql (58 KB).
- Built a throwaway schema_check DB from schema.sql (stripped its hard-coded
  CREATE DATABASE / USE AnihanSRMS so it did not redirect into the live DB), dumped
  --no-data structures of both, normalized (dropped AUTO_INCREMENT counters + comments),
  and diffed.

### Findings - ZERO functional drift
Diff showed only cosmetic differences, all verified non-functional:
1. grades columns appear in a different physical ORDER in the dump. Sorted
   column-by-column, live and schema.sql are byte-identical (every name, type,
   nullability, default matches). Column order is irrelevant to SQL and to Hibernate.
2. grades class FK: live name fk_grades_class vs schema.sql auto-name grades_ibfk_3 -
   same relationship (class_id -> classes ON DELETE SET NULL), name only.
3. users unique key: live uq_username vs schema.sql username - same UNIQUE, name only.
- Data: courses holds all 3 (CARS, BPRO, FSERV). Live is the real working DB (14 students,
  6 classes, 264 log rows), so schema.sql's fresh-install seed blocks were NOT re-applied.

### Verification
- ddl-auto=validate boot against live MySQL -> PASS (Started in 11.256s, all 19 entities
  validated, zero HHH schema-validation errors). Authoritative check; Gradle suite runs on
  H2 and cannot catch live drift.
- FK integrity sweep -> 0 orphans (grades->classes, class_enrollments->classes,
  subjects->users trainer, classes->users trainer).

### Outcome
No update to the live database was required - it already matches schema.sql. Only
filesystem change: the new backup file (untracked).

## Previous Session (July 9, 2026 — Document Management R3.1–R3.7 + Template Generation)

### Scope
Implemented Jira AGILE-75…AGILE-81 (R3.1 Upload, R3.2 Type, R3.3 Name, R3.4 View,
R3.5 Search, R3.6 Filter, R3.7 Download) plus auto-filled, editable, print-ready
generation of the 4 templates in `document-templates/` (TOR + Form IX ×3 with
Candidate-for-Graduation / Permanent-Record variants). Plan:
`docs/superpowers/plans/2026-07-09-document-management-r3.md`.

### Key Decisions
- Uses the **existing `documents` table + `Document` entity** — no DB migration.
- Listing query is a JPQL constructor-expression projection that **never selects
  `content_data`** (LONGBLOB stays out of memory); explicit LEFT JOINs on batch/section
  so students without batch/section still appear when those filters are null.
- Generated documents are **self-contained HTML** (`text/html`, print CSS inlined)
  saved into `documents` so they flow through list/view/search/download immediately.
  Curriculum (~50 subjects with fixed hours/units) lives in
  `static/js/curriculum-templates.js`, transcribed from the PDFs — deliberately NOT
  seeded into `subjects` (would entangle class management; `subjects.units` is INT).
  Grades merge in by `subject_code`.
- The rendered document itself is the fillable form: every blank is a
  `contenteditable` span, pre-filled from `GET /api/registrar/documents/generate-data/{studentId}`.
- `X-Frame-Options` changed DENY → **SAMEORIGIN** (SecurityConfig) — required for the
  View modal's same-origin iframe preview; found via live smoke test.
- Upload whitelist: pdf/docx/xlsx, 10MB cap; view=inline only for PDF/HTML (docx/xlsx
  are download-only in the UI).

### New Endpoints (`/api/registrar/documents`)
`GET` list (q/type/batchCode/sectionCode) · `GET /types` · `POST` multipart upload ·
`GET /{id}/download` (logged) · `GET /{id}/view` (inline, not logged) ·
`GET /generate-data/{studentId}` · `POST /generate` (logged). Upload/generate logged too.

### Verified
- `./gradlew test` → **200 tests, 0 failures** (was 176; +24 new).
- Live smoke test against running app + real MySQL: login → types → upload smoke.pdf →
  list/search/filter (incl. 0-rows on wrong section) → download (bytes intact,
  attachment) → view (inline, SAMEORIGIN) → generate-save (.html appended) →
  `system_logs` rows for upload/download/generate confirmed. Smoke rows deleted after.

### Open Items
- Browser smoke test: documents.html flows + generate-document.html print fidelity
  side-by-side with the sample PDFs.
- PR to `main` (user approval required).
- Follow-up ticket: R3.8–R3.11 (group download, encode, group encode, incomplete-docs
  warning) remain unimplemented; curriculum seeding into `subjects` deferred.

---

## Previous Session (July 9, 2026 — Live DB vs SQL Files Comparison & Sync)

### What Was Checked
Compared the live `AnihanSRMS` MySQL database against the newest SQL sources
(`src/main/sql/schema.sql`, dated 2026-05-19, and all 6 files in `src/main/sql/migrations/`).

### Findings
1. **Structure: zero drift.** A `mysqldump --no-data` of the live DB matches `schema.sql`
   table-for-table — all 19 tables, every column type, nullability, index, and foreign key.
   Migrations `2026-05-05`, `2026-05-09` (×2), `2026-05-19`, and `2026-05-20` were all
   already reflected in the live schema.
2. **Data gap (the only discrepancy): `2026-05-10-seed-courses-and-batch.sql` had not been
   applied.** Live `courses` held only `CARS`; the migration seeds `BPRO` and `FSERV` too.
   Batch `B2026A` was already present.

### Action Taken
- Backed up the live DB before any write.
- Applied `2026-05-10-seed-courses-and-batch.sql` (uses `INSERT IGNORE`, so re-running is safe).
- `courses` now holds `CARS`, `BPRO`, `FSERV`. No existing row was modified; `batches` unchanged.
- Re-dumped the structure and diffed against the pre-migration dump: **identical** (data-only change).

### Compatibility Verification
| Check | Result |
|-------|--------|
| Hibernate `ddl-auto=validate` booted against **live MySQL** | **PASS** — app started in 9.0s; all 19 entities validated against live tables (column names, types, nullability) |
| `./gradlew test` (176 tests) | PASS — 0 failures, 0 errors |
| Orphaned FK sweep (grades, class_enrollments, sections, subjects) | 0 orphans |
| `classes.trainer_id` all point at `ROLE_TRAINER` users | Yes |
| Grades within the `[1.0, 5.0]` range enforced by `TrainerGradeService` | Yes |
| `Active` students all have a `section_code` | Yes |

**Note on test coverage:** the Gradle suite runs against an in-memory H2 database
(`ddl-auto=create-drop`), so it does **not** validate entities against live MySQL.
The `ddl-auto=validate` boot above is the check that actually proves live-DB compatibility.

### Live Data State
`courses` 3 · `batches` 3 · `sections` 3 · `qualifications` 2 · `subjects` 6 · `users` 3 ·
`student_records` 5 · `classes` 1 · `class_enrollments` 1 · `grades` 1 · `system_logs` 10 ·
(`parents`, `documents`, `student_education`, `student_uploads` all empty)

### Follow-up (same session) — Grades-Restructure Migration Made Real
`2026-05-19-grades-restructure.sql` was a no-op verification script (its `ALTER`s were
commented out, because the spec's `ADD COLUMN IF NOT EXISTS` is MariaDB/Postgres syntax and
is invalid in MySQL 8). It has been rewritten to actually perform the restructure, using the
guarded `information_schema` + `PREPARE` pattern already proven in the 2026-05-20 migration.

It now: adds `class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at`; relaxes
`final_grade` / `hours_studied` / `remarks` to NULL; adds the `fk_grades_class` FK and the
`uq_grade_student_class` unique key; and aborts up front (statement 1, before any change) if
the `classes` FK target is missing.

**Second bug found while testing the fix:** the FK guards keyed off the *constraint name*
(`fk_grades_class`). A database built from `schema.sql` carries that same relationship under
MySQL's auto-generated name `grades_ibfk_3`, so the name-only check saw "no FK" and added a
**duplicate** foreign key on every re-run — i.e. the migration was not idempotent. Confirmed
by running it against the live DB (it did create a duplicate; reverted immediately). Both this
file and `2026-05-20-sync-and-clear-students.sql` now match on
`KEY_COLUMN_USAGE (COLUMN_NAME + REFERENCED_TABLE_NAME)` instead of the constraint name.
The same fix was applied to the `subjects.trainer_id -> users` guard.

**Verified on three paths:**
- Legacy pre-restructure `grades` table → migration applies all columns, constraints, and
  nullability changes correctly.
- Already-migrated DB (live) → two consecutive re-runs leave the structure byte-identical;
  exactly 3 FKs, no duplicate; the existing grade row is untouched.
- `classes` missing → aborts with `ERROR 1146 ... ABORT_classes_missing_run_2026_05_09_migration_first`
  and leaves `grades` completely unmodified (no half-apply).
- Hibernate `ddl-auto=validate` against live MySQL → still PASS.

Note: `SIGNAL` cannot be used inside the prepared-statement protocol, so the precondition
abort intentionally selects from a non-existent table whose *name* is the operator instruction.

### Open Items
- None from this session. Changes are uncommitted on `main`.

---

## Previous Session (May 21, 2026 — Bugfix Audit Remediation)

### Items Completed
All 10 tasks from `2026-05-21-bugfix-audit-remediation.md` plan executed. Test suite: **176 tests, 0 failures, 0 errors** (was 166; 10 new tests added).

| Task | Description | Status |
|------|-------------|--------|
| H1 | `deleteRecord()` now calls `deleteClassEnrollmentsByStudentId` before `deleteById` | Done |
| H2 | Student ID locked `readonly` in HTML; server-side rejects changes in `updateRecord` | Done |
| M2+M3+M4 | `startOrResume` handles namesake list properly; `load` guards non-Enrolling; `generateStudentId` uses MAX+1 | Done |
| M5+M6 | Grade range [1.0–5.0] validation in `saveGrades`; finalGrade always recomputed | Done |
| L1 | `@Column` lengths/nullable fixed in 5 entities (StudentUpload, StudentEducation, StudentSchoolYear, StudentTesdaQualification, StudentOjt) | Done |
| L2 | `middleName` `nullable=false` removed from `StudentRecord`; `@NotBlank` removed from `StudentRecordUpdateRequest` | Done |
| L3 | `@EnableMethodSecurity` added to `SecurityConfig` | Done |
| L5 | Default email in `AdminService.createUser` now derives from username | Done |
| L4 | CLAUDE.md CSRF statement corrected | Done |
| L6 | `StorageService.store()` now validates studentId against `[A-Za-z0-9_-]+` whitelist | Done |

### Open Items
- Commit all changes to `main`

## Previous Session (May 21, 2026 — Database Schema Sync & Grades Restructure)

### Root Causes / Needs Identified
1. **DB Column & Constraint Mismatches:** The live MySQL database had multiple column width and type mismatches compared to `schema.sql` (e.g. `batches.batch_year` as `smallint` instead of `year`, `student_school_years` string fields as `varchar(10)` instead of `varchar(20)`, etc.).
2. **Grades Table Restructure Not Applied:** The live MySQL `grades` table was in its old structure, missing critical trainer grading columns (`class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at`), unique constraints, and foreign key relations.
3. **Empty Data State:** The database was missing seeded student records and class-scoped lookups.

### Items Completed
1. **Database Backup:** Took a full SQL dump of the live database (`AnihanSRMS`) to `src/main/sql/backup.sql`.
2. **Fresh Schema Installation:** Dropped and recreated `AnihanSRMS` and successfully imported the canonical `src/main/sql/schema.sql` (which incorporates the full restructured grades schema, all tables, and seed lookup/users data).
3. **Restructure Verification:** Ran the query portion of `src/main/sql/migrations/2026-05-19-grades-restructure.sql` to confirm that all restructured grades table columns (`class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at`), nullable column properties, foreign key links, and unique constraints are fully in place.
4. **Student Records Seeding:** Verified that the 5 sample student records and lookup data are fully seeded.
5. **Testing Suite Validation:** Ran `./gradlew test` and verified that the entire suite of 166 tests passed successfully.

### Verified via Database
```
Field            Type          Null   Key   Default  Extra
grade_id         int           NO     PRI   NULL     auto_increment
student_id       varchar(20)   NO     MUL   NULL     
subject_code     varchar(20)   NO     MUL   NULL     
class_id         int           YES    MUL   NULL     
midterm_grade    decimal(5,2)  YES          NULL     
finals_grade     decimal(5,2)  YES          NULL     
locked           tinyint(1)    NO           0        
locked_at        datetime      YES          NULL     
final_grade      decimal(5,2)  YES          NULL     
re_exam_grade    decimal(5,2)  YES          NULL     
hours_studied    decimal(5,2)  YES          NULL     
remarks          varchar(255)  YES          NULL     
```

## Previous Session (May 19, 2026 — Fix Save Grades Error + Button Styling)

### Root Causes Found
1. **`Grade.java` JoinColumn mapping** — `@JoinColumn(name = "student_id")` on the `StudentRecord` relationship was mapping to `StudentRecord.recordId` (the `@Id` auto-increment integer) instead of `StudentRecord.studentId` (the business key varchar). This caused `SQLIntegrityConstraintViolationException` on every INSERT because Hibernate wrote the integer record_id into the varchar student_id FK column.
2. **`TrainerGradeController` empty error responses** — All catch blocks returned `ResponseEntity.badRequest().build()` with NO JSON body. The JS `xhr.responseJSON.message` then failed silently because `responseJSON` was `null`.
3. **Button CSS classes** — `btn-surface-success` (Lock Grades) and `btn-save` (Save Grades) did not exist in `dashboard.css`. Only `.edit-account-modal .btn-save` was scoped to the account modal, not available globally.
4. **JS sent all students** — `collectGradeUpdates()` included every table row (even empty ones), causing unnecessary save attempts for students with all-null fields.

### Items Completed
1. **`Grade.java`** — Added `referencedColumnName = "student_id"` to `@JoinColumn` to correctly join on the varchar business key.
2. **`TrainerGradeController.java`** — All endpoints now return `Map.of("message", e.getMessage())` in error responses. Added SLF4J logging. Removed `@Valid` from save endpoint (not needed for manual grade input). Changed return types to `ResponseEntity<?>`.
3. **`trainer-classes.html`** — Changed `btn-surface-success` → `btn-surface-secondary`, `btn-save` → `btn-surface`. Bumped JS cache-buster `?v=3` → `?v=4`.
4. **`trainer-classes.js`** — `collectGradeUpdates()` now only includes rows with at least one filled grade field. `saveGrades()` shows warning when no data entered. After successful save, reloads grade data in modal instead of closing it.
5. **DB cleanup** — Cleared stale failed INSERT artifacts from `grades` table, reset AUTO_INCREMENT.
6. **Browser smoke test: PASSED** — Save → Lock → Unlock full flow verified. Grades persist in DB with correct FK values and computed final_grade.

### Verified via Database
```
grade_id=1, student_id='SR20260007', subject_code='BPP-102', midterm_grade=2.00, finals_grade=3.00, final_grade=2.60, class_id=1, locked=0
```

### Open Items
- Commit on `feature/trainer-grade-input`.
- PR to main (user approval required).

## Previous Session (May 18, 2026 — Trainer Subject & Class Views)

### Items Completed
1. **`SchoolClassRepository`** extended — `findByTrainerUserId(Integer)` and `findByTrainerUserIdAndSubjectSubjectCode(Integer, String)`.
2. **4 new DTOs** in `dto/trainer/` — `TrainerSubjectResponse`, `TrainerSubjectStudentResponse`, `TrainerClassResponse`, `TrainerClassStudentResponse`.
3. **`TrainerService`** created — `resolveCurrentTrainerId()`, `getMyAssignedSubjects()`, `getStudentsForSubject()`, `getMyClasses()`, `getStudentsForClass()`.
4. **`TrainerController`** created — 4 GET endpoints under `/api/trainer/`.
5. **`TrainerServiceTest`** — 12 Mockito tests. **`TrainerControllerWebMvcTest`** — 9 WebMvc tests (RBAC + 401/403/400 edge cases).
6. **`SecurityConfig.java`** — trainer matcher extended to include `/trainer-subjects.html` and `/trainer-classes.html`.
7. **`trainer.html`** — upgraded to full dashboard pattern: `navbar-expand-lg` with 3 nav links, welcome hero, quick-link cards, jQuery import.
8. **`trainer-subjects.html`** — new page: subjects DataTable + inline student roster panel.
9. **`trainer-subjects.js`** — subjects DataTable with click-to-load roster; `GET /api/trainer/subjects` + `GET /api/trainer/subjects/{code}/students`.
10. **`trainer-classes.html`** — new page: classes DataTable + inline student roster panel.
11. **`trainer-classes.js`** — classes DataTable with click-to-load roster; `GET /api/trainer/classes` + `GET /api/trainer/classes/{classId}/students`.
12. **Full suite: 156 tests, 0 failures, 0 errors.**

### Open Items
- Manual browser smoke test: log in as `trainer`, verify My Subjects and My Classes pages load, click a row to expand student roster.
- PR to main (user approval required).

## Previous Session (May 15, 2026 — Section CRUD + Bulk Class Enrollment)

### Items Completed
1. **6 new DTOs** — `UpdateSectionRequest`, `SectionStudentResponse`, `EligibleSectionStudentResponse`, `AssignStudentsToSectionRequest`, `SectionAssignmentResultResponse`, `BulkEnrollSectionResponse`.
2. **`StudentRecordRepository`** extended — 5 new finder methods for section filtering (null section, status, batch, course combinations).
3. **`ClassEnrollmentRepository`** extended — `deleteByStudentAndSectionCode` JPQL bulk-delete query.
4. **`ClassManagementService`** extended — 6 new methods: `updateSection`, `getStudentsInSection`, `getEligibleStudentsForSection`, `assignStudentsToSection`, `removeStudentFromSection`, `bulkEnrollSectionIntoClass`.
5. **`ClassManagementController`** extended — 6 new endpoints: `PUT /sections/{code}`, `GET /sections/eligible-students`, `GET /sections/{code}/students`, `POST /sections/{code}/students`, `DELETE /sections/{code}/students/{studentId}`, `POST /classes/{classId}/enroll-section`.
6. **`ClassManagementSectionServiceTest`** — 13 Mockito tests for all 6 service methods.
7. **`ClassManagementSectionControllerWebMvcTest`** — 7 WebMvc tests. Full suite: **135 tests, 0 failures**.
8. **`sections.html`** — added `#editSectionModal` and `#manageSectionModal` (tabbed: Current Students + Add Students with batch/course filters); cache-buster `?v=3`.
9. **`registrar-sections.js`** — rewritten Actions column (Edit / Manage Students / Delete); added `setupEditSection()`, `setupManageStudents()`, `refreshCurrentStudents()`, `refreshEligibleStudents()`, `loadFilterDropdowns()`.
10. **`classes.html`** — added Bulk Enrollment section (`#enrollWholeSectionBtn` + `#enrollSectionAlert`) in `#enrollStudentModal`; cache-buster `?v=3`.
11. **`registrar-classes.js`** — wired `#enrollWholeSectionBtn` in `setupEnrollment()`; `openEnrollmentModal()` now clears `#enrollSectionAlert` on re-open.

### Verified
- `./gradlew test` → **BUILD SUCCESSFUL — 135 tests, 0 failures, 0 errors**.
- 3 commits on `feature/section-class-enrollment`.

### Open Items
- Manual browser smoke test: sections page Edit + Manage Students flows, classes page Enroll Whole Section button.
- PR to main (user approval required).

## Previous Session (May 10, 2026 — Navbar Sync on student-records.html)

### Items Completed
1. **`student-records.html` navbar updated** — added `Classes` and `Sections` `<li>` entries to match the canonical 4-link registrar nav from `registrar.html`. Previous state had only Home + Subjects (a stale 2-link pattern from before the May 9 registrar-pages rollout).
2. Navbar markup style follows the existing pattern: `nav-link admin-nav-link` classes, no `active` modifier (the edit page is a subpage of Home, not a top-level nav target).

### Verified
- Visual diff against `registrar.html` confirms identical link order, hrefs, and Bootstrap classes.

### Open Items
- Manual browser smoke test on `/student-records.html?id={recordId}` — confirm all 4 links navigate correctly and the collapsible menu still works at mobile widths.

---

## Previous Session (May 10, 2026 — Edit Class Trainer: AGILE-93 / AGILE-95)

### Items Completed
1. **UpdateClassTrainerRequest DTO** created — single nullable `Integer trainerId`; mirrors `AssignTrainerRequest`.
2. **ClassManagementService** extended — `updateClassTrainer(Integer classId, UpdateClassTrainerRequest)` added. Validates trainer role + enabled (same as `assignTrainer`). Returns `ClassResponse` with live enrolled count.
3. **ClassManagementController** extended — `PUT /api/registrar/classes/{classId}/trainer`. Writes `system_logs` row: "Assigned trainer X to class #N" or "Unassigned trainer from class #N".
4. **classes.html** updated — `#editClassModal` inserted between Create Class and Enroll Student modals. Read-only Section/Subject/Semester display + trainer `<select>` + inline alert. Cache-buster `?v=2`.
5. **registrar-classes.js** updated — Actions column now emits `Edit Trainer` + `Manage Students` buttons. `setupEditClass()` + `openEditClassModal()` added. `loadTrainersDropdown()` now returns jQuery deferred for `.done()` chaining. `currentEditClassData` module state added.
6. **Tests** — `ClassManagementServiceTest` (6 tests) + `ClassManagementControllerWebMvcTest` (4 tests). Full suite: **115 tests, 0 failures, 0 errors**.

### Open Items / Deferred
- Manual browser smoke test: open `/classes.html`, click Edit Trainer, change trainer, verify row updates; unassign and verify "Unassigned" italic; check `/logs.html` for audit rows.
- Jira: transition AGILE-93, AGILE-95 to Done after PR merges (user will handle).
- Note for follow-up: `createClass` still missing the `enabled` trainer-account check (present in `assignTrainer` and new `updateClassTrainer`).

---

## Previous Session (May 10, 2026 — Subjects CRUD: Create / Edit / Delete)

### Items Completed
1. **QualificationRepository** created — `JpaRepository<Qualification, Integer>`.
2. **QualificationResponse DTO** created — used by `GET /api/registrar/qualifications` dropdown endpoint.
3. **CreateSubjectRequest + UpdateSubjectRequest DTOs** created — Bean Validation on all fields; subject code read-only on update.
4. **SchoolClassRepository** extended — added `existsBySubjectSubjectCode(String)` for FK pre-check.
5. **SubjectRepository** extended — added `countGradesBySubjectCode` native query against `grades` table.
6. **ClassManagementService** extended — `QualificationRepository` injected; `getAllQualifications()`, `createSubject()`, `updateSubject()`, `deleteSubject()` implemented. Delete has double FK pre-check (classes + grades).
7. **ClassManagementController** extended — `GET /qualifications`, `POST /subjects`, `PUT /subjects/{code}`, `DELETE /subjects/{code}`. Every state-changing call writes a `system_logs` row.
8. **subjects.html** updated — "Create Subject" button in page header; three new Bootstrap modals (`#createSubjectModal`, `#editSubjectModal`, `#deleteSubjectConfirmModal` with strict type-to-confirm); JS cache-buster `?v=2`.
9. **registrar-subjects.js** rewritten — Actions column now returns Edit + Assign Trainer + Delete buttons; `loadQualificationsDropdown()`, `setupCreateSubject()`, `setupEditSubject()`, `setupDeleteSubject()` added.
10. **Tests** — `ClassManagementSubjectServiceTest` (9 tests) + `ClassManagementSubjectControllerWebMvcTest` (6 tests). Full suite: **105 tests, 0 failures**.

### Open Items / Deferred
- Manual browser smoke test (Create → Edit → Assign Trainer → Delete happy path + FK-block path).
- Verify `system_logs` rows for create/update/delete via `/logs.html`.
- Jira: transition AGILE-89, AGILE-90, AGILE-91 to Done after PR merges.

---

## Previous Session (May 9, 2026 — Student-Details Wizard Trim)

### Items Completed
1. **Baptismal Certificate optional.** Removed `certStatus`/`pendingBaptCert` guard from `STEP_CUSTOM_VALIDATORS[2]`. Baptism Date + Place remain required when "Baptized" is checked. File input retained — students can still upload voluntarily.
2. **Educational Background → 4 columns.** Removed `Grade/Year` and `Semester` columns from `<thead>` and all 4 `<tbody>` rows. Renamed `Year Ended` → `School Year`. Dropped `.edu-grade`/`.edu-sem` reads from `buildPayload` and `populateForm`.
3. **School Years at Anihan removed.** Deleted the entire `#syTable`/`#addSyRow` HTML block. Removed `renderSyRow()` DOMContentLoaded call, `addSyRow` listener, 12-line `#syTableBody` forEach in `buildPayload`, `schoolYears` population block in `populateForm`, and `renderSyRow()`/`addSyRowData()` functions. `const schoolYears = []` remains in payload for DTO compatibility.
4. **JS cache-buster bumped** `?v=4` → `?v=5`.

### Verified
- `grep` for `syTable|syTableBody|addSyRow|renderSyRow|addSyRowData|edu-grade|edu-sem` → zero matches in both files.
- `./gradlew test` → **90 tests, 0 failures, 0 errors**.

### Open Items / Deferred
- Manual browser smoke test of the updated wizard (Steps 2–4, baptism opt-out, education 4-col, submit).
- Registrar regression: confirm school-year rows can still be added via the registrar edit form after a new student submits with zero `student_school_years` rows.

---

## Previous Session (May 9, 2026 — DB Sync + Error-Handler Hardening + Section FK Pre-Check)

### Audit Findings (read-only investigation phase)
1. **CRITICAL:** Live MySQL had 17 tables, code expected 19. The `2026-05-09-classes-and-trainers.sql` migration had not been applied on this machine. `GET /api/registrar/classes` → 500 `Table 'AnihanSRMS.classes' doesn't exist`. `GET /api/registrar/subjects` → 500 `Unknown column 's1_0.trainer_id'`.
2. **HIGH:** `student_records.middle_name` was still `NOT NULL` despite the JPA entity treating it as optional and the student-portal wizard allowing blank input. The 2026-05-05 drift migration relaxed 27 columns but missed this one.
3. **HIGH:** `GlobalExceptionHandler` generic 500 handler returned `"Internal server error: <ExceptionClass> - <message>"` which leaked raw SQL, table names, and column names to clients.
4. **MEDIUM:** `ClassManagementService.deleteSection` had no FK pre-check — relied on raw MySQL FK violation, which then leaked through the generic handler.
5. **MEDIUM:** `getCurrentSemester` loaded all batches into memory just to compute a max.

### Items Completed
1. **Live DB migrated.** Re-applied `2026-05-09-classes-and-trainers.sql` (added `classes`, `class_enrollments`, `subjects.trainer_id`, seeded 2 qualifications + 6 subjects). Live tables: 17 → 19.
2. **New migration `2026-05-09-relax-middle-name.sql`** — applied to live DB. `student_records.middle_name` now `NULL`.
3. **`schema.sql` updated** — fresh-install schema matches live DB on the relaxed column.
4. **`GlobalExceptionHandler` hardened** — added SLF4J logger; new `DataIntegrityViolationException` handler (HTTP 409); generic `Exception` handler logs server-side and returns sanitized `"An unexpected error occurred."`.
5. **`SchoolClassRepository`** — added `existsBySectionSectionCode(String)`.
6. **`BatchRepository`** — added `findTopByOrderByBatchYearDesc()`.
7. **`ClassManagementService.deleteSection`** — pre-checks for class references and throws `IllegalArgumentException` (→ HTTP 400) with a clear actionable message.
8. **`ClassManagementService.getCurrentSemester`** — replaced `findAll()` + in-memory max with a single repository call.

### Verified
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → **90 tests, 0 failures, 0 errors**
- `./gradlew bootRun` → started cleanly. Authenticated as `registrar` and confirmed:
  - `GET /api/registrar/subjects` → HTTP 200 with 6 seeded subjects
  - `GET /api/registrar/classes` → HTTP 200 `[]`
  - `GET /api/registrar/classes/current-semester` → HTTP 200 `{"semester":"2026"}`
- Live MySQL: 19 tables, `subjects.trainer_id` present, qualifications + subjects seeded, `student_records.middle_name` nullable.

### Open Items / Deferred
- N+1 in `getEligibleStudents()` and `getClasses()` — acceptable at the ~160-student scale; revisit if perf issues surface.
- Move `ClassEnrollmentResponse` and `StudentSummary` records from `ClassManagementService` into `dto/registrar/` (cosmetic).
- Unit/WebMvc tests for `ClassManagementService` and `ClassManagementController` — would have caught the live-DB drift earlier; remains on the roadmap.

---

## Previous Session (May 9, 2026 — Registrar Subjects/Classes/Sections + Class Enrollment)

### Items Completed
1. **DB migration `2026-05-09-classes-and-trainers.sql`** — Idempotent. Adds `subjects.trainer_id` (FK → `users.user_id`, ON DELETE SET NULL), creates `classes` + `class_enrollments`, seeds 2 qualifications (Cookery NC II, Bread and Pastry Production NC II) + 6 subjects. Applied to live MySQL via `docker exec`.

2. **JPA entities** — `SchoolClass` (named to avoid `java.lang.Class` clash), `ClassEnrollment`. `Subject.java` extended with optional `@ManyToOne User trainer`.

3. **Repositories** — `SchoolClassRepository`, `ClassEnrollmentRepository`. `SectionRepository.findByBatchBatchYear`. `UserRepository.findByRoleAndEnabledTrue`.

4. **DTOs** (in `dto/registrar/`) — `SubjectResponse`, `AssignTrainerRequest`, `ClassResponse` (+`enrolledCount`), `CreateClassRequest`, `SectionResponse`, `CreateSectionRequest`, `TrainerResponse`, `EnrollStudentRequest`.

5. **Service `ClassManagementService`** — Subjects, Trainers lookup, Classes (current-semester resolution), Class Enrollment, Sections. Validates trainer role + enabled, enforces `(section_code, subject_code, semester)` uniqueness; eligible-student filter scoped to same section, Active/Submitted only.

6. **Controller `ClassManagementController`** — Separate from `RegistrarController`, mounted under `/api/registrar/`. Endpoints: `GET /subjects`, `PUT /subjects/{code}/trainer`, `GET /trainers`, `GET /classes`, `GET /classes/current-semester`, `POST /classes`, `GET /classes/{id}/enrollments`, `GET /classes/{id}/eligible-students`, `POST /classes/enroll`, `DELETE /enrollments/{id}`, `GET /sections`, `POST /sections`, `DELETE /sections/{code}`. Every state-changing call writes a `system_logs` row.

7. **SecurityConfig** — Registrar HTML matcher extended with `/classes.html` and `/sections.html`.

8. **Frontend pages** — Rebuilt `subjects.html` (DataTable + Assign Trainer modal); new `classes.html` (DataTable + Create Class + Manage Students modals, auto-fills subject's default trainer); new `sections.html` (DataTable + Create Section + Delete confirm). `registrar.html` navbar bumped from 2-link to 4-link (Home / Subjects / Classes / Sections).

9. **Frontend JS** — `registrar-subjects.js`, `registrar-classes.js`, `registrar-sections.js`.

10. **schema.sql refresh** — `subjects.trainer_id`, `classes`, `class_enrollments`, qualifications + subjects seed data. Header → 2026-05-09; table count 17 → 19.

### Verified
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → BUILD SUCCESSFUL (full suite, no regressions)
- `./gradlew bootRun` → Tomcat on 8080, 14 JPA repositories loaded, no schema validation errors
- Live MySQL: `classes` and `class_enrollments` present; `subjects.trainer_id` present; 2 qualifications + 6 subjects seeded.

### Open Items
- E2E browser smoke test of the three new pages and create/enroll flows (deferred to user)
- Unit tests for `ClassManagementService` / `ClassManagementController` not written yet (follow-up ticket)
