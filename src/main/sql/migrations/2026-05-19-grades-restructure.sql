-- Migration: 2026-05-19 - Grades Restructure for Trainer Input
-- Purpose: Add class-scoped grading, midterm/finals split, locking, GWA support
--
-- NOTE: This migration has already been applied to the AnihanSRMS database.
-- The grades table now includes: class_id, midterm_grade, finals_grade, locked, locked_at
-- Columns final_grade, hours_studied, remarks are now nullable.
--
-- For fresh installations or databases that haven't been updated yet:
-- Uncomment and run the ALTER statements below.

USE AnihanSRMS;

-- Verify current schema
SELECT 'Current grades table columns:' AS status;
DESCRIBE grades;

-- Verify constraints
SELECT 'Foreign keys on grades table:' AS status;
SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS
WHERE TABLE_NAME = 'grades';

-- Verify unique constraints
SELECT 'Indexes on grades table:' AS status;
SHOW INDEXES FROM grades;

-- If you need to apply this migration to a fresh database, uncomment these:
--
-- ALTER TABLE grades ADD COLUMN class_id INT NULL;
-- ALTER TABLE grades ADD COLUMN midterm_grade DECIMAL(5,2) NULL;
-- ALTER TABLE grades ADD COLUMN finals_grade DECIMAL(5,2) NULL;
-- ALTER TABLE grades ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0;
-- ALTER TABLE grades ADD COLUMN locked_at DATETIME NULL;
--
-- ALTER TABLE grades MODIFY COLUMN final_grade DECIMAL(5,2) NULL;
-- ALTER TABLE grades MODIFY COLUMN hours_studied DECIMAL(5,2) NULL;
-- ALTER TABLE grades MODIFY COLUMN remarks VARCHAR(255) NULL;
--
-- ALTER TABLE grades ADD CONSTRAINT fk_grades_class FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE SET NULL;
-- ALTER TABLE grades ADD UNIQUE KEY uq_grade_student_class (class_id, student_id);
