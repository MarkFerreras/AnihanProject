-- ============================================================
-- Migration: 2026-10-03 — R4.2 SO Checklist columns
-- ============================================================
-- completion_date: the day the student finished training and OJT. Set when
-- the Registrar changes the status to Completed (or, for a digitized archive
-- record, Active -> Graduated). NULL until then.
-- employment_status: Employed / Self-employed / Unemployed / Further studies,
-- or NULL ("Not set"). One of TESDA's SO requirements.
-- student_status VARCHAR(25) already fits the new 'Completed' value, so it is
-- not touched. Existing rows are left NULL (no backfill).
--
-- See docs/superpowers/specs/2026-10-01-so-checklist-design.md §3.
--
-- Idempotent: safe to re-run. Each guard matches on COLUMN_NAME.
-- ============================================================

USE AnihanSRMS;

SET @has_completion_date = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'student_records'
      AND COLUMN_NAME = 'completion_date'
);
SET @sql = IF(@has_completion_date = 0,
    'ALTER TABLE student_records ADD COLUMN completion_date DATE NULL AFTER enrollment_date',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_employment_status = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'student_records'
      AND COLUMN_NAME = 'employment_status'
);
SET @sql = IF(@has_employment_status = 0,
    'ALTER TABLE student_records ADD COLUMN employment_status VARCHAR(25) NULL AFTER student_status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION (read-only — safe to run any time)
-- ============================================================

-- Expect: enrollment_date, completion_date, student_status, employment_status
-- in that order; both new columns IS_NULLABLE = YES.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, ORDINAL_POSITION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'student_records'
  AND COLUMN_NAME IN ('enrollment_date', 'completion_date', 'student_status', 'employment_status')
ORDER BY ORDINAL_POSITION;

-- Expect: with_completion_date = 0 and with_employment_status = 0 on first run
SELECT COUNT(*) AS total_students,
       SUM(completion_date IS NOT NULL) AS with_completion_date,
       SUM(employment_status IS NOT NULL) AS with_employment_status
FROM student_records;
