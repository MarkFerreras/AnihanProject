-- ============================================================
-- AnihanSRMS.sql — Full Database Schema + Accounts
-- Updated: 2026-05-05 (post schema-drift remediation)
-- Purpose: Clone the AnihanSRMS database to a new device.
--          Includes DDL for all 17 tables and 3 user accounts.
--
-- For fresh installs prefer src/main/sql/schema.sql, which also
-- seeds 5 sample student records and the lookup data (course,
-- batches, sections) needed for the registrar/student flows.
--
-- Existing databases that predate 2026-05-05 should run the
-- one-shot migration src/main/sql/migrations/2026-05-05-fix-schema-drift.sql
-- to add student_records.civil_status and relax the column nullability
-- on student_records / parents / other_guardians.
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
-- ============================================================
CREATE TABLE IF NOT EXISTS subjects (
    subject_code VARCHAR(20) NOT NULL PRIMARY KEY,
    subject_name VARCHAR(255) NOT NULL,
    qualification_code INT NOT NULL,
    units INT NOT NULL,
    FOREIGN KEY (qualification_code) REFERENCES qualifications (qualification_code)
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
    middle_name VARCHAR(255) NOT NULL,
    birthdate DATE NULL,
    age INT NULL,                           -- calculated from birthdate; null until Step 1 is saved
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
-- ============================================================
CREATE TABLE IF NOT EXISTS grades (
    grade_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    final_grade DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    re_exam_grade DECIMAL(5, 2) NULL,
    hours_studied DECIMAL(3, 2) NOT NULL,
    remarks VARCHAR(255) NOT NULL,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id),
    FOREIGN KEY (subject_code) REFERENCES subjects (subject_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: system_logs
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

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- DATA: users (3 accounts from live database)
-- admin / registrar passwords: password123
-- trainer password: password123 (reset by admin on 2026-04-16)
-- ============================================================
INSERT INTO users (user_id, username, password, lastname, firstname, middlename, birthdate, age, email, role, enabled, password_changed_at) VALUES
(10, 'admin',     '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Ferreas',    'Mark',  'Pretopia', '2004-01-01', 22, 'mark@example.com',    'ROLE_ADMIN',     1, NULL),
(11, 'registrar', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Registrar',  'Reg',   'B',        '1995-05-05', 29, 'reg@example.com',     'ROLE_REGISTRAR', 1, NULL),
(12, 'trainer',   '$2a$10$IM6yEO9HpxmNE.cm2k1SUOGBkIDJwfjIwBg..ia8JgZIHULz.STHK', 'Trainer',    'Train', 'C',        '1985-10-10', 39, 'trainer@example.com', 'ROLE_TRAINER',   1, '2026-04-16 12:07:31');