-- ============================================================
-- SEED DATA: 3 Dummy Accounts
-- Password for ALL accounts: password123
-- BCrypt hash: $2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6
-- ============================================================
INSERT INTO
    users (
        username,
        password,
        lastname,
        firstname,
        middlename,
        birthdate,
        age,
        email,
        role,
        enabled,
        password_changed_at
    )
VALUES (
        'admin',
        '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6',
        'Dela Cruz',
        'Juan',
        'Santos',
        '1995-06-15',
        30,
        'admin@anihan.local',
        'ROLE_ADMIN',
        1,
        NULL
    ),
    (
        'registrar',
        '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6',
        'Reyes',
        'Maria',
        'Garcia',
        '1990-03-22',
        36,
        'registrar@anihan.local',
        'ROLE_REGISTRAR',
        1,
        NULL
    ),
    (
        'trainer',
        '$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6',
        'Santos',
        'Carlos',
        'Mendoza',
        '1988-11-08',
        37,
        'trainer@anihan.local',
        'ROLE_TRAINER',
        1,
        NULL
    );