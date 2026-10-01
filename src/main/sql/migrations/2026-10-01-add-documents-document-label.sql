-- ============================================================
-- Migration: 2026-10-01 — Add documents.document_label
-- ============================================================
-- Optional custom name for an "Others" document (e.g. "Medical
-- Certificate"), typed by the Registrar on upload. document_type stays
-- 'Others', so the strict type whitelist and the "Others" filter are
-- unchanged. NULL = a plain, unnamed "Others" document; existing rows are
-- left NULL (no backfill). The application only ever writes a label when
-- document_type = 'Others' (enforced in DocumentService.uploadBatch).
--
-- See docs/superpowers/specs/2026-10-01-pre-export-missing-documents-design.md §5.
--
-- Idempotent: safe to re-run. The guard matches on COLUMN_NAME.
-- ============================================================

USE AnihanSRMS;

SET @has_document_label = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'documents'
      AND COLUMN_NAME = 'document_label'
);
SET @sql = IF(@has_document_label = 0,
    'ALTER TABLE documents ADD COLUMN document_label VARCHAR(100) NULL AFTER document_type',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION (read-only — safe to run any time)
-- ============================================================

-- Expect: document_label VARCHAR(100), IS_NULLABLE = YES, right after document_type
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, ORDINAL_POSITION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'documents'
  AND COLUMN_NAME IN ('document_type', 'document_label')
ORDER BY ORDINAL_POSITION;

-- Expect: labelled = 0 on first run
SELECT COUNT(*) AS total_documents,
       SUM(document_label IS NOT NULL) AS labelled
FROM documents;
