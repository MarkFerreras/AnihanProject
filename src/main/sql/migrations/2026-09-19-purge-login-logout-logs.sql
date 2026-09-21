-- ============================================================
-- Migration: 2026-09-19 — Purge historical login/logout audit rows
-- ============================================================
-- As of 2026-09-19 the application no longer writes "User logged in" or
-- "User logged out" rows (AuthController's two logAction calls were removed).
-- This migration removes the rows written before that change, so the admin
-- Audit Logs page shows only meaningful record/account actions.
--
-- DELIBERATE EXCEPTION TO AN ESTABLISHED RULE:
--   memory-bank/decisions.md (2026-04-14) documents system_logs as
--   APPEND-ONLY — never updated, never deleted. This is a one-time,
--   user-approved exception, scoped to exactly two action strings. It is
--   NOT a precedent. Take the backup named in the header below before
--   running this; there is no other way back.
--
-- Scope: DATA ONLY. No table, column, index, or constraint is touched, so
-- no `schema.sql` change is needed and `ddl-auto=validate` is unaffected.
--
-- AUTO_INCREMENT on system_logs.log_id is intentionally NOT reset — the
-- resulting gaps in log_id are the visible evidence that rows were removed.
--
-- Idempotent: re-running deletes nothing further (step 2 matches no rows
-- the second time) and re-prints the same zero count.
--
-- Backup taken before the first run:
--   src/main/sql/backup-2026-09-19-pre-log-purge.sql
-- ============================================================

USE AnihanSRMS;

-- 1. Pre-flight: how many rows are about to be removed, and what else is there
SELECT COUNT(*) AS login_logout_rows_before
FROM system_logs
WHERE action IN ('User logged in', 'User logged out');

SELECT COUNT(*) AS total_rows_before FROM system_logs;

-- 2. The purge
DELETE FROM system_logs
WHERE action IN ('User logged in', 'User logged out');

-- 3. Verification: the first count MUST be 0; the second is the survivors
SELECT COUNT(*) AS login_logout_rows_after
FROM system_logs
WHERE action IN ('User logged in', 'User logged out');

SELECT COUNT(*) AS total_rows_after FROM system_logs;

-- 4. Sanity: the distinct actions that remain should contain no login/logout entry
SELECT action, COUNT(*) AS occurrences
FROM system_logs
GROUP BY action
ORDER BY occurrences DESC;
