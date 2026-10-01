-- Minimal H2-compatible mirror of src/main/sql/schema.sql, scoped to the
-- tables DocumentStorageIntegrationTest touches. Hibernate's create-drop
-- cannot be used here: (1) H2 has no MySQL YEAR type, which batches.batch_year
-- uses in production, and (2) create-drop never reproduces schema.sql's
-- `DEFAULT CURRENT_TIMESTAMP` on documents.upload_date. This script is loaded
-- with spring.jpa.hibernate.ddl-auto=none, matching how the real app never
-- lets Hibernate manage DDL.

DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS student_records;
DROP TABLE IF EXISTS sections;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS batches;
DROP TABLE IF EXISTS system_logs;

CREATE TABLE batches (
    batch_code VARCHAR(20) NOT NULL PRIMARY KEY,
    batch_year SMALLINT NOT NULL
);

CREATE TABLE courses (
    course_code VARCHAR(20) NOT NULL PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL
);

CREATE TABLE sections (
    section_code VARCHAR(20) NOT NULL PRIMARY KEY,
    section VARCHAR(25) NOT NULL,
    batch_code VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code)
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
    profile_picture BLOB NULL,
    enrollment_date DATE NULL,
    student_status VARCHAR(25) NOT NULL DEFAULT 'Enrolling',
    PRIMARY KEY (record_id),
    UNIQUE (student_id),
    UNIQUE (student_number),
    FOREIGN KEY (batch_code) REFERENCES batches (batch_code),
    FOREIGN KEY (course_code) REFERENCES courses (course_code),
    FOREIGN KEY (section_code) REFERENCES sections (section_code)
);

CREATE TABLE documents (
    document_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    document_label VARCHAR(100) NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size INT NOT NULL,
    content_data BLOB NOT NULL,
    upload_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student_records (student_id)
);

CREATE TABLE system_logs (
    log_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    username VARCHAR(255) NOT NULL,
    role VARCHAR(15) NOT NULL,
    action VARCHAR(500) NOT NULL,
    ip_address VARCHAR(45) NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
