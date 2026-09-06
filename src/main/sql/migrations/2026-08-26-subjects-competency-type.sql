-- ============================================================
-- Migration: 2026-08-26 — Subjects: competency_type + nullable qualification
-- ============================================================
-- Purpose: Every subject belongs to one of three TESDA-defined
--   competency types — BASIC, COMMON, or CORE. BASIC and COMMON
--   subjects are shared across all qualifications (they are not tied
--   to one specific NC program); only CORE subjects are
--   qualification-specific. Adds subjects.competency_type and relaxes
--   subjects.qualification_code so Basic/Common subjects can be
--   created without forcing a qualification onto them.
--
--   Existing subjects (the 6 seeded Cookery/BPP subjects) are all
--   CORE subjects, so they are backfilled with competency_type =
--   'CORE' before the column is tightened to NOT NULL.
--
-- Idempotent: every statement is guarded against information_schema,
--   so this file is safe to re-run against an already-migrated
--   database. Fresh installs get the final shape straight from
--   src/main/sql/schema.sql and do not need to run this at all.
--
-- IMPORTANT — every developer with a local AnihanSRMS database must
--   run this once (or re-run schema.sql on a fresh database) before
--   pulling code that depends on subjects.competency_type.
-- ============================================================

USE AnihanSRMS;

-- ------------------------------------------------------------
-- 1. Add competency_type (nullable at first so existing rows can be
--    backfilled before the NOT NULL constraint is applied).
-- ------------------------------------------------------------
SET @has_competency_type = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subjects'
      AND COLUMN_NAME = 'competency_type'
);
SET @sql = IF(@has_competency_type = 0,
    'ALTER TABLE subjects ADD COLUMN competency_type VARCHAR(15) NULL AFTER qualification_code',
    'DO 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. Backfill existing rows. Every subject seeded before this
--    migration is a qualification-specific CORE subject.
-- ------------------------------------------------------------
UPDATE subjects SET competency_type = 'CORE' WHERE competency_type IS NULL;

-- ------------------------------------------------------------
-- 3. Tighten competency_type to NOT NULL now that every row has a
--    value. MODIFY COLUMN is naturally idempotent — re-running is a
--    no-op once the column is already NOT NULL.
-- ------------------------------------------------------------
ALTER TABLE subjects MODIFY COLUMN competency_type VARCHAR(15) NOT NULL;

-- ------------------------------------------------------------
-- 4. Relax qualification_code to NULL. BASIC/COMMON subjects are not
--    tied to a single qualification; only CORE subjects require one
--    (enforced in ClassManagementService, not at the SQL level).
--    MODIFY COLUMN is naturally idempotent.
-- ------------------------------------------------------------
ALTER TABLE subjects MODIFY COLUMN qualification_code INT NULL;

-- ============================================================
-- VERIFICATION — inspect the resulting shape.
-- ============================================================
SELECT 'subjects table columns:' AS status;
DESCRIBE subjects;

SELECT 'subjects data (competency_type backfilled):' AS status;
SELECT subject_code, subject_name, qualification_code, competency_type FROM subjects;
