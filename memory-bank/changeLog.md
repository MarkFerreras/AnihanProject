# Change Log — Anihan SRMS

## 2026-03-24 — Phases 1–4: Backend Auth Setup
**Branch:** `feature/backend-auth-setup`

### Files Created
| File | Purpose |
|---|---|
| `model/User.java` | Users table entity |
| `model/Batch.java` | Batches table entity |
| `model/Course.java` | Courses table entity |
| `model/Qualification.java` | Qualifications table entity |
| `model/Section.java` | Sections table entity (FK: batch, course) |
| `model/Subject.java` | Subjects table entity (FK: qualification) |
| `model/StudentRecord.java` | Student records entity (FK: batch, course, section) |
| `model/Parent.java` | Parents table entity (FK: student) |
| `model/OtherGuardian.java` | Other guardians table entity (FK: student) |
| `model/Document.java` | Documents table entity (FK: student) |
| `model/Grade.java` | Grades table entity (FK: student, subject) |
| `repository/UserRepository.java` | JPA repo with username/email queries |
| `service/CustomUserDetailsService.java` | Login by username or email |
| `config/SecurityConfig.java` | Session-based auth, RBAC, BCrypt |
| `config/DataSeeder.java` | 3 dummy accounts seeder |
| `dto/LoginRequest.java` | Login DTO with validation |
| `controller/AuthController.java` | Login/logout/me endpoints |
| `exception/GlobalExceptionHandler.java` | Validation + auth error handler |
| `src/main/resources/static/admin.html` | Empty admin template with header/footer |
| `src/main/resources/static/registrar.html` | Empty registrar template with header/footer |
| `src/main/resources/static/trainer.html` | Empty trainer template with header/footer |

### Files Modified
| File | Change |
|---|---|
| `application.properties` | Added ddl-auto=none, MySQL dialect, session config |
| `index.html` | Added error alert div + login fetch JS |
| `src/main/sql/AnihanSRMS.sql` | Reordered tables to respect foreign key creation order |

### Files Deleted
| File | Reason |
|---|---|
| `src/main/java/controller/` | Empty folder outside Spring Boot package |
| `src/main/java/model/` | Empty folder outside Spring Boot package |
| `src/main/java/repository/` | Empty folder outside Spring Boot package |
| `src/main/java/service/` | Empty folder outside Spring Boot package |
| `docker-compose.yml` | Removed per user request, using existing `mysql-server` container |

### Verification
- Re-injected corrected SQL script into `mysql-server`
- Gradle `clean build -x test`: **BUILD SUCCESSFUL**
- `.\gradlew bootRun` started successfully
- `Invoke-RestMethod` to `/api/auth/login` returned 200 OK + `ROLE_ADMIN`
