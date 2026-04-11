SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS users (
    user_id int NOT NULL PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    firstname VARCHAR(255) NOT NULL,
    middlename VARCHAR(255) NOT NULL,
    middlename VARCHAR(255) NOT NULL,
    main birthdate DATE NOT NULL DEFAULT '2000-01-01',
    age INT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(15) NOT NULL
);

-- RUN THIS IS YOU ALREADY HAVE users TABLE! Otherwise, just run the CREATE TABLE for users
ALTER TABLE users
ADD COLUMN lastname VARCHAR(255) NOT NULL,
ADD COLUMN firstname VARCHAR(255) NOT NULL,
ADD COLUMN birthdate DATE NOT NULL DEFAULT '2000-01-01',
ADD COLUMN age INT NOT NULL;

ADD COLUMN middlename VARCHAR(255) NOT NULL,
ADD COLUMN birthdate DATE NOT NULL DEFAULT '2000-01-01',
ADD COLUMN age INT NOT NULL;
-- Run this if already have an existing student_records table
ALTER TABLE student_records add column age INT NOT NULL;

main

CREATE TABLE IF NOT EXISTS student_records (
    record_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL, -- NOTE: Automatically calculate the age in the Java code and NOT in the SQL!!!
    age INT NOT NULL,
    sex VARCHAR(10) NOT NULL,
    permanent_address VARCHAR(255) NOT NULL,
    temporary_address VARCHAR(255) NULL,
    email VARCHAR(255) NOT NULL,
    contact_no VARCHAR(255) NOT NULL,
    religion VARCHAR(255) NOT NULL,
    baptized TINYINT(1) NOT NULL DEFAULT 0, -- This is BOOLEAN
    baptism_date DATE NULL,
    baptism_place VARCHAR(255) NOT NULL,
    sibling_count INT NOT NULL,
    brother_count INT NULL,
    sister_count INT NULL,
    batch_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    section_code VARCHAR(20) NOT NULL,
    profile_picture MEDIUMBLOB NULL,
    enrollment_date DATE NULL,
    student_status VARCHAR(25) NOT NULL DEFAULT "Enrolling",
    -- Foreign Keys
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code),
    FOREIGN KEY (section_code) REFERENCES sections (section_code)
);

CREATE TABLE IF NOT EXISTS batches (
    batch_code VARCHAR(20) NOT NULL PRIMARY KEY,
    batch_year YEAR NOT NULL
);

CREATE TABLE IF NOT EXISTS courses (
    course_code VARCHAR(20) NOT NULL PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS sections (
    section_code VARCHAR(20) NOT NULL PRIMARY KEY,
    section VARCHAR(25) NOT NULL,
    batch_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    -- FOREIGN KEYS
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code)
);

CREATE TABLE IF NOT EXISTS parents (
    parent_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    relation VARCHAR(20) NOT NULL,
    family_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL,
    occupation VARCHAR(255) NOT NULL,
    est_income DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    contact_no VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    -- FOREIGN KEY
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
);

CREATE TABLE IF NOT EXISTS other_guardians (
    guardian_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    relation VARCHAR(20) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL,
    address VARCHAR(255) NOT NULL,
    -- FOREIGN KEY
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
);

CREATE TABLE IF NOT EXISTS documents (
    document_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    document_type VARCHAR(255) NOT NULL, -- This is to determine if TOR, Grades, etc.
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- This is to determine if pdf, docx, etc.
    file_size INT NOT NULL,
    content_data LONGBLOB NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Please double check this
    -- FOREIGN KEY
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
);

CREATE TABLE IF NOT EXISTS grades (
    grade_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    final_grade DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    re_exam_grade DECIMAL(5, 2) NULL,
    hours_studied DECIMAL(3, 2) NOT NULL,
    remarks VARCHAR(255) NOT NULL,
    -- FOREIGN KEYS
    FOREIGN KEY (student_id) REFERENCES student_records (student_id),
    FOREIGN KEY (subject_code) REFERENCES subjects (subject_code)
);

CREATE TABLE IF NOT EXISTS subjects (
    subject_code VARCHAR(20) NOT NULL PRIMARY KEY,
    subject_name VARCHAR(255) NOT NULL,
    qualification_code INT NOT NULL,
    units INT NOT NULL,
    -- FOREIGN KEYS
    FOREIGN KEY (qualification_code) REFERENCES qualifications (qualification_code)
);

CREATE TABLE IF NOT EXISTS qualifications (
    qualification_code INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    qualification_name VARCHAR(255) NOT NULL,
    qualification_description VARCHAR(255) NOT NULL -- Remove?
);

CREATE TABLE IF NOT EXISTS qualification_assessment (
    assessemnt_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    qualification_code INT NOT NULL,
    student_id VARCHAR(20) NOT NULL,
    assessor_id INT NOT NULL,
    assessment VARCHAR(255) NOT NULL,
    assessment_result VARCHAR(255) NOT NULL,
    assessment_date DATE NOT NULL,
    training_start_date DATE NOT NULL,
    training_end_date DATE NOT NULL,
    -- FOREIGN KEYS
    FOREIGN KEY (qualification_code) REFERENCES qualifications (qualification_code),
    FOREIGN KEY (student_id) REFERENCES student_records (student_id),
    FOREIGN KEY (assessor_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS classess (
    class_code INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    section_code VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    trainer_id INT NOT NULL,
    -- FOREIGN KEYS
    FOREIGN KEY (section_code) REFERENCES sections (section_code),
    FOREIGN KEY (subject_code) REFERENCES subjects (subject_code),
    FOREIGN KEY (trainer_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS log(
    log_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    event VARCHAR(255) NOT NULL,
    user_id INT NOT NULL,
    log_time TIME,
    log_date DATE,
    -- FOREIGN KEYS
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS previous_school (
    school_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(20) NOT NULL,
    educational_level VARCHAR(25) NOT NULL,
    school VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    start_level_year YEAR NOT NULL,
    end_level_year YEAR NOT NULL,
    -- FOREIGN KEY
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
);

);

SET FOREIGN_KEY_CHECKS = 1;