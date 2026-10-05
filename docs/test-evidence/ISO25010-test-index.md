# ISO/IEC 25010 Test Evidence Index - Anihan SRMS

Generated 2026-10-05 on branch `new-unit-tests` from `./gradlew test -q`.

## Result

- Result XML files (nested test classes counted separately): 85
- **Total tests: 1041**; failures 0, errors 0, skipped 0 (summed from `build/test-results/test/*.xml`).
- Coverage percentages: not produced. JaCoCo was deliberately skipped (build changes need user approval), so this index reports counts only. Line/branch coverage: Unverified.
- Few test classes carry an `ISO 25010:` Javadoc tag, so the grouping below follows the plan phases in `docs/superpowers/plans/2026-10-05-srms-test-suite.md`. IDs are given at task level (T1..T14); per-method IDs are in the test comments (Unverified for completeness). "baseline" means a class that existed before this plan or is not in a numbered task.

## How to open the report

Open `build/reports/tests/test/index.html` in a browser (Gradle generates it after `./gradlew test`). Raw XML is in `build/test-results/test/`.

## Tests by ISO/IEC 25010 characteristic

### Security

| Test class | Task | What it proves | Tests |
|---|---|---|---|
| security.SecurityMatrixWebMvcTest | T1 | Role x route authorization matrix; anonymous and wrong-role requests blocked, correct roles allowed | 13 |
| security.SecurityConfigTest | T2 | Session timeout, HTTP-only/SameSite cookie settings, CSRF-off rule read from configuration | 11 |
| controller.GlobalExceptionHandlerWebMvcTest | T3 | Centralised error responses: status codes and no internal detail leaked in bodies | 17 |
| controller.PasswordRecoveryControllerWebMvcTest | T4 | Recovery flow: PENDING_* session roles, lookup, answer check, reset, bad input | 15 |
| controller.SecurityQuestionControllerWebMvcTest | T5 | Security-question endpoints: authorization, slot validation, responses | 9 |
| service.SecurityQuestionServiceTest | T5 (service) | Answer verification, slot validation, reset-password rules | 28 |
| service.CustomUserDetailsServiceTest | T6 | User lookup and authority mapping for login | 4 |
| service.SessionAuthenticationHelperTest | T6 | Session authentication helper behaviour, session id handling | 3 |
| **Subtotal** | | | **100** |


### Functional suitability - validation and data contracts

| Test class | Task | What it proves | Tests |
|---|---|---|---|
| controller.LookupControllerWebMvcTest | T7 | Lookup endpoints return expected data and statuses | 6 |
| controller.StudentDetailsControllerWebMvcTest | T7 | Public enrollment wizard endpoints: load, start/resume, submit, validation | 10 |
| dto.DtoValidationTest | T8 | Bean Validation constraints on request DTOs (blank, size, pattern) | 31 |
| SchemaContractTest | T10 | schema.sql, migrations and the device-transfer dump agree with entities and rules | 12 |
| FrontendContractTest | T10 | HTML pages carry the required navbar, scripts and role attributes | 11 |
| **Subtotal** | | | **70** |


### Reliability - repositories and integration (H2)

| Test class | Task | What it proves | Tests |
|---|---|---|---|
| repository.UserRepositoryH2Test | T9 | User queries and uniqueness behaviour | 3 |
| repository.StudentRecordRepositoryH2Test | T9 | Student record queries and constraints | 7 |
| repository.SystemLogRepositoryH2Test | T9 | Append-only log queries, timestamp range filtering | 4 |
| repository.ClassManagementRepositoriesH2Test | T9 | Class, section, enrollment and grade repository queries and constraints | 23 |
| integration.AuditAndRollbackH2Test | T14 | Audit rows written with mutations; transactional rollback; id generation; partial import by design | 6 |
| integration.StudentNumberUniquenessH2Test | baseline | Student number uniqueness at DB level | 4 |
| integration.StudentRecordH2LoadTest | baseline | Bulk student record load | 1 |
| integration.DocumentStorageIntegrationTest | T13b / baseline | Document storage; export check matches ZIP export | 16 |
| integration.SoChecklistIntegrationTest | baseline | SO checklist end-to-end against H2 | 8 |
| **Subtotal** | | | **72** |


### Functional suitability - business rules and edge cases

| Test class | Task | What it proves | Tests |
|---|---|---|---|
| service.StudentStatusTransitionsMatrixTest | T11 | Full status-transition matrix | 30 |
| service.StudentStatusTransitionsTest | baseline | Transition rules with date/reason requirements | 13 |
| service.GradeEquivalentBoundaryTest | T11 | Grade to equivalent boundary values | 26 |
| service.GradeEquivalentTest | baseline | Grade equivalent mapping | 9 |
| service.AgeCalculatorEdgeTest | T11 | Age edge cases (leap day, future date, null birthdate) | 6 |
| service.AgeCalculatorTest | baseline | Age calculation | 6 |
| service.SoReadinessPolicyMatrixTest | T11 | SO readiness rule matrix | 32 |
| service.SoReadinessPolicyTest | baseline | SO readiness rules | 21 |
| service.StudentIdGenerationServiceTest | T12 | SR{year}{seq} reference generation | 4 |
| controller.ServiceMutationLogAuditWebMvcTest | T12 | Mutating service calls write the expected system_logs entry | 16 |
| service.RegistrarStatusServiceTest | baseline | Registrar status change rules and logging | 11 |
| service.RegistrarRecordFieldsServiceTest | T12 (extended) | Record field updates | 14 |
| service.ClassManagementServiceTest | T12 (extended) | Class management rules incl. section removal and deletion | 13 |
| service.ClassManagementSectionServiceTest | baseline | Section service rules | 15 |
| service.AdminServiceTest | T12 (extended) | Admin user CRUD rules | 14 |
| service.AccountServiceTest | T12 (extended) | Account self-service rules | 16 |
| service.TrainerGradeServiceTest | T13a (extended) | Trainer grade input and locking rules | 24 |
| controller.TrainerGradeControllerWebMvcTest | T13a (extended) | Trainer grade endpoints | 12 |
| service.DocumentServiceTest | T13b (extended) | Document upload, labels, types, validation | 63 |
| service.DocumentExportServiceTest | T13b (extended) | Document ZIP export and missing-document check | 27 |
| controller.DocumentControllerWebMvcTest | T13b (extended) | Document endpoints | 68 |
| service.HtmlDocxConverterTest | T13b (extended) | HTML to DOCX conversion | 5 |
| service.StudentNumberSheetParserTest | T13c (extended) | CSV/XLSX parsing of the student-number sheet | 21 |
| service.StudentNumberImportServiceTest | T13c (extended) | Import preview/apply rules | 27 |
| service.StudentNumberExportServiceTest | baseline | Student-number export | 8 |
| service.SystemLogExportServiceTest | T13c (extended) | System log export | 5 |
| service.SystemLogServiceTest | T13c (extended) | System log writing | 12 |
| **Subtotal** | | | **518** |


### Other baseline classes (pre-existing, not in the numbered plan tasks)

| Test class | Tests |
|---|---|
| PasswordTest | 1 |
| SpringbootApplicationTests | 1 |
| controller.AccountControllerWebMvcTest | 9 |
| controller.AdminBulkLoadWebMvcTest | 2 |
| controller.AdminControllerWebMvcTest | 6 |
| controller.AuthControllerWebMvcTest | 4 |
| controller.ClassManagementControllerWebMvcTest | 5 |
| controller.ClassManagementCourseControllerWebMvcTest | 6 |
| controller.ClassManagementSectionControllerWebMvcTest | 7 |
| controller.ClassManagementSubjectControllerWebMvcTest | 11 |
| controller.RegistrarBatchControllerWebMvcTest | 9 |
| controller.RegistrarBulkLoadWebMvcTest | 5 |
| controller.RegistrarRecordUpdateControllerWebMvcTest | 3 |
| controller.RegistrarStatusControllerWebMvcTest | 13 |
| controller.RegistrarStudentNumberControllerWebMvcTest | 11 |
| controller.SoChecklistControllerWebMvcTest | 4 |
| controller.StudentNumberControllerWebMvcTest | 13 |
| controller.StudentPortalControllerWebMvcTest | 6 |
| controller.SystemLogControllerWebMvcTest | 17 |
| controller.TrainerControllerWebMvcTest | 16 |
| service.AdminBulkLoadTest | 3 |
| service.ClassManagementSectionCreateServiceTest | 9 |
| service.ClassManagementSubjectServiceTest | 17 |
| service.CourseCodeGeneratorTest | 11 |
| service.DocumentFolderServiceTest | 8 |
| service.DocumentGenerationServiceTest | 3 |
| service.DocumentTypeSuggesterTest | 10 |
| service.RegistrarBatchServiceTest | 10 |
| service.RegistrarBulkLoadTest | 7 |
| service.RegistrarStudentNumberServiceTest | 15 |
| service.RequiredDocumentPolicyTest | 9 |
| service.StudentDetailsServiceTest | 10 |
| service.TrainerServiceTest | 19 |
| support.RouteSweepTest | 1 |
| **Subtotal** | **281** |


## Findings pinned by the tests

Findings are real defects or gaps the tests exposed. No production code was changed; tests assert current behaviour. IDs follow the ledger `.superpowers/sdd/2026-10-05-srms-test-suite/progress.md`.

| ID | Task/test | Characteristic | Observed behaviour (tests pin current behaviour) |
|---|---|---|---|
| F1 | T1-09 | Security | /api/lookup/** has no explicit SecurityConfig rule; reachable by PENDING_* recovery sessions |
| F2 | T3 | Reliability | Malformed JSON body returns 500 (HttpMessageNotReadableException falls to catch-all) |
| F3 | T3 | Reliability | Non-numeric path variable returns 500 (MethodArgumentTypeMismatchException falls to catch-all) |
| F4 | T4 | Functional | EmailLookupRequest has only @NotBlank, no @Email |
| F5 | T5 | Functional | Duplicate custom security questions accepted by validateSlots |
| F6 | T5 | Security (low) | SecurityQuestionService.resetPassword does no strength/length check; policy only in the DTO |
| F7 | T6 | Security | Session id never rotated on login/privilege change; no sessionFixation() config |
| F8 | T7 | Security | GET /api/student/{id} and POST /{id}/submit are permitAll, keyed by guessable SR{year}{seq}; anonymous read of enrollee data |
| F9 | T9 | Reliability (low) | User.email lacks unique=true although schema.sql has uq_email |
| F10 | T9 | Reliability (low) | ClassEnrollment/Grade entities lack uniqueConstraints that schema.sql defines |
| F11 | T10-03 | Maintainability | AnihanSRMS.sql dump lacks classes, class_enrollments, security_questions, user_security_answers |
| F12 | T10-05 | Maintainability | AnihanSRMS.sql dump lacks student_records.student_number and its unique key |
| F13 | T10 | Reliability | Migration 2026-05-20-sync-and-clear-students.sql deletes every student table and is not idempotent (allow-listed in the test) |
| F14 | T10-08 | Maintainability (low) | edit-user.html loads auth-guard.js without jQuery, against the page checklist; no runtime break recorded |
| F15 | T11 | Functional (low) | Future birthdate gives a negative age; no validation |
| F16 | T11 | Functional (low) | GradeEquivalent maps >100 to 1.00 and negatives to 5.00; no range validation |
| F17 | T12 | Reliability | deleteSection has no student guard; relies on the DB foreign key |
| F18 | T12 | Functional (note) | No explicit last-admin guard (ledger note) |
| F19 | T13c | Functional | Duplicate Reference No. in an import file not flagged; later row wins |
| F20 | T13c | Usability | Semicolon-delimited CSV (Excel in some locales) unsupported |
| F21 | T13c | Functional | XLSX formula cells read as text (no FormulaEvaluator) |
| F22 | T13b | Functional | No document-type dedup on upload |
| F23 | T13b | Reliability (by design) | Empty HTML is rejected with "The document has no content to convert." — intended guard, pinned as correct behaviour, NOT a defect |
| F24 | T14 | Reliability | generateStudentId is read-max-then-insert with no lock (race; conditional) |
| F25 | T14 | Reliability (by design) | Import apply is partial by design; a failing row does not roll back earlier rows |

Cross-check: `grep -rn FINDING src/test` matches F1-F9, F11-F14, F17, F19-F21, F24, F25. Pinned without a FINDING marker: F15 (`AgeCalculatorEdgeTest`), F16 (`GradeEquivalentBoundaryTest`), F18 (`AdminServiceTest.adminCannotDisableOrDemoteLastAdmin`, self-guard only), F22 (`DocumentServiceTest.uploadSameTypeTwiceReplacesOrRejectsPerRule`, asserts both rows kept). F10 has no real pin (the repository tests check the hand-written H2 schema, not the entity mappings): Unverified.
