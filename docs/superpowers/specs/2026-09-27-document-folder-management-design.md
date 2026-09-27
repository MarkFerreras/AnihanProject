# Document Folder Management Design

**Date:** 2026-09-27 | **Status:** Product decisions confirmed; revised specification for review.
**Intent:** Registrars can find, upload, and export student records through a folder explorer while retaining the Documents table.
**Stack:** Java 25, Spring Boot 4.0.4, Spring Data JPA, MySQL 8, Bootstrap 5.3, local jQuery/DataTables, vanilla JavaScript.

## 1. Confirmed scope

- Export a student, section, batch's unassigned students, or entire batch.
- Include all student statuses and a synthetic **No Batch** group.
- Export all stored originals, including generated HTML and ID photos; no conversion.
- Bulk upload: 20 files maximum, 10 MiB per file, 50 MiB combined, all-or-nothing.
- No schema changes, dependencies, external assets, folder CRUD, or document-generation changes. Preserve existing generation/editing, ID-picture workflows, single upload/download/view/delete, and table filters.
- ZIP originals differ from existing single-download HTML-to-DOCX behavior; explain this beside Export.

## 2. Hierarchy and identity

Batch -> Section or Unassigned Students -> Student. Include empty batches/sections and zero-document students; always show each batch's Unassigned Students folder.

Assigned students belong beneath their **section's batch**, even when their own batch differs/is null. Unassigned students use their own batch. Students with neither appear under **No Batch -> Unassigned Students**. No Batch appears only when needed; student export works there, but synthetic group export is outside scope. Every student appears exactly once; never modify enrollment data to build the tree.

Sort batches by year descending then code, No Batch last; sections by code; students by last name, first name, reference. Counts sum displayed descendants and include all stored document rows. Missing assignments have **No Section Assigned** / **No Batch Assigned** badges.

Show **Student Number** when present and **Reference No.** (studentId) separately. Upload suggestions search name, number, or reference using tree metadata and display all three. Require explicit unique selection; editing its label clears the selected reference. Duplicate names never select the first match automatically. Names/numbers are not server-side upload identifiers.

## 3. API and DTO contracts

Paths are relative to /api/registrar/documents. Existing ROLE_REGISTRAR authorization applies; new API routes return 401 anonymous, 403 ADMIN/TRAINER. Before response commitment, errors use the existing JSON {message} shape.

| Method/path | Contract |
|---|---|
| GET /folders/tree | 200 `List<DocumentFolderHierarchyResponse>`; empty database -> [] |
| GET /student/{studentId} | Exact reference; 200 `List<DocumentSummaryResponse>`, upload date/id descending; no documents -> []; unknown student -> 404 |
| POST /batch | Multipart studentId, repeated files, repeated documentTypes paired by position; 201 ordered `List<DocumentSummaryResponse>` |
| GET /export/student/{studentId} | Student archive |
| GET /export/section/{sectionCode} | Section archive |
| GET /export/batch/{batchCode}/unassigned | Batch's students with null section |
| GET /export/batch/{batchCode} | Entire displayed batch: its sections plus unassigned students |

Unknown export scopes return 404. Existing scopes with no documents return 409 {"message":"No documents to export."}, through a dedicated exception/handler. Never substitute substring search (?q=studentId) for exact student listing. The new upload parameter is deliberately studentId, consistent with existing uploads.

Tree record fields; lists never null:

~~~text
DocumentFolderHierarchyResponse(
  String batchCode, Short batchYear, long studentCount, long documentCount,
  List<SectionFolderDto> sections, List<StudentFolderDto> unassignedStudents)
SectionFolderDto(
  String sectionCode, String sectionName, String courseName,
  long studentCount, long documentCount, List<StudentFolderDto> students)
StudentFolderDto(
  String studentId, String studentNumber, String firstName, String lastName,
  String studentStatus, long documentCount)
~~~

Both batchCode and batchYear null denote No Batch; sections is empty. sectionName maps to Section.section; batchYear is Short. Profile/breadcrumb context comes from ancestry.

## 4. Reads and atomic upload

Tree reads use three scalar queries: batches; sections joined to course/batch; students with left-joined, grouped document counts. Build the hierarchy in memory. No per-student/section queries; exclude documents.content_data AND student_records.profile_picture. Loading StudentRecord entities is not metadata-only.

Retain the global summary projection; add exact student summary projection. Use JdbcTemplate (available through the JPA starter) for new folder metadata/content read repositories, and JPA for existing document writes. No entities in API responses. Fetch the complete tree once per refresh; pagination/lazy tree loading is deferred.

Validate the entire upload before writing:

- Valid studentId; unknown reference -> 404. Missing/mismatched fields or invalid files -> 400 with index/name where applicable.
- 1–20 nonempty files; each <= 10 * 1024 * 1024 bytes; sum <= 50 * 1024 * 1024 bytes using long.
- Case-insensitive pdf/docx/xlsx extensions; stored MIME comes from the existing extension map. Preserve current validation semantics; content scanning is outside scope.
- Categories come from /types, excluding ID Picture (1x1 / 2x2). Retain that category in the table filter and dedicated picture routes.
- Strip browser path prefixes; require a nonblank basename, <=255 characters, without dot-only names, controls, or invalid Windows filename characters. Duplicate filenames create separate records, never overwrite.

One public Spring-proxied transaction includes all inserts **and one success audit row**. File-read, persistence, or audit failure rolls back everything. Resolve the student once and extract/reuse validation/save helpers; uploadSingle does not currently exist. Flush and requery scalar summaries for persisted timestamps, preserving file order. Do not stage all file bytes separately.

Multipart configuration: max-file-size=10MB, max-request-size=52MB (50 MiB payload plus overhead), file-size-threshold=0 for disk spooling. The request ceiling is application-wide; other endpoints retain business limits. Keep oversized-upload status 400 when handled by Spring, with an accurate per-file/request message. Container-level rejection may abort before JSON advice; the UI needs a useful non-JSON failure fallback. Missing multipart parameters/parts return 400, not generic 500.

Audit includes user ID, username, role, remote IP, student reference, and count; keep action text within system_logs.action's 500-character limit.

## 5. ZIP construction

Build an immutable, BLOB-free manifest before headers. Scope predicates exactly match section-ownership rules in section 2. Full-batch membership uses section.batch for assigned students and student.batch for unassigned students.

| Scope | Download filename | Entry layout |
|---|---|---|
| Student | Documents_{numberOrId}_{lastName}.zip | {fileName} |
| Section | Documents_Section_{sectionCode}.zip | {studentFolder}/{fileName} |
| Unassigned | Documents_Batch_{batchCode}_Unassigned.zip | {studentFolder}/{fileName} |
| Batch | Documents_Batch_{batchCode}.zip | Sections/{sectionCode}/{studentFolder}/{fileName}, or Unassigned/{studentFolder}/{fileName} |

studentFolder = {LastName}_{FirstName}_{numberOrId}_{studentId}. Reference suffix prevents same-name student collisions. Omit empty directories.

- Use application/zip, Content-Disposition attachment via Spring's filename builder, Cache-Control: no-store; no guessed Content-Length.
- Stable student-reference/document-id entry order. Sanitize every component: discard path prefixes, remove controls/separators/drive markers and trailing dots/spaces; handle Windows reserved names. Use document-{documentId} or the relevant stable ID when empty. Cap components at 120 characters, preserving extensions.
- Allocate case-insensitive unique paths after sanitation/truncation, suffixing (1), (2), etc.; recheck collisions with already suffixed names and sanitized student/section folders. No absolute paths or dot/dot-dot components.
- Copy one document at a time from a parameterized single-row JDBC query using getBinaryStream, an 8 KiB buffer, and ZipOutputStream. Close each input/JDBC resource before the next file. Never fetch a List<Document>, buffer the archive, or materialize all file contents.
- The driver may buffer the current BLOB: memory is bounded by metadata + largest individual stored BLOB + ZIP bookkeeping, **not** by an 8 KiB total. Legacy/generated files may exceed the upload limit. Verify with the actual MySQL driver and several large files.
- Manifest membership/names are fixed when prepared. Later uploads are excluded; deletion of a manifested row fails the export; an in-place edit may provide newer bytes. This is not a snapshot/backup format.
- Before headers, append one **Requested ZIP export** audit with identity/IP, scope/key, and document count. Audit failure aborts the request. This records an attempt, never browser download completion.
- On failure/disconnect, close resources and log server-side. Reset an uncommitted response for a normal error; never append JSON/HTML to a committed ZIP or claim success. Finish the ZIP only after all entries succeed, leaving the servlet stream container-owned. Java's [ZipOutputStream.close](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/zip/ZipOutputStream.html#close()) completes output, so unconditional try-with-resources closure is insufficient for abort semantics.

## 6. UI and state

- Default Folder Explorer tab; All Documents Table preserves IDs, filters, actions, and Generate Document link. Adjust DataTables columns when its tab becomes visible.
- Desktop ~1/3 navigation and 2/3 content; stack on mobile. Use nested lists, real buttons, aria-expanded, labels, and visible focus. Do not claim an ARIA tree without its keyboard behavior.
- Filter names, number/reference, section/batch text case-insensitively; retain matching ancestors; show no-match state. Selection is independent of expansion/filtering.
- Batch overview shows sections/unassigned cards and full-batch Export; section/unassigned views list students; student view shows breadcrumb/profile/counts/files. Disable Export at zero count; No Batch has no group Export.
- Reuse existing preview: PDF/image/HTML inline, DOCX/XLSX download notice, generated HTML Edit link. Delegate card/table View/Download/Delete once to a shared ancestor. Preserve type-delete confirmation.
- Student-context Upload locks the selected student; other entry points require selection. Reopen resets staging/alerts/noncontextual selection. File chooser and drag/drop append to the same ordered list; each row has category/removal, default Others. Show count/total and disable invalid/pending submissions.
- Success refreshes tree/counts/selected content/table while retaining expansion, selection, filters, and table page where possible. Failure preserves staging. After successful upload but failed refresh, report saved/refresh failed and prevent resubmitting that completed upload.
- Abort/token-guard requests: a late response for student A cannot overwrite B. Provide loading, retryable error, empty, and session-expired states; clear preview iframe on close.
- Encode URL segments; use textContent/DOM attribute APIs for dynamic values. Existing escapeHtml alone is unsafe for quoted HTML attributes.
- Native ZIP download links avoid fetch-to-Blob buffering. Explain that the browser reports download errors; no unsupported completion toast.

## 7. Required evidence

| Area | Acceptance coverage |
|---|---|
| Hierarchy | Empty/inactive/null/mismatched batch, zero files, unique placement/counts; real scalar-query tests |
| Exact lookup/security | Prefix-collision fixture; relevant 200/400/401/403/404/409 assertions |
| Atomic upload | Limits, misaligned fields, later file-read failure; real transaction rollback including audit failure; actual servlet request ceiling |
| ZIP | Parsed entries/bytes for all four scopes; unsafe/colliding names; empty/missing scopes; failed output/resource cleanup; MySQL large-file smoke |
| UI | Both tabs; chooser/drop/remove; ambiguous names; stale requests; upload/delete refresh; preview/edit/download; keyboard/mobile; denied roles |

See the [implementation plan](../plans/2026-09-27-document-folder-management.md) for task ownership and verification commands. This documentation review does not claim runtime verification.
