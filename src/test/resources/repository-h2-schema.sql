-- H2-compatible mirror of the src/main/sql/schema.sql tables touched by
-- ClassManagementRepositoriesH2Test, keeping the real UNIQUE keys, foreign keys
-- and ON UPDATE/DELETE rules. Hibernate create-drop cannot be used: H2 has no
-- MySQL YEAR type (batches.batch_year), entities do not declare every unique
-- key, and create-drop never reproduces `DEFAULT CURRENT_TIMESTAMP`.

DROP TABLE IF EXISTS class_enrollments;
DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS student_records;
DROP TABLE IF EXISTS sections;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS qualifications;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS batches;
DROP TABLE IF EXISTS users;

CREATE TABLE batches (
    batch_code VARCHAR(20) NOT NULL PRIMARY KEY,
    batch_year SMALLINT NOT NULL
);

CREATE TABLE courses (
    course_code VARCHAR(20) NOT NULL PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL
);

CREATE TABLE qualifications (
    qualification_code INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    qualification_name VARCHAR(255) NOT NULL,
    qualification_description VARCHAR(255) NOT NULL
);

CREATE TABLE sections (
    section_code VARCHAR(20) NOT NULL PRIMARY KEY,
    section VARCHAR(25) NOT NULL,
    batch_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code)
);

CREATE TABLE subjects (
    subject_code VARCHAR(20) NOT NULL PRIMARY KEY,
    subject_name VARCHAR(255) NOT NULL,
    qualification_code INT NULL,
    competency_type VARCHAR(15) NOT NULL,
    units INT NOT NULL,
    FOREIGN KEY (qualification_code) REFERENCES qualifications (qualification_code)
);

CREATE TABLE users (
    user_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    middlename VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL DEFAULT '2000-01-01',
    age INT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(15) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    password_changed_at DATETIME NULL,
    security_locked TINYINT NOT NULL DEFAULT 0,
    failed_security_attempts INT NOT NULL DEFAULT 0,
    security_lockout_started_at DATETIME NULL,
    UNIQUE (username),
    UNIQUE (email)
);

CREATE TABLE student_records (
    record_id INT NOT NULL AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    student_number VARCHAR(20) NULL,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NULL,
    birthdate DATE NULL,
    age INT NULL,
    sex VARCHAR(10) NULL,
    civil_status VARCHAR(50) NULL,
    permanent_address VARCHAR(255) NULL,
    temporary_address VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    contact_no VARCHAR(255) NULL,
    religion VARCHAR(255) NULL,
    baptized TINYINT NOT NULL DEFAULT 0,
    baptism_date DATE NULL,
    baptism_place VARCHAR(255) NULL,
    sibling_count INT NULL,
    brother_count INT NULL,
    sister_count INT NULL,
    batch_code VARCHAR(20) NULL,
    course_code VARCHAR(20) NULL,
    section_code VARCHAR(20) NULL,
    profile_picture MEDIUMBLOB NULL,
    enrollment_date DATE NULL,
    completion_date DATE NULL,
    student_status VARCHAR(25) NOT NULL DEFAULT 'Enrolling',
    employment_status VARCHAR(25) NULL,
    PRIMARY KEY (record_id),
    UNIQUE (student_id),
    UNIQUE (student_number),
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code),
    FOREIGN KEY (section_code) REFERENCES sections (section_code)
);

CREATE TABLE classes (
    class_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    trainer_id INT NULL,
    semester VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (section_code, subject_code, semester),
    FOREIGN KEY (section_code) REFERENCES sections (section_code),
    FOREIGN KEY (subject_code) REFERENCES subjects (subject_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (trainer_id) REFERENCES users (user_id) ON DELETE SET NULL
);

CREATE TABLE class_enrollments (
    enrollment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id INT NOT NULL,
    student_id VARCHAR(20) NOT NULL,
    enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (class_id, student_id),
    FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
);

CREATE TABLE grades (
    grade_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    final_grade DECIMAL(5, 2) NULL,
    re_exam_grade DECIMAL(5, 2) NULL,
    grade_status VARCHAR(5) NULL,
    hours_rendered DECIMAL(5, 2) NULL,
    remarks VARCHAR(20) NULL,
    class_id INT NULL,
    final_percentage DECIMAL(5, 2) NULL,
    re_exam_percentage DECIMAL(5, 2) NULL,
    locked TINYINT NOT NULL DEFAULT 0,
    locked_at DATETIME NULL,
    UNIQUE (class_id, student_id),
    FOREIGN KEY (student_id) REFERENCES student_records (student_id),
    FOREIGN KEY (subject_code) REFERENCES subjects (subject_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes (class_id) ON DELETE SET NULL
);

CREATE TABLE documents (
    document_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    document_label VARCHAR(100) NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size INT NOT NULL,
    content_data LONGBLOB NOT NULL,
    upload_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
);
