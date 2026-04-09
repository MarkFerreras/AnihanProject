# Change Log — Anihan SRMS

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

### Verification
- Tested DataTables rendering users.
- Confirmed Modal successfully fetches via Ajax.
- Confirmed `edit-user.html` correctly displays "Unsaved changes" warnings visually.
- Verified backend rejects self-demoting role assignments.
