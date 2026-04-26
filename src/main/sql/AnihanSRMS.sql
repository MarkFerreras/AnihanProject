-- ============================================================
-- AnihanSRMS.sql — Full Database Dump
-- Generated: 2026-04-26
-- Purpose: Clone the current AnihanSRMS database to a new device
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

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- DATA: users (3 accounts)
-- Password for all accounts: password123
-- ============================================================
INSERT INTO users (user_id, username, password, lastname, firstname, middlename, birthdate, age, email, role, enabled, password_changed_at) VALUES
(10, 'admin',     '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Ferreas',    'Mark',  'Pretopia', '2004-01-01', 21, 'mark@example.com',    'ROLE_ADMIN',     1, NULL),
(11, 'registrar', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Registrar',  'Reg',   'B',        '1995-05-05', 29, 'reg@example.com',     'ROLE_REGISTRAR', 1, NULL),
(12, 'trainer',   '$2a$10$IM6yEO9HpxmNE.cm2k1SUOGBkIDJwfjIwBg..ia8JgZIHULz.STHK', 'Trainer',    'Train', 'C',        '1985-10-10', 39, 'trainer@example.com', 'ROLE_TRAINER',   1, '2026-04-16 12:07:31');

-- ============================================================
-- DATA: system_logs (79 entries)
-- ============================================================
INSERT INTO system_logs (log_id, user_id, username, role, action, ip_address, timestamp) VALUES
(1,  10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-14 16:05:37'),
(2,  10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-14 16:06:04'),
(3,  10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-14 16:06:18'),
(4,  10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-14 16:24:49'),
(5,  10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-14 16:25:09'),
(6,  10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-14 16:26:02'),
(7,  11, 'registrar', 'ROLE_REGISTRAR', 'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-14 16:26:09'),
(8,  11, 'registrar', 'ROLE_REGISTRAR', 'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-14 16:26:12'),
(9,  12, 'trainer',   'ROLE_TRAINER',   'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-14 16:26:23'),
(10, 12, 'trainer',   'ROLE_TRAINER',   'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-14 16:26:26'),
(11, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-14 16:26:31'),
(12, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-14 16:33:43'),
(13, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-14 17:01:36'),
(14, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 09:17:37'),
(15, 10, 'admin',     'ROLE_ADMIN',     'Created new account: Emmanuel (ROLE_ADMIN)',         '0:0:0:0:0:0:0:1', '2026-04-16 09:19:13'),
(16, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 09:19:25'),
(17, 13, 'Emmanuel',  'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 09:19:33'),
(18, 13, 'Emmanuel',  'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 09:19:39'),
(19, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 09:19:45'),
(20, 10, 'admin',     'ROLE_ADMIN',     'Permanently deleted account: Emmanuel',              '0:0:0:0:0:0:0:1', '2026-04-16 09:19:56'),
(21, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 09:20:05'),
(22, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 09:20:22'),
(23, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 11:40:08'),
(24, 12, 'trainer',   'ROLE_TRAINER',   'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:00:14'),
(25, 12, 'Prainer',   'ROLE_TRAINER',   'Changed own username from trainer to Prainer',       '0:0:0:0:0:0:0:1', '2026-04-16 12:04:52'),
(26, 12, 'Prainer',   'ROLE_TRAINER',   'Changed own password',                               '0:0:0:0:0:0:0:1', '2026-04-16 12:05:47'),
(27, 12, 'Prainer',   'ROLE_TRAINER',   'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:06:07'),
(28, 12, 'trainer',   'ROLE_TRAINER',   'Changed own username from Prainer to trainer',       '0:0:0:0:0:0:0:1', '2026-04-16 12:06:22'),
(29, 12, 'trainer',   'ROLE_TRAINER',   'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:06:26'),
(30, 11, 'registrar', 'ROLE_REGISTRAR', 'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:06:34'),
(31, 11, 'registrar', 'ROLE_REGISTRAR', 'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:06:50'),
(32, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:06:57'),
(33, 10, 'admin',     'ROLE_ADMIN',     'Updated user details for: trainer',                  '0:0:0:0:0:0:0:1', '2026-04-16 12:07:31'),
(34, 10, 'admin',     'ROLE_ADMIN',     'Reset password for: trainer',                        '0:0:0:0:0:0:0:1', '2026-04-16 12:07:31'),
(35, 10, 'admin',     'ROLE_ADMIN',     'Deactivated account: trainer',                       '0:0:0:0:0:0:0:1', '2026-04-16 12:09:13'),
(36, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:09:20'),
(37, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:09:35'),
(38, 10, 'admin',     'ROLE_ADMIN',     'Re-enabled account: trainer',                        '0:0:0:0:0:0:0:1', '2026-04-16 12:11:01'),
(39, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:11:05'),
(40, 12, 'trainer',   'ROLE_TRAINER',   'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:11:15'),
(41, 12, 'trainer',   'ROLE_TRAINER',   'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:11:18'),
(42, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:11:25'),
(43, 10, 'admin',     'ROLE_ADMIN',     'Created new account: Sean (ROLE_ADMIN)',             '0:0:0:0:0:0:0:1', '2026-04-16 12:23:13'),
(44, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:23:23'),
(45, 14, 'Sean',      'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:23:30'),
(46, 14, 'Sean',      'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:54:38'),
(47, 12, 'trainer',   'ROLE_TRAINER',   'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:54:46'),
(48, 12, 'trainer',   'ROLE_TRAINER',   'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:54:59'),
(49, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:55:06'),
(50, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:55:32'),
(51, 14, 'Sean',      'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:55:38'),
(52, 14, 'Sean',      'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 12:57:52'),
(53, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 12:58:00'),
(54, 10, 'admin',     'ROLE_ADMIN',     'Permanently deleted account: Sean',                  '0:0:0:0:0:0:0:1', '2026-04-16 12:58:11'),
(55, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:01:42'),
(56, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 23:03:28'),
(57, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:03:55'),
(58, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 23:04:22'),
(59, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:17:31'),
(60, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 23:17:37'),
(61, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:17:48'),
(62, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 23:19:09'),
(63, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:19:18'),
(64, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 23:20:08'),
(65, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:29:11'),
(66, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:34:14'),
(67, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:34:59'),
(68, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 23:35:21'),
(69, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-16 23:40:31'),
(70, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-16 23:40:55'),
(71, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-18 13:15:58'),
(72, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-18 13:25:37'),
(73, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-18 17:33:55'),
(74, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-18 17:36:39'),
(75, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-18 17:38:23'),
(76, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-18 17:44:41'),
(77, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-18 17:45:35'),
(78, 10, 'admin',     'ROLE_ADMIN',     'User logged in',                                    '0:0:0:0:0:0:0:1', '2026-04-18 17:53:20'),
(79, 10, 'admin',     'ROLE_ADMIN',     'User logged out',                                   '0:0:0:0:0:0:0:1', '2026-04-18 17:53:31');