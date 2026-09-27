# Manual Batch Code Assignment Design

**Date:** 2026-09-27 | **Status:** Approved specification ready for plan drafting.  
**Intent:** Enable Registrars to manually assign/encode batch codes with autocomplete and auto-creation, while decoupling student enrollment from automatic batch assignment and protecting section invariants.  
**Stack:** Java 25, Spring Boot 4.0.4, Spring Security 7, Spring Data JPA, MySQL 8, Bootstrap 5.3, local jQuery/DataTables, vanilla JavaScript.

---

## 1. Confirmed Scope & Core Principles

1. **Enrollment Decoupling:** New student applicants submitting details via the public portal ([`student-details.html`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/resources/static/student-details.html)) are no longer auto-assigned a batch. They start with `batch = null` and display as **"No Batch Assigned"**.
2. **Dedicated, Audited Assignment Workflow:** Replicate the proven architectural pattern of `assignStudentNumber`:
   - Dedicated endpoint: `POST /api/registrar/students/{recordId}/batch`.
   - Dedicated modal (`#assignBatchModal`) in [`registrar.html`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/resources/static/registrar.html) and [`student-records.html`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/resources/static/student-records.html).
   - Dedicated atomic transaction with its own explicit `system_logs` audit entry.
3. **Freeform Input with Auto-Creation:** The Registrar can select an existing batch via autocomplete `<datalist>` or type a new batch code. If a new batch code is typed, the backend automatically creates a new `Batch` record with the current calendar year.
4. **Blank Clearing:** Entering blank or null unassigns the student's batch (`batch = null`), provided the student is not assigned to a section.
5. **Section Invariant Protection:** A student assigned to a section cannot have their batch cleared or changed to a batch different from their section's batch. Attempts to do so return HTTP 400 with a clear error message.
6. **Edit Form Read-Only Protection:** `batchCode` in the general student edit form is made read-only so routine biographical/contact edits cannot accidentally mutate the student's batch. `assignBatch` becomes the single authoritative write path.

---

## 2. Data Model, Lifecycle & Invariants

### 2.1 Entity Model (`StudentRecord` and `Batch`)
- `StudentRecord.batch` is `@ManyToOne(optional = true) @JoinColumn(name = "batch_code", nullable = true)`.
- `Batch` entity: `@Id @Column(name = "batch_code", length = 20) private String batchCode;` and `@Column(name = "batch_year", nullable = false) private Short batchYear;`.
- No database DDL migrations are required. The database schema already permits `batch_code NULL` on `student_records`.

### 2.2 Enrollment Lifecycle
- In [`StudentDetailsService.java`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/java/com/example/springboot/service/StudentDetailsService.java), remove the auto-assignment block:
  ```java
  // REMOVE:
  if (record.getBatch() == null) {
      short currentYear = (short) LocalDate.now().getYear();
      com.example.springboot.model.Batch batch = batchRepo.findFirstByBatchYear(currentYear)
              .orElseGet(() -> batchRepo.save(new com.example.springboot.model.Batch("B" + currentYear + "A", currentYear)));
      record.setBatch(batch);
  }
  ```
- Submissions create `StudentRecord` with `batch = null`.

### 2.3 Section Invariant Rule
- `Section` entities belong strictly to a batch (`Section.batch` is non-null).
- If `student.getSection() != null`:
  - Clearing the batch (`normalized == null`) is **rejected** (HTTP 400).
  - Changing the batch to any code other than `student.getSection().getBatch().getBatchCode()` is **rejected** (HTTP 400).
  - Re-assigning or keeping the exact matching batch code is accepted (idempotent 200).
  - Error message: `"Cannot change or clear batch while student is enrolled in section {sectionCode} (Batch {sectionBatch}). Remove the student from the section first."`

---

## 3. API & DTO Contracts

### 3.1 Endpoint
- **URL:** `POST /api/registrar/students/{recordId}/batch`
- **Security:** `ROLE_REGISTRAR` (Spring Security 7 `hasRole('REGISTRAR')`)
- **Headers:** `Content-Type: application/json`

### 3.2 Request DTO
```java
package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.Size;

public record AssignBatchRequest(
    @Size(max = 20, message = "Batch code must not exceed 20 characters.")
    String batchCode
) {}
```

### 3.3 Responses
- **200 OK:** Returns [`StudentRecordDetailsResponse`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/java/com/example/springboot/dto/registrar/StudentRecordDetailsResponse.java) representing the updated student record.
- **400 Bad Request:** Section invariant violation or batch code exceeding 20 characters (`{"message": "..."}`).
- **401 Unauthorized:** Anonymous requests.
- **403 Forbidden:** Authenticated users without `ROLE_REGISTRAR` (e.g. `ROLE_ADMIN`, `ROLE_TRAINER`).
- **404 Not Found:** `recordId` does not match any existing student record (`{"message": "Student record not found: {recordId}"}`).

---

## 4. Service Logic, Auto-Creation & Auditing

### 4.1 Service Implementation ([`RegistrarService.java`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/java/com/example/springboot/service/RegistrarService.java))
```java
@Transactional
public StudentRecordDetailsResponse assignBatch(Integer recordId, String batchCode) {
    StudentRecord record = studentRecordRepository.findById(recordId)
            .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

    String normalized = emptyToNull(batchCode != null ? batchCode.trim() : null);

    // Enforce Section Invariant
    if (record.getSection() != null) {
        String currentSectionBatch = record.getSection().getBatch() != null
                ? record.getSection().getBatch().getBatchCode() : null;
        if (normalized == null || (currentSectionBatch != null && !currentSectionBatch.equalsIgnoreCase(normalized))) {
            throw new IllegalArgumentException(
                    "Cannot change or clear batch while student is enrolled in section "
                            + record.getSection().getSectionCode()
                            + (currentSectionBatch != null ? " (Batch " + currentSectionBatch + ")" : "")
                            + ". Remove the student from the section first.");
        }
    }

    if (normalized != null) {
        // Find existing or auto-create with current calendar year
        Batch batch = batchRepository.findById(normalized)
                .orElseGet(() -> {
                    short currentYear = (short) LocalDate.now().getYear();
                    return batchRepository.save(new Batch(normalized, currentYear));
                });
        record.setBatch(batch);
    } else {
        record.setBatch(null);
    }

    StudentRecord saved = studentRecordRepository.save(record);

    // Audit Logging
    String studentName = (saved.getLastName() != null ? saved.getLastName() : "")
            + (saved.getFirstName() != null ? ", " + saved.getFirstName() : "");
    if (normalized != null) {
        systemLogService.logAction("Batch assigned",
                "Assigned batch " + normalized + " to " + studentName.trim() + " (Record #" + recordId + ")");
    } else {
        systemLogService.logAction("Batch cleared",
                "Cleared batch for " + studentName.trim() + " (Record #" + recordId + ")");
    }

    return buildDetailsResponse(saved);
}
```

### 4.2 Edit Form Locking
In `RegistrarService.updateRecord(recordId, request)`:
- Remove `record.setBatch(resolveBatch(request.batchCode()));`.
- The student's existing batch assignment remains untouched during routine edits.

---

## 5. User Interface & Interaction Flow

### 5.1 Modal Markup
Added to both [`registrar.html`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/resources/static/registrar.html) and [`student-records.html`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/resources/static/student-records.html):
```html
<div class="modal fade" id="assignBatchModal" tabindex="-1"
     aria-labelledby="assignBatchModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="assignBatchModalLabel">Assign Batch</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="mb-3">
                    <label class="form-label">Student</label>
                    <input type="text" class="form-control" id="assignBatchStudentName" readonly>
                </div>
                <div class="mb-2">
                    <label for="assignBatchInput" class="form-label">Batch Code</label>
                    <input type="text" class="form-control" id="assignBatchInput"
                           list="batchLookupList" maxlength="20" autocomplete="off" spellcheck="false"
                           placeholder="Type or pick a batch code (e.g. B2026A)">
                    <datalist id="batchLookupList"></datalist>
                    <div class="form-text">
                        Pick an existing batch or type a new code. Leave blank to clear batch (only allowed if not enrolled in a section).
                    </div>
                </div>
                <div class="alert d-none mt-3" id="assignBatchAlert" role="alert"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-surface-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-surface" id="saveBatchBtn">Save</button>
            </div>
        </div>
    </div>
</div>
```

### 5.2 UI Controller (`registrar-students.js` & `registrar-student-records.js`)
- Action buttons in DataTable render:
  `[Details] [Assign Number] [Assign Batch]`
- Clicking "Assign Batch" opens `#assignBatchModal`:
  - Sets `#assignBatchStudentName` and `#assignBatchInput`.
  - Lazily loads `GET /api/lookup/batches` into `<datalist id="batchLookupList">` if empty.
  - Clears previous alerts.
- Clicking "Save":
  - Disables save button, displays "Saving...".
  - Issues `fetch('/api/registrar/students/' + recordId + '/batch', { method: 'POST', ... })`.
  - **On 200:** Hides modal, executes `dataTable.ajax.reload(null, false)`, updates details card live if open.
  - **On 400:** Displays error message in `#assignBatchAlert`, re-enables button.
  - **On Network failure:** Displays error in alert.

### 5.3 Read-Only Edit Field
- In [`student-records.html`](file:///c:/Users/estro.ESTROPIAA/OneDrive/Documents/AnihanProject/src/main/resources/static/student-records.html) (line 450), `#editBatchCode` is converted to `readonly` with a hint: `"Batch is managed separately via the 'Assign Batch' action."`

---

## 6. Security & Authorization

- Route `POST /api/registrar/students/{recordId}/batch` protected by Spring Security 7.
- Required Authority: `ROLE_REGISTRAR`.
- `ROLE_ADMIN` and `ROLE_TRAINER` are forbidden (HTTP 403).
- Unauthenticated requests are rejected (HTTP 401).
- CSRF validation is applied to all mutating POST requests.
- Parameterized repository queries prevent SQL injection.

---

## 7. Automated Testing & Verification Matrix

### 7.1 Unit & Service Tests (`RegistrarBatchServiceTest`)
1. `assignBatch_newBatchCode_createsBatchAndAssigns`
2. `assignBatch_existingBatchCode_reusesBatch`
3. `assignBatch_blankOrNull_clearsBatch`
4. `assignBatch_studentInDifferentSection_throws400`
5. `assignBatch_studentInSection_clearBatch_throws400`
6. `assignBatch_unknownRecordId_throws404`
7. `updateRecord_editForm_preservesBatch`
8. `assignBatch_auditLogWritten`

### 7.2 Enrollment Decoupling Test (`StudentDetailsServiceTest`)
1. `submitDetails_newEnrollee_batchIsNull`

### 7.3 WebMvc Slice Tests (`RegistrarBatchControllerWebMvcTest`)
1. `assignBatch_roleRegistrar_returns200`
2. `assignBatch_roleAdmin_returns403`
3. `assignBatch_roleTrainer_returns403`
4. `assignBatch_anonymous_returns401`
5. `assignBatch_codeExceeds20Chars_returns400`

### 7.4 Live Database & UI Verification
- Browser smoke test:
  - Submit new student details -> verify student has no batch assigned.
  - Assign batch via modal (existing batch) -> verify table update.
  - Assign batch via modal (new batch) -> verify `batches` table insert and student association.
  - Attempt to change batch on a section-enrolled student -> verify inline 400 warning.
  - Verify general edit form displays batch as read-only.
- Full `./gradlew test` run (all 492 existing + new tests passing).
