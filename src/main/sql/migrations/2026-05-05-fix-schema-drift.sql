-- ============================================================
-- Migration: 2026-05-05-fix-schema-drift.sql
-- Purpose:   Bring an existing AnihanSRMS database in line with
--            the canonical schema.sql.
--
-- Background
-- ----------
-- The April 26 schema sync used CREATE TABLE IF NOT EXISTS, so
-- live DBs that already had older student_records / parents /
-- other_guardians tables silently kept the old column definitions:
--   * student_records.civil_status was missing entirely
--   * many columns were NOT NULL where the JPA entity expects NULL
--     (blocks the multi-step student portal enrollment flow)
--
-- Run this ONCE on any existing AnihanSRMS database to align it
-- with src/main/sql/schema.sql. Fresh installs from schema.sql do
-- not need this file.
--
-- How to run:
--   docker exec -i mysql-server mysql -u root -pmy_password \
--     < src/main/sql/migrations/2026-05-05-fix-schema-drift.sql
-- ============================================================

USE AnihanSRMS;

-- ------------------------------------------------------------
-- student_records
-- ------------------------------------------------------------

-- Add civil_status if absent (idempotent guard via information_schema)
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'AnihanSRMS'
      AND table_name   = 'student_records'
      AND column_name  = 'civil_status'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE student_records ADD COLUMN civil_status VARCHAR(50) NULL AFTER sex',
    'SELECT "civil_status already present" AS note');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Relax NOT NULL constraints to match JPA entity (StudentRecord.java)
ALTER TABLE student_records
    MODIFY COLUMN birthdate         DATE         NULL,
    MODIFY COLUMN age               INT          NULL,
    MODIFY COLUMN sex               VARCHAR(10)  NULL,
    MODIFY COLUMN permanent_address VARCHAR(255) NULL,
    MODIFY COLUMN email             VARCHAR(255) NULL,
    MODIFY COLUMN contact_no        VARCHAR(255) NULL,
    MODIFY COLUMN religion          VARCHAR(255) NULL,
    MODIFY COLUMN baptism_place     VARCHAR(255) NULL,
    MODIFY COLUMN sibling_count     INT          NULL,
    MODIFY COLUMN batch_code        VARCHAR(20)  NULL,
    MODIFY COLUMN course_code       VARCHAR(20)  NULL,
    MODIFY COLUMN section_code      VARCHAR(20)  NULL;

-- ------------------------------------------------------------
-- parents
-- ------------------------------------------------------------
ALTER TABLE parents
    MODIFY COLUMN family_name VARCHAR(255)   NULL,
    MODIFY COLUMN first_name  VARCHAR(255)   NULL,
    MODIFY COLUMN middle_name VARCHAR(255)   NULL,
    MODIFY COLUMN birthdate   DATE           NULL,
    MODIFY COLUMN occupation  VARCHAR(255)   NULL,
    MODIFY COLUMN est_income  DECIMAL(15, 2) NULL,
    MODIFY COLUMN contact_no  VARCHAR(20)    NULL,
    MODIFY COLUMN email       VARCHAR(255)   NULL,
    MODIFY COLUMN address     VARCHAR(255)   NULL;

ALTER TABLE parents ALTER COLUMN est_income DROP DEFAULT;

-- ------------------------------------------------------------
-- other_guardians
-- ------------------------------------------------------------
ALTER TABLE other_guardians
    MODIFY COLUMN relation    VARCHAR(20)  NULL,
    MODIFY COLUMN last_name   VARCHAR(255) NULL,
    MODIFY COLUMN first_name  VARCHAR(255) NULL,
    MODIFY COLUMN middle_name VARCHAR(255) NULL,
    MODIFY COLUMN birthdate   DATE         NULL,
    MODIFY COLUMN address     VARCHAR(255) NULL;
