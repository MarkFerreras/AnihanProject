# Move the ID Photo from the Student Portal to the Registrar — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete every document-upload feature from the public student enrollment wizard (including the 1x1 ID photo), clean up the UI it leaves behind, and give the Registrar an ID-picture upload on the per-student record screens instead.

**Architecture:** The student portal loses its upload endpoints, `StorageService`, the `StudentUpload` entity/repository and the `UploadRefDto` — the whole filesystem-backed upload path disappears. The Registrar's replacement does **not** resurrect it: the ID picture is stored as a row in the existing `documents` LONGBLOB table through `DocumentService`, so it is covered by the normal database backup, inherits REGISTRAR-only RBAC and `system_logs` auditing, and is removed automatically by the existing per-student document cleanup. A dedicated `uploadIdPicture()` path with its own image whitelist and 2MB cap keeps the general Documents page accepting exactly the pdf/docx/xlsx it accepts today.

**Tech Stack:** Java 25, Spring Boot 4.0.4, Spring Data JPA, MySQL 8, Bootstrap 5.3, jQuery 4.0, vanilla ES2024, Gradle 9.4.1 (Kotlin DSL), JUnit 5 + Mockito + MockMvc.

---

## Decisions Locked In (confirmed with the user)

| # | Decision | Consequence |
|---|----------|-------------|
| 1 | **Full code removal, keep the table.** Delete the student-side upload classes; leave the empty `student_uploads` table in MySQL untouched. | No migration, no destructive DDL. Once no JPA entity maps the table, `ddl-auto=validate` ignores it. It can be dropped later in a routine schema-sync session. |
| 2 | **Registrar Documents module stays.** `DocumentController`/`DocumentService`, `documents.html`, `generate-document.html`, TOR/Form IX generation are untouched and must still pass their tests. | Part B adds alongside, never modifies, the existing upload path. |
| 3 | **Orphan files deleted, `app.storage.root` kept.** | Task 7 removes the 2 stranded files; `application.properties` keeps the property (now unused) and the multipart settings (still needed). |
| 4 | **The ID picture lives inside the student-details screens, not on the Documents page.** | Upload control goes on `student-records.html` (the Registrar's per-student edit form) and the photo is displayed in the details modal on `registrar.html`. The `documents.html` upload modal will *not* offer it. |

### Assumption to confirm before executing

"Inside the student details" is read as the **Registrar's** student-record screens (`student-records.html` edit form + the details modal on `registrar.html`) — **not** the public `student-details.html` wizard, which the original request explicitly strips of the 1x1 upload. If you meant the public wizard should keep the photo, stop and say so: Part A Tasks 5-6 and all of Part B would change.

### Deliberately out of scope

- `StudentDetailsService.startOrResume()` still creates a bare `Enrolling` record when the wizard opens. Its code comment says that exists so uploads have a `student_id` FK to point at; that rationale dies here, but the resume-by-`sessionStorage` behaviour still depends on it. **Leave it alone** — only correct the now-false comment (Task 2, Step 3).
- Dropping `student_uploads`, per decision 1.
- Any change to `documents.html` upload behaviour, `generate-document.html`, or document generation, per decision 2.

---

## Baseline to Beat

Before starting, record the green baseline so regressions are unambiguous:

```bash
./gradlew test
```

Expected at the time of writing: **BUILD SUCCESSFUL — 363 tests, 0 failures, 0 errors.**

Net test movement expected from this plan: `StudentDetailsServiceTest` loses upload stubs (no test count change), and Part B adds **11** new tests -> **374**.

---

## File Structure

### Part A — deleted outright

| File | Why |
|---|---|
| `src/main/java/com/example/springboot/service/StorageService.java` | Filesystem storage for student uploads; nothing else uses it after Task 4. |
| `src/main/java/com/example/springboot/model/StudentUpload.java` | Entity for `student_uploads`; removing it is what makes the leftover table invisible to `ddl-auto=validate`. |
| `src/main/java/com/example/springboot/repository/StudentUploadRepository.java` | Repository for the above. |
| `src/main/java/com/example/springboot/dto/student/UploadRefDto.java` | Upload reference returned to the wizard. |

### Part A — modified

| File | Responsibility after the change |
|---|---|
| `controller/StudentDetailsController.java` | Public enrollment API: `start`, `load`, `submit` only. No multipart, no file serving. |
| `service/StudentDetailsService.java` | Enrollment persistence only. No upload lookup or saving. |
| `dto/student/StudentDetailsResponse.java` | Enrollment payload with no upload references. |
| `service/RegistrarService.java` | Record CRUD; `deleteRecord()` no longer purges filesystem uploads. |
| `static/student-details.html` | Wizard with Religion as the last section of Step 1; no upload CSS. |
| `static/js/student-details.js` | Wizard logic with no file handling. |
| 3 test files | Same coverage, minus the deleted collaborators. |

### Part B — new and modified

| File | Responsibility |
|---|---|
| `service/DocumentService.java` *(modify)* | Gains `ID_PICTURE_TYPE`, an image-only whitelist, a 2MB cap, `uploadIdPicture()` (replace-in-place), `findIdPicture()` and `deleteIdPicture()`. The existing `upload()` is untouched. |
| `repository/DocumentRepository.java` *(modify)* | Gains an exact `(studentId, documentType)` finder — the `q` LIKE search is too loose to key a single photo on. |
| `controller/DocumentController.java` *(modify)* | Gains `POST /id-picture`, `GET /id-picture/{studentId}`, `DELETE /id-picture/{studentId}`, each writing `system_logs`. |
| `static/student-records.html` *(modify)* | Gains an "ID Picture" section: preview, file input, Upload + Remove buttons, inline alert. |
| `static/js/registrar-student-records-edit.js` *(modify)* | Loads, uploads and removes the photo. Kept **out** of `buildPayload()` so a routine record edit can never disturb it — same invariant as the student number. |
| `static/registrar.html` *(modify)* | Details modal gains an ID Picture card. |
| `static/js/registrar-students.js` *(modify)* | Populates that card. |
| `static/js/registrar-documents.js` *(modify)* | Hides "ID Picture" from the *upload* type dropdown while keeping it in the *filter* dropdown (decision 4); lets the view modal preview images. |
| `DocumentServiceTest`, `DocumentControllerWebMvcTest` *(modify)* | 11 new tests. |

---

## Task 0: Branch Safety

**Files:** none

- [ ] **Step 1: Confirm you are not about to work on `main`**

```bash
git branch --show-current
git status --short
```

Expected: prints `main` and an empty status. The project's branch rule forbids committing to `main`, so a branch is mandatory.

- [ ] **Step 2: Create and switch to the feature branch**

```bash
git checkout -b feature/move-id-photo-to-registrar
git branch --show-current
```

Expected: `feature/move-id-photo-to-registrar`

- [ ] **Step 3: Record the baseline**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, 363 tests, 0 failures. **If this is not green, stop and report — do not start on a red baseline.**

---

# PART A — Remove the student-side upload feature

## Task 1: Strip the upload endpoints from the public controller

**Files:**
- Modify: `src/main/java/com/example/springboot/controller/StudentDetailsController.java`

- [ ] **Step 1: Delete both upload endpoints**

Remove the entire `upload(...)` method (the `@PostMapping("/{studentId}/upload")` block) and the entire `serveFile(...)` method (the `@GetMapping("/files/{uploadId}")` block, **including its Javadoc comment** about public enumeration of student PII — that comment describes behaviour that will no longer exist).

- [ ] **Step 2: Delete the now-unused imports**

Remove these import lines:

```java
import org.springframework.web.multipart.MultipartFile;
import com.example.springboot.dto.student.UploadRefDto;
import com.example.springboot.model.StudentUpload;
```

Also remove, **only if nothing else in the file still references them**, the `Resource`, `HttpHeaders`, `MediaType`, `IOException`, `HttpStatus` and `RequestParam` imports. Verify each one before deleting:

```bash
grep -n "Resource\|HttpHeaders\|MediaType\|IOException\|HttpStatus\|RequestParam" src/main/java/com/example/springboot/controller/StudentDetailsController.java
```

Delete an import only when this grep shows no remaining *usage* of that symbol in the body.

- [ ] **Step 3: Remove the injected StorageService**

If the class has a `StorageService` field and constructor parameter, delete both. Confirm:

```bash
grep -n "storageService\|StorageService" src/main/java/com/example/springboot/controller/StudentDetailsController.java
```

Expected after the edit: no output.

- [ ] **Step 4: Verify the endpoint count**

```bash
grep -c "@PostMapping\|@GetMapping" src/main/java/com/example/springboot/controller/StudentDetailsController.java
```

Expected: `3` (start, load, submit). The build will still fail at this point because the service and DTO have not been updated yet — that is expected and fixed in Task 3.

---

## Task 2: Strip uploads from `StudentDetailsService`

**Files:**
- Modify: `src/main/java/com/example/springboot/service/StudentDetailsService.java`

- [ ] **Step 1: Delete the three upload methods**

Remove in full: `saveUpload(String, StudentUpload)`, `getEnrollingUpload(Integer)` (with its Javadoc), and the private `toUploadRef(StudentUpload)`.

- [ ] **Step 2: Remove the repository dependency**

Delete the field, the constructor parameter, and the assignment:

```java
private final StudentUploadRepository uploadRepo;      // delete
        StudentUploadRepository uploadRepo,             // delete from constructor params
        this.uploadRepo = uploadRepo;                   // delete from constructor body
```

- [ ] **Step 3: Correct the now-false comment on `startOrResume`**

The existing comment reads:

```java
    /**
     * Creates a minimal "Enrolling" student record so that file uploads
     * can reference student_id (FK constraint in student_uploads table).
     */
```

Replace it with:

```java
    /**
     * Creates a minimal "Enrolling" student record up front so the wizard can be
     * resumed later from the studentId held in sessionStorage. (This record was
     * originally created early so file uploads had a student_id FK to reference;
     * uploads were removed from the student portal on 2026-09-19, but the resume
     * behaviour still depends on the record existing.)
     */
```

- [ ] **Step 4: Remove the upload lookups from `load(...)`**

Delete these lines (around line 310):

```java
        UploadRefDto idPhoto = uploadRepo.findByStudentIdAndKind(sid, "ID_PHOTO")
                .map(this::toUploadRef).orElse(null);
        UploadRefDto baptCert = uploadRepo.findByStudentIdAndKind(sid, "BAPTISMAL_CERT")
                .map(this::toUploadRef).orElse(null);
```

and in the `new StudentDetailsResponse(...)` call, delete the line that passes them:

```java
                idPhoto, baptCert,
```

- [ ] **Step 5: Delete the unused imports**

```java
import com.example.springboot.dto.student.UploadRefDto;
import com.example.springboot.model.StudentUpload;
import com.example.springboot.repository.StudentUploadRepository;
```

- [ ] **Step 6: Verify nothing upload-shaped remains**

```bash
grep -n "upload\|Upload" src/main/java/com/example/springboot/service/StudentDetailsService.java
```

Expected: no output.

---

## Task 3: Drop the upload fields from the response DTO

**Files:**
- Modify: `src/main/java/com/example/springboot/dto/student/StudentDetailsResponse.java`

- [ ] **Step 1: Delete the two upload components**

Remove this block from the record header:

```java
    // Uploads
    UploadRefDto idPhotoRef,
    UploadRefDto baptismalCertRef,
```

The record goes from 31 to 29 components. The surrounding components (`baptismPlace` above, `ParentDto father` below) are unchanged.

- [ ] **Step 2: Verify**

```bash
grep -n "Upload\|idPhoto\|baptismalCert" src/main/java/com/example/springboot/dto/student/StudentDetailsResponse.java
```

Expected: no output.

---

## Task 4: Detach the Registrar from the upload machinery, then delete the classes

This task must be done in this order — `RegistrarService` is the last production reference to `StorageService`, so the classes cannot be deleted before it is detached.

**Files:**
- Modify: `src/main/java/com/example/springboot/service/RegistrarService.java`
- Delete: `src/main/java/com/example/springboot/service/StorageService.java`
- Delete: `src/main/java/com/example/springboot/model/StudentUpload.java`
- Delete: `src/main/java/com/example/springboot/repository/StudentUploadRepository.java`
- Delete: `src/main/java/com/example/springboot/dto/student/UploadRefDto.java`

- [ ] **Step 1: Remove the upload purge from `deleteRecord()`**

Around line 419 delete these three statements:

```java
        List<StudentUpload> uploads = uploadRepository.findByStudentId(studentId);
        uploads.forEach(storageService::delete);
        ...
        uploadRepository.deleteByStudentId(studentId);
```

Leave every other child-row deletion in `deleteRecord()` exactly as it is — parents, guardians, education, school years, TESDA, OJT, documents and grades must still be purged in FK order.

> Note: the student's ID picture added in Part B lives in the `documents` table, which `deleteRecord()` **already** purges. No replacement cleanup code is needed.

- [ ] **Step 2: Remove the two fields and constructor parameters**

```java
    private final StudentUploadRepository uploadRepository;   // delete
    private final StorageService storageService;              // delete
                            StudentUploadRepository uploadRepository,   // delete from params
                            StorageService storageService,              // delete from params
        this.uploadRepository = uploadRepository;             // delete
        this.storageService = storageService;                 // delete
```

The constructor drops from 12 parameters to 10.

- [ ] **Step 3: Remove the two imports**

```java
import com.example.springboot.model.StudentUpload;
import com.example.springboot.repository.StudentUploadRepository;
```

- [ ] **Step 4: Confirm no production code references the classes any more**

```bash
grep -rn "StorageService\|StudentUpload\|UploadRefDto" src/main/java/
```

Expected: no output. **If anything prints, fix it before proceeding** — the next step deletes the files.

- [ ] **Step 5: Delete the four files**

```bash
git rm src/main/java/com/example/springboot/service/StorageService.java \
       src/main/java/com/example/springboot/model/StudentUpload.java \
       src/main/java/com/example/springboot/repository/StudentUploadRepository.java \
       src/main/java/com/example/springboot/dto/student/UploadRefDto.java
```

- [ ] **Step 6: Compile**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`. Test compilation will still fail — fixed in Task 5.

---

## Task 5: Update the three affected test classes

The tests reference the deleted collaborators as Mockito mocks. This is mechanical mock removal — **no assertion should change**, because no behaviour these tests cover has changed.

**Files:**
- Modify: `src/test/java/com/example/springboot/service/RegistrarBulkLoadTest.java`
- Modify: `src/test/java/com/example/springboot/service/RegistrarStudentNumberServiceTest.java`
- Modify: `src/test/java/com/example/springboot/service/StudentDetailsServiceTest.java`

- [ ] **Step 1: `RegistrarBulkLoadTest` — remove the mocks and the stub**

Delete the import, the two `@Mock` fields, and the stub at line ~190:

```java
import com.example.springboot.repository.StudentUploadRepository;          // delete

    private StudentUploadRepository uploadRepository;                       // delete (with its @Mock)
    private com.example.springboot.service.StorageService storageService;   // delete (with its @Mock)

        when(uploadRepository.findByStudentId("STU-7")).thenReturn(java.util.Collections.emptyList());  // delete
```

- [ ] **Step 2: `RegistrarStudentNumberServiceTest` — remove the mocks**

```java
import com.example.springboot.repository.StudentUploadRepository;   // delete

    @Mock private StudentUploadRepository uploadRepository;          // delete
    @Mock private StorageService storageService;                     // delete
```

- [ ] **Step 3: `StudentDetailsServiceTest` — remove the mock and both stubs**

```java
import com.example.springboot.repository.StudentUploadRepository;   // delete

    @Mock private StudentUploadRepository uploadRepo;                // delete
```

and both occurrences (lines ~81 and ~229) of:

```java
        Mockito.lenient().when(uploadRepo.findByStudentIdAndKind(anyString(), anyString()))
                .thenReturn(Optional.empty());
```

After deleting these, check whether `Mockito`, `anyString` or `Optional` are still used elsewhere in the file before removing their imports:

```bash
grep -n "Mockito\.\|anyString\|Optional" src/test/java/com/example/springboot/service/StudentDetailsServiceTest.java
```

- [ ] **Step 4: Confirm no test references the deleted classes**

```bash
grep -rn "StorageService\|StudentUpload\|UploadRefDto" src/test/java/
```

Expected: no output.

- [ ] **Step 5: Run the full suite**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL — 363 tests, 0 failures, 0 errors`. The count is unchanged: mocks were removed, not tests.

- [ ] **Step 6: Commit the backend removal**

```bash
git add -A
git commit -m "refactor: remove student-portal document upload feature

Deletes the public upload/serve endpoints, StorageService, the StudentUpload
entity and repository, and UploadRefDto. The student_uploads table is left in
place (empty) and is now unmapped, so ddl-auto=validate ignores it.

RegistrarService.deleteRecord() no longer purges filesystem uploads; the
documents table it already purges covers the registrar-side ID picture."
```

---

## Task 6: Remove the upload UI and clean up the wizard

**Files:**
- Modify: `src/main/resources/static/student-details.html`
- Modify: `src/main/resources/static/js/student-details.js`

- [ ] **Step 1: Delete the Document Upload section from the HTML**

At `student-details.html:311` delete this entire block (heading, row and its contents — it is the last section of Step 1, so Religion becomes the closing section):

```html
                <div class="section-heading">Document Upload</div>
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label">ID Photo <span class="text-muted fw-normal">(JPEG/PNG/WebP, max 2
                                MB)</span></label>
                        <input type="file" class="form-control" id="idPhotoFile"
                            accept="image/jpeg,image/png,image/gif,image/webp">
                        <img class="upload-preview" id="idPhotoPreview" alt="ID Photo preview">
                        <div id="idPhotoStatus" class="upload-link mt-1"></div>
                    </div>
                </div>
```

- [ ] **Step 2: Delete the dead CSS**

At `student-details.html:103-118` delete all three now-unused rules:

```css
        .upload-preview {
            max-width: 100%;
            max-height: 160px;
            border-radius: 6px;
            display: none;
            margin-top: 0.5rem;
        }

        .upload-preview.show {
            display: block;
        }

        .upload-link {
            font-size: 0.8rem;
            color: #2d7a3a;
        }
```

- [ ] **Step 3: Bump the JS cache-buster**

The page is served from an air-gapped box where browsers hold stale JS. Find the script tag and bump it:

```bash
grep -n "student-details.js?v=" src/main/resources/static/student-details.html
```

Change `?v=5` to `?v=6` (if the current value differs, increment whatever is there by one).

- [ ] **Step 4: Delete the upload state and setup call in the JS**

In `student-details.js` delete line 6 and the call on line 50:

```javascript
let pendingIdPhoto = null;   // delete (line 6)
    setupFileUploads();       // delete (line 50, inside DOMContentLoaded)
```

- [ ] **Step 5: Delete the deferred-upload block in `submitForm`**

Around line 199 delete:

```javascript
        // Upload any files selected before submit
        if (pendingIdPhoto) {
            submitBtn.textContent = 'Uploading files…';
            await uploadPendingFile(pendingIdPhoto, 'ID_PHOTO', 'idPhotoStatus');
            pendingIdPhoto = null;
        }
```

Leave the `showSubmittedBanner(studentId);` and `sessionStorage.removeItem('studentId');` lines that follow it.

- [ ] **Step 6: Delete `uploadPendingFile`**

Delete the whole `async function uploadPendingFile(file, kind, statusId) { ... }` (lines ~216-235).

- [ ] **Step 7: Delete the idPhotoRef block in `populateForm`**

Around line 329 delete:

```javascript
    if (data.idPhotoRef) {
        document.getElementById('idPhotoStatus').textContent = `Uploaded: ${data.idPhotoRef.originalName}`;
        const img = document.getElementById('idPhotoPreview');
        img.src = `/api/student/files/${data.idPhotoRef.uploadId}`;
        img.classList.add('show');
    }
```

(Check the exact closing lines in the file; delete the complete `if` block.)

- [ ] **Step 8: Delete the file-upload section**

Delete the whole banner comment and both functions (lines ~422-451):

```javascript
// ─── File uploads ─────────────────────────────────────────────────────────────
function setupFileUploads() { ... }

function setupFileInput(inputId, previewId, statusId, kind) { ... }
```

- [ ] **Step 9: Empty the custom-validated ID list**

At line 503, `CUSTOM_VALIDATED_IDS` exists only to clear `is-invalid` from the file input. `STEP_CUSTOM_VALIDATORS` is already `{}`, so nothing sets it. Replace:

```javascript
// IDs that may receive is-invalid from custom validators (uploads + conditional fields)
const CUSTOM_VALIDATED_IDS = ['idPhotoFile'];
```

with:

```javascript
// IDs that may receive is-invalid from custom validators (conditional fields).
// Empty today: STEP_CUSTOM_VALIDATORS holds no validators. Kept so a future
// conditional field has an obvious place to register itself for clearing.
const CUSTOM_VALIDATED_IDS = [];
```

`clearValidation()` needs no change — `[].forEach` is a no-op.

- [ ] **Step 10: Verify every trace is gone**

```bash
grep -nE "upload|Upload|idPhoto|baptCert|pendingIdPhoto|FileReader|FormData" \
  src/main/resources/static/student-details.html \
  src/main/resources/static/js/student-details.js
```

Expected: no output.

- [ ] **Step 11: Check the JS still parses**

```bash
node --check src/main/resources/static/js/student-details.js
```

Expected: no output (success). If `node` is unavailable, rely on the clean-console check in Step 12.

- [ ] **Step 12: Smoke-test the wizard end to end**

Start the app (`./gradlew bootRun`), then in a browser:
1. Open `/student-portal.html`, enter a clearly fake test name, continue to the wizard.
2. **Step 1 ends with the Religion section** — no Document Upload heading, no file input.
3. Browser console shows **no** `ReferenceError` / `TypeError`.
4. Fill the required fields, walk Steps 1->4, Submit.
5. The submitted banner appears with the Reference No.
6. Reopen the wizard and confirm resume-from-`sessionStorage` still works.
7. Delete the test record afterwards from the Registrar page so live data stays clean.

- [ ] **Step 13: Commit**

```bash
git add src/main/resources/static/student-details.html src/main/resources/static/js/student-details.js
git commit -m "feat: remove ID photo upload from the student enrollment wizard

Drops the Document Upload section, its preview/status markup, the dead
.upload-preview/.upload-link CSS, and all file-handling JS. Religion is now
the closing section of Step 1. JS cache-buster bumped."
```

---

## Task 7: Remove the orphan files on disk

**Files:** `uploads/students/SR20260001/` (2 files, no matching DB rows)

- [ ] **Step 1: Confirm they really are orphans**

```bash
find uploads -type f
docker exec mysql-server mysql -uroot -pmy_password -N -e \
  "SELECT COUNT(*) FROM AnihanSRMS.student_uploads;"
```

Expected: 2 file paths listed, and `0` rows in the table. **If the count is not 0, stop** — there is live upload data this plan did not anticipate; report it rather than deleting anything.

- [ ] **Step 2: Delete the stranded tree**

> Destructive. Confirm with the user before running if the Step 1 output differed in any way from what is described above.

```bash
rm -rf uploads/students
ls -la uploads
```

Expected: `uploads/` remains, now empty. Leave the `uploads/` directory itself — `app.storage.root` still points at it per decision 3, and nothing will recreate it now that `StorageService`'s `@PostConstruct` is gone.

- [ ] **Step 3: Note the now-unused property**

In `src/main/resources/application.properties`, line 25, add a comment above `app.storage.root` — do **not** delete the line (decision 3):

```properties
# Unused since 2026-09-19: StorageService was removed with the student-portal
# upload feature. Retained for a future feature that needs a storage location.
app.storage.root=./uploads
```

Leave lines 26-27 (`spring.servlet.multipart.*`) exactly as they are — the Registrar's document upload and the new ID-picture upload both still need them.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.properties
git commit -m "chore: clear orphaned student upload files, mark storage root unused"
```

---

# PART B — Registrar ID picture on the student-details screens

## Task 8: Repository finder for a student's ID picture

**Files:**
- Modify: `src/main/java/com/example/springboot/repository/DocumentRepository.java`
- Modify: `src/main/java/com/example/springboot/service/DocumentService.java`
- Test: `src/test/java/com/example/springboot/service/DocumentServiceTest.java`

- [ ] **Step 1: Write the failing test**

Add to `DocumentServiceTest`:

```java
    @Test
    void findIdPictureReturnsEmptyWhenStudentHasNone() {
        when(documentRepository.findByStudentStudentIdAndDocumentType(
                "SR20260001", DocumentService.ID_PICTURE_TYPE))
                .thenReturn(Optional.empty());

        assertTrue(service.findIdPicture("SR20260001").isEmpty());
    }
```

- [ ] **Step 2: Run it and watch it fail**

```bash
./gradlew test --tests "com.example.springboot.service.DocumentServiceTest.findIdPictureReturnsEmptyWhenStudentHasNone"
```

Expected: **compilation failure** — `findByStudentStudentIdAndDocumentType`, `ID_PICTURE_TYPE` and `findIdPicture` do not exist yet.

- [ ] **Step 3: Add the derived finder**

In `DocumentRepository`, add inside the interface:

```java
    /**
     * Exact lookup for the one document of a given type belonging to a student.
     * Used for the ID picture, where the loose LIKE matching in
     * {@link #searchSummaries} would be wrong — a substring of one student ID
     * can match another student.
     */
    Optional<Document> findByStudentStudentIdAndDocumentType(String studentId, String documentType);
```

Add the imports if the file lacks them:

```java
import java.util.Optional;
import com.example.springboot.model.Document;
```

(`Document` may already be imported for the `JpaRepository<Document, Integer>` declaration — check before adding.)

- [ ] **Step 4: Add `ID_PICTURE_TYPE` and `findIdPicture` to the service**

In `DocumentService`, **above** the existing `DOCUMENT_TYPES` declaration (static initialisation order matters — the constant is referenced by that list in Task 9):

```java
    /** Document type reserved for the student's 1x1 / 2x2 ID picture. */
    public static final String ID_PICTURE_TYPE = "ID Picture (1x1 / 2x2)";
```

And as a public method:

```java
    /** The student's ID picture, if one has been uploaded. */
    public Optional<Document> findIdPicture(String studentId) {
        return documentRepository.findByStudentStudentIdAndDocumentType(studentId, ID_PICTURE_TYPE);
    }
```

Add `import java.util.Optional;` to `DocumentService` if it is not already present.

- [ ] **Step 5: Run the test**

```bash
./gradlew test --tests "com.example.springboot.service.DocumentServiceTest.findIdPictureReturnsEmptyWhenStudentHasNone"
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/springboot/repository/DocumentRepository.java \
        src/main/java/com/example/springboot/service/DocumentService.java \
        src/test/java/com/example/springboot/service/DocumentServiceTest.java
git commit -m "feat: add exact per-student document-type lookup for the ID picture"
```

---

## Task 9: `uploadIdPicture()` — image whitelist, 2MB cap, replace in place

**Files:**
- Modify: `src/main/java/com/example/springboot/service/DocumentService.java`
- Test: `src/test/java/com/example/springboot/service/DocumentServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Add all six to `DocumentServiceTest`:

```java
    @Test
    void idPictureTypeIsAKnownDocumentType() {
        assertTrue(service.getDocumentTypes().contains(DocumentService.ID_PICTURE_TYPE));
    }

    @Test
    void uploadIdPictureSavesJpegAndReturnsSummary() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.findByStudentStudentIdAndDocumentType(
                "SR20260001", DocumentService.ID_PICTURE_TYPE)).thenReturn(Optional.empty());
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        var file = new MockMultipartFile("file", "maria-1x1.jpg", "image/jpeg",
                "jpeg-bytes".getBytes(StandardCharsets.UTF_8));

        DocumentSummaryResponse summary = service.uploadIdPicture("SR20260001", file);

        assertEquals("maria-1x1.jpg", summary.fileName());
        assertEquals("image/jpeg", summary.fileType());
        assertEquals(DocumentService.ID_PICTURE_TYPE, summary.documentType());
    }

    @Test
    void uploadIdPictureReplacesTheExistingPictureInPlace() {
        Document existing = new Document();
        existing.setDocumentId(42);
        existing.setStudent(student);
        existing.setDocumentType(DocumentService.ID_PICTURE_TYPE);
        existing.setFileName("old.png");
        existing.setFileType("image/png");
        existing.setContentData("old".getBytes(StandardCharsets.UTF_8));

        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));
        when(documentRepository.findByStudentStudentIdAndDocumentType(
                "SR20260001", DocumentService.ID_PICTURE_TYPE)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        var file = new MockMultipartFile("file", "new.jpg", "image/jpeg",
                "new-bytes".getBytes(StandardCharsets.UTF_8));

        DocumentSummaryResponse summary = service.uploadIdPicture("SR20260001", file);

        // Same row reused: no second ID picture accumulates for this student.
        assertEquals(42, summary.documentId());
        assertEquals("new.jpg", summary.fileName());
        verify(documentRepository, never()).delete(any(Document.class));
    }

    @Test
    void uploadIdPictureRejectsNonImageFile() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        var file = new MockMultipartFile("file", "scan.pdf", "application/pdf",
                "pdf-bytes".getBytes(StandardCharsets.UTF_8));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.uploadIdPicture("SR20260001", file));
        assertTrue(ex.getMessage().toLowerCase().contains("jpg"));
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void uploadIdPictureRejectsFileOverTwoMegabytes() {
        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(student));

        byte[] tooBig = new byte[2 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("file", "huge.png", "image/png", tooBig);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.uploadIdPicture("SR20260001", file));
        assertTrue(ex.getMessage().contains("2MB"));
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void uploadIdPictureRejectsUnknownStudent() {
        when(studentRecordRepository.findByStudentId("NOPE")).thenReturn(Optional.empty());

        var file = new MockMultipartFile("file", "a.jpg", "image/jpeg", "x".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadIdPicture("NOPE", file));
        verify(documentRepository, never()).save(any(Document.class));
    }
```

> **Look before running:** if `requireStudent` throws `NoSuchElementException` rather than `IllegalArgumentException`, match the expected type used by the existing unknown-student test already in this file.

- [ ] **Step 2: Run them and watch them fail**

```bash
./gradlew test --tests "com.example.springboot.service.DocumentServiceTest"
```

Expected: compilation failure — `uploadIdPicture` does not exist.

- [ ] **Step 3: Register the new document type**

In `DocumentService`, add `ID_PICTURE_TYPE` as the **last** entry of `DOCUMENT_TYPES` so existing ordering is untouched:

```java
    private static final List<String> DOCUMENT_TYPES = List.of(
            "Transcript of Records (TOR)",
            "Form IX - Bread and Pastry Production NC II",
            "Form IX - Cookery NC II",
            "Form IX - Food and Beverage Services NC II",
            "Form 137",
            "PSA Birth Certificate",
            "OJT Report",
            "Certificate of TVET Program",
            "Others",
            ID_PICTURE_TYPE
    );
```

- [ ] **Step 4: Add the image whitelist and cap**

Next to the existing `ALLOWED_EXTENSIONS` — **do not modify that map**, it is what keeps the general Documents page at pdf/docx/xlsx:

```java
    /**
     * Image whitelist for the ID picture only. Deliberately separate from
     * {@link #ALLOWED_EXTENSIONS} so the general Documents page keeps accepting
     * exactly pdf/docx/xlsx and nothing else.
     */
    private static final Map<String, String> ID_PICTURE_EXTENSIONS = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "webp", "image/webp"
    );

    /** ID pictures are small by nature; 2MB matches the limit the old student portal used. */
    private static final long ID_PICTURE_MAX_BYTES = 2L * 1024 * 1024;
```

- [ ] **Step 5: Implement `uploadIdPicture` and `deleteIdPicture`**

```java
    /**
     * Stores (or replaces) a student's 1x1 / 2x2 ID picture as a row in the
     * {@code documents} table. One picture per student: re-uploading updates the
     * existing row in place rather than accumulating copies, mirroring the
     * replace-on-reupload behaviour the student portal used to have.
     */
    public DocumentSummaryResponse uploadIdPicture(String studentId, MultipartFile file) {
        StudentRecord student = requireStudent(studentId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No picture was provided.");
        }
        if (file.getSize() > ID_PICTURE_MAX_BYTES) {
            throw new IllegalArgumentException("ID picture exceeds the 2MB size limit.");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (originalName.isBlank() || originalName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        String mimeType = ID_PICTURE_EXTENSIONS.get(extensionOf(originalName));
        if (mimeType == null) {
            throw new IllegalArgumentException(
                    "Unsupported picture type. Allowed: jpg, jpeg, png, webp");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded picture. Please try again.");
        }

        Document document = documentRepository
                .findByStudentStudentIdAndDocumentType(studentId, ID_PICTURE_TYPE)
                .orElseGet(Document::new);

        document.setStudent(student);
        document.setDocumentType(ID_PICTURE_TYPE);
        document.setFileName(originalName);
        document.setFileType(mimeType);
        document.setFileSize(content.length);
        document.setContentData(content);

        return toSummary(documentRepository.save(document));
    }

    /** Removes a student's ID picture. No-op when none exists. */
    public void deleteIdPicture(String studentId) {
        documentRepository.findByStudentStudentIdAndDocumentType(studentId, ID_PICTURE_TYPE)
                .ifPresent(documentRepository::delete);
    }
```

> `Document.uploadDate` is `insertable=false updatable=false` (a DB default), so a replaced picture keeps its original upload timestamp. Acceptable for a photo; the replacement is audited in `system_logs` either way.

- [ ] **Step 6: Run the tests**

```bash
./gradlew test --tests "com.example.springboot.service.DocumentServiceTest"
```

Expected: PASS, **including every pre-existing test in the class** — the general `upload()` path must be unaffected.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/springboot/service/DocumentService.java \
        src/test/java/com/example/springboot/service/DocumentServiceTest.java
git commit -m "feat: add registrar ID picture upload with image-only whitelist

Stored in the documents table as a dedicated type, replaced in place on
re-upload, capped at 2MB. The general document whitelist is untouched."
```

---

## Task 10: Controller endpoints with audit logging

**Files:**
- Modify: `src/main/java/com/example/springboot/controller/DocumentController.java`
- Test: `src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java`

- [ ] **Step 1: Write the failing tests**

Add to `DocumentControllerWebMvcTest`, following the `@WithMockUser` / `mockMvc` patterns already used in that file:

```java
    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void uploadIdPictureReturns201AndWritesLog() throws Exception {
        var file = new MockMultipartFile("file", "maria-1x1.jpg", "image/jpeg", "bytes".getBytes());
        var summary = new DocumentSummaryResponse(7, "SR20260001", "Dela Cruz", "Maria",
                DocumentService.ID_PICTURE_TYPE, "maria-1x1.jpg", "image/jpeg", 5, null);

        when(documentService.uploadIdPicture(eq("SR20260001"), any())).thenReturn(summary);

        mockMvc.perform(multipart("/api/registrar/documents/id-picture")
                        .file(file)
                        .param("studentId", "SR20260001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("maria-1x1.jpg"));

        verify(systemLogService).logAction(any(), any(), any(),
                contains("ID picture"), any());
    }

    @Test
    @WithMockUser(username = "trainer", roles = "TRAINER")
    void uploadIdPictureForbiddenForTrainer() throws Exception {
        var file = new MockMultipartFile("file", "a.jpg", "image/jpeg", "x".getBytes());

        mockMvc.perform(multipart("/api/registrar/documents/id-picture")
                        .file(file)
                        .param("studentId", "SR20260001"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void getIdPictureReturns404WhenStudentHasNone() throws Exception {
        when(documentService.findIdPicture("SR20260001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/registrar/documents/id-picture/SR20260001"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "registrar", roles = "REGISTRAR")
    void deleteIdPictureReturns204AndWritesLog() throws Exception {
        mockMvc.perform(delete("/api/registrar/documents/id-picture/SR20260001"))
                .andExpect(status().isNoContent());

        verify(documentService).deleteIdPicture("SR20260001");
        verify(systemLogService).logAction(any(), any(), any(),
                contains("Removed ID picture"), any());
    }
```

> **Look before running:** match the existing file's conventions for CSRF (disabled globally in this project) and its import block. Copy from the neighbouring tests rather than guessing.

- [ ] **Step 2: Run them and watch them fail**

```bash
./gradlew test --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"
```

Expected: 404s / compilation errors — the endpoints do not exist.

- [ ] **Step 3: Add the three endpoints**

In `DocumentController`, after the existing `upload(...)` method:

```java
    /**
     * Uploads (or replaces) a student's 1x1 / 2x2 ID picture. Lives under the
     * documents API because the picture is stored as a document row, but it is
     * driven from the student-record screens, not the Documents page.
     */
    @PostMapping("/id-picture")
    public ResponseEntity<DocumentSummaryResponse> uploadIdPicture(
            @RequestParam("studentId") String studentId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        DocumentSummaryResponse saved = documentService.uploadIdPicture(studentId, file);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Uploaded ID picture '" + saved.fileName() + "' for student " + saved.studentId(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** Serves a student's ID picture inline for the record screens. 404 when none. */
    @GetMapping("/id-picture/{studentId}")
    public ResponseEntity<byte[]> idPicture(@PathVariable String studentId) {
        return documentService.findIdPicture(studentId)
                .map(d -> fileResponse(d.getFileName(), d.getFileType(), d.getContentData(), true))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Removes a student's ID picture. */
    @DeleteMapping("/id-picture/{studentId}")
    public ResponseEntity<Void> deleteIdPicture(@PathVariable String studentId,
                                                HttpServletRequest httpRequest) {
        documentService.deleteIdPicture(studentId);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Removed ID picture for student " + studentId,
                httpRequest.getRemoteAddr());

        return ResponseEntity.noContent().build();
    }
```

> **Route ordering:** the existing paths are `/{documentId}/download`, `/{documentId}/view` and `/{documentId}`. None matches a two-segment `/id-picture/{studentId}`, and `/{documentId}` binds an `Integer`, so `id-picture` cannot capture there. No reordering needed — confirmed by the Step 5 run.
>
> If `fileResponse(...)` returns a type that does not line up inside `.map(...)`, assign it to a local variable first rather than changing `fileResponse` — it is shared with `download` and `view`.

- [ ] **Step 4: Confirm the endpoints are RBAC-protected**

No `SecurityConfig` change is needed — `/api/registrar/**` is already REGISTRAR-only. Verify:

```bash
grep -n "api/registrar" src/main/java/com/example/springboot/config/SecurityConfig.java
```

Expected: a matcher restricting `/api/registrar/**` to `hasRole("REGISTRAR")`.

- [ ] **Step 5: Run the tests**

```bash
./gradlew test --tests "com.example.springboot.controller.DocumentControllerWebMvcTest"
```

Expected: PASS, all pre-existing tests in the class included.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/springboot/controller/DocumentController.java \
        src/test/java/com/example/springboot/controller/DocumentControllerWebMvcTest.java
git commit -m "feat: add ID picture upload/serve/delete endpoints with audit logging"
```

---

## Task 11: ID Picture section on the Registrar edit form

**Files:**
- Modify: `src/main/resources/static/student-records.html`
- Modify: `src/main/resources/static/js/registrar-student-records-edit.js`

- [ ] **Step 1: Add the section to the HTML**

Insert immediately **after** the identifiers row (the one holding `editRecordId` / `editStudentId` / `editStudentNumber` / `editStudentStatus`, ending around line 150) and before the Student Name fields, so the photo sits with the student's identity:

```html
                        <div class="section-heading">ID Picture</div>
                        <div class="row g-3 align-items-start mb-3">
                            <div class="col-md-3">
                                <img id="idPicturePreview"
                                     class="id-picture-preview d-none"
                                     alt="Student ID picture">
                                <p id="idPictureEmpty" class="text-muted small mb-0">
                                    No ID picture on file.
                                </p>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label" for="idPictureFile">
                                    Upload 1x1 or 2x2 ID picture
                                    <span class="text-muted fw-normal">(JPG, PNG or WebP, max 2 MB)</span>
                                </label>
                                <input type="file" class="form-control" id="idPictureFile"
                                       accept="image/jpeg,image/png,image/webp">
                                <div class="d-flex gap-2 mt-2">
                                    <button type="button" class="btn btn-surface btn-sm"
                                            id="uploadIdPictureBtn" disabled>Upload Picture</button>
                                    <button type="button" class="btn btn-surface-secondary btn-sm d-none"
                                            id="removeIdPictureBtn">Remove Picture</button>
                                </div>
                                <div id="idPictureAlert" class="alert mt-2 d-none" role="alert"></div>
                            </div>
                        </div>
```

- [ ] **Step 2: Add the preview style**

In the page's `<style>` block:

```css
        .id-picture-preview {
            width: 140px;
            height: 140px;
            object-fit: cover;
            border: 1px solid #d8d8d8;
            border-radius: 6px;
            background: #f7f7f7;
        }
```

- [ ] **Step 3: Bump the JS cache-buster**

At `student-records.html:611` change `registrar-student-records-edit.js?v=4` to `?v=5`.

- [ ] **Step 4: Add the picture logic to the edit JS**

Inside the existing IIFE in `registrar-student-records-edit.js`, add a module-level state variable next to `currentRecordId` (line 6):

```javascript
    let currentStudentId = null;
```

Set it in `populateForm` next to the existing `currentRecordId = r.recordId;` (line 215):

```javascript
        currentStudentId = r.studentId;
```

Then add these functions before the Boot section:

```javascript
    // ----- ID picture -----

    function setPictureAlert(message, type) {
        const el = document.getElementById('idPictureAlert');
        if (!el) return;
        el.textContent = message;
        el.className = 'alert alert-' + type + ' mt-2';
    }

    function hidePictureAlert() {
        const el = document.getElementById('idPictureAlert');
        if (el) el.className = 'alert mt-2 d-none';
    }

    function showIdPicture(hasPicture) {
        const img    = document.getElementById('idPicturePreview');
        const empty  = document.getElementById('idPictureEmpty');
        const remove = document.getElementById('removeIdPictureBtn');
        if (!img || !empty || !remove) return;

        if (hasPicture) {
            // Cache-bust so a freshly replaced picture is not served from cache.
            img.src = '/api/registrar/documents/id-picture/'
                + encodeURIComponent(currentStudentId) + '?t=' + Date.now();
            img.classList.remove('d-none');
            empty.classList.add('d-none');
            remove.classList.remove('d-none');
        } else {
            img.removeAttribute('src');
            img.classList.add('d-none');
            empty.classList.remove('d-none');
            remove.classList.add('d-none');
        }
    }

    async function refreshIdPicture() {
        if (!currentStudentId) return;
        try {
            const response = await fetch(
                '/api/registrar/documents/id-picture/' + encodeURIComponent(currentStudentId),
                { method: 'HEAD', credentials: 'same-origin' });
            showIdPicture(response.ok);
        } catch (error) {
            showIdPicture(false);
        }
    }

    async function uploadIdPicture() {
        const input = document.getElementById('idPictureFile');
        if (!input || !input.files.length || !currentStudentId) return;

        hidePictureAlert();
        const button = document.getElementById('uploadIdPictureBtn');
        button.disabled = true;
        button.textContent = 'Uploading...';

        const formData = new FormData();
        formData.append('studentId', currentStudentId);
        formData.append('file', input.files[0]);

        try {
            const response = await fetch('/api/registrar/documents/id-picture', {
                method: 'POST',
                credentials: 'same-origin',
                body: formData
            });
            const data = await response.json().catch(function () { return {}; });

            if (!response.ok) {
                setPictureAlert(data.message || 'Could not upload the ID picture.', 'danger');
                return;
            }

            input.value = '';
            showIdPicture(true);
            setPictureAlert('ID picture saved.', 'success');
        } catch (error) {
            setPictureAlert('Could not upload the ID picture. Check your connection.', 'danger');
        } finally {
            button.disabled = true;   // re-armed by the change listener on a new selection
            button.textContent = 'Upload Picture';
        }
    }

    async function removeIdPicture() {
        if (!currentStudentId) return;
        hidePictureAlert();
        try {
            const response = await fetch(
                '/api/registrar/documents/id-picture/' + encodeURIComponent(currentStudentId),
                { method: 'DELETE', credentials: 'same-origin' });

            if (!response.ok) {
                setPictureAlert('Could not remove the ID picture.', 'danger');
                return;
            }
            showIdPicture(false);
            setPictureAlert('ID picture removed.', 'success');
        } catch (error) {
            setPictureAlert('Could not remove the ID picture. Check your connection.', 'danger');
        }
    }

    function setupIdPicture() {
        const input  = document.getElementById('idPictureFile');
        const upload = document.getElementById('uploadIdPictureBtn');
        const remove = document.getElementById('removeIdPictureBtn');
        if (!input || !upload || !remove) return;

        input.addEventListener('change', function () {
            upload.disabled = !input.files.length;
            hidePictureAlert();
        });
        upload.addEventListener('click', uploadIdPicture);
        remove.addEventListener('click', removeIdPicture);
    }
```

- [ ] **Step 5: Wire it into the boot sequence**

In the `DOMContentLoaded` handler, after the existing `setupSchoolYearHandlers();` and **before** `setupDirtyTracking();`:

```javascript
        setupIdPicture();
        await refreshIdPicture();
```

> **Why the picture is not part of Save Changes.** `setupDirtyTracking()` attaches listeners to every field in the form, so selecting a file marks the form dirty. That is fine — but the picture is written by its own button and is deliberately **absent from `buildPayload()`**, so clicking Save Changes never sends it. This mirrors the student-number invariant: one deliberate, separately audited write path that a routine edit cannot disturb.

- [ ] **Step 6: Confirm the picture is absent from the save payload**

```bash
grep -n "idPicture" src/main/resources/static/js/registrar-student-records-edit.js | grep -i "payload"
```

Expected: no output.

- [ ] **Step 7: Check the JS parses**

```bash
node --check src/main/resources/static/js/registrar-student-records-edit.js
```

Expected: no output.

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/static/student-records.html \
        src/main/resources/static/js/registrar-student-records-edit.js
git commit -m "feat: ID picture upload on the registrar student record edit form"
```

---

## Task 12: Show the picture in the details modal; keep it off the Documents upload dropdown

**Files:**
- Modify: `src/main/resources/static/registrar.html`
- Modify: `src/main/resources/static/js/registrar-students.js`
- Modify: `src/main/resources/static/js/registrar-documents.js`
- Modify: `src/main/resources/static/documents.html`

- [ ] **Step 1: Add the picture card to the details modal**

In `registrar.html`, immediately **before** the `<div class="detail-grid">` at line 197:

```html
                    <div class="text-center mb-3">
                        <img id="detailsIdPicture" class="id-picture-preview d-none"
                             alt="Student ID picture">
                        <p id="detailsIdPictureEmpty" class="text-muted small mb-0">
                            No ID picture on file.
                        </p>
                    </div>
```

- [ ] **Step 2: Add the matching style**

In `registrar.html`'s `<style>` block (same rule as the edit page, so the two screens match):

```css
        .id-picture-preview {
            width: 140px;
            height: 140px;
            object-fit: cover;
            border: 1px solid #d8d8d8;
            border-radius: 6px;
            background: #f7f7f7;
        }
```

- [ ] **Step 3: Populate it in `loadRecordDetails`**

First confirm the property name the response actually carries:

```bash
grep -n "detailsStudentId" src/main/resources/static/js/registrar-students.js
```

Then, in `registrar-students.js` inside `loadRecordDetails` where the other `details*` fields are filled, add — reusing the same source field that line revealed:

```javascript
        // ID picture: HEAD first so a missing picture never renders a broken image.
        const idPictureImg   = document.getElementById('detailsIdPicture');
        const idPictureEmpty = document.getElementById('detailsIdPictureEmpty');
        if (idPictureImg && idPictureEmpty && data.studentId) {
            const pictureUrl = '/api/registrar/documents/id-picture/'
                + encodeURIComponent(data.studentId);
            fetch(pictureUrl, { method: 'HEAD', credentials: 'same-origin' })
                .then(function (response) {
                    if (response.ok) {
                        idPictureImg.src = pictureUrl + '?t=' + Date.now();
                        idPictureImg.classList.remove('d-none');
                        idPictureEmpty.classList.add('d-none');
                    } else {
                        idPictureImg.removeAttribute('src');
                        idPictureImg.classList.add('d-none');
                        idPictureEmpty.classList.remove('d-none');
                    }
                })
                .catch(function () {
                    idPictureImg.classList.add('d-none');
                    idPictureEmpty.classList.remove('d-none');
                });
        }
```

- [ ] **Step 4: Bump the cache-buster**

At `registrar.html:489` change `registrar-students.js?v=4` to `?v=5`.

- [ ] **Step 5: Keep "ID Picture" out of the Documents upload dropdown**

Decision 4: the picture is uploaded from the student-record screens, not the Documents page. In `registrar-documents.js`, `loadDocumentTypes()` (line 126), change the loop so the type still appears in the **filter** (the Registrar should be able to find pictures) but not in the **upload** selector:

```javascript
                types.forEach(function (t) {
                    filterSel.append('<option value="' + escapeHtml(t) + '">' + escapeHtml(t) + '</option>');
                    // The ID picture is uploaded from the student record screens
                    // (student-records.html), not from this page.
                    if (t !== 'ID Picture (1x1 / 2x2)') {
                        uploadSel.append('<option value="' + escapeHtml(t) + '">' + escapeHtml(t) + '</option>');
                    }
                });
```

> This literal must stay in step with `DocumentService.ID_PICTURE_TYPE` — a JS file cannot import the Java constant. If one changes, change both.

- [ ] **Step 6: Let the Documents view modal preview images**

At `registrar-documents.js:273` the inline-preview test is PDF/HTML only, so an ID picture row would be download-only. Widen it:

```javascript
            const previewable = mime === 'application/pdf'
                || mime.indexOf('text/html') === 0
                || mime.indexOf('image/') === 0;
```

Leave the `editable` test on line 263 alone — only `text/html` documents are editable.

- [ ] **Step 7: Bump the documents cache-buster**

```bash
grep -n "registrar-documents.js?v=" src/main/resources/static/documents.html
```

Increment the version by one.

- [ ] **Step 8: Check both files parse**

```bash
node --check src/main/resources/static/js/registrar-students.js
node --check src/main/resources/static/js/registrar-documents.js
```

Expected: no output.

- [ ] **Step 9: Commit**

```bash
git add src/main/resources/static/registrar.html \
        src/main/resources/static/js/registrar-students.js \
        src/main/resources/static/js/registrar-documents.js \
        src/main/resources/static/documents.html
git commit -m "feat: show ID picture in the student details modal

Keeps the ID Picture type out of the Documents page upload dropdown (it is
uploaded from the student record screens) while leaving it in the filter, and
lets the view modal preview images inline."
```

---

# PART C — Verification

## Task 13: Full suite + live schema validation

**Files:** none

- [ ] **Step 1: Run the full suite**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL — 374 tests, 0 failures, 0 errors` (363 baseline + 11 new).

- [ ] **Step 2: Validate the entities against the live database**

This is the check the H2-backed suite **cannot** perform, and the one that caught two real bugs in the 2026-09-19 session. `StudentUpload` has been deleted while `student_uploads` still exists in MySQL — this proves an unmapped leftover table does not break startup.

```bash
./gradlew bootRun --args='--spring.jpa.hibernate.ddl-auto=validate'
```

Expected: `Started SpringbootApplication in N seconds`, and **zero** `Schema-validation` or `SchemaManagementException` lines. Stop the app once it reports started.

- [ ] **Step 3: Confirm the leftover table is genuinely unmapped**

```bash
docker exec mysql-server mysql -uroot -pmy_password -N -e \
  "SELECT COUNT(*) FROM information_schema.TABLES
   WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='student_uploads';"
grep -rn "student_uploads" src/main/java/
```

Expected: `1` from MySQL (the table is still there, per decision 1) and **no output** from the grep (nothing maps it).

---

## Task 14: Live browser verification

**Files:** none — manual verification against the running app and real MySQL.

- [ ] **Step 1: Start the app**

```bash
./gradlew bootRun
```

- [ ] **Step 2: Student wizard — the feature is gone**

1. `/student-portal.html` -> enter a fake test name -> wizard.
2. Step 1 ends at Religion. **No Document Upload heading, no file input.**
3. Console clean.
4. Complete and submit; the submitted banner shows the Reference No.

- [ ] **Step 3: The dead endpoints really are dead**

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/student/SR20260001/upload
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/student/files/1
```

Expected: `404` or `405` for both — **not** `200`, and not `500`.

- [ ] **Step 4: Registrar ID picture — happy path**

1. Log in as `registrar`, open a student's record for editing.
2. The ID Picture section shows "No ID picture on file."
3. Choose a JPG under 2MB -> Upload Picture -> the preview appears, "ID picture saved."
4. Reload the page -> the picture is still shown.
5. Upload a **different** picture -> preview updates.
6. Confirm it replaced rather than accumulated:

```bash
docker exec mysql-server mysql -uroot -pmy_password -N -e \
  "SELECT COUNT(*) FROM AnihanSRMS.documents
   WHERE document_type='ID Picture (1x1 / 2x2)';"
```

Expected: `1`

- [ ] **Step 5: Registrar ID picture — rejection paths**

1. Try a `.pdf` -> inline error naming the allowed types; nothing is saved.
2. Try an image over 2MB -> "exceeds the 2MB size limit".

- [ ] **Step 6: Save Changes must not disturb the picture**

Edit an unrelated field (e.g. Contact No), click Save Changes, reload. The picture is still there. This is the regression the student-number work established as an invariant.

- [ ] **Step 7: Details modal**

Open the details modal for that student from the registrar home table — the picture renders at the top. Open the modal for a student **without** a picture — it shows "No ID picture on file." and no broken-image icon.

- [ ] **Step 8: The Registrar's existing document features still work (the user's explicit constraint)**

1. `/documents.html` -> upload a **PDF** against any normal type -> succeeds.
2. Upload a **docx** and an **xlsx** -> both succeed.
3. The upload modal's type dropdown does **not** list "ID Picture (1x1 / 2x2)".
4. The filter dropdown **does** list it, and filtering by it finds the picture from Step 4.
5. View the PDF -> still previews inline. View the ID picture -> previews inline as an image.
6. Download a document -> bytes intact.
7. Generate a TOR from `/generate-document.html` -> renders and saves as before.

- [ ] **Step 9: Remove flow**

Back on the edit form, click Remove Picture -> the preview disappears and the empty message returns. Re-check the DB count -> `0`.

- [ ] **Step 10: Audit trail**

Open `/logs.html` and confirm rows for "Uploaded ID picture …" and "Removed ID picture …".

- [ ] **Step 11: Clean up test data**

Delete the test student record created in Step 2 and any leftover test documents, so live data returns to its pre-session state.

---

## Task 15: Memory-bank updates (mandatory per CLAUDE.md)

**Files:**
- Modify: `memory-bank/activeContext.md`, `progress.md`, `changeLog.md`, `decisions.md`, `testing.md`
- Modify: `CLAUDE.md`

- [ ] **Step 1: `decisions.md` — add two dated entries at the top**

Entry 1: *ID picture stored in the `documents` table, not a filesystem upload.* Rationale: covered by the existing DB backup on an air-gapped box where filesystem and DB backups can diverge; inherits REGISTRAR-only RBAC, `system_logs` auditing, and the per-student purge already in `deleteRecord()`. Alternative rejected: keeping `StorageService` and `student_uploads` for the Registrar — would have preserved a second, parallel storage mechanism for one file per student.

Entry 2: *`student_uploads` left in place, unmapped.* Rationale: the table is empty; dropping it is destructive DDL with no benefit, and an unmapped table is invisible to `ddl-auto=validate` (proven by Task 13 Step 2). Flag it for a future routine schema-sync session.

- [ ] **Step 2: `changeLog.md` — add a dated section**

Follow the existing table format: Task, Files Created, Files Deleted, Files Modified, Design Decisions, Verification. Record the 4 deleted classes explicitly, and that **no migration was applied and the live schema is unchanged**.

- [ ] **Step 3: `progress.md` — add a "Recent Sessions" entry**

Record the test movement (363 -> 374), the `ddl-auto=validate` PASS, the browser verification result, and the branch name.

- [ ] **Step 4: `activeContext.md` — replace the "Current Phase" block**

New phase line, active branch `feature/move-id-photo-to-registrar`, and an Open Items list carrying: PR to `main` (user approval required) and "drop `student_uploads` in a future schema-sync session".

- [ ] **Step 5: `testing.md` — update the suite table**

Add the new tests to the `DocumentServiceTest` (17 -> 24) and `DocumentControllerWebMvcTest` (17 -> 21) rows, and update the "Latest full-suite result" line to 374.

- [ ] **Step 6: `CLAUDE.md` — correct two now-stale statements**

```bash
grep -n "StudentUpload\|19 JPA entities" CLAUDE.md
```

1. Add a short Key Conventions note: the student portal has **no** file upload; the student's ID picture is uploaded by the Registrar from `student-records.html` and stored in `documents` under the type `ID Picture (1x1 / 2x2)`.
2. The model list says "19 JPA entities" and includes `StudentUpload`. Correct the count and remove the entry.

- [ ] **Step 7: Commit**

```bash
git add memory-bank/ CLAUDE.md
git commit -m "docs: record ID photo move from student portal to registrar"
```

- [ ] **Step 8: Final state check**

```bash
git log --oneline feature/move-id-photo-to-registrar ^main
./gradlew test
```

Expected: the session's commits listed, and `BUILD SUCCESSFUL — 374 tests, 0 failures`.

**Do not merge or push to `main`** — the project rule requires user approval for the PR.

---

## Self-Review Notes

Checked while writing:

- **Spec coverage.** "Remove all document upload features for the student enrollment dashboard" -> Tasks 1-6. "Includes the uploading of 1x1 picture" -> Task 6 Steps 1-2 (UI) and Tasks 1-5 (backend). "Clean the UI for the dashboard" -> Task 6 Steps 1-2 and 9 (dead CSS and the dead validator list, not only the visible controls). "Make sure uploading of documents and also picture still works on the Registrar side" -> Tasks 8-12 build the picture path; Task 14 Step 8 verifies the existing document path is undamaged. "1x1 or 2x2 inside the student details, not outside" -> Task 11 (edit form), Task 12 Steps 1-3 (details modal), Task 12 Step 5 (kept off the Documents page).
- **Name consistency.** `ID_PICTURE_TYPE` = `"ID Picture (1x1 / 2x2)"` is used identically in `DocumentService`, both test classes, and the string literal in `registrar-documents.js` Step 5 (flagged inline as a paired edit). `findIdPicture` / `uploadIdPicture` / `deleteIdPicture` are spelled the same in service, controller and tests. Element IDs `idPicturePreview` / `idPictureEmpty` / `idPictureFile` / `uploadIdPictureBtn` / `removeIdPictureBtn` / `idPictureAlert` match between Task 11 Steps 1 and 4; the modal uses the distinct `detailsIdPicture` / `detailsIdPictureEmpty`.
- **Ordering hazards.** `ID_PICTURE_TYPE` must be declared before `DOCUMENT_TYPES` (Java static init) — called out in Task 8 Step 4. `RegistrarService` must be detached before the classes are deleted — Task 4 Steps 1-5, with a blocking grep at Step 4.
- **Points where the executing engineer must look rather than assume** (each flagged inline): the exception type thrown by `requireStudent`; the CSRF convention in `DocumentControllerWebMvcTest`; the return type of `fileResponse(...)`; the student-ID property name in `registrar-students.js`; which imports are still in use before deleting them.
