-- ============================================================
-- Migration: 2026-09-19 — Security Questions / Forgot Password
-- ============================================================
-- Adds the "forgot password" security-question feature:
--   * security_questions        — catalog of 6 fixed default questions
--   * user_security_answers     — each user's 2 chosen question+answer pairs
--                                 (one row per slot; either a default
--                                 question_id or a plaintext custom_question,
--                                 never both)
--   * users.security_locked / failed_security_attempts /
--     security_lockout_started_at — lockout state for repeated wrong answers,
--     kept deliberately separate from the existing `enabled` (admin
--     deactivate/re-enable) flag so the two cannot be confused with or
--     silently override one another.
--
-- Answers are hashed (BCrypt, same PasswordEncoder as account passwords).
-- Custom question TEXT is stored in plaintext on purpose — it is not the
-- secret (the answer is), and this project has no safe place to configure
-- an encryption key before delivery. See memory-bank/decisions.md.
--
-- Also fixes a real gap found while designing this feature: users.email had
-- no DB-level uniqueness (app-code-only check in AdminService, race-prone).
-- The 3 existing seed accounts are also given real, memorable
-- @anihan.local addresses (they held @example.com placeholders), since this
-- feature looks accounts up by email.
--
-- Idempotent: safe to re-run. Guards match on COLUMN_NAME / INDEX existence,
-- never on constraint name (a name-based guard caused a real duplicate-FK
-- bug in the 2026-05-19 migration — see memory-bank/changeLog.md).
-- ============================================================

USE AnihanSRMS;

-- 1. security_questions -------------------------------------------------
CREATE TABLE IF NOT EXISTS security_questions (
    question_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_text VARCHAR(255) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed the 6 fixed default questions (wording is fixed — do not alter).
-- WHERE NOT EXISTS guards each row individually since question_text has no
-- unique key to hang an INSERT IGNORE off of.
INSERT INTO security_questions (question_text)
SELECT 'What is your favorite color' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE question_text = 'What is your favorite color');

INSERT INTO security_questions (question_text)
SELECT 'What is your favorite vacation place?' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE question_text = 'What is your favorite vacation place?');

INSERT INTO security_questions (question_text)
SELECT 'What is your favorite song?' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE question_text = 'What is your favorite song?');

INSERT INTO security_questions (question_text)
SELECT 'Who is your favorite artist?' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE question_text = 'Who is your favorite artist?');

INSERT INTO security_questions (question_text)
SELECT 'What is your favorite food?' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE question_text = 'What is your favorite food?');

INSERT INTO security_questions (question_text)
SELECT 'What is the name of your pet?' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE question_text = 'What is the name of your pet?');

-- 2. user_security_answers -----------------------------------------------
CREATE TABLE IF NOT EXISTS user_security_answers (
    answer_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    question_id INT NULL,
    custom_question VARCHAR(255) NULL,
    answer_hash VARCHAR(255) NOT NULL,
    slot INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES security_questions (question_id),
    UNIQUE KEY uq_user_question (user_id, question_id),
    UNIQUE KEY uq_user_slot (user_id, slot),
    CONSTRAINT chk_question_xor_custom
        CHECK ((question_id IS NULL) <> (custom_question IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2b. Correct slot to INT ---------------------------------------------------
-- An earlier revision of this migration created `slot` as TINYINT, which does
-- not match UserSecurityAnswer.slot (Integer) and fails Hibernate
-- ddl-auto=validate with "found [tinyint], but expecting [integer]".
-- INT also matches the existing student_tesda_qualifications.slot convention.
-- MODIFY COLUMN is naturally idempotent; the guard keeps re-runs silent.
SET @slot_is_tinyint = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_security_answers'
      AND COLUMN_NAME = 'slot' AND DATA_TYPE = 'tinyint'
);
SET @sql = IF(@slot_is_tinyint = 1,
    'ALTER TABLE user_security_answers MODIFY COLUMN slot INT NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Lockout columns on users ---------------------------------------------
SET @has_security_locked = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'security_locked'
);
SET @sql = IF(@has_security_locked = 0,
    'ALTER TABLE users ADD COLUMN security_locked TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_failed_attempts = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'failed_security_attempts'
);
SET @sql = IF(@has_failed_attempts = 0,
    'ALTER TABLE users ADD COLUMN failed_security_attempts INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_lockout_started_at = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'security_lockout_started_at'
);
SET @sql = IF(@has_lockout_started_at = 0,
    'ALTER TABLE users ADD COLUMN security_lockout_started_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3b. Correct failed_security_attempts to INT -------------------------------
-- Same defect as slot above: an earlier revision created this as TINYINT,
-- which does not match User.failedSecurityAttempts (Integer) and fails
-- ddl-auto=validate. security_locked stays TINYINT(1) — that is the correct
-- MySQL mapping for the Boolean field — and security_lockout_started_at
-- (DATETIME / LocalDateTime) is already correct.
SET @attempts_is_tinyint = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'failed_security_attempts' AND DATA_TYPE = 'tinyint'
);
SET @sql = IF(@attempts_is_tinyint = 1,
    'ALTER TABLE users MODIFY COLUMN failed_security_attempts INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Fix seed-account emails, then close the uniqueness gap ---------------
UPDATE users SET email = 'admin@anihan.local' WHERE username = 'admin' AND email <> 'admin@anihan.local';
UPDATE users SET email = 'registrar@anihan.local' WHERE username = 'registrar' AND email <> 'registrar@anihan.local';
UPDATE users SET email = 'trainer@anihan.local' WHERE username = 'trainer' AND email <> 'trainer@anihan.local';

SET @has_email_unique = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'email' AND NON_UNIQUE = 0
);
SET @sql = IF(@has_email_unique = 0,
    'ALTER TABLE users ADD CONSTRAINT uq_email UNIQUE (email)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- VERIFICATION (read-only — safe to run any time)
-- ============================================================

-- Expect: exactly 6 rows
SELECT COUNT(*) AS default_question_count FROM security_questions;

-- Expect: slot = int (NOT tinyint) — must match UserSecurityAnswer.slot
SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_security_answers'
  AND COLUMN_NAME = 'slot';

-- Expect: the 3 new lockout columns present on users
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'
  AND COLUMN_NAME IN ('security_locked', 'failed_security_attempts', 'security_lockout_started_at');

-- Expect: admin@anihan.local / registrar@anihan.local / trainer@anihan.local
SELECT username, email FROM users WHERE username IN ('admin', 'registrar', 'trainer');

-- Expect: exactly ONE row (uq_email, NON_UNIQUE = 0) — will error out above
-- instead of reaching here if a duplicate email exists in the live data.
SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email';
