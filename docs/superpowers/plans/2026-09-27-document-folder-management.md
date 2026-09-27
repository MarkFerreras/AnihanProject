# Document Management: Split-View Folder Explorer, Bulk Upload & Streaming ZIP Export Implementation Plan

> **For agentic workers (Claude Sonnet 5):**  
> **REQUIRED SUB-SKILL:** Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`. The executor must also use `superpowers:test-driven-development` and `superpowers:verification-before-completion`.  
>  
> ⚠️ **CRITICAL TOKEN EFFICIENCY DIRECTIVE:**  
> **DO NOT READ THE ENTIRE `memory-bank/` FOLDER OR HISTORICAL LOGS.**  
> Only read the first 100 lines of `memory-bank/activeContext.md` and `memory-bank/progress.md` if recent context is needed. This plan is completely self-contained with exact filenames, DTO structures, method logic, test fixtures, and HTML/CSS/JS snippets. Do not waste tokens searching or reading unrelated files.

---

## Overview

**Goal:** Transform the Registrar Document Management module (`documents.html`) from a flat list into a professional **Split-View Folder Explorer** organized by `Batch` $\rightarrow$ `Section` / `⚠️ Unassigned Students` $\rightarrow$ `Student`, add **Multi-File Bulk Upload** to a single student (identified by name or student number), and provide **Streaming Bulk ZIP Exports** at Student, Section, and Batch levels.

**Architecture:** Spring Boot 4.0.4 REST API + Spring Data JPA + MySQL 8 Docker + Bootstrap 5.3 + Vanilla ES2024 JS.  
**Spec Reference:** `docs/superpowers/specs/2026-09-27-document-folder-management-design.md`.

---

## Global Constraints & Safety Rules

1. **Branch Rule:** Work on a dedicated feature branch: `feature/document-folder-management` (created from `main`). **NEVER commit directly to `main`.**
2. **Database Integrity:** No table changes or migrations are needed. The existing `documents` table (`LONGBLOB content_data`), `student_records`, `sections`, and `batches` tables fully support this feature.
3. **Memory Safety:** Tree queries must strictly exclude `content_data`. ZIP export endpoints must stream chunks directly through `ZipOutputStream` to `HttpServletResponse.getOutputStream()`.
4. **Role Security:** All endpoints and pages are strictly restricted to `ROLE_REGISTRAR`.
5. **Audit Logging:** Every ZIP export and batch upload must record an entry in `system_logs` via `SystemLogService.logAction(...)`.
6. **No Generation:** Document generation (templates/DOCX conversion) remains strictly out of scope.

---

## Tasks

### Task 1: Tree Hierarchy DTOs and Metadata Endpoint (Backend)

**Files:**
- Create: `src/main/java/com/example/springboot/dto/registrar/DocumentFolderHierarchyResponse.java`
- Modify: `src/main/java/com/example/springboot/service/DocumentService.java`
- Modify: `src/main/java/com/example/springboot/controller/DocumentController.java`
- Create/Modify: `src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java`
- Create/Modify: `src/test/java/com/example/springboot/service/DocumentServiceTest.java`

- [ ] **Step 1.1: Create DTOs**
Create `DocumentFolderHierarchyResponse.java` containing the hierarchical records:
```java
package com.example.springboot.dto.registrar;

import java.util.List;

public record DocumentFolderHierarchyResponse(
        String batchCode,
        Integer batchYear,
        List<SectionFolderDto> sections,
        List<StudentFolderDto> unassignedStudents
) {
    public record SectionFolderDto(
            String sectionCode,
            String sectionName,
            long studentCount,
            long documentCount,
            List<StudentFolderDto> students
    ) {}

    public record StudentFolderDto(
            String studentId,
            String studentNumber,
            String firstName,
            String lastName,
            long documentCount
    ) {}
}
```

- [ ] **Step 1.2: Write Unit Tests (TDD RED)**
Add test in `DocumentControllerWebMvcTest.java`:
```java
@Test
@WithMockUser(username = "registrar", roles = "REGISTRAR")
void getFolderTreeReturnsHierarchy() throws Exception {
    when(documentService.getFolderHierarchy()).thenReturn(List.of(
            new DocumentFolderHierarchyResponse("BATCH-2026", 2026, List.of(), List.of())
    ));
    mvc.perform(get("/api/registrar/documents/folders/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].batchCode").value("BATCH-2026"));
}
```

- [ ] **Step 1.3: Implement Service and Controller (TDD GREEN)**
In `DocumentService.java`:
Implement `getFolderHierarchy()`:
- Query all Batches ordered by `batchYear DESC`.
- For each batch, query sections using `SectionRepository.findByBatch(...)`.
- For each section, query students via `StudentRecordRepository.findBySectionSectionCode(...)`.
- For unassigned students in that batch, query `StudentRecordRepository.findBySectionIsNullAndStudentStatusIgnoreCaseAndBatchBatchCode("Enrolling", batchCode)` and `"Active"`.
- Count documents per student using `DocumentRepository.countByStudentStudentId(studentId)`.
- Return `List<DocumentFolderHierarchyResponse>`.

In `DocumentController.java`:
```java
@GetMapping("/folders/tree")
public ResponseEntity<List<DocumentFolderHierarchyResponse>> getFolderTree() {
    return ResponseEntity.ok(documentService.getFolderHierarchy());
}
```

- [ ] **Step 1.4: Verify**
Run: `./gradlew test --tests *DocumentControllerWebMvcTest* --tests *DocumentServiceTest*`

---

### Task 2: Multi-File Bulk Upload to Single Student (Backend)

**Files:**
- Modify: `src/main/java/com/example/springboot/service/DocumentService.java`
- Modify: `src/main/java/com/example/springboot/controller/DocumentController.java`
- Modify: `src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java`
- Modify: `src/test/java/com/example/springboot/service/DocumentServiceTest.java`

- [ ] **Step 2.1: Write Tests (TDD RED)**
In `DocumentControllerWebMvcTest.java`:
```java
@Test
@WithMockUser(username = "registrar", roles = "REGISTRAR")
void batchUploadSavesMultipleDocuments() throws Exception {
    MockMultipartFile file1 = new MockMultipartFile("files", "doc1.pdf", "application/pdf", "content1".getBytes());
    MockMultipartFile file2 = new MockMultipartFile("files", "doc2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "content2".getBytes());

    when(documentService.uploadBatch(eq("SR20260001"), any(), any()))
            .thenReturn(List.of(
                    new DocumentSummaryResponse(1, "SR20260001", "Santos", "Maria", "Form 137", "doc1.pdf", "application/pdf", 8, LocalDateTime.now()),
                    new DocumentSummaryResponse(2, "SR20260001", "Santos", "Maria", "PSA Birth Certificate", "doc2.docx", "docx", 8, LocalDateTime.now())
            ));

    mvc.perform(multipart("/api/registrar/documents/batch")
            .file(file1)
            .file(file2)
            .param("studentIdentifier", "SR20260001")
            .param("documentTypes", "Form 137", "PSA Birth Certificate"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.length()").value(2));
}
```

- [ ] **Step 2.2: Implement Service Logic (TDD GREEN)**
In `DocumentService.java`:
```java
@Transactional
public List<DocumentSummaryResponse> uploadBatch(String studentIdentifier, List<String> documentTypes, List<MultipartFile> files) {
    StudentRecord student = resolveStudent(studentIdentifier);
    if (files == null || files.isEmpty()) {
        throw new IllegalArgumentException("At least one file must be provided.");
    }
    if (documentTypes == null || documentTypes.size() != files.size()) {
        throw new IllegalArgumentException("Each file must have a corresponding document type.");
    }

    List<DocumentSummaryResponse> results = new ArrayList<>();
    for (int i = 0; i < files.size(); i++) {
        MultipartFile file = files.get(i);
        String docType = documentTypes.get(i);
        results.add(uploadSingle(student, docType, file));
    }
    return results;
}

private StudentRecord resolveStudent(String identifier) {
    if (!StringUtils.hasText(identifier)) {
        throw new IllegalArgumentException("Student name or student number is required.");
    }
    String trimmed = identifier.trim();
    return studentRecordRepository.findByStudentId(trimmed)
            .or(() -> studentRecordRepository.findByStudentNumber(trimmed))
            .orElseThrow(() -> new IllegalArgumentException("No student found matching: " + trimmed));
}
```

In `DocumentController.java`:
Expose `POST /api/registrar/documents/batch` with `systemLogService.logAction(...)`.

- [ ] **Step 2.3: Verify**
Run: `./gradlew test --tests *DocumentControllerWebMvcTest* --tests *DocumentServiceTest*`

---

### Task 3: Streaming ZIP Export Endpoints (Backend)

**Files:**
- Modify: `src/main/java/com/example/springboot/service/DocumentService.java`
- Modify: `src/main/java/com/example/springboot/controller/DocumentController.java`
- Modify: `src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java`

- [ ] **Step 3.1: Write Tests (TDD RED)**
In `DocumentControllerWebMvcTest.java`:
```java
@Test
@WithMockUser(username = "registrar", roles = "REGISTRAR")
void exportStudentZipStreamsZip() throws Exception {
    doAnswer(inv -> {
        OutputStream os = inv.getArgument(1);
        try (ZipOutputStream zos = new ZipOutputStream(os)) {
            zos.putNextEntry(new ZipEntry("test.pdf"));
            zos.write("dummy".getBytes());
            zos.closeEntry();
        }
        return null;
    }).when(documentService).streamStudentZip(eq("SR20260001"), any());

    mvc.perform(get("/api/registrar/documents/export/student/SR20260001"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"));
}
```

- [ ] **Step 3.2: Implement Streaming Service Methods (TDD GREEN)**
In `DocumentService.java`:
- `streamStudentZip(String studentId, OutputStream os)`:
  - Fetch all `Document` rows for `studentId`.
  - Wrap `os` in `ZipOutputStream`.
  - For each document, add entry `{fileName}` (deduplicating if name collision: `name (1).ext`), stream `content_data` in 4KB chunks, close entry.
- `streamSectionZip(String sectionCode, OutputStream os)`:
  - Fetch all students in `sectionCode`.
  - For each student, add subfolder entries: `{LastName}_{FirstName}_{studentNumberOrId}/{fileName}`.
- `streamBatchUnassignedZip(String batchCode, OutputStream os)`:
  - Fetch unassigned students for `batchCode`.
  - Write entries with student subfolders.

In `DocumentController.java`:
Expose the 3 endpoints:
- `GET /api/registrar/documents/export/student/{studentId}`
- `GET /api/registrar/documents/export/section/{sectionCode}`
- `GET /api/registrar/documents/export/batch/{batchCode}/unassigned`
Set header: `Content-Disposition: attachment; filename="..."`.

- [ ] **Step 3.3: Verify**
Run: `./gradlew test --tests *DocumentControllerWebMvcTest*`

---

### Task 4: Split-View HTML Scaffolding and CSS (Frontend)

**Files:**
- Modify: `src/main/resources/static/documents.html`
- Modify: `src/main/resources/static/css/dashboard.css`

- [ ] **Step 4.1: Add View Switcher Tabs to `documents.html`**
Inside `<main>`, above the table surface card:
```html
<ul class="nav nav-tabs document-view-tabs mb-3" id="docViewTabs" role="tablist">
    <li class="nav-item" role="presentation">
        <button class="nav-link active" id="tab-folder-view" data-bs-toggle="tab"
                data-bs-target="#pane-folder-view" type="button" role="tab">
            📁 Folder Explorer
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-table-view" data-bs-toggle="tab"
                data-bs-target="#pane-table-view" type="button" role="tab">
            📋 All Documents Table
        </button>
    </li>
</ul>
```

- [ ] **Step 4.2: Build Split-View Grid Structure in `#pane-folder-view`**
```html
<div class="tab-content" id="docViewTabContent">
    <div class="tab-pane fade show active" id="pane-folder-view" role="tabpanel">
        <div class="row g-3">
            <!-- Left Sidebar: Folder Tree -->
            <div class="col-lg-4 col-md-5">
                <div class="surface-card folder-tree-card">
                    <div class="surface-card-header p-3">
                        <input type="search" class="form-control form-control-sm" id="treeFilterInput"
                               placeholder="Filter sections or students...">
                    </div>
                    <div class="surface-card-body p-2 folder-tree-scroll" id="folderTreeContainer">
                        <!-- Populated by JS -->
                    </div>
                </div>
            </div>

            <!-- Right Panel: Active Content View -->
            <div class="col-lg-8 col-md-7">
                <div class="surface-card active-folder-card">
                    <div class="surface-card-header d-flex justify-content-between align-items-center flex-wrap gap-2">
                        <nav aria-label="breadcrumb">
                            <ol class="breadcrumb mb-0" id="explorerBreadcrumb">
                                <li class="breadcrumb-item active">Select a section or student</li>
                            </ol>
                        </nav>
                        <div class="d-flex gap-2">
                            <button type="button" class="btn btn-sm btn-surface d-none" id="exportZipBtn">
                                ⬇️ Export ZIP
                            </button>
                            <button type="button" class="btn btn-sm btn-primary" id="openUploadModalBtn"
                                    data-bs-toggle="modal" data-bs-target="#uploadDocumentModal">
                                + Upload Document
                            </button>
                        </div>
                    </div>
                    <div class="surface-card-body" id="explorerContentBody">
                        <!-- Populated dynamically: Batch summary, Section summary, or Student file list -->
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Tab 2: Existing Table View -->
    <div class="tab-pane fade" id="pane-table-view" role="tabpanel">
        <!-- Wrap the existing surface-card here -->
    </div>
</div>
```

- [ ] **Step 4.3: Enhance `#uploadDocumentModal` for Bulk Multi-File Staging**
Update `#uploadDocumentModal` body:
- Student input supports typing Student Name or Student Number, with datalist `#studentsDatalist`.
- File input: `<input type="file" id="uploadDocumentFiles" multiple accept=".pdf,.docx,.xlsx">`.
- Staged file list table `#stagedFilesTable` with individual Document Type dropdowns and delete buttons.

- [ ] **Step 4.4: Add Responsive Split-View Styles to `dashboard.css`**
Add styles for `.folder-tree-card`, `.folder-tree-scroll` (max-height 70vh, overflow-y auto), `.tree-node`, `.tree-badge`, and `.file-item-card`.

---

### Task 5: Client-Side Explorer Logic & Multi-File Upload (Frontend JS)

**Files:**
- Modify: `src/main/resources/static/js/registrar-documents.js`

- [ ] **Step 5.1: Fetch and Render Folder Tree**
In `registrar-documents.js`:
- Fetch `GET /api/registrar/documents/folders/tree`.
- Store hierarchy in memory.
- Render tree with collapsible Batch nodes, Section nodes, and `📁 ⚠️ Unassigned Students` nodes.
- Attach click handlers to select node and trigger `renderContentPanel(nodeType, nodeId)`.

- [ ] **Step 5.2: Render Right-Hand Panel Views**
- `renderStudentView(studentId)`:
  - Fetch `GET /api/registrar/documents?q={studentId}` or student files.
  - Render banner with Student Name, Student Number, Batch, Section (or `⚠️ No Section Assigned` badge).
  - Render file cards with View, Download, and Delete buttons.
  - Set active student in `#uploadStudentId` so upload modal pre-selects this student.
  - Enable `[⬇️ Export ZIP]` pointing to `/api/registrar/documents/export/student/{studentId}`.
- `renderSectionView(sectionCode)`:
  - Render Section overview, list of students in section, and `[⬇️ Export Section ZIP]`.
- `renderBatchUnassignedView(batchCode)`:
  - Render unassigned students list with warning badges and `[⬇️ Export Unassigned ZIP]`.

- [ ] **Step 5.3: Implement Multi-File Staging in `#uploadDocumentModal`**
- On file selection change, populate staging list.
- Allow adjusting document type per file.
- On submit, construct `FormData` with `studentIdentifier`, `files`, and `documentTypes`.
- POST to `/api/registrar/documents/batch`.
- On success: refresh folder tree and active student view, close modal, show success toast/alert.

---

### Task 6: Verification and Live End-to-End Smoke Test

**Files:**
- Modify: `memory-bank/activeContext.md`
- Modify: `memory-bank/progress.md`
- Modify: `memory-bank/changeLog.md`

- [ ] **Step 6.1: Run Full Test Suite**
Execute: `./gradlew clean test`  
Verify: Zero failures, zero errors.

- [ ] **Step 6.2: Live Role-Based Verification against Docker MySQL**
Start app via `./gradlew bootRun` and log in as `registrar`:
1. Navigate to `documents.html`. Confirm Tab 1 ("Folder Explorer") opens by default.
2. Confirm the tree displays Batches, Sections, and Unassigned Students with correct badges.
3. Click a student: confirm breadcrumbs, profile banner, document list, and action buttons.
4. Click `+ Upload Document`: stage 2 files (`.pdf`, `.docx`), verify student is pre-selected, submit, confirm both files appear instantly.
5. Click `⬇️ Export ZIP` on student: verify downloaded `.zip` contains both files.
6. Click a section node: click `⬇️ Export Section ZIP`: verify `.zip` contains student subfolders.
7. Switch to Tab 2 ("All Documents Table"): confirm the existing DataTables search and filters remain fully functional.
8. Stop background server process cleanly.

- [ ] **Step 6.3: Update Memory Bank**
Record the completed feature in `activeContext.md`, `progress.md`, and `changeLog.md`.
