-- ============================================================
-- seed-sample-students.sql — Demo Student Records (OPTIONAL — DEV ONLY)
-- Created: 2026-06-22
-- Purpose: 5 fully-populated sample students for development and demos.
--          DO NOT load this on a real client/production deployment —
--          the registrar enters real students via the app.
--          Run AFTER schema.sql, seed-accounts.sql, and seed-lookups.sql.
--
--   Apply:  mysql -u <user> -p AnihanSRMS < seed-sample-students.sql
--
-- Note: profile_picture is an empty BLOB (X'') placeholder; replace with
--       a real upload via the registrar once available.
-- ============================================================

USE AnihanSRMS;

INSERT INTO student_records (
    student_id, last_name, first_name, middle_name,
    birthdate, age, sex, civil_status,
    permanent_address, temporary_address, email, contact_no, religion,
    baptized, baptism_date, baptism_place,
    sibling_count, brother_count, sister_count,
    batch_code, course_code, section_code,
    profile_picture, enrollment_date, student_status
) VALUES
('STU-2024-001', 'Reyes',     'Anna',     'Cruz',
    '2003-04-12', 22, 'Female', 'Single',
    '123 Mabini St, Quezon City',  '45 Aurora Blvd, Manila',  'anna.reyes@example.com',     '09171234001', 'Roman Catholic',
    1, '2003-06-20', 'San Pedro Parish, Manila',
    2, 1, 1,
    'B2024A', 'CARS', 'SEC-A24',
    X'', '2024-06-03', 'Active'),

('STU-2024-002', 'Santos',    'Bea',      'Lim',
    '2002-09-30', 23, 'Female', 'Single',
    '88 Roxas Ave, Pasig',         '12 EDSA, Mandaluyong',    'bea.santos@example.com',     '09171234002', 'Iglesia ni Cristo',
    1, '2003-01-15', 'INC Central Temple, Quezon City',
    3, 2, 1,
    'B2024A', 'CARS', 'SEC-A24',
    X'', '2024-06-03', 'Active'),

('STU-2025-001', 'Cruz',      'Carla',    'Mendoza',
    '2004-01-18', 22, 'Female', 'Single',
    '7 Bonifacio St, Makati',      '7 Bonifacio St, Makati',  'carla.cruz@example.com',     '09171234003', 'Christian',
    1, '2004-05-10', 'Christ Fellowship Church, Makati',
    1, 0, 1,
    'B2025A', 'CARS', 'SEC-A25',
    X'', '2025-06-02', 'Active'),

('STU-2025-002', 'Garcia',    'Diana',    'Reyes',
    '2003-12-05', 22, 'Female', 'Single',
    '256 Espana Blvd, Manila',     '256 Espana Blvd, Manila', 'diana.garcia@example.com',   '09171234004', 'Roman Catholic',
    1, '2004-02-28', 'Sto. Domingo Church, Manila',
    4, 2, 2,
    'B2025A', 'CARS', 'SEC-A25',
    X'', '2025-06-02', 'Active'),

('STU-2026-001', 'Lopez',     'Elise',    'Tan',
    '2005-07-22', 20, 'Female', 'Single',
    '19 Katipunan Ave, Quezon City','19 Katipunan Ave, Quezon City','elise.lopez@example.com', '09171234005', 'Roman Catholic',
    1, '2005-10-14', 'Mary Immaculate Parish, Quezon City',
    2, 0, 2,
    'B2026A', 'CARS', 'SEC-A26',
    X'', '2026-06-01', 'Active');
