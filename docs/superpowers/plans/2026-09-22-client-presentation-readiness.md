# Client Presentation Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans`. The executor must also use `superpowers:using-git-worktrees`, `superpowers:test-driven-development`, and `superpowers:verification-before-completion` as directed by those skills.

**Goal:** Deliver a presentation-ready Anihan SRMS build with reliable class-year filters, working DOCX/XLSX document uploads, correct missing-resource 404s, repeatable demo-account reset, and recorded live-MySQL/browser evidence.

**Architecture:** Preserve the existing Spring MVC/JPA and vanilla jQuery/DataTables design. Lock the merged class-year-filter contract with focused tests, make frontend event binding idempotent, synchronize every schema representation, and verify database-dependent behavior against the Docker MySQL instance.

**Tech Stack:** Java 25, Spring Boot 4.0.4, Spring Security 7, Spring Data JPA, MySQL 8/Docker, Gradle 9.4.1 Kotlin DSL, jQuery 4, Bootstrap 5.3, DataTables 2.

**Spec:** `memory-bank/bugs.md` (Bugs 12–13), `CLAUDE.md`, and the decisions below.

## Global Constraints

- Work in an isolated worktree on `fix/client-demo-readiness-and-audit`; never edit or commit on `main`.
- Preserve existing records and security answers; do not purge data or insert sample grades.
- JPA DDL remains `none`; synchronize entity metadata, `schema.sql`, `AnihanSRMS.sql`, and the dated migration.
- Do not add a JavaScript test framework for two small lifecycle fixes; reproduce and verify them in the browser.
- Demo-account reset must preserve user IDs/security answers and clear both administrative and security lockout state.
- A demo account is ready only when it has exactly two security answers and can log in to its role dashboard.
- For any newly discovered glitch, stop that path and use `superpowers:systematic-debugging`; do not fold speculative fixes into this plan.

## Decisions Already Made

| Topic | Decision |
|---|---|
| Class-year filter | Keep existing endpoints and service ordering; add missing characterization coverage and fix only frontend lifecycle issues. |
| Bug 12 | Widen `documents.file_type` and `Document.fileType` from 50 to 100. |
| Bug 13 | Handle `NoResourceFoundException` as JSON HTTP 404 and prove it with MockMvc. |
| Demo accounts | Reset `admin`, `registrar`, and `trainer` to `password123`; preserve security answers and require two before rehearsal. |
| Database | Back up first, apply only the dated migration and seed script, then verify live behavior. |

## Review Focus

- Repeated semester refreshes must not duplicate `<option>` elements or change handlers.
- Empty, missing, and explicit semester filters must call the intended backend behavior.
- OpenXML MIME types must fit in the entity, both schema files, and live MySQL.
- Missing permitted static resources must return 404 JSON, while protected routes retain security behavior.
- Reset accounts must keep their IDs/answers, clear lockouts, and reach the correct dashboards.

---

### Task 1: Lock the Class-Year Contract and Fix Frontend Lifecycle

**Files:**
- Modify: `src/test/java/com/example/springboot/controller/TrainerControllerWebMvcTest.java`
- Modify: `src/test/java/com/example/springboot/controller/ClassManagementControllerWebMvcTest.java`
- Modify: `src/test/java/com/example/springboot/service/TrainerServiceTest.java`
- Modify: `src/main/resources/static/js/registrar-classes.js`
- Modify: `src/main/resources/static/js/trainer-classes.js`

**Interfaces:**
- Consumes: existing `GET /api/{registrar|trainer}/classes/semesters` and optional `semester` query parameters.
- Produces: one change handler per page, deduplicated options, preserved selection, and no `TrainerService` production change.

- [ ] **Step 1: Add backend characterization tests**

Add these methods and required `verify` imports to the existing test classes:

```java
// TrainerControllerWebMvcTest
@Test
@WithMockUser(username = "trainer", roles = "TRAINER")
void getAvailableSemestersReturnsAssignedYears() throws Exception {
    when(service.getAvailableSemesters()).thenReturn(List.of("2026", "2025"));
    mvc.perform(get("/api/trainer/classes/semesters"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("2026"))
            .andExpect(jsonPath("$[1]").value("2025"));
}

@Test
@WithMockUser(username = "trainer", roles = "TRAINER")
void getMyClassesPassesExplicitSemesterToService() throws Exception {
    when(service.getMyClasses("2026")).thenReturn(List.of());
    mvc.perform(get("/api/trainer/classes").param("semester", "2026"))
            .andExpect(status().isOk());
    verify(service).getMyClasses("2026");
}

@Test
@WithMockUser(username = "trainer", roles = "TRAINER")
void getMyClassesPassesBlankSemesterToService() throws Exception {
    when(service.getMyClasses("")).thenReturn(List.of());
    mvc.perform(get("/api/trainer/classes").param("semester", ""))
            .andExpect(status().isOk());
    verify(service).getMyClasses("");
}

// ClassManagementControllerWebMvcTest
@Test
@WithMockUser(username = "registrar", roles = "REGISTRAR")
void getAvailableSemestersReturnsYears() throws Exception {
    when(service.getAvailableSemesters()).thenReturn(List.of("2026", "2025"));
    mvc.perform(get("/api/registrar/classes/semesters"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("2026"));
}
```

In `TrainerServiceTest`, add:

```java
private SchoolClass classForYear(int id, String year) {
    Section section = new Section();
    section.setSectionCode("SEC-" + year);
    section.setSection("Section " + year);
    Subject subject = new Subject();
    subject.setSubjectCode("SUB-" + year);
    subject.setSubjectName("Subject " + year);
    SchoolClass schoolClass = new SchoolClass();
    schoolClass.setClassId(id);
    schoolClass.setSection(section);
    schoolClass.setSubject(subject);
    schoolClass.setSemester(year);
    return schoolClass;
}

@Test
void getMyClassesFiltersByExplicitSemester() {
    when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
    when(classRepository.findByTrainerUserId(42))
            .thenReturn(List.of(classForYear(1, "2025"), classForYear(2, "2026")));

    List<TrainerClassResponse> result = service.getMyClasses("2026");

    assertEquals(1, result.size());
    assertEquals("2026", result.getFirst().semester());
}
```

- [ ] **Step 2: Run characterization tests**

Run:

```bash
./gradlew test --tests "com.example.springboot.controller.TrainerControllerWebMvcTest" --tests "com.example.springboot.controller.ClassManagementControllerWebMvcTest" --tests "com.example.springboot.service.TrainerServiceTest"
```

Expected: PASS. These pin already-merged backend behavior; a failure means the plan is stale and must be ruled on before frontend edits.

- [ ] **Step 3: Make registrar refresh and binding idempotent**

In `registrar-classes.js`, call `bindSemesterFilter()` once from `document.ready`, make `loadAvailableSemesters()` return only the AJAX request, and call it after successful class creation:

```javascript
function bindSemesterFilter() {
    $('#semesterFilterSelect').off('change.semesterFilter').on('change.semesterFilter', function () {
        reloadTable($(this).val() || null);
    });
}

function loadAvailableSemesters() {
    return $.ajax({
        url: '/api/registrar/classes/semesters',
        method: 'GET',
        success: function (semesters) {
            const select = $('#semesterFilterSelect');
            const selected = select.val();
            select.find('option:not([value=""])').remove();
            semesters.forEach(sem => select.append($('<option>').val(sem).text(sem)));
            if (selected && semesters.includes(selected)) select.val(selected);
            else if (currentSemester && semesters.includes(currentSemester)) select.val(currentSemester);
        }
    });
}
```

Use `encodeURIComponent(semester)` when building the registrar classes URL. In the create success callback retain the table reload and add `loadAvailableSemesters();`.

- [ ] **Step 4: Initialize trainer table before binding/loading the filter**

In `trainer-classes.js`, remove the pre-table `loadAvailableSemesters()` call. After `classesTable = ...DataTable(...)`, call a deduplicating loader and one-time namespaced binder:

```javascript
function loadAvailableSemesters() {
    return $.ajax({
        url: '/api/trainer/classes/semesters',
        method: 'GET',
        success: function (semesters) {
            const select = $('#semesterFilterSelect');
            const selected = select.val();
            select.find('option:not([value=""])').remove();
            semesters.forEach(sem => select.append($('<option>').val(sem).text(sem)));
            if (selected && semesters.includes(selected)) select.val(selected);
        }
    });
}

function bindSemesterFilter() {
    $('#semesterFilterSelect').off('change.semesterFilter').on('change.semesterFilter', function () {
        const semester = $(this).val();
        const url = semester
            ? '/api/trainer/classes?semester=' + encodeURIComponent(semester)
            : '/api/trainer/classes';
        classesTable.ajax.url(url).load();
    });
}

bindSemesterFilter();
loadAvailableSemesters();
```

- [ ] **Step 5: Verify and commit Task 1**

Run the Step 2 command again. In a running app, open Registrar and Trainer Classes pages; confirm one option per year and exactly one `/classes` request per filter change in DevTools Network.

```bash
git add src/main/resources/static/js/registrar-classes.js src/main/resources/static/js/trainer-classes.js src/test/java/com/example/springboot/controller/TrainerControllerWebMvcTest.java src/test/java/com/example/springboot/controller/ClassManagementControllerWebMvcTest.java src/test/java/com/example/springboot/service/TrainerServiceTest.java
git commit -m "fix: stabilize class year filters"
```

---

### Task 2: Fix Bug 12 Across Every Schema Source

**Files:**
- Create: `src/test/java/com/example/springboot/SchemaContractTest.java`
- Create: `src/main/sql/migrations/2026-09-22-widen-documents-file-type.sql`
- Modify: `src/main/java/com/example/springboot/model/Document.java`
- Modify: `src/main/sql/schema.sql`
- Modify: `src/main/sql/AnihanSRMS.sql`

**Interfaces:**
- Consumes: `Document.fileType` and `documents.file_type`.
- Produces: a consistent 100-character contract for the 73-character DOCX MIME type.

- [ ] **Step 1: Write the failing schema contract test**

```java
package com.example.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.springboot.model.Document;

import jakarta.persistence.Column;

class SchemaContractTest {
    @Test
    void documentFileTypeSupportsOpenXmlMimeTypesEverywhere() throws Exception {
        Column column = Document.class.getDeclaredField("fileType").getAnnotation(Column.class);
        assertEquals(100, column.length());

        for (String file : List.of("src/main/sql/schema.sql", "src/main/sql/AnihanSRMS.sql")) {
            String sql = Files.readString(Path.of(file));
            assertTrue(sql.matches("(?s).*file_type\\s+VARCHAR\\(100\\)\\s+NOT NULL.*"), file);
        }
        assertTrue(Files.exists(Path.of(
                "src/main/sql/migrations/2026-09-22-widen-documents-file-type.sql")));
    }
}
```

- [ ] **Step 2: Run it and confirm RED**

Run: `./gradlew test --tests "com.example.springboot.SchemaContractTest"`

Expected: FAIL because the entity and both schemas still use 50 and the migration is absent.

- [ ] **Step 3: Make all four representations consistent**

Change `Document.fileType` to `@Column(name = "file_type", nullable = false, length = 100)` and both schema declarations to `file_type VARCHAR(100) NOT NULL`.

Create the migration:

```sql
-- Idempotent in effect: repeated execution preserves the same definition.
USE AnihanSRMS;

ALTER TABLE documents
    MODIFY COLUMN file_type VARCHAR(100) NOT NULL;

SELECT COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'AnihanSRMS'
  AND TABLE_NAME = 'documents'
  AND COLUMN_NAME = 'file_type';
```

- [ ] **Step 4: Run GREEN and commit**

Run: `./gradlew test --tests "com.example.springboot.SchemaContractTest"`

Expected: PASS.

```bash
git add src/test/java/com/example/springboot/SchemaContractTest.java src/main/java/com/example/springboot/model/Document.java src/main/sql/schema.sql src/main/sql/AnihanSRMS.sql src/main/sql/migrations/2026-09-22-widen-documents-file-type.sql
git commit -m "fix: widen document MIME type storage"
```

---

### Task 3: Fix Bug 13 With a 404 Regression Test

**Files:**
- Modify: `src/test/java/com/example/springboot/controller/AuthControllerWebMvcTest.java`
- Modify: `src/main/java/com/example/springboot/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: Spring MVC `NoResourceFoundException` for missing static resources.
- Produces: HTTP 404 JSON `{ "message": ... }`; authentication rules remain unchanged.

- [ ] **Step 1: Write the failing test**

Add static imports for `get` and Hamcrest `containsString`, then add:

```java
@Test
void missingPermittedStaticResourceReturnsJson404() throws Exception {
    mockMvc.perform(get("/js/does-not-exist.js"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", containsString("does-not-exist.js")));
}
```

- [ ] **Step 2: Confirm RED**

Run: `./gradlew test --tests "com.example.springboot.controller.AuthControllerWebMvcTest.missingPermittedStaticResourceReturnsJson404"`

Expected: FAIL with HTTP 500 from the generic handler.

- [ ] **Step 3: Add the specific handler**

```java
@ExceptionHandler(NoResourceFoundException.class)
public ResponseEntity<Map<String, String>> handleNoResourceFound(NoResourceFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "Resource not found: " + ex.getResourcePath()));
}
```

Import `org.springframework.web.servlet.resource.NoResourceFoundException`.

- [ ] **Step 4: Run GREEN and commit**

Run: `./gradlew test --tests "com.example.springboot.controller.AuthControllerWebMvcTest"`

Expected: PASS.

```bash
git add src/test/java/com/example/springboot/controller/AuthControllerWebMvcTest.java src/main/java/com/example/springboot/exception/GlobalExceptionHandler.java
git commit -m "fix: return 404 for missing resources"
```

---

### Task 4: Add a Safe, Repeatable Demo-Account Reset

**Files:**
- Modify: `src/test/java/com/example/springboot/SchemaContractTest.java`
- Create: `src/main/sql/seed-accounts.sql`

**Interfaces:**
- Consumes: existing `users` rows and `user_security_answers` keyed by stable `user_id`.
- Produces: canonical enabled accounts with cleared lockouts; never deletes answers or changes user IDs.

- [ ] **Step 1: Add the failing script contract test**

```java
@Test
void demoAccountSeedResetsLockoutsWithoutDeletingAnswers() throws Exception {
    Path seed = Path.of("src/main/sql/seed-accounts.sql");
    assertTrue(Files.exists(seed));
    String sql = Files.readString(seed);
    assertTrue(sql.contains("security_locked = 0"));
    assertTrue(sql.contains("failed_security_attempts = 0"));
    assertTrue(sql.contains("security_lockout_started_at = NULL"));
    assertTrue(sql.contains("TIMESTAMPDIFF(YEAR"));
    assertTrue(!sql.toLowerCase().contains("delete from user_security_answers"));
}
```

- [ ] **Step 2: Confirm RED**

Run: `./gradlew test --tests "com.example.springboot.SchemaContractTest.demoAccountSeedResetsLockoutsWithoutDeletingAnswers"`

Expected: FAIL because the script does not exist.

- [ ] **Step 3: Create `seed-accounts.sql`**

```sql
USE AnihanSRMS;

-- Preflight must return zero rows. Stop if an email belongs to another username.
SELECT username, email
FROM users
WHERE (email = 'admin@anihan.local' AND username <> 'admin')
   OR (email = 'registrar@anihan.local' AND username <> 'registrar')
   OR (email = 'trainer@anihan.local' AND username <> 'trainer');

START TRANSACTION;

CREATE TEMPORARY TABLE desired_demo_accounts (
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    middlename VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(15) NOT NULL
);

INSERT INTO desired_demo_accounts VALUES
('admin', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Dela Cruz', 'Juan', 'Santos', '1995-06-15', 'admin@anihan.local', 'ROLE_ADMIN'),
('registrar', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Reyes', 'Maria', 'Garcia', '1990-03-22', 'registrar@anihan.local', 'ROLE_REGISTRAR'),
('trainer', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Santos', 'Carlos', 'Mendoza', '1988-11-08', 'trainer@anihan.local', 'ROLE_TRAINER');

UPDATE users u
JOIN desired_demo_accounts d ON d.username = u.username
SET u.password = d.password,
    u.lastname = d.lastname,
    u.firstname = d.firstname,
    u.middlename = d.middlename,
    u.birthdate = d.birthdate,
    u.age = TIMESTAMPDIFF(YEAR, d.birthdate, CURDATE()),
    u.email = d.email,
    u.role = d.role,
    u.enabled = 1,
    u.password_changed_at = NULL,
    u.security_locked = 0,
    u.failed_security_attempts = 0,
    u.security_lockout_started_at = NULL;

INSERT INTO users (username, password, lastname, firstname, middlename, birthdate, age,
                   email, role, enabled, password_changed_at, security_locked,
                   failed_security_attempts, security_lockout_started_at)
SELECT d.username, d.password, d.lastname, d.firstname, d.middlename, d.birthdate,
       TIMESTAMPDIFF(YEAR, d.birthdate, CURDATE()), d.email, d.role, 1, NULL, 0, 0, NULL
FROM desired_demo_accounts d
LEFT JOIN users u ON u.username = d.username
WHERE u.user_id IS NULL;

DROP TEMPORARY TABLE desired_demo_accounts;
COMMIT;

-- Readiness: exactly 3 rows; security_answer_count must be 2 before rehearsal.
SELECT u.user_id, u.username, u.role, u.enabled, u.security_locked,
       COUNT(a.answer_id) AS security_answer_count
FROM users u
LEFT JOIN user_security_answers a ON a.user_id = u.user_id
WHERE u.username IN ('admin', 'registrar', 'trainer')
GROUP BY u.user_id, u.username, u.role, u.enabled, u.security_locked
ORDER BY u.username;
```

- [ ] **Step 4: Run GREEN and commit**

Run: `./gradlew test --tests "com.example.springboot.SchemaContractTest"`

Expected: PASS.

```bash
git add src/test/java/com/example/springboot/SchemaContractTest.java src/main/sql/seed-accounts.sql
git commit -m "chore: add safe demo account reset"
```

---

### Task 5: Apply to MySQL, Rehearse the Presentation, and Record Evidence

**Files:**
- Modify: `memory-bank/bugs.md`
- Modify: `memory-bank/activeContext.md`
- Modify: `memory-bank/progress.md`
- Modify: `memory-bank/changeLog.md`
- Modify: `memory-bank/testing.md`

**Interfaces:**
- Consumes: Tasks 1–4 and the Docker container `mysql-server` documented in `README.md`.
- Produces: a backed-up, migrated, login-ready database plus reproducible test evidence.

- [ ] **Step 1: Verify prerequisites and take a recoverable backup**

Run in PowerShell:

```powershell
docker ps --filter "name=mysql-server"
New-Item -ItemType Directory -Force 'C:\tmp\anihan-client-demo' | Out-Null
docker exec mysql-server mysqldump -uroot -pmy_password --databases AnihanSRMS --result-file=/tmp/pre-readiness.sql
docker cp mysql-server:/tmp/pre-readiness.sql 'C:\tmp\anihan-client-demo\pre-readiness.sql'
Get-Item 'C:\tmp\anihan-client-demo\pre-readiness.sql' | Select-Object FullName,Length
```

Expected: `mysql-server` is Up and the backup length is greater than zero. Stop before mutation if either check fails.

- [ ] **Step 2: Apply migration and account reset**

Run the seed preflight `SELECT` alone first; expected zero rows. Then:

```powershell
docker cp 'src/main/sql/migrations/2026-09-22-widen-documents-file-type.sql' mysql-server:/tmp/widen-documents-file-type.sql
docker exec mysql-server sh -c "mysql -uroot -pmy_password < /tmp/widen-documents-file-type.sql"
docker cp 'src/main/sql/seed-accounts.sql' mysql-server:/tmp/seed-accounts.sql
docker exec mysql-server sh -c "mysql -uroot -pmy_password < /tmp/seed-accounts.sql"
```

Expected: `varchar(100)`, three canonical users, `enabled=1`, `security_locked=0`, and `security_answer_count=2`. If any count is below two, log in once and complete setup before continuing; do not seed shared answers.

- [ ] **Step 3: Run automated verification**

```bash
./gradlew clean test
```

Expected: BUILD SUCCESSFUL, zero failures. Record the exact test count from the generated report; do not reuse the historical 385 count.

- [ ] **Step 4: Run the presentation smoke matrix**

Start the app with `./gradlew bootRun`, then verify:

| Role/area | Evidence required |
|---|---|
| Public | `/js/does-not-exist.js` returns 404 JSON, not 500. |
| Admin | `admin/password123` reaches `admin.html`; users and logs load. |
| Registrar/classes | Login succeeds; years are unique; each filter change sends one request; All Years and explicit year both work. |
| Registrar/documents | Upload and download one DOCX and one XLSX; neither returns 409/truncation. Remove the two rehearsal documents afterward. |
| Trainer | `trainer/password123` reaches `trainer-classes.html`; year filtering works and grade modal opens. Do not save sample grades. |

Expected: browser console has no new error and all temporary document rows are removed.

- [ ] **Step 5: Update memory with exact evidence**

Record:

- `bugs.md`: Bugs 12 and 13 fixed on 2026-09-22, including regression test names.
- `activeContext.md`: branch, live migration status, backup path, and any unresolved presentation risk.
- `progress.md`: Tasks 1–5 completed and exact suite count.
- `changeLog.md`: every modified source/test/SQL file and its purpose.
- `testing.md`: commands, exact pass counts, MySQL verification, role smoke matrix, and cleanup result.

- [ ] **Step 6: Commit documentation and run the final gate**

```bash
git add memory-bank/bugs.md memory-bank/activeContext.md memory-bank/progress.md memory-bank/changeLog.md memory-bank/testing.md
git commit -m "docs: record client presentation readiness"
./gradlew test
git status --short
```

Expected: tests pass; status is clean. Then run the execution skill's required whole-branch review and `superpowers:finishing-a-development-branch`.
