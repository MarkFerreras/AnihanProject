-- ============================================================
-- Migration: 2026-08-27 — Remove subjects.trainer_id
-- ============================================================
-- Purpose: Trainer assignment is now represented only by
--   classes.trainer_id, which is already the sole field used for every
--   real authorization/roster check in the codebase. subjects.trainer_id
--   was a single-value "default trainer" convenience (pre-filled the
--   Create Class modal, shown on the Subjects table) that was never
--   enforced and implied "one trainer per subject" — contradicting the
--   actual, already-working class-level multi-trainer capability.
--   See memory-bank/decisions.md (2026-08-27) and
--   capstonepaper/2026-08-27-trainer-subject-assignment-design-discussion.md
--   for the full rationale.
--
-- Idempotent: checks whether the FK/column still exist before dropping
--   them, so re-running this after it has already applied is a no-op.
-- ============================================================

USE AnihanSRMS;

-- ------------------------------------------------------------
-- 1. Drop the FK (subjects.trainer_id -> users.user_id) if present.
--    Constraint name varies (auto-named on databases built straight
--    from an older schema.sql), so it's looked up dynamically.
-- ------------------------------------------------------------
SET @fk_name = (
    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subjects'
      AND COLUMN_NAME = 'trainer_id' AND REFERENCED_TABLE_NAME = 'users'
    LIMIT 1
);
SET @sql = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE subjects DROP FOREIGN KEY ', @fk_name),
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. Drop the column itself, if present.
-- ------------------------------------------------------------
SET @has_trainer_id = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subjects'
      AND COLUMN_NAME = 'trainer_id'
);
SET @sql = IF(@has_trainer_id > 0,
    'ALTER TABLE subjects DROP COLUMN trainer_id',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION
-- ============================================================
SELECT 'subjects table columns (trainer_id should be gone):' AS status;
DESCRIBE subjects;
