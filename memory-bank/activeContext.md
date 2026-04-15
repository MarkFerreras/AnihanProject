# Active Context - Anihan SRMS

## Current Phase
**Add Account Feature — Complete, Verified**

## Active Branch
`feature/add-account`

## Status (April 16, 2026)
All code implemented and verified. `./gradlew build -x test` passes. Browser testing confirmed — admin can create new user accounts. DataSeeder created, used, and deleted.

### What Was Built
- **Backend**: `AdminCreateUserRequest` DTO, `createUser()` in `AdminService`, `POST /api/admin/users` in `AdminController`
- **Security**: `/add-user.html` restricted to ADMIN role in `SecurityConfig`
- **Frontend**: `add-user.html` page with form-shell layout, `admin-add-user.js` for AJAX submission
- **Dashboard**: "Add Account" button in admin.html surface-card-header, closeable success alert on redirect

### Database Migrations Executed
- `ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1`
- `ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL`
- `CREATE TABLE system_logs` (was missing from live DB)

## Verified
- `./gradlew build -x test` → BUILD SUCCESSFUL
- Browser: admin login, create user, success alert, DataTable refresh — all working
- Seeded accounts (admin, registrar, trainer) remain functional in database
