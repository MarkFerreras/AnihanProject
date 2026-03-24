CREATE TABLE IF NOT EXISTS users (
    user_id int NOT NULL PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(15) NOT NULL
);

CREATE TABLE IF NOT EXISTS student_records (
    student_id VARCHAR(20) NOT NULL PRIMARY KEY,
    last_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NOT NULL,
    birthdate DATE NOT NULL, -- NOTE: Automatically calculate the age in the Java code and NOT in the SQL!!!
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