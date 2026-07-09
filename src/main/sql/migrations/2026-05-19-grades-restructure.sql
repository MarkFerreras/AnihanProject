-- ============================================================
-- Migration: 2026-05-19 — Grades Restructure for Trainer Input
-- ============================================================
-- Purpose: Add class-scoped grading to the `grades` table:
--            * class_id  — links a grade to a specific class
--            * midterm_grade / finals_grade — component grade split
--            * locked / locked_at — grade locking by the trainer
--          and relax the legacy NOT NULL columns (final_grade,
--          hours_studied, remarks) so a grade row can exist before
--          every component has been entered.
--
-- Prerequisite: the `classes` table must already exist, because
--   grades.class_id takes a foreign key to classes(class_id).
--   Run src/main/sql/migrations/2026-05-09-classes-and-trainers.sql
--   first if this is a legacy database. This migration aborts with a
--   clear error rather than half-applying if `classes` is missing.
--
-- Idempotent: every statement is guarded against information_schema,
--   so this file is safe to re-run against an already-migrated
--   database. Fresh installs get the final shape straight from
--   src/main/sql/schema.sql and do not need to run this at all.
--
-- Note: MySQL 8 does not support `ALTER TABLE ... ADD COLUMN IF NOT
--   EXISTS`, so each change is wrapped in a prepared statement that
--   collapses to a no-op `DO 1` when the change is already present.
-- ============================================================

USE AnihanSRMS;

-- ------------------------------------------------------------
-- 0. Precondition: `classes` must exist (FK target below).
-- ------------------------------------------------------------
SET @classes_exists = (
    SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'classes'
);
-- Fail loudly rather than half-applying if the FK target is absent.
-- SIGNAL is not allowed in the prepared-statement protocol, so instead we
-- reference a table that does not exist and whose *name* is the instruction.
-- (MySQL identifiers cap at 64 characters, hence the terse name.) The script
-- aborts on statement 1 with:
--   ERROR 1146: Table 'ABORT_classes_missing_run_2026_05_09_migration_first'
--               doesn't exist
SET @sql = IF(@classes_exists = 1,
    'DO 1',
    'SELECT 1 FROM ABORT_classes_missing_run_2026_05_09_migration_first');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 1. Add class_id
-- ------------------------------------------------------------
SET @has_class_id = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND COLUMN_NAME = 'class_id'
);
SET @sql = IF(@has_class_id = 0,
    'ALTER TABLE grades ADD COLUMN class_id INT NULL AFTER subject_code',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. Add component grade columns (midterm / finals)
-- ------------------------------------------------------------
SET @has_midterm = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND COLUMN_NAME = 'midterm_grade'
);
SET @sql = IF(@has_midterm = 0,
    'ALTER TABLE grades ADD COLUMN midterm_grade DECIMAL(5,2) NULL',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_finals = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND COLUMN_NAME = 'finals_grade'
);
SET @sql = IF(@has_finals = 0,
    'ALTER TABLE grades ADD COLUMN finals_grade DECIMAL(5,2) NULL',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3. Add lock fields
-- ------------------------------------------------------------
SET @has_locked = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND COLUMN_NAME = 'locked'
);
SET @sql = IF(@has_locked = 0,
    'ALTER TABLE grades ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_locked_at = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND COLUMN_NAME = 'locked_at'
);
SET @sql = IF(@has_locked_at = 0,
    'ALTER TABLE grades ADD COLUMN locked_at DATETIME NULL',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 4. Relax legacy NOT NULL columns.
--    MODIFY COLUMN is naturally idempotent — re-running is a no-op.
--    Widths follow schema.sql (hours_studied is DECIMAL(5,2), not (3,2)).
-- ------------------------------------------------------------
ALTER TABLE grades MODIFY COLUMN final_grade   DECIMAL(5,2) NULL;
ALTER TABLE grades MODIFY COLUMN hours_studied DECIMAL(5,2) NULL;
ALTER TABLE grades MODIFY COLUMN remarks       VARCHAR(255) NULL;

-- ------------------------------------------------------------
-- 5. Foreign key: grades.class_id -> classes.class_id
--    ON DELETE SET NULL so deleting a class preserves the grade history.
--
--    Detect the FK by what it constrains (class_id -> classes), NOT by
--    name. Databases created from schema.sql get an auto-named FK
--    (grades_ibfk_3) for the same relationship; matching on the name
--    'fk_grades_class' alone would add a duplicate FK on every re-run.
-- ------------------------------------------------------------
SET @has_class_fk = (
    SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND COLUMN_NAME = 'class_id'
      AND REFERENCED_TABLE_NAME = 'classes'
);
SET @sql = IF(@has_class_fk = 0,
    'ALTER TABLE grades ADD CONSTRAINT fk_grades_class FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE SET NULL',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 6. Unique constraint: one grade row per student per class.
-- ------------------------------------------------------------
SET @has_uq = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND INDEX_NAME = 'uq_grade_student_class'
);
SET @sql = IF(@has_uq = 0,
    'ALTER TABLE grades ADD UNIQUE KEY uq_grade_student_class (class_id, student_id)',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION — inspect the resulting shape.
-- ============================================================
SELECT 'grades table columns:' AS status;
DESCRIBE grades;

SELECT 'Foreign keys on grades table:' AS status;
SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'grades';

SELECT 'Indexes on grades table:' AS status;
SHOW INDEXES FROM grades;
