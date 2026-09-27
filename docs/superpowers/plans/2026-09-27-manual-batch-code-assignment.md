# Manual Batch Code Assignment Implementation Plan

> **For agentic workers:** Use `superpowers:executing-plans`, `superpowers:test-driven-development`, and `superpowers:verification-before-completion`; track checkboxes.
> **User-selected workflow:** One sequential Claude Sonnet 5 implementer on a dedicated feature branch, then one independent final review. This plan was brainstormed and drafted on `main` as a documentation-only preparation. Execution must create and switch to `feature/manual-batch-code-assignment`.

**Goal:** Decouple student enrollment from automatic batch assignment, provide a dedicated, audited Registrar manual batch assignment workflow with datalist autocomplete and auto-creation, and enforce strict section invariants.  
**Architecture:** Dedicated Spring Boot `@PostMapping` endpoint + `@Transactional` service write path mirroring `assignStudentNumber`; read-only edit form protection; Bootstrap 5 modal with dynamic lookup datalist.  
**Stack:** Java 25, Spring Boot 4.0.4, Spring Security 7, Spring Data JPA, MySQL 8, Bootstrap 5.3, local jQuery/DataTables.  
**Spec:** [Manual Batch Code Assignment Design](../specs/2026-09-27-manual-batch-code-assignment-design.md); read once, use as the behavior/constant source.  
**Status:** Design approved; implementation plan ready for execution by Claude Sonnet 5 on branch `feature/manual-batch-code-assignment`.

---

## Global Constraints

- `ROLE_REGISTRAR` only; strict 403 for `ROLE_ADMIN` and `ROLE_TRAINER`, 401 for anonymous.
- No schema DDL changes required (database schema already defines `student_records.batch_code` as nullable).
- Decouple enrollment: applicants submitting via `student-details.html` receive `batch = null` ("No Batch Assigned").
- Invariant safety: Students assigned to a section cannot have their batch changed or cleared without first being removed from that section (returns HTTP 400).
- Edit form protection: `batchCode` in general student edit form must be read-only so routine biographical updates cannot mutate batch. `assignBatch` is the sole write path.
- Auto-creation: Entering a non-existing batch code automatically saves a new `Batch` record with the current calendar year.
- Preserve existing table, pagination, search, document folder explorer, and section assignment workflows.
- Execution branch: Create and checkout `feature/manual-batch-code-assignment` before implementing. Never commit or push directly to `main`.

---

## Review Focus

1. **Enrollment decoupling:** Verify that `StudentDetailsService.applyDetails()` leaves `record.batch == null` on submission.
2. **Section invariant enforcement:** Verify that changing or clearing a batch on a student enrolled in a section fails with HTTP 400 and an actionable error message.
3. **Auto-creation correctness:** Verify that entering a brand-new batch code persists a new `batches` row with `batch_year = currentYear` and links to the student in one transaction.
4. **Edit form isolation:** Verify that `RegistrarService.updateRecord()` does not overwrite the student's batch even if a batch code is passed in the update request.
5. **Audit accountability:** Verify that `system_logs` receives exactly one row per batch assignment/clear with the student's name, record ID, and batch code.
6. **UI reactivity:** Verify that saving from `#assignBatchModal` updates the DataTable row immediately without page reload, and displays inline alert banners on 400 conflicts.

---

## Paths and Baseline

- **Java source prefix:** `src/main/java/com/example/springboot/`
- **Test source prefix:** `src/test/java/com/example/springboot/`
- **Static assets prefix:** `src/main/resources/static/`

**Key Anchors:**
- Service: `service/RegistrarService.java`, `service/StudentDetailsService.java`
- Controller: `controller/RegistrarController.java`, `controller/LookupController.java`
- Model: `model/StudentRecord.java`, `model/Batch.java`, `model/Section.java`
- HTML: `static/registrar.html`, `static/student-records.html`
- JS: `static/js/registrar-students.js`, `static/js/registrar-student-records.js`, `static/js/registrar-student-records-edit.js`

**Baseline Check:**
- [ ] Confirm git status clean on `main`.
- [ ] Create and switch to feature branch: `git checkout -b feature/manual-batch-code-assignment`.
- [ ] Baseline test run:
  ```powershell
  .\gradlew.bat test --tests "*RegistrarServiceTest" --tests "*StudentDetailsServiceTest" --console=plain
  ```
  Expect `BUILD SUCCESSFUL`.

---

## Task 1: Enrollment Decoupling & Batch Assignment DTO

**Files:**
- Create `src/main/java/com/example/springboot/dto/registrar/AssignBatchRequest.java`
- Modify `src/main/java/com/example/springboot/service/StudentDetailsService.java`
- Create `src/test/java/com/example/springboot/service/StudentDetailsServiceTest.java` (or extend existing)

**Interfaces:**
```java
package com.example.springboot.dto.registrar;

import jakarta.validation.constraints.Size;

public record AssignBatchRequest(
    @Size(max = 20, message = "Batch code must not exceed 20 characters.")
    String batchCode
) {}
```

- [ ] Write unit test: `submitDetails_newEnrollee_batchIsNull` verifying that submitted details no longer auto-assign `B<Year>A` and leave `record.getBatch() == null`.
- [ ] RED: Run `StudentDetailsServiceTest` expecting failure.
- [ ] Modify `StudentDetailsService.java`: Remove lines 117–124 that auto-assigned `B<Year>A`.
- [ ] GREEN: Rerun `StudentDetailsServiceTest`.
- [ ] Commit: `feat: decouple student enrollment from auto batch assignment`.

---

## Task 2: Service Layer Manual Batch Assignment & Invariant Guard

**Files:**
- Modify `src/main/java/com/example/springboot/service/RegistrarService.java`
- Create `src/test/java/com/example/springboot/service/RegistrarBatchServiceTest.java`

**Interfaces:**
```java
public StudentRecordDetailsResponse assignBatch(Integer recordId, String batchCode)
```

- [ ] Write unit tests in `RegistrarBatchServiceTest`:
  - `assignBatch_newBatchCode_createsBatchAndAssigns` (asserts new `Batch` saved with current year, student linked, audit row written).
  - `assignBatch_existingBatchCode_reusesBatch` (asserts no duplicate `Batch` created).
  - `assignBatch_blankOrNull_clearsBatch` (sets `record.batch = null`, writes `"Batch cleared"` audit row).
  - `assignBatch_studentInDifferentSection_throws400` (throws `IllegalArgumentException` when batch does not match section's batch).
  - `assignBatch_studentInSection_clearBatch_throws400` (throws `IllegalArgumentException` when clearing batch for enrolled student).
  - `assignBatch_studentInMatchingSection_succeeds` (idempotent assign allowed when batch matches section).
  - `assignBatch_unknownRecordId_throws404` (throws `NoSuchElementException`).
  - `updateRecord_editForm_preservesBatch` (asserts calling `updateRecord` ignores or preserves student batch).
- [ ] RED: Run `RegistrarBatchServiceTest` expecting failures.
- [ ] Implement in `RegistrarService.java`:
  - Implement `assignBatch()` method per spec §4.1.
  - In `updateRecord()`, remove `record.setBatch(...)` so general edits cannot modify the student's batch.
- [ ] GREEN: Rerun `RegistrarBatchServiceTest`.
- [ ] Commit: `feat: implement registrar batch assignment service and invariant guard`.

---

## Task 3: Controller Layer Endpoint & Authorization Slice Tests

**Files:**
- Modify `src/main/java/com/example/springboot/controller/RegistrarController.java`
- Create `src/test/java/com/example/springboot/controller/RegistrarBatchControllerWebMvcTest.java`

**Endpoint Contract:**
`POST /api/registrar/students/{recordId}/batch`
Body: `{"batchCode": "B2026A"}`
Response: `200 OK` with `StudentRecordDetailsResponse`

- [ ] Write MockMvc slice tests in `RegistrarBatchControllerWebMvcTest`:
  - `assignBatch_roleRegistrar_returns200`
  - `assignBatch_roleAdmin_returns403`
  - `assignBatch_roleTrainer_returns403`
  - `assignBatch_anonymous_returns401`
  - `assignBatch_codeExceeds20Chars_returns400`
  - `assignBatch_notFound_returns404`
- [ ] RED: Run `RegistrarBatchControllerWebMvcTest` expecting failures.
- [ ] Implement endpoint in `RegistrarController.java`:
  ```java
  @PostMapping("/students/{recordId}/batch")
  public ResponseEntity<StudentRecordDetailsResponse> assignBatch(
          @PathVariable Integer recordId,
          @Valid @RequestBody AssignBatchRequest request) {
      return ResponseEntity.ok(registrarService.assignBatch(recordId, request.batchCode()));
  }
  ```
- [ ] GREEN: Rerun `RegistrarBatchControllerWebMvcTest`.
- [ ] Commit: `feat: expose assign batch endpoint with registrar role security`.

---

## Task 4: Frontend UI, Modal & DataTable Integration

**Files:**
- Modify `src/main/resources/static/registrar.html`
- Modify `src/main/resources/static/student-records.html`
- Modify `src/main/resources/static/js/registrar-students.js`
- Modify `src/main/resources/static/js/registrar-student-records.js`
- Modify `src/main/resources/static/js/registrar-student-records-edit.js`

- [ ] Add `#assignBatchModal` to `registrar.html` and `student-records.html` with student name display, text input with `<datalist id="batchLookupList">`, and alert element.
- [ ] In `student-records.html`, change `#editBatchCode` to `readonly` and add explanatory text.
- [ ] In `registrar-students.js` and `registrar-student-records.js`:
  - Add `"Assign Batch"` button to row actions:
    ```javascript
    <button class="btn btn-surface btn-sm js-assign-batch" data-record-id="..." data-student-name="..." data-batch-code="...">Assign Batch</button>
    ```
  - Implement `openAssignBatchModal(recordId, studentName, batchCode)`.
  - Lazy-load `/api/lookup/batches` into `<datalist id="batchLookupList">` on first open.
  - Wire Save button to `POST /api/registrar/students/{recordId}/batch`.
  - Handle success: hide modal, reload table with `dataTable.ajax.reload(null, false)`, update details card.
  - Handle 400 error: display backend message in `#assignBatchAlert`, keep modal open.
- [ ] Commit: `feat: add assign batch modal and datalist integration to registrar UI`.

---

## Task 5: End-to-End Verification & Full Regression

- [ ] Start application and verify against test database:
  - Submit online enrollment via student portal -> Verify applicant has `No Batch Assigned`.
  - Open Registrar portal -> Click "Assign Batch" -> Select existing batch -> Save -> Verify row updates.
  - Click "Assign Batch" -> Type brand new batch `B2027A` -> Save -> Verify batch created in DB and student updated.
  - Assign student to a section -> Attempt to clear or change batch -> Verify HTTP 400 error alert appears in modal.
  - Open Edit Student modal -> Verify batch field is read-only.
- [ ] Run full test suite:
  ```powershell
  .\gradlew.bat test --console=plain
  ```
  Assert: All existing 492 tests + new tests pass with 0 failures.
- [ ] Commit: `docs: record manual batch code assignment implementation and verification`.
