# Testing - Anihan SRMS

## Login Page - Visual Checklist
- [x] Header visible at top with "Anihan SRMS" brand
- [x] Login card centered vertically and horizontally
- [x] Username and password fields present and functional
- [x] Login button displays with gradient styling
- [x] Footer visible at bottom with copyright
- [x] Green/white/blue palette applied throughout
- [x] Responsive - card stays centered on mobile
- [x] Hover effects work on button and inputs
- [x] Session check on load redirects already-authenticated users

## Login Security (AGILE-142) - API Tests
- [x] Unauthenticated GET `/admin.html` -> 302 redirect to `/index.html`
- [x] Unauthenticated GET `/registrar.html` -> 302 redirect to `/index.html`
- [x] Unauthenticated GET `/trainer.html` -> 302 redirect to `/index.html`
- [x] Unauthenticated GET `/api/auth/me` -> 401 JSON
- [x] POST `/api/auth/login` (Mark/password123) -> 200 + `ROLE_ADMIN`
- [x] POST `/api/auth/login` (registrar/password123) -> 200 + `ROLE_REGISTRAR`
- [x] POST `/api/auth/login` (trainer/password123) -> 200 + `ROLE_TRAINER`
- [x] Authenticated admin GET `/admin.html` -> 200
- [x] Authenticated admin GET `/registrar.html` -> 302 (wrong role blocked)
- [x] Authenticated registrar GET `/registrar.html` -> 200

## Account Management (AGILE-142) - API Tests
- [x] PUT `/api/account/profile` (valid) -> 200 + username updated
- [x] PUT `/api/account/profile` (wrong password) -> 400 "Current password is incorrect"
- [x] PUT `/api/account/profile` (duplicate username) -> 400 "Username is already taken"
- [x] PUT `/api/account/password` (valid) -> 200 + session invalidated
- [x] Login with new password -> 200 (confirms password persisted)
- [x] Logout -> subsequent dashboard access returns 302

## Admin Merge - Automated Tests
- [x] GET `/api/admin/users` as admin -> 200 with sanitized DTO response (no password field)
- [x] GET `/api/admin/users` as non-admin -> 403
- [x] PUT `/api/admin/users/{id}` with invalid payload -> 400 validation response
- [x] Admin service blocks self-role change
- [x] Admin service returns updated sanitized user response
- [x] `./gradlew test` -> BUILD SUCCESSFUL
- [x] `./gradlew build` -> BUILD SUCCESSFUL

## Commit-Safety Recheck
- [x] `git diff --check` -> no tracked whitespace or conflict-marker errors
- [x] `rg -n "^(<<<<<<<|=======|>>>>>>>)" -S .` -> no unresolved merge markers in the workspace
- [x] `./gradlew test` -> BUILD SUCCESSFUL after the conflict cleanup pass
- [x] `./gradlew build` -> BUILD SUCCESSFUL after the conflict cleanup pass

## Build Repair - Admin Controller
- [x] Restored `AdminController` to constructor-injected `AdminService` usage
- [x] Reinstated validated `AdminUpdateUserRequest` handling on `PUT /api/admin/users/{id}`
- [x] Reinstated sanitized `AdminUserResponse` payloads on admin user endpoints
- [x] `./gradlew test` -> BUILD SUCCESSFUL after the controller repair
- [x] `./gradlew build` -> BUILD SUCCESSFUL after the controller repair

## Admin Front-End Repair
- [x] Rebuilt `admin.html` so it now has one valid document structure and matches `admin-users.js`
- [x] Rebuilt `edit-user.html` so it matches `admin-edit-user.js`
- [x] Confirmed required static assets exist for the repaired admin flow (`dashboard.css`, `auth-guard.js`, `admin-users.js`, `admin-edit-user.js`, `datatables.min.js`, `jquery-4.0.0.min.js`)
- [x] `./gradlew build` -> BUILD SUCCESSFUL after the front-end shell rebuild
- [ ] Browser retest: confirm admin login opens the rebuilt dashboard instead of a blank page
- [ ] Browser retest: confirm the user detail modal and edit-user flow work end-to-end

## Dashboard UI - Visual Checklist
- [ ] Account icon visible top-right on admin dashboard
- [ ] Account icon visible top-right on registrar dashboard
- [ ] Account icon visible top-right on trainer dashboard
- [ ] Dropdown shows username, role, Edit Account, Log Out
- [ ] Edit Account modal opens with username + password forms
- [ ] Username change via modal works end-to-end
- [ ] Password change via modal triggers redirect to login
- [ ] Modal styling matches green/white/blue theme

## Admin Merge - Manual Visual Checklist
- [ ] Admin dashboard shows merged user table and summary cards cleanly
- [ ] User details modal opens from the admin table
- [ ] Edit User page loads existing user details and saves successfully
- [ ] `student-records.html`, `subjects.html`, and `logs.html` share the same admin shell and theme
- [ ] Merged admin pages render correctly on mobile widths
