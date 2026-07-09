# Plan: Document Management + Template Generation (R3.1–R3.7)

**Source**: Jira board `capstoned.atlassian.net`, project AGILE, parent feature AGILE-17
"REGISTRAR: R3 Document Management" — tickets AGILE-75 … AGILE-81 (R3.1–R3.7), plus
user-requested template generation (overlaps R3.9 "Encode Document").
**Branch**: `feature/document-management`
**Complexity**: Large

## Requirements

| Ticket | Req | Requirement |
|---|---|---|
| AGILE-75 | R3.1 Upload | Registrar uploads `.pdf` / `.docx` / `.xlsx` files linked to a student record |
| AGILE-76 | R3.2 Type | Uploads categorized: TOR, Form IX (BPP / Cookery / FBS), Form 137, etc. |
| AGILE-77 | R3.3 Name | Document name + type visible in listings |
| AGILE-78 | R3.4 View | Registrar previews document contents in-app |
| AGILE-79 | R3.5 Search | Search by student ID and student name |
| AGILE-80 | R3.6 Filter | Filter by category — type, section, batch, etc. |
| AGILE-81 | R3.7 Download | Download a copy of any stored document |
| (user) | Generation | Auto-filled, editable, print-ready generation of the 4 templates in `document-templates/` |

**Confirmed decisions** (user, 2026-07-09):
- Output format: print-ready HTML page (print CSS, browser print-to-PDF). No new dependencies.
- Fill mode: auto-fill from DB + editable review form before generating.
- Persistence: generated documents saved into `documents` BLOB table + `system_logs` entry.
- Curriculum source: static template data transcribed from the PDFs, merged with DB grades
  by `subject_code`. Seeding ~50 curriculum subjects into `subjects` is deferred (would
  entangle class management; `subjects.units` is INT but templates need decimals).

## Template Analysis (document-templates/)

- **TOR.pdf** — student header (name, address, admission date, sex, birthday, student no.,
  entrance/graduation, entrance data, SY ended), course title, certificate no.,
  3 TESDA assessment blocks (BPP / FBS / Cookery: date taken, result, SO no., issued),
  remarks, two semester subject tables (code, title, final, re-exam, hours, units),
  grading-system legend, Prepared by / Approved by signatories.
- **Form IX ×3** (per qualification; two variants each: "Records of Candidate for
  Graduation" and "Student's Permanent Record") — header + parent + ULI + start/end of
  training + assessment row, education history (Elementary/JHS/SHS/Last School + years —
  maps to `student_education`), one qualification's subject table with Remarks
  (Competent) and totals row, certification block, Registrar signatory.

## What Already Exists

- `documents` table (`schema.sql:183`) + `Document` JPA entity — no repository/service/
  controller/UI. **No DB migration required.**
- Multipart limits: 10MB file / 15MB request (`application.properties`).
- Auto-fill sources: `student_records`, `parents`, `student_education`,
  `student_tesda_qualifications`, `student_ojt`, `grades`, batch/course/section.

## Patterns to Mirror

| Category | Source | Pattern |
|---|---|---|
| Controller | `RegistrarController.java:31-80` | Constructor injection; `system_logs` via LogContext + IP on state changes |
| Separation | `decisions.md` 2026-05-09 | New `DocumentController` under `/api/registrar/documents` |
| Errors | `GlobalExceptionHandler` | `IllegalArgumentException` → 400 actionable message; sanitized 500 |
| Input guard | `StorageService.store()` | Whitelist validation before file handling |
| Frontend | `sections.html` + `registrar-sections.js` | Registrar page pattern: DataTable, modals, cache-buster, auth-guard |
| Tests | `ClassManagementSectionServiceTest` / `...WebMvcTest` | Mockito service + WebMvc RBAC/validation tests |

## Files to Change

| File | Action | Why |
|---|---|---|
| `repository/DocumentRepository.java` | CREATE | Finders + BLOB-free summary projection queries |
| `dto/registrar/DocumentSummaryResponse.java` | CREATE | Listing DTO without `content_data` |
| `service/DocumentService.java` | CREATE | Upload validation, search/filter, content fetch, generated-doc save |
| `controller/DocumentController.java` | CREATE | REST endpoints + system_logs |
| `config/SecurityConfig.java` | UPDATE | Add `/documents.html`, `/generate-document.html` to registrar matcher |
| `static/documents.html` | CREATE | Documents DataTable page + upload/view modals |
| `static/js/registrar-documents.js` | CREATE | Page logic |
| `static/generate-document.html` | CREATE | Template picker + editable review form + print preview |
| `static/js/registrar-generate-document.js` | CREATE | Auto-fill, form↔preview binding, save/print |
| `static/js/curriculum-templates.js` | CREATE | Static transcription of the 4 PDF subject tables |
| `static/css/document-print.css` | CREATE | Print-faithful document layout (@page, tables, legend) |
| `static/registrar.html`, `subjects.html`, `classes.html`, `sections.html`, `student-records.html` | UPDATE | Navbar 4 → 5 links (add Documents) |
| `test/.../DocumentServiceTest.java` | CREATE | Service tests |
| `test/.../DocumentControllerWebMvcTest.java` | CREATE | WebMvc RBAC/multipart/download tests |
| `memory-bank/*` | UPDATE | Session records |

## Tasks

### Phase 0 — Branch
Create `feature/document-management` from `main`.

### Phase 1 — Backend core (R3.1, R3.2, R3.3, R3.7)
- `DocumentRepository` with projection queries that never select `content_data`.
- `DocumentService.upload()`: student-exists check, extension + MIME whitelist
  (pdf/docx/xlsx), type from fixed list; `getContent(id)` for view/download.
- `DocumentController`: `POST /api/registrar/documents` (multipart),
  `GET /api/registrar/documents` (list), `GET /{id}/download` (attachment),
  `GET /{id}/view` (inline). Upload/download/generate write `system_logs`.
- **Validate**: `./gradlew test`

### Phase 2 — Search & filter (R3.5, R3.6)
- JPQL joining `student_records`: `?q=` matches student ID or name; `type`,
  `sectionCode`, `batchCode` params.
- **Validate**: service tests for filter combinations.

### Phase 3 — Frontend documents page (R3.1–R3.7)
- `documents.html` + `registrar-documents.js`: DataTable, Upload modal (student search
  picker, type select, file input), View modal (iframe for PDF/HTML; docx/xlsx
  download-only), Download button, filter bar.
- SecurityConfig matcher + navbar updates on all registrar pages.
- **Validate**: `./gradlew test` + manual smoke.

### Phase 4 — Template generation
- `curriculum-templates.js`: PDF subject tables transcribed (sections: Basic, Common,
  Core per qualification, Other Subjects, 2nd-sem OJT block; code/title/hours/units).
- `GET /api/registrar/documents/generate-data/{studentId}`: aggregated auto-fill payload.
- `generate-document.html`: student + template picker (TOR; Form IX per qualification ×
  Candidate/Permanent variant), editable review form, live print-preview, Print button.
- `POST /api/registrar/documents/generate`: store filled self-contained HTML in
  `documents` (`file_type: text/html`) + `system_logs`.
- **Validate**: `./gradlew test`; visual side-by-side with the sample PDFs.

### Phase 5 — Tests + memory bank
- `DocumentServiceTest`, `DocumentControllerWebMvcTest`, generation-data tests.
- Full suite green; update `activeContext.md`, `progress.md`, `changeLog.md`, `testing.md`.

## Validation

```bash
./gradlew test
./gradlew bootRun   # manual smoke: upload → list → search/filter → view → download → generate
```

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| LONGBLOBs loaded in list queries exhaust memory | HIGH impact | Projection queries exclude `content_data`; only view/download fetch it |
| TOR header fields with no DB source (certificate no., SO no., entrance data) | MEDIUM | Editable review form; blanks print as blank lines |
| Static curriculum JSON drifts from future `subjects` seeding | MEDIUM | Documented deferral; follow-up ticket for curriculum seed + units DECIMAL migration |
| docx/xlsx cannot preview in-browser | LOW | View = download for those types; stated in UI |
| Print fidelity vs original PDFs | MEDIUM | `@page` print CSS tuning; visual smoke test |

## Acceptance

- [ ] R3.1–R3.7 flows work end-to-end from `documents.html`
- [ ] All 4 templates generate, print cleanly, and persist to `documents`
- [ ] Every upload/download/generate writes a `system_logs` row
- [ ] Full Gradle suite green
- [ ] Memory bank updated