# Change Log - Anihan SRMS

## 2026-09-21 - Follow-Up: admin.html Browser Walkthrough (post-merge)
**Branch:** `feature/remove-login-audit-and-admin-stats` (PR #59 already merged into `main`
on 2026-09-21T08:03:37Z; this is a doc-only follow-up on the same branch, no `src/` changes)

### Task
PR #59 shipped without one item: a real rendered-browser walkthrough of `admin.html` (hero
layout, DataTable, details modal, console cleanliness) — no Playwright browser bridge was
available in that session. A Playwright MCP browser bridge became available this session;
this closes that open item.

### What happened
Started `./gradlew bootRun` against live MySQL and drove `admin.html` as `admin` via the
Playwright MCP tools. The first page load rendered the OLD "Total Users / Admins /
Registrars / Trainers" stat-card hero, which looked like a serious regression since that
markup was reportedly removed weeks earlier. Diagnosed, not assumed: a `fetch(url, {cache:
'no-store'})` against the live server returned the clean response with zero stat-card
markup, and a `grep` across all three on-disk copies of `admin.html`
(`src/main/resources/static`, `build/resources/main/static`, `bin/main/static`) confirmed
none contain `hero-stats`/`stat-card`/"Total Users". Root cause: a stale browser HTTP cache
in that browser profile from earlier testing, not a code issue. A cache-busted navigation
(`admin.html?cb=1`) rendered the correct page.

### Verified (cache-busted load)
- Hero: full-width, eyebrow/title/subtitle only, no stat cards, no leftover
  `.page-hero-grid` gap.
- User Directory DataTable: all 5 seed accounts render with correct role badges,
  "Showing 1 to 5 of 5 entries"; search filters correctly (`registrar` → 1/5, cleared → 5/5);
  the `dataSrc: ''` fix from the 2026-09-20 stat-removal session still works.
- Details modal: opens with all 10 fields populated (User ID, Username, Email, Role,
  Last/First/Middle Name, Age, Birthdate, Password Last Changed) plus working View Logs /
  Edit User links.
- Zero horizontal overflow at 1280px and 992px.
- Console clean apart from one pre-existing, unrelated `favicon.ico` 500 (documented as Bug
  13 in `bugs.md` — `GlobalExceptionHandler`'s missing-route handling; not touched by this
  branch).

Test server stopped after verification. No files under `src/` changed this session — only
memory-bank verification records.

---

## 2026-09-19 - Remove Login/Logout Auditing + Admin Dashboard Statistics
**Branch:** `feature/remove-login-audit-and-admin-stats`

### Task
Stop treating routine login and logout as audit-worthy events — they were the single
largest category of row in `system_logs` (every sign-in/sign-out cycle wrote one each)
with the least investigative value of anything the table records (a login/logout says
nothing about what a user did once inside). Purge the historical rows already sitting in
the live database, and finish an admin-dashboard cleanup left over from an earlier session
(a leftover empty wrapper div in `admin.html`, orphaned after that session's stat-card
removal). Plan: `docs/superpowers/plans/2026-09-19-remove-login-audit-and-admin-stats.md`.

### Files Modified
| File | Change |
|------|--------|
| `src/main/java/com/example/springboot/controller/AuthController.java` | Removed the two `systemLogService.logAction(...)` calls (end of `login()`, inside `logout()`). `logout()`'s identity-capture block — `userRepository.findByUsername(...).ifPresent(...)`, which existed only to have an identity to log — was deleted entirely along with the log call; `logout()` is back to invalidating the session and clearing the security context, nothing more. `SystemLogService` field/import/constructor dependency removed — constructor arity 5 → 4 (`AuthenticationManager`, `UserRepository`, `UserSecurityAnswerRepository`, `SessionAuthenticationHelper`). |
| `src/test/java/com/example/springboot/service/SystemLogServiceTest.java` | `logActionSavesSystemLog`'s sample action string changed from the now-nonexistent `"User logged in"` to `"Reset password for: registrar"` — a real action `AdminController` still writes — so the test still exercises current, genuine behavior. |
| `src/main/resources/static/admin.html` | Removed a leftover empty `.page-hero-grid` wrapper `<div>` (and its inner div) from the hero section. The stat cards this wrapper used to hold, `updateStats()` in `admin-users.js`, and the five related CSS rules in `dashboard.css` were **already removed in an earlier, separately-merged session** (2026-09-20, PR #58, branch `admin_stats_and_SR_overhaul`) — confirmed via `git log` and a repo-wide grep for every removed identifier (`hero-stats`, `stat-card`, `stat-label`, `stat-value`, `stat-caption`, `updateStats`) returning zero matches *before* this branch's own work began. `.page-hero-grid` itself is not removed as a class — 13 other pages still use it; only `admin.html`'s now-unused instance of it was cleaned up. The hero is now full-width (eyebrow/title/subtitle only), matching every other dashboard page. |

### Files Created
| File | Purpose |
|------|---------|
| `src/test/java/com/example/springboot/controller/AuthControllerWebMvcTest.java` | 3 tests pinning the 2026-09-19 decision that login/logout are not audited: `loginWritesNoSystemLogRow` and `logoutWritesNoSystemLogRow` both assert `verifyNoInteractions(systemLogService)`; `loginStillReturnsUsernameAndRole` confirms the login response contract is unaffected. The logout test deliberately stubs `userRepository.findByUsername("admin")` even though the new code never calls it — a genuine regression pin against the *old* code, which did call it before logging, not a vacuous pass that would succeed against either version. |
| `src/main/sql/migrations/2026-09-19-purge-login-logout-logs.sql` | Idempotent, data-only migration: `DELETE FROM system_logs WHERE action IN ('User logged in', 'User logged out')`, with pre-flight and post-verification `SELECT COUNT(*)` queries plus a sanity `GROUP BY action`. |
| `src/main/sql/backup-2026-09-19-pre-log-purge.sql` | Full `mysqldump` of the live `AnihanSRMS` database, taken and verified (122,172 bytes, 21 `CREATE TABLE`, 1 `system_logs` INSERT) immediately before the purge migration ran. Intentionally untracked, per this project's backup-file convention. |

### Live DB Changes (Docker `mysql-server`, DB `AnihanSRMS` — backup taken first)
Applied `2026-09-19-purge-login-logout-logs.sql`. Pre-purge: **334 total `system_logs`
rows, 197 of them `action IN ('User logged in', 'User logged out')`**. Post-purge: **137
total rows, 0 login/logout rows** (334 − 197 = 137, exact match). Re-ran the migration a
second time to confirm idempotency — 0 further rows deleted, total stayed at 137. Confirmed
via the post-purge action breakdown that every surviving row is a genuine audited action
(e.g. "Updated student record: …", "Created new account: …"), with zero "User logged
in"/"User logged out" entries remaining. **No schema change** — `system_logs`'s columns are
untouched; this is a data-only migration.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 385 tests, 0 failures, 0 errors** (higher than
  the plan's originally-guessed 377 — baseline drift from other work merged into `main`
  before this branch was created, the same recurring pattern noted in the 2026-09-06 and
  2026-09-19 sessions above; not a regression introduced here).
- App booted cleanly against live MySQL (`Started SpringbootApplication in 11.65 seconds`,
  zero schema-validation errors).
- Live `curl` verification against the running app + real MySQL: a real login
  (`admin`/`password123`, succeeded, returned `ROLE_ADMIN`) left `system_logs` unchanged at
  137; a real logout also left it unchanged at 137; a real still-audited action
  (`PUT /api/account/details`, a safe no-op re-save of admin's own existing personal
  details) DID write a fresh row (137 → 138, action "Updated own personal details") —
  proving the removal was surgical rather than accidentally disabling auditing elsewhere.
- Static/API-level inspection confirmed: `admin.html` carries zero stat-related markup and
  a full-width hero; `dashboard.css` carries zero stat rules; `admin-users.js` carries zero
  `updateStats` references; `logs.html` returns HTTP 200; `GET /api/logs?rangeDays=3000`
  returns all 138 surviving rows with zero containing "logged in"/"logged out".
- **Not completed:** an actual rendered-browser walkthrough of `admin.html` (visual hero
  layout, DataTable rendering, details modal, a clean console) — no Playwright browser
  bridge extension was available in this environment, a known recurring limitation in this
  project's history (see the 2026-09-19 ID-photo and 2026-09-20 admin-stats sessions).
  Flagged as an open item in `activeContext.md`, not silently skipped.

---

## 2026-09-20 - Student Record Edit Form: Category Tabs + Per-Section Edit Lock
**Branch:** `admin_stats_and_SR_overhaul`

### Task
Reorganize the registrar's Student Record edit page (`student-records.html`) — a single
12-section, 65+ field scrolling form — into detail categories the way the student portal
wizard already groups its own fields, with each category viewed/edited in place (tabs, not
a new page or a modal). Per user follow-up: each category must start read-only, requiring an
explicit "Edit Section" click before its fields become editable.

### Files Modified
| File | Change |
|------|--------|
| `static/student-records.html` | Wrapped the form's 12 flat sections into 3 Bootstrap tabs — **Personal Information** (Identifiers, ID Picture, Personal Details, Contact, "Religion & Siblings" — renamed from the ambiguous "Family / Religion" now that a real Family tab exists), **Family Background** (Father/Mother/Guardian, unchanged), **Enrollment & Academics** (Enrollment, OJT, TESDA, School Years). Each tab-pane's content is wrapped in `<fieldset data-edit-section="..." disabled>` with an "Edit Section" toggle button above it. No field `id` was changed, so `populateForm()`/`buildPayload()` in the JS needed no changes to their field mapping. Subtitle text updated to explain the new Edit-Section gate. `dashboard.css?v=2→3`, `registrar-student-records-edit.js?v=5→6`. |
| `static/js/registrar-student-records-edit.js` | Added `setSectionEditable()` / `setupSectionEditToggles()` — toggles each tab's `<fieldset>` `disabled` attribute, which natively cascades to every descendant control including ones added later (new School Year rows). `setupDirtyTracking()` no longer skips fields that start disabled (safe: disabled fields never fire input/change events, so nothing extra fires). |
| `static/css/dashboard.css` | Added `.record-edit-tabs`, `.tab-pane-toolbar`, `.tab-pane-hint`, `.edit-section-fieldset` (resets default fieldset chrome to stay layout-invisible), and `#editRecordForm .section-title` — scoped to this one form's ID rather than unscoped, after a repo-wide grep showed `.section-title` is used on 15 other pages that must not be visually affected. |
| 15 other `*.html` pages | `dashboard.css?v=2` → `?v=3` — this project's cache-buster convention requires bumping the version on every page that loads an edited shared CSS file in the same change, or `bootRun` keeps serving the stale compiled copy. |

### A bug caught before shipping, not after
The new tab/pane IDs were initially `tab-personal`/`pane-personal` — duplicating IDs already
used by this same page's pre-existing "Edit Account" modal (present on every dashboard page).
Duplicate IDs make `getElementById`/Bootstrap's `data-bs-target` resolution ambiguous, which
would have silently broken the unrelated Edit Account modal's own tab switching. Renamed to
`tab-record-personal`/`pane-record-personal` etc.; verified via grep that only the original
Edit Account modal instance of the generic IDs remains.

### Design decisions
- **Save stayed a single global button/endpoint.** The backend has no per-category save
  route, and the task was a display/interaction reorg, not a request to add granular
  persistence. A locked field's value is simply whatever the server already has, so it
  round-trips correctly through the existing whole-record PUT regardless of which tabs were
  ever unlocked.
- **Native `<fieldset disabled>` over manual per-field enable/disable bookkeeping** — it
  cascades to descendants automatically and dynamically (verified this holds for elements
  added after the fieldset renders, e.g. a new School Year `<tr>`), so locking a whole
  category is one attribute write, not a loop over every input/select/button in it.
- **Education was not added as a 4th tab.** DTO inspection confirmed
  `StudentRecordUpdateRequest`/`StudentRecordDetailsResponse` have never exposed the
  student's Educational Background — the registrar edit form has no such section today, and
  adding one would be new functionality outside a reorg task's scope.

### Verified
- Repo-wide grep of `.section-title` usage (15 files) before deciding to scope the new CSS
  rule to `#editRecordForm` rather than unscoped.
- Live: started `./gradlew bootRun` against local MySQL and fetched the served page/JS over
  HTTP — grep on the raw response confirms all 3 tab/pane IDs, all 3 Edit Section buttons,
  and the expected fieldset count are present in what the server actually sends (not a stale
  build).
- **Not completed:** a full interactive click-through (unlock a tab, confirm the others stay
  locked, edit, save) — blocked by this machine's local MySQL missing the `student_number`
  column (`Unknown column 'sr1_0.student_number'` on every `/api/registrar/student-records/**`
  call), confirmed via `DESCRIBE student_records` to be pre-existing local schema drift
  unrelated to this session's diff (no SQL/Java file was touched). No browser automation
  tooling was available in this environment either. See `activeContext.md` for the open
  question to the user about syncing the local DB to unblock this.

---

## 2026-09-20 - Remove Admin Statistics Panel
**Branch:** `admin_stats_and_SR_overhaul`

### Task
Remove the admin statistics panel (Total Users / Admins / Registrars / Trainers stat cards)
from the admin dashboard hero section, without affecting any other admin dashboard
functionality.

### Files Modified
| File | Change |
|------|--------|
| `static/admin.html` | Removed the `.hero-stats` block (4 `.stat-card` articles) from the page hero, leaving the same plain single-column hero already used by every other dashboard page. |
| `static/js/admin-users.js` | Removed the `updateStats()` helper and its call inside the DataTable `ajax.dataSrc`. Replaced the now-empty `dataSrc` callback with `dataSrc: ''` — required because `/api/admin/users` returns a bare array and DataTables' default `dataSrc` (`"data"`) expects `{data:[...]}`; simply deleting the callback would have broken the table. |
| `static/css/dashboard.css` | Removed `.hero-stats`, `.stat-card`, `.stat-label`, `.stat-value`, `.stat-caption` and their 2 responsive `@media` overrides — confirmed via repo-wide grep unused anywhere else. |

### Verification
- Repo-wide grep for every removed identifier (`hero-stats`, `stat-card`, `stat-label`,
  `stat-value`, `stat-caption`, `totalUsersStat`, `adminUsersStat`, `registrarUsersStat`,
  `trainerUsersStat`, `updateStats`) → zero remaining references.
- No backend/DTO/test code referenced these identifiers — the stats were pure client-side
  arithmetic over the same `/api/admin/users` payload already powering the table.
- Frontend-only change; no `./gradlew` build required. Manual browser smoke test still
  recommended (see `activeContext.md`) before merge.

---

## 2026-09-19 (follow-up 3) - Duplicate "Account Locked" Audit Log Entries
**Branch:** `security_questions`

### Task
User asked to confirm the audit log records lockout events and which account — it already
did (`system_logs` correctly showed "Account locked due to repeated failed security question
attempts" with the right username/role for `registrar`), but checking the live data surfaced a
real bug: the same lock event was being logged multiple times.

### Root Cause
`PasswordRecoveryController.verify()`'s `logIfNewlyLocked()` logged whenever the account was
*currently* locked after a failed attempt — with no way to tell "this attempt just caused the
lock" apart from "this account was already locked before this attempt, and got rejected
immediately without changing anything." Every subsequent attempt against an already-locked
account re-logged the same "Account locked..." message.

### Fix
`verify()` now checks whether the account was locked *before* calling the service, and only
logs on the failure path when it wasn't — i.e. only at the actual transition into lockout, not
on repeated bounces off an already-locked account.

### Verification
`./gradlew test` → 363 tests, 0 failures (unchanged — no test covered this controller-level
timing distinction yet; still only service-level Mockito coverage exists for this feature, per
the earlier-noted open item).

### Also noted (also two frontend gaps closed this session, not yet written up until now)
- The admin Users table's status column only ever checked `enabled`, never the new
  `securityLocked` field, so a locked account still showed "Active" — fixed in
  `admin-users.js` (`renderStatusBadge` now takes both, renders a distinct amber "Locked"
  badge) and `admin.html` (DataTables column `render` callback now passes the full `row`).
- `admin-users.js` is loaded as `admin-users.js?v=2` on `admin.html` — editing the file without
  bumping that number meant browsers kept serving the pre-fix cached copy indefinitely, since
  the URL itself never changed. Bumped to `?v=3`. **Lesson, stated plainly for next time: any
  edit to a JS/CSS file loaded with an explicit `?v=N` on its `<link>`/`<script>` tag must bump
  that number in the same change, every time — this project relies on that convention instead
  of cache headers, and skipping it silently defeats it.**

---

## 2026-09-19 (follow-up) - Lockout Counter Was Silently Rolled Back on Every Wrong Answer
**Branch:** `security_questions`

### Task
User reported that failing the security questions 3 times did not lock the account.

### Root Cause
`SecurityQuestionService.verifyAnswers()` is `@Transactional`. On a wrong answer it correctly
incremented `failed_security_attempts` (and set `security_locked` on the 3rd) and called
`userRepository.save(user)` — then threw `IllegalArgumentException` so the controller could
return a 400 to the caller. Spring rolls back the *entire* transaction by default whenever an
unchecked exception escapes a `@Transactional` method, so that save was silently undone every
time. The 21 `SecurityQuestionServiceTest` cases all passed because they mock `UserRepository`
directly and never exercise real Spring transaction/rollback semantics — this is a real gap in
that test file's coverage, not just a code bug, and is worth remembering for any other
service method that deliberately throws after a write it needs to keep.

### Fix
`@Transactional(noRollbackFor = IllegalArgumentException.class)` on `verifyAnswers()`.

### Verification
Recompiled, restarted the app, and ran the real 3-attempt sequence against the live database
via the actual HTTP endpoints (not mocks): attempt 1 → counter 1, attempt 2 → counter 2,
attempt 3 → counter 3 **and `security_locked=1`**, a 4th attempt rejected immediately without
checking answers, and a subsequent login with the correct password also blocked with the
locked-account message — confirming the lockout blocks normal login, not just forgot-password,
as designed. Since `admin` is the only admin account, this also locked itself out with no
other admin available — used `BREAKGLASS-account-unlock.md` for the first time for real,
confirming that procedure works as written. Noted in passing: `admin`'s password no longer
matches the `password123` seed value and `password_changed_at` is set, consistent with the
user having already exercised the reset-password step themselves during their own testing —
not a bug, just means a live login re-check needs the user's current password, not the seed.

### Open follow-up
Add a `@DataJpaTest` or full-context integration test for `verifyAnswers()` that exercises a
real transaction (not a mocked repository), specifically to catch this class of "state change
made right before a deliberate throw gets rolled back" bug — Mockito-based service tests
structurally cannot catch it.

---

## 2026-09-19 - Security Questions / Forgot Password Feature
**Branch:** `security_questions`

### Task
Implement the full security-questions/forgot-password feature reached after an extended
design discussion with the user (every point below reflects an explicit decision made in
that discussion — see `decisions.md`): mandatory first-login setup of 2 security questions,
editing them later, a public forgot-password flow, a 3-strike lockout with 15-minute decay
clearable only by an admin, and an admin unlock action.

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/migrations/2026-09-19-security-questions.sql` | New tables + lockout columns + `uq_email` + seed-account email fix. Idempotent, guarded on column/index existence (never constraint name). |
| `src/main/sql/BREAKGLASS-account-unlock.md` | Manual DB procedure for the case where the sole admin account itself gets locked/disabled and there's no other admin to click "Unlock" in the UI. |
| `model/SecurityQuestion.java`, `model/UserSecurityAnswer.java` | The 6-question catalog entity and the per-user, per-slot answer entity (default question XOR plaintext custom question, BCrypt answer hash). |
| `repository/SecurityQuestionRepository.java`, `repository/UserSecurityAnswerRepository.java` | — |
| `service/SecurityQuestionService.java` | Setup/edit validation (exactly one of default/custom per slot, no duplicate default question, custom question rejected if it word-for-word matches a default), the lockout state machine, email lookup, password reset. |
| `service/SessionAuthenticationHelper.java` | One shared mechanism for issuing either a full session (real role) or a restricted, single-purpose session (a synthetic `ROLE_PENDING_*` authority) — reused for all three "not fully authenticated yet" states in this feature. |
| `controller/SecurityQuestionController.java` | `/api/account/security-questions/**` — default-questions list, setup, get-current-for-edit, edit. |
| `controller/PasswordRecoveryController.java` | `/api/password-recovery/**` — lookup, verify, reset. |
| `dto/SecurityQuestionResponse.java`, `SecurityAnswerSlotRequest.java`, `SetupSecurityAnswersRequest.java`, `EditSecurityAnswersRequest.java`, `EmailLookupRequest.java`, `VerifyAnswersRequest.java`, `ResetPasswordRequest.java`, `SecurityQuestionTextsResponse.java` | Request/response shapes. `ResetPasswordRequest` reuses `UpdatePasswordRequest`'s hoisted password-policy constants rather than re-typing the regex. |
| `static/security-question-setup.html` + `js/security-question-setup.js` | Mandatory, non-skippable first-login setup page. |
| `static/forgot-password.html` + `js/forgot-password.js` | Email entry. |
| `static/forgot-password-questions.html` + `js/forgot-password-questions.js` | Answer both questions. |
| `static/reset-password.html` + `js/reset-password.js` | New password twice. |
| `static/js/password-toggle.js` | Password reveal-toggle logic extracted out of `auth-guard.js` so pages that don't include it (login, reset-password) still get the eye-icon toggle. |
| `src/main/sql/backup-2026-09-19-pre-security-questions.sql` | Pre-migration live DB backup. |
| `test/.../SecurityQuestionServiceTest.java` | 21 tests: setup validation (duplicate default, custom-matches-default, both-custom, both-fields-set), replace-answers password gate, email lookup (not found / disabled / locked / setup-incomplete / happy path), the full lockout state machine (immediate reject when locked, success resets counter, wrong answer increments, 3rd strike locks, 15-minute decay), password reset (mismatch, same-as-current, happy path). |

---

## 2026-09-19 - Move ID Photo Upload from Student Portal to Registrar
**Branch:** `feature/move-id-photo-to-registrar`

### Task
Per the user's request: remove every document-upload feature from the public student
enrollment wizard (including the 1x1/2x2 ID photo), clean up the UI it leaves behind, and
give the Registrar an ID-picture upload on the per-student record screens instead — while
making sure the Registrar's existing document upload/download/view features keep working.
Plan: `docs/superpowers/plans/2026-09-19-move-id-photo-to-registrar.md`.

### Part A — Removed the student-portal upload feature entirely
**Deleted:** `service/StorageService.java`, `model/StudentUpload.java`,
`repository/StudentUploadRepository.java`, `dto/student/UploadRefDto.java`.

**Modified:** `controller/StudentDetailsController.java` (removed the `/{studentId}/upload`
and `/files/{uploadId}` endpoints and the `StorageService` field); `service/
StudentDetailsService.java` (removed `saveUpload`/`getEnrollingUpload`/`toUploadRef` and the
upload lookups in `buildResponse`; corrected the now-false comment on `startOrResume` — the
record is still created early, but now purely so the wizard can resume from
`sessionStorage`, not for an upload FK); `dto/student/StudentDetailsResponse.java` (record
31 → 29 components, `idPhotoRef`/`baptismalCertRef` removed); `service/RegistrarService.java`
(`deleteRecord()` no longer purges filesystem uploads — the `documents` table it already
purges now covers the Registrar-side ID picture; constructor 12 → 10 params).

**Frontend:** `static/student-details.html` — removed the "Document Upload" section (Religion
is now the closing section of Step 1) and the dead `.upload-preview`/`.upload-link` CSS.
`static/js/student-details.js` — removed `pendingIdPhoto` state, `setupFileUploads`,
`setupFileInput`, `uploadPendingFile`, the deferred-upload block in `submitForm`, and the
`idPhotoRef` populate block; `CUSTOM_VALIDATED_IDS` emptied (no conditional fields need it
now). Cache-buster bumped `?v=6` → `?v=7`.

**Cleanup:** 2 stranded files under `uploads/students/SR20260001/` (confirmed 0 matching
rows in `student_uploads` before deleting) — these turned out to be git-tracked rather than
gitignored as the plan assumed, so their deletion needed its own follow-up commit.
`application.properties` — `app.storage.root` commented as unused (kept, decision 3).

### Part B — Registrar ID picture, stored in `documents`
**Backend:** `DocumentRepository.findByStudentStudentIdAndDocumentType` (exact lookup — the
existing `searchSummaries` LIKE-search is too loose to key one photo on).
`DocumentService.ID_PICTURE_TYPE = "ID Picture (1x1 / 2x2)"`, a separate
`ID_PICTURE_EXTENSIONS` whitelist (jpg/jpeg/png/webp) and `ID_PICTURE_MAX_BYTES` (2MB) —
deliberately apart from `ALLOWED_EXTENSIONS` so the general Documents page keeps accepting
only pdf/docx/xlsx. `uploadIdPicture()` replaces the existing row in place on re-upload (no
accumulation); `findIdPicture()`/`deleteIdPicture()`. `DocumentController` gains
`POST/GET/DELETE .../documents/id-picture[/{studentId}]`, each writing `system_logs`; no
`SecurityConfig` change needed (`/api/registrar/**` is already REGISTRAR-only).

**Frontend:** `static/student-records.html` — new "ID Picture" section on the edit form
(preview, file input, Upload/Remove buttons, inline alert); matches the page's actual
`<h6 class="section-title">` convention (the plan's own snippet used `.section-heading`,
undefined on this page — corrected during implementation, IDs unchanged).
`static/js/registrar-student-records-edit.js` — picture load/upload/remove logic, kept out
of `buildPayload()` so Save Changes can never disturb it (same invariant as the student
number). `static/registrar.html` — ID picture card in the details modal.
`static/js/registrar-students.js` — populates it via a `HEAD` request first, so a missing
picture never renders a broken image. `static/js/registrar-documents.js` — excludes "ID
Picture (1x1 / 2x2)" from the upload-type dropdown while keeping it in the filter dropdown
(decision 4), and widens the view-modal's inline-preview test to include `image/*`.
Cache-busters bumped across all four files.

### Verified
- `./gradlew test` → **BUILD SUCCESSFUL — 374 tests, 0 failures, 0 errors** (363 baseline +
  11: 1 for the repository/service lookup, 6 for `uploadIdPicture`, 4 for the controller
  endpoints).
- `ddl-auto=validate` boot against live MySQL → **PASS** (started in 12.7s). Confirms an
  unmapped `student_uploads` (still present, per decision) does not break startup.
- Live API verification (curl, real MySQL, session cookie): upload → preview fetch → replace
  with a second picture reuses the same `documentId` (no accumulation, DB count stayed 1) →
  non-image rejected ("Allowed: jpg, jpeg, png, webp") → oversized (2MB+1 byte) rejected
  ("exceeds the 2MB size limit") → neither rejection wrote a row → remove → 404 on re-fetch,
  DB count back to 0 → `system_logs` rows for all three actions confirmed. Existing PDF
  upload/view/download and TOR generation against the *general* documents endpoint verified
  unaffected. Test student + its documents deleted afterward; live DB returned to its
  pre-session state (10 students).
- **No schema change** — `documents` table already existed; no migration needed.

### Two pre-existing, unrelated findings from live verification (not fixed — out of scope)
Logged as **Bug 12** and **Bug 13** in `bugs.md`:
1. `documents.file_type VARCHAR(50)` is too short for the docx/xlsx MIME strings
   `DocumentService.ALLOWED_EXTENSIONS` itself declares (73/65 chars) — docx/xlsx upload has
   always failed against a real database with a truncation error; the mocked test suite
   never exercises a real column-length constraint so this was never caught.
2. `GlobalExceptionHandler`'s catch-all turns any genuinely non-existent route under a
   `permitAll()` prefix into a 500 (`NoResourceFoundException` has no dedicated handler)
   instead of a 404 — reproduced on the now-deleted `/api/student/{id}/upload` and
   `/api/student/files/{id}` paths, and confirmed pre-existing (any nonsense path under
   `/api/student/**` does the same; authenticated prefixes 401 first, which is why this went
   unnoticed elsewhere).

### Environment note
No Playwright browser extension / `playwright-core` was available in this session, so Task
14's browser-click verification was done at the API level (curl + live MySQL) instead of a
rendered-DOM walkthrough. Everything server-side, DB-backed, and audit-logged was verified;
the purely visual pieces (preview image rendering, the "No ID picture on file" placeholder,
button enable/disable state, a clean browser console) were not independently confirmed this
session.

---

## 2026-09-19 - schema.sql vs Live DB Comparison + Sync (security questions)
**Branch:** `main` (DB-sync task)

### Task
Compare `src/main/sql/schema.sql` against the live `AnihanSRMS` MySQL database and, if
they differ, bring the live DB into line with the file.

### Drift found
The `security_questions` branch had been merged into `main` (`b17c031`, `64be20e`) but
its migration had never been applied to the live database. `schema.sql` described **21
tables**; live had **19**.

| Missing from live | Kind |
|---|---|
| `security_questions` table | whole table |
| `user_security_answers` table | whole table |
| `users.security_locked` | column |
| `users.failed_security_attempts` | column |
| `users.security_lockout_started_at` | column |
| `users.email` UNIQUE index | index |

All other differences were the four known **cosmetic** categories (FK/unique-index
auto-names, secondary-index listing order, `grades` physical column order) — identical
to the 2026-07-14 and 2026-09-06 findings.

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/schema.sql` | New tables, 3 new `users` columns, `email` now `UNIQUE`, seed accounts' emails changed to `@anihan.local`, header dated. |
| `model/User.java` | `securityLocked`, `failedSecurityAttempts`, `securityLockoutStartedAt` fields. |
| `service/CustomUserDetailsService.java` | `accountNonLocked` (previously hardcoded `true`) now reads `!user.getSecurityLocked()` — Spring Security's own `LockedException` now enforces the lockout on every login, not just forgot-password. |
| `exception/GlobalExceptionHandler.java` | Distinct `LockedException` / `DisabledException` handlers (previously both fell through to one generic message). |
| `controller/AuthController.java` | `login()` checks setup status and issues a `ROLE_PENDING_SETUP`-only session instead of the real role when incomplete; `/me` gained `securityQuestionsSetUp`. |
| `service/AdminService.java`, `controller/AdminController.java` | `unlockUser()` + `PUT /api/admin/users/{id}/unlock` — clears `security_locked` and `enabled` together, one action regardless of which condition(s) apply. |
| `dto/AdminUserResponse.java` | Added `securityLocked` (arity 11→12 — fixed 3 existing test call sites, same class of fix as this project's earlier `StudentRecordDetailsResponse` arity incidents). |
| `dto/UpdatePasswordRequest.java` | Hoisted the password-policy regex/length into public constants so `ResetPasswordRequest` can reuse them exactly. |
| `config/SecurityConfig.java` | Matchers for the 4 new pages/endpoint groups and the 3 synthetic `ROLE_PENDING_*` roles; `/api/account/**` narrowed from `authenticated()` to the 3 real roles specifically, so a pending-role session (which Spring Security still considers "authenticated") can't reach account settings. |
| `static/index.html` | "Forgot Password?" link; role-routing extended for `ROLE_PENDING_SETUP`; password-toggle include. |
| `static/js/auth-guard.js` | Mandatory-setup client-side redirect; "Edit Security Questions" modal wiring. |
| `static/admin.html`, `registrar.html`, `trainer.html` | "Edit 'Forgot Password' Security Questions" button + modal in the Account Settings tab. |
| `static/admin.html` / `static/js/admin-users.js` | "Unlock Account" button in the user-details modal. |
| `static/reset-password.html`, `static/reset-password.js` | Password-toggle include. |
| `static/css/login.css` | `.password-toggle-btn` styling (previously only in `dashboard.css`, which the public pre-login pages don't load). |
| `static/css/dashboard.css` | Green `.btn-save` styling extended to `#editSecurityQuestionsModal` (it was scoped to `.edit-account-modal` only); `?v=2` cache-buster added to the `<link>` tag across all 16 pages that load this file. |
| `test/.../AdminServiceTest.java`, `AdminControllerWebMvcTest.java`, `AdminBulkLoadWebMvcTest.java` | +2 new tests for `unlockUser`; 3 existing `AdminUserResponse` call sites fixed for the new arity. |

### Two Real Bugs Found During Live Verification (both fixed)
1. **Login failed for every account** immediately after implementation. The migration file
   had been written and verified against the throwaway H2 test database, but never actually
   *applied* to the live MySQL database — so `users` was missing the 3 new lockout columns
   the entity now queries on every login, and the seed accounts still had their old
   `@example.com` placeholder emails. Fixed: backed up the live DB
   (`backup-2026-09-19-pre-security-questions.sql`), applied the migration, verified it's
   idempotent by re-running it, confirmed live via `curl` that login and the forgot-password
   email lookup both work. This is the same class of mistake flagged repeatedly elsewhere in
   this changelog (2026-07-09, 2026-09-06) — applying a migration to the live database is
   part of finishing a schema change, not a follow-up step.
2. **A CSS button-color fix appeared to have no effect** no matter how the browser was
   refreshed, hard-refreshed, or tested in a private window. Root cause: `./gradlew bootRun`
   copies `src/main/resources` into `build/resources/main` once at startup and does not watch
   for live edits — the already-running server process kept serving a stale compiled copy of
   `dashboard.css` regardless of browser-side caching. Confirmed by diffing
   `build/resources/main/static/css/dashboard.css` against the source file, and by an isolated
   headless-Edge screenshot test (`msedge.exe --headless --screenshot=...` against a minimal
   repro page using the real served stylesheets) that proved the CSS itself rendered correctly
   before the real cause was identified. Fix: restart the app process, not just the browser;
   added `?v=2` cache-busting to `dashboard.css` as a belt-and-suspenders measure since it
   never had one.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 363 tests, 0 failures, 0 errors** (was 340).
- Live round trip against real MySQL (post-migration): `admin`/`password123` login → 200
  `ROLE_PENDING_SETUP` → fetched the 6 default questions → completed setup → session upgraded
  to `ROLE_ADMIN`, `/api/auth/me` → `securityQuestionsSetUp: true` → forgot-password lookup by
  `admin@anihan.local` returns the 2 question texts. The test security-question answers
  created during this check were deleted afterward so the user can go through the real setup
  flow themselves; the live password-reset step was deliberately not exercised to avoid
  changing the real admin password.
- Not yet done: WebMvc tests for the 2 new controllers and the admin unlock endpoint; a full
  Playwright/manual browser pass of the end-to-end journeys.
| `src/main/sql/schema.sql` | `user_security_answers.slot` `TINYINT` → **`INT`**; `users.failed_security_attempts` `TINYINT` → **`INT`**. Both were type mismatches against the JPA entities that broke `ddl-auto=validate` (see below). |
| `src/main/sql/migrations/2026-09-19-security-questions.sql` | Same two declarations corrected in the `CREATE TABLE` / `ADD COLUMN` statements, **plus two new guarded idempotent steps** — **2b** (`MODIFY COLUMN slot INT`) and **3b** (`MODIFY COLUMN failed_security_attempts INT`) — so a database that already ran the previous revision is repaired on re-run instead of staying unbootable. Added a verification query asserting `slot` is `int`. |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md`, `testing.md` | Session notes. |

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/backup-2026-09-19-pre-schema-sync.sql` | Full pre-sync `mysqldump --databases AnihanSRMS --routines --triggers` (116,733 bytes, 19 `CREATE TABLE`). Taken before any live DDL. |

### Two real bugs in the merged SQL — found by `ddl-auto=validate`, not by the tests
The migration applied cleanly and every one of its own verification queries passed, but
the application then **refused to boot** against the result. Two columns had been
declared `TINYINT` while their JPA fields are `Integer`:

```
Schema validation: wrong column type encountered in column [slot]
in table [user_security_answers]; found [tinyint], but expecting [integer]

Schema validation: wrong column type encountered in column [failed_security_attempts]
in table [users]; found [tinyint], but expecting [integer]
```

Both corrected to `INT`, which also matches the pre-existing
`student_tesda_qualifications.slot INT` convention. Deliberately left alone:
`users.security_locked` stays `TINYINT(1)` (the correct MySQL mapping for its `Boolean`
field) and `security_lockout_started_at` stays `DATETIME` (`LocalDateTime`).

This means the security-questions feature could not have worked against **any** database
built from the merged files — the defect was in the SQL sources, not in the live DB.

### Live DB Changes (Docker `mysql-server`, DB `AnihanSRMS` — backup taken first)
Applied `2026-09-19-security-questions.sql`: created both tables, seeded the 6 default
questions, added the 3 lockout columns, rewrote the 3 seed-account emails from
`@example.com` placeholders to `admin@anihan.local` / `registrar@anihan.local` /
`trainer@anihan.local`, and added the `uq_email` UNIQUE index. Then re-applied twice more
as the two type fixes landed. Live DB: 19 → **21 tables**.

Pre-flight check before adding `uq_email`: no duplicate and no NULL/empty emails in
`users`, so the constraint could not fail mid-migration.

### Method — dry run before touching live
The live backup was restored into a throwaway `schema_check` database and the migration
applied **there** first, so the real run was against a proven path. Re-running it on that
copy produced a byte-identical structure and left `security_questions` at 6 rows (not 12),
confirming idempotency. A second throwaway DB built from `schema.sql` supplied the
comparison target. Both dropped afterwards; `schema.sql`'s hard-coded
`CREATE DATABASE`/`USE AnihanSRMS` lines were stripped first so nothing could redirect
into the live database.

### Verification
- **`ddl-auto=validate` boot against live MySQL → PASS**: `Started SpringbootApplication
  in 9.368 seconds`, zero `ERROR` / `Schema-validation` / `SchemaManagementException`
  lines. The authoritative check — the Gradle suite runs on H2 and cannot catch this.
- `./gradlew test` → **BUILD SUCCESSFUL — 363 tests, 0 failures, 0 errors** (was 332).
- Final structural diff (live `--no-data` dump vs a fresh DB built from the corrected
  `schema.sql`) → **cosmetic only**; no functional drift.
- Data preserved: 10 students, 5 users, 323 `system_logs`, 8 classes.
  `user_security_answers` is empty (0 rows) — nobody has set questions yet.

---

## 2026-09-06 - Post-Merge Bug Fix + Live DB Sync
**Branch:** `main`

### Task
Get `main` green again after two branches were merged into it — `grade_input_fix`
(TESDA grading overhaul — `GradeEquivalent`, changed `Grade` entity, a trailing
`BigDecimal totalGwa` component on the `StudentRecordDetailsResponse` record, a new
`GradeRepository` dependency in `RegistrarService.buildDetailsResponse`) and
`student-ID-number` (registrar-controlled `student_number` +
`StudentNumberController`/service/tests) — then bring the live `AnihanSRMS` MySQL
database into line with the updated `src/main/sql/schema.sql` by applying the five
outstanding migrations.

### Already-committed test fixes (this session, before this doc commit)
| Commit | Fix |
|--------|-----|
| `1916904` | `RegistrarStudentNumberControllerWebMvcTest.details(...)` helper passed 32 args to the now-33-component `StudentRecordDetailsResponse` record. The merge reconciled the production `StudentRecordDetailsResponse.from(...)` factory but not this test from the other branch. Added one trailing `null` for `totalGwa`. Test-only. |
| `fec8004` | `RegistrarStudentNumberServiceTest` (from `student-ID-number`) had no `@Mock GradeRepository`, so `@InjectMocks` left it null → 7 NPEs at `RegistrarService.buildDetailsResponse` (~line 355, which computes `totalGwa` via `GradeEquivalent.gwa(gradeRepository.findByStudentStudentId(...))`). Added the mock + `when(gradeRepository.findByStudentStudentId(any())).thenReturn(List.of());` in the existing `stubEmptyChildLookups()` helper. Test-only; no production code changed. |

Full suite after both fixes: **`./gradlew test` → 332 tests, 0 failures, 0 errors,
0 skipped** (was 299 on `student-ID-number`; `grade_input_fix` added `GradeEquivalentTest`
and rewrote `TrainerGradeServiceTest` / `TrainerGradeControllerWebMvcTest`).

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/schema.sql` | **Header comment only** — no table body touched. Added a `-- Updated: 2026-08-29` changelog line (dropped `subjects.trainer_id`; grades overhauled to the TESDA model) and an "existing databases that predate 2026-08-29 should also run…" note pointing at the two 2026-08-29 migrations (with "delete any existing grade rows first"). |
| `src/main/sql/migrations/2026-08-29-grades-overhaul.sql` | (a) Corrected the stale header comment that claimed the live table had 0 rows — it had 4 test rows, deleted this session before applying. (b) Added a guarded/idempotent **step 6** that shrinks `grades.remarks` to `VARCHAR(20)` to match `schema.sql` (`MODIFY COLUMN` — naturally idempotent). |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md`, `testing.md`, `bugs.md` | Session notes. |

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/backup-2026-09-06-pre-merge-sync.sql` | Full pre-sync `mysqldump --databases AnihanSRMS --routines --triggers` (116,859 bytes, 19 `CREATE TABLE`). Taken before any live DDL. |
| `docs/superpowers/plans/2026-09-06-post-merge-db-sync.md` | The approved plan for this session. |

### Live DB Changes (Docker `mysql-server`, DB `AnihanSRMS` — backup taken first)
Deleted the 4 test rows from `grades` (`grade_id` 1–4: `SR20260007`/`BPP-102`,
`SR20260004`/`COOK-101`, `SR20260014` & `SR20260015`/`CAP-111`; all `locked=0`) —
user-approved disposable test data, so the grades overhaul runs against an empty table.
`grades` now has 0 rows.

Applied these 5 migrations **in date order**, verified each, then re-applied all 5 once
more (idempotent — byte-identical result, no duplicate columns/FKs):
| Migration | Effect on live DB |
|-----------|-------------------|
| `2026-08-26-subjects-competency-type.sql` | `subjects.competency_type VARCHAR(15) NOT NULL` (6 existing subjects backfilled `CORE`); `subjects.qualification_code` relaxed to nullable. |
| `2026-08-26-subjects-code-update-cascade.sql` | `grades`→`subjects` and `classes`→`subjects` FKs recreated as `fk_grades_subject` / `fk_classes_subject`, `ON UPDATE CASCADE`, `ON DELETE RESTRICT`. |
| `2026-08-27-add-student-number.sql` | `student_records.student_number VARCHAR(20) NULL` after `student_id` + `uq_student_number` UNIQUE index (all 10 students NULL — expected). |
| `2026-08-29-drop-subjects-trainer-id.sql` | `subjects.trainer_id` column + its FK dropped. |
| `2026-08-29-grades-overhaul.sql` | Dropped `midterm_grade` / `finals_grade`; added `final_percentage`, `re_exam_percentage` (`DECIMAL(5,2)`), `grade_status VARCHAR(5)`; renamed `hours_studied` → `hours_rendered`; `remarks` → `VARCHAR(20)`. `grades` now has 13 columns. |

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 332 tests, 0 failures, 0 errors, 0 skipped**.
- **Structural diff** (live `--no-data` dump vs a throwaway DB built from `schema.sql`):
  only cosmetic differences — FK auto-names (`classes_ibfk_2` vs `classes_ibfk_3`;
  `grades_ibfk_2` vs `fk_grades_class`), unique-index name (`username` vs `uq_username`),
  secondary-index listing order, and `grades` physical column order (the name-stripped
  column definition set is byte-identical). **No real structural drift** — same result as
  the 2026-07-14 comparison.
- **`ddl-auto=validate` boot against live MySQL → PASS**: `Started SpringbootApplication
  in 10.192 seconds`, 19 JPA repositories, zero `Schema-validation` /
  `SchemaManagementException` lines. The merged `main` entities validate against the
  synced live schema.
- Live DB still 19 tables. No `schema.sql` structural change was needed — its table bodies
  already describe the target state; only the header notes lagged.

---

## 2026-08-30 - Student Number Export / Import / Report Page
**Branch:** `fix/student-ID-number`

### Task
Make bulk entry of student numbers practical: a report page listing all students with filters
to isolate those still missing a number, export of the filtered set to CSV/Excel for encoding,
and import of the completed sheet back. Editability (single assignment) already shipped on
2026-08-27 and is reused here.

### Design constraint (from the user)
The school's real record format is not yet known, so the file-reading rules must be **easily
editable later**. Everything about how a sheet is recognised lives in ONE file —
`StudentNumberImportMapping` — and the parser/service/controller/UI carry no format knowledge.

### Files Created
| File | Purpose |
|------|---------|
| `service/StudentNumberImportMapping.java` | **The file to edit when the real format arrives.** Header alias lists per column, the match key, `normaliseHeader` (case/punctuation-insensitive), `normaliseValue` (trims, strips Excel's `.0` tail, preserves leading zeros), `HEADER_SCAN_ROWS`, `MAX_DATA_ROWS`, and the canonical export column names. |
| `service/StudentNumberSheetParser.java` | Format mechanics only, no DB access: RFC-4180 CSV splitter (read side of `SystemLogExportService.csvEscape`), XLSX via POI `DataFormatter` (so `2026001` does not come back as `2026001.0`), UTF-8 BOM strip, and header-row auto-detection over the first 10 rows — school files carry title blocks above the real header. |
| `service/StudentNumberExportService.java` | Builds the encoding sheet. Headers on row 1 using the canonical aliases so an export re-imports untouched; Student Number last and blank; written as a **text** cell so `0012` is not eaten by Excel. Modelled on `SystemLogExportService`. |
| `service/StudentNumberExportFormat.java` | CSV/XLSX. Separate from `SystemLogExportFormat`, which is log-scoped and includes non-round-trippable DOCX. |
| `service/StudentNumberImportService.java` | One `classify()` pass shared by preview and apply, so the two cannot diverge. Guard rails follow `DocumentService` (extension whitelist, 5MB cap, empty/invalid-name rejection). |
| `dto/registrar/StudentNumberImportOutcome.java` | Ten outcomes; `applicable()` is the single definition of "this row writes", used by both the preview counts and the apply loop. |
| `dto/registrar/StudentNumberImportRowResult.java`, `StudentNumberImportReport.java` | Per-row detail and the counts/summary. |
| `controller/StudentNumberController.java` | `GET /export`, `POST /import/preview`, `POST /import/apply` under `/api/registrar/student-numbers`. Apply logs one row per assignment plus a summary; preview logs nothing. |
| `static/student-numbers.html` + `static/js/registrar-student-numbers.js` | The report page: filters, "N of M students still need a student number", export, import preview→apply with outcome badges, and the existing Assign Number modal for singles. |
| `test/.../StudentNumberSheetParserTest.java` | 18 tests — the regression net for future mapping edits. |
| `test/.../StudentNumberExportServiceTest.java` | 8 tests incl. CSV/XLSX round-trip back through the parser and leading-zero survival. |
| `test/.../StudentNumberImportServiceTest.java` | 22 tests — every outcome, overwrite on/off, apply writes only applicable rows. |
| `test/.../StudentNumberControllerWebMvcTest.java` | 13 tests — export headers per format, multipart preview/apply, RBAC, logging on apply and **not** on preview. |

### Files Modified
| File | Change |
|------|--------|
| `dto/registrar/AssignStudentNumberRequest.java` | Validation hoisted to `MAX_LENGTH` / `PATTERN` / `ALLOWED_CHARS_MESSAGE` constants used by both its own annotations and the bulk import, so a value the Assign action accepts is exactly one the import accepts. |
| `config/SecurityConfig.java` | `/student-numbers.html` added to the REGISTRAR matcher. (`/api/registrar/**` was already REGISTRAR-only, so the new endpoints needed no change.) |
| `static/registrar.html`, `subjects.html`, `classes.html`, `sections.html`, `documents.html`, `student-records.html`, `generate-document.html` | Registrar navbar 5 → 6 links (Student Numbers). |

### Key behaviours
- **Preview writes nothing** — verified against the live DB and `system_logs`.
- **Apply** writes only applicable rows; the rest are reported. With the preview in front, that
  beats failing 200 good rows over one typo.
- **Never silently overwrite** — an existing *different* number is `CONFLICT_EXISTING` unless
  "Allow overwriting existing numbers" is ticked.
- **A number repeated within one file blocks both rows** — we cannot know which was intended.
- **Name columns warn, never match** — a `NAME_MISMATCH` still applies but is surfaced, which
  catches rows slipping out of alignment in a hand-edited sheet.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 299 tests, 0 failures, 0 errors** (was 238).
- Live round trip against real MySQL: exported the 6 unnumbered students (the already-numbered
  one correctly excluded), encoded a sheet containing every failure mode at once
  (duplicate-in-file pair, number already in use, unknown reference, blank, invalid characters),
  previewed → nothing written, applied → exactly the 2 valid rows written.
- **Excel fidelity via a POI-authored file:** `0012` (leading zero), `2025-777` and `A/2026/03`
  all survived export → edit → import. Header auto-detection found the header under two pasted
  title rows.
- `system_logs`: per-assignment rows + `"Imported student numbers from encoded.csv: 2 assigned,
  5 skipped"` + the export row. No rows from a preview.
- Playwright headless-Edge E2E **22/22**, run twice.
- Live DB restored to its pre-session state; the pre-existing `231472` on record 5 preserved
  throughout. Backup: `src/main/sql/backup-2026-08-30-pre-import-test.sql`.
- **No schema change** — `student_number` already existed.

---

## 2026-08-27 - Registrar-Controlled Student Number (no auto-generation)
**Branch:** `fix/student-ID-number`

### Task
Per the stakeholder meeting: the system must stop auto-generating student numbers. Add a
student number that is nullable, is never invented by the system, can be assigned later by
the archive import, and whose absence is visible to the Registrar.

### Design decision (see decisions.md)
`student_records.student_id` is `NOT NULL UNIQUE` and the **FK target of 10 child tables**;
`record_id` is the PK. Making `student_id` nullable would orphan child rows written before a
number exists. So `student_id` is kept untouched as an internal **Reference No.**, and a new
nullable `student_number` column carries the registrar-controlled value. The meeting note's
"should remain the primary key" is not literally satisfiable (a nullable column cannot be a
SQL PK, and it was never the PK) — a UNIQUE index provides "unique when present".

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/migrations/2026-08-27-add-student-number.sql` | Adds `student_number VARCHAR(20) NULL` + `uq_student_number`. Idempotent; guards match on `COLUMN_NAME`/`NON_UNIQUE`, never a constraint name (the name-based guard caused the 2026-05-19 duplicate-FK bug). Ends with read-only verification queries. |
| `dto/registrar/AssignStudentNumberRequest.java` | Deliberately NOT `@NotBlank` — blank/null means "clear". `@Size(max=20)` + `@Pattern` allowing letters, digits, `-`, `/` (the shapes real archive numbers take). |
| `test/.../RegistrarStudentNumberServiceTest.java` | 11 tests: assign, trim, overwrite, same-number-same-record, blank/null clear, duplicate rejection (target untouched, no save), unknown record, **edit-form update preserves the number**, filter partitioning, search-by-number. |
| `test/.../RegistrarStudentNumberControllerWebMvcTest.java` | 9 tests: assign 200 + log, clear 200 + log, null accepted, duplicate 400, bad chars 400 (field error), too long 400, 404, 403 trainer, 401 anonymous. |

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/schema.sql` | `student_number` column + `uq_student_number` on `student_records`; header dated; comment explains the two-identifier split. Seed INSERT uses an explicit column list, so sample students correctly start with NULL. |
| `model/StudentRecord.java` | `studentNumber` field + accessors, with Javadoc distinguishing it from the internal `studentId`. |
| `repository/StudentRecordRepository.java` | `findByStudentNumber` for the uniqueness pre-check. |
| `dto/registrar/StudentRecordSummaryResponse.java`, `StudentRecordDetailsResponse.java` | `studentNumber` added and mapped. |
| `service/RegistrarService.java` | `assignStudentNumber()` (trim via existing `emptyToNull`, blank→clear, uniqueness pre-check throwing an actionable `IllegalArgumentException` → 400 rather than a generic 409 from the index); 5-arg `getAllRecords` with `hasStudentNumber` (4-arg delegates, mirroring the `status` filter); `matchesQuery` now also matches the number. |
| `controller/RegistrarController.java` | `PUT /{recordId}/student-number` (logs "Assigned student number X to: …" / "Cleared student number for: …"); `list()` accepts `hasStudentNumber`. |
| `static/registrar.html` | "Student ID" → "Reference No."; new "Student Number" column + detail card; Student No. filter select; `#assignStudentNumberModal`; page-scoped `.record-actions .btn` compact sizing and a `flex-wrap` override for the filter bar; JS `?v=4`. |
| `static/js/registrar-students.js` | `renderStudentNumber` (warning badge when absent — a missing number is an action item, not merely absent data); Assign Number button in a `record-actions` group; details handler narrowed to `.js-open-details`; `hasStudentNumber` in `buildAjaxUrl` + Reset; `openAssignNumberModal` / `setupAssignStudentNumber` (PUT, inline errors, Enter-to-save, table reload). |
| `static/student-records.html` + `js/registrar-student-records-edit.js` | Identifiers row now 4 columns: Record ID, Reference No., **read-only** Student Number (with a pointer to the Assign action), Status. Populated but never sent in `buildPayload` — the edit form must not be a second write path. JS `?v=4`. |
| `static/student-details.html` | Submitted banner relabelled "Reference No." with a line telling the student the Registrar assigns their student number. |
| `test/.../RegistrarBulkLoadWebMvcTest.java` | Stubs updated to the 5-arg `getAllRecords`; fixture gives every third student a null number; new test asserting `?hasStudentNumber=false` is forwarded. |

**Deliberately unchanged:** `StudentDetailsService.generateStudentId()` and the
"Student ID cannot be changed." guard. The internal reference is still generated and still
immutable; only the new column is registrar-owned.

### Two frontend bugs found by the browser E2E
1. **86px of horizontal table scroll at 1280px.** The table itself fit (1045px in 1069px) —
   the culprit was `dashboard.css:683` pinning
   `#studentRecordsTable_wrapper #batchFilterBar .logs-filter-section` to `flex-wrap: nowrap`;
   the added dropdown pushed that bar to 1155px. Fixed with a page-scoped `flex-wrap: wrap`
   below 1400px. Now 0px overflow at 1280/1440/1920.
2. The existing details handler bound to `button[data-record-id]` — which the new Assign
   button also matched. Narrowed to `button.js-open-details`.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 238 tests, 0 failures, 0 errors** (was 217).
- Migration applied to live MySQL, then re-run: `SHOW CREATE TABLE student_records`
  byte-identical, exactly one unique index on the column. Two NULL rows coexist; a duplicate
  real value is rejected with `ERROR 1062`.
- `ddl-auto=validate` boot against live MySQL → **PASS** (started in 8.86s, all 19 entities).
- Playwright headless-Edge E2E **18/18**, run twice.
- Live API smoke: assign, duplicate → 400 with the naming message, invalid chars → field-level
  400, clear, 404, both filters, combined filter, search-by-number. `system_logs` rows present;
  a rejected duplicate writes no log row.
- Live DB restored to its pre-session state (all 7 students NULL). Backup:
  `src/main/sql/backup-2026-08-27-pre-student-number.sql`.

---

## 2026-07-14 PM #2 - Document Management Polish: Print, Logo, Filenames, DOCX, Delete/Edit
**Branch:** `fix/generate-document-student-picker`

### Task
Six approved polish items on the Document Management module: (1) print formatting —
no grey backdrop/chrome, 1-page templates print as exactly 1 page; (2) school logo in
the generated document header; (3) auto PDF/download filenames
"{ShortType}-{LastName} {FirstName}"; (4) documents.html Actions column fit; (5)
generated HTML documents download as editable Word .docx; (6) DELETE endpoint +
type-to-confirm modal, view-modal Edit that re-opens a saved document for re-editing,
generate-page Cancel + post-save redirect.

### Files Created
| File | Purpose |
|------|---------|
| `service/HtmlDocxConverter.java` | Wraps stored self-contained HTML into a minimal OOXML package whose document.xml references the HTML as an altChunk — Word converts it to editable content on open. Pure `java.util.zip`, no new dependency, air-gap safe. A4 sectPr matches the print CSS margins. |
| `static/js/anihan-logo.js` | `window.AnihanLogo.DATA_URI` — base64 data URI of images/logo.png (17KB) so generated/saved documents stay fully self-contained. |
| `test/.../HtmlDocxConverterTest.java` | 4 tests: OOXML parts present, original bytes preserved as the chunk part, altChunk references wired, empty-content rejection. |

### Files Modified
| File | Change |
|------|--------|
| `service/DocumentService.java` | New `delete(id)` (fetch → summary → delete, 404 via `NoSuchElementException`); new `prepareDownload(id)` + `DownloadPayload` record — `text/html` documents convert to docx named "{ShortType}-{Last} {First}.docx" (`TYPE_SHORT_NAMES` mirrors curriculum-templates.js; blank-name fallback to studentId), uploads pass through unchanged; `saveGenerated` gained a 5-arg overload with `documentId` for update-in-place, guarded by student ownership **and** text/html fileType (an API call must not overwrite an uploaded PSA scan — code-review finding). |
| `controller/DocumentController.java` | `DELETE /{documentId}` → 204 + "Deleted document…" system_logs row; download uses `prepareDownload` and logs the delivered filename; `generate` passes `documentId` and logs "Updated generated document…" on edits; `fileResponse` refactored to (name, type, bytes, inline). |
| `dto/registrar/GenerateDocumentRequest.java` | Optional `Integer documentId` (null = create, present = update in place). |
| `css/document-print.css` | Header now flex with `.doc-school-logo` (62px). Print rules rewritten: white body, chrome hidden, no sheet shadow, `body { display:block; min-height:0 }` (**root cause of the page-2 spill: Chromium cannot fragment flex items**), tighter margins/fonts, `page-break-inside: avoid` on table rows/signatories/certification/footer, `@page 9mm 11mm`. |
| `static/js/registrar-generate-document.js` | Logo in `SCHOOL_HEADER` (guarded `LOGO_URI` — degrades logo-less if the script fails); `printDocument()` swaps `document.title` to the sanitized filename around `window.print()` (afterprint + 2s fallback restore); edit mode (`?documentId=`) fetches the saved HTML, extracts `.document-sheet`, re-arms contenteditable, locks setup controls; save passes `documentId`, redirects to documents.html on success; Cancel button handler. |
| `static/js/registrar-documents.js` | Actions render: flex-nowrap group with View/Download/Delete (btn-sm, data-student/-type attrs); view modal shows Edit only for `text/html` docs, linking to the generate page edit mode; `setupDelete()` — type-"delete"-to-confirm modal flow, DELETE call, table refresh. |
| `static/documents.html` | Page-scoped `.document-actions .btn` size fix (dashboard.css `.btn-surface` padding overrides Bootstrap `.btn-sm`); `#viewDocumentEditBtn`; `#deleteDocumentConfirmModal` (registrar.html pattern); JS `?v=2`. |
| `static/generate-document.html` | `anihan-logo.js` include; Cancel button beside Save; JS `?v=3`. |
| `test/.../DocumentServiceTest.java` | +7 tests: delete (success/missing), prepareDownload (docx conversion + friendly name, uploads unchanged), update-in-place (success, wrong student, uploaded-file rejection). |
| `test/.../DocumentControllerWebMvcTest.java` | +6 tests: DELETE 204+log / 404 / 403 trainer / 401 anon; docx download headers; generate-with-documentId logs "Updated generated document". |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md` | Session notes. |

### Print Verification Result (real print-to-PDF, headless Edge `page.pdf()`)
All four Form IX variants print as **exactly 1 page**; the TOR prints as **2 dense
pages** — its 57 fixed subject rows measure ~1718px against ~1054px of usable A4, so
one page is physically impossible; breaks now fall cleanly between table rows with no
near-empty trailing page. No grey backdrop, no app chrome, logo present.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 217 tests, 0 failures, 0 errors** (was 200).
- Playwright headless-Edge E2E **30/30**: print-media assertions, PDFs (page counts +
  visual check of the rendered PDF), title swap/restore ("TOR-Ferreras Mark"), actions
  contained at 1400px and 992px, docx download (zip magic + `word/afchunk.html` +
  Content-Disposition), edit → re-save in place (marker persisted, no duplicate row),
  cancel + post-save redirects, type-to-confirm delete removes the row.
- `system_logs` (live MySQL): Generated / Downloaded ('TOR-Ferreras Mark.docx') /
  Updated generated / Deleted rows present. E2E's own test document deleted itself.
- /code-review on the diff: 3 findings (uploaded-file overwrite guard, blank-name docx
  filename, hard `window.AnihanLogo` dereference) — all fixed with tests.
- E2E harness note: `emulateMedia({media:'screen'})` is sticky and overrides
  `page.pdf()`'s default print media — reset with `media: null`.

---

## 2026-07-14 PM - Generate-Document Student Picker + Load-Failure Diagnosis + Record Cleanup
**Branch:** `fix/generate-document-student-picker`

### Task
(1) Replace the native `<datalist>` student list on the Generate Document page with a
dropdown that can also be typed into to search; (2) explain the "Failed to load student
data" error and the requirements for generating a document; (3) clean up duplicate and
incomplete student records in the live database.

### Files Modified
| File | Change |
|------|--------|
| `static/generate-document.html` | `<input list>` + `<datalist id="studentsDatalist">` replaced with `#studentPicker` combobox (input `role="combobox"` + Bootstrap `.dropdown-menu` `#studentPickerMenu`, 300px scrollable). JS cache-buster `?v=1` → `?v=2`. Follow-up fix: `.surface-card` has `overflow: hidden` in dashboard.css, which clipped the open picker menu at the card edge - added a page-scoped `overflow: visible` override plus `min-width: max-content` on the menu so long names are not truncated (verified by browser hit-test past the card boundary). Second follow-up (user feedback: full list too tall): menu capped at exactly 5 visible rows (items fixed at 2.5rem, menu max-height 13.5rem = 5 rows + padding) with overflow-y scroll for the rest; flex layout + 0.3rem gap keeps the "ID - Name" spacing. Browser-verified: 5/10 items visible, last item reachable by scroll. |
| `static/js/registrar-generate-document.js` | `loadStudentsDatalist()` → `setupStudentPicker()` (open on focus, live filter on ID/last/first name, ArrowUp/Down + Enter + Escape keyboard nav, mouse select, outside-click close, "Loading students…" placeholder + re-render when the student list arrives). New `resolveStudentId()` (exact-ID match, else unique search match, else friendly alert). New `ajaxErrorMessage()` used by load + save error paths: prefers server JSON message, else distinguishes network (status 0), 401 session-expired, 404 stale-build, and other HTTP statuses. |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md` | Session notes. |

### Diagnosis - "Failed to load student data"
The endpoint works: `GET /api/registrar/documents/generate-data/SR20260016` → HTTP 200
with the full auto-fill payload (student, parents, education, TESDA, OJT, grades). The
only hard requirement is that the student ID exists in `student_records`; missing
course/section/grades just render as editable blanks. The generic alert appears only when
the error response carries no JSON `message` — a 404 from a server still running a build
that predates the document-management merge (PR #49). Fix for operators: restart the app
after pulling. The hardened error handler now names that condition explicitly.

### Database Changes (live `AnihanSRMS`, backup taken first)
Deleted 4 student records via `DELETE /api/registrar/student-records/{recordId}`
(cascade-safe service path; each write logged to `system_logs`):
| Record | Reason |
|--------|--------|
| SR20260009 Wong, Angelica | Duplicate of SR20260008 (same name + middle initial); all-NULL `Enrolling` stub |
| SR20260010 Avellaneda, Keith | Abandoned `Enrolling` stub — no birthdate/sex/contact/batch/course/section, zero child rows |
| SR20260011 Mark, Mark | Same |
| SR20260017 test125, test125 | Same |

Left in place pending user decision: SR20260005 (dwd, wdw), SR20260007 (dwadwa, dwadad —
has grade data), SR20260013 (fff, fff) — fake-name test fixtures but `Active` with sections.

### Verification
- Headless Edge E2E (playwright-core driving system Edge): 10/10 checks pass — login,
  picker opens on focus (10 students), filter "lipata" → 1 match, keyboard select fills
  SR20260016, TOR renders with auto-filled data, mouse select works, unknown text shows
  friendly error. The E2E run caught and led to fixing a focus-before-load race.
- `./gradlew test` → **200 tests, 0 failures, 0 errors** (frontend-only change).

---

## 2026-07-14 - Live DB vs schema.sql Comparison and Sync
**Branch:** `main` (user explicitly kept work on main - DB-only task)

### Task
Compare the live `AnihanSRMS` MySQL database against `src/main/sql/schema.sql`, find any
discrepancies, and update the live DB so it works with the latest project schema.

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/backup-2026-07-14.sql` | New full `mysqldump` backup taken before any inspection (untracked). |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md` | Session notes. |

No application code, entity, or SQL source file was changed. **No change was applied to the
live database** - it already matched `schema.sql`.

### Comparison Result - no functional drift
Built a throwaway `schema_check` DB from `schema.sql`, dumped `--no-data` structures of both,
normalized (stripped `AUTO_INCREMENT=` + comments), and diffed. Only cosmetic differences,
all verified non-functional:
- **`grades` column order** differs in the raw dump; sorted column-by-column the two are
  byte-identical (name/type/nullability/default all match). Order is irrelevant to SQL/Hibernate.
- **`grades` class FK name:** live `fk_grades_class` vs schema.sql auto-name `grades_ibfk_3` -
  same `class_id -> classes ON DELETE SET NULL` relationship, name only.
- **`users` unique key name:** live `uq_username` vs schema.sql `username` - same UNIQUE, name only.
- **`courses`** already holds `CARS`, `BPRO`, `FSERV` (the 2026-07-09 seed gap remains closed).

### Compatibility Verification
- **`ddl-auto=validate` boot against live MySQL -> PASS** (`Started SpringbootApplication in
  11.256s`, all 19 entities validated, zero `HHH` schema-validation errors). Authoritative check;
  the Gradle test suite runs on in-memory H2 and cannot detect live-DB drift.
- FK integrity sweep -> 0 orphaned rows across `grades`, `class_enrollments`, `subjects`,
  `classes`.
- Live schema unchanged (19 tables). Throwaway `schema_check` DB dropped afterward.

### Note - schema.sql seed blocks intentionally not re-applied
Live is the real working DB (14 students, 6 classes, 264 `system_logs` rows). `schema.sql`'s
fresh-install seeds (3 test accounts, 5 sample students) were deliberately skipped - re-running
them would duplicate/corrupt live data (the load also errored on a duplicate `admin` username,
confirming the guard).


## 2026-07-09 - Document Management (R3.1–R3.7) + TOR/Form IX Generation
**Branch:** `feature/document-management`

### Task
Implement the registrar Document Management module — Jira AGILE-75…AGILE-81 (R3.1 Upload,
R3.2 Type, R3.3 Name, R3.4 View, R3.5 Search, R3.6 Filter, R3.7 Download) — plus
auto-filled, editable, print-ready generation of the four official templates in
`document-templates/` (TOR; Form IX for BPP / Cookery / FBS, each with Records-of-
Candidate-for-Graduation and Student's-Permanent-Record variants). Confirmed decisions:
print-ready HTML output, auto-fill + editable review, generated documents persisted to
the `documents` BLOB table with `system_logs` entries. No DB migration required.

### Files Created
| File | Purpose |
|------|---------|
| `repository/DocumentRepository.java` | `searchSummaries` JPQL constructor-expression projection (never selects `content_data`); explicit LEFT JOINs so null batch/section students survive the optional filters |
| `dto/registrar/DocumentSummaryResponse.java` | BLOB-free listing row |
| `dto/registrar/DocumentGenerateDataResponse.java` | Aggregated auto-fill payload (student + parents + education + TESDA + OJT + grades) |
| `dto/registrar/GenerateDocumentRequest.java` | `@NotBlank` studentId/documentType/fileName/html |
| `service/DocumentService.java` | Upload (extension+MIME whitelist pdf/docx/xlsx, 10MB cap, student + type validation), generated-HTML save (`text/html`, `.html` appended), type list, BLOB fetch for view/download |
| `service/DocumentGenerationService.java` | Builds the generate-data payload from 6 repositories |
| `controller/DocumentController.java` | `GET /api/registrar/documents` (q/type/batchCode/sectionCode), `GET /types`, `POST` multipart, `GET /{id}/download` + `GET /{id}/view`, `GET /generate-data/{studentId}`, `POST /generate`; upload/download/generate write `system_logs` |
| `static/documents.html` | Documents page — DataTable, upload modal (student datalist), iframe view modal, filter bar |
| `static/js/registrar-documents.js` | Table + debounced search, filters, FormData upload, PDF/HTML inline preview (docx/xlsx download-only) |
| `static/generate-document.html` | Template picker (TOR / Form IX × variant) + editable in-document form |
| `static/js/registrar-generate-document.js` | Renders the document as the fillable form (contenteditable spans), auto-fills from generate-data, Print (window.print), Save (self-contained HTML with inlined CSS → POST /generate) |
| `static/js/curriculum-templates.js` | Full curriculum transcription from the PDFs (~50 subjects with fixed hours/units, grading legend); grades merged by subject_code at render time |
| `static/css/document-print.css` | Document layout + `@page`/`@media print` rules; embedded into saved HTML |
| `test/.../DocumentServiceTest.java` | 10 Mockito tests (whitelist, size, unknown student/type, generated save, filter normalization) |
| `test/.../DocumentGenerationServiceTest.java` | 3 Mockito tests (aggregation, null OJT, missing student) |
| `test/.../DocumentControllerWebMvcTest.java` | 11 WebMvc tests (RBAC 401/403, multipart 201+log, 400 paths, download/view headers, generate) |
| `docs/superpowers/plans/2026-07-09-document-management-r3.md` | Approved implementation plan |

### Files Modified
| File | Change |
|------|--------|
| `config/SecurityConfig.java` | Registrar matcher += `/documents.html`, `/generate-document.html`; `X-Frame-Options` DENY → **SAMEORIGIN** (View modal iframe was blocked by the default — found via live smoke test) |
| `exception/GlobalExceptionHandler.java` | `MaxUploadSizeExceededException` → 400 with friendly message |
| `static/registrar.html`, `subjects.html`, `classes.html`, `sections.html`, `student-records.html` | Registrar navbar 4 → 5 links (Documents added) |

### Design Decisions
- **Curriculum is static template data, not `subjects` rows.** The printed documents carry
  ~50 fixed subjects with decimal units; `subjects.units` is INT and seeding would entangle
  class management. Deferred as a follow-up (units DECIMAL migration + seed).
- **The document is the form.** Instead of a separate form panel bound to a preview, every
  blank is a contenteditable span inside the print-faithful layout — what you edit is what
  prints and what gets saved.
- **Saved documents are self-contained HTML** so `GET /{id}/view` renders them in the iframe
  with zero extra dependencies and reprints keep their styling.
- **View is not logged; upload/download/generate are** — mirrors the trainer read-only
  no-logging precedent.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 200 tests, 0 failures, 0 errors** (was 176).
- Live smoke test (running app + real MySQL): login → types → upload → list → `?q=` search →
  type+batch filter → wrong-section filter (0 rows) → download (attachment, bytes intact) →
  view (inline, SAMEORIGIN) → generate-save (`.html` appended, `text/html`) → `system_logs`
  rows for upload/download/generate confirmed. Smoke-test document rows deleted afterwards.

---

## 2026-07-09 - Live DB vs SQL Files Comparison & Sync
**Branch:** `main` (user explicitly approved working on `main`)

### Task
Compare the live `AnihanSRMS` MySQL database against the most recent SQL files in
`src/main/sql/`, apply whatever the live DB was missing, then check for compatibility
issues and discrepancies between the database and the application code.

### Files Modified
| File | Change |
|------|--------|
| `memory-bank/activeContext.md` | New session entry: comparison findings, action taken, verification matrix |
| `memory-bank/progress.md` | Added this session under Recent Sessions |
| `memory-bank/changeLog.md` | This entry |

No application code, entity, or SQL source file was changed — the live database was the
only thing brought into sync.

### Database Changes Applied
| Statement | Purpose |
|-----------|---------|
| `src/main/sql/migrations/2026-05-10-seed-courses-and-batch.sql` | Seeded the two missing courses `BPRO` (Bread and Pastry Production) and `FSERV` (Food and Beverage Services). `CARS` and batch `B2026A` already existed and were left untouched by `INSERT IGNORE`. |

A full `mysqldump` backup was taken before applying anything.

### Comparison Result
- **Structure: no drift.** `mysqldump --no-data` of the live DB matches `schema.sql` exactly —
  19 tables, identical columns, types, nullability, indexes, and foreign keys. Every prior
  migration (`2026-05-05`, both `2026-05-09` files, `2026-05-19`, `2026-05-20`) was already
  reflected in the live schema.
- **Data: one gap**, the 2026-05-10 course seed, now applied.
- Structure re-dumped after the migration and diffed against the pre-migration dump: identical,
  confirming the change was data-only.

### Compatibility Verification
- **`ddl-auto=validate` boot against live MySQL → PASS.** Hibernate compared all 19 entities to
  the live tables and started cleanly in 9.0s. This is the authoritative compatibility check;
  the Gradle test suite runs on in-memory H2 (`ddl-auto=create-drop`) and therefore cannot
  catch live-DB drift.
- `./gradlew test` → 176 tests, 0 failures, 0 errors.
- Referential-integrity sweep → 0 orphaned rows across `grades`, `class_enrollments`,
  `sections`, and `subjects`.
- Domain-invariant sweep → every `classes.trainer_id` references a `ROLE_TRAINER` user;
  all grades sit inside the `[1.0, 5.0]` range that `TrainerGradeService` enforces;
  every `Active` student has a `section_code`.

### Discrepancy Noted → Fixed in the same session (see next entry)
`src/main/sql/migrations/2026-05-19-grades-restructure.sql` contained only `DESCRIBE`/`SHOW`
verification queries — its `ALTER TABLE` statements were commented out. It documented the
grades restructure rather than applying it.

---

## 2026-07-09 - Grades-Restructure Migration Made Functional + FK Idempotency Fix
**Branch:** `main`

### Task
Fix the flag raised above: make `2026-05-19-grades-restructure.sql` actually perform the
restructure instead of only verifying it, without breaking already-migrated databases.

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/migrations/2026-05-19-grades-restructure.sql` | Rewritten. Commented-out `ALTER`s replaced with real, guarded statements. Adds `class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at`; relaxes `final_grade`/`hours_studied`/`remarks` to NULL; adds `fk_grades_class` + `uq_grade_student_class`. Aborts before any change if the `classes` FK target is missing. Verification queries retained at the end. |
| `src/main/sql/migrations/2026-05-20-sync-and-clear-students.sql` | Fixed the same latent FK-guard bug in section A4 (`subjects.trainer_id`) and A5 (`grades.class_id`). |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md` | Session notes. |

No application code, entity, or `schema.sql` change. The live database ends the session
structurally identical to how it started.

### Why the ALTERs were commented out
The design spec (`docs/superpowers/specs/2026-05-19-trainer-grading-design.md`) wrote them as
`ALTER TABLE grades ADD COLUMN IF NOT EXISTS ...` — valid in MariaDB and Postgres, **not** in
MySQL 8. Rather than translate, they were disabled. The rewrite uses the guarded
`information_schema` + `PREPARE`/`EXECUTE` pattern already used by the 2026-05-20 migration,
which is the project's established idiom for idempotent DDL on MySQL 8.

### Second Bug Found While Testing (the important one)
The FK guards keyed off the **constraint name** `fk_grades_class`. But a database created from
`schema.sql` declares that FK inline and unnamed, so MySQL auto-names it `grades_ibfk_3`.
The name-only check therefore concluded "no FK present" and issued `ADD CONSTRAINT` — creating
a **duplicate foreign key on `grades.class_id` on every single re-run**. The migration was not
idempotent despite claiming to be.

Reproduced against the live database (a duplicate `fk_grades_class` really did appear; it was
dropped immediately). Fixed by matching on what the constraint *does* rather than what it is
called:

```sql
SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
  AND COLUMN_NAME = 'class_id' AND REFERENCED_TABLE_NAME = 'classes';
```

The identical name-based guard in `2026-05-20-sync-and-clear-students.sql` (for both
`grades.class_id` and `subjects.trainer_id`) had the same defect and was corrected the same way.

### Design Note — why not `SIGNAL` for the precondition
`SIGNAL` is not supported inside the prepared-statement protocol (`ERROR 1295`). The
precondition check instead selects from a deliberately non-existent table whose name carries
the operator instruction, so a missing `classes` table aborts the script on its first statement
with `ERROR 1146: Table '...ABORT_classes_missing_run_2026_05_09_migration_first' doesn't exist`.
MySQL identifiers cap at 64 characters, which is why the name is terse.

### Verification
Tested on all three paths a migration can meet:

| Scenario | Result |
|----------|--------|
| Legacy pre-restructure `grades` (no `class_id`, legacy `NOT NULL` cols) | Applies all 5 columns, relaxes 3 columns, creates FK + unique key |
| Re-run on the migrated legacy DB | Structure byte-identical — idempotent, no duplicate FK |
| Re-run twice on the live DB | Structure byte-identical to the pre-fix baseline; exactly 3 FKs; grade row intact |
| `classes` table absent | Aborts on statement 1; `grades` completely unmodified (no half-apply) |
| Hibernate `ddl-auto=validate` vs live MySQL | PASS |

Throwaway schemas `legacy_test` and `noclasses_test` were used for the first four and dropped
afterwards; only `AnihanSRMS` remains.

---

## 2026-05-21 - Bugfix Audit Remediation
**Branch:** `main`

### Task
Execute all 10 items from `docs/superpowers/plans/2026-05-21-bugfix-audit-remediation.md`.

### Files Modified
| File | Change |
|------|--------|
| `repository/StudentRecordRepository.java` | List return on name-lookup; `deleteClassEnrollmentsByStudentId` native query; `findMaxStudentIdWithPrefix` |
| `service/RegistrarService.java` | Delete cascades class enrollments; Student ID change rejected; middleName uses emptyToNull |
| `service/StudentDetailsService.java` | startOrResume list+filter+throws; load guards non-Enrolling; generateStudentId uses MAX+1 |
| `controller/StudentDetailsController.java` | start returns 409 on duplicate name |
| `controller/StudentPortalController.java` | checkDuplicate uses stream on List |
| `service/TrainerGradeService.java` | Grade range validation [1.0–5.0]; unconditional finalGrade |
| `service/StorageService.java` | studentId whitelist guard before file I/O |
| `service/AdminService.java` | Default email derives from username |
| `config/SecurityConfig.java` | @EnableMethodSecurity added |
| `model/StudentRecord.java` | middleName nullable=false removed |
| `model/StudentUpload.java` | kind 20→30, file_path 512→500, optional columns relaxed |
| `model/StudentEducation.java` | level/grade_year/semester/ended_year lengths corrected |
| `model/StudentSchoolYear.java` | sy_start/sem_start/sy_end/sem_end 10→20 |
| `model/StudentTesdaQualification.java` | result 25→50 |
| `model/StudentOjt.java` | hours_rendered precision 6→8 |
| `dto/registrar/StudentRecordUpdateRequest.java` | @NotBlank removed from middleName |
| `static/student-records.html` | editStudentId readonly; editMiddleName required removed |
| `CLAUDE.md` | CSRF statement corrected |
| `test/.../RegistrarBulkLoadTest.java` | 2 new tests |
| `test/.../StudentDetailsServiceTest.java` | 3 new tests + stub updates |
| `test/.../TrainerGradeServiceTest.java` | 2 new tests |
| `test/.../AdminServiceTest.java` | 1 new test + import |

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 176 tests, 0 failures, 0 errors**

---

## 2026-05-21 - Database Schema Sync & Grades Restructure
**Branch:** `main` (stayed on `main` per user's instruction)

### Task
Synchronize the live MySQL database (`AnihanSRMS`) running in Docker with the canonical `schema.sql` to resolve structural column mismatches, apply the `2026-05-19-grades-restructure.sql` schema verification, seed 5 dummy student records, and verify backend test suite integrity.

### Files Modified
| File | Change |
|------|--------|
| `memory-bank/activeContext.md` | Documented database sync items, verified grades table schema layout, and current branch state. |
| `memory-bank/progress.md` | Added database sync details under Recent Sessions. |
| `memory-bank/changeLog.md` | This entry. |

### Database Schema Updates
- **`AnihanSRMS` database dropped and recreated** to perform a clean, error-free reinstall of the schema.
- **Imported `schema.sql`**, resolving all structural column mismatches:
  - `batches.batch_year` changed from `smallint` to `year`.
  - `student_school_years.sy_start`, `.sem_start`, `.sy_end`, `.sem_end` expanded from `varchar(10)` to `varchar(20)`.
  - `student_tesda_qualifications.result` expanded from `varchar(25)` to `varchar(50)`.
  - `student_uploads.kind` expanded from `varchar(20)` to `varchar(30)`, and `.file_path` shortened from `varchar(512)` to `varchar(500)`.
  - `student_education.level` expanded from `varchar(10)` to `varchar(50)`, `.grade_year` shortened from `varchar(255)` to `varchar(50)`, `.semester` shortened from `varchar(255)` to `varchar(20)`, and `.ended_year` expanded from `varchar(7)` to `varchar(20)`.
  - `student_ojt.hours_rendered` expanded from `decimal(6,2)` to `decimal(8,2)`.
  - **`grades` restructured:** Incorporated `class_id`, `midterm_grade`, `finals_grade`, `locked`, `locked_at` columns, changed `hours_studied` to `decimal(5,2)` and `remarks` to nullable, and added the unique constraint `uq_grade_student_class` and foreign key constraint `fk_grades_class`.
- **Seeded 5 dummy student records** and lookup/user data successfully.

### Verification
- Ran the query checks in `2026-05-19-grades-restructure.sql` to confirm that all restructured grades table columns, indexes, and constraints are in place.
- `./gradlew test` → **BUILD SUCCESSFUL — 166 tests, 0 failures, 0 errors**.

---

## 2026-05-18 - Trainer Read-Only Views (AGILE-123 / AGILE-124)
**Branch:** `feature/trainer-view-subjects-classes`

### Task
Implement the trainer-facing read-only subject and class views. Trainers can see which subjects they are assigned to teach (with enrolled counts and section names), drill into the student roster per subject, view their class list, and drill into the roster per class. No DB schema changes — trainer assignment already exists on `classes.trainer_id`. No `system_logs` writes — all endpoints are read-only GETs.

### Files Created
| File | Purpose |
|------|---------|
| `dto/trainer/TrainerSubjectResponse.java` | Subject summary: code, name, qualification, units, enrolledCount, sectionNames[], courseNames[] |
| `dto/trainer/TrainerSubjectStudentResponse.java` | Per-subject student: studentId, lastName, firstName, middleName, sectionCode, sectionName |
| `dto/trainer/TrainerClassResponse.java` | Class summary: classId, sectionCode, sectionName, subjectCode, subjectName, courseName, semester, enrolledCount |
| `dto/trainer/TrainerClassStudentResponse.java` | Per-class student: studentId, lastName, firstName, middleName |
| `service/TrainerService.java` | Business logic — resolveCurrentTrainerId(), getMyAssignedSubjects(), getStudentsForSubject(), getMyClasses(), getStudentsForClass() |
| `controller/TrainerController.java` | 4 GET endpoints under `/api/trainer/` |
| `test/.../TrainerServiceTest.java` | 12 Mockito service tests |
| `test/.../TrainerControllerWebMvcTest.java` | 9 WebMvc controller tests |
| `static/trainer-subjects.html` | Subjects DataTable page with inline student roster panel |
| `static/js/trainer-subjects.js` | Subjects DataTable + click-to-load roster AJAX |
| `static/trainer-classes.html` | Classes DataTable page with inline student roster panel |
| `static/js/trainer-classes.js` | Classes DataTable + click-to-load roster AJAX |

### Files Modified
| File | Change |
|------|--------|
| `repository/SchoolClassRepository.java` | Added `findByTrainerUserId(Integer)` and `findByTrainerUserIdAndSubjectSubjectCode(Integer, String)` |
| `config/SecurityConfig.java` | Trainer HTML matcher extended: `/trainer-subjects.html` and `/trainer-classes.html` added |
| `static/trainer.html` | Upgraded to full dashboard pattern: `navbar-expand-lg` with 3-link nav (Home / My Subjects / My Classes), welcome hero section, quick-link cards, jQuery script import |

### Design Decisions
- **Trainer assignment is class-level only.** `Subject.trainer_id` is a registrar-side default and is not used to filter trainer views. The trainer sees only classes where `classes.trainer_id = currentUserId`.
- **`resolveCurrentTrainerId()` is package-private** to allow `@WithMockUser` stubbing in WebMvc tests without exposing it as a public API.
- **Subjects view groups by subjectCode** using `LinkedHashMap` to preserve insertion order, sums enrolled counts across all classes for that subject, and collects distinct section/course names.
- **Ownership guard in `getStudentsForClass()`** throws `IllegalArgumentException` (→ HTTP 400) rather than returning an empty list silently.
- **No `system_logs` writes** — all 4 endpoints are read-only views.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 156 tests, 0 failures, 0 errors**.

---

## 2026-05-15 - Section Student Management + Bulk Class Enrollment (AGILE-164 / AGILE-165)
**Branch:** `feature/section-class-enrollment`

### Task
Implement section-level student management (assign/remove students from sections) and bulk enrollment of a whole section into a class. No schema changes required — uses existing `student_records.section_code` FK and `student_status` column.

### Files Created
| File | Purpose |
|------|---------|
| `dto/registrar/UpdateSectionRequest.java` | `@NotBlank @Size(max=25) String sectionName` — PUT body for section rename |
| `dto/registrar/SectionStudentResponse.java` | Roster DTO: `studentId`, `lastName`, `firstName`, `middleName`, `studentStatus` |
| `dto/registrar/EligibleSectionStudentResponse.java` | Picker DTO: `studentId`, `lastName`, `firstName`, `batchCode`, `batchYear`, `courseCode`, `courseName` |
| `dto/registrar/AssignStudentsToSectionRequest.java` | `@NotEmpty List<@NotBlank String> studentIds` |
| `dto/registrar/SectionAssignmentResultResponse.java` | `int assignedCount`, `List<String> skippedStudentIds`, `List<String> reasons` |
| `dto/registrar/BulkEnrollSectionResponse.java` | `int enrolledCount`, `int skippedAlreadyEnrolled`, `int skippedIneligible`, `int totalConsidered` |
| `test/.../ClassManagementSectionServiceTest.java` | 13 Mockito service tests for all 6 new service methods |
| `test/.../ClassManagementSectionControllerWebMvcTest.java` | 7 WebMvc controller tests |

### Files Modified
| File | Change |
|------|--------|
| `repository/StudentRecordRepository.java` | Added 5 derived finders: `findBySectionSectionCode`, `findBySectionIsNullAndStudentStatusIgnoreCase`, and 3 variants adding batch/course filters |
| `repository/ClassEnrollmentRepository.java` | Added `deleteByStudentAndSectionCode` JPQL `@Modifying @Transactional @Query` |
| `service/ClassManagementService.java` | Added 6 methods: `updateSection`, `getStudentsInSection`, `getEligibleStudentsForSection`, `assignStudentsToSection`, `removeStudentFromSection`, `bulkEnrollSectionIntoClass` |
| `controller/ClassManagementController.java` | Added 6 endpoints: `PUT /sections/{code}`, `GET /sections/eligible-students`, `GET /sections/{code}/students`, `POST /sections/{code}/students`, `DELETE /sections/{code}/students/{studentId}`, `POST /classes/{classId}/enroll-section` — all write `system_logs` |
| `static/sections.html` | Added `#editSectionModal` and `#manageSectionModal` (tabbed: current roster + eligible picker with batch/course filters); cache-buster `?v=3` |
| `static/js/registrar-sections.js` | Full rewrite: 3-button Actions column; `setupEditSection()`, `setupManageStudents()`, `refreshCurrentStudents()`, `refreshEligibleStudents()`, `loadFilterDropdowns()` |
| `static/classes.html` | Added Bulk Enrollment block (`#enrollWholeSectionBtn` + `#enrollSectionAlert`) in `#enrollStudentModal`; cache-buster `?v=3` |
| `static/js/registrar-classes.js` | `setupEnrollment()` wires `#enrollWholeSectionBtn`; `openEnrollmentModal()` clears `#enrollSectionAlert` |

### Design Decisions
- `assignStudentsToSection` promotes `Submitted` → `Active`; skips students already in any section.
- `removeStudentFromSection` cascades via `deleteByStudentAndSectionCode`, reverts status to `Submitted`, clears section FK.
- `bulkEnrollSectionIntoClass` is idempotent: skips already-enrolled and non-Active students; returns counts for UI feedback.
- `GET /sections/eligible-students` registered before `GET /sections/{sectionCode}/students` to avoid path-variable capture.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 135 tests, 0 failures, 0 errors**.

## 2026-05-10 - Navbar Sync on student-records.html
**Branch:** `feature/edit-class-trainer`

### Task
Update the registrar's edit student record page navbar to expose the recently added registrar pages (`classes.html`, `sections.html`). The page had been stuck on the pre-May-9 2-link layout (Home + Subjects).

### Files Modified
| File | Change |
|------|--------|
| `src/main/resources/static/student-records.html` | Added two `<li class="nav-item">` entries linking to `classes.html` and `sections.html` after the existing `Subjects` link. Link order, classes, and styling mirror `registrar.html` exactly. |

### Design Decisions
- No `active` class on any link — the edit page is a subpage of Home (reached via "Open Details → Edit" on the home table), not itself a top-level nav target.
- Pure HTML change; no JS, CSS, or backend update needed.
- Re-used existing `nav-link admin-nav-link` classes — no new styles.

### Verification
- Visual diff against `registrar.html` confirms identical 4-link structure.
- No build needed; static asset change only.

### Open Items
- Manual browser check: open `/student-records.html?id={recordId}`, click each nav link, verify routing + Bootstrap collapse at mobile widths.

---

## 2026-05-10 - Edit Class Trainer (AGILE-93 / AGILE-95)
**Branch:** `feature/edit-class-trainer`

### Task
Allow a registrar to reassign (or unassign) the trainer on an existing class via a new `PUT /api/registrar/classes/{classId}/trainer` endpoint and an Edit Trainer modal on `classes.html`. Edit scope is trainer-only — section, subject, and semester remain immutable after class creation. No DB schema change needed (`classes.trainer_id` is already nullable with `ON DELETE SET NULL`).

### Files Created
| File | Purpose |
|------|---------|
| `src/main/java/com/example/springboot/dto/registrar/UpdateClassTrainerRequest.java` | Request DTO — single nullable `Integer trainerId` |
| `src/test/java/com/example/springboot/service/ClassManagementServiceTest.java` | 6 Mockito service tests for `updateClassTrainer` |
| `src/test/java/com/example/springboot/controller/ClassManagementControllerWebMvcTest.java` | 4 WebMvc controller tests for `PUT /classes/{id}/trainer` |

### Files Modified
| File | Change |
|------|--------|
| `src/main/java/com/example/springboot/service/ClassManagementService.java` | Added `UpdateClassTrainerRequest` import + `updateClassTrainer()` method (~25 LOC) |
| `src/main/java/com/example/springboot/controller/ClassManagementController.java` | Added `UpdateClassTrainerRequest` import + `PUT /classes/{classId}/trainer` endpoint with `system_logs` |
| `src/main/resources/static/classes.html` | Added `#editClassModal` (read-only context + trainer select + inline alert); cache-buster `?v=2` |
| `src/main/resources/static/js/registrar-classes.js` | Actions column now emits two buttons; `currentEditClassData` state + `editClassModal` init; `setupEditClass()` + `openEditClassModal()`; `loadTrainersDropdown()` returns jQuery deferred |

### Design Decisions
- Trainer-only edit scope per user decision — changing section/subject/semester would break the `(section_code, subject_code, semester)` unique key and leave enrollments in invalid states.
- `loadTrainersDropdown()` now returns the `$.ajax` deferred so `openEditClassModal()` can call `.done()` to pre-select the current trainer after the dropdown populates.
- Validation mirrors `assignTrainer` exactly: role must be `ROLE_TRAINER`, `enabled` must be `true`.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 115 tests, 0 failures, 0 errors**.

---

## 2026-05-10 - Subjects CRUD (Create / Edit / Delete)
**Branch:** `feature/subjects-crud`

### Task
Add Create, Edit, and Delete capabilities to the existing Subjects page (closes AGILE-89, AGILE-90, AGILE-91). Delete uses strict type-to-confirm modal. Existing Assign Trainer flow preserved unchanged. No schema changes — `subjects` table already supports all operations.

### Files Created
| File | Purpose |
|------|---------|
| `src/main/java/com/example/springboot/repository/QualificationRepository.java` | `JpaRepository<Qualification, Integer>` |
| `src/main/java/com/example/springboot/dto/registrar/QualificationResponse.java` | Response DTO for qualification dropdown |
| `src/main/java/com/example/springboot/dto/registrar/CreateSubjectRequest.java` | POST body with Bean Validation |
| `src/main/java/com/example/springboot/dto/registrar/UpdateSubjectRequest.java` | PUT body (code read-only) |
| `src/test/java/com/example/springboot/service/ClassManagementSubjectServiceTest.java` | 9 Mockito service tests |
| `src/test/java/com/example/springboot/controller/ClassManagementSubjectControllerWebMvcTest.java` | 6 WebMvc controller tests |

### Files Modified
| File | Change |
|------|--------|
| `src/main/java/com/example/springboot/repository/SchoolClassRepository.java` | Added `existsBySubjectSubjectCode(String)` FK pre-check |
| `src/main/java/com/example/springboot/repository/SubjectRepository.java` | Added `countGradesBySubjectCode` native query |
| `src/main/java/com/example/springboot/service/ClassManagementService.java` | Added `QualificationRepository` + 4 new methods: `getAllQualifications`, `createSubject`, `updateSubject`, `deleteSubject` |
| `src/main/java/com/example/springboot/controller/ClassManagementController.java` | Added `GET /qualifications`, `POST /subjects`, `PUT /subjects/{code}`, `DELETE /subjects/{code}` with system_logs |
| `src/main/resources/static/subjects.html` | Create button + 3 modals (Create, Edit, Delete strict-confirm); JS cache-buster `?v=2` |
| `src/main/resources/static/js/registrar-subjects.js` | Full rewrite — 3-button Actions column; CRUD setup functions added |

### Design Decisions
- Subject code is read-only after creation (PK cascades across `classes`/`grades`).
- Double FK pre-check on delete (classes then grades) yields actionable 400 messages instead of generic 409.
- Assign Trainer modal kept separate per user preference; Edit modal handles only name, qualification, units.
- `loadQualificationsDropdown()` returns a jQuery deferred so Edit can chain `.done()` to pre-select current qualification.

### Verification
- `./gradlew test` → **BUILD SUCCESSFUL — 105 tests, 0 failures, 0 errors**.

---

## 2026-05-09 - Student-Details Wizard Trim
**Branch:** `fix/student-details-trim`

### Task
Shorten the student enrollment wizard: (1) make the Baptismal Certificate upload optional, (2) reduce the Educational Background table from 6 to 4 columns, (3) remove the "School Years at Anihan" section. Pure frontend change — no backend, DTO, entity, or DB migration required.

### Files Modified
| File | Change |
|------|--------|
| `src/main/resources/static/js/student-details.js` | Dropped `certStatus`/`pendingBaptCert` check from `STEP_CUSTOM_VALIDATORS[2]` (Baptism Date + Place still required when Baptized is checked; cert is now optional). Removed `renderSyRow()` DOMContentLoaded call and `addSyRow` click listener. Dropped `gradeYear`/`semester` from `educationHistory` map in `buildPayload`. Replaced 12-line `#syTableBody` forEach with `const schoolYears = []`. Removed `edu-grade`/`edu-sem` reads from `populateForm`. Removed `schoolYears` population block from `populateForm`. Deleted `renderSyRow()` and `addSyRowData()` functions. |
| `src/main/resources/static/student-details.html` | Replaced 6-column `<thead>` with 4-column (`Grade/Year`/`Semester` removed; `Year Ended` → `School Year`). Removed `.edu-grade`/`.edu-sem` `<td>` cells from all 4 education rows. Deleted entire "School Years at Anihan" section (`#syTable`, `#syTableBody`, `#addSyRow`). Bumped JS cache-buster `?v=4` → `?v=5`. |

### Design Decisions
- Baptism cert file input stays in the UI — students can still upload voluntarily. Only the wizard-block is removed.
- `EducationItemDto.gradeYear`/`.semester` and `student_education` columns kept — new submissions write `NULL`. No migration needed.
- Registrar continues to own school-year history via `RegistrarService.saveSchoolYears`; newly enrolled students arrive with zero `student_school_years` rows.

### Verification
- `grep -nE "syTable|syTableBody|addSyRow|renderSyRow|addSyRowData|edu-grade|edu-sem"` on both files → empty.
- `./gradlew test` → **BUILD SUCCESSFUL — 90 tests, 0 failures, 0 errors**.

---

## 2026-05-09 - DB Sync + Error-Handler Hardening + Section FK Pre-Check
**Branch:** `fix/db-sync-and-bugs`

### Task
Audit-driven bugfix sweep. Five issues addressed: (1) live MySQL was missing the May 9 migration so the registrar Classes/Subjects/Sections pages threw 500s; (2) `student_records.middle_name` was still `NOT NULL` despite the JPA entity treating it as optional; (3) `GlobalExceptionHandler` leaked SQL/exception internals to API clients; (4) `ClassManagementService.deleteSection` had no pre-check for FK references; (5) `getCurrentSemester` loaded all batches into memory just to find the max year.

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/migrations/2026-05-09-relax-middle-name.sql` | Idempotent migration relaxing `student_records.middle_name` to `NULL`. Companion fix to the 2026-05-05 drift migration which missed this column. |

### Files Modified
| File | Change |
|------|--------|
| `src/main/sql/schema.sql` | `student_records.middle_name` declared `NULL` (was `NOT NULL`). Aligns fresh-install schema with the JPA entity and student-portal wizard which both treat middle name as optional. |
| `src/main/java/com/example/springboot/exception/GlobalExceptionHandler.java` | Added SLF4J logger. New `@ExceptionHandler(DataIntegrityViolationException)` returns HTTP 409 with a generic conflict message. Generic `Exception` handler now logs server-side via `log.error` and returns a sanitized `"An unexpected error occurred."` instead of echoing `ex.getClass().getSimpleName() + " - " + ex.getMessage()` (which previously leaked raw SQL, table names, and column names to clients). |
| `src/main/java/com/example/springboot/repository/SchoolClassRepository.java` | Added `boolean existsBySectionSectionCode(String sectionCode)` for the new pre-check. |
| `src/main/java/com/example/springboot/repository/BatchRepository.java` | Added `Optional<Batch> findTopByOrderByBatchYearDesc()` so `getCurrentSemester()` can resolve the latest batch year in a single SQL query instead of `findAll()` + in-memory max. |
| `src/main/java/com/example/springboot/service/ClassManagementService.java` | `getCurrentSemester()` now calls `batchRepository.findTopByOrderByBatchYearDesc()`. `deleteSection()` now calls `classRepository.existsBySectionSectionCode()` and throws `IllegalArgumentException("Cannot delete section: one or more classes still reference it. Remove those classes first.")` (mapped to HTTP 400 by `GlobalExceptionHandler`) instead of letting the FK violation bubble up as a generic 500. |

### Database Migrations Applied (live `AnihanSRMS` on this machine)
| Statement | Purpose |
|-----------|---------|
| `2026-05-09-classes-and-trainers.sql` | Re-applied — was previously not present on this machine despite memory-bank claim. Created `classes`, `class_enrollments`, added `subjects.trainer_id` FK, seeded 2 qualifications + 6 subjects. |
| `ALTER TABLE student_records MODIFY COLUMN middle_name VARCHAR(255) NULL` | Relaxed the column to allow students with no middle name to save without a SQL constraint violation. |

### Design Decisions
- **Generic 500 message instead of exception details.** The previous `"Internal server error: <ExceptionClass> - <message>"` body leaked SQL queries, table names, and JPA internals — confirmed during the audit by hitting `/api/registrar/classes` and seeing the full SELECT echoed back. The new behavior logs the full stack trace via SLF4J (so it appears in operator logs) but returns only a generic message to the client. This matches the project's on-premise deployment context where stack traces should never leave the server.
- **Pre-check, don't retry.** `deleteSection()` checks for class references *before* attempting the delete. The FK constraint stays as a defense in depth, but the pre-check converts a leaky 500 into a clean 400 with an actionable message.
- **`findTopByOrderByBatchYearDesc()` over a custom `@Query`.** Spring Data derived queries are preferred for simple cases; the runtime SQL is `SELECT ... FROM batches ORDER BY batch_year DESC LIMIT 1`, which is what we want. Avoids the maintenance cost of an explicit JPQL string.

### Verification
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → BUILD SUCCESSFUL — 90 tests, 0 failures, 0 errors
- `./gradlew bootRun` → started cleanly. Hit each previously-broken endpoint while authenticated as `registrar`:
  - `GET /api/registrar/subjects` → HTTP 200, returns 6 seeded subjects with qualifications + trainer fields
  - `GET /api/registrar/classes` → HTTP 200, `[]` (no classes yet — expected, no batches/courses/sections seeded; that's data the registrar enters via the UI)
  - `GET /api/registrar/classes/current-semester` → HTTP 200, `{"semester":"2026"}`
- Live MySQL `SHOW TABLES` → 19 tables (was 17). `subjects.trainer_id` present. 2 qualifications + 6 subjects rows present. `student_records.middle_name` `IS_NULLABLE = YES`.

### Out of Scope (deferred to follow-up tickets)
- N+1 in `getEligibleStudents()` and `getClasses()` — both still use `findAll()` + filter/count per row. Acceptable at the school's scale (~160 students); revisit if perf issues surface.
- Move `ClassEnrollmentResponse` and `StudentSummary` records out of `ClassManagementService` into `dto/registrar/`. Cosmetic; doesn't affect behavior.
- Unit/WebMvc tests for `ClassManagementService` and `ClassManagementController`. Existing audit caught the live-DB drift via integration probing rather than tests; tests would have caught a regression earlier and remain on the roadmap.

---

## 2026-05-09 - Registrar Subjects / Classes / Sections + Class Enrollment
**Branch:** `feature/class-assignment`

### Task
Implement three registrar-facing features: (1) per-subject default trainer assignment, (2) per-class scheduling with optional trainer + student enrollment, (3) section creation/listing scoped to the current semester. Includes a brand-new `classes` and `class_enrollments` table, seeded qualifications + subjects, and three new dashboard pages.

### Files Created
| File | Purpose |
|------|---------|
| `src/main/sql/migrations/2026-05-09-classes-and-trainers.sql` | Idempotent migration: adds `subjects.trainer_id` + FK, creates `classes` and `class_enrollments`, seeds 2 qualifications + 6 subjects |
| `model/SchoolClass.java` | JPA entity for `classes` (named `SchoolClass` to avoid clash with `java.lang.Class`) |
| `model/ClassEnrollment.java` | JPA entity for `class_enrollments` |
| `repository/SchoolClassRepository.java` | `findBySemester`, `existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester` |
| `repository/ClassEnrollmentRepository.java` | `findBySchoolClassClassId`, `existsBySchoolClassClassIdAndStudentStudentId`, `countBySchoolClassClassId` |
| `dto/registrar/SubjectResponse.java` | Subject DTO with flattened qualification name + trainer info |
| `dto/registrar/AssignTrainerRequest.java` | Single optional `trainerId` (null to unassign) |
| `dto/registrar/ClassResponse.java` | Class DTO including `enrolledCount` |
| `dto/registrar/CreateClassRequest.java` | Validated create-class payload |
| `dto/registrar/SectionResponse.java` | Section DTO with batch + course info |
| `dto/registrar/CreateSectionRequest.java` | Validated create-section payload |
| `dto/registrar/TrainerResponse.java` | Lightweight trainer-dropdown DTO |
| `dto/registrar/EnrollStudentRequest.java` | Validated enrollment payload |
| `service/ClassManagementService.java` | Subjects + Trainers + Classes + Class Enrollment + Sections business logic |
| `controller/ClassManagementController.java` | New controller under `/api/registrar/...` (separate from `RegistrarController`); every state-changing call writes a `system_logs` row |
| `static/classes.html` | Classes dashboard page with create + enrollment modals; 4-link registrar navbar |
| `static/sections.html` | Sections dashboard page with create + delete modals; 4-link registrar navbar |
| `static/js/registrar-subjects.js` | Subjects DataTable + Assign Trainer modal logic |
| `static/js/registrar-classes.js` | Classes DataTable + create-class + enrollment management logic |
| `static/js/registrar-sections.js` | Sections DataTable + create + delete logic |

### Files Modified
| File | Change |
|------|--------|
| `model/Subject.java` | Added `@ManyToOne User trainer` mapped to `trainer_id` (optional) |
| `repository/SectionRepository.java` | Added `List<Section> findByBatchBatchYear(Short batchYear)` |
| `repository/UserRepository.java` | Added `List<User> findByRoleAndEnabledTrue(String role)` |
| `config/SecurityConfig.java` | REGISTRAR HTML matcher extended with `/classes.html` and `/sections.html` |
| `static/registrar.html` | Navbar bumped from 2-link (Home / Subjects) to 4-link (Home / Subjects / Classes / Sections) |
| `static/subjects.html` | Replaced placeholder with full DataTable + Assign Trainer modal; navbar bumped to 4 links |
| `src/main/sql/schema.sql` | Added `subjects.trainer_id` column + FK, added `classes` + `class_enrollments` CREATE TABLE, added qualifications + subjects seed data; header bumped to 2026-05-09 with table count 17 → 19 |

### Database Migrations Applied
| Statement | Purpose |
|-----------|---------|
| `ALTER TABLE subjects ADD COLUMN trainer_id INT NULL` | Optional default trainer per subject |
| `ALTER TABLE subjects ADD CONSTRAINT fk_subjects_trainer FOREIGN KEY (trainer_id) REFERENCES users(user_id) ON DELETE SET NULL` | FK keeps subjects intact when a trainer account is deleted |
| `CREATE TABLE classes` | New table — section + subject + trainer + semester with `(section_code, subject_code, semester)` unique key |
| `CREATE TABLE class_enrollments` | New table — student-to-class link with `(class_id, student_id)` unique key, `ON DELETE CASCADE` from classes |
| `INSERT INTO qualifications` | Seeded `Cookery NC II` and `Bread and Pastry Production NC II` |
| `INSERT INTO subjects` | Seeded 6 subjects (4 cookery + 2 bread/pastry) |

### Design Decisions
- **Two trainer touchpoints (subject and class).** A trainer can be assigned as a *default* on the Subject (via Subjects page) and again on a specific Class (per-section, per-semester). The class-level trainer is the authoritative teacher; the subject-level trainer is a convenience pre-fill on the Create Class modal (auto-selected when the subject changes).
- **`SchoolClass` instead of `Class`.** `Class` would collide with `java.lang.Class`, so the JPA entity uses `SchoolClass` while the DB table is `classes`.
- **Separate controller `ClassManagementController` instead of bloating `RegistrarController`.** Keeps endpoints organized and limits the cross-cutting impact on existing tests.
- **Semester = batch year (string).** `getCurrentSemester()` reads `MAX(batch.batchYear)` and falls back to `Year.now()` if no batches exist. Filtering classes/sections uses this same value.
- **Eligible-student filter** restricts to `Active` or `Submitted` students in the same `section_code` as the class who aren't already enrolled. Avoids enrolling students from a different section by accident.
- **Delete a section** requires no linked classes — FK on `classes.section_code` blocks the delete and the frontend surfaces the DB error.

### Verification
- `./gradlew compileJava` → BUILD SUCCESSFUL
- `./gradlew test` → BUILD SUCCESSFUL (no regressions)
- `./gradlew bootRun` → Tomcat started on port 8080, Spring context loaded with no schema validation errors
- Live MySQL: `SHOW TABLES` reports `classes` + `class_enrollments`; `DESCRIBE subjects` shows new `trainer_id` column; `SELECT COUNT(*)` returns 2 qualifications + 6 subjects.

---

## 2026-05-07 - Emoji Cleanup Across Static Frontend
**Branch:** `feature/registrar-fix`

### Task
Remove visible emoji glyphs from user-facing pages and JS, keeping the third-party DataTables library (`datatables.min.js`, `datatables.min.css`) untouched.

### Files Modified
| File | Change |
|------|--------|
| `static/registrar.html` | Removed `<span class="dropdown-icon">✏️</span>` and `<span class="dropdown-icon">🚪</span>` from the Edit Account and Log Out dropdown items. |
| `static/trainer.html` | Same removals as registrar.html. |
| `static/index.html` | Dropped the `✅ ` prefix from the logout notification text. |
| `static/student-portal.html` | Dropped the `⚠️ ` prefix from the duplicate-record alert title. |
| `static/js/student-details.js` | Replaced 3 occurrences of `` `✓ ${...}` `` with `` `Uploaded: ${...}` ``; updated the baptismal-cert validator's `startsWith('✓')` check to `startsWith('Uploaded:')` so the logic still detects an already-uploaded file. |

### Notes
- Confirmed via `grep` over `static/css/` and `static/js/` that no CSS or JS code referenced the removed `dropdown-icon` span class — it was decorative only.
- The character `—` (em dash) used in headers/labels is **not** an emoji and was kept where present.
- Vendored DataTables library files (`datatables.min.js`, `datatables.min.css`) contain a few non-ASCII chars used internally and were intentionally left as-is.

### Verification
- `./gradlew build -x test` → BUILD SUCCESSFUL
- No backend, DTO, or DB changes.

---

## 2026-05-07 - Strict Type-to-Confirm Delete Modals (Registrar + Admin)
**Branch:** `feature/registrar-fix`

### Task
Replace the lightweight `window.confirm()` / `window.alert()` dialogs used for destructive deletes with stricter Bootstrap modals that require the user to type the literal word `delete` before the action is enabled. Apply on the registrar student-record delete and on the admin permanent (hard) account delete. Soft-delete (Deactivate Account) is unchanged because it is reversible.

### Files Modified
| File | Change |
|------|--------|
| `static/registrar.html` | New `#deleteRecordConfirmModal` (typing-confirm) above the footer. Reuses existing `delete-confirm-modal`, `warning-text`, `danger-zone`, and `btn-permanent-delete` styles in `dashboard.css` — no CSS changes. Modal echoes the student identifier in `#deleteRecordIdentifier` and shows inline result feedback in `#deleteRecordResultAlert`. |
| `static/js/registrar-students.js` | Added `deleteConfirmModal` and `currentRecordIdentifier` module state. `loadRecordDetails()` now stores a human-readable identifier (Student ID + last/first name). The old `deleteRecordBtn` inline handler that called `window.confirm()` was replaced with `setupDeleteRecordFlow(dataTable)`, which: (1) on click, hides the details modal, populates the identifier, clears the input, disables the confirm button, and shows the typing-confirm modal; (2) wires an `input` listener that enables the confirm button only when the input matches `delete` after `trim().toLowerCase()`; (3) on confirm, performs the existing `DELETE /api/registrar/student-records/{id}` call and surfaces success/error inline; (4) on `hidden.bs.modal`, resets input/button/alert state. All `window.confirm()` and `window.alert()` calls in the delete path are gone. |
| `static/admin.html` | Added a second modal `#permanentDeleteConfirmModal` next to the existing `#deleteConfirmModal`. The existing soft/hard chooser (`#deleteConfirmModal`) is kept as-is for the Deactivate flow; the new modal handles the Permanently Delete flow with typing confirmation. Echoes the username in `#permanentDeleteUserName` and shows inline result feedback in `#permanentDeleteResultAlert`. |
| `static/js/admin-users.js` | Added `currentDeleteUserName` and `permanentDeleteConfirmModal` module state. `loadUserDetails()` now also captures `user.username` into `currentDeleteUserName`. `confirmHardDeleteBtn` click handler simplified — it no longer performs the delete itself; it hides `deleteConfirmModal` and calls `openPermanentDeleteModal()` to open the typing-confirm modal. New `setupPermanentDeleteFlow(dataTable)` mirrors the registrar pattern: input gating, click handler that calls `deleteUser(currentDeleteUserId, true)` via the existing helper, success alert + `dataTable.ajax.reload(null, false)` after a short delay, and modal-state reset on `hidden.bs.modal`. The previous `window.confirm('This action is PERMANENT...')` has been removed. |

### Design Decisions
- **Soft delete (Deactivate) keeps its existing single-modal flow.** It is reversible (re-enable is supported), so a typing confirmation would be friction without a safety benefit.
- **Two-step admin flow preserved.** The new modal opens *from* the existing soft/hard chooser rather than replacing it, so admins still get the explicit Deactivate vs. Permanently Delete choice. The chooser closes before the typing-confirm modal opens to avoid Bootstrap stacking-modal issues.
- **Comparison is `value.trim().toLowerCase() === 'delete'`** so leading/trailing whitespace and capitalization (`DELETE`, `Delete`) are accepted. The placeholder shows the literal lowercase word `delete` so the contract is unambiguous.
- **Reused existing CSS classes** (`delete-confirm-modal`, `warning-text`, `danger-zone`, `btn-permanent-delete`, `btn-surface-secondary`) — no `dashboard.css` changes were needed.

### Verification
- `./gradlew build -x test` → BUILD SUCCESSFUL
- No backend, DTO, repository, or DB changes — purely a frontend UX guardrail in front of the existing DELETE endpoints (`/api/registrar/student-records/{id}` and `/api/admin/users/{id}/permanent`).

---

## 2026-05-07 - Bugs & Registrar Features: Parents/Guardian, Delete, Deferred Uploads, Not Available
**Branch:** `feature/registrar-fix`

### Task
6 items: Feature 2 (Not Available), Bug 3 (ID Photo not required), Bug 2 (Parents/Guardian in view+edit), Feature 1 (Delete record), Feature 3 (Auto-assign batch), Bug 1 (Defer file uploads).

### Files Modified
| File | Change |
|------|--------|
| `dto/registrar/StudentRecordDetailsResponse.java` | Added `father`, `mother`, `guardian` (ParentDto/GuardianDto) fields. New 7-arg `from()` factory; 4-arg and 1-arg factories delegate to it. |
| `dto/registrar/StudentRecordUpdateRequest.java` | Added `father`, `mother`, `guardian` optional fields (no validation constraints). |
| `service/RegistrarService.java` | Constructor expanded to 12-arg (added `ParentRepository`, `OtherGuardianRepository`, `StudentEducationRepository`, `StudentUploadRepository`, `StorageService`). `buildDetailsResponse()` loads parent/guardian from repos and passes to `from()`. `updateRecord()` calls `saveParents()` and `saveGuardian()` after `saveSchoolYears()`. New `deleteRecord()` deletes physical uploads, all child rows in FK order (uploads → parents → guardian → education → school years → TESDA → OJT → documents → grades), then parent row. New helpers: `saveParents()`, `upsertParent()`, `saveGuardian()`, `toParentDto()`, `toGuardianDto()`. |
| `controller/RegistrarController.java` | Added `DELETE /{recordId}` with `@ResponseStatus(NO_CONTENT)` and system log entry. |
| `repository/StudentUploadRepository.java` | Added `List<StudentUpload> findByStudentId(String)` and `void deleteByStudentId(String)`. |
| `repository/StudentRecordRepository.java` | Added `@Modifying @Query(nativeQuery=true)` `deleteDocumentsByStudentId()` and `deleteGradesByStudentId()` (no JPA repositories exist for `documents`/`grades` tables). |
| `repository/BatchRepository.java` | Added `Optional<Batch> findFirstByBatchYear(Short batchYear)`. |
| `service/StudentDetailsService.java` | Constructor +`BatchRepository batchRepo`. `submitEnrollment()`: if no batch set, auto-assigns via `batchRepo.findFirstByBatchYear(currentYear)`. |
| `test/service/StudentDetailsServiceTest.java` | Added `@Mock private BatchRepository batchRepo`. |
| `test/service/RegistrarBulkLoadTest.java` | Added 5 `@Mock` fields needed by expanded 12-arg constructor: `parentRepository`, `guardianRepository`, `educationRepository`, `uploadRepository`, `storageService`. |
| `static/registrar.html` | Added Father/Mother/Guardian detail-grid sections in modal body. Added Delete button (danger-surface, margin-right:auto) in modal footer. |
| `static/js/registrar-students.js` | Added `currentRecordId` state variable. `loadRecordDetails()` sets `currentRecordId` and populates all 24 parent/guardian sub-fields. Delete button handler: confirm → `DELETE /api/registrar/student-records/{id}` → hide modal → `dataTable.ajax.reload()`. |
| `static/student-records.html` | Added Father, Mother, Guardian form sections (each with full set of inputs) between Family/Religion and Enrollment sections. JS cache bumped `?v=2` → `?v=3`. |
| `static/js/registrar-student-records-edit.js` | `populateForm()` fills Father/Mother/Guardian fields. New `buildParent(prefix)` and `buildGuardian()` helpers. `buildPayload()` includes `father`, `mother`, `guardian`. |
| `static/js/student-details.js` | Added `pendingIdPhoto`/`pendingBaptCert` state. `setupFileInput()` rewritten: stores File in pending var, shows local FileReader preview, sets "Selected: filename" status (no network). `submitForm()` uploads pending files after JSON submit succeeds via `uploadPendingFile()`. Baptism cert validator: `!certStatus.startsWith('✓') && pendingBaptCert === null` (pending file satisfies requirement). |
| `static/student-details.html` | Removed `*` from ID Photo label (no longer required). |

### Design Decisions
- `deleteRecord()` uses explicit ordered deletes (not JPA cascade) because `documents` and `grades` have no JPA repositories and FK constraints must be cleared before the parent row can be removed.
- `findFirstByBatchYear` (not `findByBatchYear`) used for batch auto-assignment because `batch_year` has no UNIQUE constraint; `findFirst` safely handles multiple batches per year.
- Deferred upload approach (store File in JS var, upload after JSON submit) ensures a student record exists in the DB before the upload FK constraint fires, and avoids wasted uploads if the student abandons the form before submitting.

### Verification
- `./gradlew test` → BUILD SUCCESSFUL — all tests pass, no regressions

---

## 2026-05-06 - Registrar Enhancements: Status Filter + OJT/TESDA/SchoolYears on Edit Form
**Branch:** `feature/registrar-fixes`

### Task
Feature 1: Add a student status filter dropdown to the registrar home page (`registrar.html`).
Feature 2: Restore OJT, TESDA Qualifications, and School Years sections to the registrar edit form (`student-records.html`) so the registrar can manage the full training record alongside basic student details.

### Files Modified
| File | Change |
|------|--------|
| `dto/registrar/StudentRecordDetailsResponse.java` | Added `OjtDto ojt`, `List<TesdaQualDto> tesdaQualifications`, `List<SchoolYearDto> schoolYears` fields. New 4-arg `from(record, ojt, tesda, sy)` factory. Old 1-arg `from(record)` delegates to new factory with empty collections. |
| `dto/registrar/StudentRecordUpdateRequest.java` | Added same 3 optional fields with no validation constraints (empty allowed). |
| `service/RegistrarService.java` | Constructor extended with `StudentOjtRepository`, `StudentTesdaQualificationRepository`, `StudentSchoolYearRepository`. Added `getAllRecords(query, fromYear, toYear, status)` 4-arg overload; older overloads delegate to it. Added `matchesStatus()`. `updateRecord()` annotated `@Transactional`; after saving basic record, calls `saveOjt()`, `saveTesda()`, `saveSchoolYears()` helpers. Each helper deletes by old studentId (with explicit `flush()` to prevent unique-constraint race before inserts) then inserts new rows with new studentId. `getRecordById()` now calls `buildDetailsResponse()` which loads all 3 collections. |
| `controller/RegistrarController.java` | Added `@RequestParam(value = "status", required = false) String status` to `list()` and forwarded to service. |
| `test/service/RegistrarBulkLoadTest.java` | Added `@Mock StudentOjtRepository`, `@Mock StudentTesdaQualificationRepository`, `@Mock StudentSchoolYearRepository` so `@InjectMocks` can build the new 7-arg constructor. Added `statusFilterRestrictsResultsByStudentStatus` test (Active filter, case-insensitive, null/blank returns all, unknown returns empty). |
| `test/controller/RegistrarBulkLoadWebMvcTest.java` | Updated all 3 `when(registrarService.getAllRecords(...))` stubs from 3-arg to 4-arg signature (added trailing `isNull()` for status). |
| `static/registrar.html` | Added `<label>` + `<select id="studentStatusFilter">` with options All/Enrolling/Submitted/Active/Graduated inside the existing `#batchFilterBar`. |
| `static/js/registrar-students.js` | `buildAjaxUrl()` now reads `#studentStatusFilter` and appends `?status=` when non-empty. Reset handler clears the select alongside year inputs. |
| `static/student-records.html` | Added 3 new form sections before `</form>`: OJT (company name, address, hours), TESDA (3 fixed `<fieldset>` slots with title, center address, assessment date, result), School Years (dynamic `<table>` with `<tbody id="schoolYearsBody">` + "Add Row" button). Bumped JS cache to `?v=2`. |
| `static/js/registrar-student-records-edit.js` | Added `esc()`, `createSchoolYearRow()`, `setupSchoolYearHandlers()`. Extended `populateForm()` to fill OJT inputs, TESDA slots 1–3 (keyed by `slot` number), and render School Year rows. Extended `buildPayload()` with `buildOjt()`, `buildTesdaSlot()`, `buildSchoolYearRows()` helpers. Added form-level `input`/`change` event delegation in `setupDirtyTracking()` to cover dynamically-added rows. `setupSchoolYearHandlers()` called before `setupDirtyTracking()` in boot sequence. |

### Design Decisions
- **Delete-all-then-insert-new** for TESDA and SchoolYears: simpler than diffing; "edit form replaces state" matches the registrar's mental model. See `decisions.md`.
- **Explicit `flush()` after delete**: prevents Hibernate from buffering the DELETE past the INSERT within the same `@Transactional`, which would violate the `(student_id, slot)` unique constraint on TESDA.
- **OJT upsert** (find existing row, update or create): OJT is a 1:1 relationship per student; upsert avoids losing the `ojt_id` PK unnecessarily.
- **Form-level dirty delegation**: `form.addEventListener('input', markDirty)` is added alongside per-element listeners so dynamically-added School Year row inputs are covered without re-running `setupDirtyTracking`.

### Verification
- `./gradlew test` → BUILD SUCCESSFUL — all tests pass, no regressions
- New test: `statusFilterRestrictsResultsByStudentStatus` — passes

---

## 2026-05-05 - Student Portal Enrollment Flow Fix
**Branch:** `fix/student-portal-flow`

### Task
Fix 5 root-cause bugs in the student enrollment flow (student-portal → student-details wizard → submit). Scope: student-facing code only — no Registrar/Trainer changes.

### Root Causes Fixed
| RC | Severity | Description |
|----|----------|-------------|
| RC-1 | 🔴 Critical | `startOrResume()` created full student record on portal start. Now creates name+status only (minimal for upload FK). |
| RC-2 | 🔴 Critical | `saveDraft()` persisted to DB on every "Next" click. Removed — data stays in browser until final submit. |
| RC-3 | 🟡 Medium | `submitEnrollment()` replaces old two-step saveDraft/submit with a single `@Transactional` block that persists record + parents + guardian + education + school years atomically. |
| RC-4 | 🟡 Medium | OJT/TESDA removed from student-facing flow (HTML, JS, DTOs, service). Entities/repos kept for Registrar. |
| RC-5 | 🟢 Low | `AgeCalculator` returns `Integer null` instead of `int 0` for null birthdate. |

### Files Created
| File | Purpose |
|------|---------|
| `test/service/StudentDetailsServiceTest.java` | 7 Mockito unit tests: start (minimal record), resume, submit (all data), double-submit guard, load, invalid ID |

### Files Modified
| File | Change |
|------|--------|
| `service/StudentDetailsService.java` | Rewrote: removed `saveDraft()` and old `submit()`; added `submitEnrollment()` with full `@Transactional` persistence; removed OJT/TESDA handling |
| `controller/StudentDetailsController.java` | Removed PUT saveDraft endpoint; new `POST /{studentId}/submit` accepts full `StudentDetailsRequest` body |
| `dto/student/StudentDetailsRequest.java` | Added `lastName`, `firstName`, `middleName` fields; removed `ojt` and `tesdaQualifications` |
| `dto/student/StudentDetailsResponse.java` | Removed `ojt` and `tesdaQualifications` |
| `static/js/student-details.js` | Removed `saveDraft()` function and all calls; removed OJT/TESDA from `buildPayload()`/`populateForm()`; submit sends full payload as POST body |
| `static/student-details.html` | Removed OJT/TESDA HTML sections (20 lines); bumped JS cache `v=3` → `v=4` |
| `service/AgeCalculator.java` | Return type `int` → `Integer`; null birthdate → `null` instead of `0` |
| `test/service/AgeCalculatorTest.java` | Updated: `assertEquals(0)` → `assertNull`; added `assertNull` import |
| `memory-bank/activeContext.md` | Updated with enrollment flow fix session |
| `memory-bank/progress.md` | Added completed enrollment flow fix section |
| `memory-bank/changeLog.md` | This entry |

### Scope Boundary (NOT Changed)
- All Registrar/Trainer controllers, services, HTML, JS — untouched
- `StudentOjt.java`, `StudentTesdaQualification.java` entities — kept for Registrar use
- `StudentOjtRepository.java`, `StudentTesdaQualificationRepository.java` — kept
- `AdminService.java`, `RegistrarService.java`, `AccountService.java` — untouched

### Verification
- `./gradlew test` → BUILD SUCCESSFUL — all tests pass, no regressions
- New `StudentDetailsServiceTest`: 7/7 pass
- Updated `AgeCalculatorTest`: 6/6 pass
- No lint warnings

---

## 2026-05-05 - Schema Drift Remediation + DataSeeder Removal
**Branch:** `main` (uncommitted, per user instruction — no feature branch, no commit)

### Task
Diagnose live `AnihanSRMS` database against `schema.sql` / `AnihanSRMS.sql` and JPA entities. Apply migrations to fix the drift, delete the redundant `DataSeeder` so application data lives in the database only, and update SQL files for other developers.

### Files Created
| File | Purpose |
|---|---|
| `src/main/sql/migrations/2026-05-05-fix-schema-drift.sql` | One-shot migration to bring legacy databases in line with `schema.sql`. Adds `student_records.civil_status` (idempotent guard via `information_schema`), relaxes 27 columns from `NOT NULL` to `NULL` across `student_records`, `parents`, and `other_guardians`, drops `parents.est_income DEFAULT 0.00`. |

### Files Deleted
| File | Reason |
|---|---|
| `src/main/java/com/example/springboot/config/DataSeeder.java` | Application data must come from the database. The seeder duplicated `schema.sql` seed inserts and was the proximate cause of the `contextLoads()` test failure when the live DB drifted. |

### Files Modified
| File | Change |
|---|---|
| `src/main/sql/schema.sql` | Header updated to 2026-05-05; description corrected to acknowledge the 5 sample student records that were already being inserted; added cross-reference to the new migration file |
| `src/main/sql/AnihanSRMS.sql` | Header updated to 2026-05-05; added cross-reference to `schema.sql` (for fresh installs) and to the new migration file (for existing DBs predating the drift fix) |
| `memory-bank/activeContext.md` | New "Schema Drift Remediation + DataSeeder Removal" session entry; current branch refreshed to `main`; removed stale "Active Branch: registrar-retry" duplicate |
| `memory-bank/progress.md` | New completed section at the top |
| `memory-bank/decisions.md` | Two new decision records (live-DB ALTER over schema rewrite; DataSeeder removal) |

### Database Migrations Applied (Docker `mysql-server` container)
| Statement | Purpose |
|---|---|
| `ALTER TABLE student_records ADD COLUMN civil_status VARCHAR(50) NULL AFTER sex` | Restore missing column referenced by `StudentRecord.java` and the enrollment wizard |
| `ALTER TABLE student_records MODIFY COLUMN ... NULL` (×12) | birthdate, age, sex, permanent_address, email, contact_no, religion, baptism_place, sibling_count, batch_code, course_code, section_code |
| `ALTER TABLE parents MODIFY COLUMN ... NULL` (×9) | family_name, first_name, middle_name, birthdate, occupation, est_income, contact_no, email, address |
| `ALTER TABLE parents ALTER COLUMN est_income DROP DEFAULT` | Removed leftover `DEFAULT 0.00` |
| `ALTER TABLE other_guardians MODIFY COLUMN ... NULL` (×6) | relation, last_name, first_name, middle_name, birthdate, address |

### Why Drift Returned After the May 2 Fix
The May 2 session corrected the same drift on a different live DB. The DB on this machine on 2026-05-05 either predated that fix or was rebuilt from an older dump. Both `schema.sql` and `AnihanSRMS.sql` use `CREATE TABLE IF NOT EXISTS`, so re-running them against an existing legacy DB silently keeps the old column definitions. The new migration file is the durable answer for that scenario.

### Verification
- `SHOW CREATE TABLE student_records` → `civil_status` present, 12 fixed columns nullable
- `SHOW CREATE TABLE parents` / `other_guardians` → all expected columns nullable, `est_income` no default
- `./gradlew test` → BUILD SUCCESSFUL — **82 tests, 0 failures, 0 skipped** (was 81/82 with `contextLoads` failing)
- `Grep DataSeeder` → no remaining production references; only historical mentions in memory-bank files

---

## Historical Summary (pre-2026-05-05)

Older sessions condensed to one line each. For full file-change tables, rationale, and verification details, recover from `git log` on the branch named in each entry.

### May 2026

- **2026-05-04 — Student Status Dropdown + Badge Colors:** Status `<select>` on `student-records.html` (Enrolling/Active/Graduated only); `renderStatusBadge()` colors — Active=green, Enrolling/Submitted=grey, Graduated=blue.
- **2026-05-03 — Student Portal Mandatory Field Validation** (`feature/student-field-validation`): Civil Status, ID Photo, conditional baptism fields, Father/Mother core fields. Added `STEP_CUSTOM_VALIDATORS` pattern.
- **2026-05-02 — Database Sync Migration:** Dropped 4 legacy tables; `student_records` PK changed to `record_id`; nullability + type fixes; 17 canonical tables.
- **2026-05-02 — Search Bar Selector + Filter Width + DataSeeder:** Dual selector for DataTables 1/2 search input; batch-year filter input width; (interim) DataSeeder added — later removed 2026-05-05.
- **2026-05-02 — Registrar Search Bar + Batch Year Filter + Dummy Seed Data:** 320px search input, From/To year inputs, `?fromYear=&toYear=` server-side, 5 dummy student records in `schema.sql`.
- **2026-05-01 — Registrar Bulk Load Tests + H2 Isolation + Server-Side Search:** `?q=` query, 200-record perf test, `StudentRecordH2LoadTest` with isolated H2 (MODE=MySQL).
- **2026-05-01 — Registrar Edit Student Record + Search + Unsaved Notifications:** Full edit form with FK resolution, dirty-tracking + `beforeunload`.
- **2026-05-01 — Registrar Home: Student Records Table & Detail Modal:** `RegistrarController`, `RegistrarService`, summary + details DTOs (BLOB excluded), 9-col DataTable.
- **2026-05-01 — Live Age Recalculation in Edit Account Modal:** `auth-guard.js` recalculates `#ageDisplay` on birthdate change.
- **2026-05-01 — Registrar Navbar Standardization:** `subjects.html` rebranded from admin to registrar (REGISTRAR role, 2-link navbar, security matcher updated).

### April 2026

- **2026-04-30 — Enrollment Flow Bug Audit & Fixes** (`fix/db-sync-username-unique`): Bug 3 (Parent/OtherGuardian FK), Bug 6 (duplicate security matchers), Bug 7 (`saveDraft()` failure handling).
- **2026-04-30 — Database Sync from `AnihanSRMS.sql`:** Added missing UNIQUE index on `users.username`.
- **2026-04-30 — Bug 1 Fix: `student_records.age` NOT NULL** (`feature/student-details`): `ALTER TABLE ... MODIFY COLUMN age INT NULL`.
- **2026-04-30 — Bug 2 Fix: `StudentRecord` `@Id` mismatch:** Moved `@Id @GeneratedValue(IDENTITY)` to `recordId`; repo generic `String → Integer`; `findByStudentId()`.
- **2026-04-30 — Database Schema Audit & Migration:** 5 missing student tables created; `civil_status` added; 26 NOT NULL → NULL relaxations; UNIQUE index on username.
- **2026-04-29 — SQL Files Synced with Live Database:** `AnihanSRMS.sql` + `schema.sql` rewritten — 17 tables + 3 user accounts.
- **2026-04-29 — Submit Button Fix:** JS cache-busting (`?v=2`) + removed `throw e` in `saveDraft()` catch block.
- **2026-04-29 — Student Details Enrollment Wizard:** 5 new entities, 7 new repos, 9 DTOs, `StorageService`, `StudentDetailsService`+Controller, 4-step Bootstrap wizard, live DB migrated.
- **2026-04-27 — Admin Users Table Column Split** (`test-user-table`): Single Name column → Last Name + First Name.
- **2026-04-26 — Database Schema Sync & SQL Export Files:** Created 6 missing tables in live DB; rewrote `AnihanSRMS.sql` + `schema.sql`.
- **2026-04-19 — Age Auto-Calculation from Birthdate:** `AgeCalculator` utility; age removed from input DTOs/forms; silent recalc on individual view; `birthdate` `@NotNull` on create.
- **2026-04-18 — System Logs Export UI Cleanup** (`feature/export-logs`): Server-side CSV/XLSX/DOCX via `GET /api/logs/export`; Apache POI dependency added.
- **2026-04-18 — System Logs Date Filtering** (`feature/logs-date-filter`): Optional `rangeDays`/`startDate`/`endDate` params; default 7 days; preset pills UI.
- **2026-04-18 — Admin Bulk Load Tests:** 100-user perf tests at service (0.008s) and HTTP layer (0.663s).
- **2026-04-17 — Admin Navbar Cleanup:** Removed Student Records + Subjects from active admin navbar; pages preserved with stale internal navbars (TD-2).
- **2026-04-17 — Database Migration Fix & SQL Cleanup:** Re-added `enabled`, `password_changed_at`, `system_logs` after live DB rebuild; removed merge-conflict remnants from `AnihanSRMS.sql`.
- **2026-04-17 — Unit Test Coverage Expansion:** Added Account + SystemLog service/controller test suites.
- **2026-04-16 — Navbar Logo UI Standardization** (`ui-style/fix`): Removed `.brand-title`, logo enlarged to 85px with `-22px` margin compensation.
- **2026-04-14 — Account Icon Dropdown on All Admin Pages:** Brand-mark navbar + Edit Account modal added to `student-records.html`, `subjects.html`, `logs.html`.
- **2026-04-14 — Admin System Logs:** `system_logs` table + entity/repo/service/controller/DTO + `logs.html` + `system-logs.js`; integrated in Auth/Admin/Account controllers.
- **2026-04-11 — Admin Username Edit & Hover Fixes:** `.btn.btn-reenable:hover` Bootstrap-specificity fixes; editable username with no-spaces pattern.
- **2026-04-11 — Re-enable, Password Toggle, Strong Validation, `passwordChangedAt`:** New column; `PUT /api/admin/users/{id}/enable`; eye-icon toggle auto-injected; `@Pattern` strong-password regex (self-service only).
- **2026-04-11 — Admin Password Reset, Delete Account & Hover Fix:** Optional password in admin update DTO; `enabled` column + Spring Security flag; soft + hard delete endpoints; status badge column.
- **2026-04-11 — Admin Dashboard Front-End Repair** (`feature/fix-login-security`): Rebuilt malformed `admin.html` and `edit-user.html` after donor merge artifacts.
- **2026-04-11 — Build Repair for Admin Controller Regression:** Restored DTO/service-based `AdminController` (had drifted back to repository-only).
- **2026-04-11 — Conflict Cleanup and Commit-Safety Recheck:** Removed unresolved merge markers across HTML/SQL/config.
- **2026-04-11 — Root/Admin Merge from `main-em`:** `AdminController`, `AdminService`, sanitized DTOs, edit-user/student-records/subjects/logs HTML, admin DataTable JS, automated tests.
- **2026-04-10 — Admin Dashboard UI & Logic** (`feature/admin-dashboard-ui`): MySQL session config, login error UI, `AnihanSRMS.sql` reordered for FK creation order.
- **2026-04-07 — Database Schema Development:** Defined complete schema with 15 tables.
- **2026-04-06 — AGILE-100 G2.1 Edit Personal Details:** Tabbed Edit Account modal; `LookupController` (subjects/sections); trainer dropdowns; users table extended with `lastName/firstName/middleName/birthdate`.
- **2026-04-05 — AGILE-142 G1.R Fix Login:** Role-specific page matchers, dual-mode entry/denied handlers, `AccountController`/`AccountService`, account dropdown, `auth-guard.js`, unique username index, removed `DataSeeder`.

### March 2026

- **2026-03-24 — Backend Auth Setup** (`feature/backend-auth-setup`): `User` entity + repo, `AuthController` login/logout/me, dashboard HTML templates.
- **2026-03-24 — Troubleshooting IDE Syntax Errors** (`feature/fix-src-errors`): JDTLS Java 25 JDK reconnection; `build.gradle.kts` updated to Java 25.
- **2026-03-21 — Login Page Design:** Card-based centered form with gradient header/button.

