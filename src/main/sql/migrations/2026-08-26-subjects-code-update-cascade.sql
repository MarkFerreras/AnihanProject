-- ============================================================
-- Migration: 2026-08-26 PM — Cascade subject_code renames into classes/grades
-- ============================================================
-- Purpose: Allow the registrar to rename a subject's code via Edit Subject
--   even after classes/grades already reference it. `classes.subject_code`
--   and `grades.subject_code` were both created with MySQL's default FK
--   behavior (ON UPDATE RESTRICT), which silently rejects any rename to
--   `subjects.subject_code` the moment real classes/grades exist for it.
--   This adds ON UPDATE CASCADE to both so a rename in `subjects` ripples
--   automatically into every child row that references it.
--
--   ON DELETE stays RESTRICT (unchanged) — deleting a subject that still
--   has classes/grades is still blocked, enforced both by the DB and by
--   ClassManagementService.deleteSubject's pre-checks, same as before.
--
-- Idempotent: checks the *current* ON UPDATE rule (not just whether an FK
--   exists) before dropping/recreating, so re-running this after it has
--   already applied is a no-op.
--
-- Note: MySQL cannot ALTER a foreign key's ON UPDATE/ON DELETE rule in
--   place — the constraint must be dropped and re-added. The existing
--   constraint name varies (auto-named `grades_ibfk_N`/`classes_ibfk_N` on
--   databases built straight from an older schema.sql, vs whatever name
--   the app used if added another way), so it's looked up dynamically via
--   information_schema rather than assumed.
-- ============================================================

USE AnihanSRMS;

-- ------------------------------------------------------------
-- 1. grades.subject_code -> subjects.subject_code
-- ------------------------------------------------------------
SET @grades_fk_needs_cascade = (
    SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND REFERENCED_TABLE_NAME = 'subjects' AND UPDATE_RULE <> 'CASCADE'
);
SET @grades_fk_name = (
    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'grades'
      AND COLUMN_NAME = 'subject_code' AND REFERENCED_TABLE_NAME = 'subjects'
    LIMIT 1
);
SET @sql = IF(@grades_fk_needs_cascade > 0,
    CONCAT('ALTER TABLE grades DROP FOREIGN KEY ', @grades_fk_name),
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@grades_fk_needs_cascade > 0,
    'ALTER TABLE grades ADD CONSTRAINT fk_grades_subject FOREIGN KEY (subject_code) REFERENCES subjects(subject_code) ON DELETE RESTRICT ON UPDATE CASCADE',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. classes.subject_code -> subjects.subject_code
-- ------------------------------------------------------------
SET @classes_fk_needs_cascade = (
    SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'classes'
      AND REFERENCED_TABLE_NAME = 'subjects' AND UPDATE_RULE <> 'CASCADE'
);
SET @classes_fk_name = (
    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'classes'
      AND COLUMN_NAME = 'subject_code' AND REFERENCED_TABLE_NAME = 'subjects'
    LIMIT 1
);
SET @sql = IF(@classes_fk_needs_cascade > 0,
    CONCAT('ALTER TABLE classes DROP FOREIGN KEY ', @classes_fk_name),
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@classes_fk_needs_cascade > 0,
    'ALTER TABLE classes ADD CONSTRAINT fk_classes_subject FOREIGN KEY (subject_code) REFERENCES subjects(subject_code) ON DELETE RESTRICT ON UPDATE CASCADE',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION — inspect the resulting FK rules.
-- ============================================================
SELECT 'grades -> subjects FK:' AS status;
SELECT CONSTRAINT_NAME, UPDATE_RULE, DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'grades' AND REFERENCED_TABLE_NAME = 'subjects';

SELECT 'classes -> subjects FK:' AS status;
SELECT CONSTRAINT_NAME, UPDATE_RULE, DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'classes' AND REFERENCED_TABLE_NAME = 'subjects';
