-- ============================================================
-- Migration: 2026-05-20 — Sync Live DB to schema.sql + Clear Student Data
-- ============================================================
-- This migration was written against a live AnihanSRMS snapshot that
-- predated the May 2026 migrations. It performs two phases:
--
--   PHASE A — Structural sync to src/main/sql/schema.sql:
--     * Drop the empty legacy tables: log, previous_school,
--       qualification_assessment (all 0 rows; log is superseded by
--       system_logs, previous_school by student_education).
--     * Rename the misspelled `classess` table to `classes` and bring
--       its columns in line with schema.sql.
--     * Create class_enrollments.
--     * Add subjects.trainer_id + FK.
--     * Restructure grades for class-scoped midterm/finals grading.
--
--   PHASE B — Clear all student records:
--     * Delete every row from student_records and the tables that
--       reference it, plus the classes table.
--     * NO tables are dropped in Phase B — structures are preserved.
--
-- Phase A operations are idempotent / guarded where practical.
-- ============================================================

USE AnihanSRMS;

-- ============================================================
-- PHASE A — STRUCTURAL SYNC
-- ============================================================

-- A1. Drop empty legacy tables ------------------------------------------------
DROP TABLE IF EXISTS log;
DROP TABLE IF EXISTS previous_school;
DROP TABLE IF EXISTS qualification_assessment;

-- A2. Rename misspelled `classess` -> `classes` and fix its shape -------------
-- classess is empty, so column changes carry no data-migration risk.
SET @classess_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'classess'
);
SET @classes_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'classes'
);
SET @sql = IF(@classess_exists = 1 AND @classes_exists = 0,
    'RENAME TABLE classess TO classes', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Align column set with schema.sql (only if the renamed table is present).
SET @classes_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'classes'
);

-- class_code -> class_id
SET @has_class_code = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'class_code'
);
SET @sql = IF(@classes_exists = 1 AND @has_class_code = 1,
    'ALTER TABLE classes CHANGE COLUMN class_code class_id INT NOT NULL AUTO_INCREMENT',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- trainer_id NOT NULL -> NULL
SET @sql = IF(@classes_exists = 1,
    'ALTER TABLE classes MODIFY COLUMN trainer_id INT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- add semester column
SET @has_semester = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'semester'
);
SET @sql = IF(@classes_exists = 1 AND @has_semester = 0,
    'ALTER TABLE classes ADD COLUMN semester VARCHAR(20) NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- add created_at column
SET @has_created_at = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'created_at'
);
SET @sql = IF(@classes_exists = 1 AND @has_created_at = 0,
    'ALTER TABLE classes ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- add (section_code, subject_code, semester) unique key
SET @has_uq_class = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'classes'
      AND INDEX_NAME = 'uq_class'
);
SET @sql = IF(@classes_exists = 1 AND @has_uq_class = 0,
    'ALTER TABLE classes ADD UNIQUE KEY uq_class (section_code, subject_code, semester)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- A3. Create class_enrollments ------------------------------------------------
CREATE TABLE IF NOT EXISTS class_enrollments (
    enrollment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id      INT NOT NULL,
    student_id    VARCHAR(20) NOT NULL,
    enrolled_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_class_student (class_id, student_id),
    FOREIGN KEY (class_id)   REFERENCES classes(class_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES student_records(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A4. Add subjects.trainer_id + FK -------------------------------------------
SET @has_subj_trainer = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'subjects'
      AND COLUMN_NAME = 'trainer_id'
);
SET @sql = IF(@has_subj_trainer = 0,
    'ALTER TABLE subjects ADD COLUMN trainer_id INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_subj_trainer_fk = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'subjects'
      AND CONSTRAINT_NAME = 'fk_subjects_trainer'
);
SET @sql = IF(@has_subj_trainer_fk = 0,
    'ALTER TABLE subjects ADD CONSTRAINT fk_subjects_trainer FOREIGN KEY (trainer_id) REFERENCES users(user_id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- A5. Restructure grades for class-scoped midterm/finals grading -------------
-- grades is empty, so adding/relaxing columns carries no data-migration risk.
SET @g_has_class_id = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='grades' AND COLUMN_NAME='class_id');
SET @sql = IF(@g_has_class_id = 0,
    'ALTER TABLE grades ADD COLUMN class_id INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @g_has_midterm = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='grades' AND COLUMN_NAME='midterm_grade');
SET @sql = IF(@g_has_midterm = 0,
    'ALTER TABLE grades ADD COLUMN midterm_grade DECIMAL(5,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @g_has_finals = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='grades' AND COLUMN_NAME='finals_grade');
SET @sql = IF(@g_has_finals = 0,
    'ALTER TABLE grades ADD COLUMN finals_grade DECIMAL(5,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @g_has_locked = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='grades' AND COLUMN_NAME='locked');
SET @sql = IF(@g_has_locked = 0,
    'ALTER TABLE grades ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @g_has_locked_at = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='grades' AND COLUMN_NAME='locked_at');
SET @sql = IF(@g_has_locked_at = 0,
    'ALTER TABLE grades ADD COLUMN locked_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Relax legacy NOT NULL columns.
ALTER TABLE grades MODIFY COLUMN final_grade DECIMAL(5,2) NULL;
ALTER TABLE grades MODIFY COLUMN hours_studied DECIMAL(5,2) NULL;
ALTER TABLE grades MODIFY COLUMN remarks VARCHAR(255) NULL;

-- grades.class_id FK -> classes
SET @g_has_class_fk = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='grades'
      AND CONSTRAINT_NAME='fk_grades_class');
SET @sql = IF(@g_has_class_fk = 0,
    'ALTER TABLE grades ADD CONSTRAINT fk_grades_class FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (class_id, student_id) unique key
SET @g_has_uq = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA='AnihanSRMS' AND TABLE_NAME='grades'
      AND INDEX_NAME='uq_grade_student_class');
SET @sql = IF(@g_has_uq = 0,
    'ALTER TABLE grades ADD UNIQUE KEY uq_grade_student_class (class_id, student_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- PHASE B — CLEAR ALL STUDENT RECORDS
-- ============================================================
-- Deletes data only. NO tables are dropped here. Child tables are
-- cleared before student_records to satisfy FK constraints.
-- ============================================================

DELETE FROM class_enrollments;
DELETE FROM grades;
DELETE FROM documents;
DELETE FROM parents;
DELETE FROM other_guardians;
DELETE FROM student_education;
DELETE FROM student_school_years;
DELETE FROM student_ojt;
DELETE FROM student_tesda_qualifications;
DELETE FROM student_uploads;
DELETE FROM student_records;

-- Clear the classes table (no student PII; requested explicitly).
DELETE FROM classes;

-- Reset AUTO_INCREMENT counters so new records start clean.
ALTER TABLE class_enrollments            AUTO_INCREMENT = 1;
ALTER TABLE grades                       AUTO_INCREMENT = 1;
ALTER TABLE documents                    AUTO_INCREMENT = 1;
ALTER TABLE parents                      AUTO_INCREMENT = 1;
ALTER TABLE other_guardians              AUTO_INCREMENT = 1;
ALTER TABLE student_education            AUTO_INCREMENT = 1;
ALTER TABLE student_school_years         AUTO_INCREMENT = 1;
ALTER TABLE student_ojt                  AUTO_INCREMENT = 1;
ALTER TABLE student_tesda_qualifications AUTO_INCREMENT = 1;
ALTER TABLE student_uploads              AUTO_INCREMENT = 1;
ALTER TABLE student_records              AUTO_INCREMENT = 1;
ALTER TABLE classes                      AUTO_INCREMENT = 1;
