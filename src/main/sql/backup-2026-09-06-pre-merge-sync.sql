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
-- Current Database: `AnihanSRMS`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `AnihanSRMS` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `AnihanSRMS`;

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
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_enrollments`
--

LOCK TABLES `class_enrollments` WRITE;
/*!40000 ALTER TABLE `class_enrollments` DISABLE KEYS */;
INSERT INTO `class_enrollments` VALUES (4,3,'SR20260004','2026-05-13 23:29:47'),(14,4,'SR20260003','2026-05-23 06:32:25'),(15,5,'SR20260004','2026-05-23 06:34:16'),(16,6,'SR20260014','2026-05-23 14:56:04'),(17,6,'SR20260015','2026-05-23 15:13:11'),(18,7,'SR20260003','2026-07-24 14:40:31'),(19,8,'SR20260003','2026-07-24 14:40:44');
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (1,'TEST-T1','BPP-102',12,'2026','2026-05-10 16:46:12'),(2,'TEST-T1','COOK-101',12,'2026','2026-05-13 23:21:41'),(3,'TEST-T2','COOK-101',12,'2026','2026-05-13 23:27:08'),(4,'TEST-T3','COOK-102',15,'2026','2026-05-23 06:30:33'),(5,'TEST-T2','TEST-01',15,'2026','2026-05-23 06:34:08'),(6,'TEST-T4','CAP-111',17,'2026','2026-05-23 14:55:58'),(7,'TEST-T3','BPP-102',15,'2026','2026-07-24 14:29:23'),(8,'TEST-T3','BPP-101',17,'2026','2026-07-24 14:40:01');
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT INTO `documents` VALUES (7,'SR20260002','Transcript of Records (TOR)','SR20260002-TOR.html','text/html',45262,_binary '<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<title>Transcript of Records (TOR) — SR20260002</title>\n<style>\n/*\r\n * document-print.css — layout for generated official documents (TOR / Form IX).\r\n * Used live on generate-document.html and embedded inline into saved HTML\r\n * documents so they stay self-contained when viewed or reprinted later.\r\n */\r\n\r\n.document-sheet {\r\n    background: #fff;\r\n    color: #000;\r\n    font-family: \"Times New Roman\", Georgia, serif;\r\n    font-size: 11px;\r\n    line-height: 1.35;\r\n    width: 8.27in;\r\n    max-width: 100%;\r\n    margin: 0 auto;\r\n    padding: 0.6in 0.55in;\r\n    box-sizing: border-box;\r\n}\r\n\r\n.document-sheet * {\r\n    box-sizing: border-box;\r\n}\r\n\r\n/* ----- School header ----- */\r\n.doc-school-header {\r\n    display: flex;\r\n    align-items: center;\r\n    justify-content: center;\r\n    gap: 14px;\r\n    text-align: center;\r\n    margin-bottom: 14px;\r\n}\r\n\r\n.doc-school-logo {\r\n    flex: 0 0 auto;\r\n    width: 62px;\r\n    height: auto;\r\n}\r\n\r\n.doc-school-name {\r\n    font-size: 22px;\r\n    font-style: italic;\r\n    font-weight: bold;\r\n    margin: 0;\r\n}\r\n\r\n.doc-school-sub {\r\n    font-size: 10px;\r\n    margin: 0;\r\n}\r\n\r\n.doc-form-label {\r\n    text-align: right;\r\n    font-weight: bold;\r\n    font-size: 11px;\r\n    margin: 4px 0;\r\n}\r\n\r\n.doc-title {\r\n    text-align: center;\r\n    font-weight: bold;\r\n    letter-spacing: 2px;\r\n    margin: 10px 0 14px;\r\n    font-size: 13px;\r\n}\r\n\r\n.doc-title .doc-subtitle {\r\n    display: block;\r\n    letter-spacing: 3px;\r\n}\r\n\r\n/* ----- Header field rows ----- */\r\n.doc-fields {\r\n    width: 100%;\r\n    border-bottom: 2px solid #000;\r\n    padding-bottom: 6px;\r\n    margin-bottom: 10px;\r\n}\r\n\r\n.doc-field-row {\r\n    display: flex;\r\n    gap: 14px;\r\n    margin-bottom: 3px;\r\n}\r\n\r\n.doc-field {\r\n    display: flex;\r\n    flex: 1;\r\n    align-items: flex-end;\r\n    min-width: 0;\r\n}\r\n\r\n.doc-field-label {\r\n    white-space: nowrap;\r\n    padding-right: 4px;\r\n}\r\n\r\n.doc-field .fill {\r\n    flex: 1;\r\n    border-bottom: 1px solid #000;\r\n    min-height: 14px;\r\n    padding: 0 3px;\r\n    font-weight: 600;\r\n}\r\n\r\n.doc-field .fill.static-value {\r\n    font-weight: 600;\r\n}\r\n\r\n.doc-field.narrow { flex: 0 0 auto; min-width: 130px; }\r\n.doc-field.wide { flex: 2; }\r\n\r\n/* ----- Fillable spans ----- */\r\n.fill {\r\n    display: inline-block;\r\n    min-width: 30px;\r\n    outline: none;\r\n}\r\n\r\n.fill:empty::before {\r\n    content: \'\\00a0\';\r\n}\r\n\r\n.document-toolbar-active .fill[contenteditable=\"true\"]:hover,\r\n.document-toolbar-active .fill[contenteditable=\"true\"]:focus {\r\n    background: #fff8d6;\r\n}\r\n\r\n/* ----- Semester heading ----- */\r\n.doc-semester {\r\n    font-weight: bold;\r\n    text-decoration: underline;\r\n    margin: 12px 0 6px;\r\n}\r\n\r\n/* ----- Subjects table ----- */\r\n.doc-subjects-table {\r\n    width: 100%;\r\n    border-collapse: collapse;\r\n    margin-bottom: 8px;\r\n}\r\n\r\n.doc-subjects-table th,\r\n.doc-subjects-table td {\r\n    border: 1px dotted #666;\r\n    padding: 1px 4px;\r\n    vertical-align: bottom;\r\n}\r\n\r\n.doc-subjects-table thead th {\r\n    border: 1px solid #000;\r\n    text-align: center;\r\n    font-size: 10px;\r\n    padding: 2px 4px;\r\n}\r\n\r\n.doc-subjects-table .col-code { width: 12%; text-align: center; }\r\n.doc-subjects-table .col-grade { width: 7%; text-align: center; }\r\n.doc-subjects-table .col-hours { width: 7%; text-align: right; }\r\n.doc-subjects-table .col-units { width: 7%; text-align: right; }\r\n.doc-subjects-table .col-remarks { width: 11%; text-align: center; }\r\n\r\n.doc-subjects-table .section-row td {\r\n    font-weight: bold;\r\n    border-left: 1px dotted #666;\r\n}\r\n\r\n.doc-subjects-table .total-row td {\r\n    font-weight: bold;\r\n    text-align: right;\r\n}\r\n\r\n/* ----- End of transcript ----- */\r\n.doc-end-line {\r\n    text-align: center;\r\n    font-size: 9px;\r\n    letter-spacing: 1px;\r\n    margin: 6px 0;\r\n    word-break: break-all;\r\n}\r\n\r\n/* ----- Notes + grading legend ----- */\r\n.doc-notes {\r\n    font-size: 9px;\r\n    margin: 8px 0;\r\n}\r\n\r\n.doc-notes p { margin: 0 0 2px; }\r\n\r\n.doc-grading-wrap {\r\n    display: flex;\r\n    gap: 12px;\r\n    align-items: center;\r\n    margin: 10px 0;\r\n}\r\n\r\n.doc-seal-note {\r\n    flex: 0 0 110px;\r\n    text-align: center;\r\n    font-size: 9px;\r\n    color: #444;\r\n}\r\n\r\n.doc-grading-table {\r\n    flex: 1;\r\n    border-collapse: collapse;\r\n    font-size: 9px;\r\n}\r\n\r\n.doc-grading-table caption {\r\n    caption-side: top;\r\n    text-align: center;\r\n    font-weight: bold;\r\n    letter-spacing: 3px;\r\n    font-size: 10px;\r\n    color: #000;\r\n}\r\n\r\n.doc-grading-table td {\r\n    border: 1px dotted #666;\r\n    padding: 1px 4px;\r\n}\r\n\r\n.doc-grading-table .competent-cell {\r\n    border: 1px solid #000;\r\n    text-align: center;\r\n    vertical-align: middle;\r\n    width: 70px;\r\n}\r\n\r\n/* ----- Certification ----- */\r\n.doc-certification {\r\n    margin: 14px 0;\r\n    text-align: justify;\r\n}\r\n\r\n.doc-certification .cert-heading {\r\n    letter-spacing: 3px;\r\n    font-weight: bold;\r\n}\r\n\r\n.doc-certification .fill {\r\n    border-bottom: 1px solid #000;\r\n    padding: 0 4px;\r\n    font-weight: 600;\r\n}\r\n\r\n/* ----- Signatories ----- */\r\n.doc-signatories {\r\n    display: flex;\r\n    justify-content: space-between;\r\n    margin-top: 22px;\r\n    gap: 40px;\r\n}\r\n\r\n.doc-signatory {\r\n    flex: 1;\r\n    text-align: center;\r\n}\r\n\r\n.doc-signatory .sig-caption {\r\n    text-align: left;\r\n    margin-bottom: 28px;\r\n}\r\n\r\n.doc-signatory .sig-name {\r\n    border-top: 1px solid #000;\r\n    display: inline-block;\r\n    min-width: 220px;\r\n    padding-top: 2px;\r\n    font-weight: bold;\r\n}\r\n\r\n.doc-signatory .sig-role {\r\n    margin-top: 0;\r\n}\r\n\r\n.doc-date-line {\r\n    text-align: right;\r\n    margin: 10px 0;\r\n}\r\n\r\n/* ----- Footer contact line ----- */\r\n.doc-contact-footer {\r\n    text-align: center;\r\n    font-size: 9px;\r\n    margin-top: 18px;\r\n    border-top: 1px solid #000;\r\n    padding-top: 4px;\r\n}\r\n\r\n/* ----- Page breaks between sheets ----- */\r\n.doc-page-break {\r\n    page-break-before: always;\r\n}\r\n\r\n/* ----- Print rules ----- */\r\n@page {\r\n    size: A4 portrait;\r\n    margin: 9mm 11mm;\r\n}\r\n\r\n@media print {\r\n    /* Only the white document content prints — no grey app backdrop, no chrome.\r\n       body must not stay a flex container: Chromium cannot fragment flex items\r\n       across pages, which pushed whole documents onto a second page. */\r\n    html,\r\n    body {\r\n        display: block !important;\r\n        min-height: 0 !important;\r\n        height: auto !important;\r\n        background: #fff !important;\r\n        margin: 0 !important;\r\n        padding: 0 !important;\r\n    }\r\n\r\n    body.generate-document-page > *:not(#documentArea) {\r\n        display: none !important;\r\n    }\r\n\r\n    #documentArea {\r\n        margin: 0 !important;\r\n        padding: 0 !important;\r\n    }\r\n\r\n    .document-sheet {\r\n        width: 100%;\r\n        padding: 0;\r\n        box-shadow: none !important;\r\n        font-size: 10px;\r\n        line-height: 1.25;\r\n    }\r\n\r\n    .fill[contenteditable=\"true\"] {\r\n        background: transparent !important;\r\n    }\r\n\r\n    /* Tighter vertical rhythm so 1-page templates stay on one page. */\r\n    .doc-school-header { margin-bottom: 8px; }\r\n    .doc-title { margin: 6px 0 8px; }\r\n    .doc-grading-wrap { margin: 6px 0; }\r\n    .doc-certification { margin: 10px 0; }\r\n    .doc-signatories { margin-top: 14px; }\r\n    .doc-contact-footer { margin-top: 10px; }\r\n\r\n    /* No table row or trailing block may straddle a page break. */\r\n    .doc-subjects-table tr,\r\n    .doc-grading-wrap,\r\n    .doc-signatories,\r\n    .doc-certification,\r\n    .doc-contact-footer {\r\n        page-break-inside: avoid;\r\n    }\r\n\r\n    .doc-subjects-table thead {\r\n        display: table-header-group;\r\n    }\r\n}\n</style>\n</head>\n<body>\n<div class=\"document-sheet\"><div class=\"doc-school-header\"><img class=\"doc-school-logo\" src=\"data:image/png;base64,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\" alt=\"Anihan Technical School logo\"><div><p class=\"doc-school-name\">Anihan Technical School</p><p class=\"doc-school-sub\">(For Women-in-Development)</p><p class=\"doc-school-sub\">A project of the Foundation for Professional Training, Inc. (FPTI)</p></div></div><div class=\"doc-title\">O F F I C E&nbsp;&nbsp;O F&nbsp;&nbsp;T H E&nbsp;&nbsp;R E G I S T R A R<span class=\"doc-subtitle\">O F F I C I A L&nbsp;&nbsp;T R A N S C R I P T&nbsp;&nbsp;O F&nbsp;&nbsp;R E C O R D S</span></div><div class=\"doc-fields\"><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Name</span><span class=\"fill\">Ferreras, Mark E.</span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Address</span><span class=\"fill\">Summerfield Village, Taytay Rizal</span></div></div><div class=\"doc-field-row\"><div class=\"doc-field \"><span class=\"doc-field-label\">Date of Admission</span><span class=\"fill\">02/18/2026</span></div><div class=\"doc-field narrow\"><span class=\"doc-field-label\">Sex</span><span class=\"fill\">Male</span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Birthday</span><span class=\"fill\">January 1, 2004</span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Student No.</span><span class=\"fill\">SR20260002</span></div></div><div class=\"doc-field-row\"><div class=\"doc-field \"><span class=\"doc-field-label\">Entrance Date</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Hon. Dismissal</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Graduation</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Entrance Data</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">SY Ended</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Course Title</span><span class=\"fill\">Culinary Arts and Restaurant Services</span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Certificate No.</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field\"><span class=\"doc-field-label\">TESDA Assessment</span><span class=\"fill static-value\"><u><strong>Bread and Pastry Production-NC II</strong></u></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Date Taken</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Result</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Special Order No.</span><span class=\"fill\"></span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Issued</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field\"><span class=\"doc-field-label\">TESDA Assessment</span><span class=\"fill static-value\"><u><strong>Food and Beverage Services-NC II</strong></u></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Date Taken</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Result</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Special Order No.</span><span class=\"fill\"></span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Issued</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field\"><span class=\"doc-field-label\">TESDA Assessment</span><span class=\"fill static-value\"><u><strong>Cookery -NC II</strong></u></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Date Taken</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Result</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Special Order No.</span><span class=\"fill\"></span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Issued</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Remarks</span><span class=\"fill\"></span></div></div></div><p class=\"doc-semester\"><span class=\"fill\">SY 2026-2027, First Semester</span></p><table class=\"doc-subjects-table\"><thead><tr><th rowspan=\"2\" class=\"col-code\">SUBJECT CODE</th><th rowspan=\"2\">SUBJECT TITLE</th><th colspan=\"2\">GRADES</th><th rowspan=\"2\" class=\"col-hours\">HOURS</th><th rowspan=\"2\" class=\"col-units\">UNITS</th></tr><tr><th class=\"col-grade\">FINAL</th><th class=\"col-grade\">RE-EXAM</th></tr></thead><tbody><tr class=\"section-row\"><td></td><td colspan=\"5\">BASIC COMPETENCIES:</td></tr><tr><td class=\"col-code\">400311210</td><td>Participating in Workplace Communication</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311211</td><td>Working in a Team Environment</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311212</td><td>Solving and Addressing General Workplace Problems</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311213</td><td>Developing Career and Life Decisions</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311214</td><td>Contributing to Workplace Innovation</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311215</td><td>Presenting Relevant Information</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311216</td><td>Practicing Occupational Safety and Health Policies and Procedures</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311217</td><td>Exercising Efficient and Effective Sustainable Practices in the Workplace</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311218</td><td>Practicing Entrepreneurial Skills in the Workplace</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">COMMON COMPETENCIES:</td></tr><tr><td class=\"col-code\">TRS311201</td><td>Developing and Updating Industry Knowledge</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">4.5</td><td class=\"col-units\">0.25</td></tr><tr><td class=\"col-code\">TRS311202</td><td>Observing Workplace Hygiene Procedures</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">4.5</td><td class=\"col-units\">0.25</td></tr><tr><td class=\"col-code\">TRS311203</td><td>Performing Computer Operations</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS311204</td><td>Performing Workplace and Safety Practices</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">4.5</td><td class=\"col-units\">0.25</td></tr><tr><td class=\"col-code\">TRS311205</td><td>Providing Effective Customer Service</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">4.5</td><td class=\"col-units\">0.25</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">CORE COMPETENCIES-BREAD and PASTRY PRODUCTION NC II:</td></tr><tr><td class=\"col-code\">TRS741342</td><td>Preparing and Presenting Gateaux, Tortes and Cakes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">38</td><td class=\"col-units\">2.11</td></tr><tr><td class=\"col-code\">TRS741343</td><td>Presenting Desserts</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">10</td><td class=\"col-units\">0.56</td></tr><tr><td class=\"col-code\">TRS741344</td><td>Preparing and Displaying Petit Fours</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">10</td><td class=\"col-units\">0.56</td></tr><tr><td class=\"col-code\">TRS741379</td><td>Preparing and Producing Bakery Products</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">38</td><td class=\"col-units\">2.11</td></tr><tr><td class=\"col-code\">TRS741380</td><td>Preparing and Producing Pastry Products</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">40</td><td class=\"col-units\">2.22</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">CORE COMPETENCIES - FOOD AND BEVERAGE SERVICES NC II:</td></tr><tr><td class=\"col-code\">TRS512387</td><td>Preparing the Dining Room/Restaurant Area for Service</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">63</td><td class=\"col-units\">3.50</td></tr><tr><td class=\"col-code\">TRS512388</td><td>Welcoming Guests and Take Food and Beverage Orders</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">60</td><td class=\"col-units\">3.33</td></tr><tr><td class=\"col-code\">TRS512389</td><td>Promoting Food and Beverage Products</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">26</td><td class=\"col-units\">1.44</td></tr><tr><td class=\"col-code\">TRS512390</td><td>Providing Food and Beverage Services to Guests</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">60</td><td class=\"col-units\">3.33</td></tr><tr><td class=\"col-code\">TRS512391</td><td>Providing Room Service</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">60</td><td class=\"col-units\">3.33</td></tr><tr><td class=\"col-code\">TRS512392</td><td>Receiving and Handling Guest Concerns</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">27</td><td class=\"col-units\">1.50</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">CORE COMPETENCIES-COOKERY NC II:</td></tr><tr><td class=\"col-code\">TRS512328</td><td>Cleaning and Maintaining Kitchen Premises</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">13</td><td class=\"col-units\">0.72</td></tr><tr><td class=\"col-code\">TRS512330</td><td>Preparing Sandwiches</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">12</td><td class=\"col-units\">0.67</td></tr><tr><td class=\"col-code\">TRS512331</td><td>Preparing Stocks, Sauces, and Soups</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS512333</td><td>Preparing Poultry and Game Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS512334</td><td>Preparing Seafood Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">27</td><td class=\"col-units\">1.50</td></tr><tr><td class=\"col-code\">TRS512335</td><td>Preparing Desserts</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS512340</td><td>Packaging Prepared Food</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">9</td><td class=\"col-units\">0.50</td></tr><tr><td class=\"col-code\">TRS512381</td><td>Preparing Appetizers</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">12</td><td class=\"col-units\">0.67</td></tr><tr><td class=\"col-code\">TRS512382</td><td>Preparing Salads and Dressings</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">12</td><td class=\"col-units\">0.67</td></tr><tr><td class=\"col-code\">TRS512383</td><td>Preparing Meat Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">27</td><td class=\"col-units\">1.50</td></tr><tr><td class=\"col-code\">TRS512384</td><td>Preparing Vegetable Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">36</td><td class=\"col-units\">2.00</td></tr><tr><td class=\"col-code\">TRS512385</td><td>Preparing Egg Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS512386</td><td>Preparing Starch Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">36</td><td class=\"col-units\">2.00</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">OTHER SUBJECTS</td></tr><tr><td class=\"col-code\">BFS</td><td>Basic Food Safety</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">ENG</td><td>English</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">ENTREP</td><td>Entrepreneurship</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">MATH</td><td>Culinary Math</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">CL 1</td><td>Christian Living 1</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">CL 2</td><td>Christian Living 2</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">PD</td><td>Personality Development 1</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">VE 1</td><td>Values Education 1</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr></tbody></table><p class=\"doc-semester\"><span class=\"fill\">SY 2026-2027, Second Semester</span></p><table class=\"doc-subjects-table\"><thead><tr><th rowspan=\"2\" class=\"col-code\">SUBJECT CODE</th><th rowspan=\"2\">SUBJECT TITLE</th><th colspan=\"2\">GRADES</th><th rowspan=\"2\" class=\"col-hours\">HOURS</th><th rowspan=\"2\" class=\"col-units\">UNITS</th></tr><tr><th class=\"col-grade\">FINAL</th><th class=\"col-grade\">RE-EXAM</th></tr></thead><tbody><tr class=\"section-row\"><td></td><td colspan=\"5\">OTHER SUBJECTS</td></tr><tr><td class=\"col-code\">OJT</td><td>On-the-Job-Training</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">900</td><td class=\"col-units\">50.00</td></tr><tr><td class=\"col-code\">CL 3</td><td>Christian Living 3</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">CL 4</td><td>Christian Living 4</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">VE 2</td><td>Values Education 2</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr></tbody></table><p class=\"doc-end-line\">X X X X X X X X X X X X X X X X X END OF TRANSCRIPT X X X X X X X X X X X X X X X X X</p><div class=\"doc-notes\"><p><strong>CREDITS:</strong> One unit credit is one hour lecture, laboratory or practicum / OJT per week for a period of a complete semester.</p><p><strong>NOTE:</strong> Any erasure or alteration on this record invalidates the whole transcript.</p></div><div class=\"doc-grading-wrap\"><div class=\"doc-seal-note\">Not Valid<br>Without School<br>Dry Seal<br><span class=\"fill\">July 14, 2026</span></div><table class=\"doc-grading-table\"><caption>G R A D I N G&nbsp;&nbsp;S Y S T E M</caption><tbody><tr><td>99-100</td><td>1.00</td><td>Excellent</td><td class=\"competent-cell\" rowspan=\"8\">Competent</td><td>75-77</td><td>3.00</td><td>Passed</td><td class=\"competent-cell\" rowspan=\"2\">Competent</td></tr><tr><td>96-98</td><td>1.25</td><td>Very Good</td><td>C</td><td></td><td>Complete</td></tr><tr><td>93-95</td><td>1.50</td><td>Very Good</td><td>70-74</td><td>4.00</td><td>Conditional Failure</td><td class=\"competent-cell\" rowspan=\"5\">Not Competent</td></tr><tr><td>90-92</td><td>1.75</td><td>Good</td><td>Below 70</td><td>5.00</td><td>Failure</td></tr><tr><td>87-89</td><td>2.00</td><td>Good</td><td>FA</td><td></td><td>Failure Due to Absences</td></tr><tr><td>84-86</td><td>2.25</td><td>Good</td><td>INC</td><td></td><td>Incomplete</td></tr><tr><td>81-83</td><td>2.50</td><td>Satisfactory</td><td>D</td><td></td><td>Dropped</td></tr><tr><td>78-80</td><td>2.75</td><td>Satisfactory</td><td></td><td></td><td></td><td></td></tr></tbody></table></div><div class=\"doc-signatories\"><div class=\"doc-signatory\"><p class=\"sig-caption\">Prepared by:</p><span class=\"sig-name fill\">MELESINE B. VELASCO</span><p class=\"sig-role\">Assistant to the Registrar</p></div><div class=\"doc-signatory\"><p class=\"sig-caption\">Approved by:</p><span class=\"sig-name fill\">HERMINIA F. GABUTINA</span><p class=\"sig-role\">Registrar</p></div></div><div class=\"doc-contact-footer\">Email: info@anihan.edu.ph / anihanschool@gmail.com / registrar@anihan.edu.ph * Web: anihan.edu.ph<br>294 Purok 6 Brgy. Milagrosa, Calamba City, Laguna 4027 Philippines * (049) 545-1598 * 0939-2666108</div></div>\n</body>\n</html>\n','2026-07-14 14:25:47');
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
INSERT INTO `student_records` VALUES (2,'SR20260002','Ferreras','Mark','E.','1999-06-16',26,'Male','Married','Village Taytay',NULL,NULL,'097473742843','Christian',1,'2003-06-12','Christ Church',NULL,NULL,NULL,'B2026A','CARS','TEST-T1',NULL,NULL,'Active'),(3,'SR20260003','Katana','Vivian','D','2002-06-13',23,'Female','Single','Metro Manila','Laguna','None','+6395674852','Catholic',1,'2005-01-05','Christ The Church',2,2,0,'B2026A','CARS','TEST-T3',NULL,NULL,'Active'),(4,'SR20260004','De Guzman','Kenneth','E.','2026-04-13',0,'Female',NULL,'dwadaw',NULL,NULL,'+63 213 123 1','dwadwad',1,'2026-04-21','dwad',NULL,NULL,NULL,'B2026A','BPRO','TEST-T2',NULL,NULL,'Active'),(5,'SR20260005','dwd','wdw','dw','2026-04-13',0,'Female',NULL,'wdadawdaw',NULL,NULL,'+63 123 131 231','dwada',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'TEST-T1',NULL,NULL,'Active'),(7,'SR20260007','dwadwa','dwadad','dwadwa','2026-03-31',0,'Female','Single','dasdwad',NULL,NULL,'+63 123 123 1','dwasdwa',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'TEST-T1',NULL,NULL,'Active'),(8,'SR20260008','Wong','Angelica','V.','2006-09-17',19,'Female','Single','Acacia Estates',NULL,NULL,'+63 973 784 6367','Catholic',1,'2006-10-21','Christ Church',1,1,NULL,NULL,NULL,'TEST-T1',NULL,NULL,'Active'),(13,'SR20260013','fff','fff','fff','2026-05-05',0,'Male','Single','sfesfsef',NULL,NULL,'+63 131 423 4','fsefes',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'TEST-T1',NULL,NULL,'Active'),(14,'SR20260014','Perkin','Mazlo','P.','2003-02-12',23,'Male','Single','Pasig City',NULL,NULL,'+63 956 373 4234','Christian',1,'2004-01-21','Stella O',2,2,0,'B2026A',NULL,'TEST-T4',NULL,NULL,'Active'),(15,'SR20260015','Plankton','Plank','P.','2000-03-08',26,'Male','Single','Laguna, 3',NULL,NULL,'+63 837 462 7312','Catholic',1,'2001-02-07','Sanctuario',NULL,NULL,NULL,'B2026A','CARS','TEST-T4',NULL,NULL,'Active'),(16,'SR20260016','Lipata','Mike','R.','2005-03-03',21,'Male','Single','Caint, Rizal',NULL,NULL,'+63 978 465 3423','Catholic',0,NULL,NULL,2,2,NULL,'B2026A',NULL,NULL,NULL,NULL,'Submitted');
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
) ENGINE=InnoDB AUTO_INCREMENT=324 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_logs`
--

LOCK TABLES `system_logs` WRITE;
/*!40000 ALTER TABLE `system_logs` DISABLE KEYS */;
INSERT INTO `system_logs` VALUES (1,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:05:37'),(2,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:06:04'),(3,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:06:18'),(4,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:24:49'),(5,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:25:09'),(6,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:26:02'),(7,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:26:09'),(8,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:26:12'),(9,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:26:23'),(10,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:26:26'),(11,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 16:26:31'),(12,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-14 16:33:43'),(13,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-14 17:01:36'),(14,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 09:17:37'),(15,10,'admin','ROLE_ADMIN','Created new account: Emmanuel (ROLE_ADMIN)','0:0:0:0:0:0:0:1','2026-04-16 09:19:13'),(16,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 09:19:25'),(17,13,'Emmanuel','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 09:19:33'),(18,13,'Emmanuel','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 09:19:39'),(19,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 09:19:45'),(20,10,'admin','ROLE_ADMIN','Permanently deleted account: Emmanuel','0:0:0:0:0:0:0:1','2026-04-16 09:19:56'),(21,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 09:20:05'),(22,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 09:20:22'),(23,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 11:40:08'),(24,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:00:14'),(25,12,'Prainer','ROLE_TRAINER','Changed own username from trainer to Prainer','0:0:0:0:0:0:0:1','2026-04-16 12:04:52'),(26,12,'Prainer','ROLE_TRAINER','Changed own password','0:0:0:0:0:0:0:1','2026-04-16 12:05:47'),(27,12,'Prainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:06:07'),(28,12,'trainer','ROLE_TRAINER','Changed own username from Prainer to trainer','0:0:0:0:0:0:0:1','2026-04-16 12:06:22'),(29,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:06:26'),(30,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:06:34'),(31,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:06:50'),(32,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:06:57'),(33,10,'admin','ROLE_ADMIN','Updated user details for: trainer','0:0:0:0:0:0:0:1','2026-04-16 12:07:31'),(34,10,'admin','ROLE_ADMIN','Reset password for: trainer','0:0:0:0:0:0:0:1','2026-04-16 12:07:31'),(35,10,'admin','ROLE_ADMIN','Deactivated account: trainer','0:0:0:0:0:0:0:1','2026-04-16 12:09:13'),(36,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:09:20'),(37,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:09:35'),(38,10,'admin','ROLE_ADMIN','Re-enabled account: trainer','0:0:0:0:0:0:0:1','2026-04-16 12:11:01'),(39,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:11:05'),(40,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:11:15'),(41,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:11:18'),(42,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:11:25'),(43,10,'admin','ROLE_ADMIN','Created new account: Sean (ROLE_ADMIN)','0:0:0:0:0:0:0:1','2026-04-16 12:23:13'),(44,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:23:23'),(45,14,'Sean','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:23:30'),(46,14,'Sean','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:54:38'),(47,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:54:46'),(48,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:54:59'),(49,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:55:06'),(50,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:55:32'),(51,14,'Sean','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:55:38'),(52,14,'Sean','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 12:57:52'),(53,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 12:58:00'),(54,10,'admin','ROLE_ADMIN','Permanently deleted account: Sean','0:0:0:0:0:0:0:1','2026-04-16 12:58:11'),(55,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:01:42'),(56,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:03:28'),(57,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:03:55'),(58,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:04:22'),(59,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:17:31'),(60,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:17:37'),(61,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:17:48'),(62,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:19:09'),(63,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:19:18'),(64,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:20:08'),(65,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:29:11'),(66,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:34:14'),(67,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:34:59'),(68,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:35:21'),(69,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-16 23:40:31'),(70,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-16 23:40:55'),(71,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 13:15:58'),(72,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 13:25:37'),(73,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 17:33:55'),(74,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-18 17:36:39'),(75,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 17:38:23'),(76,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 17:44:41'),(77,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-18 17:45:35'),(78,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-18 17:53:20'),(79,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-18 17:53:31'),(80,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-28 07:40:40'),(81,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-28 07:40:57'),(82,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-28 18:39:15'),(83,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-28 18:39:20'),(84,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-04-29 23:00:22'),(85,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-04-29 23:00:25'),(86,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 10:03:25'),(87,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 10:28:04'),(88,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 10:51:30'),(89,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 10:54:12'),(90,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:02:30'),(91,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject BPP-101','0:0:0:0:0:0:0:1','2026-05-09 13:02:54'),(92,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject BPP-102','0:0:0:0:0:0:0:1','2026-05-09 13:02:56'),(93,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject COOK-101','0:0:0:0:0:0:0:1','2026-05-09 13:03:27'),(94,11,'registrar','ROLE_REGISTRAR','Updated own personal details','0:0:0:0:0:0:0:1','2026-05-09 13:06:01'),(95,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:06:11'),(96,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:14:20'),(97,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:15:34'),(98,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:18:48'),(99,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:19:26'),(100,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:19:33'),(101,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:22:29'),(102,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:22:36'),(103,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 13:49:12'),(104,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 13:50:33'),(105,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-09 14:26:47'),(106,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-09 14:26:54'),(107,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-10 09:51:09'),(108,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-10 11:09:10'),(109,11,'registrar','ROLE_REGISTRAR','Created subject 001 (TestSubject)','0:0:0:0:0:0:0:1','2026-05-10 11:09:51'),(110,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject 001','0:0:0:0:0:0:0:1','2026-05-10 11:10:01'),(111,11,'registrar','ROLE_REGISTRAR','Updated subject 001','0:0:0:0:0:0:0:1','2026-05-10 11:10:08'),(112,11,'registrar','ROLE_REGISTRAR','Deleted subject 001','0:0:0:0:0:0:0:1','2026-05-10 11:10:15'),(113,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-10 11:13:14'),(114,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-10 11:13:21'),(115,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-10 16:17:40'),(116,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-10 16:45:26'),(117,11,'registrar','ROLE_REGISTRAR','Created section: TEST-T1 (TEST-T1)','0:0:0:0:0:0:0:1','2026-05-10 16:45:55'),(118,11,'registrar','ROLE_REGISTRAR','Created class: Pastry Arts in TEST-T1 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-10 16:46:12'),(119,11,'registrar','ROLE_REGISTRAR','Unassigned trainer from class #1 (BPP-102 / TEST-T1)','0:0:0:0:0:0:0:1','2026-05-10 16:46:35'),(120,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to class #1 (BPP-102 / TEST-T1)','0:0:0:0:0:0:0:1','2026-05-10 16:46:40'),(121,11,'registrar','ROLE_REGISTRAR','Updated student record: Ferreras, Mark (ID: SR20260002)','0:0:0:0:0:0:0:1','2026-05-10 16:48:06'),(122,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260002 in class #1','0:0:0:0:0:0:0:1','2026-05-10 16:48:26'),(123,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-13 16:22:40'),(124,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-13 23:20:10'),(125,11,'registrar','ROLE_REGISTRAR','Created class: Introduction to Cookery in TEST-T1 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-13 23:21:41'),(126,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260002 in class #2','0:0:0:0:0:0:0:1','2026-05-13 23:21:51'),(127,11,'registrar','ROLE_REGISTRAR','Created section: TEST-T2 (TEST-T2)','0:0:0:0:0:0:0:1','2026-05-13 23:26:26'),(128,11,'registrar','ROLE_REGISTRAR','Created class: Introduction to Cookery in TEST-T2 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-13 23:27:08'),(129,11,'registrar','ROLE_REGISTRAR','Removed enrollment #2','0:0:0:0:0:0:0:1','2026-05-13 23:27:46'),(130,11,'registrar','ROLE_REGISTRAR','Removed enrollment #1','0:0:0:0:0:0:0:1','2026-05-13 23:27:58'),(131,11,'registrar','ROLE_REGISTRAR','Updated student record: De Guzman, Kenneth (ID: SR20260004)','0:0:0:0:0:0:0:1','2026-05-13 23:29:09'),(132,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260002 in class #2','0:0:0:0:0:0:0:1','2026-05-13 23:29:39'),(133,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260004 in class #3','0:0:0:0:0:0:0:1','2026-05-13 23:29:47'),(134,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-15 15:17:27'),(135,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-15 15:20:00'),(136,11,'registrar','ROLE_REGISTRAR','Deleted subject COOK-104','0:0:0:0:0:0:0:1','2026-05-15 15:20:54'),(137,11,'registrar','ROLE_REGISTRAR','Deleted subject COOK-103','0:0:0:0:0:0:0:1','2026-05-15 15:21:01'),(138,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T1 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-15 15:21:27'),(139,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T1 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-15 15:21:29'),(140,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T1 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-15 15:21:29'),(141,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T1 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-15 15:21:30'),(142,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #2: 4 enrolled, 1 already enrolled, 0 ineligible','0:0:0:0:0:0:0:1','2026-05-15 15:21:52'),(143,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #1: 5 enrolled, 0 already enrolled, 0 ineligible','0:0:0:0:0:0:0:1','2026-05-15 15:22:01'),(144,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #3: 0 enrolled, 1 already enrolled, 0 ineligible','0:0:0:0:0:0:0:1','2026-05-15 15:22:12'),(145,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-15 15:22:29'),(146,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-18 16:28:35'),(147,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-18 22:49:37'),(148,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-18 23:20:30'),(149,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 07:30:10'),(150,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-19 07:44:02'),(151,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 11:13:45'),(152,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 12:06:52'),(153,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 12:53:04'),(154,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 13:54:37'),(155,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 14:06:32'),(156,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-19 14:39:54'),(157,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-19 14:40:23'),(158,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:17:15'),(159,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:18:34'),(160,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:19:39'),(161,11,'registrar','ROLE_REGISTRAR','Assigned trainer Trainer, Train to subject COOK-102','0:0:0:0:0:0:0:1','2026-05-23 06:19:59'),(162,11,'registrar','ROLE_REGISTRAR','Deleted student record: Keith, Keith (ID: SR20260012)','0:0:0:0:0:0:0:1','2026-05-23 06:20:25'),(163,11,'registrar','ROLE_REGISTRAR','Updated student record: Katana, Vivian (ID: SR20260003)','0:0:0:0:0:0:0:1','2026-05-23 06:25:12'),(164,11,'registrar','ROLE_REGISTRAR','Created section: Apple (TEST-T3)','0:0:0:0:0:0:0:1','2026-05-23 06:25:47'),(165,11,'registrar','ROLE_REGISTRAR','Updated student record: Katana, Vivian (ID: SR20260003)','0:0:0:0:0:0:0:1','2026-05-23 06:26:08'),(166,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:27:04'),(167,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:27:16'),(168,10,'admin','ROLE_ADMIN','Created new account: trainer2 (ROLE_TRAINER)','0:0:0:0:0:0:0:1','2026-05-23 06:28:54'),(169,10,'admin','ROLE_ADMIN','Deactivated account: trainer2','0:0:0:0:0:0:0:1','2026-05-23 06:29:11'),(170,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:29:18'),(171,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:29:41'),(172,10,'admin','ROLE_ADMIN','Re-enabled account: trainer2','0:0:0:0:0:0:0:1','2026-05-23 06:29:46'),(173,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:29:50'),(174,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:29:57'),(175,11,'registrar','ROLE_REGISTRAR','Created class: Food Safety and Sanitation in Apple (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-23 06:30:33'),(176,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #4: 0 enrolled, 0 already enrolled, 1 ineligible','0:0:0:0:0:0:0:1','2026-05-23 06:30:56'),(177,11,'registrar','ROLE_REGISTRAR','Updated student record: Katana, Vivian (ID: SR20260003)','0:0:0:0:0:0:0:1','2026-05-23 06:32:11'),(178,11,'registrar','ROLE_REGISTRAR','Bulk-enrolled section into class #4: 1 enrolled, 0 already enrolled, 0 ineligible','0:0:0:0:0:0:0:1','2026-05-23 06:32:25'),(179,11,'registrar','ROLE_REGISTRAR','Created subject TEST-01 (Computer Literacy)','0:0:0:0:0:0:0:1','2026-05-23 06:33:17'),(180,11,'registrar','ROLE_REGISTRAR','Assigned trainer Velasco, Alex to subject TEST-01','0:0:0:0:0:0:0:1','2026-05-23 06:33:33'),(181,11,'registrar','ROLE_REGISTRAR','Created class: Computer Literacy in TEST-T2 (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-23 06:34:08'),(182,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260004 in class #5','0:0:0:0:0:0:0:1','2026-05-23 06:34:16'),(183,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:34:24'),(184,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 06:34:30'),(185,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 06:36:52'),(186,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 10:03:46'),(187,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 10:04:07'),(188,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 10:11:23'),(189,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 10:14:05'),(190,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 10:14:12'),(191,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:03:42'),(192,10,'admin','ROLE_ADMIN','Created new account: TestUser (ROLE_TRAINER)','0:0:0:0:0:0:0:1','2026-05-23 12:07:30'),(193,10,'admin','ROLE_ADMIN','Updated user details for: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:07:58'),(194,10,'admin','ROLE_ADMIN','Reset password for: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:07:58'),(195,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:08:09'),(196,16,'TestUser','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:08:48'),(197,16,'TestUser','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:09:06'),(198,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:27:32'),(199,12,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:30:40'),(200,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:32:29'),(201,10,'admin','ROLE_ADMIN','Deactivated account: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:34:57'),(202,10,'admin','ROLE_ADMIN','Re-enabled account: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:36:01'),(203,10,'admin','ROLE_ADMIN','Permanently deleted account: TestUser','0:0:0:0:0:0:0:1','2026-05-23 12:36:08'),(204,10,'admin','ROLE_ADMIN','Updated own personal details','0:0:0:0:0:0:0:1','2026-05-23 12:43:09'),(205,10,'New Admin','ROLE_ADMIN','Changed own username from admin to New Admin','0:0:0:0:0:0:0:1','2026-05-23 12:43:59'),(206,10,'admin','ROLE_ADMIN','Changed own username from New Admin to admin','0:0:0:0:0:0:0:1','2026-05-23 12:44:38'),(207,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:44:47'),(208,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:57:21'),(209,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:58:53'),(210,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:58:59'),(211,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 12:59:46'),(212,12,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 12:59:52'),(213,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 14:45:26'),(214,10,'admin','ROLE_ADMIN','Created new account: wilkins (ROLE_TRAINER)','0:0:0:0:0:0:0:1','2026-05-23 14:49:43'),(215,10,'admin','ROLE_ADMIN','Deactivated account: trainer2','0:0:0:0:0:0:0:1','2026-05-23 14:50:04'),(216,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 14:50:16'),(217,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 14:50:22'),(218,11,'registrar','ROLE_REGISTRAR','Created subject CAP-111 (Capstone 1)','0:0:0:0:0:0:0:1','2026-05-23 14:51:50'),(219,11,'registrar','ROLE_REGISTRAR','Assigned trainer Wilks, Mark to subject CAP-111','0:0:0:0:0:0:0:1','2026-05-23 14:52:21'),(220,11,'registrar','ROLE_REGISTRAR','Removed enrollment #9','0:0:0:0:0:0:0:1','2026-05-23 14:53:59'),(221,11,'registrar','ROLE_REGISTRAR','Removed enrollment #10','0:0:0:0:0:0:0:1','2026-05-23 14:54:00'),(222,11,'registrar','ROLE_REGISTRAR','Removed enrollment #11','0:0:0:0:0:0:0:1','2026-05-23 14:54:01'),(223,11,'registrar','ROLE_REGISTRAR','Removed enrollment #12','0:0:0:0:0:0:0:1','2026-05-23 14:54:01'),(224,11,'registrar','ROLE_REGISTRAR','Removed enrollment #13','0:0:0:0:0:0:0:1','2026-05-23 14:54:02'),(225,11,'registrar','ROLE_REGISTRAR','Removed enrollment #3','0:0:0:0:0:0:0:1','2026-05-23 14:54:04'),(226,11,'registrar','ROLE_REGISTRAR','Removed enrollment #5','0:0:0:0:0:0:0:1','2026-05-23 14:54:05'),(227,11,'registrar','ROLE_REGISTRAR','Removed enrollment #6','0:0:0:0:0:0:0:1','2026-05-23 14:54:06'),(228,11,'registrar','ROLE_REGISTRAR','Removed enrollment #7','0:0:0:0:0:0:0:1','2026-05-23 14:54:07'),(229,11,'registrar','ROLE_REGISTRAR','Removed enrollment #8','0:0:0:0:0:0:0:1','2026-05-23 14:54:07'),(230,11,'registrar','ROLE_REGISTRAR','Created section: Gumamela (TEST-T4)','0:0:0:0:0:0:0:1','2026-05-23 14:55:15'),(231,11,'registrar','ROLE_REGISTRAR','Assigned 1 students to section TEST-T4 (skipped 0)','0:0:0:0:0:0:0:1','2026-05-23 14:55:37'),(232,11,'registrar','ROLE_REGISTRAR','Created class: Capstone 1 in Gumamela (Semester 2026)','0:0:0:0:0:0:0:1','2026-05-23 14:55:58'),(233,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260014 in class #6','0:0:0:0:0:0:0:1','2026-05-23 14:56:04'),(234,11,'registrar','ROLE_REGISTRAR','Deleted student record: dad, dad (ID: SR20260006)','0:0:0:0:0:0:0:1','2026-05-23 14:57:08'),(235,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 14:57:50'),(236,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 14:58:25'),(237,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 14:58:36'),(238,17,'wilkins','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 14:58:42'),(239,17,'wilkins','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 15:05:28'),(240,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 15:12:05'),(241,11,'registrar','ROLE_REGISTRAR','Updated student record: Plankton, Plank (ID: SR20260015)','0:0:0:0:0:0:0:1','2026-05-23 15:12:40'),(242,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260015 in class #6','0:0:0:0:0:0:0:1','2026-05-23 15:13:11'),(243,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-05-23 15:13:59'),(244,17,'wilkins','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-05-23 15:14:10'),(245,17,'wilkins','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-05-23 15:15:25'),(246,10,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-05-23 15:15:31'),(247,10,'admin','ROLE_ADMIN','Re-enabled account: trainer2','0:0:0:0:0:0:0:1','2026-05-23 15:15:35'),(248,10,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-05-23 15:16:12'),(249,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 15:16:19'),(250,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-05-23 17:50:38'),(251,11,'registrar','ROLE_REGISTRAR','Updated student record: Ferreras, Mark (ID: SR20260002)','0:0:0:0:0:0:0:1','2026-05-23 17:51:09'),(252,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 14:29:29'),(253,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 14:29:38'),(254,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 14:35:03'),(255,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 14:48:17'),(256,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 14:50:06'),(257,11,'registrar','ROLE_REGISTRAR','Deleted student record: Ferreras, Katch (ID: SR20260001)','0:0:0:0:0:0:0:1','2026-06-22 14:50:16'),(258,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 14:55:33'),(259,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 14:55:40'),(260,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 15:04:41'),(261,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 15:04:49'),(262,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-06-22 15:04:57'),(263,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 15:05:31'),(264,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-06-22 15:54:43'),(265,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 08:57:26'),(266,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 09:10:26'),(267,11,'registrar','ROLE_REGISTRAR','Deleted student record: Wong, Angelica (ID: SR20260009)','0:0:0:0:0:0:0:1','2026-07-14 09:25:27'),(268,11,'registrar','ROLE_REGISTRAR','Deleted student record: Avellaneda, Keith (ID: SR20260010)','0:0:0:0:0:0:0:1','2026-07-14 09:25:28'),(269,11,'registrar','ROLE_REGISTRAR','Deleted student record: Mark, Mark (ID: SR20260011)','0:0:0:0:0:0:0:1','2026-07-14 09:25:28'),(270,11,'registrar','ROLE_REGISTRAR','Deleted student record: test125, test125 (ID: SR20260017)','0:0:0:0:0:0:0:1','2026-07-14 09:25:28'),(271,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 09:38:28'),(272,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 09:40:41'),(273,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 09:51:28'),(274,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 12:42:30'),(275,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 12:53:35'),(276,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 12:56:14'),(277,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 13:06:09'),(278,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 13:19:27'),(279,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 14:18:59'),(280,11,'registrar','ROLE_REGISTRAR','Generated document \'SR20260002-FormIX-BPP-permanent.html\' (Form IX - Bread and Pastry Production NC II) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 14:20:58'),(281,11,'registrar','ROLE_REGISTRAR','Downloaded document \'SR20260002-FormIX-BPP-permanent.html\' (Form IX - Bread and Pastry Production NC II) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 14:21:38'),(282,11,'registrar','ROLE_REGISTRAR','Downloaded document \'SR20260002-FormIX-BPP-permanent.html\' (Form IX - Bread and Pastry Production NC II) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 14:21:59'),(283,11,'registrar','ROLE_REGISTRAR','Generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 14:26:36'),(284,11,'registrar','ROLE_REGISTRAR','Downloaded document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 14:29:39'),(285,11,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-07-14 14:59:47'),(286,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 17:47:58'),(287,11,'registrar','ROLE_REGISTRAR','Generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 17:48:22'),(288,11,'registrar','ROLE_REGISTRAR','Downloaded document \'TOR-Ferreras Mark.docx\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 17:48:24'),(289,11,'registrar','ROLE_REGISTRAR','Updated generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 17:48:26'),(290,11,'registrar','ROLE_REGISTRAR','Deleted document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 17:48:30'),(291,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 17:49:56'),(292,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 19:39:40'),(293,11,'registrar','ROLE_REGISTRAR','Generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 19:39:58'),(294,11,'registrar','ROLE_REGISTRAR','Downloaded document \'TOR-Ferreras Mark.docx\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 19:40:01'),(295,11,'registrar','ROLE_REGISTRAR','Updated generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 19:40:03'),(296,11,'registrar','ROLE_REGISTRAR','Deleted document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 19:40:06'),(297,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 19:41:13'),(298,11,'registrar','ROLE_REGISTRAR','Generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 19:41:28'),(299,11,'registrar','ROLE_REGISTRAR','Downloaded document \'TOR-Ferreras Mark.docx\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 19:41:30'),(300,11,'registrar','ROLE_REGISTRAR','Updated generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 19:41:32'),(301,11,'registrar','ROLE_REGISTRAR','Deleted document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 19:41:34'),(302,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 19:42:22'),(303,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 20:29:43'),(304,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 20:46:33'),(305,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 20:54:11'),(306,11,'registrar','ROLE_REGISTRAR','Generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 20:54:26'),(307,11,'registrar','ROLE_REGISTRAR','Downloaded document \'TOR-Ferreras Mark.docx\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 20:54:29'),(308,11,'registrar','ROLE_REGISTRAR','Updated generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 20:54:30'),(309,11,'registrar','ROLE_REGISTRAR','Deleted document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 20:54:33'),(310,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-14 22:22:45'),(311,11,'registrar','ROLE_REGISTRAR','Downloaded document \'TOR-Ferreras Mark.docx\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 22:22:51'),(312,11,'registrar','ROLE_REGISTRAR','Deleted document \'SR20260002-FormIX-BPP-permanent.html\' (Form IX - Bread and Pastry Production NC II) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 22:23:41'),(313,11,'registrar','ROLE_REGISTRAR','Deleted document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 22:24:11'),(314,11,'registrar','ROLE_REGISTRAR','Generated document \'SR20260002-TOR.html\' (Transcript of Records (TOR)) for student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 22:25:48'),(315,11,'registrar','ROLE_REGISTRAR','Downloaded document \'TOR-Ferreras Mark.docx\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-14 22:26:04'),(316,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-24 14:28:41'),(317,11,'registrar','ROLE_REGISTRAR','Created class: Pastry Arts in Apple (Semester 2026)','0:0:0:0:0:0:0:1','2026-07-24 14:29:23'),(318,11,'registrar','ROLE_REGISTRAR','Created class: Bread Making Fundamentals in Apple (Semester 2026)','0:0:0:0:0:0:0:1','2026-07-24 14:40:01'),(319,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260003 in class #7','0:0:0:0:0:0:0:1','2026-07-24 14:40:32'),(320,11,'registrar','ROLE_REGISTRAR','Enrolled student SR20260003 in class #8','0:0:0:0:0:0:0:1','2026-07-24 14:40:44'),(321,11,'registrar','ROLE_REGISTRAR','Updated student record: Perkin, Mazlo (ID: SR20260014)','0:0:0:0:0:0:0:1','2026-07-24 14:42:05'),(322,11,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-07-24 15:27:32'),(323,11,'registrar','ROLE_REGISTRAR','Downloaded document \'TOR-Ferreras Mark.docx\' (Transcript of Records (TOR)) of student SR20260002','0:0:0:0:0:0:0:1','2026-07-24 15:28:42');
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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (10,'admin','$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6','Big','Cheese','Tiger','2004-01-01',22,'mark@example.com','ROLE_ADMIN',1,NULL),(11,'registrar','$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6','Registrar','Reg','B','1996-06-05',30,'reg@example.com','ROLE_REGISTRAR',1,NULL),(12,'trainer','$2a$10$IM6yEO9HpxmNE.cm2k1SUOGBkIDJwfjIwBg..ia8JgZIHULz.STHK','Trainer','Train','C','1985-10-10',40,'trainer@example.com','ROLE_TRAINER',1,'2026-04-16 12:07:31'),(15,'trainer2','$2a$10$zBGUpRTUhVsC4ieutm3oi.nsF0esYpIg/ABD4zaHZ88BgfK0D0Pr2','Velasco','Alex','E.','1993-11-24',32,'user@anihan.local','ROLE_TRAINER',1,NULL),(17,'wilkins','$2a$10$HPzOlw8I30AUKgZGMGIbWudvmwXJb/eVLf8XiUnRe30TJ/IlYD/pK','Wilks','Mark','F','1988-06-15',37,'wilks@gmail.com','ROLE_TRAINER',1,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'AnihanSRMS'
--
/*!50003 DROP FUNCTION IF EXISTS `column_exists` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = latin1 */ ;
/*!50003 SET character_set_results = latin1 */ ;
/*!50003 SET collation_connection  = latin1_swedish_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` FUNCTION `column_exists`(
  p_table_name VARCHAR(255),
  p_column_name VARCHAR(255)
) RETURNS tinyint(1)
    READS SQL DATA
    DETERMINISTIC
BEGIN
  DECLARE col_count INT;
  SELECT COUNT(*) INTO col_count
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = p_table_name
  AND COLUMN_NAME = p_column_name;
  RETURN col_count > 0;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `AddColumnIfNotExists` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = latin1 */ ;
/*!50003 SET character_set_results = latin1 */ ;
/*!50003 SET collation_connection  = latin1_swedish_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `AddColumnIfNotExists`(
  IN p_table_name VARCHAR(255),
  IN p_column_name VARCHAR(255),
  IN p_column_definition VARCHAR(255)
)
BEGIN
  DECLARE column_exists INT DEFAULT 0;

  SELECT COUNT(*) INTO column_exists
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = p_table_name
  AND COLUMN_NAME = p_column_name;

  IF column_exists = 0 THEN
    SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_definition);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-06 11:00:09
