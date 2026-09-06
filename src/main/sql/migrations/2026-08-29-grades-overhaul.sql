-- ============================================================
-- Migration: 2026-08-29 — Trainer Grading Overhaul
-- Purpose:
--   Replace the midterm/finals (40/60) model with the TESDA/TOR model:
--     * Trainer enters a raw percentage (final_percentage), OR a status
--       code (grade_status: C / FA / INC / D) instead of a percentage.
--     * System transmutes the percentage to a 1.00-5.00 equivalent, stored
--       in final_grade (and re_exam_grade for the optional re-exam).
--     * remarks now holds a derived token: COMPETENT / NOT_COMPETENT / NULL.
--     * hours_studied -> hours_rendered (attendance hours, TESDA requirement,
--       separate from the fixed curriculum hours).
--   See memory-bank/decisions.md (2026-08-29 - Trainer Grading Overhaul).
--
--   Idempotent — guarded via information_schema. The live grades table has
--   0 rows, so this is a structural change only (no data migration).
-- ============================================================

USE AnihanSRMS;

-- 1. Drop the old component-grade columns (midterm / finals) if present.
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'midterm_grade') = 1,
    'ALTER TABLE grades DROP COLUMN midterm_grade', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'finals_grade') = 1,
    'ALTER TABLE grades DROP COLUMN finals_grade', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Add final_percentage (raw % entered by the trainer; NULL when a status code is used).
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'final_percentage') = 0,
    'ALTER TABLE grades ADD COLUMN final_percentage DECIMAL(5,2) NULL AFTER class_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Add re_exam_percentage (raw %; only meaningful when the final is a failing mark).
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 're_exam_percentage') = 0,
    'ALTER TABLE grades ADD COLUMN re_exam_percentage DECIMAL(5,2) NULL AFTER final_percentage', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Add grade_status (C / FA / INC / D) — used INSTEAD of a percentage.
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'grade_status') = 0,
    'ALTER TABLE grades ADD COLUMN grade_status VARCHAR(5) NULL AFTER re_exam_grade', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. Rename hours_studied -> hours_rendered (keeps DECIMAL(5,2) NULL).
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'hours_studied') = 1
    AND (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'hours_rendered') = 0,
    'ALTER TABLE grades CHANGE COLUMN hours_studied hours_rendered DECIMAL(5,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fallback: if neither column exists yet (e.g. a very old grades table), add hours_rendered.
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades' AND COLUMN_NAME = 'hours_rendered') = 0,
    'ALTER TABLE grades ADD COLUMN hours_rendered DECIMAL(5,2) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Verification
SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION) AS grades_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'AnihanSRMS' AND TABLE_NAME = 'grades';
