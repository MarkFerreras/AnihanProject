-- Idempotent in effect: repeated execution preserves the same definition.
USE AnihanSRMS;

ALTER TABLE documents
    MODIFY COLUMN file_type VARCHAR(100) NOT NULL;

SELECT COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'AnihanSRMS'
  AND TABLE_NAME = 'documents'
  AND COLUMN_NAME = 'file_type';
