-- ============================================================
-- schema.sql — Clean Schema (Structure Only)
-- Updated: 2026-06-22 (verified against live DB; reconciled grades
--                       column order; seed data split into separate files)
-- Updated: 2026-05-19 (added grades restructure for trainer grading)
-- Updated: 2026-05-09 (added classes, class_enrollments, subjects.trainer_id,
--                       seeded qualifications + subjects)
-- Purpose: Set up a fresh AnihanSRMS database structure (19 tables).
--          This file contains ONLY table definitions — no seed data.
--
--          For a working fresh install, run the seed files AFTER this one:
--            1. seed-accounts.sql   — the 3 login accounts (required)
--            2. seed-lookups.sql    — course / batches / sections /
--                                     qualifications / subjects (required for
--                                     enrollment + class management to work)
--            3. seed-sample-students.sql — 5 demo students (optional, dev only)
--
--          Apply order:  schema.sql → seed-accounts.sql → seed-lookups.sql
--                        [→ seed-sample-students.sql]
-- Tables: 19
--
-- Existing databases that predate the 2026-05-05 schema-drift fix
-- should also run src/main/sql/migrations/2026-05-05-fix-schema-drift.sql
-- once to align student_records / parents / other_guardians with the
-- canonical column nullability and pick up student_records.civil_status.
--
-- Existing databases that predate 2026-05-09 should also run
-- src/main/sql/migrations/2026-05-09-classes-and-trainers.sql to add
-- the classes and class_enrollments tables and the subjects.trainer_id
-- column.
--
-- Existing databases that predate 2026-05-19 should also run
-- src/main/sql/migrations/2026-05-19-grades-restructure.sql to add
-- class-scoped grading, midterm/finals split, locking, and GWA support.
-- ============================================================

CREATE DATABASE IF NOT EXISTS AnihanSRMS
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE AnihanSRMS;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- TABLE: batches
-- ============================================================
CREATE TABLE IF NOT EXISTS batches (
    batch_code VARCHAR(20) NOT NULL PRIMARY KEY,
    batch_year YEAR NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: courses
-- ============================================================
CREATE TABLE IF NOT EXISTS courses (
    course_code VARCHAR(20) NOT NULL PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: qualifications
-- ============================================================
CREATE TABLE IF NOT EXISTS qualifications (
    qualification_code INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    qualification_name VARCHAR(255) NOT NULL,
    qualification_description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: subjects
-- trainer_id is an optional default trainer for this subject.
-- ============================================================
CREATE TABLE IF NOT EXISTS subjects (
    subject_code VARCHAR(20) NOT NULL PRIMARY KEY,
    subject_name VARCHAR(255) NOT NULL,
    qualification_code INT NOT NULL,
    units INT NOT NULL,
    trainer_id INT NULL,
    FOREIGN KEY (qualification_code) REFERENCES qualifications (qualification_code),
    CONSTRAINT fk_subjects_trainer FOREIGN KEY (trainer_id) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    middlename VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL DEFAULT '2000-01-01',
    age INT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(15) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    password_changed_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: student_records
-- Many columns are nullable — students fill them in during the
-- enrollment wizard; batch/course/section are assigned later
-- by the Registrar.
-- ============================================================
CREATE TABLE IF NOT EXISTS student_records (
    record_id INT NOT NULL AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
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
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code),
    FOREIGN KEY (section_code) REFERENCES sections (section_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: documents
-- ============================================================
CREATE TABLE IF NOT EXISTS documents (
    document_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size INT NOT NULL,
    content_data LONGBLOB NOT NULL,
    upload_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: grades
-- Class-scoped grading with midterm/finals, locking, and GWA support.
-- ============================================================
CREATE TABLE IF NOT EXISTS grades (
    grade_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    final_grade DECIMAL(5, 2) NULL,
    re_exam_grade DECIMAL(5, 2) NULL,
    hours_studied DECIMAL(5, 2) NULL,
    remarks VARCHAR(255) NULL,
    class_id INT NULL,
    midterm_grade DECIMAL(5, 2) NULL,
    finals_grade DECIMAL(5, 2) NULL,
    locked TINYINT(1) NOT NULL DEFAULT 0,
    locked_at DATETIME NULL,
    UNIQUE KEY uq_grade_student_class (class_id, student_id),
    FOREIGN KEY (student_id) REFERENCES student_records (student_id),
    FOREIGN KEY (subject_code) REFERENCES subjects (subject_code),
    FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: classes
-- Pairs a section with a subject for a given semester (batch year),
-- with an optional trainer assigned to teach the class.
-- ============================================================
CREATE TABLE IF NOT EXISTS classes (
    class_id     INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    trainer_id   INT NULL,
    semester     VARCHAR(20) NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_class (section_code, subject_code, semester),
    FOREIGN KEY (section_code) REFERENCES sections(section_code),
    FOREIGN KEY (subject_code) REFERENCES subjects(subject_code),
    FOREIGN KEY (trainer_id)   REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: class_enrollments
-- Links a student to a specific class (registrar-managed).
-- ============================================================
CREATE TABLE IF NOT EXISTS class_enrollments (
    enrollment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id      INT NOT NULL,
    student_id    VARCHAR(20) NOT NULL,
    enrolled_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_class_student (class_id, student_id),
    FOREIGN KEY (class_id)   REFERENCES classes(class_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES student_records(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- END OF STRUCTURE.
-- Seed data lives in separate files — run them after this one:
--   seed-accounts.sql        (required — login accounts)
--   seed-lookups.sql         (required — course/batches/sections/subjects)
--   seed-sample-students.sql (optional — demo students, dev only)
-- ============================================================
