# Change Log — Anihan SRMS

## 2026-04-06 — AGILE-100: G2.1 Edit Personal Details
**Branch:** `feature/fix-login-security`

### Files Created
| File | Purpose |
|---|---|
| `dto/UpdatePersonalDetailsRequest.java` | Personal details update DTO (record) |
| `repository/SubjectRepository.java` | JPA repo for subjects dropdown |
| `repository/SectionRepository.java` | JPA repo for sections dropdown |
| `controller/LookupController.java` | GET /api/lookup/subjects + /sections |

### Files Modified
| File | Change |
|---|---|
| `service/CustomUserDetailsService.java`| Enforced strict case match on username login; fallback to case-insensitive email |
| `model/User.java` | Added fullName, age, dateOfBirth, subjectCode, sectionCode fields |
| `service/AccountService.java` | Added updatePersonalDetails() with role-based field enforcement |
| `controller/AccountController.java` | Added PUT /api/account/details |
| `controller/AuthController.java` | Injected UserRepository, expanded /api/auth/me to include personal details |
| `static/css/dashboard.css` | Added nav-tab styles, form-select, trainer-only-fields, logout notification |
| `static/js/auth-guard.js` | Added personal details form, dropdown loading, trainer field visibility, logout notification redirect |
| `static/admin.html` | Tabbed Edit Account modal (Personal Details + Account Settings) |
| `static/registrar.html` | Same tabbed modal |
| `static/trainer.html` | Same tabbed modal (trainer sees Subject/Section dropdowns) |
| `static/index.html` | Added logout notification banner + JS trigger |

### Database Changes
| Change | Command |
|---|---|
| Added columns to users | `ALTER TABLE users ADD full_name, age, date_of_birth, subject_code (FK), section_code (FK)` |
| Seeded courses | `INSERT INTO courses VALUES ('CARS', 'Culinary Arts and Restaurant Services')` |
| Seeded batches | `INSERT INTO batches VALUES ('B2026A', 2026), ('B2026B', 2026)` |
| Seeded qualifications | NC II - Cookery, NC II - Food and Beverage Services |
| Seeded subjects | COOK101, COOK102, FBS101 |
| Seeded sections | SEC-A, SEC-B, SEC-C |

### Verification
- `./gradlew clean build -x test` → BUILD SUCCESSFUL
- `./gradlew bootRun` → Started in ~10s
- PUT /api/account/details (trainer) → 200 + all fields saved ✅
- GET /api/lookup/subjects → 3 subjects returned ✅
- GET /api/lookup/sections → 3 sections returned ✅
- GET /api/auth/me → includes personal details ✅
- Browser: tabbed modal works, dropdowns populated ✅
- Browser: logout notification banner slides in ✅

## 2026-04-05 — AGILE-142: G1.R Fix Login
**Branch:** `feature/fix-login-security`

### Files Created
| File | Purpose |
|---|---|
| `controller/AccountController.java` | PUT /api/account/profile + /api/account/password |
| `service/AccountService.java` | Username/password change business logic |
| `dto/UpdateProfileRequest.java` | Username change DTO with validation |
| `dto/UpdatePasswordRequest.java` | Password change DTO with validation |
| `static/js/auth-guard.js` | Shared session guard + account UI logic |
| `static/css/dashboard.css` | Account dropdown + modal styles |

### Files Modified
| File | Change |
|---|---|
| `config/SecurityConfig.java` | Rewritten: role-specific HTML matchers, dual-mode entry/denied handlers, cache-control |
| `model/User.java` | Added `@Column(unique = true)` on username |
| `exception/GlobalExceptionHandler.java` | Added IllegalArgumentException handler |
| `static/admin.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/registrar.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/trainer.html` | Added account dropdown, Edit Account modal, auth-guard.js |
| `static/index.html` | Added session check to redirect authenticated users |

### Files Deleted
| File | Reason |
|---|---|
| `config/DataSeeder.java` | Removed — accounts managed directly in database per user decision |

### Database Changes
| Change | Command |
|---|---|
| Added unique index on `users.username` | `ALTER TABLE AnihanSRMS.users ADD UNIQUE INDEX idx_username (username);` |

### Verification
- `./gradlew clean build -x test` → BUILD SUCCESSFUL
- `./gradlew bootRun` → Started successfully (no errors, no DataSeeder output)
- Unauthenticated `/admin.html` → 302 redirect to `/index.html` ✅
- API `/api/auth/me` without session → 401 JSON ✅
- Admin login → 200 + admin.html ✅
- Admin accessing `/registrar.html` → 302 redirect (wrong role blocked) ✅
- Registrar login → 200 + registrar.html ✅
- Username change → 200 + session updated ✅
- Password change → 200 + session invalidated (401 on next request) ✅
- Login with new password → 200 ✅
- Logout → subsequent access returns 302 ✅
- Wrong password on profile update → 400 "Current password is incorrect" ✅

## 2026-03-24 — Phases 1–4: Backend Auth Setup
**Branch:** `feature/backend-auth-setup`

### Files Created
| File | Purpose |
|---|---|
| `model/User.java` | Users table entity |
| `repository/UserRepository.java` | JPA repo with username/email queries |
| `controller/AuthController.java` | Login/logout/me endpoints |
| `src/main/resources/static/admin.html` | Empty admin template with header/footer |
| `src/main/resources/static/registrar.html` | Empty registrar template with header/footer |
| `src/main/resources/static/trainer.html` | Empty trainer template with header/footer |

## 2026-04-07 — Database Schema Development
| File | Action | Description |
|------|--------|-------------|
| `AnihanSRMS.sql` | Created | Defined complete database schema with 15 tables for student records, enrollment, and results |

## 2026-04-10 — Admin Dashboard UI & Logic (Current)
**Branch:** `feature/admin-dashboard-ui`

### Files Created/Modifed
| File | Change |
|---|---|
| `model/User.java` | Added DB columns: name, birthdate, age |
| `controller/AdminController.java` | Created `GET /users`, `GET /users/{id}`, `PUT /users/{id}` APIs with self-role lock |
| `config/SecurityConfig.java` | Added HTML static page security bindings (hasRole) |
| `admin.html` | Built responsive Navbar, DataTables User table, and Expand Details Modal |
| `edit-user.html` | Created page for updating user details with Javascript change-tracking |
| `logs.html, subjects.html, student-records.html` | Created placeholder pages ensuring navbar linking |
| `admin.html, et al` | Reverted navbar color to the original login.css light green gradient, removing inline styles |
| `edit-user.html` | Fixed structural HTML bug: removed extra `</div>` that prematurely closed the card-body, causing form fields to render outside the card |

### Verification
- Tested DataTables rendering users.
- Confirmed Modal successfully fetches via Ajax.
- Confirmed `edit-user.html` correctly displays "Unsaved changes" warnings visually.
- Verified backend rejects self-demoting role assignments.
- Checked user IDE error: `String cannot be resolved to a type` inside `LoginRequest.java`.
- Ran `./gradlew clean compileJava` locally. Confirmed `BUILD SUCCESSFUL`.
- Diagnosed issue as a false-positive caused by IDE Java Language Server (JDTLS) losing connection to the Java 25 JDK.
- Updated `activeContext.md` and `progress.md`.
- Updated `build.gradle.kts` to use Java 25.
- Updated `src/main/resources/static/index.html` to add error alert div + login fetch JS.
- Updated `src/main/resources/static/admin.html` to add header/footer + empty main.
- Updated `src/main/resources/static/registrar.html` to add header/footer + empty main.
- Updated `src/main/resources/static/trainer.html` to add header/footer + empty main.
### 2026-04-10
- Refactored DB Schema to drop 'full_name' in favor of 'lastname', 'firstname', 'middlename'.
- Renamed 'date_of_birth' to 'birthdate'.
- Removed inline 'subject_code' and 'section_code' from users table and corresponding java entities.
- Updated AnihanSRMS.sql to remove GIT conflict marks and use clean CREATE TABLE.
- Updated User.java, UpdatePersonalDetailsRequest.java for the entity structural changes.
- Updated AccountService.java, AuthController.java, AccountController.java for new personal detail endpoints.
- Updated auth-guard.js and admin, trainer, registrar HTML pages to render distinct inputs for names.
- Re-seeded 3 standard test accounts into the new 'users' table schema.

### 2026-04-10
- Resolved Hibernate MappingException in User.java by removing duplicate fields (lastname/lastName, firstname/firstName, middlename/middleName).
- Updated AdminController.java to reflect standard camelCase getter and setter calls on the User entity.
- Restored successful execution of gradle build contextLoads().
