-- ============================================================
-- Migration: 2026-08-27 — Add registrar-controlled student_number
-- ============================================================
-- Adds `student_records.student_number`: the REAL student number, owned by
-- the Registrar (and, later, by the bulk import of the paper archive).
--
-- Why a new column instead of reusing student_id:
--   `student_id` is NOT NULL UNIQUE and is the foreign-key target of TEN
--   child tables (parents, other_guardians, documents, grades,
--   student_education, student_school_years, student_ojt,
--   student_tesda_qualifications, student_uploads, class_enrollments).
--   Making it nullable would orphan every child row written before a number
--   is assigned — including the ID-photo upload in wizard step 2. So
--   `student_id` stays exactly as it is, demoted to an internal system
--   reference, and the registrar-controlled number lives in a new column.
--
-- `student_number` is nullable ON PURPOSE: a student may exist without one
-- until the Registrar assigns it or the import supplies it. MySQL permits
-- multiple NULLs in a UNIQUE index, which gives us "unique when present".
-- Nothing auto-generates this value.
--
-- Existing rows are left NULL — every current student correctly starts out
-- as "missing a student number".
--
-- Idempotent: safe to re-run. Guards match on COLUMN_NAME rather than on a
-- constraint name (a name-based guard caused a real duplicate-FK bug in the
-- 2026-05-19 migration — see memory-bank/changeLog.md).
-- ============================================================

USE AnihanSRMS;

-- 1. Add the column ----------------------------------------------------------
SET @has_student_number = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'student_records'
      AND COLUMN_NAME = 'student_number'
);
SET @sql = IF(@has_student_number = 0,
    'ALTER TABLE student_records ADD COLUMN student_number VARCHAR(20) NULL AFTER student_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Add the UNIQUE index ----------------------------------------------------
-- Matched by COLUMN_NAME + NON_UNIQUE, not by index name, so an index created
-- under a different name (e.g. by schema.sql) is not duplicated on re-run.
SET @has_unique_idx = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'student_records'
      AND COLUMN_NAME = 'student_number'
      AND NON_UNIQUE = 0
);
SET @sql = IF(@has_unique_idx = 0,
    'ALTER TABLE student_records ADD UNIQUE KEY uq_student_number (student_number)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION (read-only — safe to run any time)
-- ============================================================

-- Expect: student_number VARCHAR(20), IS_NULLABLE = YES, positioned after student_id
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, ORDINAL_POSITION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'student_records'
  AND COLUMN_NAME IN ('student_id', 'student_number')
ORDER BY ORDINAL_POSITION;

-- Expect: exactly ONE row (uq_student_number, NON_UNIQUE = 0)
SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'student_records'
  AND COLUMN_NAME = 'student_number';

-- Expect: total = students still missing a student number (all of them, on first run)
SELECT COUNT(*) AS total_students,
       SUM(student_number IS NULL) AS missing_student_number
FROM student_records;
