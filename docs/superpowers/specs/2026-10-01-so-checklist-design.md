# R4.2 SO Checklist — Design

**Date:** 2026-10-01 | **Status:** Approved design (brainstormed with the user decision by decision), ready for plan drafting. Not implemented.
**Story:** AGILE-87 "R4.2 SO Checklist" (epic AGILE-44) — *"Registrar wants to see if the student has completed the SO requirements so that they can start SO filing."*
**Intent:** Show the Registrar, per student, which TESDA Special Order (SO) requirements are met, met with a warning, unmet, or not yet due — computed live from data already in the system, plus two new fields and one new status.
**Stack:** Java 25, Spring Boot 4.0.4, Spring Security 7, Spring Data JPA + `JdbcTemplate`, MySQL 8, Bootstrap 5.3, local jQuery 4 / vanilla JS. No new libraries.
**Related:** `service/RequiredDocumentPolicy.java` (PR #68, merged as `f6c3afe`); `docs/superpowers/specs/2026-10-01-pre-export-missing-documents-design.md`; `docs/superpowers/specs/2026-09-27-document-folder-management-design.md` (batch/section ownership rules); `capstonepaper/Chapter-3.pdf` §3.1.1 (the TESDA SO requirement list).

---

## 1. Confirmed Decisions (user-approved)

1. **Source of truth is TESDA's list of 12 SO requirements** (Chapter 3), not the 7-item export policy.
2. **R4.2 is per-student only.** Batch-level items are a later card. The card was deliberately trimmed; see §12 for the follow-up cards.
3. **New status `Completed`** between `Active` and `Graduated`. `Graduated` now means "SO issued".
4. **Status transitions are restricted only around Completed/Graduated** (§4.2), with two logged escape hatches: Graduated → Completed (reason required) and Active → Graduated (completion date + reason required, for digitized archive records).
5. **SO numbers are per student** (assumption to verify, §13). Storing them is a later card.
6. **Checklist states:** ✓ met, ⚠ met with warning, ✗ unmet, — not yet due.
7. **Employment Status** is a new nullable column with four fixed values; *met* means *answered*, not *employed*.
8. **Permanent Record (Form IX): at least one** of the three Form IX types, labelled honestly and showing which one is on file.
9. **TOR:** file on record **and** grades final; a TOR uploaded before the latest grade lock is a ⚠ warning, not ✗.
10. **Student Information:** core TOR/Form IX fields are required; blank middle name is ⚠. Course and batch resolve section-first.
11. **Start and End Date:** `enrollment_date` (made editable on the edit form) + new `completion_date` (suggested when changing to Completed).
12. **Where:** a view-only **SO Checklist** tab in the `registrar.html` Student Record Details modal.
13. **Active students** see a preview (real states for items due now, "not yet due" for the rest, no verdict). Enrolling/Submitted students get no checklist.
14. **Upload dropdown gets no new types.** Instead, a non-blocking "Did you mean …?" hint on "Others" labels.
15. **`RequiredDocumentPolicy` is amended** so completion documents apply to Completed **and** Graduated.

## 2. The 12 TESDA Items and How Each Is Handled

| # | TESDA item | Bucket | In R4.2? | Source |
|---|---|---|---|---|
| 1 | Transcript of Records | per-student file + grades | Yes | `documents` + `class_enrollments`/`grades` |
| 2 | Student Permanent Record | per-student file | Yes | any Form IX `document_type` |
| 3 | Certificate of TVET Program | per-student file | Yes | `documents` |
| 4 | OJT Report | per-student file | Yes | `documents` |
| 5 | PSA Birth Certificate | per-student file | Yes | `documents` |
| 6 | Student Information | per-student data | Yes | `student_records` (+ section) |
| 7 | Start and End Date | per-student data | Yes | `enrollment_date`, `completion_date` |
| 8 | Employment Status | per-student data | Yes | new `employment_status` |
| 9 | List of Students | per-batch | No — later card | — |
| 10 | Attendance Sheet | per-batch | No — later card | — |
| 11 | Registry of Workers | per-batch | No — later card | — |
| 12 | Registry of Workers Assessed | per-batch | No — later card | — |

The UI must say "Student requirements" (not "Ready to file") because batch items are not checked.

## 3. Schema Changes

New migration `src/main/sql/migrations/<implementation-date>-so-checklist.sql`, idempotent like the existing dated migrations; mirror into `src/main/sql/schema.sql` and `src/main/sql/AnihanSRMS.sql`.

```sql
ALTER TABLE student_records ADD COLUMN employment_status VARCHAR(25) NULL;  -- after student_status
ALTER TABLE student_records ADD COLUMN completion_date DATE NULL;           -- after enrollment_date
```

- `student_status VARCHAR(25)` already fits `Completed`; no column change.
- No new tables. Like PR #68, this build **requires the migration**: entity paths fail against a DB without the columns.
- Applying it to the live `AnihanSRMS` DB needs the user's approval at implementation time.

## 4. Student Lifecycle

### 4.1 Statuses

`Enrolling` → `Submitted` (portal) → `Active` → **`Completed`** (training + OJT done) → `Graduated` (SO issued).

### 4.2 Transition rules — `service/StudentStatusTransitions.java` (new, pure, static, like `RequiredDocumentPolicy`)

Only moves involving Completed or Graduated are restricted; everything else behaves as today.

| From → To | Allowed | Extra input | Effect |
|---|---|---|---|
| Active → Completed | Yes | `completionDate` (required; dialog pre-fills today, editable) | sets `completion_date` |
| Any other → Completed | **No** | — | 400 |
| Completed → Active | Yes | — | clears `completion_date`; old value logged |
| Completed → Graduated | Yes | — | — |
| Active → Graduated | Yes (archive escape hatch) | `completionDate` + `reason` (both required) | sets `completion_date`; reason logged |
| Graduated → Completed | Yes | `reason` (required) | keeps `completion_date`; reason logged |
| Graduated → anything else | **No** | — | 400 |
| Any other (not Completed/Active) → Graduated | **No** | — | 400 |
| Among Enrolling / Submitted / Active | Unchanged from today | — | — |

- Enforced in `RegistrarService.updateStatus` (the request pattern only sees the target, not the current status).
- Applies only to changes through `PUT /api/registrar/student-records/{recordId}/status`; existing rows are never re-validated. The internal Submitted → Active in `ClassManagementService` is unaffected.
- `completionDate` validation: not in the future; not before `enrollment_date` when that is set.
- `reason`: trimmed, 1–255 characters.

### 4.3 API changes

- `UpdateStudentStatusRequest`: `ALLOWED_VALUES_PATTERN` → `^(Enrolling|Active|Completed|Graduated)$`; add optional `LocalDate completionDate` and `String reason`. Required-ness per §4.2 is checked in the service.
- `StudentRecordUpdateRequest` / `StudentRecordDetailsResponse`: add `enrollmentDate`, `completionDate`, `employmentStatus`.
  - `enrollmentDate`: editable for every status. Not in the future; not after `completion_date`.
  - `completionDate`: accepted only when the current status is Completed or Graduated, otherwise 400 if non-null and different from the stored value.
  - The edit form always sends the loaded values back, so a routine save never wipes them (pin with a test).

### 4.4 Front-end touch points for the new status

- `registrar.html` `#editStatusSelect`: add Completed; **disable options not allowed from the current status**; show a date field when the target needs `completionDate`; show a reason field when the target needs `reason`; explain why a date is asked when moving an old graduate.
- Status filters: `registrar.html` `#studentStatusFilter`, `student-numbers.html`.
- Badges: add `status-badge-completed` in `registrar-students.js`, `registrar-student-numbers.js`, and check `registrar-classes.js`.
- `student-records.html` edit form: make `#editEnrollmentDate` an editable `type="date"`; add `#editCompletionDate` (enabled only for Completed/Graduated) beside it.

### 4.5 Knock-on changes

- `RequiredDocumentPolicy.isGraduated` → completion documents apply to **Completed or Graduated**; update its tests that pin Graduated-only behaviour.
- `ClassManagementService` eligibility stays `Active`/`Submitted` → Completed students cannot be added to classes (intended).
- `StudentPortalController.checkDuplicate` blocks only Submitted/Active; Completed (and, already today, Graduated) slip past the pre-check, though `startOrResume` still blocks them. Recommended (confirm while planning): add Completed and Graduated to the pre-check.
- Side effect: once `enrollment_date` is filled, the generated TOR "Start of Training" and Form IX "Date of Admission" stop printing blank.

## 5. Employment Status

- Column `student_records.employment_status`, values **Employed / Self-employed / Unemployed / Further studies**, or NULL ("Not set").
- One Java constant holds the list; request validation and the dropdown both derive from it (as `UpdateStudentStatusRequest` does for statuses).
- Edit form: new **Employment** section directly under **On-the-Job Training (OJT)** in `student-records.html`, native `<select>` with a blank "Not set" option. Not stored on `student_ojt` (OJT is a placement, employment is an outcome).
- Checklist: met when non-null — including "Unemployed".

## 6. SO Readiness Policy — `service/SoReadinessPolicy.java` (new, pure, static)

Separate from `RequiredDocumentPolicy` by design ("SO readiness" vs "intake/export completeness"); both build on the shared `DocumentService.*_TYPE` constants.

### 6.1 States and stages

- Item state: `MET`, `WARNING`, `UNMET`, `NOT_DUE`, each with a short human-readable `detail`.
- Checklist stage from the student's status: Enrolling/Submitted → `NOT_APPLICABLE` (no items); Active → `PREVIEW`; Completed/Graduated → `FINAL`.
- In `PREVIEW`, items whose "due from" is Completed are `NOT_DUE`; the rest are evaluated normally. No verdict.
- In `FINAL`, verdict = **complete** when no item is `UNMET`; the summary always reports the warning count ("Complete — 1 warning").

### 6.2 Items

| Item (display label) | Due from | MET | WARNING | UNMET detail examples |
|---|---|---|---|---|
| PSA Birth Certificate | Active | type on record | — | "No PSA Birth Certificate on file" |
| Student Information | Active | last, first name, birthdate, sex, permanent address, course, batch all present | middle name blank: "No middle name — confirm this is correct" | "Missing: birthdate, permanent address" |
| Start and End Date — start | Active | `enrollment_date` set | — | "Enrollment date not set" |
| Start and End Date — end | Completed | `completion_date` set | — | "Completion date not set" |
| Permanent Record (Form IX) — at least one | Completed | any Form IX type; detail names which ("Form IX: Cookery NC II") | — | "No Form IX on file" |
| Transcript of Records | Completed | see §6.3 | TOR older than latest grade lock | see §6.3 |
| Certificate of TVET Program | Completed | type on record | — | — |
| OJT Report | Completed | type on record | — | — |
| Employment Status | Completed | non-null | — | "Employment status not set" |

- Course and batch resolve **section-first** (section's course/batch, else the student's own), matching the folder-tree ownership rules.
- "Others" documents (labelled or not) never satisfy anything.

### 6.3 TOR rule

Inputs: TOR present, newest TOR `upload_date`, and per enrollment the grade row (if any).

- **UNMET** when any of: no TOR file; the student has **zero** `class_enrollments`; any enrollment has **no grade row**; any grade row is **not locked**; any grade row has `remarks <> 'COMPETENT'` (incl. NULL).
  Detail names the reason and subjects, e.g. "TOR on file, but BPP-101 is not locked" / "INC in Cookery-102".
- **WARNING** when all of the above pass but the newest TOR `upload_date` < max(`grades.locked_at`): "TOR uploaded before the last grade lock — re-check it". Clears itself when a newer TOR is uploaded; no dismiss button.
- **MET** otherwise.
- Verified in code: `remarks` is derived from the *effective* grade (re-exam included) via `GradeEquivalent.remarkFor`; status codes map C → COMPETENT, FA → NOT_COMPETENT, INC/D → NULL. So `remarks = 'COMPETENT'` is the whole "clean" test.
- Known limit (documented, not shown in the UI): the system cannot tell whether the student was enrolled in *every* subject her course requires, because there is no server-side curriculum rule.

## 7. Backend — Checklist Endpoint

`GET /api/registrar/student-records/{recordId}/so-checklist` in `RegistrarController` (REGISTRAR via the existing `/api/registrar/**` rule). Read-only; writes **no** audit row (same as the export check). Unknown record → 404.

```java
public record SoChecklistResponse(String stage,            // NOT_APPLICABLE | PREVIEW | FINAL
                                  Boolean complete,         // null unless FINAL
                                  int warningCount,
                                  List<SoChecklistItem> items) { }

public record SoChecklistItem(String key, String label, String state, String detail) { }
```

Data gathering in a new `SoChecklistService` using `JdbcTemplate`, no BLOB columns (`content_data`, `profile_picture`), no `GROUP_CONCAT`/window functions (must run identically on MySQL and the H2 tests):

1. Student row + section's course/batch (LEFT JOIN `sections`).
2. Document types for the student + newest TOR `upload_date`.
3. Enrollments LEFT JOIN `grades` on `(class_id, student_id)` with subject code, `locked`, `locked_at`, `remarks`.

Grouping and evaluation happen in Java via `SoReadinessPolicy`.

## 8. Frontend — SO Checklist Tab

- `registrar.html` Student Record Details modal (`#studentRecordDetailsModal`, today a single grid of detail cards) gains Bootstrap tabs: **Details** | **SO Checklist**.
- The tab is hidden for Enrolling/Submitted. Content loads when the tab is first shown (not on modal open).
- PREVIEW banner: "Preview — the full checklist applies once the student is Completed." No verdict.
- FINAL summary: "Student requirements: Complete — 1 warning" / "3 of 9 unmet". Wording never says "Ready to file" (batch items unchecked).
- Each row: icon (✓ / ⚠ / ✗ / —) **plus** visually-hidden state text (not colour-only), label, detail.
- The existing **Edit** button (`#studentDetailsEditLink`) is the "fix it" path; no new deep-link work.
- Errors: 401 → existing session-expired notice; 404 → "This student is no longer available — refresh."

## 9. "Others" Label Hint on Upload

- New pure `service/DocumentTypeSuggester.java`: `Optional<String> suggest(String label)` → a real document type when the label matches an alias as a **whole word**, case-insensitive. The alias list lives only here (same idea as `StudentNumberImportMapping`):
  - TOR: `tor`, `transcript`, `transcript of records`
  - PSA Birth Certificate: `psa`, `birth certificate`, `birth cert`
  - Form IX: `form ix`, `form 9`, `permanent record` (suggest the generic "pick a Form IX type")
  - OJT Report: `ojt`, `ojt report`
  - Certificate of TVET Program: `tvet`, `tvet certificate`
  - Form 137: `form 137`, `f137`
- `GET /api/registrar/documents/type-suggestion?label=…` → `{ "suggestedType": "…" | null }`; read-only, no audit row.
- `registrar-documents.js` staged rows: when an Others row's label changes (debounced ~300 ms), call it; show "Did you mean *X*? [Switch]" **per row**. Never auto-switch, never block upload.
- Tests pin non-matches — "Director's letter", "Monitoring report", "History" suggest nothing — and one accepted match: "TOR request letter" suggests TOR (a hint only; the Registrar may ignore it).
- One-time manual review (not code): check existing labels from `GET /api/registrar/documents/labels` for misfiled TOR/PSA/Form IX uploads.

## 10. Audit Logging

- Status change text (existing row, extended), e.g.
  `Changed status of Dela Cruz, Ana from Active to Completed (completion date 2026-09-30)`
  `Changed status of … from Active to Graduated (completion date 2014-03-15; reason: Digitized archive record)`
  `Changed status of … from Completed to Active (cleared completion date 2026-09-30)`
  `Changed status of … from Graduated to Completed (reason: …)`
- Keep within `system_logs.action`'s 500 characters (reason capped at 255).
- `enrollment_date`, `completion_date` and `employment_status` edits are covered by the existing student-edit audit row.
- The checklist and suggestion reads write no rows.

## 11. Testing

- `SoReadinessPolicyTest`: every item × state; stage handling (NOT_APPLICABLE / PREVIEW / FINAL); section-first course/batch; middle-name warning; Unemployed counts as met; Others never counts.
- TOR cases: no file; zero enrollments; enrollment without grade row; unlocked row; INC/D (NULL remarks); FA; re-exam pass; stale TOR → WARNING; newer TOR clears it.
- `StudentStatusTransitionsTest`: full transition table incl. both escape hatches and their required inputs.
- `RegistrarService`/controller tests: status endpoint dates/reasons/clearing; audit text; `completionDate` validation; edit form round-trip never wipes the new fields.
- `RequiredDocumentPolicyTest`: Completed now requires completion documents.
- `SoChecklistService` integration test on the real-H2 setup: query returns correct rows with no BLOB columns.
- `DocumentTypeSuggesterTest`: aliases, whole-word rule, the non-match cases.
- Live check (Playwright, real DB) after the migration: tab hidden/preview/final; status dialog flows; edit form dates; upload hint.

## 12. Out of Scope → Follow-up Cards

| Card | Contents |
|---|---|
| SO Waivers & Sign-off | Per-item waivers with reasons/history; hard vs soft items; "Ready to file" tick; mark when requirements change after sign-off |
| SO Filing Folder | Special Order Filing → Batch → Course → Students view; batch-level document storage; batch readiness; filed-on date |
| SO Number & Graduation | Per-student `so_number` (UNIQUE) + issue date; Completed → Graduated requires it; correction endpoint; Form IX fill-in; optional bulk import |
| Archive Import | Create historical records directly from a sheet with status and dates (would retire the Active → Graduated escape hatch) |
| Form IX per qualification | Fixed list for TESDA slot titles → one Form IX required per filled slot |
| Data fix | Move the three delayed batches to Completed with real completion dates (Registrar-approved) |

## 13. Assumptions to Verify with the Registrar

1. SO numbers are per student (the Form IX template has an SO line per NC II assessment block, which hints otherwise).
2. Employment Status values — whether TESDA uses different wording or wants "Abroad" separately.
3. Whether TESDA's "Start and End Date" means calendar dates (assumed) or school-year/semester labels.
4. Which of the 12 items are non-waivable (needed by the Waivers card, not R4.2).
