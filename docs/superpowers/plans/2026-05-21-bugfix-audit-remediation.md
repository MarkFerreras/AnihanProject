# Anihan SRMS Bug-Fix Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the 12 bugs and inconsistencies found in the 2026-05-21 codebase/database audit, ordered by severity.

**Architecture:** Surgical, minimal-diff fixes. Backend logic bugs get Mockito tests written first (TDD); pure annotation/config/doc changes are verified by the full test suite plus a build. No schema changes — the live DB already matches `schema.sql`; only JPA entity annotations are corrected to match the DB.

**Tech Stack:** Java 25, Spring Boot 4.0, Spring Data JPA, Spring Security 7, MySQL 8, Gradle 9.4.1 (Kotlin DSL), JUnit 5 + Mockito.

**Conventions:**
- Run a single test class: `./gradlew test --tests "com.example.springboot.service.ClassName"`
- Run the full suite: `./gradlew test`
- Baseline before starting: full suite is **166 tests, 0 failures**.
- Work stays on `main` per the user's instruction for this remediation.

---

## File Structure

| File | Responsibility | Tasks |
|------|----------------|-------|
| `repository/StudentRecordRepository.java` | Add `class_enrollments` delete + max-id query; widen name lookup | 1, 3 |
| `service/RegistrarService.java` | Delete class enrollments; reject Student ID change | 1, 2 |
| `src/main/resources/static/student-records.html` | Make Student ID input read-only | 2 |
| `service/StudentDetailsService.java` | Resume-only-Enrolling, collision-safe ID generation | 3 |
| `controller/StudentDetailsController.java` | Return 409 on duplicate enrollment | 3 |
| `controller/StudentPortalController.java` | Handle non-unique name matches | 3 |
| `service/TrainerGradeService.java` | Server-side grade-range validation; always recompute final grade | 4 |
| `model/Student*.java` (5 entities) | Correct `@Column` metadata to match the DB | 5 |
| `model/StudentRecord.java`, `dto/registrar/StudentRecordUpdateRequest.java` | Make middle name optional | 6 |
| `config/SecurityConfig.java` | Enable method security | 7 |
| `service/AdminService.java` | Unique default email | 8 |
| `CLAUDE.md` | Correct CSRF documentation | 9 |
| `service/StorageService.java` | Reject unsafe student identifiers | 10 |

---

## Task 1: H1 — `deleteRecord` must remove `class_enrollments` rows

**Problem:** `RegistrarService.deleteRecord()` deletes 9 child-table groups but not `class_enrollments`, which has an FK to `student_records.student_id`. Deleting any student who was enrolled in a class throws a FK violation and the whole delete rolls back.

**Files:**
- Modify: `src/main/java/com/example/springboot/repository/StudentRecordRepository.java`
- Modify: `src/main/java/com/example/springboot/service/RegistrarService.java:363-386`
- Test: `src/test/java/com/example/springboot/service/RegistrarBulkLoadTest.java`

- [ ] **Step 1: Write the failing test**

Add this method to `RegistrarBulkLoadTest` (inside the class, after `statusFilterRestrictsResultsByStudentStatus`):

```java
@Test
void deleteRecordAlsoRemovesClassEnrollments() {
    StudentRecord record = new StudentRecord();
    record.setRecordId(7);
    record.setStudentId("STU-7");
    when(studentRecordRepository.findById(7)).thenReturn(java.util.Optional.of(record));

    registrarService.deleteRecord(7);

    verify(studentRecordRepository).deleteClassEnrollmentsByStudentId("STU-7");
    verify(studentRecordRepository).deleteById(7);
}
```

Add this static import to the top of `RegistrarBulkLoadTest` (it currently imports only `assertEquals`, `assertTrue`, `when`):

```java
import static org.mockito.Mockito.verify;
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.service.RegistrarBulkLoadTest"`
Expected: FAIL — compilation error, `deleteClassEnrollmentsByStudentId` does not exist on `StudentRecordRepository`.

- [ ] **Step 3: Add the repository delete query**

In `StudentRecordRepository.java`, after the existing `deleteGradesByStudentId` method, add:

```java
    @Modifying
    @Query(value = "DELETE FROM class_enrollments WHERE student_id = :studentId", nativeQuery = true)
    void deleteClassEnrollmentsByStudentId(@Param("studentId") String studentId);
```

- [ ] **Step 4: Call it in `deleteRecord`**

In `RegistrarService.deleteRecord()`, in the "Delete child rows in FK dependency order" block, add the call immediately before `studentRecordRepository.deleteById(recordId);`:

```java
        studentRecordRepository.deleteDocumentsByStudentId(studentId);
        studentRecordRepository.deleteGradesByStudentId(studentId);
        studentRecordRepository.deleteClassEnrollmentsByStudentId(studentId);

        studentRecordRepository.deleteById(recordId);
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew test --tests "com.example.springboot.service.RegistrarBulkLoadTest"`
Expected: PASS — all tests in the class green.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/springboot/repository/StudentRecordRepository.java src/main/java/com/example/springboot/service/RegistrarService.java src/test/java/com/example/springboot/service/RegistrarBulkLoadTest.java
git commit -m "fix: delete class_enrollments rows when deleting a student record

deleteRecord() omitted class_enrollments, so deleting any student
enrolled in a class failed with an FK constraint violation."
```

---

## Task 2: H2 — Make Student ID read-only (cannot be changed)

**Problem:** The registrar edit form exposes `editStudentId` as an editable field. `student_id` is a business key referenced by 10 child tables with `ON UPDATE RESTRICT`; changing it fails for any student with related data. Decision: lock the field and reject changes server-side.

**Files:**
- Modify: `src/main/resources/static/student-records.html:126`
- Modify: `src/main/java/com/example/springboot/service/RegistrarService.java:152-201`
- Test: `src/test/java/com/example/springboot/service/RegistrarBulkLoadTest.java`

- [ ] **Step 1: Write the failing test**

Add this method to `RegistrarBulkLoadTest`:

```java
@Test
void updateRecordRejectsChangedStudentId() {
    StudentRecord record = new StudentRecord();
    record.setRecordId(5);
    record.setStudentId("STU-ORIGINAL");
    when(studentRecordRepository.findById(5)).thenReturn(java.util.Optional.of(record));

    com.example.springboot.dto.registrar.StudentRecordUpdateRequest req =
        new com.example.springboot.dto.registrar.StudentRecordUpdateRequest(
            "STU-CHANGED", "Last", "First", "Middle", null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null,
            "Active", null, null, null, null, null, null);

    assertThrows(IllegalArgumentException.class,
        () -> registrarService.updateRecord(5, req));
}
```

Add this static import to `RegistrarBulkLoadTest`:

```java
import static org.junit.jupiter.api.Assertions.assertThrows;
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.service.RegistrarBulkLoadTest"`
Expected: FAIL — `updateRecord` does not throw; it proceeds to mutate the record.

- [ ] **Step 3: Reject Student ID changes in `updateRecord`**

In `RegistrarService.updateRecord()`, replace the current ID-change block:

```java
        String oldStudentId = record.getStudentId();

        if (!oldStudentId.equalsIgnoreCase(request.studentId())) {
            studentRecordRepository.findByStudentId(request.studentId())
                    .filter(other -> !other.getRecordId().equals(recordId))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Student ID is already in use: " + request.studentId());
                    });
            record.setStudentId(request.studentId());
        }
```

with:

```java
        String studentId = record.getStudentId();

        if (!studentId.equals(request.studentId())) {
            throw new IllegalArgumentException("Student ID cannot be changed.");
        }
```

- [ ] **Step 4: Update the child-save calls to use the single `studentId` variable**

Further down in `updateRecord()`, replace:

```java
        StudentRecord saved = studentRecordRepository.save(record);
        String newStudentId = saved.getStudentId();

        saveOjt(oldStudentId, newStudentId, request.ojt());
        saveTesda(oldStudentId, newStudentId, request.tesdaQualifications());
        saveSchoolYears(oldStudentId, newStudentId, request.schoolYears());
        saveParents(saved, request.father(), request.mother());
        saveGuardian(saved, request.guardian());
```

with:

```java
        StudentRecord saved = studentRecordRepository.save(record);

        saveOjt(studentId, studentId, request.ojt());
        saveTesda(studentId, studentId, request.tesdaQualifications());
        saveSchoolYears(studentId, studentId, request.schoolYears());
        saveParents(saved, request.father(), request.mother());
        saveGuardian(saved, request.guardian());
```

- [ ] **Step 5: Make the form field read-only**

In `src/main/resources/static/student-records.html`, change line 126 from:

```html
                                <input type="text" class="form-control" id="editStudentId" maxlength="20" required>
```

to:

```html
                                <input type="text" class="form-control" id="editStudentId" maxlength="20" readonly>
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew test --tests "com.example.springboot.service.RegistrarBulkLoadTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/springboot/service/RegistrarService.java src/main/resources/static/student-records.html src/test/java/com/example/springboot/service/RegistrarBulkLoadTest.java
git commit -m "fix: make Student ID read-only in registrar edit form

student_id is a business key referenced by 10 child tables with
ON UPDATE RESTRICT; changing it failed for any student with related
data. Field is now read-only and the API rejects any change."
```

---

## Task 3: M2 + M3 + M4 — Student portal enrollment lookup & ID generation

**Problems:**
- **M2:** `startOrResume` resumes records of any status, leaking submitted students' PII.
- **M3:** `findByLastNameIgnoreCase...MiddleNameIgnoreCase` returns `Optional` but full names are not unique → `NonUniqueResultException` (500) on a name collision.
- **M4:** `generateStudentId()` uses `count + 1`; after any deletion the next ID re-uses an existing one → unique-constraint 409.

**Files:**
- Modify: `src/main/java/com/example/springboot/repository/StudentRecordRepository.java`
- Modify: `src/main/java/com/example/springboot/service/StudentDetailsService.java`
- Modify: `src/main/java/com/example/springboot/controller/StudentDetailsController.java`
- Modify: `src/main/java/com/example/springboot/controller/StudentPortalController.java`
- Test: `src/test/java/com/example/springboot/service/StudentDetailsServiceTest.java`

- [ ] **Step 1: Write the failing tests**

In `StudentDetailsServiceTest`, inside the `StartOrResume` nested class, add:

```java
@Test
void rejectsResumeWhenMatchingRecordIsAlreadySubmitted() {
    StudentRecord submitted = buildMinimalRecord("SR20260001", "Submitted");
    when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            "Reyes", "Anna", "Cruz")).thenReturn(List.of(submitted));

    assertThrows(IllegalStateException.class,
            () -> service.startOrResume("Reyes", "Anna", "Cruz"));
    verify(studentRecordRepo, never()).save(any());
}

@Test
void resumesEnrollingRecordEvenWhenAnotherNamesakeExists() {
    StudentRecord submitted = buildMinimalRecord("SR20260001", "Submitted");
    StudentRecord enrolling = buildMinimalRecord("SR20260002", "Enrolling");
    when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            "Reyes", "Anna", "Cruz")).thenReturn(List.of(submitted, enrolling));
    stubRelationsLenient();

    StudentDetailsResponse result = service.startOrResume("Reyes", "Anna", "Cruz");

    assertEquals("SR20260002", result.studentId());
    verify(studentRecordRepo, never()).save(any());
}
```

In the `Load` nested class, add:

```java
@Test
void throwsWhenRecordIsNoLongerEnrolling() {
    StudentRecord record = buildMinimalRecord("SR20260001", "Submitted");
    when(studentRecordRepo.findByStudentId("SR20260001")).thenReturn(Optional.of(record));

    assertThrows(IllegalArgumentException.class,
            () -> service.load("SR20260001"));
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.service.StudentDetailsServiceTest"`
Expected: FAIL — compilation error (`findByLastName...` still returns `Optional`, cannot pass `List.of(...)`).

- [ ] **Step 3: Widen the repository lookup to return a list**

In `StudentRecordRepository.java`, change:

```java
    Optional<StudentRecord> findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            String lastName, String firstName, String middleName);
```

to:

```java
    java.util.List<StudentRecord> findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            String lastName, String firstName, String middleName);
```

Then, after the existing `countByStudentIdStartingWith` method, add the max-id query:

```java
    @Query("SELECT MAX(s.studentId) FROM StudentRecord s WHERE s.studentId LIKE :prefix%")
    Optional<String> findMaxStudentIdWithPrefix(@Param("prefix") String prefix);
```

Delete this now-unused method from the same file:

```java
    @Query("SELECT COUNT(s) FROM StudentRecord s WHERE s.studentId LIKE :prefix%")
    long countByStudentIdStartingWith(@Param("prefix") String prefix);
```

(Lexicographic `MAX` equals numeric max because IDs are zero-padded to 4 digits.)

- [ ] **Step 4: Fix `startOrResume` resume logic in `StudentDetailsService`**

Replace the body of `startOrResume` up to (and including) the `StudentRecord record = new StudentRecord();` line:

```java
    @Transactional
    public StudentDetailsResponse startOrResume(String lastName, String firstName, String middleName) {
        var existing = studentRecordRepo
                .findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                        lastName.trim(), firstName.trim(), middleName.trim());

        if (existing.isPresent()) {
            return buildResponse(existing.get());
        }

        StudentRecord record = new StudentRecord();
```

with:

```java
    @Transactional
    public StudentDetailsResponse startOrResume(String lastName, String firstName, String middleName) {
        List<StudentRecord> matches = studentRecordRepo
                .findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                        lastName.trim(), firstName.trim(), middleName.trim());

        // Resume only an in-progress (Enrolling) record; never expose a
        // Submitted/Active/Graduated student's data through the public portal.
        var enrolling = matches.stream()
                .filter(r -> "Enrolling".equalsIgnoreCase(r.getStudentStatus()))
                .findFirst();
        if (enrolling.isPresent()) {
            return buildResponse(enrolling.get());
        }
        if (!matches.isEmpty()) {
            throw new IllegalStateException(
                    "An enrollment already exists for this name. Please contact the registrar.");
        }

        StudentRecord record = new StudentRecord();
```

- [ ] **Step 5: Replace `generateStudentId` with a collision-safe implementation**

In `StudentDetailsService.java`, replace the whole `generateStudentId()` method:

```java
    private String generateStudentId() {
        int year = LocalDate.now().getYear();
        String prefix = "SR" + year;
        long count = studentRecordRepo.countByStudentIdStartingWith(prefix);
        return String.format("%s%04d", prefix, count + 1);
    }
```

with:

```java
    private String generateStudentId() {
        int year = LocalDate.now().getYear();
        String prefix = "SR" + year;
        int next = studentRecordRepo.findMaxStudentIdWithPrefix(prefix)
                .map(maxId -> Integer.parseInt(maxId.substring(prefix.length())) + 1)
                .orElse(1);
        return String.format("%s%04d", prefix, next);
    }
```

- [ ] **Step 6: Add the `load` status guard in `StudentDetailsService`**

Replace the `load` method:

```java
    @Transactional(readOnly = true)
    public StudentDetailsResponse load(String studentId) {
        StudentRecord record = findOrThrow(studentId);
        return buildResponse(record);
    }
```

with:

```java
    @Transactional(readOnly = true)
    public StudentDetailsResponse load(String studentId) {
        StudentRecord record = findOrThrow(studentId);
        if (!"Enrolling".equalsIgnoreCase(record.getStudentStatus())) {
            throw new IllegalArgumentException("This enrollment is no longer editable.");
        }
        return buildResponse(record);
    }
```

- [ ] **Step 7: Update the two existing `StudentDetailsServiceTest` stubs**

In `StudentDetailsServiceTest`, in `createsMinimalRecordWhenNoExistingStudent`, change:

```java
            when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                    "Reyes", "Anna", "Cruz")).thenReturn(Optional.empty());
            when(studentRecordRepo.countByStudentIdStartingWith(anyString())).thenReturn(0L);
```

to:

```java
            when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                    "Reyes", "Anna", "Cruz")).thenReturn(java.util.Collections.emptyList());
            when(studentRecordRepo.findMaxStudentIdWithPrefix(anyString())).thenReturn(Optional.empty());
```

In `resumesExistingRecordWithoutCreatingNew`, change:

```java
            when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                    "Reyes", "Anna", "Cruz")).thenReturn(Optional.of(existing));
```

to:

```java
            when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                    "Reyes", "Anna", "Cruz")).thenReturn(List.of(existing));
```

- [ ] **Step 8: Handle the duplicate-enrollment 409 in `StudentDetailsController`**

Replace the `start` method:

```java
    @PostMapping("/start")
    public ResponseEntity<StudentDetailsResponse> start(@RequestBody Map<String, String> body) {
        String lastName = body.getOrDefault("lastName", "").trim();
        String firstName = body.getOrDefault("firstName", "").trim();
        String middleName = body.getOrDefault("middleName", "").trim();
        if (lastName.isBlank() || firstName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        StudentDetailsResponse response = studentDetailsService.startOrResume(lastName, firstName, middleName);
        return ResponseEntity.ok(response);
    }
```

with:

```java
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody Map<String, String> body) {
        String lastName = body.getOrDefault("lastName", "").trim();
        String firstName = body.getOrDefault("firstName", "").trim();
        String middleName = body.getOrDefault("middleName", "").trim();
        if (lastName.isBlank() || firstName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            StudentDetailsResponse response =
                    studentDetailsService.startOrResume(lastName, firstName, middleName);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }
```

- [ ] **Step 9: Fix the non-unique name match in `StudentPortalController`**

Replace the body of `checkDuplicate`:

```java
        boolean blocked = studentRecordRepository
                .findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                        lastName.trim(), firstName.trim(), middleName.trim())
                .map(r -> "Submitted".equals(r.getStudentStatus()) || "Active".equals(r.getStudentStatus()))
                .orElse(false);
```

with:

```java
        boolean blocked = studentRecordRepository
                .findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                        lastName.trim(), firstName.trim(), middleName.trim())
                .stream()
                .anyMatch(r -> "Submitted".equals(r.getStudentStatus())
                        || "Active".equals(r.getStudentStatus()));
```

- [ ] **Step 10: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.service.StudentDetailsServiceTest"`
Expected: PASS — all tests green (including the 3 new ones and the 2 updated stubs).

- [ ] **Step 11: Run the full suite to confirm no regression**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — `countByStudentIdStartingWith` removal compiles cleanly (no other callers).

- [ ] **Step 12: Commit**

```bash
git add src/main/java/com/example/springboot/repository/StudentRecordRepository.java src/main/java/com/example/springboot/service/StudentDetailsService.java src/main/java/com/example/springboot/controller/StudentDetailsController.java src/main/java/com/example/springboot/controller/StudentPortalController.java src/test/java/com/example/springboot/service/StudentDetailsServiceTest.java
git commit -m "fix: harden student portal enrollment lookup and ID generation

- Resume only in-progress (Enrolling) records; submitted students'
  PII is no longer reachable through the public portal.
- Name lookup returns a list — a name collision no longer 500s.
- Student ID generation uses the max existing suffix instead of a
  row count, so IDs no longer collide after a record is deleted."
```

---

## Task 4: M5 + M6 — Trainer grade validation and final-grade recomputation

**Problems:**
- **M5:** `TrainerGradeController.saveGrades` has no `@Valid`, so `SaveGradeRequest`'s range constraints are never enforced — invalid grades (7.5, -3) persist.
- **M6:** `saveGrades` only recomputes `finalGrade` when both midterm and finals are present; clearing one leaves a stale computed grade behind.

**Fix approach:** Validate ranges in the service layer (consistent with the service's existing `IllegalArgumentException` checks, which the controller already maps to HTTP 400), and recompute `finalGrade` unconditionally.

**Files:**
- Modify: `src/main/java/com/example/springboot/service/TrainerGradeService.java`
- Test: `src/test/java/com/example/springboot/service/TrainerGradeServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Add these two methods to `TrainerGradeServiceTest`:

```java
@Test
void testSaveGradesRejectsOutOfRangeGrade() {
    stubTrainerLookup();
    Integer classId = 1;

    SchoolClass schoolClass = new SchoolClass();
    schoolClass.setClassId(classId);
    schoolClass.setTrainer(trainerUser);

    when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));

    SaveGradeRequest request = new SaveGradeRequest(
            "STU001", new BigDecimal("7.5"), new BigDecimal("3.0"), null, null, null);

    assertThrows(IllegalArgumentException.class,
            () -> gradeService.saveGrades(classId, List.of(request)));
    verify(gradeRepository, never()).save(any());
}

@Test
void testSaveGradesClearsStaleFinalGradeWhenInputIncomplete() {
    stubTrainerLookup();
    Integer classId = 1;

    Subject subject = new Subject();
    subject.setSubjectCode("CUL101");

    SchoolClass schoolClass = new SchoolClass();
    schoolClass.setClassId(classId);
    schoolClass.setTrainer(trainerUser);
    schoolClass.setSubject(subject);

    StudentRecord student = new StudentRecord();
    student.setStudentId("STU001");

    Grade grade = new Grade();
    grade.setGradeId(1);
    grade.setStudent(student);
    grade.setMidtermGrade(new BigDecimal("2.0"));
    grade.setFinalsGrade(new BigDecimal("3.0"));
    grade.setFinalGrade(new BigDecimal("2.60"));

    // New save clears the finals grade — finalGrade must become null
    SaveGradeRequest request = new SaveGradeRequest(
            "STU001", new BigDecimal("2.0"), null, null, null, null);

    when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
    when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
            .thenReturn(true);
    when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
            .thenReturn(Optional.of(grade));
    when(gradeRepository.save(any())).thenReturn(grade);

    gradeService.saveGrades(classId, List.of(request));

    assertNull(grade.getFinalGrade());
}
```

(Note: `testSaveGradesRejectsOutOfRangeGrade` does not stub `enrollmentRepository` because validation runs before the enrollment check.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests "com.example.springboot.service.TrainerGradeServiceTest"`
Expected: FAIL — `testSaveGradesRejectsOutOfRangeGrade` (7.5 is saved, no exception) and `testSaveGradesClearsStaleFinalGradeWhenInputIncomplete` (stale 2.60 remains).

- [ ] **Step 3: Add a range-validation helper to `TrainerGradeService`**

Add these two private methods to `TrainerGradeService` (next to `computeFinalGrade`):

```java
    private void validateGradeRanges(SaveGradeRequest request) {
        checkRange("Midterm grade", request.midtermGrade(), "1.0", "5.0");
        checkRange("Finals grade", request.finalsGrade(), "1.0", "5.0");
        checkRange("Re-exam grade", request.reExamGrade(), "1.0", "5.0");
        checkRange("Hours studied", request.hoursStudied(), "0.0", "9.99");
    }

    private void checkRange(String label, BigDecimal value, String min, String max) {
        if (value == null) {
            return;
        }
        if (value.compareTo(new BigDecimal(min)) < 0 || value.compareTo(new BigDecimal(max)) > 0) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max);
        }
    }
```

- [ ] **Step 4: Call validation and fix final-grade recomputation in `saveGrades`**

In `TrainerGradeService.saveGrades`, inside the `for (SaveGradeRequest request : gradeUpdates)` loop, add the validation call as the **first** statement in the loop body (before the enrollment check):

```java
        for (SaveGradeRequest request : gradeUpdates) {
            validateGradeRanges(request);

            // Verify student is enrolled in this class
            if (!enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, request.studentId())) {
```

Then, further down in the same loop, replace:

```java
            if (request.midtermGrade() != null && request.finalsGrade() != null) {
                BigDecimal finalGrade = computeFinalGrade(request.midtermGrade(), request.finalsGrade());
                grade.setFinalGrade(finalGrade);
            }
```

with:

```java
            // Always recompute — computeFinalGrade returns null when either
            // input is missing, so a stale final grade is never left behind.
            grade.setFinalGrade(computeFinalGrade(request.midtermGrade(), request.finalsGrade()));
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew test --tests "com.example.springboot.service.TrainerGradeServiceTest"`
Expected: PASS — all tests green (the existing `testSaveGradesComputesFinalGrade` still passes: 4.0 and 3.0 → 3.40).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/springboot/service/TrainerGradeService.java src/test/java/com/example/springboot/service/TrainerGradeServiceTest.java
git commit -m "fix: validate trainer grade ranges and recompute final grade

- saveGrades now rejects grades outside 1.0-5.0 (and hours outside
  0-9.99) server-side; the controller lacked @Valid so the DTO
  constraints were never enforced.
- finalGrade is recomputed unconditionally, so clearing midterm or
  finals no longer leaves a stale computed grade."
```

---

## Task 5: L1 — Correct JPA `@Column` metadata drift

**Problem:** The 2026-05-21 DB widening updated the live DB and `schema.sql` but not the entity annotations. All are DDL-only attributes (no runtime failure today with `ddl-auto=none`), but the entities are misleading and would mis-generate DDL / could fail a future `validate`.

**Files:**
- Modify: `src/main/java/com/example/springboot/model/StudentUpload.java`
- Modify: `src/main/java/com/example/springboot/model/StudentEducation.java`
- Modify: `src/main/java/com/example/springboot/model/StudentSchoolYear.java`
- Modify: `src/main/java/com/example/springboot/model/StudentTesdaQualification.java`
- Modify: `src/main/java/com/example/springboot/model/StudentOjt.java`

- [ ] **Step 1: Fix `StudentUpload.java`**

Change these fields to match the DB (`kind` VARCHAR(30), `file_path` VARCHAR(500), `original_name`/`mime_type`/`size_bytes` nullable):

```java
    @Column(name = "kind", length = 30, nullable = false)
    private String kind;

    @Column(name = "file_path", length = 500, nullable = false)
    private String filePath;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;
```

- [ ] **Step 2: Fix `StudentEducation.java`**

Change these fields (`level` VARCHAR(50), `grade_year` VARCHAR(50), `semester` VARCHAR(20), `ended_year` VARCHAR(20)):

```java
    @Column(name = "level", length = 50, nullable = false)
    private String level;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "school_address")
    private String schoolAddress;

    @Column(name = "grade_year", length = 50)
    private String gradeYear;

    @Column(name = "semester", length = 20)
    private String semester;

    @Column(name = "ended_year", length = 20)
    private String endedYear;
```

- [ ] **Step 3: Fix `StudentSchoolYear.java`**

Change the four start/end fields to `length = 20`:

```java
    @Column(name = "sy_start", length = 20)
    private String syStart;

    @Column(name = "sem_start", length = 20)
    private String semStart;

    @Column(name = "sy_end", length = 20)
    private String syEnd;

    @Column(name = "sem_end", length = 20)
    private String semEnd;
```

- [ ] **Step 4: Fix `StudentTesdaQualification.java`**

Change the `result` field to `length = 50`:

```java
    @Column(name = "result", length = 50)
    private String result;
```

- [ ] **Step 5: Fix `StudentOjt.java`**

Change `hoursRendered` precision to 8:

```java
    @Column(name = "hours_rendered", precision = 8, scale = 2)
    private BigDecimal hoursRendered;
```

- [ ] **Step 6: Build and run the full suite to confirm no regression**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — 166 tests, 0 failures (pure annotation change, no behavior change).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/springboot/model/StudentUpload.java src/main/java/com/example/springboot/model/StudentEducation.java src/main/java/com/example/springboot/model/StudentSchoolYear.java src/main/java/com/example/springboot/model/StudentTesdaQualification.java src/main/java/com/example/springboot/model/StudentOjt.java
git commit -m "fix: align JPA @Column metadata with the widened DB schema

The 2026-05-21 DB column widening was not reflected in the entity
annotations (lengths, precision, nullability). DDL-only attributes,
but the entities were misleading."
```

---

## Task 6: L2 — Make middle name optional

**Problem:** `StudentRecord.middleName` is `@Column(nullable=false)` and `StudentRecordUpdateRequest.middleName` is `@NotBlank`, contradicting the DB (`middle_name` nullable), `schema.sql`, the 2026-05-09 decision, and the portal allowing blank middle names. The registrar is forced to invent a middle name for students who legitimately enrolled without one.

**Files:**
- Modify: `src/main/java/com/example/springboot/model/StudentRecord.java:33`
- Modify: `src/main/java/com/example/springboot/dto/registrar/StudentRecordUpdateRequest.java:27-28`
- Modify: `src/main/java/com/example/springboot/service/RegistrarService.java:169`
- Modify: `src/main/resources/static/student-records.html`

- [ ] **Step 1: Make the entity column nullable**

In `StudentRecord.java`, change:

```java
    @Column(name = "middle_name", nullable = false)
    private String middleName;
```

to:

```java
    @Column(name = "middle_name")
    private String middleName;
```

- [ ] **Step 2: Remove `@NotBlank` from the DTO**

In `StudentRecordUpdateRequest.java`, change:

```java
        @NotBlank(message = "Middle name is required")
        String middleName,
```

to:

```java
        String middleName,
```

If `NotBlank` becomes an unused import after this change, leave the import — `lastName` and `firstName` still use it.

- [ ] **Step 3: Normalize blank middle name to null in `RegistrarService.updateRecord`**

Change:

```java
        record.setMiddleName(request.middleName());
```

to:

```java
        record.setMiddleName(emptyToNull(request.middleName()));
```

- [ ] **Step 4: Remove the `required` attribute from the form field**

In `src/main/resources/static/student-records.html`, locate the input with `id="editMiddleName"`. If it carries a `required` attribute, remove that attribute. (Leave the field otherwise unchanged.)

- [ ] **Step 5: Build and run the full suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — 166 tests, 0 failures.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/springboot/model/StudentRecord.java src/main/java/com/example/springboot/dto/registrar/StudentRecordUpdateRequest.java src/main/java/com/example/springboot/service/RegistrarService.java src/main/resources/static/student-records.html
git commit -m "fix: make student middle name optional end-to-end

The entity, update DTO, and registrar form required a middle name,
contradicting the nullable DB column and the portal which allows
students with no middle name to enroll."
```

---

## Task 7: L3 — Enable method security so `@PreAuthorize` is enforced

**Problem:** `TrainerGradeController` carries `@PreAuthorize("hasRole('TRAINER')")`, but `@EnableMethodSecurity` is absent, so the annotation is silently ignored. (The `/api/trainer/**` URL matcher still protects the endpoint, so there is no live hole — but the annotation is dead and would fail open if the URL mapping ever changed.)

**Files:**
- Modify: `src/main/java/com/example/springboot/config/SecurityConfig.java`

- [ ] **Step 1: Add `@EnableMethodSecurity` to `SecurityConfig`**

Add the import:

```java
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
```

Add the annotation to the class, alongside the existing ones:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
```

- [ ] **Step 2: Build and run the full suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — 166 tests, 0 failures. (Only `TrainerGradeController` has `@PreAuthorize`, and it now consistently enforces the same `ROLE_TRAINER` the URL matcher already requires.)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/springboot/config/SecurityConfig.java
git commit -m "fix: enable method security so @PreAuthorize is enforced

@PreAuthorize on TrainerGradeController was silently ignored because
@EnableMethodSecurity was missing."
```

---

## Task 8: L5 — Make the default account email unique

**Problem:** `AdminService.createUser` defaults a blank email to the constant `user@anihan.local`. The `existsByEmail` check then blocks the second email-less account ("Email is already taken"), so only one email-less account can ever exist.

**Files:**
- Modify: `src/main/java/com/example/springboot/service/AdminService.java:41-43`
- Test: `src/test/java/com/example/springboot/service/AdminServiceTest.java`

- [ ] **Step 1: Write the failing test**

Add this method to `AdminServiceTest`. First inspect the existing `createUser` tests in that file for the exact `userRepository`/`passwordEncoder` mock field names and reuse them; the snippet below assumes the conventional names `userRepository`, `passwordEncoder`, `adminService`:

```java
@Test
void createUserWithoutEmailDerivesUsernameBasedEmail() {
    AdminCreateUserRequest request = new AdminCreateUserRequest(
            "newtrainer", "Password1!", "ROLE_TRAINER",
            "Cruz", "Maria", "L", null, LocalDate.of(1990, 1, 1));

    when(userRepository.existsByUsername("newtrainer")).thenReturn(false);
    when(userRepository.existsByEmail("newtrainer@anihan.local")).thenReturn(false);
    when(passwordEncoder.encode("Password1!")).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    adminService.createUser(request);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertEquals("newtrainer@anihan.local", captor.getValue().getEmail());
}
```

Ensure the imports `java.time.LocalDate`, `org.mockito.ArgumentCaptor`, `com.example.springboot.dto.AdminCreateUserRequest`, and `com.example.springboot.model.User` are present (add any that are missing).

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests "com.example.springboot.service.AdminServiceTest"`
Expected: FAIL — derived email is `user@anihan.local`, not `newtrainer@anihan.local`.

- [ ] **Step 3: Derive a unique default email**

In `AdminService.createUser`, change:

```java
        // Derive effective email
        String email = (request.email() != null && !request.email().isBlank())
                ? request.email().trim()
                : "user@anihan.local";
```

to:

```java
        // Derive effective email — username is unique, so this default is too
        String email = (request.email() != null && !request.email().isBlank())
                ? request.email().trim()
                : request.username().trim().toLowerCase() + "@anihan.local";
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests "com.example.springboot.service.AdminServiceTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/springboot/service/AdminService.java src/test/java/com/example/springboot/service/AdminServiceTest.java
git commit -m "fix: derive a unique default email for accounts created without one

The fixed default user@anihan.local collided on the second
email-less account, blocking the create."
```

---

## Task 9: L4 — Correct the CSRF statement in CLAUDE.md

**Problem:** `SecurityConfig` disables CSRF globally (`csrf.disable()`), but `CLAUDE.md` states "CSRF is disabled for `/api/**` endpoints and enabled for form submissions." The app is entirely AJAX/JSON with no server-rendered form posts, so global disable is consistent with reality — only the documentation is wrong. (Re-enabling CSRF properly would require a token round-trip in every JS request and is out of scope for this bug-fix pass.)

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Correct the documentation**

In `CLAUDE.md`, under "Key Conventions", change the **Security** line:

```
**Security:** CSRF is disabled for `/api/**` endpoints and enabled for form submissions. Session timeout is 30 minutes; cookies are HTTP-only, SameSite=Lax.
```

to:

```
**Security:** CSRF protection is disabled globally — the app is entirely AJAX/JSON and has no server-rendered form posts. Session timeout is 30 minutes; cookies are HTTP-only, SameSite=Lax.
```

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: correct CSRF description to match SecurityConfig

SecurityConfig disables CSRF globally; the doc incorrectly claimed
it was enabled for form submissions."
```

---

## Task 10: L6 — Reject unsafe student identifiers in `StorageService`

**Problem:** `StorageService.store()` uses the path-variable `studentId` directly in a filesystem path before the DB existence check runs. Spring's single-segment path matching largely prevents traversal, but a defensive whitelist check closes the gap cheaply.

**Files:**
- Modify: `src/main/java/com/example/springboot/service/StorageService.java:40-50`

- [ ] **Step 1: Add the identifier guard**

In `StorageService.store()`, add this as the **first** statement of the method, before `validateFile(kind, file);`:

```java
        if (studentId == null || !studentId.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid student identifier.");
        }
```

- [ ] **Step 2: Build and run the full suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — 166 tests, 0 failures (no existing test exercises `store` with an invalid id).

- [ ] **Step 3: Manual verification**

Start the app (`./gradlew bootRun`) and confirm a crafted identifier is rejected:

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST "http://localhost:8080/api/student/..%2F..%2Fevil/upload?kind=ID_PHOTO" -F "file=@README.md"
```

Expected: `400` (request rejected). Stop the app afterward.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/springboot/service/StorageService.java
git commit -m "fix: reject unsafe student identifiers in file storage

Defense-in-depth: whitelist the studentId path segment before it is
used to build an upload path."
```

---

## Final Verification

- [ ] **Run the full test suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — at least 171 tests (166 baseline + 5 new), 0 failures.

- [ ] **Manual smoke test (registrar + trainer + portal)**

Start `./gradlew bootRun` and verify:
- Registrar: delete a student who is enrolled in a class → succeeds (Task 1).
- Registrar: the Student ID field on the edit page is read-only (Task 2).
- Portal: entering the name of an already-submitted student → blocked, no PII shown (Task 3).
- Trainer: entering a grade of `7` → rejected with a range error (Task 4).

- [ ] **Update the memory bank**

Per `CLAUDE.md`'s Memory Update Protocol, update `memory-bank/progress.md`, `memory-bank/changeLog.md`, and `memory-bank/activeContext.md` with this remediation, then commit:

```bash
git add memory-bank/
git commit -m "docs: record 2026-05-21 bug-fix audit remediation in memory bank"
```

---

## Decisions & Residual Risk (not in scope of this plan)

- **M1 (full portal IDOR):** This plan stops submitted students from being resumed/loaded (Task 3). The `GET /api/student/{studentId}` and `GET /api/student/files/{uploadId}` endpoints remain unauthenticated and enumerable for in-progress records. The `load` guard limits exposure to Enrolling-status students only; fully closing the file/ID IDOR (e.g. signed tokens or authentication) is a separate design decision the user chose to defer.
- **M4 concurrency:** Task 3 fixes the post-deletion ID collision. A pure race between two simultaneous enrollments is still theoretically possible; it is caught by the `student_id` unique constraint (409). A retry-on-duplicate loop or a DB sequence would close it fully if needed.
- **L4 (CSRF):** Documentation corrected only. Re-enabling CSRF for the cookie-authenticated JSON API (token round-trip in every JS request) is deferred.
- **N+1 queries** in `ClassManagementService.getEligibleStudents`/`getClasses` and `RegistrarService.getAllRecords` are acceptable at ~160-student scale (per existing memory-bank notes) and are not addressed here.
