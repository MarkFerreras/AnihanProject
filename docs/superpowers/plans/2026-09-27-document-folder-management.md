# Document Folder Management Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans, superpowers:test-driven-development, and superpowers:verification-before-completion; track checkboxes.
> **User-selected workflow:** One sequential Claude Sonnet 5 implementer, then one independent final review. This overrides per-task implementer/spec/code-review subagent recommendations.

**Goal:** Folder explorer, atomic bulk upload, and student/section/unassigned/full-batch ZIPs.
**Architecture:** Existing controller/service/JPA writes; scalar JDBC metadata and one-BLOB-at-a-time ZIP output; shared table/explorer actions.
**Stack:** Java 25, Spring Boot 4.0.4, MySQL 8, Bootstrap 5.3, local jQuery/DataTables.
**Spec:** [Document Folder Management Design](../specs/2026-09-27-document-folder-management-design.md); read once, use as the behavior/constant source.
**Status:** Scope and workflow confirmed; revised artifacts ready for review. No implementation performed.

## Global constraints

- ROLE_REGISTRAR only; no schema changes, dependencies, external assets, folder CRUD, or generation changes.
- All statuses; No Batch group; section ownership determines assigned students' batch.
- Four ZIP scopes, stored originals including HTML/photos.
- 20 files maximum, 10 MiB/file, 50 MiB combined; atomic documents + success audit.
- Metadata excludes content_data AND profile_picture; ZIP memory = metadata + largest BLOB + bookkeeping.
- Preserve existing table, generation/editing, ID-picture, and single-document workflows.
- This documentation revision stays on the current branch. Later implementation must inspect branch/dirty state, preserve changes, and create/reuse feature/document-folder-management under repository rules. Never reset to main or auto-merge/push.

## Superpowers + ECC context policy

Superpowers owns sequencing/TDD/verification/review. ECC supplies targeted Java/Spring guidance and strategic compaction; avoid duplicate planners, TDD loops, orchestration, and reviews. Adapt guidance to Boot 4/Gradle.

Locally verified plugins: Claude Superpowers 5.1.0, ECC 2.0.0-rc.1. Recheck available skills at execution; no automatic upgrades, model switching, or invented model ID. Use the user's selected Sonnet model.

Read project rules, spec, plan, and current-task files once; use targeted rg searches and relevant memory excerpts. CLAUDE.md imports the whole memory bank: this plan cannot remove that startup cost. Changing imports/settings is separate work.

Compact only at completed-task boundaries after recording next task, interface decisions, command/results, and blockers. Optional ECC_SESSION_START_MAX_CHARS=4000 caps ECC startup context, not CLAUDE imports; no hook installation/configuration is required.

Each task follows RED -> implement -> GREEN -> commit. Record concise evidence; run focused tests per task and the full suite at completion. Resolve source/contract conflicts explicitly.

## Review focus

1. Duplicate names/reference prefixes -> explicit selection and exact query (Tasks 1, 4).
2. Inactive/null/mismatched batch -> unique placement and matching export membership (1, 3).
3. Late file/audit failure -> rollback and no misleading retry (2, 4).
4. Unsafe/colliding ZIP names/disconnect -> safe entries, cleanup, no JSON in ZIP (3).
5. Hidden tabs/stale responses -> shared actions, preserved filters, correct student panel (4).

## Paths and baseline

Prefixes: J = src/main/java/com/example/springboot/; T = src/test/java/com/example/springboot/; R = src/main/resources/. Other paths are repository-relative. Brace lists name individual files.

Verified anchors: J/service/DocumentService.java, J/controller/DocumentController.java, J/repository/DocumentRepository.java, J/model/{Document,StudentRecord,Section,Batch}.java, J/dto/registrar/DocumentSummaryResponse.java.

Batch.batchYear is Short; Section.section is its label; StudentRecord contains a BLOB; Open Session in View is false. Global searchSummaries is already BLOB-free. Existing request cap is 15MB. Folder/ZIP APIs and uploadSingle do not exist.

- [ ] Confirm spec, implementation branch, and git status.
- [ ] Baseline: .\gradlew.bat test --tests "*DocumentServiceTest" --tests "*DocumentControllerWebMvcTest" --console=plain
      Expect BUILD SUCCESSFUL; record preexisting/environment failures separately. Bash uses ./gradlew with the same arguments.

## Task 1: Folder metadata and exact student listing

**Files**
- Create J/dto/registrar/DocumentFolderHierarchyResponse.java, J/repository/DocumentFolderRepository.java, J/service/DocumentFolderService.java.
- Modify J/repository/{DocumentRepository,StudentRecordRepository}.java, J/service/DocumentService.java, J/controller/DocumentController.java.
- Create T/service/DocumentFolderServiceTest.java, T/integration/DocumentStorageIntegrationTest.java.
- Extend T/service/DocumentServiceTest.java, T/controller/DocumentControllerWebMvcTest.java. Add @MockitoBean for new constructor dependencies using existing Boot 4 imports.

**Interfaces** (tree DTO is spec §3):

~~~text
DocumentFolderRepository nested records:
  BatchRow(String batchCode, Short batchYear)
  SectionRow(String sectionCode, String sectionName, String courseName, String batchCode)
  StudentRow(String studentId, String studentNumber, String firstName, String lastName,
             String studentStatus, String batchCode, String sectionCode, long documentCount)
Repository: List<BatchRow> findBatchRows()
            List<SectionRow> findSectionRows()
            List<StudentRow> findStudentRows()
DocumentFolderService: List<DocumentFolderHierarchyResponse> getFolderHierarchy()
StudentRecordRepository: boolean existsByStudentId(String studentId)
DocumentRepository: List<DocumentSummaryResponse> findSummariesByStudentId(String studentId)
DocumentService: List<DocumentSummaryResponse> getStudentDocuments(String studentId)
~~~

- [ ] Write tests: 2026/2025 batches, empty section, active/inactive assigned/unassigned students, null/mismatched batch, zero files. Assert unique placement, sorting, labels, aggregate counts, No Batch last. MVC: fields, [] vs 404, registrar 200, anonymous 401, ADMIN/TRAINER 403.
- [ ] Add real H2 projection tests using StudentRecordH2LoadTest's Boot 4 annotations/required-field fixtures. SR20260001 must not match SR202600010. Execute the three JDBC queries; capture SQL with a JdbcTemplate spy to verify three reads independent of student count and no BLOB columns/SELECT *. Match schema.sql's upload_date default in test schema; JPA create-drop alone does not reproduce that database default.
- [ ] RED: .\gradlew.bat test --tests "*DocumentFolderServiceTest" --tests "*DocumentStorageIntegrationTest" --tests "*DocumentControllerWebMvcTest" --tests "*DocumentServiceTest" --console=plain
- [ ] Implement scalar reads: batches; sections/course; students LEFT JOIN documents, grouped by selected student columns. Assemble hierarchy per spec §2. Add exact summary query and both endpoints, checking student existence without loading its entity.
- [ ] GREEN: rerun RED command; inspect query assertions.
- [ ] Commit: feat: add document folder metadata and exact student listing.

## Task 2: Atomic bulk upload

**Files**
- Create J/dto/registrar/DocumentAuditContext.java.
- Modify J/service/DocumentService.java, J/controller/DocumentController.java, J/repository/DocumentRepository.java, J/exception/GlobalExceptionHandler.java, R/application.properties.
- Extend T/service/DocumentServiceTest.java, T/controller/DocumentControllerWebMvcTest.java, T/integration/DocumentStorageIntegrationTest.java.

**Interfaces**

~~~text
DocumentAuditContext(Integer userId, String username, String role, String ipAddress)
DocumentService:
  List<DocumentSummaryResponse> uploadBatch(String studentId, List<String> documentTypes,
      List<MultipartFile> files, DocumentAuditContext audit)
DocumentRepository:
  List<DocumentSummaryResponse> findSummariesByIds(Collection<Integer> ids)
~~~

Controller builds audit context from existing getLogContext and remote address; the service owns the single success log. Reorder queried summaries to input/saved-ID order.

- [ ] Test 0/20/21 files, 10 MiB/+1 per file, 50 MiB/+1 total, empty files, unequal/missing lists, invalid category/extension/name, unknown student, reserved ID category. Validated failures write nothing; use mocked sizes instead of giant byte arrays.
- [ ] MVC: ordered 201, missing fields/parts 400, unknown student 404, denied roles. Preserve single-upload/picture regressions.
- [ ] Real H2 transaction tests: success commits documents + one log; second-file IOException or audit persistence failure leaves neither. Import real proxied services; disable enclosing test transactions with Propagation.NOT_SUPPORTED and query results in a fresh transaction. Isolate/clean test-owned fixtures.
- [ ] RED: .\gradlew.bat test --tests "*DocumentServiceTest" --tests "*DocumentControllerWebMvcTest" --tests "*DocumentStorageIntegrationTest" --console=plain
- [ ] Implement spec §4 using shared validation/save helpers and public @Transactional bulk method; convert file I/O failure to a runtime exception or configure rollbackFor. Resolve once, prevalidate metadata, save in order, audit within the transaction, flush/requery timestamps.
- [ ] Configure max-file-size=10MB, max-request-size=52MB, file-size-threshold=0. Fix oversized/missing multipart 400 responses; preserve other contracts.
- [ ] GREEN: rerun RED command. Real servlet limits are checked in Task 5, not MockMvc.
- [ ] Commit: feat: add atomic multi-document upload.

## Task 3: Four bounded-memory ZIP exports

**Files**
- Create J/dto/registrar/DocumentExportScope.java, J/service/DocumentExportService.java, J/repository/DocumentContentRepository.java, J/exception/EmptyDocumentExportException.java.
- Extend J/repository/DocumentFolderRepository.java; modify J/controller/DocumentController.java and J/exception/GlobalExceptionHandler.java.
- Create T/service/DocumentExportServiceTest.java; extend MVC and DocumentStorageIntegrationTest.

**Interfaces**

~~~text
DocumentExportScope enum: STUDENT, SECTION, UNASSIGNED, BATCH
DocumentFolderRepository.ExportRow(Integer documentId, String studentId,
  String studentNumber, String firstName, String lastName, String sectionCode, String fileName)
DocumentFolderRepository:
  List<ExportRow> findExportRows(DocumentExportScope scope, String key)
DocumentExportService.ExportEntry(Integer documentId, String entryName)
DocumentExportService.PreparedExport(String fileName, List<ExportEntry> entries)
DocumentExportService:
  PreparedExport prepareExport(DocumentExportScope scope, String key)
  void writeZip(PreparedExport prepared, OutputStream output) throws IOException
DocumentContentRepository:
  void copyTo(Integer documentId, OutputStream output) throws IOException
~~~

One parameterized metadata query per scope; predicates/order from spec §5. Preparation checks student/section/batch existence, throws EmptyDocumentExportException for zero documents (dedicated JSON 409 handler), and allocates safe paths. Content query selects one BLOB by ID; missing row fails. Unwrap callback I/O exceptions.

- [ ] Parse actual generated ZIPs with ZipInputStream for all scopes; assert paths/bytes for PDF, HTML, photo. Cover duplicates/preexisting "(1)"/case collisions, unsafe/control/reserved names, long names, and sanitized folder collisions.
- [ ] Test 409 empty/404 missing scope, zero-file members, null/mismatched batch membership, download headers, and parameterized 401/403 across all ZIP routes.
- [ ] Failing output/missing content: close current input/JDBC resources, stop subsequent reads, append no JSON/success message. Verify one audit before streaming; audit failure prevents output.
- [ ] Real H2 tests exercise scope SQL and content-byte copying; mocks cannot prove these queries or MySQL memory behavior.
- [ ] RED: .\gradlew.bat test --tests "*DocumentExportServiceTest" --tests "*DocumentControllerWebMvcTest" --tests "*DocumentStorageIntegrationTest" --console=plain
- [ ] Implement spec §5. Cache allocated section/student folder names by stable IDs so multiple files share their folder. Copy sequentially using JDBC/8 KiB/ZipOutputStream; no entity collection/archive buffer/async context handoff.
- [ ] Controller prepares, audits Requested ZIP export, sets headers, then streams. Handle I/O/runtime streaming failures locally when committed; reset before normal error handling when uncommitted. Call finish only on success. A private ZipOutputStream subclass can expose protected def.end() cleanup in finally without closing/completing failed output; the servlet owns its stream. See the [JDK protected compressor API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/zip/DeflaterOutputStream.html#def).
- [ ] GREEN: rerun RED command; inspect decoded paths/bytes and failure assertions.
- [ ] Commit: feat: stream student section and batch document archives.

## Task 4: Explorer and shared document actions

**Files:** R/static/documents.html, R/static/css/dashboard.css, R/static/js/registrar-documents.js. Scope CSS to the Documents page; retain navbar/account/auth assets and existing modal IDs; bump JS cache query.

**JS contract:** hierarchy; selection {kind,key}; expandedKeys; studentRequestToken; stagedFiles [{file,documentType}]. kind = batch/section/unassigned/student; null batch key is synthetic, never a literal API ID. Internal helpers: selectFolder(kind,key), renderContentPanel(), refreshDocumentViews(), openUploadForStudent(studentId|null). Reuse current table/modal helpers.

- [ ] Browser RED: demonstrate absent explorer, staging, and ZIP actions. Use existing browser tooling; no new JS test dependency.
- [ ] Implement spec §6 tabs/navigation/content/breadcrumbs/states and four export contexts; one table and one set of modals/IDs. Match parent filter nodes with their descendants and retain ancestors of leaf matches.
- [ ] Fetch/index tree once; implement unique selection/request guards and exact student endpoint. Use DOM text/attribute APIs and encoded URLs.
- [ ] Implement context-aware modal reset, chooser/drop, ordered staging/category/removal, limits, and multipart studentId/files/documentTypes. Failed upload preserves staging; successful upload clears it before refresh.
- [ ] Share delegated View/Download/Delete handlers; retain preview/edit/delete confirmation. Refresh tree/content/table, preserving state; adjust DataTables on shown.bs.tab.
- [ ] Browser GREEN: exercise original RED cases plus duplicate-name choice, delayed A->B requests, repeated tab/modal use, table/card actions, saved-but-refresh-failed state, keyboard/mobile. No duplicated requests/handlers or console errors.
- [ ] Commit: feat: add registrar document folder explorer.

## Task 5: Acceptance and one independent review

**Files:** memory-bank/testing.md, activeContext.md, progress.md, changeLog.md; decisions.md if needed. Record actual commands/results and implemented vs unverified work.

- [ ] Full suite: .\gradlew.bat test --console=plain -> BUILD SUCCESSFUL, zero failed tests. Record actual count. After fixes rerun affected checks; repeat full suite only when warranted.
- [ ] Start app against an isolated disposable MySQL 8 database with explicit datasource overrides and schema.sql. Verify target before fixture writes; never reset live AnihanSRMS. If isolation is unavailable, get an approved test target and record the verification blocker.
- [ ] Browser/API acceptance:
  1. Every hierarchy fixture appears once with correct counts/order/badges; duplicate names require selection; zero-file students work.
  2. Chooser/drop/category/remove/reopen/preselection and upload/delete refresh work across both tabs.
  3. Real multipart: combined upload above old 15 MiB cap and at 50 MiB succeeds; >10 MiB/file, >50 MiB combined, and 21 files are rejected. Service/advice errors return useful 400 responses; >52 MiB transport must be rejected without writes, with its actual container response recorded and non-JSON UI fallback verified. Check rollback/audit. MockMvc does not enforce servlet caps.
  4. Inspect all four ZIPs, including batch section/unassigned paths, stored HTML/photo, safe names, empty/missing targets, audit identity/IP.
  5. Export several large incompressible files; inspect actual MySQL heap/allocation observations and valid archive output. The copy-buffer size alone is not memory evidence. Disconnect download; confirm recovery/resource release.
  6. Retain table filters, card/table preview/download/delete, HTML edit link, picture workflow, keyboard/mobile, stale-response guards, retry states, and hidden-tab sizing.
  7. Anonymous/ADMIN/TRAINER cannot access new APIs or Documents page.
- [ ] Clean only test-owned fixtures/downloads; preserve append-only audits. Stop only this task's server and verify port release.
- [ ] Request one independent final review of spec compliance and quality, focusing on scoping, transactions, ZIP safety/memory, and UI regressions. Supply spec/plan, diff/base, and evidence; use selected Sonnet, no automatic escalation.
- [ ] Fix findings and verify affected behavior; record unresolved limitations and update memory/checkboxes.
- [ ] Commit fixes/evidence; use finishing-a-development-branch for the user's integration choice. No auto-merge/push.

**Completion:** Every confirmed requirement has evidence. Missing infrastructure/browser/MySQL checks remain explicit blockers to verified completion, not silently skipped tasks.
