-- ============================================================
-- seed-accounts.sql — Login Accounts (REQUIRED)
-- Created: 2026-06-22
-- Purpose: The three role-based login accounts the system needs to
--          operate. Run this AFTER schema.sql on a fresh database.
--
--   Apply:  mysql -u <user> -p AnihanSRMS < seed-accounts.sql
--
-- ------------------------------------------------------------
--  Username   | Role            | Password
-- ------------------------------------------------------------
--  admin      | ROLE_ADMIN      | password123
--  registrar  | ROLE_REGISTRAR  | password123
--  trainer    | ROLE_TRAINER    | password123
-- ------------------------------------------------------------
--
--  All three share the same BCrypt hash for the password
--  "password123":
--    $2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6
--
--  SECURITY NOTE (production / client deployment):
--  Change every password immediately after first login via the
--  Edit Account modal. These defaults are for first access only and
--  must NOT remain in use on a live deployment.
-- ============================================================

USE AnihanSRMS;

INSERT INTO users (username, password, lastname, firstname, middlename, birthdate, age, email, role, enabled, password_changed_at) VALUES
('admin',     '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Dela Cruz', 'Juan',   'Santos',  '1995-06-15', 30, 'juan.delacruz@example.com', 'ROLE_ADMIN',     1, NULL),
('registrar', '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Reyes',     'Maria',  'Garcia',  '1990-03-22', 36, 'maria.reyes@example.com',   'ROLE_REGISTRAR', 1, NULL),
('trainer',   '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6', 'Santos',    'Carlos', 'Mendoza', '1988-11-08', 37, 'carlos.santos@example.com', 'ROLE_TRAINER',   1, NULL);
