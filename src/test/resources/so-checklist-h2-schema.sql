-- Grade tables for SoChecklistIntegrationTest, loaded AFTER
-- document-storage-h2-schema.sql. Only the columns SoChecklistService reads.
-- Deliberately no foreign keys to student_records, so the first script's
-- DROP TABLE student_records can never trip over these tables.

DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS class_enrollments;
DROP TABLE IF EXISTS classes;

CREATE TABLE classes (
    class_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    semester VARCHAR(20) NOT NULL
);

CREATE TABLE class_enrollments (
    enrollment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    class_id INT NOT NULL,
    student_id VARCHAR(20) NOT NULL,
    UNIQUE (class_id, student_id)
);

CREATE TABLE grades (
    grade_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(20) NOT NULL,
    subject_code VARCHAR(20) NOT NULL,
    grade_status VARCHAR(5) NULL,
    remarks VARCHAR(20) NULL,
    class_id INT NULL,
    locked TINYINT NOT NULL DEFAULT 0,
    locked_at DATETIME NULL,
    UNIQUE (class_id, student_id)
);
