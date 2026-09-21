# Remove Login/Logout Auditing + Admin Dashboard Statistics — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop writing "User logged in" / "User logged out" rows to `system_logs`, purge the ones already there, and remove the four statistic cards from the top of the admin dashboard.

**Architecture:** Three independent slices. (1) Backend — delete the two `systemLogService.logAction(...)` calls in `AuthController` and the now-unused `SystemLogService` dependency, pinned by a new regression test that fails today. (2) Frontend — delete the `hero-stats` block from `admin.html`, the `updateStats()` function from `admin-users.js`, and the five now-dead stat rules from `dashboard.css`. (3) Data — a guarded, idempotent SQL migration that deletes historical login/logout rows from the live database, applied only after a full backup.

**Tech Stack:** Java 25 / Spring Boot 4.0 / Spring Security 7, JUnit 5 + Mockito + `@WebMvcTest`, vanilla JS + Bootstrap 5.3 + DataTables 2, MySQL 8 (Docker), Gradle 9.4.1 (Kotlin DSL).

---

## Decisions Already Made (do not re-litigate)

| Question | Decision | Source |
|---|---|---|
| Historical login/logout rows in `system_logs` | **Purge them from the live DB** | User, this session |
| Admin hero layout after removing stats | **Full-width hero text** — unwrap `page-hero-grid` on admin.html only | User, this session |
| Dead stat CSS in `dashboard.css` | **Remove it** — verified used by admin.html only | User, this session |

> **Flagged risk, accepted by the user:** `system_logs` is documented as append-only in
> `memory-bank/decisions.md` ("2026-04-14 — Separate `system_logs` Table"). The purge in
> Task 7 is a deliberate, one-time exception. It is irreversible once the backup is
> discarded — **do not skip the backup step.**

---

## File Structure

| File | Action | Responsibility after the change |
|---|---|---|
| `src/main/java/com/example/springboot/controller/AuthController.java` | Modify | Authenticates, issues the session, returns identity. Writes **no** audit rows. |
| `src/test/java/com/example/springboot/controller/AuthControllerWebMvcTest.java` | **Create** | Pins the invariant: login and logout must never touch `SystemLogService`. |
| `src/test/java/com/example/springboot/service/SystemLogServiceTest.java` | Modify | Unit-tests `logAction` with an action string that still exists in the app. |
| `src/main/resources/static/admin.html` | Modify | Admin dashboard page — hero (title only) + user directory table + modals. |
| `src/main/resources/static/js/admin-users.js` | Modify | Admin user DataTable, details modal, delete flows. No stat computation. |
| `src/main/resources/static/css/dashboard.css` | Modify | Shared dashboard styling, minus the five unused stat rules. |
| `src/main/sql/migrations/2026-09-19-purge-login-logout-logs.sql` | **Create** | One-time, idempotent purge of historical login/logout audit rows. |
| `src/main/sql/backup-2026-09-19-pre-log-purge.sql` | **Create** (generated) | Full pre-purge `mysqldump`. The only route back if the purge is wrong. |
| `memory-bank/activeContext.md`, `progress.md`, `changeLog.md`, `decisions.md`, `testing.md` | Modify | Project memory. Mandatory per CLAUDE.md. |

**Not touched, deliberately:** `SystemLogService`, `SystemLogController`, `SystemLogExportService`,
`logs.html`, `system-logs.js`, and every other `logAction` call site (Admin, Account, Registrar,
Document, ClassManagement, StudentNumber, TrainerGrade, PasswordRecovery, SecurityQuestion
controllers). Only the two auth call sites go away. Password-reset and security-question-setup
logging stays — those are not login/logout events.

---

## Task 0: Create the feature branch

**Phase 0 of `.agents/rules/full-stack-anihan.md` is non-negotiable: no work happens on `main`.**

**Files:** none (git only)

- [ ] **Step 1: Confirm the current branch and a clean tree**

```bash
git branch --show-current
git status --short
```

Expected: `main`, and no output from `git status --short` (clean tree). If the tree is dirty, stop and ask the user what to do with the uncommitted work.

- [ ] **Step 2: Create and switch to the feature branch**

```bash
git checkout -b feature/remove-login-audit-and-admin-stats
```

- [ ] **Step 3: Confirm the switch took effect**

```bash
git branch --show-current
```

Expected: `feature/remove-login-audit-and-admin-stats`

---

## Task 1: Stop writing login/logout audit rows

**Files:**
- Test (create): `src/test/java/com/example/springboot/controller/AuthControllerWebMvcTest.java`
- Modify: `src/main/java/com/example/springboot/controller/AuthController.java` (imports, field, constructor, `login` ~line 100-105, `logout` ~line 116-133)

**Context for the implementer:** `AuthController` currently injects `SystemLogService` and calls
`logAction` in exactly two places — once at the end of `login()` and once inside `logout()`, wrapped
in a block whose only purpose is to capture the user's identity before the security context is
cleared. Removing the logging makes that whole capture block dead, and makes the `SystemLogService`
field unused. `userRepository` is still needed (login uses it for the security-question check, and
`/me` uses it for personal details), so it stays.

- [ ] **Step 1: Write the failing regression test**

Create `src/test/java/com/example/springboot/controller/AuthControllerWebMvcTest.java`:

```java
package com.example.springboot.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.repository.UserSecurityAnswerRepository;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SessionAuthenticationHelper;
import com.example.springboot.service.SystemLogService;

/**
 * Pins the 2026-09-19 decision that logging in and logging out are NOT audited.
 *
 * <p>The {@code verifyNoInteractions(systemLogService)} assertions are the point of
 * this class: if someone re-introduces a {@code logAction} call in {@code AuthController},
 * the mock gets injected again and these tests fail.
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSecurityAnswerRepository userSecurityAnswerRepository;

    @MockitoBean
    private SessionAuthenticationHelper sessionAuthenticationHelper;

    @MockitoBean
    private SystemLogService systemLogService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User seedAdmin() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("admin");
        user.setRole("ROLE_ADMIN");
        return user;
    }

    private void stubSuccessfulAuthentication() {
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(seedAdmin()));
        when(userSecurityAnswerRepository.countByUserUserId(1)).thenReturn(2L);
    }

    @Test
    void loginWritesNoSystemLogRow() throws Exception {
        stubSuccessfulAuthentication();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        verifyNoInteractions(systemLogService);
    }

    @Test
    void loginStillReturnsUsernameAndRole() throws Exception {
        stubSuccessfulAuthentication();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void logoutWritesNoSystemLogRow() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verifyNoInteractions(systemLogService);
    }
}
```

- [ ] **Step 2: Run the new test and watch it fail**

```bash
./gradlew test --tests "com.example.springboot.controller.AuthControllerWebMvcTest"
```

Expected: **FAIL**. `loginWritesNoSystemLogRow` and `logoutWritesNoSystemLogRow` both fail with
`org.mockito.exceptions.verification.NoInteractionsWanted: No interactions wanted here` pointing at
the `logAction` call. `loginStillReturnsUsernameAndRole` passes (it describes behaviour that must
survive the change).

- [ ] **Step 3: Remove the login log call**

In `src/main/java/com/example/springboot/controller/AuthController.java`, delete these six lines from
the end of `login()` (they sit between the `responseRole` if/else and the `return ResponseEntity.ok(...)`):

```java
        // Log the login action (against the account's real role, not the
        // temporary one, so the audit trail reads naturally)
        String ipAddress = httpRequest.getRemoteAddr();
        if (user != null) {
            systemLogService.logAction(user.getUserId(), username, realRole, "User logged in", ipAddress);
        }
```

The method now ends:

```java
        } else {
            sessionAuthenticationHelper.establishRestrictedSession(httpRequest, username, "PENDING_SETUP");
            responseRole = "ROLE_PENDING_SETUP";
        }

        return ResponseEntity.ok(Map.of(
                "username", username,
                "role", responseRole
        ));
    }
```

Leave `User user = ...` and `boolean setupComplete = ...` alone — `setupComplete` still drives the
branch above.

- [ ] **Step 4: Remove the logout log call and its now-dead identity capture**

Replace the whole body of `logout()` so the method reads exactly:

```java
    /**
     * POST /api/auth/logout
     * Invalidates the current session.
     *
     * <p>Logging out is deliberately NOT written to {@code system_logs} —
     * see memory-bank/decisions.md (2026-09-19).
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
```

- [ ] **Step 5: Drop the now-unused SystemLogService dependency**

Three edits in the same file:

1. Delete the import:

```java
import com.example.springboot.service.SystemLogService;
```

2. Delete the field:

```java
    private final SystemLogService systemLogService;
```

3. Rewrite the constructor to the 4-argument form:

```java
    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          UserSecurityAnswerRepository userSecurityAnswerRepository,
                          SessionAuthenticationHelper sessionAuthenticationHelper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userSecurityAnswerRepository = userSecurityAnswerRepository;
        this.sessionAuthenticationHelper = sessionAuthenticationHelper;
    }
```

Keep every other import. `GrantedAuthority`, `Authentication`, `SecurityContextHolder`, and
`HttpSession` are all still used by `login()` and `/me`.

- [ ] **Step 6: Confirm no stray references remain**

```bash
grep -n "systemLogService\|SystemLogService" src/main/java/com/example/springboot/controller/AuthController.java
```

Expected: **no output.**

- [ ] **Step 7: Run the test again and watch it pass**

```bash
./gradlew test --tests "com.example.springboot.controller.AuthControllerWebMvcTest"
```

Expected: PASS — 3 tests, 0 failures.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/springboot/controller/AuthController.java src/test/java/com/example/springboot/controller/AuthControllerWebMvcTest.java
git commit -m "feat: stop auditing login and logout in system_logs"
```

---

## Task 2: Stop the log-service unit test from referencing a removed action

**Files:**
- Modify: `src/test/java/com/example/springboot/service/SystemLogServiceTest.java:42` and `:51`

**Context:** `SystemLogServiceTest.logActionSavesSystemLog` uses `"User logged in"` as its sample
action string. The test is correct and must keep passing — it tests `logAction` generically, not the
auth flow — but leaving that string in the suite implies an audit action that no longer exists.
Swap it for one the app really writes (`AdminController` logs "Reset password for: …").

- [ ] **Step 1: Change the action string in the arrange step**

Line 42, from:

```java
        systemLogService.logAction(1, "admin", "ROLE_ADMIN", "User logged in", "127.0.0.1");
```

to:

```java
        systemLogService.logAction(1, "admin", "ROLE_ADMIN", "Reset password for: registrar", "127.0.0.1");
```

- [ ] **Step 2: Change the matching assertion**

Line 51, from:

```java
        assertEquals("User logged in", saved.getAction());
```

to:

```java
        assertEquals("Reset password for: registrar", saved.getAction());
```

- [ ] **Step 3: Run the test class**

```bash
./gradlew test --tests "com.example.springboot.service.SystemLogServiceTest"
```

Expected: PASS — 10 tests, 0 failures.

- [ ] **Step 4: Confirm the phrase is gone from the whole Java tree**

```bash
grep -rn "User logged in\|User logged out" src/main/java src/test/java
```

Expected: **no output.**

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/example/springboot/service/SystemLogServiceTest.java
git commit -m "test: use a live action string in SystemLogService unit test"
```

---

## Task 3: Remove the statistic cards from admin.html

**Files:**
- Modify: `src/main/resources/static/admin.html:79-111` (the `page-hero` section) and `:450` (cache-buster)

**Context:** The hero is a two-column CSS grid — title/subtitle on the left, four stat cards on the
right. The user chose full-width hero text, so the `page-hero-grid` wrapper and the inner `<div>`
that holds the text both go away along with the cards. `page-hero-grid` is used by 13 other pages,
so **only the admin.html usage is removed — the CSS rule itself stays.**

- [ ] **Step 1: Replace the whole hero section**

Replace lines 79–111 (from `<section class="page-hero">` through its closing `</section>`) with:

```html
            <section class="page-hero">
                <span class="page-eyebrow">Administration</span>
                <h1 class="page-title">Admin Dashboard</h1>
                <p class="page-subtitle">
                    Account creation, Edit accounts, Delete accounts, System logs.
                </p>
            </section>
```

That removes: the `page-hero-grid` wrapper, its inner text `<div>`, the `hero-stats` container, and
all four `stat-card` articles (`totalUsersStat`, `adminUsersStat`, `registrarUsersStat`,
`trainerUsersStat`).

- [ ] **Step 2: Bump the JS cache-buster**

Line 450, from:

```html
    <script src="js/admin-users.js?v=2"></script>
```

to:

```html
    <script src="js/admin-users.js?v=3"></script>
```

This matters — the file is edited in Task 4, and stale-JS caching has bitten this project before
(see `changeLog.md`, 2026-04-29).

- [ ] **Step 3: Verify no stat markup survives**

```bash
grep -n "hero-stats\|stat-card\|stat-label\|stat-value\|stat-caption\|UsersStat" src/main/resources/static/admin.html
```

Expected: **no output.**

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/admin.html
git commit -m "feat: remove statistic cards from the admin dashboard hero"
```

---

## Task 4: Remove the stat computation from admin-users.js

**Files:**
- Modify: `src/main/resources/static/js/admin-users.js:75-80` (the function) and `:274` (the call site)

**Context:** `updateStats(users)` is called from the DataTable's `dataSrc` callback, which must still
return `json` for the table to render. Only the `updateStats(json);` line is removed from it. The
`setText` helper at line 47 stays — the details modal uses it in ten other places.

- [ ] **Step 1: Delete the `updateStats` function**

Remove lines 75–80 entirely:

```javascript
    function updateStats(users) {
        setText('totalUsersStat', users.length);
        setText('adminUsersStat', users.filter(user => user.role === 'ROLE_ADMIN').length);
        setText('registrarUsersStat', users.filter(user => user.role === 'ROLE_REGISTRAR').length);
        setText('trainerUsersStat', users.filter(user => user.role === 'ROLE_TRAINER').length);
    }
```

- [ ] **Step 2: Remove the call site, keeping the DataTable contract intact**

Around line 270-277, change:

```javascript
        var dataTable = window.jQuery('#usersTable').DataTable({
            ajax: {
                url: '/api/admin/users',
                dataSrc: function (json) {
                    updateStats(json);
                    return json;
                }
            },
```

to:

```javascript
        var dataTable = window.jQuery('#usersTable').DataTable({
            ajax: {
                url: '/api/admin/users'
            },
```

DataTables defaults `dataSrc` to the root array when the response is a bare JSON array, which is
what `GET /api/admin/users` returns — so dropping the callback is safe and is the simpler form.

- [ ] **Step 3: Verify no references survive**

```bash
grep -n "updateStats\|UsersStat" src/main/resources/static/js/admin-users.js
```

Expected: **no output.**

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/js/admin-users.js
git commit -m "refactor: drop admin user stat computation"
```

---

## Task 5: Remove the dead stat rules from dashboard.css

**Files:**
- Modify: `src/main/resources/static/css/dashboard.css:108-145` (five rule blocks), `:705-707` (991px override), `:730-733` (767px override)

**Context:** Verified before writing this plan — `.hero-stats`, `.stat-card`, `.stat-label`,
`.stat-value`, and `.stat-caption` appear in exactly one HTML file (`admin.html`), which Task 3
emptied. `.page-hero-grid`, `.page-eyebrow`, `.page-title`, `.page-subtitle`, and `.detail-grid`
are shared with other pages and **must be kept.**

- [ ] **Step 1: Delete the five main rule blocks**

Remove this contiguous run (between the `.page-subtitle` rule and the `.surface-card` rule):

```css
.hero-stats {
    display: grid;
    gap: 0.9rem;
    grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stat-card {
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(212, 237, 218, 0.58));
    border: 1px solid rgba(40, 167, 69, 0.14);
    border-radius: 1rem;
    min-height: 7rem;
    padding: 1rem 1.1rem;
}

.stat-label {
    color: var(--color-text-muted);
    display: block;
    font-size: 0.82rem;
    font-weight: 600;
    letter-spacing: 0.04em;
    margin-bottom: 0.55rem;
    text-transform: uppercase;
}

.stat-value {
    color: var(--color-text-dark);
    display: block;
    font-size: 2rem;
    font-weight: 700;
    line-height: 1;
}

.stat-caption {
    color: var(--color-text-muted);
    display: block;
    font-size: 0.84rem;
    margin-top: 0.55rem;
}
```

- [ ] **Step 2: Remove the `.hero-stats` override in the 991px media query**

Inside `@media (max-width: 991px)`, delete only this rule:

```css
    .hero-stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
```

Leave the `.page-hero-grid, .form-shell { grid-template-columns: 1fr; }` rule above it untouched —
13 other pages depend on it.

- [ ] **Step 3: Narrow the shared selector in the 767px media query**

Inside `@media (max-width: 767px)`, change:

```css
    .hero-stats,
    .detail-grid {
        grid-template-columns: 1fr;
    }
```

to:

```css
    .detail-grid {
        grid-template-columns: 1fr;
    }
```

- [ ] **Step 4: Verify the stat rules are gone and the shared ones remain**

```bash
grep -n "hero-stats\|stat-card\|stat-label\|stat-value\|stat-caption" src/main/resources/static/css/dashboard.css
grep -c "page-hero-grid\|detail-grid" src/main/resources/static/css/dashboard.css
```

Expected: the first command prints **nothing**; the second prints a non-zero count.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/css/dashboard.css
git commit -m "chore: remove now-unused stat card styles"
```

---

## Task 6: Write the purge migration

**Files:**
- Create: `src/main/sql/migrations/2026-09-19-purge-login-logout-logs.sql`

**Context:** This task only *writes* the file. Task 7 runs it. Keeping them apart means the SQL can
be reviewed before it touches a real database.

- [ ] **Step 1: Create the migration file**

```sql
-- ============================================================
-- Migration: 2026-09-19 — Purge historical login/logout audit rows
-- ============================================================
-- As of 2026-09-19 the application no longer writes "User logged in" or
-- "User logged out" rows (AuthController's two logAction calls were removed).
-- This migration removes the rows written before that change, so the admin
-- Audit Logs page shows only meaningful record/account actions.
--
-- DELIBERATE EXCEPTION TO AN ESTABLISHED RULE:
--   memory-bank/decisions.md (2026-04-14) documents system_logs as
--   APPEND-ONLY — never updated, never deleted. This is a one-time,
--   user-approved exception, scoped to exactly two action strings. It is
--   NOT a precedent. Take the backup named in the header below before
--   running this; there is no other way back.
--
-- Scope: DATA ONLY. No table, column, index, or constraint is touched, so
-- no `schema.sql` change is needed and `ddl-auto=validate` is unaffected.
--
-- AUTO_INCREMENT on system_logs.log_id is intentionally NOT reset — the
-- resulting gaps in log_id are the visible evidence that rows were removed.
--
-- Idempotent: re-running deletes nothing further (step 2 matches no rows
-- the second time) and re-prints the same zero count.
--
-- Backup taken before the first run:
--   src/main/sql/backup-2026-09-19-pre-log-purge.sql
-- ============================================================

USE AnihanSRMS;

-- 1. Pre-flight: how many rows are about to be removed, and what else is there
SELECT COUNT(*) AS login_logout_rows_before
FROM system_logs
WHERE action IN ('User logged in', 'User logged out');

SELECT COUNT(*) AS total_rows_before FROM system_logs;

-- 2. The purge
DELETE FROM system_logs
WHERE action IN ('User logged in', 'User logged out');

-- 3. Verification: the first count MUST be 0; the second is the survivors
SELECT COUNT(*) AS login_logout_rows_after
FROM system_logs
WHERE action IN ('User logged in', 'User logged out');

SELECT COUNT(*) AS total_rows_after FROM system_logs;

-- 4. Sanity: the distinct actions that remain should contain no login/logout entry
SELECT action, COUNT(*) AS occurrences
FROM system_logs
GROUP BY action
ORDER BY occurrences DESC;
```

- [ ] **Step 2: Confirm the file was written where the other migrations live**

```bash
ls src/main/sql/migrations/ | grep purge
```

Expected: `2026-09-19-purge-login-logout-logs.sql`

- [ ] **Step 3: Commit**

```bash
git add src/main/sql/migrations/2026-09-19-purge-login-logout-logs.sql
git commit -m "chore: add migration purging historical login/logout audit rows"
```

---

## Task 7: Back up the live database, then apply the purge

**Files:**
- Create (generated, untracked): `src/main/sql/backup-2026-09-19-pre-log-purge.sql`

**Context:** The live database is MySQL 8 in a Docker container named `mysql-server`, database
`AnihanSRMS`, credentials `root / my_password`. Every prior session in this project takes the backup
*first* — follow that. **The backup step is not optional and not skippable.**

- [ ] **Step 1: Confirm the container is running**

```bash
docker ps --filter "name=mysql-server" --format "{{.Names}} {{.Status}}"
```

Expected: a line naming `mysql-server` with an `Up …` status. If there is no output, start the
container before continuing — do not proceed against a database you cannot see.

- [ ] **Step 2: Take the full backup**

```bash
docker exec mysql-server mysqldump -uroot -pmy_password --databases AnihanSRMS --routines --triggers > src/main/sql/backup-2026-09-19-pre-log-purge.sql
```

- [ ] **Step 3: Verify the backup is real before deleting anything**

```bash
wc -c src/main/sql/backup-2026-09-19-pre-log-purge.sql
grep -c "CREATE TABLE" src/main/sql/backup-2026-09-19-pre-log-purge.sql
grep -c "INSERT INTO .system_logs." src/main/sql/backup-2026-09-19-pre-log-purge.sql
```

Expected: a size around 100,000+ bytes; `21` CREATE TABLE statements; at least `1` system_logs
INSERT. **If any of these look wrong, STOP — do not run the purge.**

- [ ] **Step 4: Record the pre-purge counts**

```bash
docker exec mysql-server mysql -uroot -pmy_password AnihanSRMS -e "SELECT COUNT(*) AS total FROM system_logs; SELECT COUNT(*) AS login_logout FROM system_logs WHERE action IN ('User logged in','User logged out');"
```

Write both numbers down — Step 5 and Step 6 check against them.

- [ ] **Step 5: Apply the migration**

```bash
docker exec -i mysql-server mysql -uroot -pmy_password < src/main/sql/migrations/2026-09-19-purge-login-logout-logs.sql
```

Expected output includes `login_logout_rows_after` = `0`, and `total_rows_after` equal to
`total_rows_before` minus `login_logout_rows_before` from Step 4.

- [ ] **Step 6: Re-run the migration to prove it is idempotent**

```bash
docker exec -i mysql-server mysql -uroot -pmy_password < src/main/sql/migrations/2026-09-19-purge-login-logout-logs.sql
```

Expected: `login_logout_rows_before` is now `0`, `total_rows_after` is unchanged from Step 5, and
the action breakdown in section 4 is identical. Nothing further is deleted.

- [ ] **Step 7: Confirm the surviving audit trail is intact**

```bash
docker exec mysql-server mysql -uroot -pmy_password AnihanSRMS -e "SELECT action, COUNT(*) FROM system_logs GROUP BY action ORDER BY 2 DESC LIMIT 15;"
```

Expected: account/record/document actions still present (e.g. "Created account: …", "Updated
student record: …"); **no** row reading "User logged in" or "User logged out".

> **Note:** the backup file is generated output, not source. Leave it untracked unless the user
> asks otherwise — previous backups in `src/main/sql/` follow the same convention.

---

## Task 8: Full verification

**Files:** none modified — this task only runs and observes.

- [ ] **Step 1: Run the complete backend test suite**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, **377 tests, 0 failures, 0 errors** (374 baseline + 3 new
`AuthControllerWebMvcTest` tests). If the count differs, reconcile before continuing — do not
proceed on a red or unexplained suite.

- [ ] **Step 2: Boot the app against the live database**

```bash
./gradlew bootRun
```

Expected: `Started SpringbootApplication in …`, no `Schema-validation` or
`SchemaManagementException` lines. This is a data-only change, so validation should be unaffected —
this run also gives you a live server for Steps 3-6. Leave it running.

- [ ] **Step 3: Verify a real login writes no log row**

In a second terminal, note the current row count, log in, then re-count:

```bash
docker exec mysql-server mysql -uroot -pmy_password AnihanSRMS -e "SELECT COUNT(*) FROM system_logs;"
curl -s -c /tmp/anihan-cookies.txt -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"password123\"}"
docker exec mysql-server mysql -uroot -pmy_password AnihanSRMS -e "SELECT COUNT(*) FROM system_logs;"
```

Expected: the login returns `{"username":"admin","role":"ROLE_ADMIN"}` (or `ROLE_PENDING_SETUP` if
that account has not completed security-question setup — either is fine here), and **the two counts
are identical.**

- [ ] **Step 4: Verify a real logout writes no log row**

```bash
curl -s -b /tmp/anihan-cookies.txt -X POST http://localhost:8080/api/auth/logout
docker exec mysql-server mysql -uroot -pmy_password AnihanSRMS -e "SELECT COUNT(*) FROM system_logs;"
```

Expected: `{"message":"Logged out successfully"}` and, again, **an unchanged count.**

- [ ] **Step 5: Verify a still-audited action DID log (proves the rest of auditing survives)**

Log in as admin in the browser at `http://localhost:8080/index.html`, open **Edit Account**, change
your own personal details, and save. Then:

```bash
docker exec mysql-server mysql -uroot -pmy_password AnihanSRMS -e "SELECT action, username, timestamp FROM system_logs ORDER BY log_id DESC LIMIT 3;"
```

Expected: a fresh `Updated own personal details` row. This is the check that the change was
surgical rather than breaking `SystemLogService` wholesale.

- [ ] **Step 6: Browser walkthrough of the admin dashboard**

At `http://localhost:8080/admin.html`, hard-refresh (Ctrl+Shift+R) and confirm:

- No stat cards anywhere on the page.
- The "Admin Dashboard" title, eyebrow, and subtitle span the full width of the hero card, with no empty gap to the right.
- The User Directory DataTable still loads every user, with role pills and status badges rendering.
- The details modal still opens from a row and fills in every field (this exercises the `setText` helper that was kept).
- The browser console is clean — in particular no `updateStats is not defined` and no `Cannot set properties of null`.
- Check the layout at 1920px, 1400px, 991px, and 767px widths; the hero must not collapse oddly at any of them.
- `logs.html` still loads, and its table contains no login/logout entries.

- [ ] **Step 7: Stop the server**

Ctrl+C the `bootRun` terminal.

---

## Task 9: Update project memory and commit

**Files:**
- Modify: `memory-bank/activeContext.md`, `memory-bank/progress.md`, `memory-bank/changeLog.md`, `memory-bank/decisions.md`, `memory-bank/testing.md`

**Context:** CLAUDE.md marks this step **not optional** — the memory bank is the project's source of
truth for state. Write for a developer with no prior context.

- [ ] **Step 1: Add a decision record for the audit removal**

Add to the top of `memory-bank/decisions.md`, above the 2026-09-19 ID-picture entry:

```markdown
## 2026-09-19 - Login/Logout Are Not Audited, and Historical Rows Were Purged

**Decision:** `AuthController` no longer writes `system_logs` rows for login or logout, and the
rows written before this change were deleted from the live database via
`src/main/sql/migrations/2026-09-19-purge-login-logout-logs.sql`.

**Why:** Session events dominated the audit log by volume while carrying the least investigative
value — the log exists to answer "who changed this record", and every login row pushed those
answers further down the page and out of the default 7-day window.

**The rule this breaks, knowingly:** the 2026-04-14 decision below establishes `system_logs` as
append-only. The purge is a one-time, user-approved exception scoped to exactly two action
strings, taken with a full backup (`src/main/sql/backup-2026-09-19-pre-log-purge.sql`). It is not
a precedent — every other action remains append-only.

**Alternative rejected:** filtering the two actions out at query time in `SystemLogService`. It
would have preserved the rows, but it adds permanent filter logic to the read path and to the
export, and it leaves the table growing with data nobody is allowed to see.

**Pinned by test:** `AuthControllerWebMvcTest` mocks `SystemLogService` and asserts
`verifyNoInteractions` on both endpoints, so re-introducing the logging fails the build.
```

- [ ] **Step 2: Add a decision record for the stats removal**

Immediately below the entry from Step 1:

```markdown
## 2026-09-19 - Admin Dashboard Stat Cards Removed

**Decision:** The four hero stat cards on `admin.html` (Total Users / Admins / Registrars /
Trainers) were removed, along with `updateStats()` in `admin-users.js` and the five now-unused
stat rules in `dashboard.css`. The hero title now spans the full card width.

**Why:** The counts restated what the User Directory table below them already showed, and they
were computed client-side from the full user list the table had already fetched — so they carried
no information the reader did not have two inches lower on the page.

**Scope note:** `.page-hero-grid` was removed from `admin.html`'s markup but the CSS rule stays —
13 other pages still use it. Only the stat-specific rules were deleted.
```

- [ ] **Step 3: Rewrite the Current Phase and add a session entry in `activeContext.md`**

Replace the `## Current Phase` block at the top with:

```markdown
## Current Phase
**Login/logout auditing removed and historical rows purged; admin dashboard stat cards removed —
branch green at 377 tests (was 374), live login/logout verified to write no rows**

## Active Branch
`feature/remove-login-audit-and-admin-stats` (user-approved, branched from `main`)

## Open Items (as of 2026-09-19, remove-login-audit session)
- PR to `main` — user approval required.
- The live `system_logs` purge is **irreversible** once
  `src/main/sql/backup-2026-09-19-pre-log-purge.sql` is discarded. Keep that backup until the
  change has been accepted in use.
- Bugs 12 and 13 in `bugs.md` (documents.file_type too short; GlobalExceptionHandler 500s on
  missing routes) remain open — untouched by this session.
```

Then insert a `## Latest Session (2026-09-19 — Remove Login Auditing + Admin Stats)` block above
the previous "Latest Session" heading (demote that one to "Previous Session"), covering: scope, the
files changed in each of the three slices, the pre- and post-purge row counts recorded in Task 7,
and the Task 8 verification results.

- [ ] **Step 4: Add a `progress.md` entry**

Add at the top of `## Recent Sessions (detail)`:

```markdown
### Remove Login/Logout Auditing + Admin Dashboard Statistics (Completed - September 19, 2026)
- **Task:** Stop auditing login/logout in `system_logs`, purge the historical rows, and remove the
  four statistic cards from the top of the admin dashboard.
- **Backend:** `AuthController` lost both `logAction` calls, the dead identity-capture block in
  `logout()`, and its `SystemLogService` dependency (constructor 5 → 4 params). New
  `AuthControllerWebMvcTest` (3 tests) pins the invariant with `verifyNoInteractions`.
  `SystemLogServiceTest`'s sample action string changed from `"User logged in"` to a string the app
  still writes.
- **Frontend:** `admin.html` hero simplified to full-width text (stat cards + `page-hero-grid`
  wrapper removed, JS cache-buster `?v=2` → `?v=3`); `updateStats()` and its `dataSrc` call removed
  from `admin-users.js`; five stat rules plus two responsive overrides removed from `dashboard.css`
  (verified used by `admin.html` only).
- **Data:** `2026-09-19-purge-login-logout-logs.sql` — idempotent, data-only, deletes rows whose
  action is `'User logged in'` or `'User logged out'`. Applied to live MySQL after a full backup
  (`backup-2026-09-19-pre-log-purge.sql`), then re-run to prove idempotency. **A deliberate,
  user-approved exception to the append-only rule** — recorded in `decisions.md`.
- **Verified:** `./gradlew test` → **377 tests, 0 failures** (was 374). Live: a real login and a
  real logout each left the `system_logs` count unchanged; an "Updated own personal details" action
  still logged, proving the rest of auditing survives. Browser walkthrough of `admin.html` at
  1920/1400/991/767px with a clean console.
- **No schema change** — data only; `ddl-auto=validate` unaffected.
- **Branch:** `feature/remove-login-audit-and-admin-stats`. Open: PR to `main`.
```

- [ ] **Step 5: Add a `changeLog.md` entry**

Add at the very top, following the existing format: a `## 2026-09-19 - Remove Login/Logout Auditing
+ Admin Dashboard Statistics` heading with the branch name, a **Task** paragraph, a **Files
Modified** table (one row per file, with what changed and why), a **Files Created** table
(`AuthControllerWebMvcTest.java`, the purge migration, the backup), a **Live DB Changes** section
with the actual before/after row counts from Task 7, and a **Verification** section with the test
count and the live-verification results from Task 8.

- [ ] **Step 6: Update `testing.md`**

Add a row to the test-suite table:

```markdown
| `AuthControllerWebMvcTest` | WebMvc | 3 | Login writes no `system_logs` row, logout writes no row (both via `verifyNoInteractions` on a mocked `SystemLogService`), login still returns username + role |
```

Update the "Latest full-suite result" line to **377 tests, 0 failures, 0 errors** (2026-09-19, after
the remove-login-audit session; was 374 — 3 new tests). Then add a short
`## Live Verification — 2026-09-19 (Login Auditing Removed)` section recording the unchanged
`system_logs` counts across a real login and logout, the still-logging control action, and the
admin.html browser walkthrough.

- [ ] **Step 7: Commit the memory-bank updates**

```bash
git add memory-bank/
git commit -m "docs: record login-audit removal and admin stats removal"
```

- [ ] **Step 8: Confirm the branch is clean and review the full diff**

```bash
git status --short
git diff main --stat
```

Expected: no output from the first command. The second lists exactly: `AuthController.java`,
`AuthControllerWebMvcTest.java`, `SystemLogServiceTest.java`, `admin.html`, `admin-users.js`,
`dashboard.css`, the purge migration, and the five memory-bank files.

---

## Done Criteria

- [ ] `grep -rn "User logged in\|User logged out" src/main/java src/test/java` prints nothing
- [ ] `AuthController` has no reference to `SystemLogService`
- [ ] `./gradlew test` → 377 tests, 0 failures, 0 errors
- [ ] A real login and a real logout against the running app leave `SELECT COUNT(*) FROM system_logs` unchanged
- [ ] A non-auth action (edit own personal details) still writes a `system_logs` row
- [ ] `SELECT COUNT(*) FROM system_logs WHERE action IN ('User logged in','User logged out')` returns 0 on the live DB
- [ ] The purge migration is idempotent (second run changes nothing)
- [ ] `src/main/sql/backup-2026-09-19-pre-log-purge.sql` exists and contains 21 `CREATE TABLE` statements
- [ ] `admin.html` shows no stat cards; the hero title spans full width at 1920/1400/991/767px
- [ ] Admin browser console is clean; the User Directory table and details modal still work
- [ ] All five memory-bank files updated and committed
- [ ] `main` was never committed to directly
