# Add Account Feature — Session Summary

**Branch:** `feature/add-account`  
**Date:** April 16, 2026  
**Status:** Complete — verified working via browser

---

## Overview

This session implemented two features on the `feature/add-account` branch:

1. **Temporary DataSeeder** — Created and later deleted after verification
2. **Admin Create User Feature** — Permanent admin-only page to create new user accounts

---

## 1. Temporary DataSeeder (Created → Verified → Deleted)

A temporary `DataSeeder.java` (`CommandLineRunner`) was created to seed three initial accounts into the empty `users` table:

| Account | Username | Password | Role |
|---------|----------|----------|------|
| Admin | `admin` | `password123` | `ROLE_ADMIN` |
| Registrar | `registrar` | `password123` | `ROLE_REGISTRAR` |
| Trainer | `trainer` | `password123` | `ROLE_TRAINER` |

- Used `existsByUsername()` guard to skip duplicates on restart
- Passwords BCrypt-hashed via `PasswordEncoder`
- **Deleted** after login was confirmed working — seeded rows remain in database

### Database Migrations Executed During Seeder Setup

Two columns were missing from the live MySQL `users` table and were added via Docker exec:

```sql
ALTER TABLE AnihanSRMS.users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE AnihanSRMS.users ADD COLUMN password_changed_at DATETIME NULL;
```

The `system_logs` table was also created:

```sql
CREATE TABLE IF NOT EXISTS AnihanSRMS.system_logs (
    log_id      INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id     INT NULL,
    username    VARCHAR(255) NOT NULL,
    role        VARCHAR(15) NOT NULL,
    action      VARCHAR(500) NOT NULL,
    ip_address  VARCHAR(45) NULL,
    timestamp   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_system_logs_timestamp (timestamp DESC)
);
```

---

## 2. Admin Create User Feature

### Files Created

| File | Purpose |
|------|---------|
| `dto/AdminCreateUserRequest.java` | Validated DTO for user creation (username, password, role required; personal details optional with defaults) |
| `static/add-user.html` | New admin page styled identically to `edit-user.html` using existing `dashboard.css` classes |
| `static/js/admin-add-user.js` | Form submission JS — POST to API, client-side validation, redirect on success |

### Files Modified

| File | Change |
|------|--------|
| `service/AdminService.java` | Added `createUser(AdminCreateUserRequest)` method with duplicate checks, defaults, BCrypt hashing |
| `controller/AdminController.java` | Added `POST /api/admin/users` endpoint with system log integration |
| `config/SecurityConfig.java` | Added `/add-user.html` to admin-only HTML page matchers |
| `static/admin.html` | Added "Add Account" button in surface-card-header; added closeable success alert div |
| `static/js/admin-users.js` | Added `?created=true` URL param check to show success alert on redirect |

### Files Deleted

| File | Reason |
|------|--------|
| `config/DataSeeder.java` | Temporary — deleted after seeded accounts were verified working |

### New API Endpoint

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/admin/users` | `ROLE_ADMIN` | Creates a new user account. Returns `201 Created` with `AdminUserResponse`. |

### Request Body — `AdminCreateUserRequest`

| Field | Required | Validation | Default |
|-------|----------|-----------|---------|
| `username` | ✅ | `@NotBlank`, no spaces | — |
| `password` | ✅ | `@NotBlank`, min 8 chars | — |
| `role` | ✅ | `ROLE_ADMIN`, `ROLE_REGISTRAR`, or `ROLE_TRAINER` | — |
| `lastName` | ❌ | — | `"User"` |
| `firstName` | ❌ | — | `"New"` |
| `middleName` | ❌ | — | `"N/A"` |
| `email` | ❌ | `@Email` | `"user@anihan.local"` |
| `birthdate` | ❌ | `@PastOrPresent` | `2000-01-01` |
| `age` | ❌ | 1–150 | `25` |

### UI Flow

1. Admin clicks **"Add Account"** button on admin dashboard (`admin.html`)
2. Navigates to `add-user.html` — form pre-filled with safe defaults
3. Admin fills in username, password, role (and optionally edits personal details)
4. On submit → `POST /api/admin/users` → on success → redirect to `admin.html?created=true`
5. Admin dashboard shows closeable green **"Account created successfully"** alert
6. DataTable auto-refreshes to include the new user

### Design Decisions

- **No new CSS** — all styling reuses existing `dashboard.css` classes (`form-shell`, `form-panel`, `form-note`, `surface-card`, `page-hero`, etc.)
- **Age stays manual** — no auto-calculation from birthdate was added, matching existing codebase patterns
- **Simple password policy** — 8-char minimum for admin-created accounts (matches existing admin password reset policy)
- **Duplicate guards** — both username and email are checked for uniqueness before creation
- **System logging** — creation action is logged to `system_logs` via `SystemLogService`

### Verification

- `.\gradlew.bat build -x test` → **BUILD SUCCESSFUL**
- Browser test: logged in as admin, created a new account with defaults, success alert displayed, new user appeared in DataTable
- Seeded accounts (`admin`, `registrar`, `trainer`) remain in database and are functional
