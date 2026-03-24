# Progress — Anihan SRMS

## Completed
- [x] Project folder structure created (`controller`, `model`, `repository`, `service`)
- [x] Login page front-end UI (`index.html`, `css/login.css`)
- [x] Memory bank initialized
- [x] Gradle upgraded to 9.4.1
- [x] Existing `mysql-server` container reused
- [x] `AnihanSRMS.sql` table order fixed for FKs and injected into database
- [x] `application.properties` updated (ddl-auto=none, MySQL dialect, session config)
- [x] 11 JPA entities mapped from `AnihanSRMS.sql`
- [x] `UserRepository` with username/email query methods
- [x] `CustomUserDetailsService` (login by username OR email)
- [x] `SecurityConfig` (session-based, RBAC, BCrypt, 401 JSON)
- [x] `LoginRequest` DTO with @Valid / @NotBlank
- [x] `AuthController` (login/logout/me endpoints)
- [x] `GlobalExceptionHandler` (@ControllerAdvice)
- [x] `DataSeeder` (3 dummy accounts)
- [x] Frontend login JS (fetch POST → route by role)
- [x] Gradle build passes
- [x] Spring Boot boots cleanly + seeds basic users
- [x] Login endpoint verified via `Invoke-RestMethod`
- [x] Created empty dashboard templates (`admin.html`, `registrar.html`, `trainer.html`)

## In Progress
- [/] User review of Phases 1–4 implementation
- [/] Troubleshoot IDE indexing errors

## Remaining
- [ ] Student record CRUD
- [ ] DataTables integration
