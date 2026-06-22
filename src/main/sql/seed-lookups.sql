-- ============================================================
-- seed-lookups.sql — Reference / Lookup Data (REQUIRED)
-- Created: 2026-06-22
-- Purpose: Foundational lookup rows the app needs to function:
--          1 course, 3 batches, 3 sections, 2 qualifications, 6 subjects.
--          These are FK targets for student records and class management.
--          Run AFTER schema.sql and seed-accounts.sql.
--
--   Apply:  mysql -u <user> -p AnihanSRMS < seed-lookups.sql
-- ============================================================

USE AnihanSRMS;

-- Courses
INSERT INTO courses (course_code, course_name) VALUES
('CARS', 'Culinary Arts and Restaurant Services');

-- Batches
INSERT INTO batches (batch_code, batch_year) VALUES
('B2024A', 2024),
('B2025A', 2025),
('B2026A', 2026);

-- Sections
INSERT INTO sections (section_code, section, batch_code, course_code) VALUES
('SEC-A24', 'Section A 2024', 'B2024A', 'CARS'),
('SEC-A25', 'Section A 2025', 'B2025A', 'CARS'),
('SEC-A26', 'Section A 2026', 'B2026A', 'CARS');

-- Qualifications
INSERT INTO qualifications (qualification_name, qualification_description) VALUES
('Cookery NC II', 'TESDA National Certificate II in Cookery'),
('Bread and Pastry Production NC II', 'TESDA NC II in Bread and Pastry Production');

-- Subjects
INSERT INTO subjects (subject_code, subject_name, qualification_code, units, trainer_id) VALUES
('COOK-101', 'Introduction to Cookery',    (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Cookery NC II'),                      3, NULL),
('COOK-102', 'Food Safety and Sanitation', (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Cookery NC II'),                      3, NULL),
('COOK-103', 'Prepare Hot Meals',          (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Cookery NC II'),                      5, NULL),
('COOK-104', 'Prepare Cold Meals',         (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Cookery NC II'),                      4, NULL),
('BPP-101',  'Bread Making Fundamentals',  (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Bread and Pastry Production NC II'), 3, NULL),
('BPP-102',  'Pastry Arts',                (SELECT qualification_code FROM qualifications WHERE qualification_name = 'Bread and Pastry Production NC II'), 4, NULL);
