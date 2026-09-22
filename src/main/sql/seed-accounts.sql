-- ============================================================
-- Fallback demo account seed — INSERT-ONLY, never touches existing rows
-- ============================================================
-- Purpose: guarantee the three canonical demo accounts (admin, registrar,
-- trainer) exist before a client demo, even against a database where one
-- or more of them were never created (a fresh/reset DB, or a partial one).
--
-- Safety contract (see docs/superpowers/plans/2026-09-22-client-presentation-readiness.md):
--   - Only INSERTs. Never UPDATEs or DELETEs any row in `users` or
--     `user_security_answers`.
--   - Each INSERT is itself guarded by `WHERE NOT EXISTS (... WHERE
--     username = '<name>')`, so re-running this script is a no-op once the
--     account exists — it will never overwrite a password, role, or any
--     other column on an account that already exists, no matter what
--     values that existing row holds.
--   - Does not seed `user_security_answers` for the accounts it creates.
--     A newly-inserted account therefore has zero security-question
--     answers and will be required to complete this project's existing
--     mandatory first-login security-question setup flow
--     (SecurityQuestionService / ROLE_PENDING_SETUP) before it can reach
--     its dashboard. That is expected, pre-existing behavior from an
--     already-shipped feature — this script does not need to, and does
--     not, do anything about it.
--
-- Password for all three accounts: password123 (same seed convention and
-- the same bcrypt hash already used for these three usernames in
-- src/main/sql/schema.sql's own fresh-install seed data).
--
-- Usage: run this against a target database only after confirming (e.g.
-- via `SELECT username FROM users WHERE username IN ('admin','registrar',
-- 'trainer')`) that one or more of the three accounts is actually missing.
-- Safe to re-run.
-- ============================================================

USE AnihanSRMS;

START TRANSACTION;

INSERT INTO users (username, password, lastname, firstname, middlename, birthdate, age,
                   email, role, enabled, password_changed_at, security_locked,
                   failed_security_attempts, security_lockout_started_at)
SELECT 'admin', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6',
       'Dela Cruz', 'Juan', 'Santos', '1995-06-15',
       TIMESTAMPDIFF(YEAR, '1995-06-15', CURDATE()), 'admin@anihan.local',
       'ROLE_ADMIN', 1, NULL, 0, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password, lastname, firstname, middlename, birthdate, age,
                   email, role, enabled, password_changed_at, security_locked,
                   failed_security_attempts, security_lockout_started_at)
SELECT 'registrar', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6',
       'Reyes', 'Maria', 'Garcia', '1990-03-22',
       TIMESTAMPDIFF(YEAR, '1990-03-22', CURDATE()), 'registrar@anihan.local',
       'ROLE_REGISTRAR', 1, NULL, 0, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'registrar');

INSERT INTO users (username, password, lastname, firstname, middlename, birthdate, age,
                   email, role, enabled, password_changed_at, security_locked,
                   failed_security_attempts, security_lockout_started_at)
SELECT 'trainer', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6',
       'Santos', 'Carlos', 'Mendoza', '1988-11-08',
       TIMESTAMPDIFF(YEAR, '1988-11-08', CURDATE()), 'trainer@anihan.local',
       'ROLE_TRAINER', 1, NULL, 0, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'trainer');

COMMIT;

-- Existing rows were not changed. Newly inserted rows have zero answers and must complete setup.
SELECT u.user_id, u.username, u.role, u.enabled, u.security_locked,
       COUNT(a.answer_id) AS security_answer_count
FROM users u
LEFT JOIN user_security_answers a ON a.user_id = u.user_id
WHERE u.username IN ('admin', 'registrar', 'trainer')
GROUP BY u.user_id, u.username, u.role, u.enabled, u.security_locked
ORDER BY u.username;
