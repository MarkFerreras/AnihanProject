"""Generate Thread Testing Cases for Anihan SRMS into an Excel workbook."""
from openpyxl import Workbook
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side

wb = Workbook()
ws = wb.active
ws.title = "Thread Testing Cases"

headers = ["Test ID", "Title", "Pre-Conditions", "Test Steps", "Expected Results"]
ws.append(headers)

# Styling
header_font = Font(bold=True, color="FFFFFF", size=11)
header_fill = PatternFill(start_color="2E5C8A", end_color="2E5C8A", fill_type="solid")
thin = Side(border_style="thin", color="999999")
border = Border(top=thin, left=thin, right=thin, bottom=thin)
wrap = Alignment(wrap_text=True, vertical="top", horizontal="left")
header_align = Alignment(wrap_text=True, vertical="center", horizontal="center")

for col in range(1, len(headers) + 1):
    c = ws.cell(row=1, column=col)
    c.font = header_font
    c.fill = header_fill
    c.alignment = header_align
    c.border = border

# Thread test cases: (Title, Pre-Conditions, Test Steps, Expected Results)
cases = [
    # ---------- AUTH / LOGIN (index.html) ----------
    ("Successful Admin Login Thread",
     "1. Application is running on localhost:8080.\n2. MySQL is running with seeded admin account (username: admin, password: password123).\n3. Browser session is empty (no active session cookie).",
     "1. Navigate to / (index.html).\n2. Enter username 'admin'.\n3. Enter password 'password123'.\n4. Click the Login button.",
     "1. POST /api/auth/login returns HTTP 200.\n2. JSESSIONID cookie is set (HttpOnly, SameSite=Lax).\n3. Browser is redirected to /admin.html.\n4. A system_logs row is written with action 'LOGIN' for user 'admin'."),

    ("Successful Registrar Login Thread",
     "1. Registrar seed account exists (username: registrar, password: password123).\n2. No active session.",
     "1. Navigate to /.\n2. Enter 'registrar' / 'password123'.\n3. Click Login.",
     "1. HTTP 200 returned from /api/auth/login.\n2. Redirect to /registrar.html.\n3. Registrar navbar (Home | Subjects | Classes | Sections) renders.\n4. system_logs LOGIN row created."),

    ("Successful Trainer Login Thread",
     "1. Trainer seed account exists (username: trainer, password: password123).\n2. No active session.",
     "1. Navigate to /.\n2. Enter 'trainer' / 'password123'.\n3. Click Login.",
     "1. HTTP 200 from /api/auth/login.\n2. Redirect to /trainer.html.\n3. Trainer navbar (Home | My Subjects | My Classes) renders."),

    ("Login Failure With Invalid Credentials",
     "1. Login page is accessible.\n2. Username 'admin' exists but password 'wrongpass' is incorrect.",
     "1. Navigate to /.\n2. Enter 'admin' / 'wrongpass'.\n3. Click Login.",
     "1. HTTP 401 returned.\n2. Error alert 'Invalid username or password' displays on login card.\n3. No session cookie is set.\n4. User remains on /."),

    ("Login Failure With Disabled Account",
     "1. A user account exists with enabled=false (soft-deleted).",
     "1. Navigate to /.\n2. Enter the disabled user's credentials.\n3. Click Login.",
     "1. HTTP 401 returned (DisabledException handled).\n2. Login fails with appropriate error message.\n3. No session cookie set."),

    ("Logout Thread",
     "1. User is logged in (any role).\n2. Account dropdown is visible in navbar.",
     "1. Click the account icon in the navbar.\n2. Click 'Log Out' in the dropdown.\n3. Confirm logout.",
     "1. POST /api/auth/logout returns HTTP 200.\n2. Session is invalidated.\n3. Browser redirects to /index.html.\n4. system_logs LOGOUT row created."),

    ("Session Timeout / Auth Guard Thread",
     "1. User is logged in.\n2. Session timeout is configured at 30 minutes.",
     "1. Log in as any role.\n2. Wait 30+ minutes without activity (or invalidate session via dev tools).\n3. Attempt to navigate or make an API call.",
     "1. GET /api/auth/me returns HTTP 401.\n2. auth-guard.js redirects user to /index.html.\n3. Protected page contents are not displayed."),

    ("Cross-Role Access Denial Thread",
     "1. Logged in as TRAINER.\n2. /admin.html requires ROLE_ADMIN.",
     "1. While authenticated as trainer, navigate directly to /admin.html in the URL bar.",
     "1. HTTP 302 redirect (or 403) issued by Spring Security.\n2. Trainer is not granted access; redirected to denied or own dashboard.\n3. No admin content rendered."),

    # ---------- ADMIN DASHBOARD ----------
    ("Admin Dashboard Loads User List",
     "1. Logged in as admin.\n2. users table contains at least 3 seed accounts.",
     "1. Navigate to /admin.html.\n2. Allow the users DataTable to load.",
     "1. GET /api/admin/users returns HTTP 200 with user list.\n2. DataTable renders Last Name, First Name, Username, Role, Status columns.\n3. Each row has Edit and Delete actions."),

    ("Add New User Thread",
     "1. Logged in as admin.\n2. Username 'newuser01' does not exist.",
     "1. Click 'Add User' on /admin.html.\n2. Fill in last name, first name, username 'newuser01', strong password, role TRAINER, birthdate.\n3. Submit the form.",
     "1. POST /api/admin/users returns HTTP 201.\n2. Age is auto-calculated from birthdate (not user-input).\n3. New user appears in DataTable after reload.\n4. system_logs row created (action: 'Created user ...')."),

    ("Add User Duplicate Username Rejection",
     "1. Logged in as admin.\n2. Username 'admin' already exists (UNIQUE index).",
     "1. Click 'Add User'.\n2. Enter username 'admin' with valid other fields.\n3. Submit.",
     "1. HTTP 409 (DataIntegrityViolationException) returned.\n2. Generic conflict message displayed inline.\n3. No new row inserted; DataTable unchanged."),

    ("Edit User Personal Details Thread",
     "1. Logged in as admin.\n2. Target user exists (not the admin himself).",
     "1. Click Edit on a user row.\n2. Update last name and birthdate.\n3. Save changes.",
     "1. PUT /api/admin/users/{id} returns HTTP 200.\n2. Age is silently recalculated from new birthdate.\n3. Row updates with new data on reload.\n4. system_logs row written."),

    ("Admin Password Reset Thread",
     "1. Logged in as admin.\n2. Target user exists.",
     "1. Edit a user.\n2. Enter a new password (min 8 chars, no strong-rule enforcement on admin reset).\n3. Save.",
     "1. Password is BCrypt-hashed and saved.\n2. passwordChangedAt timestamp updated.\n3. Target user can log in with the new password.\n4. system_logs entry 'Reset password for: <username>' created."),

    ("Admin Self-Role-Change Lock",
     "1. Logged in as admin (own user record).",
     "1. Open edit modal for own admin account.\n2. Attempt to change own role to REGISTRAR.\n3. Save.",
     "1. HTTP 400 returned with validation error.\n2. Role is not modified.\n3. Admin remains ROLE_ADMIN."),

    ("Soft Delete (Deactivate) User Thread",
     "1. Logged in as admin.\n2. Target user is enabled.",
     "1. Click Delete on a user row.\n2. Choose 'Deactivate Account' in the chooser modal.\n3. Confirm.",
     "1. PUT to disable endpoint returns HTTP 200; enabled=false in DB.\n2. User row shows 'Disabled' status badge.\n3. The user can no longer log in (DisabledException).\n4. system_logs entry written."),

    ("Hard Delete User With Type-To-Confirm",
     "1. Logged in as admin.\n2. Target user exists.",
     "1. Click Delete; choose 'Permanently Delete'.\n2. In the strict confirm modal, type 'delete'.\n3. Click confirm.",
     "1. Confirm button is disabled until input equals 'delete' (case-insensitive).\n2. DELETE /api/admin/users/{id}/permanent returns HTTP 200.\n3. User row is removed from DataTable.\n4. system_logs entry written."),

    ("Re-enable Soft-Deleted User",
     "1. Logged in as admin.\n2. User has enabled=false.",
     "1. Click the re-enable button on a disabled user row.\n2. Confirm.",
     "1. PUT /api/admin/users/{id}/enable returns HTTP 200.\n2. enabled=true persisted.\n3. Status badge updates to Active.\n4. User can log in again."),

    # ---------- SYSTEM LOGS (logs.html) ----------
    ("View System Logs Default 7-Day Window",
     "1. Logged in as admin.\n2. system_logs table contains entries.",
     "1. Navigate to /logs.html.",
     "1. GET /api/logs returns logs from the last 7 days (default).\n2. DataTable displays user_id, username, role, action, ip_address, timestamp columns."),

    ("System Logs Custom Date Range Filter",
     "1. Logged in as admin.\n2. Logs exist across multiple dates.",
     "1. On /logs.html, choose 'Custom Range'.\n2. Set startDate and endDate (3-day window).\n3. Apply filter.",
     "1. GET /api/logs?startDate=...&endDate=... returns only entries within the range.\n2. Custom range filter takes precedence over rangeDays preset."),

    ("Export System Logs As CSV",
     "1. Logged in as admin.\n2. Logs page is loaded.",
     "1. Click 'Export' -> CSV with current date filter.",
     "1. GET /api/logs/export?format=csv returns HTTP 200 with text/csv content.\n2. Downloaded file contains filtered log rows with correct headers."),

    ("Export System Logs As XLSX",
     "1. Logged in as admin.",
     "1. Click 'Export' -> XLSX.",
     "1. Server returns an .xlsx (Apache POI generated) with filtered rows.\n2. Headers match: Timestamp, Username, Role, Action, IP."),

    ("Export System Logs As DOCX",
     "1. Logged in as admin.",
     "1. Click 'Export' -> DOCX.",
     "1. Server returns an .docx with a formatted table of logs.\n2. Content respects current date filter."),

    # ---------- EDIT ACCOUNT MODAL (all roles) ----------
    ("Edit Account Personal Details Thread",
     "1. User is logged in.",
     "1. Click account icon -> Edit Account.\n2. Open Personal Details tab.\n3. Update last name, first name, birthdate.\n4. Save.",
     "1. PUT /api/account/details returns HTTP 200.\n2. Age field updates live as birthdate changes.\n3. Changes persist; reload reflects new values."),

    ("Change Own Username Thread",
     "1. User is logged in.\n2. Target new username is unique.",
     "1. Edit Account -> Account Settings.\n2. Enter new username + current password.\n3. Save.",
     "1. PUT /api/account/profile returns HTTP 200.\n2. Session updated; subsequent /api/auth/me returns the new username."),

    ("Change Own Password Thread",
     "1. User is logged in.",
     "1. Edit Account -> Account Settings.\n2. Enter current password, then a new strong password (upper, lower, digit, special, 8+).\n3. Save.",
     "1. PUT /api/account/password returns HTTP 200.\n2. passwordChangedAt is updated.\n3. New password works on next login; old password fails."),

    ("Reject Weak Password On Self-Change",
     "1. User is logged in.",
     "1. Edit Account -> Account Settings.\n2. Enter new password 'short' (fails strong-rule).\n3. Save.",
     "1. HTTP 400 returned with validation error.\n2. Password is not updated."),

    ("Password Eye-Icon Toggle",
     "1. User is on any page with a password input.",
     "1. Type into the password field.\n2. Click the eye icon.\n3. Click it again.",
     "1. Input type toggles between password and text.\n2. Toggle is injected automatically on every password field by auth-guard.js."),

    # ---------- REGISTRAR DASHBOARD ----------
    ("Registrar Student Records Table Loads",
     "1. Logged in as registrar.\n2. student_records contains data.",
     "1. Navigate to /registrar.html.",
     "1. GET /api/registrar/student-records returns HTTP 200.\n2. DataTable shows 9 columns (Student ID, Name, Status, Batch, Course, Section, etc.).\n3. Search bar (320px) and status filter dropdown are visible."),

    ("Registrar Server-Side Search Thread",
     "1. Logged in as registrar.\n2. Records exist with matching surnames.",
     "1. Type a surname fragment into the DataTable search.",
     "1. Request sent with ?q=<term>.\n2. Server returns rows matching across 9 searchable fields.\n3. Table refreshes with results."),

    ("Registrar Batch Year Range Filter",
     "1. Logged in as registrar.\n2. Records exist across multiple batch years.",
     "1. Enter fromYear and toYear.\n2. Click Apply.",
     "1. Request includes ?fromYear=&toYear=.\n2. Only records within the year range are returned."),

    ("Registrar Status Filter Thread",
     "1. Logged in as registrar.\n2. Records exist with status Active, Submitted, Enrolling, Graduated.",
     "1. Select status 'Active' from the dropdown.",
     "1. Request includes ?status=Active.\n2. Only Active rows are returned (case-insensitive match)."),

    ("Open Student Detail Modal Thread",
     "1. Logged in as registrar.\n2. A student record exists.",
     "1. Click 'Open Details' on a row.",
     "1. GET /api/registrar/student-records/{id} returns full details.\n2. Modal displays personal details, parents, guardian.\n3. Status badge color matches (Active=green, Submitted=grey, Graduated=blue)."),

    # ---------- EDIT STUDENT RECORD (student-records.html) ----------
    ("Edit Student Record Full Update Thread",
     "1. Logged in as registrar.\n2. Existing student record selected.",
     "1. From /registrar.html click Edit on a record.\n2. On /student-records.html update last name, birthdate, parents fields.\n3. Save.",
     "1. PUT /api/registrar/student-records/{id} returns HTTP 200.\n2. Age auto-recomputed.\n3. Parents (Father/Mother) and Guardian rows upserted.\n4. system_logs entry written."),

    ("Edit Student OJT Upsert Thread",
     "1. Logged in as registrar.\n2. Existing student record open.",
     "1. Fill OJT company name, address, hours.\n2. Save.",
     "1. OJT row is upserted (existing OJT updated, or created if none).\n2. ojt_id PK preserved on update."),

    ("Edit Student TESDA Qualifications Thread",
     "1. Logged in as registrar.",
     "1. Fill TESDA fields in slots 1, 2.\n2. Save.\n3. Edit again, clear slot 2 and add slot 3.\n4. Save.",
     "1. Delete-all-then-insert pattern with explicit flush() avoids unique-constraint violation on (student_id, slot).\n2. Final state matches what was on screen."),

    ("Edit Student School Years Add/Delete Rows",
     "1. Logged in as registrar.",
     "1. Click 'Add Row' on School Years table 3 times.\n2. Fill row values.\n3. Save.\n4. Re-open, delete 1 row, save.",
     "1. Delete-all-then-insert pattern persists exactly the rows on screen.\n2. rowIndex reassigned on each save."),

    ("Unsaved Changes Guard Thread",
     "1. Logged in as registrar.\n2. Student edit form is open.",
     "1. Modify any field.\n2. Try to navigate away (back button or another nav link).",
     "1. beforeunload prompt shows 'You have unsaved changes...'.\n2. User can stay or leave."),

    ("Auto-Assign Current-Year Batch Thread",
     "1. Logged in as registrar.\n2. A batch row exists for the current year.\n3. A student record has no batch.",
     "1. Submit / save the student record with no batch set.",
     "1. Service auto-resolves current-year batch via findFirstByBatchYear and assigns it.\n2. student_records.batch_code is populated."),

    ("Delete Student Record Strict Confirm Thread",
     "1. Logged in as registrar.\n2. Student record exists with parents, guardian, education, uploads.",
     "1. Open detail modal.\n2. Click Delete.\n3. In strict modal, type 'delete'.\n4. Confirm.",
     "1. Confirm button enabled only when 'delete' typed.\n2. DELETE /api/registrar/student-records/{id} returns HTTP 204.\n3. Child rows (uploads, parents, guardian, education, school years, TESDA, OJT, documents, grades) deleted in FK order.\n4. Physical upload files removed."),

    # ---------- SUBJECTS (subjects.html) ----------
    ("Create Subject Thread",
     "1. Logged in as registrar.\n2. At least one qualification exists.",
     "1. Click 'Create Subject' on /subjects.html.\n2. Enter subject code, name, qualification, units.\n3. Save.",
     "1. POST /api/registrar/subjects returns HTTP 201.\n2. Row appears in DataTable.\n3. system_logs row written."),

    ("Edit Subject Thread",
     "1. Logged in as registrar.\n2. A subject exists.",
     "1. Click Edit on a subject row.\n2. Modify name and units (code is read-only).\n3. Save.",
     "1. PUT /api/registrar/subjects/{code} returns HTTP 200.\n2. Updated values appear; subject code unchanged."),

    ("Assign Trainer To Subject Thread",
     "1. Logged in as registrar.\n2. Trainer accounts exist (enabled).",
     "1. Click 'Assign Trainer' on a subject.\n2. Select a trainer.\n3. Save.",
     "1. PUT /api/registrar/subjects/{code}/trainer returns HTTP 200.\n2. Trainer badge updates on the row.\n3. system_logs entry written."),

    ("Delete Subject With FK Pre-Check",
     "1. Logged in as registrar.\n2. A subject is referenced by a class or a grade.",
     "1. Click Delete on the referenced subject.\n2. Type 'delete' in strict-confirm.\n3. Confirm.",
     "1. HTTP 400 returned with actionable message indicating classes/grades reference it.\n2. Subject is NOT deleted."),

    ("Delete Unreferenced Subject Thread",
     "1. Logged in as registrar.\n2. Subject has no class or grade references.",
     "1. Delete with strict confirm.",
     "1. DELETE /api/registrar/subjects/{code} returns HTTP 200.\n2. Row removed; system_logs entry written."),

    # ---------- CLASSES (classes.html) ----------
    ("Create Class Thread",
     "1. Logged in as registrar.\n2. Section, subject, and (optionally) a trainer exist.",
     "1. Click 'Create Class'.\n2. Choose section, subject (trainer auto-fills from subject default), confirm semester.\n3. Save.",
     "1. POST /api/registrar/classes returns HTTP 201.\n2. (section_code, subject_code, semester) uniqueness enforced.\n3. Class row appears with enrolledCount=0."),

    ("Create Class Duplicate Rejection",
     "1. A class already exists with the same (section, subject, semester).",
     "1. Create another class with the same triple.",
     "1. HTTP 400 or 409 returned with validation message.\n2. No duplicate row inserted."),

    ("Edit Class Trainer Reassignment",
     "1. Logged in as registrar.\n2. Class exists with a trainer assigned.",
     "1. Click 'Edit Trainer' on a class row.\n2. Choose a different trainer (or 'Unassigned').\n3. Save.",
     "1. PUT /api/registrar/classes/{id}/trainer returns HTTP 200.\n2. Row updates; 'Unassigned' shown italic if cleared.\n3. system_logs 'Assigned trainer X to class #N' or 'Unassigned trainer from class #N'."),

    ("Enroll Single Student To Class",
     "1. Class exists.\n2. Eligible student (Active/Submitted, same section, not yet enrolled) exists.",
     "1. Open Manage Students modal.\n2. Pick an eligible student.\n3. Click Enroll.",
     "1. POST /api/registrar/classes/enroll returns HTTP 201.\n2. enrolledCount increments.\n3. Student appears in class roster."),

    ("Bulk Enroll Whole Section Into Class",
     "1. Class exists.\n2. Section has multiple Active students, some already enrolled.",
     "1. Open Manage Students modal.\n2. Click 'Enroll Whole Section'.",
     "1. POST /api/registrar/classes/{id}/enroll-section returns counts: enrolledCount, skippedAlreadyEnrolled, skippedIneligible, totalConsidered.\n2. Success alert shows the counts.\n3. system_logs bulk-enroll row written."),

    ("Unenroll Student From Class",
     "1. Class has at least one enrollment.",
     "1. Open Manage Students.\n2. Click Remove on an enrolled student.\n3. Confirm.",
     "1. DELETE /api/registrar/enrollments/{id} returns HTTP 200.\n2. enrolledCount decrements.\n3. system_logs entry written."),

    ("Get Current Semester Thread",
     "1. Logged in as registrar.\n2. At least one batch exists.",
     "1. Open /classes.html.",
     "1. GET /api/registrar/classes/current-semester returns {semester: '<latest batch year>'}.\n2. Returned value pre-fills the Create Class modal."),

    # ---------- SECTIONS (sections.html) ----------
    ("Create Section Thread",
     "1. Logged in as registrar.\n2. A batch and course exist.",
     "1. Click 'Create Section'.\n2. Enter section code, section name; pick batch and course.\n3. Save.",
     "1. POST /api/registrar/sections returns HTTP 201.\n2. Row appears in DataTable; system_logs entry created."),

    ("Edit Section Name Thread",
     "1. A section exists.",
     "1. Click Edit on a section row.\n2. Change section name.\n3. Save.",
     "1. PUT /api/registrar/sections/{code} returns HTTP 200.\n2. Name updated on row."),

    ("Assign Students To Section Thread",
     "1. Section exists.\n2. Eligible students exist (no section, status Submitted, matching batch/course filter).",
     "1. Open Manage Students -> Add Students tab.\n2. Apply batch/course filter.\n3. Select students and Assign.",
     "1. POST /api/registrar/sections/{code}/students returns assignedCount + skipped lists.\n2. Students promoted Submitted -> Active.\n3. system_logs row written."),

    ("Remove Student From Section Thread",
     "1. Section contains one or more students.",
     "1. Open Manage Students -> Current Students.\n2. Click Remove on a student.\n3. Confirm.",
     "1. DELETE /api/registrar/sections/{code}/students/{studentId} returns HTTP 200.\n2. Student.section_code cleared.\n3. Student status reverts Active -> Submitted.\n4. Cascade removes class_enrollments tied to this section."),

    ("Delete Section FK Pre-Check Thread",
     "1. Section is referenced by one or more classes.",
     "1. Click Delete on the section.\n2. Type 'delete' to confirm.",
     "1. HTTP 400 with message 'Cannot delete section: one or more classes still reference it. Remove those classes first.'.\n2. Section is not deleted."),

    ("Delete Unreferenced Section",
     "1. Section has no classes and no students.",
     "1. Delete with strict confirm.",
     "1. DELETE /api/registrar/sections/{code} returns HTTP 200.\n2. Row removed; system_logs entry written."),

    # ---------- TRAINER PAGES ----------
    ("Trainer Dashboard Loads",
     "1. Logged in as trainer.",
     "1. Navigate to /trainer.html.",
     "1. Welcome hero + quick-link cards render.\n2. 3-link navbar (Home, My Subjects, My Classes) visible.\n3. /api/auth/me returns ROLE_TRAINER."),

    ("Trainer My Subjects Roster Drill-Down",
     "1. Trainer is assigned to at least one class for some subject.",
     "1. Navigate to /trainer-subjects.html.\n2. Click a subject row.",
     "1. GET /api/trainer/subjects returns assigned subjects with enrolledCount + section/course names.\n2. Clicking expands GET /api/trainer/subjects/{code}/students panel.\n3. Roster displays students from all classes for that subject."),

    ("Trainer My Classes Roster Drill-Down",
     "1. Trainer has at least one assigned class.",
     "1. Navigate to /trainer-classes.html.\n2. Click a class row.",
     "1. GET /api/trainer/classes returns the trainer's classes.\n2. GET /api/trainer/classes/{id}/students returns the roster.\n3. Roster panel expands inline."),

    ("Trainer Ownership Guard Thread",
     "1. Logged in as trainer.\n2. A class exists owned by a DIFFERENT trainer.",
     "1. Manually call GET /api/trainer/classes/{otherClassId}/students.",
     "1. HTTP 400 returned (IllegalArgumentException).\n2. Roster is not exposed."),

    # ---------- GRADE INPUT ----------
    ("Open Grade Input Modal Thread",
     "1. Trainer owns a class with enrolled students.",
     "1. From /trainer-classes.html, click Grade Input on the class.",
     "1. Modal opens and lists all enrolled students.\n2. Rows merge existing grade rows with enrollments (upsert-friendly view)."),

    ("Save Grades Thread",
     "1. Modal open with at least one grade filled.",
     "1. Enter midterm and finals grades for 1+ students.\n2. Click Save.",
     "1. Empty rows are filtered out client-side.\n2. PUT /api/trainer/classes/{classId}/grades returns HTTP 200.\n3. final_grade is computed and persisted.\n4. DB row: student_id (varchar business key), grades correctly linked via referencedColumnName."),

    ("Save Grades Empty Submission Warning",
     "1. Modal open; no row has any grade value.",
     "1. Click Save.",
     "1. JS warning 'No grades entered' displayed.\n2. No POST request issued."),

    ("Lock Grades Thread",
     "1. Trainer has saved grades for a class.",
     "1. Click 'Lock Grades'.\n2. Confirm.",
     "1. POST /api/trainer/classes/{classId}/grades/lock returns HTTP 200.\n2. locked=1 in DB.\n3. Inputs become read-only in modal."),

    ("Unlock Grades Thread",
     "1. Grades are locked.",
     "1. Click 'Unlock Grades'.\n2. Confirm.",
     "1. POST /api/trainer/classes/{classId}/grades/unlock returns HTTP 200.\n2. locked=0.\n3. Inputs become editable again."),

    # ---------- STUDENT PORTAL ----------
    ("Student Portal Welcome Page Loads (Public)",
     "1. No authentication required.",
     "1. Navigate to /student-portal.html anonymously.",
     "1. Page loads without redirect (permitAll).\n2. Login-style card with name inputs visible.\n3. No account dropdown."),

    ("Student Duplicate Name Check Thread",
     "1. A student_records row already exists with names matching the input (case-insensitive).",
     "1. On /student-portal.html, enter Last, First, Middle names matching an existing record.\n2. Submit.",
     "1. GET /api/student-portal/check-duplicate returns true.\n2. Duplicate alert shown on portal card.\n3. User is NOT taken to /student-details.html."),

    ("Student Portal New Submission Thread",
     "1. No duplicate match.",
     "1. Enter unique names.\n2. Submit.",
     "1. Names passed via URL params to /student-details.html (no DB insert at this stage).\n2. Wizard page loads."),

    ("Student Details Wizard Step 1 Validation",
     "1. On /student-details.html.",
     "1. Try to advance past Step 1 with required fields empty (e.g., civil_status).",
     "1. Field-level validation triggers.\n2. Cannot navigate to next step.\n3. STEP_CUSTOM_VALIDATORS[0] enforces required fields."),

    ("Student Details Baptismal Optional Thread",
     "1. On Step 2 of wizard.",
     "1. Check 'Baptized'.\n2. Fill Baptism Date + Place.\n3. Do NOT upload baptismal cert.\n4. Continue.",
     "1. Step 2 passes validation (cert is now optional).\n2. Wizard advances to next step."),

    ("Student Details Education Section",
     "1. On Step 3 (Educational Background).",
     "1. Fill 4 rows (School Name, Address, Course, School Year).",
     "1. Table has 4 columns (no Grade/Year, no Semester).\n2. Payload's educationHistory does not include gradeYear/semester."),

    ("Student Details Deferred File Upload Thread",
     "1. On Step 1 wizard.\n2. ID Photo input present (not required).",
     "1. Select a file for ID Photo (preview shows).\n2. Continue through wizard without uploading.\n3. On final step, click Submit.",
     "1. JSON submit happens first (POST /api/student/{id}/submit) within @Transactional.\n2. Pending file uploaded only AFTER record is created (FK satisfied).\n3. 'Uploaded: <filename>' status shown."),

    ("Student Details Submit Atomic Thread",
     "1. All wizard steps validated.",
     "1. Click Submit on the final step.",
     "1. POST /api/student/{id}/submit returns HTTP 200.\n2. Single @Transactional save creates student_records + parents + guardian + education + school_years rows.\n3. New student_status = 'Submitted'.\n4. Confirmation message displayed."),

    ("Student Details Double-Submit Guard",
     "1. Student already submitted.",
     "1. Attempt to submit the same student details again.",
     "1. Service rejects with HTTP 400 (already submitted).\n2. No duplicate rows persisted."),

    # ---------- NON-FUNCTIONAL / CROSS-CUTTING ----------
    ("Generic 500 Hardening Thread",
     "1. Logged in as registrar.\n2. An unexpected server-side exception is forced.",
     "1. Trigger an unhandled exception path.",
     "1. Response body is 'An unexpected error occurred. Please contact the administrator.' (no SQL/internals leaked).\n2. Full stack logged server-side via SLF4J."),

    ("Data Integrity 409 Thread",
     "1. A DataIntegrityViolationException is forced (e.g., FK violation).",
     "1. Trigger the violating operation.",
     "1. HTTP 409 returned with a generic conflict message.\n2. No raw SQL exposed to client."),

    ("System Log Append-Only Constraint",
     "1. system_logs has rows.",
     "1. Attempt to UPDATE or DELETE a row in code review / SQL audit.",
     "1. Application code never updates or deletes log rows (verified by repository review).\n2. Table is append-only by convention."),

    ("CSRF Disabled For /api/** Form Behavior",
     "1. Application running.",
     "1. POST to an /api/** endpoint without CSRF token.\n2. POST to a form endpoint without CSRF token.",
     "1. /api/** call succeeds (CSRF disabled for APIs).\n2. Form POSTs without token are rejected."),

    ("Session Cookie Security Attributes",
     "1. User logs in.",
     "1. Inspect JSESSIONID cookie via browser dev tools.",
     "1. Cookie is HttpOnly.\n2. SameSite=Lax.\n3. Session timeout: 30 minutes."),

    ("Concurrent Users Load Thread",
     "1. Production-like environment.\n2. 50+ concurrent users target.",
     "1. Simulate 50 concurrent logins across roles.\n2. Each performs typical CRUD.",
     "1. All requests respond < acceptable latency.\n2. No deadlocks or session bleed-through.\n3. Server RAM remains < 6GB."),

    ("Document Upload Performance Thread",
     "1. Logged in as registrar.\n2. A document file < 5MB.",
     "1. Upload a document for a student.",
     "1. Upload completes in < 3 seconds.\n2. File saved and metadata persisted."),

    ("Student Record Retrieval Performance",
     "1. student_records contains 200+ rows.",
     "1. Open /registrar.html.",
     "1. Initial table load completes in < 5 seconds.\n2. Server-side pagination/search responses are snappy."),
]

# Write rows
for i, (title, pre, steps, exp) in enumerate(cases, start=1):
    test_id = f"TC-{i:03d}"
    ws.append([test_id, title, pre, steps, exp])

# Column widths
ws.column_dimensions['A'].width = 10
ws.column_dimensions['B'].width = 42
ws.column_dimensions['C'].width = 48
ws.column_dimensions['D'].width = 60
ws.column_dimensions['E'].width = 60

# Apply wrap + border + alternating fill
alt_fill = PatternFill(start_color="F4F7FB", end_color="F4F7FB", fill_type="solid")
for row in range(2, ws.max_row + 1):
    for col in range(1, len(headers) + 1):
        c = ws.cell(row=row, column=col)
        c.alignment = wrap
        c.border = border
        if row % 2 == 0:
            c.fill = alt_fill
    ws.row_dimensions[row].height = None  # auto

# Bold Test ID column
for row in range(2, ws.max_row + 1):
    ws.cell(row=row, column=1).font = Font(bold=True)

# Freeze header
ws.freeze_panes = "A2"

out_path = r"c:\Users\emman\Desktop\Capstone Files\DEVELOPMENT\AnihanProject\capstonepaper\Anihan_SRMS_Thread_Testing_Cases.xlsx"
wb.save(out_path)
print(f"Saved: {out_path}")
print(f"Total test cases: {len(cases)}")
