-- ============================================================
-- schema.sql — Clean Schema + Dummy Seed Accounts
-- Generated: 2026-04-26
-- Purpose: Set up a fresh AnihanSRMS database with 3 test
--          accounts (admin, registrar, trainer).
--          No system log data is included.
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
-- ============================================================
CREATE TABLE IF NOT EXISTS student_records (
    record_id INT NOT NULL AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL,
    age INT NOT NULL,
    sex VARCHAR(10) NOT NULL,
    permanent_address VARCHAR(255) NOT NULL,
    temporary_address VARCHAR(255) NULL,
    email VARCHAR(255) NOT NULL,
    contact_no VARCHAR(255) NOT NULL,
    religion VARCHAR(255) NOT NULL,
    baptized TINYINT(1) NOT NULL DEFAULT 0,
    baptism_date DATE NULL,
    baptism_place VARCHAR(255) NOT NULL,
    sibling_count INT NOT NULL,
    brother_count INT NULL,
    sister_count INT NULL,
    batch_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    section_code VARCHAR(20) NOT NULL,
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
    family_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL,
    occupation VARCHAR(255) NOT NULL,
    est_income DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    contact_no VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TABLE: other_guardians
-- ============================================================
CREATE TABLE IF NOT EXISTS other_guardians (
    guardian_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    relation VARCHAR(20) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL,
    address VARCHAR(255) NOT NULL,
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
