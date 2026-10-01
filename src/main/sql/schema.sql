-- ============================================================
-- schema.sql — Clean Schema + Default Security Questions
-- Updated: 2026-10-01 (added documents.document_label — optional custom name
--            for "Others" documents — see
--            migrations/2026-10-01-add-documents-document-label.sql)
-- Updated: 2026-09-30 (removed account, student, and academic lookup seeds;
--            retained the 6 default security questions)
-- Updated: 2026-09-22 (widened documents.file_type to VARCHAR(100) so
--            uploaded DOCX/XLSX OpenXML MIME type strings fit — see
--            migrations/2026-09-22-widen-documents-file-type.sql)
-- Updated: 2026-09-19 (added security_questions + user_security_answers for
--            the "forgot password" feature; added users.security_locked /
--            failed_security_attempts / security_lockout_started_at;
--            users.email is now UNIQUE)
-- Updated: 2026-08-29 (dropped subjects.trainer_id; grades overhauled to the
--            TESDA model: final_percentage / re_exam_percentage / grade_status /
--            hours_rendered replace midterm_grade / finals_grade / hours_studied)
-- Updated: 2026-08-27 (added student_records.student_number — the
--                       registrar-controlled, nullable student number)
-- Updated: 2026-05-19 (added grades restructure for trainer grading)
-- Updated: 2026-05-09 (added classes, class_enrollments, subjects.trainer_id)
-- Purpose: Set up a fresh AnihanSRMS database with:
--            * all 21 table definitions
--            * 6 default security questions for account recovery
--          Accounts are inserted manually. Student records, academic lookup
--          tables, and system logs start empty on a fresh installation.
--          Configure courses, batches, sections, qualifications, and subjects
--          before using the corresponding academic workflows.
--          This script does not clear existing data or migrate existing tables.
--          Run once on a fresh database; rerunning duplicates the question seeds.
-- Tables: 21
--
-- Existing databases that predate the 2026-05-05 schema-drift fix
-- should also run src/main/sql/migrations/2026-05-05-fix-schema-drift.sql
-- once to align student_records / parents / other_guardians with the
-- canonical column nullability and pick up student_records.civil_status.
--
-- Existing databases that predate 2026-05-09 should also run
-- src/main/sql/migrations/2026-05-09-classes-and-trainers.sql to add
-- the classes and class_enrollments tables.
--
-- Existing databases that still have subjects.trainer_id should run
-- src/main/sql/migrations/2026-08-29-drop-subjects-trainer-id.sql to
-- drop it (trainer assignment is class-level only).
--
-- Existing databases that predate 2026-09-19 should run
-- src/main/sql/migrations/2026-09-19-security-questions.sql to add the
-- security-questions tables/columns and update legacy seed-account emails.
--
-- Existing databases that predate 2026-05-19 should also run
-- src/main/sql/migrations/2026-05-19-grades-restructure.sql to add
-- class-scoped grading, midterm/finals split, locking, and GWA support.
--
-- Existing databases that predate 2026-08-26 should also run
-- src/main/sql/migrations/2026-08-26-subjects-competency-type.sql to
-- add subjects.competency_type (BASIC/COMMON/CORE) and relax
-- subjects.qualification_code to nullable (Basic/Common subjects are
-- not tied to a single qualification — only Core subjects are).
--
-- Existing databases that predate 2026-08-26 PM should also run
-- src/main/sql/migrations/2026-08-26-subjects-code-update-cascade.sql
-- so that renaming a subject's code (Edit Subject) cascades into
-- classes.subject_code and grades.subject_code instead of being
-- rejected by MySQL's default FK behavior.
--
-- Existing databases that predate 2026-08-29 should also run
-- src/main/sql/migrations/2026-08-29-drop-subjects-trainer-id.sql and
-- src/main/sql/migrations/2026-08-29-grades-overhaul.sql (delete any existing
-- grade rows first — the overhaul drops the old component-grade columns).
-- ============================================================

CREATE DATABASE IF NOT EXISTS AnihanSRMS DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE AnihanSRMS;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- TABLE: batches
-- ============================================================
CREATE TABLE IF NOT EXISTS batches (
    batch_code VARCHAR(20) NOT NULL PRIMARY KEY,
    batch_year YEAR NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: courses
-- ============================================================
CREATE TABLE IF NOT EXISTS courses (
    course_code VARCHAR(20) NOT NULL PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: qualifications
-- ============================================================
CREATE TABLE IF NOT EXISTS qualifications (
    qualification_code INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    qualification_name VARCHAR(255) NOT NULL,
    qualification_description VARCHAR(255) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: sections
-- ============================================================
CREATE TABLE IF NOT EXISTS sections (
    section_code VARCHAR(20) NOT NULL PRIMARY KEY,
    section VARCHAR(25) NOT NULL,
    batch_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: subjects
-- ============================================================
-- competency_type: BASIC / COMMON / CORE (TESDA-defined, fixed set).
-- Basic and Common subjects are shared across all qualifications and
-- are not tied to a single NC program, so qualification_code is
-- nullable — only Core subjects require one.
-- Trainers are NOT stored here: a subject has many classes, each with its
-- own trainer (classes.trainer_id), so a subject has many trainers. The
-- Subjects page derives that list from the subject's classes.
CREATE TABLE IF NOT EXISTS subjects (
    subject_code VARCHAR(20) NOT NULL PRIMARY KEY,
    subject_name VARCHAR(255) NOT NULL,
    qualification_code INT NULL,
    competency_type VARCHAR(15) NOT NULL,
    units INT NOT NULL,
    FOREIGN KEY (qualification_code) REFERENCES qualifications (qualification_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    middlename VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL DEFAULT '2000-01-01',
    age INT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(15) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    password_changed_at DATETIME NULL,
    -- Security-question lockout state — deliberately separate from `enabled`
    -- (admin deactivate/re-enable). See migrations/2026-09-19-security-questions.sql.
    security_locked TINYINT(1) NOT NULL DEFAULT 0,
    failed_security_attempts INT NOT NULL DEFAULT 0,
    security_lockout_started_at DATETIME NULL,
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: security_questions
-- Catalog of the 6 fixed default security questions. Wording is fixed —
-- do not alter. Users may also supply their own custom question instead of
-- picking from this list (stored per-user in user_security_answers).
-- ============================================================
CREATE TABLE IF NOT EXISTS security_questions (
    question_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_text VARCHAR(255) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: user_security_answers
-- Each user picks exactly 2 questions for "forgot password" recovery — one
-- row per slot. A row references a default question (question_id) OR
-- carries its own custom_question text, never both (chk_question_xor_custom).
-- Custom question text is plaintext (it's not the secret); answer_hash is
-- BCrypt, same as account passwords. uq_user_question stops picking the
-- same default question twice; MySQL allows multiple NULLs in a unique
-- index, so custom-question rows (question_id NULL) never collide there.
-- ============================================================
CREATE TABLE IF NOT EXISTS user_security_answers (
    answer_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    question_id INT NULL,
    custom_question VARCHAR(255) NULL,
    answer_hash VARCHAR(255) NOT NULL,
    slot INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES security_questions (question_id),
    UNIQUE KEY uq_user_slot (user_id, slot),
    UNIQUE KEY uq_user_question (user_id, question_id),
    CONSTRAINT chk_question_xor_custom CHECK (
        (question_id IS NULL) <> (custom_question IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: student_records
-- Many columns are nullable — students fill them in during the
-- enrollment wizard; batch/course/section are assigned later
-- by the Registrar.
--
-- Two identifiers, deliberately:
--   * student_id      — internal system reference, auto-generated when the
--                       enrollment wizard starts. NOT NULL because it is the
--                       FK target of 10 child tables. Never edited.
--   * student_number  — the REAL student number, owned by the Registrar and
--                       the archive import. NULL until assigned; nothing
--                       auto-generates it. UNIQUE allows many NULLs in MySQL.
-- ============================================================
CREATE TABLE IF NOT EXISTS student_records (
    record_id INT NOT NULL AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    student_number VARCHAR(20) NULL,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NULL,
    birthdate DATE NULL,
    age INT NULL,
    sex VARCHAR(10) NULL,
    civil_status VARCHAR(50) NULL,
    permanent_address VARCHAR(255) NULL,
    temporary_address VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    contact_no VARCHAR(255) NULL,
    religion VARCHAR(255) NULL,
    baptized TINYINT(1) NOT NULL DEFAULT 0,
    baptism_date DATE NULL,
    baptism_place VARCHAR(255) NULL,
    sibling_count INT NULL,
    brother_count INT NULL,
    sister_count INT NULL,
    batch_code VARCHAR(20) NULL,
    course_code VARCHAR(20) NULL,
    section_code VARCHAR(20) NULL,
    profile_picture MEDIUMBLOB NULL,
    enrollment_date DATE NULL,
    student_status VARCHAR(25) NOT NULL DEFAULT 'Enrolling',
    PRIMARY KEY (record_id),
    UNIQUE KEY idx_student_id (student_id),
    UNIQUE KEY uq_student_number (student_number),
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code),
    FOREIGN KEY (section_code) REFERENCES sections (section_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: parents
-- ============================================================
CREATE TABLE IF NOT EXISTS parents (
    parent_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    relation VARCHAR(20) NOT NULL,
    family_name VARCHAR(255) NULL,
    first_name VARCHAR(255) NULL,
    middle_name VARCHAR(255) NULL,
    birthdate DATE NULL,
    occupation VARCHAR(255) NULL,
    est_income DECIMAL(15, 2) NULL,
    contact_no VARCHAR(20) NULL,
    email VARCHAR(255) NULL,
    address VARCHAR(255) NULL,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: other_guardians
-- ============================================================
CREATE TABLE IF NOT EXISTS other_guardians (
    guardian_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    relation VARCHAR(20) NULL,
    last_name VARCHAR(255) NULL,
    first_name VARCHAR(255) NULL,
    middle_name VARCHAR(255) NULL,
    birthdate DATE NULL,
    address VARCHAR(255) NULL,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: documents
-- ============================================================
CREATE TABLE IF NOT EXISTS documents (
    document_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    document_label VARCHAR(100) NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size INT NOT NULL,
    content_data LONGBLOB NOT NULL,
    upload_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: grades
-- Class-scoped grading: the trainer enters a raw percentage (final_percentage,
-- transmuted to the 1.00-5.00 final_grade equivalent) OR a status code
-- (grade_status: C/FA/INC/D). remarks is a derived token (COMPETENT /
-- NOT_COMPETENT / NULL). hours_rendered is attendance hours (TESDA), separate
-- from the fixed curriculum hours. locked/locked_at freeze a class's grades.
-- ============================================================
CREATE TABLE IF NOT EXISTS grades (
    grade_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    final_grade DECIMAL(5, 2) NULL, -- 1.00-5.00 equivalent, transmuted from final_percentage
    re_exam_grade DECIMAL(5, 2) NULL, -- 1.00-5.00 equivalent, transmuted from re_exam_percentage
    grade_status VARCHAR(5) NULL, -- C / FA / INC / D — used INSTEAD of a percentage
    hours_rendered DECIMAL(5, 2) NULL, -- attendance hours (TESDA); separate from curriculum hours
    remarks VARCHAR(20) NULL, -- derived token: COMPETENT / NOT_COMPETENT / NULL
    class_id INT NULL,
    final_percentage DECIMAL(5, 2) NULL, -- raw % the trainer enters (NULL when grade_status is used)
    re_exam_percentage DECIMAL(5, 2) NULL, -- raw % for the optional re-exam (only when the final failed)
    locked TINYINT(1) NOT NULL DEFAULT 0,
    locked_at DATETIME NULL,
    UNIQUE KEY uq_grade_student_class (class_id, student_id),
    FOREIGN KEY (student_id) REFERENCES student_records (student_id),
    CONSTRAINT fk_grades_subject FOREIGN KEY (subject_code) REFERENCES subjects (subject_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_grades_class FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: system_logs (structure only, no data)
-- ============================================================
CREATE TABLE IF NOT EXISTS system_logs (
    log_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    username VARCHAR(255) NOT NULL,
    role VARCHAR(15) NOT NULL,
    action VARCHAR(500) NOT NULL,
    ip_address VARCHAR(45) NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_system_logs_timestamp (timestamp DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: student_education
-- Prior school history per student (one row per level).
-- ============================================================
CREATE TABLE IF NOT EXISTS student_education (
    education_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    level VARCHAR(50) NOT NULL,
    school_name VARCHAR(255) NULL,
    school_address VARCHAR(255) NULL,
    grade_year VARCHAR(50) NULL,
    semester VARCHAR(20) NULL,
    ended_year VARCHAR(20) NULL,
    UNIQUE KEY uq_student_education (student_id, level),
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: student_school_years
-- Semesters the student attended at Anihan.
-- ============================================================
CREATE TABLE IF NOT EXISTS student_school_years (
    school_year_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    row_index INT NOT NULL,
    sy_start VARCHAR(20) NULL,
    sem_start VARCHAR(20) NULL,
    sy_end VARCHAR(20) NULL,
    sem_end VARCHAR(20) NULL,
    remarks VARCHAR(255) NULL,
    UNIQUE KEY uq_student_school_year (student_id, row_index),
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: student_ojt
-- One OJT record per student.
-- ============================================================
CREATE TABLE IF NOT EXISTS student_ojt (
    ojt_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    company_name VARCHAR(255) NULL,
    company_address VARCHAR(255) NULL,
    hours_rendered DECIMAL(8, 2) NULL,
    UNIQUE KEY uq_student_ojt (student_id),
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: student_tesda_qualifications
-- Up to 3 TESDA qualification slots per student.
-- ============================================================
CREATE TABLE IF NOT EXISTS student_tesda_qualifications (
    qual_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    slot INT NOT NULL,
    title VARCHAR(255) NULL,
    center_address VARCHAR(255) NULL,
    assessment_date DATE NULL,
    result VARCHAR(50) NULL,
    UNIQUE KEY uq_student_tesda_qual (student_id, slot),
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: student_uploads
-- File metadata for ID photo and baptismal cert uploads.
-- Files are stored on disk, not as BLOBs.
-- ============================================================
CREATE TABLE IF NOT EXISTS student_uploads (
    upload_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    kind VARCHAR(30) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NULL,
    mime_type VARCHAR(100) NULL,
    size_bytes BIGINT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: classes
-- Pairs a section with a subject for a given semester (batch year),
-- with an optional trainer assigned to teach the class.
-- ============================================================
CREATE TABLE IF NOT EXISTS classes (
    class_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    trainer_id INT NULL,
    semester VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_class (
        section_code,
        subject_code,
        semester
    ),
    FOREIGN KEY (section_code) REFERENCES sections (section_code),
    CONSTRAINT fk_classes_subject FOREIGN KEY (subject_code) REFERENCES subjects (subject_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT classes_ibfk_3 FOREIGN KEY (trainer_id) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: class_enrollments
-- Links a student to a specific class (registrar-managed).
-- ============================================================
CREATE TABLE IF NOT EXISTS class_enrollments (
    enrollment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id INT NOT NULL,
    student_id VARCHAR(20) NOT NULL,
    enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_class_student (class_id, student_id),
    FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- SEED DATA: Default Security Questions (fixed wording — do not alter)
-- ============================================================
INSERT INTO
    security_questions (question_text)
VALUES ('What is your favorite color'),
    (
        'What is your favorite vacation place?'
    ),
    ('What is your favorite song?'),
    (
        'Who is your favorite artist?'
    ),
    ('What is your favorite food?'),
    (
        'What is the name of your pet?'
    );
