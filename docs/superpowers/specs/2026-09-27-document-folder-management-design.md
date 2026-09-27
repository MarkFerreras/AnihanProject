# Document Management: Split-View Folder Explorer, Bulk Upload & ZIP Export Design

**Date:** 2026-09-27  
**Status:** Approved  
**Target Role:** Registrar (`ROLE_REGISTRAR`)  
**Stack:** Java 25, Spring Boot 4.0.4, Spring Data JPA, MySQL 8 Docker, Bootstrap 5.3, Vanilla JS (ES2024), DataTables 2  

---

## 1. Executive Summary

The Anihan SRMS Document Management module currently presents a flat DataTable listing of uploaded documents on `documents.html`. While functional for search, it lacks hierarchical structure, requires single-file-only uploads, and does not support bulk exporting.

This design introduces:
1. **Split-View Folder Explorer** within a new tab on `documents.html`:
   - Left Sidebar: Interactive tree structured as `Batch` $\rightarrow$ `Section` / `⚠️ Unassigned Students` $\rightarrow$ `Student`.
   - Right Panel: Active content manager showing breadcrumb path, student details banner, document cards, preview modal trigger, single download, and delete.
2. **Multi-File Bulk Upload to a Single Student**:
   - Registrar identifies student by typing **Student Name** or **Student Number** (mandatory).
   - Staging table to assign document types to multiple files before atomic upload.
3. **Streaming Bulk ZIP Export**:
   - Download all documents at the Student, Section, or Batch Unassigned level packaged in a `.zip` archive.
4. **Exclusions**:
   - Document generation (templates/DOCX rendering) is strictly excluded.

---

## 2. Information Architecture & Hierarchy

### 2.1. Tree Structure
```text
▼ 📁 Batch 2026
  ▼ 📁 Section COOK-101 (Commercial Cooking NC II)
      👤 Santos, Maria (2026-001) [3 docs]
      👤 Dela Cruz, Juan (2026-002) [2 docs]
  ▼ 📁 Section BREAD-201 (Bread & Pastry Production NC II)
      👤 Reyes, Ana (2026-003) [4 docs]
  ▼ 📁 ⚠️ Unassigned Students (No Section)
      👤 Gomez, Elena (2026-004) [1 doc] ⚠️ No Section Assigned
▶ 📁 Batch 2025
```

### 2.2. Unassigned Student Handling
- Students in "Enrolling" or "Active" status without an assigned section are grouped under their enrolled `batch_code`.
- Under each Batch, an explicit `📁 ⚠️ Unassigned Students (No Section)` folder lists these students.
- In student lists and profile banners, a prominent warning badge (`⚠️ No Section Assigned`) is rendered.

---

## 3. User Interface & Experience (UI/UX)

### 3.1. Page Layout on `documents.html`
- **View Toggle Tabs:**
  - Tab 1: `📁 Folder Explorer` (Active default)
  - Tab 2: `📋 All Documents Table` (Preserves existing DataTables view for global searches)
- **Top Action Bar:**
  - Fast search/filter input to filter tree nodes.
  - `[⬇️ Export ZIP]` button (context-sensitive: exports active Batch/Section/Student).
  - `[+ Upload Document]` button (opens modal; pre-selects active student).

### 3.2. Left Panel: Folder Tree Navigation (~33% width)
- Renders expandable/collapsible tree items.
- Displays counts for sections, students, and documents.
- Clicking any node marks it active and drives the right-hand panel view.

### 3.3. Right Panel: Active Content View (~67% width)
- **Breadcrumb Header:** Dynamically updates (e.g. `Batch 2026 > Section COOK-101 > Santos, Maria (2026-001)`).
- **When a Student Node is Active:**
  - Profile header card: Student Name, Student Number / ID, Batch, Section (or `⚠️ No Section Assigned` badge), total documents count.
  - Action shortcuts: `[⬇️ Download All as ZIP]` and `[+ Upload Document]`.
  - Document items list:
    - File icon (`.pdf`, `.docx`, `.xlsx`).
    - File name, Document Type badge (*PSA Birth Certificate*, *Good Moral*, *Form 137*, etc.).
    - File size (formatted KB/MB), upload date timestamp.
    - Actions: `[👁️ View]` (iframe preview modal), `[⬇️ Download]` (direct download), `[🗑️ Delete]` (type-to-confirm modal).
    - Empty state: Clean empty graphic with `+ Upload Document for this Student` prompt.
- **When a Section Node is Active:**
  - Section overview card: Section Code, Course name, Batch year, Student count, Document count.
  - `[⬇️ Download Entire Section ZIP]` button.
  - Grid of student cards for one-click navigation to any student.
- **When a Batch Node is Active:**
  - Overview of sections and unassigned students with total document counts.

### 3.4. Enhanced Upload Modal (`#uploadDocumentModal`)
- **Student Identification (Mandatory):**
  - Text input tied to `<datalist>` matching either **Student Name** (`Santos, Maria`) or **Student Number** (`2026-001`).
  - Auto-locked/pre-filled when triggered while a student node is selected in the explorer.
- **Multi-File Selection & Drag-and-Drop:**
  - `<input type="file" multiple accept=".pdf,.docx,.xlsx">`.
  - File Staging List:
    - Lists each staged file with name and size.
    - Per-file Document Type dropdown (defaults to selected default, customizable per file).
    - Remove button `[✕]` for individual staged files.
- **Submission:**
  - Single `Upload (X) Documents` button with upload spinner.

---

## 4. Backend Architecture & API Specifications

### 4.1. Metadata Endpoints (No BLOB loading)

#### 1. Folder Tree Hierarchy
- **Endpoint:** `GET /api/registrar/documents/folders/tree`
- **Security:** `ROLE_REGISTRAR`
- **Response Model:**
  ```json
  [
    {
      "batchCode": "BATCH-2026",
      "batchYear": 2026,
      "sections": [
        {
          "sectionCode": "COOK-101",
          "sectionName": "Commercial Cooking",
          "studentCount": 20,
          "documentCount": 45,
          "students": [
            {
              "studentId": "SR20260001",
              "studentNumber": "2026-001",
              "firstName": "Maria",
              "lastName": "Santos",
              "documentCount": 3
            }
          ]
        }
      ],
      "unassignedStudents": [
        {
          "studentId": "SR20260004",
          "studentNumber": null,
          "firstName": "Elena",
          "lastName": "Gomez",
          "documentCount": 1
        }
      ]
    }
  ]
  ```

#### 2. Student Document List
- **Endpoint:** `GET /api/registrar/documents/student/{studentId}`
- **Response:** `List<DocumentSummaryResponse>` (excludes `content_data`).

### 4.2. Bulk Upload Endpoint

- **Endpoint:** `POST /api/registrar/documents/batch`
- **Consumes:** `multipart/form-data`
- **Parameters:**
  - `studentIdentifier`: String (Student ID or Student Number)
  - `files`: `List<MultipartFile>`
  - `documentTypes`: `List<String>` (matching `files` size)
- **Validation:**
  - Resolves student via `findByStudentId` or `findByStudentNumber`.
  - Allowed extensions: `pdf`, `docx`, `xlsx`.
  - File size: <= 10 MB per file.
- **Transaction:** `@Transactional` saves all documents atomically.
- **Audit Logging:** Writes a single `SystemLog` entry: `"Bulk uploaded X documents for student <Name> (<Number>)"`.

### 4.3. Streaming ZIP Export Endpoints

- All ZIP endpoints write directly to `HttpServletResponse.getOutputStream()` wrapped in `ZipOutputStream` using buffered chunks to prevent heap memory exhaustion.
- **Content-Type:** `application/zip`
- **Content-Disposition:** `attachment; filename="..."`

1. **Student Export:**
   - `GET /api/registrar/documents/export/student/{studentId}`
   - Filename: `Documents_{studentNumberOrId}_{lastName}.zip`
   - Archive entries: `{fileName}` (deduplicated with index if names collide).

2. **Section Export:**
   - `GET /api/registrar/documents/export/section/{sectionCode}`
   - Filename: `Documents_Section_{sectionCode}.zip`
   - Archive entries with student subfolders:
     `{LastName}_{FirstName}_{studentNumberOrId}/{fileName}`.

3. **Batch Unassigned Export:**
   - `GET /api/registrar/documents/export/batch/{batchCode}/unassigned`
   - Filename: `Documents_Batch_{batchCode}_Unassigned.zip`
   - Archive entries with student subfolders:
     `{LastName}_{FirstName}_{studentNumberOrId}/{fileName}`.

---

## 5. Database Schema & Persistence

- **Zero Table Alterations:**
  Existing `documents` table completely fulfills storage needs:
  - `document_id` (PK)
  - `student_id` (FK to `student_records.student_id`)
  - `document_type`, `file_name`, `file_type`, `file_size`
  - `content_data` (`LONGBLOB`)
  - `upload_date` (timestamp)
- **Relationships Utilized:**
  - `StudentRecord.section` -> `Section.sectionCode`
  - `StudentRecord.batch` -> `Batch.batchCode`

---

## 6. Security & Performance Constraints

1. **Access Control:** Restricted strictly to `ROLE_REGISTRAR`.
2. **Memory Efficiency:**
   - BLOB `content_data` is excluded from all tree, listing, and summary queries.
   - ZIP creation streams byte buffers (4KB-8KB buffer) into the servlet output stream; never loads all documents into byte arrays simultaneously.
3. **Auditability:** Every ZIP export and bulk upload logs user identity, remote IP, and affected student/section in `system_logs`.
