# Testing — Anihan SRMS

## Login Page — Visual Checklist
- [x] Header visible at top with "Anihan SRMS" brand
- [x] Login card centered vertically and horizontally
- [x] Username and password fields present and functional
- [x] Login button displays with gradient styling
- [x] Footer visible at bottom with copyright
- [x] Green/white/blue palette applied throughout
- [x] Responsive — card stays centered on mobile
- [x] Hover effects work on button and inputs
- [x] Session check on load redirects already-authenticated users

## Login Security (AGILE-142) — API Tests
- [x] Unauthenticated GET /admin.html → 302 redirect to /index.html
- [x] Unauthenticated GET /registrar.html → 302 redirect to /index.html
- [x] Unauthenticated GET /trainer.html → 302 redirect to /index.html
- [x] Unauthenticated GET /api/auth/me → 401 JSON
- [x] POST /api/auth/login (Mark/password123) → 200 + ROLE_ADMIN
- [x] POST /api/auth/login (registrar/password123) → 200 + ROLE_REGISTRAR
- [x] POST /api/auth/login (trainer/password123) → 200 + ROLE_TRAINER
- [x] Authenticated admin GET /admin.html → 200
- [x] Authenticated admin GET /registrar.html → 302 (wrong role blocked)
- [x] Authenticated registrar GET /registrar.html → 200

## Account Management (AGILE-142) — API Tests
- [x] PUT /api/account/profile (valid) → 200 + username updated
- [x] PUT /api/account/profile (wrong password) → 400 "Current password is incorrect"
- [x] PUT /api/account/profile (duplicate username) → 400 "Username is already taken"
- [x] PUT /api/account/password (valid) → 200 + session invalidated
- [x] Login with new password → 200 (confirms password persisted)
- [x] Logout → subsequent dashboard access returns 302

## Dashboard UI — Visual Checklist
- [ ] Account icon visible top-right on admin dashboard
- [ ] Account icon visible top-right on registrar dashboard
- [ ] Account icon visible top-right on trainer dashboard
- [ ] Dropdown shows username, role, Edit Account, Log Out
- [ ] Edit Account modal opens with username + password forms
- [ ] Username change via modal works end-to-end
- [ ] Password change via modal triggers redirect to login
- [ ] Modal styling matches green/white/blue theme
