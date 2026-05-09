-- ============================================================
-- Migration: 2026-05-09 — Relax student_records.middle_name to NULL
-- Purpose:
--   The 2026-05-05 schema-drift migration relaxed many NOT NULL
--   columns but missed `student_records.middle_name`. The student
--   portal wizard treats middle name as optional; the JPA entity
--   has no @NotNull. A student with no middle name currently fails
--   to save with a SQL NOT NULL constraint violation.
-- Idempotent: safe to re-run.
-- ============================================================

USE AnihanSRMS;

ALTER TABLE student_records MODIFY COLUMN middle_name VARCHAR(255) NULL;
