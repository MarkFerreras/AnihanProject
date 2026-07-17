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
INSERT INTO `batches` VALUES ('B2026A',2026);
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
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_enrollments`
--

LOCK TABLES `class_enrollments` WRITE;
/*!40000 ALTER TABLE `class_enrollments` DISABLE KEYS */;
INSERT INTO `class_enrollments` VALUES (4,3,'SR20260004','2026-05-13 23:29:47'),(14,4,'SR20260003','2026-05-23 06:32:25'),(15,5,'SR20260004','2026-05-23 06:34:16'),(16,6,'SR20260014','2026-05-23 14:56:04'),(17,6,'SR20260015','2026-05-23 15:13:11');
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (1,'TEST-T1','BPP-102',12,'2026','2026-05-10 16:46:12'),(2,'TEST-T1','COOK-101',12,'2026','2026-05-13 23:21:41'),(3,'TEST-T2','COOK-101',12,'2026','2026-05-13 23:27:08'),(4,'TEST-T3','COOK-102',15,'2026','2026-05-23 06:30:33'),(5,'TEST-T2','TEST-01',15,'2026','2026-05-23 06:34:08'),(6,'TEST-T4','CAP-111',17,'2026','2026-05-23 14:55:58');
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
INSERT INTO `courses` VALUES ('BPRO','Bread and Pastry Production'),('CARS','Culinary Arts and Restaurant Services'),('FSERV','Food and Beverage Services');
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
  KEY `student_id` (`student_id`),
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
  `final_grade` decimal(5,2) DEFAULT NULL,
  `re_exam_grade` decimal(5,2) DEFAULT NULL,
  `hours_studied` decimal(5,2) DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  `class_id` int DEFAULT NULL,
  `midterm_grade` decimal(5,2) DEFAULT NULL,
  `finals_grade` decimal(5,2) DEFAULT NULL,
  `locked` tinyint(1) NOT NULL DEFAULT '0',
  `locked_at` datetime DEFAULT NULL,
  PRIMARY KEY (`grade_id`),
  UNIQUE KEY `uq_grade_student_class` (`class_id`,`student_id`),
  KEY `student_id` (`student_id`),
  KEY `subject_code` (`subject_code`),
  CONSTRAINT `fk_grades_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE SET NULL,
  CONSTRAINT `grades_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`),
  CONSTRAINT `grades_ibfk_2` FOREIGN KEY (`subject_code`) REFERENCES `subjects` (`subject_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grades`
--

LOCK TABLES `grades` WRITE;
/*!40000 ALTER TABLE `grades` DISABLE KEYS */;
INSERT INTO `grades` VALUES (1,'SR20260007','BPP-102',2.60,NULL,NULL,NULL,1,2.00,3.00,0,NULL),(2,'SR20260004','COOK-101',2.48,NULL,NULL,NULL,3,1.40,3.20,0,NULL),(3,'SR20260014','CAP-111',1.70,NULL,NULL,'Passed',6,2.00,1.50,0,NULL),(4,'SR20260015','CAP-111',2.64,NULL,NULL,NULL,6,1.50,3.40,0,NULL);
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
  KEY `student_id` (`student_id`),
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
  KEY `student_id` (`student_id`),
  CONSTRAINT `parents_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parents`
--

LOCK TABLES `parents` WRITE;
/*!40000 ALTER TABLE `parents` DISABLE KEYS */;
INSERT INTO `parents` VALUES (3,'SR20260002','FATHER','dwada','dawda','dwada','2001-06-13','dwasdw',123124.00,'093482823','N/A','Village Taytay'),(4,'SR20260002','MOTHER','dwadwad','awdawd','awdaw','2026-04-08','dawdaw',1231231.00,'9876354','N/A','dwadwa'),(5,'SR20260004','FATHER','dwa','dwada','dwada','2026-04-21','dwa',31231.00,'+63 231 231 2312','awdawa','31231'),(6,'SR20260004','MOTHER','dawda','dwada','dawdaw','2026-04-14','dawdaw',13132.00,'+63 123 12','dwadaw','dwadadwa'),(8,'SR20260008','FATHER','Wong','Wowie','G.','1994-05-11','CEO',1500000.00,'+63 937 416 3274','N/A','Acacia Estates'),(9,'SR20260008','MOTHER','Villarel','JV','V.','1997-07-17','CEO',2500000.00,'+63 837 475 2374','N/A','Acacia Estates'),(10,'SR20260013','FATHER','fef','esf','dwad','2026-05-13','asdwa',3453.00,'+63 123 123 1','sdwa','sdwa'),(11,'SR20260013','MOTHER','was','dwas','dwasd','2026-05-12','dwadw',1231.00,'+63 131 231 231','dawd','sadwasda'),(12,'SR20260014','FATHER','Perkin','Sheldon','E.','1991-08-13','Worker',34623.00,'+63 934 773 4134','random@gmail.com','Pasig City'),(13,'SR20260014','MOTHER','Perkin','Sheila','D.','1994-09-12','Waiter',67535.00,'+63 673 457 3234','random@gmail.com','Pasig City'),(14,'SR20260015','FATHER','N/A','ads','aw','2026-05-21','dwas',12312.00,'+63 346 545 6','asdwa','sdwa'),(15,'SR20260015','MOTHER','asdw','asdsa','sda','2026-05-19','dasdwa',123123.00,'+63 465 522','sdwasdwasd','276567865546'),(16,'SR20260016','FATHER','Lipata','Joseph','T.','1997-07-16','Carpenter',25000.00,'+63 913 483 8423','random@gmail.com','Cainta, Rizal'),(17,'SR20260016','MOTHER','Lipata','Nikki','R.','1998-03-05','House Wife',0.00,'+63 887 463 4313','random2@gmail.com','Cainta, Rizal');
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
INSERT INTO `sections` VALUES ('TEST-T1','TEST-T1','B2026A','BPRO'),('TEST-T2','TEST-T2','B2026A','FSERV'),('TEST-T3','Apple','B2026A','CARS'),('TEST-T4','Gumamela','B2026A','CARS');
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
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_education`
--

LOCK TABLES `student_education` WRITE;
/*!40000 ALTER TABLE `student_education` DISABLE KEYS */;
INSERT INTO `student_education` VALUES (5,'SR20260002','Elementary',NULL,NULL,NULL,NULL,NULL),(6,'SR20260002','Secondary',NULL,NULL,NULL,NULL,NULL),(7,'SR20260002','Tertiary',NULL,NULL,NULL,NULL,NULL),(8,'SR20260002','Vocational',NULL,NULL,NULL,NULL,NULL),(9,'SR20260004','Elementary',NULL,NULL,NULL,NULL,NULL),(10,'SR20260004','Secondary',NULL,NULL,NULL,NULL,NULL),(11,'SR20260004','Tertiary',NULL,NULL,NULL,NULL,NULL),(12,'SR20260004','Vocational',NULL,NULL,NULL,NULL,NULL),(13,'SR20260005','Elementary',NULL,NULL,NULL,NULL,NULL),(14,'SR20260005','Secondary',NULL,NULL,NULL,NULL,NULL),(15,'SR20260005','Tertiary',NULL,NULL,NULL,NULL,NULL),(16,'SR20260005','Vocational',NULL,NULL,NULL,NULL,NULL),(21,'SR20260007','Elementary',NULL,NULL,NULL,NULL,NULL),(22,'SR20260007','Secondary',NULL,NULL,NULL,NULL,NULL),(23,'SR20260007','Tertiary',NULL,NULL,NULL,NULL,NULL),(24,'SR20260007','Vocational',NULL,NULL,NULL,NULL,NULL),(25,'SR20260008','Elementary',NULL,NULL,NULL,NULL,NULL),(26,'SR20260008','Secondary',NULL,NULL,NULL,NULL,NULL),(27,'SR20260008','Tertiary',NULL,NULL,NULL,NULL,NULL),(28,'SR20260008','Vocational',NULL,NULL,NULL,NULL,NULL),(29,'SR20260013','Elementary',NULL,NULL,NULL,NULL,NULL),(30,'SR20260013','Secondary',NULL,NULL,NULL,NULL,NULL),(31,'SR20260013','Tertiary',NULL,NULL,NULL,NULL,NULL),(32,'SR20260013','Vocational',NULL,NULL,NULL,NULL,NULL),(33,'SR20260014','Elementary','Young St. John','Taytay',NULL,NULL,'2005'),(34,'SR20260014','Secondary','Random1','Random1',NULL,NULL,'2008'),(35,'SR20260014','Tertiary','Random2','Random2',NULL,NULL,'2016'),(36,'SR20260014','Vocational',NULL,NULL,NULL,NULL,NULL),(37,'SR20260015','Elementary','asdwa','aasd',NULL,NULL,'12312'),(38,'SR20260015','Secondary','sda','asda',NULL,NULL,'12312'),(39,'SR20260015','Tertiary','asd','sadas',NULL,NULL,'123123'),(40,'SR20260015','Vocational',NULL,NULL,NULL,NULL,NULL),(41,'SR20260016','Elementary','test','Cainta',NULL,NULL,'2005'),(42,'SR20260016','Secondary',NULL,NULL,NULL,NULL,NULL),(43,'SR20260016','Tertiary',NULL,NULL,NULL,NULL,NULL),(44,'SR20260016','Vocational',NULL,NULL,NULL,NULL,NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_ojt`
--

LOCK TABLES `student_ojt` WRITE;
/*!40000 ALTER TABLE `student_ojt` DISABLE KEYS */;
INSERT INTO `student_ojt` VALUES (4,'SR20260005',NULL,NULL,NULL),(6,'SR20260007',NULL,NULL,NULL),(7,'SR20260008',NULL,NULL,NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_records`
--

LOCK TABLES `student_records` WRITE;
/*!40000 ALTER TABLE `student_records` DISABLE KEYS */;
INSERT INTO `student_records` VALUES (2,'SR20260002','Ferreras','Mark','E.','1999-06-16',26,'Male','Married','Village Taytay',NULL,NULL,'097473742843','Christian',1,'2003-06-12','Christ Church',NULL,NULL,NULL,'B2026A','CARS','TEST-T1',NULL,NULL,'Active'),(3,'SR20260003','Katana','Vivian','D','2002-06-13',23,'Female','Single','Metro Manila','Laguna','None','+6395674852','Catholic',1,'2005-01-05','Christ The Church',2,2,0,'B2026A','CARS','TEST-T3',NULL,NULL,'Active'),(4,'SR20260004','De Guzman','Kenneth','E.','2026-04-13',0,'Female',NULL,'dwadaw',NULL,NULL,'+63 213 123 1','dwadwad',1,'2026-04-21','dwad',NULL,NULL,NULL,'B2026A','BPRO','TEST-T2',NULL,NULL,'Active'),(5,'SR20260005','dwd','wdw','dw','2026-04-13',0,'Female',NULL,'wdadawdaw',NULL,NULL,'+63 123 131 231','dwada',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'TEST-T1',NULL,NULL,'Active'),(7,'SR20260007','dwadwa','dwadad','dwadwa','2026-03-31',0,'Female','Single','dasdwad',NULL,NULL,'+63 123 123 1','dwasdwa',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'TEST-T1',NULL,NULL,'Active'),(8,'SR20260008','Wong','Angelica','V.','2006-09-17',19,'Female','Single','Acacia Estates',NULL,NULL,'+63 973 784 6367','Catholic',1,'2006-10-21','Christ Church',1,1,NULL,NULL,NULL,'TEST-T1',NULL,NULL,'Active'),(9,'SR20260009','Wong','Angelica','V',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Enrolling'),(10,'SR20260010','Avellaneda','Keith','Avellano',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Enrolling'),(11,'SR20260011','Mark','Mark','Mark',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Enrolling'),(13,'SR20260013','fff','fff','fff','2026-05-05',0,'Male','Single','sfesfsef',NULL,NULL,'+63 131 423 4','fsefes',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'TEST-T1',NULL,NULL,'Active'),(14,'SR20260014','Perkin','Mazlo','P.','2003-02-12',23,'Male','Single','Pasig City',NULL,NULL,'+63 956 373 4234','Christian',1,'2004-01-21','Stella O',2,2,0,'B2026A',NULL,'TEST-T4',NULL,NULL,'Active'),(15,'SR20260015','Plankton','Plank','P.','2000-03-08',26,'Male','Single','Laguna, 3',NULL,NULL,'+63 837 462 7312','Catholic',1,'2001-02-07','Sanctuario',NULL,NULL,NULL,'B2026A','CARS','TEST-T4',NULL,NULL,'Active'),(16,'SR20260016','Lipata','Mike','R.','2005-03-03',21,'Male','Single','Caint, Rizal',NULL,NULL,'+63 978 465 3423','Catholic',0,NULL,NULL,2,2,NULL,'B2026A',NULL,NULL,NULL,NULL,'Submitted'),(17,'SR20260017','test125','test125','test125',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Enrolling');
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_school_years`
--

LOCK TABLES `student_school_years` WRITE;
/*!40000 ALTER TABLE `student_school_years` DISABLE KEYS */;
INSERT INTO `student_school_years` VALUES (4,'SR20260005',1,NULL,NULL,NULL,NULL,NULL),(6,'SR20260007',1,NULL,NULL,NULL,NULL,NULL),(7,'SR20260008',1,NULL,NULL,NULL,NULL,NULL),(8,'SR20260008',2,NULL,NULL,NULL,NULL,NULL),(10,'SR20260003',1,'2026','June','2028','December',NULL),(11,'SR20260002',1,'2026','June','2027','December',NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_tesda_qualifications`
--

LOCK TABLES `student_tesda_qualifications` WRITE;
/*!40000 ALTER TABLE `student_tesda_qualifications` DISABLE KEYS */;
INSERT INTO `student_tesda_qualifications` VALUES (10,'SR20260005',1,NULL,NULL,NULL,NULL),(11,'SR20260005',2,NULL,NULL,NULL,NULL),(12,'SR20260005',3,NULL,NULL,NULL,NULL),(16,'SR20260007',1,NULL,NULL,NULL,NULL),(17,'SR20260007',2,NULL,NULL,NULL,NULL),(18,'SR20260007',3,NULL,NULL,NULL,NULL),(19,'SR20260008',1,NULL,NULL,NULL,NULL),(20,'SR20260008',2,NULL,NULL,NULL,NULL),(21,'SR20260008',3,NULL,NULL,NULL,NULL),(22,'SR20260002',1,'Testing','Test Address','2027-06-16','Passed');
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
  KEY `student_id` (`student_id`),
  CONSTRAINT `student_uploads_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_uploads`
--

LOCK TABLES `student_uploads` WRITE;
/*!40000 ALTER TABLE `student_uploads` DISABLE KEYS */;
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
INSERT INTO `subjects` VALUES ('BPP-101','Bread Making Fundamentals',2,3,12),('BPP-102','Pastry Arts',2,4,12),('CAP-111','Capstone 1',2,4,17),('COOK-101','Introduction to Cookery',1,3,12),('COOK-102','Food Safety and Sanitation',1,3,12),('TEST-01','Computer Literacy',1,2,15);
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
) ENGINE=InnoDB AUTO_INCREMENT=265 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_logs`
--

LOCK TABLES `system_logs` WRITE;
/*!40000 ALTER TABLE `system_logs` DISABLE KEYS */;
INSERT INTO `system_logs` VALUES (1,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:05:37'),(2,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:06:04'),(3,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:06:18'),(4,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:24:49'),(5,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:25:09'),(6,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:26:02'),(7,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:26:09'),(8,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:26:12'),(9,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:26:23'),(10,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:26:26'),(11,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:26:31'),(12,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:33:43'),(13,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 17:01:36'),(14,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 09:17:37'),(15,10,'admin','ROLE_ADMIN','Created new account: Emmanuel (ROLE_ADMIN)','0:0:0:0:0:0:0:1','2026-04-16 09:19:13'),(16,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 09:19:25'),(17,13,'Emmanuel','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 09:19:33'),(18,13,'Emmanuel','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 09:19:39'),(19,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 09:19:45'),(20,10,'admin','ROLE_ADMIN','Permanently deleted account: Emmanuel','0:0:0:0:0:0:0:1','2026-04-16 09:19:56'),(21,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 09:20:05'),(22,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 09:20:22'),(23,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 11:40:08'),(24,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:00:14'),(25,12,'Prainer','ROLE_TRAINER','Changed own username from trainer to Prainer','0:0:0:0:0:0:0:1','2026-04-16 12:04:52'),(26,12,'Prainer','ROLE_TRAINER','Changed own password','0:0:0:0:0:0:0:1','2026-04-16 12:05:47'),(27,12,'Prainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:06:07'),(28,12,'trainer','ROLE_TRAINER','Changed own username from Prainer to trainer','0:0:0:0:0:0:0:1','2026-04-16 12:06:22'),(29,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:06:26'),(30,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:06:34'),(31,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:06:50'),(32,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:06:57'),(33,10,'admin','ROLE_ADMIN','Updated user details for: trainer','0:0:0:0:0:0:0:1','2026-04-16 12:07:31'),(34,10,'admin','ROLE_ADMIN','Reset password for: trainer','0:0:0:0:0:0:0:1','2026-04-16 12:07:31'),(35,10,'admin','ROLE_ADMIN','Deactivated account: trainer','0:0:0:0:0:0:0:1','2026-04-16 12:09:13'),(36,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:09:20'),(37,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:09:35'),(38,10,'admin','ROLE_ADMIN','Re-enabled account: trainer','0:0:0:0:0:0:0:1','2026-04-16 12:11:01'),(39,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:11:05'),(40,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:11:15'),(41,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:11:18'),(42,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:11:25'),(43,10,'admin','ROLE_ADMIN','Created new account: Sean (ROLE_ADMIN)','0:0:0:0:0:0:0:1','2026-04-16 12:23:13'),(44,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:23:23'),(45,14,'Sean','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:23:30'),(46,14,'Sean','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:54:38'),(47,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:54:46'),(48,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:54:59'),(49,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:55:06'),(50,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:55:32'),(51,14,'Sean','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:55:38'),(52,14,'Sean','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:57:52'),(53,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:58:00'),(54,10,'admin','ROLE_ADMIN','Permanently deleted account: Sean','0:0:0:0:0:0:0:1','2026-04-16 12:58:11'),(55,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:01:42'),(56,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:03:28'),(57,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:03:55'),(58,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:04:22'),(59,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:17:31'),(60,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:17:37'),(61,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:17:48'),(62,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:19:09'),(63,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:19:18'),(64,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:20:08'),(65,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:29:11'),(66,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:34:14'),(67,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:34:59'),(68,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:35:21'),(69,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:40:31'),(70,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:40:55'),(71,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 13:15:58'),(72,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 13:25:37'),(73,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 17:33:55'),(74,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-18 17:36:39'),(75,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 17:38:23'),(76,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 17:44:41'),(77,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-18 17:45:35'),(78,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 17:53:20'),(79,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-18 17:53:31'),(80,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-28 07:40:40'),(81,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-28 07:40:57'),(82,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-28 18:39:15'),(83,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-28 18:39:20'),(84,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-29 23:00:22'),(85,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-29 23:00:25'),(86,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 10:03:25'),(87,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 10:28:04'),(88,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 10:51:30'),(89,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 10:54:12'),(90,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:02:30'),(91,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject BPP-101','0:0:0:0:0:0:0:1','2026-05-09 13:02:54'),(92,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject BPP-102','0:0:0:0:0:0:0:1','2026-05-09 13:02:56'),(93,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject COOK-101','0:0:0:0:0:0:0:1','2026-05-09 13:03:27'),(94,11,'registrar','ROLE_REGISTRAR','Updated own personal details','0:0:0:0:0:0:0:1','2026-05-09 13:06:01'),(95,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:06:11'),(96,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:14:20'),(97,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:15:34'),(98,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:18:48'),(99,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:19:26'),(100,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:19:33'),(101,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:22:29'),(102,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:22:36'),(103,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:49:12'),(104,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:50:33'),(105,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 14:26:47'),(106,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 14:26:54'),(107,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-10 09:51:09'),(108,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-10 11:09:10'),(109,11,'registrar','ROLE_REGISTRAR','Created subject 001 (TestSubject)','0:0:0:0:0:0:0:1','2026-05-10 11:09:51'),(110,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject 001','0:0:0:0:0:0:0:1','2026-05-10 11:10:01'),(111,11,'registrar','ROLE_REGISTRAR','Updated subject 001','0:0:0:0:0:0:0:1','2026-05-10 11:10:08'),(112,11,'registrar','ROLE_REGISTRAR','Deleted subject 001','0:0:0:0:0:0:0:1','2026-05-10 11:10:15'),(113,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-10 11:13:14'),(114,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-10 11:13:21'),(115,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-10 16:17:40'),(116,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-10 16:45:26'),(117,11,'registrar','ROLE_REGISTRAR','Created section: TEST-T1 (TEST-T1)','0:0:0:0:0:0:0:1','2026-05-10 16:45:55'),(118,11,'registrar','ROLE_REGISTRAR','Created class: Pastry Arts in TEST-T1 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-10 16:46:12'),(119,11,'registrar','ROLE_REGISTRAR','Unassigned trainer from class #1 (BPP-102 / TEST-T1)','0:0:0:0:0:0:0:1','2026-05-10 16:46:35'),(120,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to class #1 (BPP-102 / TEST-T1)','0:0:0:0:0:0:0:1','2026-05-10 16:46:40'),(121,11,'registrar','ROLE_REGISTRAR','Updated student record: Ferreras, Mark (ID: SR20260002)','0:0:0:0:0:0:0:1','2026-05-10 16:48:06'),(122,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260002 in class #1','0:0:0:0:0:0:0:1','2026-05-10 16:48:26'),(123,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-13 16:22:40'),(124,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-13 23:20:10'),(125,11,'registrar','ROLE_REGISTRAR','Created class: Introduction to Cookery in TEST-T1 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-13 23:21:41'),(126,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260002 in class #2','0:0:0:0:0:0:0:1','2026-05-13 23:21:51'),(127,11,'registrar','ROLE_REGISTRAR','Created section: TEST-T2 (TEST-T2)','0:0:0:0:0:0:0:1','2026-05-13 23:26:26'),(128,11,'registrar','ROLE_REGISTRAR','Created class: Introduction to Cookery in TEST-T2 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-13 23:27:08'),(129,11,'registrar','ROLE_REGISTRAR','Removed enrollment #2','0:0:0:0:0:0:0:1','2026-05-13 23:27:46'),(130,11,'registrar','ROLE_REGISTRAR','Removed enrollment #1','0:0:0:0:0:0:0:1','2026-05-13 23:27:58'),(131,11,'registrar','ROLE_REGISTRAR','Updated student record: De Guzman, Kenneth (ID: SR20260004)','0:0:0:0:0:0:0:1','2026-05-13 23:29:09'),(132,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260002 in class #2','0:0:0:0:0:0:0:1','2026-05-13 23:29:39'),(133,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260004 in class #3','0:0:0:0:0:0:0:1','2026-05-13 23:29:47'),(134,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-15 15:17:27'),(135,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-15 15:20:00'),(136,11,'registrar','ROLE_REGISTRAR','Deleted subject COOK-104','0:0:0:0:0:0:0:1','2026-05-15 15:20:54'),(137,11,'registrar','ROLE_REGISTRAR','Deleted subject COOK-103','0:0:0:0:0:0:0:1','2026-05-15 15:21:01'),(138,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T1 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-15 15:21:27'),(139,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T1 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-15 15:21:29'),(140,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T1 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-15 15:21:29'),(141,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T1 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-15 15:21:30'),(142,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #2: 4 enrolled, 1 already enrolled, 0 ineligible','0:0:0:0:0:0:0:1','2026-05-15 15:21:52'),(143,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #1: 5 enrolled, 0 already enrolled, 0 ineligible','0:0:0:0:0:0:0:1','2026-05-15 15:22:01'),(144,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #3: 0 enrolled, 1 already enrolled, 0 ineligible','0:0:0:0:0:0:0:1','2026-05-15 15:22:12'),(145,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-15 15:22:29'),(146,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-18 16:28:35'),(147,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-18 22:49:37'),(148,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-18 23:20:30'),(149,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 07:30:10'),(150,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-19 07:44:02'),(151,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 11:13:45'),(152,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 12:06:52'),(153,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 12:53:04'),(154,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 13:54:37'),(155,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 14:06:32'),(156,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 14:39:54'),(157,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-19 14:40:23'),(158,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:17:15'),(159,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:18:34'),(160,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:19:39'),(161,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject COOK-102','0:0:0:0:0:0:0:1','2026-05-23 06:19:59'),(162,11,'registrar','ROLE_REGISTRAR','Deleted student record: Keith, Keith (ID: SR20260012)','0:0:0:0:0:0:0:1','2026-05-23 06:20:25'),(163,11,'registrar','ROLE_REGISTRAR','Updated student record: Katana, Vivian (ID: SR20260003)','0:0:0:0:0:0:0:1','2026-05-23 06:25:12'),(164,11,'registrar','ROLE_REGISTRAR','Created section: Apple (TEST-T3)','0:0:0:0:0:0:0:1','2026-05-23 06:25:47'),(165,11,'registrar','ROLE_REGISTRAR','Updated student record: Katana, Vivian (ID: SR20260003)','0:0:0:0:0:0:0:1','2026-05-23 06:26:08'),(166,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:27:04'),(167,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:27:16'),(168,10,'admin','ROLE_ADMIN','Created new account: trainer2 (ROLE_TRAINER)','0:0:0:0:0:0:0:1','2026-05-23 06:28:54'),(169,10,'admin','ROLE_ADMIN','Deactivated account: trainer2','0:0:0:0:0:0:0:1','2026-05-23 06:29:11'),(170,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:29:18'),(171,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:29:41'),(172,10,'admin','ROLE_ADMIN','Re-enabled account: trainer2','0:0:0:0:0:0:0:1','2026-05-23 06:29:46'),(173,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:29:50'),(174,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:29:57'),(175,11,'registrar','ROLE_REGISTRAR','Created class: Food Safety and Sanitation in Apple (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-23 06:30:33'),(176,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #4: 0 enrolled, 0 already enrolled, 1 ineligible','0:0:0:0:0:0:0:1','2026-05-23 06:30:56'),(177,11,'registrar','ROLE_REGISTRAR','Updated student record: Katana, Vivian (ID: SR20260003)','0:0:0:0:0:0:0:1','2026-05-23 06:32:11'),(178,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #4: 1 enrolled, 0 already enrolled, 0 ineligible','0:0:0:0:0:0:0:1','2026-05-23 06:32:25'),(179,11,'registrar','ROLE_REGISTRAR','Created subject TEST-01 (Computer Literacy)','0:0:0:0:0:0:0:1','2026-05-23 06:33:17'),(180,11,'registrar','ROLE_REGISTRAR','Assigned trainer Velasco, Alex to subject TEST-01','0:0:0:0:0:0:0:1','2026-05-23 06:33:33'),(181,11,'registrar','ROLE_REGISTRAR','Created class: Computer Literacy in TEST-T2 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-23 06:34:08'),(182,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260004 in class #5','0:0:0:0:0:0:0:1','2026-05-23 06:34:16'),(183,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:34:24'),(184,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:34:30'),(185,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:36:52'),(186,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 10:03:46'),(187,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 10:04:07'),(188,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 10:11:23'),(189,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 10:14:05'),(190,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 10:14:12'),(191,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:03:42'),(192,10,'admin','ROLE_ADMIN','Created new account: TestUser (ROLE_TRAINER)','0:0:0:0:0:0:0:1','2026-05-23 12:07:30'),(193,10,'admin','ROLE_ADMIN','Updated user details for: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:07:58'),(194,10,'admin','ROLE_ADMIN','Reset password for: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:07:58'),(195,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:08:09'),(196,16,'TestUser','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:08:48'),(197,16,'TestUser','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:09:06'),(198,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:27:32'),(199,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:30:40'),(200,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:32:29'),(201,10,'admin','ROLE_ADMIN','Deactivated account: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:34:57'),(202,10,'admin','ROLE_ADMIN','Re-enabled account: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:36:01'),(203,10,'admin','ROLE_ADMIN','Permanently deleted account: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:36:08'),(204,10,'admin','ROLE_ADMIN','Updated own personal details','0:0:0:0:0:0:0:1','2026-05-23 12:43:09'),(205,10,'New Admin','ROLE_ADMIN','Changed own username from admin to New Admin','0:0:0:0:0:0:0:1','2026-05-23 12:43:59'),(206,10,'admin','ROLE_ADMIN','Changed own username from New Admin to admin','0:0:0:0:0:0:0:1','2026-05-23 12:44:38'),(207,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:44:47'),(208,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:57:21'),(209,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:58:53'),(210,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:58:59'),(211,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:59:46'),(212,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:59:52'),(213,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 14:45:26'),(214,10,'admin','ROLE_ADMIN','Created new account: wilkins (ROLE_TRAINER)','0:0:0:0:0:0:0:1','2026-05-23 14:49:43'),(215,10,'admin','ROLE_ADMIN','Deactivated account: trainer2','0:0:0:0:0:0:0:1','2026-05-23 14:50:04'),(216,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 14:50:16'),(217,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 14:50:22'),(218,11,'registrar','ROLE_REGISTRAR','Created subject CAP-111 (Capstone 1)','0:0:0:0:0:0:0:1','2026-05-23 14:51:50'),(219,11,'registrar','ROLE_REGISTRAR','Assigned trainer Wilks, Mark to subject CAP-111','0:0:0:0:0:0:0:1','2026-05-23 14:52:21'),(220,11,'registrar','ROLE_REGISTRAR','Removed enrollment #9','0:0:0:0:0:0:0:1','2026-05-23 14:53:59'),(221,11,'registrar','ROLE_REGISTRAR','Removed enrollment #10','0:0:0:0:0:0:0:1','2026-05-23 14:54:00'),(222,11,'registrar','ROLE_REGISTRAR','Removed enrollment #11','0:0:0:0:0:0:0:1','2026-05-23 14:54:01'),(223,11,'registrar','ROLE_REGISTRAR','Removed enrollment #12','0:0:0:0:0:0:0:1','2026-05-23 14:54:01'),(224,11,'registrar','ROLE_REGISTRAR','Removed enrollment #13','0:0:0:0:0:0:0:1','2026-05-23 14:54:02'),(225,11,'registrar','ROLE_REGISTRAR','Removed enrollment #3','0:0:0:0:0:0:0:1','2026-05-23 14:54:04'),(226,11,'registrar','ROLE_REGISTRAR','Removed enrollment #5','0:0:0:0:0:0:0:1','2026-05-23 14:54:05'),(227,11,'registrar','ROLE_REGISTRAR','Removed enrollment #6','0:0:0:0:0:0:0:1','2026-05-23 14:54:06'),(228,11,'registrar','ROLE_REGISTRAR','Removed enrollment #7','0:0:0:0:0:0:0:1','2026-05-23 14:54:07'),(229,11,'registrar','ROLE_REGISTRAR','Removed enrollment #8','0:0:0:0:0:0:0:1','2026-05-23 14:54:07'),(230,11,'registrar','ROLE_REGISTRAR','Created section: Gumamela (TEST-T4)','0:0:0:0:0:0:0:1','2026-05-23 14:55:15'),(231,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T4 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-23 14:55:37'),(232,11,'registrar','ROLE_REGISTRAR','Created class: Capstone 1 in Gumamela (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-23 14:55:58'),(233,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260014 in class #6','0:0:0:0:0:0:0:1','2026-05-23 14:56:04'),(234,11,'registrar','ROLE_REGISTRAR','Deleted student record: dad, dad (ID: SR20260006)','0:0:0:0:0:0:0:1','2026-05-23 14:57:08'),(235,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 14:57:50'),(236,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 14:58:25'),(237,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 14:58:36'),(238,17,'wilkins','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 14:58:42'),(239,17,'wilkins','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 15:05:28'),(240,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 15:12:05'),(241,11,'registrar','ROLE_REGISTRAR','Updated student record: Plankton, Plank (ID: SR20260015)','0:0:0:0:0:0:0:1','2026-05-23 15:12:40'),(242,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260015 in class #6','0:0:0:0:0:0:0:1','2026-05-23 15:13:11'),(243,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 15:13:59'),(244,17,'wilkins','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 15:14:10'),(245,17,'wilkins','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 15:15:25'),(246,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 15:15:31'),(247,10,'admin','ROLE_ADMIN','Re-enabled account: trainer2','0:0:0:0:0:0:0:1','2026-05-23 15:15:35'),(248,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 15:16:12'),(249,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 15:16:19'),(250,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 17:50:38'),(251,11,'registrar','ROLE_REGISTRAR','Updated student record: Ferreras, Mark (ID: SR20260002)','0:0:0:0:0:0:0:1','2026-05-23 17:51:09'),(252,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 14:29:29'),(253,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 14:29:38'),(254,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 14:35:03'),(255,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 14:48:17'),(256,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 14:50:06'),(257,11,'registrar','ROLE_REGISTRAR','Deleted student record: Ferreras, Katch (ID: SR20260001)','0:0:0:0:0:0:0:1','2026-06-22 14:50:16'),(258,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 14:55:33'),(259,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 14:55:40'),(260,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 15:04:41'),(261,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 15:04:49'),(262,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 15:04:57'),(263,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 15:05:31'),(264,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 15:54:43');
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
  UNIQUE KEY `uq_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (10,'admin','$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6','Big','Cheese','Tiger','2004-01-01',22,'mark@example.com','ROLE_ADMIN',1,NULL),(11,'registrar','$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6','Registrar','Reg','B','1996-06-05',30,'reg@example.com','ROLE_REGISTRAR',1,NULL),(12,'trainer','$2a$10$IM6yEO9HpxmNE.cm2k1SUOGBkIDJwfjIwBg..ia8JgZIHULz.STHK','Trainer','Train','C','1985-10-10',40,'trainer@example.com','ROLE_TRAINER',1,'2026-04-16 12:07:31'),(15,'trainer2','$2a$10$zBGUpRTUhVsC4ieutm3oi.nsF0esYpIg/ABD4zaHZ88BgfK0D0Pr2','Velasco','Alex','E.','1993-11-24',32,'user@anihan.local','ROLE_TRAINER',1,NULL),(17,'wilkins','$2a$10$HPzOlw8I30AUKgZGMGIbWudvmwXJb/eVLf8XiUnRe30TJ/IlYD/pK','Wilks','Mark','F','1988-06-15',37,'wilks@gmail.com','ROLE_TRAINER',1,NULL);
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

-- Dump completed on 2026-07-14  0:25:26
