-- ============================================================
-- schema.sql — Clean Schema + Seed Accounts + Sample Students
-- Updated: 2026-05-05 (post schema-drift remediation)
-- Purpose: Set up a fresh AnihanSRMS database with:
--            * 3 test accounts (admin, registrar, trainer)
--            * lookup data (1 course, 3 batches, 3 sections)
--            * 5 sample student records for development/testing
--          No system log data is included.
-- Tables: 17
--
-- Existing databases that predate the 2026-05-05 schema-drift fix
-- should also run src/main/sql/migrations/2026-05-05-fix-schema-drift.sql
-- once to align student_records / parents / other_guardians with the
-- canonical column nullability and pick up student_records.civil_status.
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

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- SEED DATA: 3 Dummy Accounts
-- Password for ALL accounts: password123
-- BCrypt hash: $2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6
-- ============================================================
INSERT INTO users (username, password, lastname, firstname, middlename, birthdate, age, email, role, enabled, password_changed_at) VALUES
('admin',     '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Dela Cruz',  'Juan',    'Santos',   '1995-06-15', 30, 'juan.delacruz@example.com', 'ROLE_ADMIN',     1, NULL),
('registrar', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Reyes',      'Maria',   'Garcia',   '1990-03-22', 36, 'maria.reyes@example.com',   'ROLE_REGISTRAR', 1, NULL),
('trainer',   '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Santos',     'Carlos',  'Mendoza',  '1988-11-08', 37, 'carlos.santos@example.com', 'ROLE_TRAINER',   1, NULL);

-- ============================================================
-- SEED DATA: Lookup Tables (course, batches, sections)
-- Required as foreign-key targets for the dummy student records below.
-- ============================================================
INSERT INTO courses (course_code, course_name) VALUES
('CARS', 'Culinary Arts and Restaurant Services');

INSERT INTO batches (batch_code, batch_year) VALUES
('B2024A', 2024),
('B2025A', 2025),
('B2026A', 2026);

INSERT INTO sections (section_code, section, batch_code, course_code) VALUES
('SEC-A24', 'Section A 2024', 'B2024A', 'CARS'),
('SEC-A25', 'Section A 2025', 'B2025A', 'CARS'),
('SEC-A26', 'Section A 2026', 'B2026A', 'CARS');

-- ============================================================
-- SEED DATA: 5 Dummy Student Records — All Columns Populated
-- profile_picture is intentionally an empty BLOB (X'') as a placeholder;
-- replace with a real image upload once the registrar adds one.
-- Image assets for the SRMS live at:
--   src/main/resources/static/images/
-- ============================================================
INSERT INTO student_records (
    student_id, last_name, first_name, middle_name,
    birthdate, age, sex, civil_status,
    permanent_address, temporary_address, email, contact_no, religion,
    baptized, baptism_date, baptism_place,
    sibling_count, brother_count, sister_count,
    batch_code, course_code, section_code,
    profile_picture, enrollment_date, student_status
) VALUES
('STU-2024-001', 'Reyes',     'Anna',     'Cruz',
    '2003-04-12', 22, 'Female', 'Single',
    '123 Mabini St, Quezon City',  '45 Aurora Blvd, Manila',  'anna.reyes@example.com',     '09171234001', 'Roman Catholic',
    1, '2003-06-20', 'San Pedro Parish, Manila',
    2, 1, 1,
    'B2024A', 'CARS', 'SEC-A24',
    X'', '2024-06-03', 'Active'),

('STU-2024-002', 'Santos',    'Bea',      'Lim',
    '2002-09-30', 23, 'Female', 'Single',
    '88 Roxas Ave, Pasig',         '12 EDSA, Mandaluyong',    'bea.santos@example.com',     '09171234002', 'Iglesia ni Cristo',
    1, '2003-01-15', 'INC Central Temple, Quezon City',
    3, 2, 1,
    'B2024A', 'CARS', 'SEC-A24',
    X'', '2024-06-03', 'Active'),

('STU-2025-001', 'Cruz',      'Carla',    'Mendoza',
    '2004-01-18', 22, 'Female', 'Single',
    '7 Bonifacio St, Makati',      '7 Bonifacio St, Makati',  'carla.cruz@example.com',     '09171234003', 'Christian',
    1, '2004-05-10', 'Christ Fellowship Church, Makati',
    1, 0, 1,
    'B2025A', 'CARS', 'SEC-A25',
    X'', '2025-06-02', 'Active'),

('STU-2025-002', 'Garcia',    'Diana',    'Reyes',
    '2003-12-05', 22, 'Female', 'Single',
    '256 Espana Blvd, Manila',     '256 Espana Blvd, Manila', 'diana.garcia@example.com',   '09171234004', 'Roman Catholic',
    1, '2004-02-28', 'Sto. Domingo Church, Manila',
    4, 2, 2,
    'B2025A', 'CARS', 'SEC-A25',
    X'', '2025-06-02', 'Active'),

('STU-2026-001', 'Lopez',     'Elise',    'Tan',
    '2005-07-22', 20, 'Female', 'Single',
    '19 Katipunan Ave, Quezon City','19 Katipunan Ave, Quezon City','elise.lopez@example.com', '09171234005', 'Roman Catholic',
    1, '2005-10-14', 'Mary Immaculate Parish, Quezon City',
    2, 0, 2,
    'B2026A', 'CARS', 'SEC-A26',
    X'', '2026-06-01', 'Active');
