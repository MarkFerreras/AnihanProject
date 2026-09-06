-- ============================================================
-- Migration: 2026-08-29 — Drop subjects.trainer_id
-- Purpose:
--   Trainer assignment is a class-level concept only (classes.trainer_id).
--   The per-subject "default trainer" column (subjects.trainer_id, added by
--   2026-05-09-classes-and-trainers.sql) is removed: a subject has many
--   classes, each with its own trainer, so a subject legitimately has many
--   trainers and cannot be modelled by a single trainer_id. The Subjects
--   page now derives a read-only "Trainer(s)" list from the subject's
--   classes instead. See memory-bank/decisions.md (2026-08-29).
--
--   Idempotent — guarded via information_schema. Safe to re-run, and safe on
--   a database that never had the column (e.g. one rebuilt from an older
--   snapshot — which is exactly the drift that caused bugs.md Bug 8).
-- ============================================================

USE AnihanSRMS;

-- 1. Drop the foreign key first (its name may vary: fk_subjects_trainer from a
--    migrated DB, or an auto-generated name from a schema.sql build).
SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = 'AnihanSRMS'
      AND TABLE_NAME   = 'subjects'
      AND COLUMN_NAME  = 'trainer_id'
      AND REFERENCED_TABLE_NAME = 'users'
    LIMIT 1
);
SET @sql = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE subjects DROP FOREIGN KEY ', @fk_name),
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Drop the column (this also drops the index MySQL created for the FK).
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'AnihanSRMS'
      AND TABLE_NAME   = 'subjects'
      AND COLUMN_NAME  = 'trainer_id'
);
SET @sql = IF(@col_exists = 1,
    'ALTER TABLE subjects DROP COLUMN trainer_id',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verification
SELECT COUNT(*) AS trainer_id_column_should_be_zero
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'AnihanSRMS'
  AND TABLE_NAME   = 'subjects'
  AND COLUMN_NAME  = 'trainer_id';
