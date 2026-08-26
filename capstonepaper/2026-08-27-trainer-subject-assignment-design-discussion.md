# Design Discussion: Trainer–Subject–Class Assignment Model

**Date:** 2026-08-27
**Status:** Decision made (remove `subjects.trainer_id`); UX follow-up options discussed, **none implemented yet**
**Participants:** Registrar/dev lead (user) + Claude (AI pair)

## 1. Business logic being verified

The user described how trainer/subject/class assignment is supposed to work at Anihan:

> Multiple trainers can be assigned to handle a subject. Under a subject is
> multiple classes, each class assigned by a trainer. Then a class teaches a
> section.

The question was whether the current system (as of the `edit_subjects` branch)
actually follows this.

## 2. What the code was found to actually do

Verified directly against the source (not from memory-bank summaries alone):

| Rule | Modeled? | Where |
|---|---|---|
| A subject can have many classes | Yes | `classes.subject_code` has no uniqueness on its own; only `(section_code, subject_code, semester)` is unique |
| Each class has one trainer | Yes | `classes.trainer_id` |
| A class teaches one section | Yes | `classes.section_code` |
| Multiple trainers can handle a subject | Yes, but **only via the class layer** | A subject can have many `classes` rows, each with an independent trainer |

**The catch:** `subjects.trainer_id` (a single nullable FK, added 2026-05-09) is a
separate field from `classes.trainer_id`. Grepping every use of `Subject.getTrainer()`
across the codebase confirmed it is used **only** for:
- Display on the Subjects page's "Assigned Trainer" column
- Pre-filling the trainer dropdown on the Create Class modal

It has **zero role** in authorization or trainer-facing logic. Every real
assignment/ownership check (`TrainerService`, `TrainerGradeService`'s grade-entry
ownership checks, class rosters) reads exclusively from `classes.trainer_id`.

**One real constraint worth naming:** the unique key on
`(section_code, subject_code, semester)` means two trainers cannot co-teach the
*exact same* section's instance of a subject in the same semester (e.g. a
lecture/lab split). Different sections or different semesters, however, can
each have their own trainer for the same subject — that case already works.

### Why does `subjects.trainer_id` exist at all?

Checked `decisions.md` and `changeLog.md` for the original 2026-05-09 rationale.
The full documented reasoning:

> "Two trainer touchpoints (subject and class). A trainer can be assigned as a
> *default* on the Subject (via Subjects page) and again on a specific Class
> (per-section, per-semester). The class-level trainer is the authoritative
> teacher; the subject-level trainer is a convenience pre-fill on the Create
> Class modal."

There is no deeper rationale on record. It was never a deliberate "a subject
may only have one trainer" business rule — it's a byproduct of implementing a
UX convenience as a single-value column rather than deriving it from `classes`.

## 3. Decision

**Remove `subjects.trainer_id`.** Rationale:
- It is functionally redundant — nothing reads it for access control.
- It is actively misleading — it visually implies "one trainer per subject,"
  which contradicts the real, working, class-level multi-trainer capability.
- It carries a staleness risk — nothing keeps it in sync with who is actually
  teaching live classes of that subject.
- A dedicated trainer↔subject join table (an alternative also discussed) was
  rejected for the same reason `competencies` was earlier rejected as its own
  lookup table (see `decisions.md`, 2026-08-26): it would duplicate a fact
  `classes` already represents, unless there's a genuine need to distinguish
  "qualified/eligible to teach" from "currently teaching a live class" — no
  such need was identified.

**Not yet implemented.** This session was investigation + decision only. Next
step (separate task) is the actual migration/entity/DTO/service/frontend removal,
following the same pattern used for the `competency_type` and subject-code-rename
changes earlier this branch (migration + `schema.sql` + entity + tests + live
verification).

## 4. Three options considered for surfacing "who teaches what" after removal

Once `subjects.trainer_id` is gone, the Subjects page loses its "Assigned
Trainer" column. Three options were discussed for how the registrar should see
trainer↔subject↔class assignments going forward.

### Option 1 — View via the existing Classes page

Do nothing new. The Classes page (`classes.html`) already has Section, Subject,
and Trainer as columns in the same row, and DataTables' built-in search box
searches across all of them.

- **Initial framing:** "most straightforward, but a bit inconvenient."
- **Correction:** it's less inconvenient than it sounds — a registrar can
  already type a trainer's name and see every subject/section they teach,
  today, with zero new code.
- **Real limitation:** it's a flat, filterable list, not a *grouped* view.
  There's no single screen that answers "show me everything Trainer Maria
  teaches" without relying on search-and-scan.

### Option 2 — Restructure Subject/Class browsing into a drill-down

Clicking a subject shows its details and the list of classes teaching it
(and clicking a class shows its own details).

- **Initial framing:** correctly identified by the user as the heaviest option,
  risky to raise with the research adviser this close to a sprint boundary.
- **First-pass assessment:** agreed it was heavy — read literally ("clicking a
  subject *directs* the user" to a detail view), this implies a new page/URL
  navigation pattern with no existing precedent anywhere in the registrar UI.
- **Revised after further investigation:** there is a second way to build the
  *same outcome* that is **not** heavy. `trainer-subjects.html`/
  `trainer-subjects.js` (the trainer's own read-only "My Subjects" page)
  already implements exactly this shape of interaction — click a subject row →
  a modal opens → a second DataTable loads that subject's data via one AJAX
  call. No new page, no new routing.
  - Applied to this case: clicking a subject on `subjects.html` opens a modal
    listing that subject's classes (section, trainer, semester, enrolled
    count) — columns that already exist on the Classes page today. The class
    list is small enough at this school's scale to filter client-side, so no
    new backend endpoint is strictly required.
  - From within that modal, clicking a class could reuse the *already-built*
    Edit Class Trainer modal to change its trainer.
  - One known wrinkle: Bootstrap's modal-stacking quirks. This project has
    already solved that exact problem once (the delete-confirmation flow
    closes one modal before opening the next) — the same discipline applies
    here, not a new problem to solve from scratch.
- **Conclusion:** the "full detail page" reading of Option 2 is still worth
  deferring. The "modal drill-in" reading is not heavy, reuses a proven
  in-codebase pattern, and delivers the real UX benefit the user was after
  (grouped, organized classes-per-subject instead of a flat table).

### Option 3 — Dedicated Trainers page

A new page listing trainers and the subjects/classes they teach, plus a way to
assign a trainer.

- **Correction to the original framing:** once `subjects.trainer_id` is gone,
  there is no way to "assign a trainer to a subject" in the abstract —
  assignment only ever happens at the *class* level (a trainer is tied to one
  specific section+subject+semester). So this page's real job is (a) a
  trainer-centric grouped *view* of existing classes, and (b) a shortcut into
  the *existing* class-trainer-edit action — not a new assignment mechanism.
- **CRUD concern raised by the user** ("creating a trainer is done by admin")
  — a non-issue. This page never needs to create trainer *accounts*; it only
  needs to pick among existing enabled trainer accounts, exactly like the
  Assign Trainer and Create Class modals already do via the existing
  trainers-lookup endpoint. Admin still owns account creation, unchanged.
- **Feasibility finding:** the backend pieces this page needs already exist —
  `SchoolClassRepository.findByTrainerUserId()` (built for the trainer's own
  "My Classes" view) and `PUT /classes/{classId}/trainer` (built for Edit
  Class Trainer) are both already implemented. This page is mostly a new list
  endpoint + frontend, not a new subsystem. It also fits the registrar navbar
  pattern (5-link → 6-link) already used three times in this project's history.

## 5. Where this landed

Options 1 and the "full page" reading of Option 2 were set aside — Option 1 as
insufficient on its own (no grouping), the full-page version of Option 2 as
unnecessarily heavy for what's actually needed.

**Option 3 (trainer-centric page) and the modal-drill-in reading of Option 2
(subject-centric grouped view) are not competing alternatives — they answer
two different questions** ("what does this trainer teach" vs. "who teaches
each section of this subject") and both are cheap to build given how much of
each reuses code that already exists. Building both in the same pass is
reasonable for one sprint.

**Explicitly noted:** none of these three options are required by Anihan's
actual business logic (SO processing, record digitization, TESDA compliance).
This entire discussion is an internal admin-UX improvement layered on top of
data that is already correct — worth keeping in mind when framing this for the
research adviser.

## 6. Open items / not yet decided

- Whether to build both (Option 3 + modal-drill-in Option 2) in the same pass,
  or sequence them.
- Exact scope/timing relative to the current sprint.
- The actual removal of `subjects.trainer_id` (schema migration + code) has
  not been implemented — this document is a decision + design record only.
