-- ============================================================
-- Migration: 2026-05-09 — Classes, Trainer Assignment & Seed Data
-- Purpose:
--   1. Add optional trainer_id FK to subjects (default trainer)
--   2. Create classes table (section + subject + trainer per semester)
--   3. Create class_enrollments table (student-to-class assignment)
--   4. Seed qualifications and subjects for development/testing
-- ============================================================

USE AnihanSRMS;

-- -------------------------------------------------------
-- 1. Add trainer_id to subjects (idempotent)
-- -------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'AnihanSRMS'
      AND TABLE_NAME   = 'subjects'
      AND COLUMN_NAME  = 'trainer_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE subjects ADD COLUMN trainer_id INT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add FK if not already present
SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA    = 'AnihanSRMS'
      AND TABLE_NAME      = 'subjects'
      AND CONSTRAINT_NAME = 'fk_subjects_trainer'
);

SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE subjects ADD CONSTRAINT fk_subjects_trainer FOREIGN KEY (trainer_id) REFERENCES users(user_id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------
-- 2. Create classes table
-- -------------------------------------------------------
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

-- -------------------------------------------------------
-- 3. Create class_enrollments table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS class_enrollments (
    enrollment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id      INT NOT NULL,
    student_id    VARCHAR(20) NOT NULL,
    enrolled_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_class_student (class_id, student_id),
    FOREIGN KEY (class_id)   REFERENCES classes(class_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES student_records(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -------------------------------------------------------
-- 4. Seed qualifications (idempotent — skip if already exist)
-- -------------------------------------------------------
INSERT INTO qualifications (qualification_name, qualification_description)
SELECT 'Cookery NC II', 'TESDA National Certificate II in Cookery'
FROM dual WHERE NOT EXISTS (SELECT 1 FROM qualifications WHERE qualification_name = 'Cookery NC II');

INSERT INTO qualifications (qualification_name, qualification_description)
SELECT 'Bread and Pastry Production NC II', 'TESDA NC II in Bread and Pastry Production'
FROM dual WHERE NOT EXISTS (SELECT 1 FROM qualifications WHERE qualification_name = 'Bread and Pastry Production NC II');

-- -------------------------------------------------------
-- 5. Seed subjects (idempotent — skip if already exist)
--    Uses subqueries to resolve qualification_code by name
-- -------------------------------------------------------
INSERT IGNORE INTO subjects (subject_code, subject_name, qualification_code, units) VALUES
('COOK-101', 'Introduction to Cookery',    (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Cookery NC II'), 3),
('COOK-102', 'Food Safety and Sanitation', (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Cookery NC II'), 3),
('COOK-103', 'Prepare Hot Meals',          (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Cookery NC II'), 5),
('COOK-104', 'Prepare Cold Meals',         (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Cookery NC II'), 4),
('BPP-101',  'Bread Making Fundamentals',  (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Bread and Pastry Production NC II'), 3),
('BPP-102',  'Pastry Arts',                (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Bread and Pastry Production NC II'), 4);
