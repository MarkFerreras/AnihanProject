-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: AnihanSRMS
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `batches`
--

DROP TABLE IF EXISTS `batches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batches` (
  `batch_code` varchar(20) NOT NULL,
  `batch_year` year NOT NULL,
  PRIMARY KEY (`batch_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `batches`
--

LOCK TABLES `batches` WRITE;
/*!40000 ALTER TABLE `batches` DISABLE KEYS */;
INSERT INTO `batches` VALUES ('B2024A',2024),('B2025A',2025),('B2026A',2026);
/*!40000 ALTER TABLE `batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_enrollments`
--

DROP TABLE IF EXISTS `class_enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_enrollments` (
  `enrollment_id` int NOT NULL AUTO_INCREMENT,
  `class_id` int NOT NULL,
  `student_id` varchar(20) NOT NULL,
  `enrolled_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `uq_class_student` (`class_id`,`student_id`),
  KEY `student_id` (`student_id`),
  CONSTRAINT `class_enrollments_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE CASCADE,
  CONSTRAINT `class_enrollments_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_enrollments`
--

LOCK TABLES `class_enrollments` WRITE;
/*!40000 ALTER TABLE `class_enrollments` DISABLE KEYS */;
INSERT INTO `class_enrollments` VALUES (1,1,'STU-2026-001','2026-05-09 03:47:42');
/*!40000 ALTER TABLE `class_enrollments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classes` (
  `class_id` int NOT NULL AUTO_INCREMENT,
  `section_code` varchar(20) NOT NULL,
  `subject_code` varchar(20) NOT NULL,
  `trainer_id` int DEFAULT NULL,
  `semester` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`class_id`),
  UNIQUE KEY `uq_class` (`section_code`,`subject_code`,`semester`),
  KEY `subject_code` (`subject_code`),
  KEY `trainer_id` (`trainer_id`),
  CONSTRAINT `classes_ibfk_1` FOREIGN KEY (`section_code`) REFERENCES `sections` (`section_code`),
  CONSTRAINT `classes_ibfk_2` FOREIGN KEY (`subject_code`) REFERENCES `subjects` (`subject_code`),
  CONSTRAINT `classes_ibfk_3` FOREIGN KEY (`trainer_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (1,'SEC-A26','BPP-101',4,'2026','2026-05-09 03:47:37');
/*!40000 ALTER TABLE `classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `course_code` varchar(20) NOT NULL,
  `course_name` varchar(100) NOT NULL,
  PRIMARY KEY (`course_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` VALUES ('CARS','Culinary Arts and Restaurant Services');
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `documents`
--

DROP TABLE IF EXISTS `documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `documents` (
  `document_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `document_type` varchar(255) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_type` varchar(50) NOT NULL,
  `file_size` int NOT NULL,
  `content_data` longblob NOT NULL,
  `upload_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`document_id`),
  KEY `documents_ibfk_1` (`student_id`),
  CONSTRAINT `documents_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grades`
--

DROP TABLE IF EXISTS `grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grades` (
  `grade_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `subject_code` varchar(20) NOT NULL,
  `class_id` int DEFAULT NULL,
  `midterm_grade` decimal(5,2) DEFAULT NULL,
  `finals_grade` decimal(5,2) DEFAULT NULL,
  `locked` tinyint(1) NOT NULL DEFAULT '0',
  `locked_at` datetime DEFAULT NULL,
  `final_grade` decimal(5,2) DEFAULT NULL,
  `re_exam_grade` decimal(5,2) DEFAULT NULL,
  `hours_studied` decimal(5,2) DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`grade_id`),
  UNIQUE KEY `uq_grade_student_class` (`class_id`,`student_id`),
  KEY `subject_code` (`subject_code`),
  KEY `grades_ibfk_1` (`student_id`),
  CONSTRAINT `fk_grades_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE SET NULL,
  CONSTRAINT `grades_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`),
  CONSTRAINT `grades_ibfk_2` FOREIGN KEY (`subject_code`) REFERENCES `subjects` (`subject_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grades`
--

LOCK TABLES `grades` WRITE;
/*!40000 ALTER TABLE `grades` DISABLE KEYS */;
INSERT INTO `grades` VALUES (1,'STU-2026-001','BPP-101',1,1.00,2.00,0,NULL,1.60,NULL,NULL,NULL);
/*!40000 ALTER TABLE `grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `other_guardians`
--

DROP TABLE IF EXISTS `other_guardians`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `other_guardians` (
  `guardian_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `relation` varchar(20) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `middle_name` varchar(255) DEFAULT NULL,
  `birthdate` date DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`guardian_id`),
  KEY `other_guardians_ibfk_1` (`student_id`),
  CONSTRAINT `other_guardians_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `other_guardians`
--

LOCK TABLES `other_guardians` WRITE;
/*!40000 ALTER TABLE `other_guardians` DISABLE KEYS */;
/*!40000 ALTER TABLE `other_guardians` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parents`
--

DROP TABLE IF EXISTS `parents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parents` (
  `parent_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `relation` varchar(20) NOT NULL,
  `family_name` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `middle_name` varchar(255) DEFAULT NULL,
  `birthdate` date DEFAULT NULL,
  `occupation` varchar(255) DEFAULT NULL,
  `est_income` decimal(15,2) DEFAULT NULL,
  `contact_no` varchar(20) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`parent_id`),
  KEY `parents_ibfk_1` (`student_id`),
  CONSTRAINT `parents_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parents`
--

LOCK TABLES `parents` WRITE;
/*!40000 ALTER TABLE `parents` DISABLE KEYS */;
INSERT INTO `parents` VALUES (1,'SR20260002','FATHER','x','x',NULL,'2026-04-29','x',NULL,'+63 12',NULL,'x'),(2,'SR20260002','MOTHER','x','x',NULL,'2026-06-03','x',NULL,'+63 12',NULL,'x');
/*!40000 ALTER TABLE `parents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qualifications`
--

DROP TABLE IF EXISTS `qualifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qualifications` (
  `qualification_code` int NOT NULL AUTO_INCREMENT,
  `qualification_name` varchar(255) NOT NULL,
  `qualification_description` varchar(255) NOT NULL,
  PRIMARY KEY (`qualification_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qualifications`
--

LOCK TABLES `qualifications` WRITE;
/*!40000 ALTER TABLE `qualifications` DISABLE KEYS */;
INSERT INTO `qualifications` VALUES (1,'Cookery NC II','TESDA National Certificate II in Cookery'),(2,'Bread and Pastry Production NC II','TESDA NC II in Bread and Pastry Production');
/*!40000 ALTER TABLE `qualifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sections`
--

DROP TABLE IF EXISTS `sections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sections` (
  `section_code` varchar(20) NOT NULL,
  `section` varchar(25) NOT NULL,
  `batch_code` varchar(20) NOT NULL,
  `course_code` varchar(20) NOT NULL,
  PRIMARY KEY (`section_code`),
  KEY `batch_code` (`batch_code`),
  KEY `course_code` (`course_code`),
  CONSTRAINT `sections_ibfk_1` FOREIGN KEY (`batch_code`) REFERENCES `batches` (`batch_code`),
  CONSTRAINT `sections_ibfk_2` FOREIGN KEY (`course_code`) REFERENCES `courses` (`course_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sections`
--

LOCK TABLES `sections` WRITE;
/*!40000 ALTER TABLE `sections` DISABLE KEYS */;
INSERT INTO `sections` VALUES ('SEC-A24','Section A 2024','B2024A','CARS'),('SEC-A25','Section A 2025','B2025A','CARS'),('SEC-A26','Section A 2026','B2026A','CARS');
/*!40000 ALTER TABLE `sections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_education`
--

DROP TABLE IF EXISTS `student_education`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_education` (
  `education_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `level` varchar(50) NOT NULL,
  `school_name` varchar(255) DEFAULT NULL,
  `school_address` varchar(255) DEFAULT NULL,
  `grade_year` varchar(50) DEFAULT NULL,
  `semester` varchar(20) DEFAULT NULL,
  `ended_year` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`education_id`),
  UNIQUE KEY `uq_student_education` (`student_id`,`level`),
  CONSTRAINT `student_education_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_education`
--

LOCK TABLES `student_education` WRITE;
/*!40000 ALTER TABLE `student_education` DISABLE KEYS */;
INSERT INTO `student_education` VALUES (1,'SR20260001','Elementary',NULL,NULL,NULL,NULL,NULL),(2,'SR20260001','Secondary',NULL,NULL,NULL,NULL,NULL),(3,'SR20260001','Tertiary',NULL,NULL,NULL,NULL,NULL),(4,'SR20260001','Vocational',NULL,NULL,NULL,NULL,NULL),(5,'SR20260002','Elementary',NULL,NULL,NULL,NULL,NULL),(6,'SR20260002','Secondary',NULL,NULL,NULL,NULL,NULL),(7,'SR20260002','Tertiary',NULL,NULL,NULL,NULL,NULL),(8,'SR20260002','Vocational',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `student_education` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_ojt`
--

DROP TABLE IF EXISTS `student_ojt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_ojt` (
  `ojt_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `company_address` varchar(255) DEFAULT NULL,
  `hours_rendered` decimal(8,2) DEFAULT NULL,
  PRIMARY KEY (`ojt_id`),
  UNIQUE KEY `uq_student_ojt` (`student_id`),
  CONSTRAINT `student_ojt_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_ojt`
--

LOCK TABLES `student_ojt` WRITE;
/*!40000 ALTER TABLE `student_ojt` DISABLE KEYS */;
INSERT INTO `student_ojt` VALUES (2,'SR20260002',NULL,NULL,NULL);
/*!40000 ALTER TABLE `student_ojt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_records`
--

DROP TABLE IF EXISTS `student_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_records` (
  `record_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `middle_name` varchar(255) DEFAULT NULL,
  `birthdate` date DEFAULT NULL,
  `age` int DEFAULT NULL,
  `sex` varchar(10) DEFAULT NULL,
  `civil_status` varchar(50) DEFAULT NULL,
  `permanent_address` varchar(255) DEFAULT NULL,
  `temporary_address` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `contact_no` varchar(255) DEFAULT NULL,
  `religion` varchar(255) DEFAULT NULL,
  `baptized` tinyint(1) NOT NULL DEFAULT '0',
  `baptism_date` date DEFAULT NULL,
  `baptism_place` varchar(255) DEFAULT NULL,
  `sibling_count` int DEFAULT NULL,
  `brother_count` int DEFAULT NULL,
  `sister_count` int DEFAULT NULL,
  `batch_code` varchar(20) DEFAULT NULL,
  `course_code` varchar(20) DEFAULT NULL,
  `section_code` varchar(20) DEFAULT NULL,
  `profile_picture` mediumblob,
  `enrollment_date` date DEFAULT NULL,
  `student_status` varchar(25) NOT NULL DEFAULT 'Enrolling',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `idx_student_id` (`student_id`),
  KEY `batch_code` (`batch_code`),
  KEY `course_code` (`course_code`),
  KEY `section_code` (`section_code`),
  CONSTRAINT `student_records_ibfk_1` FOREIGN KEY (`batch_code`) REFERENCES `batches` (`batch_code`),
  CONSTRAINT `student_records_ibfk_2` FOREIGN KEY (`course_code`) REFERENCES `courses` (`course_code`),
  CONSTRAINT `student_records_ibfk_3` FOREIGN KEY (`section_code`) REFERENCES `sections` (`section_code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_records`
--

LOCK TABLES `student_records` WRITE;
/*!40000 ALTER TABLE `student_records` DISABLE KEYS */;
INSERT INTO `student_records` VALUES (1,'STU-2024-001','Reyes','Anna','Cruz','2003-04-12',22,'Female','Single','123 Mabini St, Quezon City','45 Aurora Blvd, Manila','anna.reyes@example.com','09171234001','Roman Catholic',1,'2003-06-20','San Pedro Parish, Manila',2,1,1,'B2024A','CARS','SEC-A24','','2024-06-03','Active'),(2,'STU-2024-002','Santos','Bea','Lim','2002-09-30',23,'Female','Single','88 Roxas Ave, Pasig','12 EDSA, Mandaluyong','bea.santos@example.com','09171234002','Iglesia ni Cristo',1,'2003-01-15','INC Central Temple, Quezon City',3,2,1,'B2024A','CARS','SEC-A24','','2024-06-03','Active'),(3,'STU-2025-001','Cruz','Carla','Mendoza','2004-01-18',22,'Female','Single','7 Bonifacio St, Makati','7 Bonifacio St, Makati','carla.cruz@example.com','09171234003','Christian',1,'2004-05-10','Christ Fellowship Church, Makati',1,0,1,'B2025A','CARS','SEC-A25','','2025-06-02','Active'),(4,'STU-2025-002','Garcia','Diana','Reyes','2003-12-05',22,'Female','Single','256 Espana Blvd, Manila','256 Espana Blvd, Manila','diana.garcia@example.com','09171234004','Roman Catholic',1,'2004-02-28','Sto. Domingo Church, Manila',4,2,2,'B2025A','CARS','SEC-A25','','2025-06-02','Active'),(5,'STU-2026-001','Lopez','Elise','Tan','2005-07-22',20,'Female','Single','19 Katipunan Ave, Quezon City','19 Katipunan Ave, Quezon City','elise.lopez@example.com','09171234005','Roman Catholic',1,'2005-10-14','Mary Immaculate Parish, Quezon City',2,0,2,'B2026A','CARS','SEC-A26','','2026-06-01','Active'),(6,'SR20260001','Ligan','Sean','Michael','2004-10-12',21,'Female','Single','x',NULL,NULL,'+63 12','roman catholic',0,NULL,NULL,2,1,1,NULL,NULL,NULL,NULL,NULL,'Enrolling'),(7,'SR20260002','x','x','x','2026-05-20',0,'Female','Single','x',NULL,NULL,'+63 12','b',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Draft');
/*!40000 ALTER TABLE `student_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_school_years`
--

DROP TABLE IF EXISTS `student_school_years`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_school_years` (
  `school_year_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `row_index` int NOT NULL,
  `sy_start` varchar(20) DEFAULT NULL,
  `sem_start` varchar(20) DEFAULT NULL,
  `sy_end` varchar(20) DEFAULT NULL,
  `sem_end` varchar(20) DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`school_year_id`),
  UNIQUE KEY `uq_student_school_year` (`student_id`,`row_index`),
  CONSTRAINT `student_school_years_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_school_years`
--

LOCK TABLES `student_school_years` WRITE;
/*!40000 ALTER TABLE `student_school_years` DISABLE KEYS */;
INSERT INTO `student_school_years` VALUES (2,'SR20260002',1,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `student_school_years` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_tesda_qualifications`
--

DROP TABLE IF EXISTS `student_tesda_qualifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_tesda_qualifications` (
  `qual_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `slot` int NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `center_address` varchar(255) DEFAULT NULL,
  `assessment_date` date DEFAULT NULL,
  `result` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`qual_id`),
  UNIQUE KEY `uq_student_tesda_qual` (`student_id`,`slot`),
  CONSTRAINT `student_tesda_qualifications_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_tesda_qualifications`
--

LOCK TABLES `student_tesda_qualifications` WRITE;
/*!40000 ALTER TABLE `student_tesda_qualifications` DISABLE KEYS */;
INSERT INTO `student_tesda_qualifications` VALUES (4,'SR20260002',1,NULL,NULL,NULL,NULL),(5,'SR20260002',2,NULL,NULL,NULL,NULL),(6,'SR20260002',3,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `student_tesda_qualifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_uploads`
--

DROP TABLE IF EXISTS `student_uploads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_uploads` (
  `upload_id` int NOT NULL AUTO_INCREMENT,
  `student_id` varchar(20) NOT NULL,
  `kind` varchar(30) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `original_name` varchar(255) DEFAULT NULL,
  `mime_type` varchar(100) DEFAULT NULL,
  `size_bytes` bigint DEFAULT NULL,
  `uploaded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`upload_id`),
  KEY `student_uploads_ibfk_1` (`student_id`),
  CONSTRAINT `student_uploads_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_uploads`
--

LOCK TABLES `student_uploads` WRITE;
/*!40000 ALTER TABLE `student_uploads` DISABLE KEYS */;
INSERT INTO `student_uploads` VALUES (1,'SR20260001','ID_PHOTO','.\\uploads\\students\\SR20260001\\ID_PHOTO_e7a35cd0-daf4-44fb-8412-17af810732de.png','Gadgets Lab Logo PNG.png','image/png',517115,'2026-05-03 23:38:14'),(2,'SR20260002','ID_PHOTO','.\\uploads\\students\\SR20260002\\ID_PHOTO_0b955911-2b07-461c-9709-556f5bad428b.png','Gadgets Lab Logo PNG.png','image/png',517115,'2026-05-03 23:57:16');
/*!40000 ALTER TABLE `student_uploads` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subjects`
--

DROP TABLE IF EXISTS `subjects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subjects` (
  `subject_code` varchar(20) NOT NULL,
  `subject_name` varchar(255) NOT NULL,
  `qualification_code` int NOT NULL,
  `units` int NOT NULL,
  `trainer_id` int DEFAULT NULL,
  PRIMARY KEY (`subject_code`),
  KEY `qualification_code` (`qualification_code`),
  KEY `fk_subjects_trainer` (`trainer_id`),
  CONSTRAINT `fk_subjects_trainer` FOREIGN KEY (`trainer_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `subjects_ibfk_1` FOREIGN KEY (`qualification_code`) REFERENCES `qualifications` (`qualification_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subjects`
--

LOCK TABLES `subjects` WRITE;
/*!40000 ALTER TABLE `subjects` DISABLE KEYS */;
INSERT INTO `subjects` VALUES ('BPP-101','Bread Making Fundamentals',2,3,NULL),('BPP-102','Pastry Arts',2,4,NULL),('COOK-101','Introduction to Cookery',1,3,NULL),('COOK-102','Food Safety and Sanitation',1,3,NULL),('COOK-103','Prepare Hot Meals',1,5,NULL),('COOK-104','Prepare Cold Meals',1,4,NULL);
/*!40000 ALTER TABLE `subjects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_logs`
--

DROP TABLE IF EXISTS `system_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_logs` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `role` varchar(15) NOT NULL,
  `action` varchar(500) NOT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_system_logs_timestamp` (`timestamp` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_logs`
--

LOCK TABLES `system_logs` WRITE;
/*!40000 ALTER TABLE `system_logs` DISABLE KEYS */;
INSERT INTO `system_logs` VALUES (1,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 00:34:59'),(2,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 00:51:09'),(3,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 01:23:39'),(4,2,'admin','ROLE_ADMIN','Created new account: sean (ROLE_TRAINER)','0:0:0:0:0:0:0:1','2026-04-16 01:24:57'),(5,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 01:29:52'),(6,5,'sean','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 01:30:00'),(7,5,'sean','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-16 01:30:46'),(8,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 01:30:55'),(9,2,'admin','ROLE_ADMIN','Permanently deleted account: sean','0:0:0:0:0:0:0:1','2026-04-16 01:31:48'),(10,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-17 14:16:28'),(11,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-02 14:33:10'),(12,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-02 14:36:28'),(13,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-02 14:36:35'),(14,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-02 20:18:43'),(15,3,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-02 20:21:38'),(16,4,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-02 20:22:41'),(17,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-02 20:27:00'),(18,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-08 03:56:29'),(19,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-08 03:56:44'),(20,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-08 03:56:54'),(21,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-08 22:05:38'),(22,3,'registrar','ROLE_REGISTRAR','Updated student record: Ligan, Sean (ID: SR20260001)','0:0:0:0:0:0:0:1','2026-05-08 22:06:31'),(23,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 01:44:32'),(24,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 01:47:34'),(25,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 03:39:17'),(26,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 03:47:05'),(27,3,'registrar','ROLE_REGISTRAR','Created class: Bread Making Fundamentals in Section A 2026 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-09 03:47:37'),(28,3,'registrar','ROLE_REGISTRAR','Enrolled student STU-2026-001 in class #1','0:0:0:0:0:0:0:1','2026-05-09 03:47:42'),(29,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-16 13:20:59'),(30,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-16 13:21:03'),(31,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-16 13:21:07'),(32,4,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-21 21:37:47'),(33,4,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-21 21:38:40'),(34,4,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-21 21:38:45'),(35,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-21 21:38:49'),(36,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-21 23:01:45'),(37,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-21 23:02:02'),(38,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-21 23:02:17'),(39,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-21 23:50:42'),(40,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-21 23:53:10'),(41,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-21 23:53:14'),(42,3,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-21 23:55:21'),(43,4,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-21 23:55:24'),(44,4,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-22 15:04:01'),(45,4,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-22 15:05:53'),(46,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-22 15:06:09'),(47,4,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 13:15:32'),(48,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 20:59:06'),(49,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 21:01:44'),(50,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 21:02:00'),(51,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 21:07:54'),(52,2,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 21:07:58'),(53,2,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 21:08:21'),(54,3,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 21:08:25'),(55,3,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 21:16:27'),(56,4,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 21:16:37');
/*!40000 ALTER TABLE `system_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `lastname` varchar(255) NOT NULL,
  `firstname` varchar(255) NOT NULL,
  `middlename` varchar(255) NOT NULL,
  `birthdate` date NOT NULL DEFAULT '2000-01-01',
  `age` int NOT NULL,
  `email` varchar(255) NOT NULL,
  `role` varchar(15) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `password_changed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (2,'admin','$2a$10$tpS5eP5OWEwNyPhmee7ew.RtMM8p3baWuVJD9jy23wm/Z3oO8Q5Za','Admin','System','A','1990-01-01',36,'admin@anihan.local','ROLE_ADMIN',1,NULL),(3,'registrar','$2a$10$KXQuFI3KJVAkBc7N2N7ZFuNuRYkGh2XAcigreA31i6Wxs0twC9ZDm','Registrar','Default','R','1992-06-15',33,'registrar@anihan.local','ROLE_REGISTRAR',1,NULL),(4,'trainer','$2a$10$C72sa4zeOmYdg9TsH1U5H.2Rr0QajTFdfhXDt623dfurHmCSi9uTC','Trainer','Default','T','1995-03-20',31,'trainer@anihan.local','ROLE_TRAINER',1,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-27  7:18:50
