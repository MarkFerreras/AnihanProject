-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: AnihanSRMS
-- ------------------------------------------------------
-- Server version	8.0.46

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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_enrollments`
--

LOCK TABLES `class_enrollments` WRITE;
/*!40000 ALTER TABLE `class_enrollments` DISABLE KEYS */;
INSERT INTO `class_enrollments` VALUES (2,7,'STU-2026-001','2026-08-29 23:50:39');
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
  KEY `trainer_id` (`trainer_id`),
  KEY `fk_classes_subject` (`subject_code`),
  CONSTRAINT `classes_ibfk_1` FOREIGN KEY (`section_code`) REFERENCES `sections` (`section_code`),
  CONSTRAINT `classes_ibfk_3` FOREIGN KEY (`trainer_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_classes_subject` FOREIGN KEY (`subject_code`) REFERENCES `subjects` (`subject_code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (7,'SEC-A26','BPP-101',3,'2026','2026-08-29 01:47:17');
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
  KEY `student_id` (`student_id`),
  CONSTRAINT `documents_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT INTO `documents` VALUES (1,'STU-2024-001','Transcript of Records (TOR)','STU-2024-001-TOR.html','text/html',45263,_binary '<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<title>Transcript of Records (TOR) — STU-2024-001</title>\n<style>\n/*\r\n * document-print.css — layout for generated official documents (TOR / Form IX).\r\n * Used live on generate-document.html and embedded inline into saved HTML\r\n * documents so they stay self-contained when viewed or reprinted later.\r\n */\r\n\r\n.document-sheet {\r\n    background: #fff;\r\n    color: #000;\r\n    font-family: \"Times New Roman\", Georgia, serif;\r\n    font-size: 11px;\r\n    line-height: 1.35;\r\n    width: 8.27in;\r\n    max-width: 100%;\r\n    margin: 0 auto;\r\n    padding: 0.6in 0.55in;\r\n    box-sizing: border-box;\r\n}\r\n\r\n.document-sheet * {\r\n    box-sizing: border-box;\r\n}\r\n\r\n/* ----- School header ----- */\r\n.doc-school-header {\r\n    display: flex;\r\n    align-items: center;\r\n    justify-content: center;\r\n    gap: 14px;\r\n    text-align: center;\r\n    margin-bottom: 14px;\r\n}\r\n\r\n.doc-school-logo {\r\n    flex: 0 0 auto;\r\n    width: 62px;\r\n    height: auto;\r\n}\r\n\r\n.doc-school-name {\r\n    font-size: 22px;\r\n    font-style: italic;\r\n    font-weight: bold;\r\n    margin: 0;\r\n}\r\n\r\n.doc-school-sub {\r\n    font-size: 10px;\r\n    margin: 0;\r\n}\r\n\r\n.doc-form-label {\r\n    text-align: right;\r\n    font-weight: bold;\r\n    font-size: 11px;\r\n    margin: 4px 0;\r\n}\r\n\r\n.doc-title {\r\n    text-align: center;\r\n    font-weight: bold;\r\n    letter-spacing: 2px;\r\n    margin: 10px 0 14px;\r\n    font-size: 13px;\r\n}\r\n\r\n.doc-title .doc-subtitle {\r\n    display: block;\r\n    letter-spacing: 3px;\r\n}\r\n\r\n/* ----- Header field rows ----- */\r\n.doc-fields {\r\n    width: 100%;\r\n    border-bottom: 2px solid #000;\r\n    padding-bottom: 6px;\r\n    margin-bottom: 10px;\r\n}\r\n\r\n.doc-field-row {\r\n    display: flex;\r\n    gap: 14px;\r\n    margin-bottom: 3px;\r\n}\r\n\r\n.doc-field {\r\n    display: flex;\r\n    flex: 1;\r\n    align-items: flex-end;\r\n    min-width: 0;\r\n}\r\n\r\n.doc-field-label {\r\n    white-space: nowrap;\r\n    padding-right: 4px;\r\n}\r\n\r\n.doc-field .fill {\r\n    flex: 1;\r\n    border-bottom: 1px solid #000;\r\n    min-height: 14px;\r\n    padding: 0 3px;\r\n    font-weight: 600;\r\n}\r\n\r\n.doc-field .fill.static-value {\r\n    font-weight: 600;\r\n}\r\n\r\n.doc-field.narrow { flex: 0 0 auto; min-width: 130px; }\r\n.doc-field.wide { flex: 2; }\r\n\r\n/* ----- Fillable spans ----- */\r\n.fill {\r\n    display: inline-block;\r\n    min-width: 30px;\r\n    outline: none;\r\n}\r\n\r\n.fill:empty::before {\r\n    content: \'\\00a0\';\r\n}\r\n\r\n.document-toolbar-active .fill[contenteditable=\"true\"]:hover,\r\n.document-toolbar-active .fill[contenteditable=\"true\"]:focus {\r\n    background: #fff8d6;\r\n}\r\n\r\n/* ----- Semester heading ----- */\r\n.doc-semester {\r\n    font-weight: bold;\r\n    text-decoration: underline;\r\n    margin: 12px 0 6px;\r\n}\r\n\r\n/* ----- Subjects table ----- */\r\n.doc-subjects-table {\r\n    width: 100%;\r\n    border-collapse: collapse;\r\n    margin-bottom: 8px;\r\n}\r\n\r\n.doc-subjects-table th,\r\n.doc-subjects-table td {\r\n    border: 1px dotted #666;\r\n    padding: 1px 4px;\r\n    vertical-align: bottom;\r\n}\r\n\r\n.doc-subjects-table thead th {\r\n    border: 1px solid #000;\r\n    text-align: center;\r\n    font-size: 10px;\r\n    padding: 2px 4px;\r\n}\r\n\r\n.doc-subjects-table .col-code { width: 12%; text-align: center; }\r\n.doc-subjects-table .col-grade { width: 7%; text-align: center; }\r\n.doc-subjects-table .col-hours { width: 7%; text-align: right; }\r\n.doc-subjects-table .col-units { width: 7%; text-align: right; }\r\n.doc-subjects-table .col-remarks { width: 11%; text-align: center; }\r\n\r\n.doc-subjects-table .section-row td {\r\n    font-weight: bold;\r\n    border-left: 1px dotted #666;\r\n}\r\n\r\n.doc-subjects-table .total-row td {\r\n    font-weight: bold;\r\n    text-align: right;\r\n}\r\n\r\n/* ----- End of transcript ----- */\r\n.doc-end-line {\r\n    text-align: center;\r\n    font-size: 9px;\r\n    letter-spacing: 1px;\r\n    margin: 6px 0;\r\n    word-break: break-all;\r\n}\r\n\r\n/* ----- Notes + grading legend ----- */\r\n.doc-notes {\r\n    font-size: 9px;\r\n    margin: 8px 0;\r\n}\r\n\r\n.doc-notes p { margin: 0 0 2px; }\r\n\r\n.doc-grading-wrap {\r\n    display: flex;\r\n    gap: 12px;\r\n    align-items: center;\r\n    margin: 10px 0;\r\n}\r\n\r\n.doc-seal-note {\r\n    flex: 0 0 110px;\r\n    text-align: center;\r\n    font-size: 9px;\r\n    color: #444;\r\n}\r\n\r\n.doc-grading-table {\r\n    flex: 1;\r\n    border-collapse: collapse;\r\n    font-size: 9px;\r\n}\r\n\r\n.doc-grading-table caption {\r\n    caption-side: top;\r\n    text-align: center;\r\n    font-weight: bold;\r\n    letter-spacing: 3px;\r\n    font-size: 10px;\r\n    color: #000;\r\n}\r\n\r\n.doc-grading-table td {\r\n    border: 1px dotted #666;\r\n    padding: 1px 4px;\r\n}\r\n\r\n.doc-grading-table .competent-cell {\r\n    border: 1px solid #000;\r\n    text-align: center;\r\n    vertical-align: middle;\r\n    width: 70px;\r\n}\r\n\r\n/* ----- Certification ----- */\r\n.doc-certification {\r\n    margin: 14px 0;\r\n    text-align: justify;\r\n}\r\n\r\n.doc-certification .cert-heading {\r\n    letter-spacing: 3px;\r\n    font-weight: bold;\r\n}\r\n\r\n.doc-certification .fill {\r\n    border-bottom: 1px solid #000;\r\n    padding: 0 4px;\r\n    font-weight: 600;\r\n}\r\n\r\n/* ----- Signatories ----- */\r\n.doc-signatories {\r\n    display: flex;\r\n    justify-content: space-between;\r\n    margin-top: 22px;\r\n    gap: 40px;\r\n}\r\n\r\n.doc-signatory {\r\n    flex: 1;\r\n    text-align: center;\r\n}\r\n\r\n.doc-signatory .sig-caption {\r\n    text-align: left;\r\n    margin-bottom: 28px;\r\n}\r\n\r\n.doc-signatory .sig-name {\r\n    border-top: 1px solid #000;\r\n    display: inline-block;\r\n    min-width: 220px;\r\n    padding-top: 2px;\r\n    font-weight: bold;\r\n}\r\n\r\n.doc-signatory .sig-role {\r\n    margin-top: 0;\r\n}\r\n\r\n.doc-date-line {\r\n    text-align: right;\r\n    margin: 10px 0;\r\n}\r\n\r\n/* ----- Footer contact line ----- */\r\n.doc-contact-footer {\r\n    text-align: center;\r\n    font-size: 9px;\r\n    margin-top: 18px;\r\n    border-top: 1px solid #000;\r\n    padding-top: 4px;\r\n}\r\n\r\n/* ----- Page breaks between sheets ----- */\r\n.doc-page-break {\r\n    page-break-before: always;\r\n}\r\n\r\n/* ----- Print rules ----- */\r\n@page {\r\n    size: A4 portrait;\r\n    margin: 9mm 11mm;\r\n}\r\n\r\n@media print {\r\n    /* Only the white document content prints — no grey app backdrop, no chrome.\r\n       body must not stay a flex container: Chromium cannot fragment flex items\r\n       across pages, which pushed whole documents onto a second page. */\r\n    html,\r\n    body {\r\n        display: block !important;\r\n        min-height: 0 !important;\r\n        height: auto !important;\r\n        background: #fff !important;\r\n        margin: 0 !important;\r\n        padding: 0 !important;\r\n    }\r\n\r\n    body.generate-document-page > *:not(#documentArea) {\r\n        display: none !important;\r\n    }\r\n\r\n    #documentArea {\r\n        margin: 0 !important;\r\n        padding: 0 !important;\r\n    }\r\n\r\n    .document-sheet {\r\n        width: 100%;\r\n        padding: 0;\r\n        box-shadow: none !important;\r\n        font-size: 10px;\r\n        line-height: 1.25;\r\n    }\r\n\r\n    .fill[contenteditable=\"true\"] {\r\n        background: transparent !important;\r\n    }\r\n\r\n    /* Tighter vertical rhythm so 1-page templates stay on one page. */\r\n    .doc-school-header { margin-bottom: 8px; }\r\n    .doc-title { margin: 6px 0 8px; }\r\n    .doc-grading-wrap { margin: 6px 0; }\r\n    .doc-certification { margin: 10px 0; }\r\n    .doc-signatories { margin-top: 14px; }\r\n    .doc-contact-footer { margin-top: 10px; }\r\n\r\n    /* No table row or trailing block may straddle a page break. */\r\n    .doc-subjects-table tr,\r\n    .doc-grading-wrap,\r\n    .doc-signatories,\r\n    .doc-certification,\r\n    .doc-contact-footer {\r\n        page-break-inside: avoid;\r\n    }\r\n\r\n    .doc-subjects-table thead {\r\n        display: table-header-group;\r\n    }\r\n}\n</style>\n</head>\n<body>\n<div class=\"document-sheet\"><div class=\"doc-school-header\"><img class=\"doc-school-logo\" src=\"data:image/png;base64,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\" alt=\"Anihan Technical School logo\"><div><p class=\"doc-school-name\">Anihan Technical School</p><p class=\"doc-school-sub\">(For Women-in-Development)</p><p class=\"doc-school-sub\">A project of the Foundation for Professional Training, Inc. (FPTI)</p></div></div><div class=\"doc-title\">O F F I C E&nbsp;&nbsp;O F&nbsp;&nbsp;T H E&nbsp;&nbsp;R E G I S T R A R<span class=\"doc-subtitle\">O F F I C I A L&nbsp;&nbsp;T R A N S C R I P T&nbsp;&nbsp;O F&nbsp;&nbsp;R E C O R D S</span></div><div class=\"doc-fields\"><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Name</span><span class=\"fill\">Reyes, Anna Cruz</span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Address</span><span class=\"fill\">123 Mabini St, Quezon City</span></div></div><div class=\"doc-field-row\"><div class=\"doc-field \"><span class=\"doc-field-label\">Date of Admission</span><span class=\"fill\">June 3, 2024</span></div><div class=\"doc-field narrow\"><span class=\"doc-field-label\">Sex</span><span class=\"fill\">Female</span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Birthday</span><span class=\"fill\">April 12, 2003</span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Student No.</span><span class=\"fill\">STU-2024-001</span></div></div><div class=\"doc-field-row\"><div class=\"doc-field \"><span class=\"doc-field-label\">Entrance Date</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Hon. Dismissal</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Graduation</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Entrance Data</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">SY Ended</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Course Title</span><span class=\"fill\">Culinary Arts and Restaurant Services</span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Certificate No.</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field\"><span class=\"doc-field-label\">TESDA Assessment</span><span class=\"fill static-value\"><u><strong>Bread and Pastry Production-NC II</strong></u></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Date Taken</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Result</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Special Order No.</span><span class=\"fill\"></span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Issued</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field\"><span class=\"doc-field-label\">TESDA Assessment</span><span class=\"fill static-value\"><u><strong>Food and Beverage Services-NC II</strong></u></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Date Taken</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Result</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Special Order No.</span><span class=\"fill\"></span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Issued</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field\"><span class=\"doc-field-label\">TESDA Assessment</span><span class=\"fill static-value\"><u><strong>Cookery -NC II</strong></u></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Date Taken</span><span class=\"fill\"></span></div><div class=\"doc-field \"><span class=\"doc-field-label\">Result</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Special Order No.</span><span class=\"fill\"></span></div><div class=\"doc-field wide\"><span class=\"doc-field-label\">Issued</span><span class=\"fill\"></span></div></div><div class=\"doc-field-row\"><div class=\"doc-field wide\"><span class=\"doc-field-label\">Remarks</span><span class=\"fill\"></span></div></div></div><p class=\"doc-semester\"><span class=\"fill\">SY 2024-2025, First Semester</span></p><table class=\"doc-subjects-table\"><thead><tr><th rowspan=\"2\" class=\"col-code\">SUBJECT CODE</th><th rowspan=\"2\">SUBJECT TITLE</th><th colspan=\"2\">GRADES</th><th rowspan=\"2\" class=\"col-hours\">HOURS</th><th rowspan=\"2\" class=\"col-units\">UNITS</th></tr><tr><th class=\"col-grade\">FINAL</th><th class=\"col-grade\">RE-EXAM</th></tr></thead><tbody><tr class=\"section-row\"><td></td><td colspan=\"5\">BASIC COMPETENCIES:</td></tr><tr><td class=\"col-code\">400311210</td><td>Participating in Workplace Communication</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311211</td><td>Working in a Team Environment</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311212</td><td>Solving and Addressing General Workplace Problems</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311213</td><td>Developing Career and Life Decisions</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311214</td><td>Contributing to Workplace Innovation</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311215</td><td>Presenting Relevant Information</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311216</td><td>Practicing Occupational Safety and Health Policies and Procedures</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311217</td><td>Exercising Efficient and Effective Sustainable Practices in the Workplace</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr><td class=\"col-code\">400311218</td><td>Practicing Entrepreneurial Skills in the Workplace</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">2</td><td class=\"col-units\">0.11</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">COMMON COMPETENCIES:</td></tr><tr><td class=\"col-code\">TRS311201</td><td>Developing and Updating Industry Knowledge</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">4.5</td><td class=\"col-units\">0.25</td></tr><tr><td class=\"col-code\">TRS311202</td><td>Observing Workplace Hygiene Procedures</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">4.5</td><td class=\"col-units\">0.25</td></tr><tr><td class=\"col-code\">TRS311203</td><td>Performing Computer Operations</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS311204</td><td>Performing Workplace and Safety Practices</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">4.5</td><td class=\"col-units\">0.25</td></tr><tr><td class=\"col-code\">TRS311205</td><td>Providing Effective Customer Service</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">4.5</td><td class=\"col-units\">0.25</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">CORE COMPETENCIES-BREAD and PASTRY PRODUCTION NC II:</td></tr><tr><td class=\"col-code\">TRS741342</td><td>Preparing and Presenting Gateaux, Tortes and Cakes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">38</td><td class=\"col-units\">2.11</td></tr><tr><td class=\"col-code\">TRS741343</td><td>Presenting Desserts</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">10</td><td class=\"col-units\">0.56</td></tr><tr><td class=\"col-code\">TRS741344</td><td>Preparing and Displaying Petit Fours</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">10</td><td class=\"col-units\">0.56</td></tr><tr><td class=\"col-code\">TRS741379</td><td>Preparing and Producing Bakery Products</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">38</td><td class=\"col-units\">2.11</td></tr><tr><td class=\"col-code\">TRS741380</td><td>Preparing and Producing Pastry Products</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">40</td><td class=\"col-units\">2.22</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">CORE COMPETENCIES - FOOD AND BEVERAGE SERVICES NC II:</td></tr><tr><td class=\"col-code\">TRS512387</td><td>Preparing the Dining Room/Restaurant Area for Service</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">63</td><td class=\"col-units\">3.50</td></tr><tr><td class=\"col-code\">TRS512388</td><td>Welcoming Guests and Take Food and Beverage Orders</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">60</td><td class=\"col-units\">3.33</td></tr><tr><td class=\"col-code\">TRS512389</td><td>Promoting Food and Beverage Products</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">26</td><td class=\"col-units\">1.44</td></tr><tr><td class=\"col-code\">TRS512390</td><td>Providing Food and Beverage Services to Guests</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">60</td><td class=\"col-units\">3.33</td></tr><tr><td class=\"col-code\">TRS512391</td><td>Providing Room Service</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">60</td><td class=\"col-units\">3.33</td></tr><tr><td class=\"col-code\">TRS512392</td><td>Receiving and Handling Guest Concerns</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">27</td><td class=\"col-units\">1.50</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">CORE COMPETENCIES-COOKERY NC II:</td></tr><tr><td class=\"col-code\">TRS512328</td><td>Cleaning and Maintaining Kitchen Premises</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">13</td><td class=\"col-units\">0.72</td></tr><tr><td class=\"col-code\">TRS512330</td><td>Preparing Sandwiches</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">12</td><td class=\"col-units\">0.67</td></tr><tr><td class=\"col-code\">TRS512331</td><td>Preparing Stocks, Sauces, and Soups</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS512333</td><td>Preparing Poultry and Game Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS512334</td><td>Preparing Seafood Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">27</td><td class=\"col-units\">1.50</td></tr><tr><td class=\"col-code\">TRS512335</td><td>Preparing Desserts</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS512340</td><td>Packaging Prepared Food</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">9</td><td class=\"col-units\">0.50</td></tr><tr><td class=\"col-code\">TRS512381</td><td>Preparing Appetizers</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">12</td><td class=\"col-units\">0.67</td></tr><tr><td class=\"col-code\">TRS512382</td><td>Preparing Salads and Dressings</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">12</td><td class=\"col-units\">0.67</td></tr><tr><td class=\"col-code\">TRS512383</td><td>Preparing Meat Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">27</td><td class=\"col-units\">1.50</td></tr><tr><td class=\"col-code\">TRS512384</td><td>Preparing Vegetable Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">36</td><td class=\"col-units\">2.00</td></tr><tr><td class=\"col-code\">TRS512385</td><td>Preparing Egg Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">TRS512386</td><td>Preparing Starch Dishes</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">36</td><td class=\"col-units\">2.00</td></tr><tr class=\"section-row\"><td></td><td colspan=\"5\">OTHER SUBJECTS</td></tr><tr><td class=\"col-code\">BFS</td><td>Basic Food Safety</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">ENG</td><td>English</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">ENTREP</td><td>Entrepreneurship</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">24</td><td class=\"col-units\">1.33</td></tr><tr><td class=\"col-code\">MATH</td><td>Culinary Math</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">CL 1</td><td>Christian Living 1</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">CL 2</td><td>Christian Living 2</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">PD</td><td>Personality Development 1</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">VE 1</td><td>Values Education 1</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr></tbody></table><p class=\"doc-semester\"><span class=\"fill\">SY 2024-2025, Second Semester</span></p><table class=\"doc-subjects-table\"><thead><tr><th rowspan=\"2\" class=\"col-code\">SUBJECT CODE</th><th rowspan=\"2\">SUBJECT TITLE</th><th colspan=\"2\">GRADES</th><th rowspan=\"2\" class=\"col-hours\">HOURS</th><th rowspan=\"2\" class=\"col-units\">UNITS</th></tr><tr><th class=\"col-grade\">FINAL</th><th class=\"col-grade\">RE-EXAM</th></tr></thead><tbody><tr class=\"section-row\"><td></td><td colspan=\"5\">OTHER SUBJECTS</td></tr><tr><td class=\"col-code\">OJT</td><td>On-the-Job-Training</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">900</td><td class=\"col-units\">50.00</td></tr><tr><td class=\"col-code\">CL 3</td><td>Christian Living 3</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">CL 4</td><td>Christian Living 4</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr><tr><td class=\"col-code\">VE 2</td><td>Values Education 2</td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-grade\"><span class=\"fill\"></span></td><td class=\"col-hours\">18</td><td class=\"col-units\">1.00</td></tr></tbody></table><p class=\"doc-end-line\">X X X X X X X X X X X X X X X X X END OF TRANSCRIPT X X X X X X X X X X X X X X X X X</p><div class=\"doc-notes\"><p><strong>CREDITS:</strong> One unit credit is one hour lecture, laboratory or practicum / OJT per week for a period of a complete semester.</p><p><strong>NOTE:</strong> Any erasure or alteration on this record invalidates the whole transcript.</p></div><div class=\"doc-grading-wrap\"><div class=\"doc-seal-note\">Not Valid<br>Without School<br>Dry Seal<br><span class=\"fill\">August 27, 2026</span></div><table class=\"doc-grading-table\"><caption>G R A D I N G&nbsp;&nbsp;S Y S T E M</caption><tbody><tr><td>99-100</td><td>1.00</td><td>Excellent</td><td class=\"competent-cell\" rowspan=\"8\">Competent</td><td>75-77</td><td>3.00</td><td>Passed</td><td class=\"competent-cell\" rowspan=\"2\">Competent</td></tr><tr><td>96-98</td><td>1.25</td><td>Very Good</td><td>C</td><td></td><td>Complete</td></tr><tr><td>93-95</td><td>1.50</td><td>Very Good</td><td>70-74</td><td>4.00</td><td>Conditional Failure</td><td class=\"competent-cell\" rowspan=\"5\">Not Competent</td></tr><tr><td>90-92</td><td>1.75</td><td>Good</td><td>Below 70</td><td>5.00</td><td>Failure</td></tr><tr><td>87-89</td><td>2.00</td><td>Good</td><td>FA</td><td></td><td>Failure Due to Absences</td></tr><tr><td>84-86</td><td>2.25</td><td>Good</td><td>INC</td><td></td><td>Incomplete</td></tr><tr><td>81-83</td><td>2.50</td><td>Satisfactory</td><td>D</td><td></td><td>Dropped</td></tr><tr><td>78-80</td><td>2.75</td><td>Satisfactory</td><td></td><td></td><td></td><td></td></tr></tbody></table></div><div class=\"doc-signatories\"><div class=\"doc-signatory\"><p class=\"sig-caption\">Prepared by:</p><span class=\"sig-name fill\">MELESINE B. VELASCO</span><p class=\"sig-role\">Assistant to the Registrar</p></div><div class=\"doc-signatory\"><p class=\"sig-caption\">Approved by:</p><span class=\"sig-name fill\">HERMINIA F. GABUTINA</span><p class=\"sig-role\">Registrar</p></div></div><div class=\"doc-contact-footer\">Email: info@anihan.edu.ph / anihanschool@gmail.com / registrar@anihan.edu.ph * Web: anihan.edu.ph<br>294 Purok 6 Brgy. Milagrosa, Calamba City, Laguna 4027 Philippines * (049) 545-1598 * 0939-2666108</div></div>\n</body>\n</html>\n','2026-08-27 06:00:34');
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
  `final_percentage` decimal(5,2) DEFAULT NULL,
  `re_exam_percentage` decimal(5,2) DEFAULT NULL,
  `locked` tinyint(1) NOT NULL DEFAULT '0',
  `locked_at` datetime DEFAULT NULL,
  `final_grade` decimal(5,2) DEFAULT NULL,
  `re_exam_grade` decimal(5,2) DEFAULT NULL,
  `grade_status` varchar(5) DEFAULT NULL,
  `hours_rendered` decimal(5,2) DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`grade_id`),
  UNIQUE KEY `uq_grade_student_class` (`class_id`,`student_id`),
  KEY `student_id` (`student_id`),
  KEY `fk_grades_subject` (`subject_code`),
  CONSTRAINT `fk_grades_subject` FOREIGN KEY (`subject_code`) REFERENCES `subjects` (`subject_code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `grades_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student_records` (`student_id`),
  CONSTRAINT `grades_ibfk_3` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grades`
--

LOCK TABLES `grades` WRITE;
/*!40000 ALTER TABLE `grades` DISABLE KEYS */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parents`
--

LOCK TABLES `parents` WRITE;
/*!40000 ALTER TABLE `parents` DISABLE KEYS */;
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
-- Table structure for table `security_questions`
--

DROP TABLE IF EXISTS `security_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `security_questions` (
  `question_id` int NOT NULL AUTO_INCREMENT,
  `question_text` varchar(255) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`question_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `security_questions`
--

LOCK TABLES `security_questions` WRITE;
/*!40000 ALTER TABLE `security_questions` DISABLE KEYS */;
INSERT INTO `security_questions` VALUES (1,'What is your favorite color',1),(2,'What is your favorite vacation place?',1),(3,'What is your favorite song?',1),(4,'Who is your favorite artist?',1),(5,'What is your favorite food?',1),(6,'What is the name of your pet?',1);
/*!40000 ALTER TABLE `security_questions` ENABLE KEYS */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_education`
--

LOCK TABLES `student_education` WRITE;
/*!40000 ALTER TABLE `student_education` DISABLE KEYS */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_ojt`
--

LOCK TABLES `student_ojt` WRITE;
/*!40000 ALTER TABLE `student_ojt` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_records`
--

LOCK TABLES `student_records` WRITE;
/*!40000 ALTER TABLE `student_records` DISABLE KEYS */;
INSERT INTO `student_records` VALUES (1,'STU-2024-001','Reyes','Anna','Cruz','2003-04-12',22,'Female','Single','123 Mabini St, Quezon City','45 Aurora Blvd, Manila','anna.reyes@example.com','09171234001','Roman Catholic',1,'2003-06-20','San Pedro Parish, Manila',2,1,1,'B2024A','CARS','SEC-A24','','2024-06-03','Active'),(2,'STU-2024-002','Santos','Bea','Lim','2002-09-30',23,'Female','Single','88 Roxas Ave, Pasig','12 EDSA, Mandaluyong','bea.santos@example.com','09171234002','Iglesia ni Cristo',1,'2003-01-15','INC Central Temple, Quezon City',3,2,1,'B2024A','CARS','SEC-A24','','2024-06-03','Active'),(3,'STU-2025-001','Cruz','Carla','Mendoza','2004-01-18',22,'Female','Single','7 Bonifacio St, Makati','7 Bonifacio St, Makati','carla.cruz@example.com','09171234003','Christian',1,'2004-05-10','Christ Fellowship Church, Makati',1,0,1,'B2025A','CARS','SEC-A25','','2025-06-02','Active'),(4,'STU-2025-002','Garcia','Diana','Reyes','2003-12-05',22,'Female','Single','256 Espana Blvd, Manila','256 Espana Blvd, Manila','diana.garcia@example.com','09171234004','Roman Catholic',1,'2004-02-28','Sto. Domingo Church, Manila',4,2,2,'B2025A','CARS','SEC-A25','','2025-06-02','Active'),(5,'STU-2026-001','Lopez','Elise','Tan','2005-07-22',20,'Female','Single','19 Katipunan Ave, Quezon City','19 Katipunan Ave, Quezon City','elise.lopez@example.com','09171234005','Roman Catholic',1,'2005-10-14','Mary Immaculate Parish, Quezon City',2,0,2,'B2026A','CARS','SEC-A26','','2026-06-01','Active');
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_school_years`
--

LOCK TABLES `student_school_years` WRITE;
/*!40000 ALTER TABLE `student_school_years` DISABLE KEYS */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_tesda_qualifications`
--

LOCK TABLES `student_tesda_qualifications` WRITE;
/*!40000 ALTER TABLE `student_tesda_qualifications` DISABLE KEYS */;
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
  `qualification_code` int DEFAULT NULL,
  `competency_type` varchar(15) NOT NULL,
  `units` int NOT NULL,
  PRIMARY KEY (`subject_code`),
  KEY `qualification_code` (`qualification_code`),
  CONSTRAINT `subjects_ibfk_1` FOREIGN KEY (`qualification_code`) REFERENCES `qualifications` (`qualification_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subjects`
--

LOCK TABLES `subjects` WRITE;
/*!40000 ALTER TABLE `subjects` DISABLE KEYS */;
INSERT INTO `subjects` VALUES ('BPP-101','Bread Making Fundamentals',2,'CORE',3),('BPP-102','Pastry Arts',2,'CORE',4),('COOK-101','Introduction to Cookery',1,'CORE',3),('COOK-102','Food Safety and Sanitation',1,'CORE',3),('COOK-103','Prepare Hot Meals',1,'CORE',5),('COOK-104','Prepare Cold Meals',1,'CORE',4);
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
) ENGINE=InnoDB AUTO_INCREMENT=112 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_logs`
--

LOCK TABLES `system_logs` WRITE;
/*!40000 ALTER TABLE `system_logs` DISABLE KEYS */;
INSERT INTO `system_logs` VALUES (1,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-26 11:45:28'),(2,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-26 17:48:21'),(3,2,'registrar','ROLE_REGISTRAR','User logged in','127.0.0.1','2026-08-26 20:02:55'),(4,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-26 20:18:08'),(5,2,'registrar','ROLE_REGISTRAR','Created subject SMOKE-101 (Smoke Test Subject)','0:0:0:0:0:0:0:1','2026-08-26 20:18:08'),(6,2,'registrar','ROLE_REGISTRAR','Updated subject SMOKE-101 (renamed to SMOKE-101-RENAMED)','0:0:0:0:0:0:0:1','2026-08-26 20:18:21'),(7,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-26 21:56:22'),(8,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 00:09:09'),(9,2,'registrar','ROLE_REGISTRAR','Assigned trainer Santos, Carlos to subject BPP-101','0:0:0:0:0:0:0:1','2026-08-27 00:09:17'),(10,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 00:57:21'),(11,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 01:17:44'),(12,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 01:25:43'),(13,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 01:45:47'),(14,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 12:56:33'),(15,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-08-27 13:33:38'),(16,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-08-27 13:34:59'),(17,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-08-27 13:35:22'),(18,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-08-27 13:37:51'),(19,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 13:38:14'),(20,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 13:42:58'),(21,2,'registrar','ROLE_REGISTRAR','Generated document \'STU-2024-001-TOR.html\' (Transcript of Records (TOR)) for student STU-2024-001','0:0:0:0:0:0:0:1','2026-08-27 14:00:35'),(22,2,'registrar','ROLE_REGISTRAR','Downloaded document \'TOR-Reyes Anna.docx\' (Transcript of Records (TOR)) of student STU-2024-001','0:0:0:0:0:0:0:1','2026-08-27 14:00:40'),(23,2,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-08-27 14:01:36'),(24,3,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-08-27 14:01:42'),(25,3,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-08-27 14:02:21'),(26,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-08-27 15:32:52'),(27,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-08-27 15:56:52'),(28,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-08-27 16:01:17'),(29,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 16:01:27'),(30,2,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-08-27 16:03:50'),(31,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-08-27 16:04:21'),(32,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-08-27 16:24:31'),(33,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-27 16:24:37'),(34,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-29 01:34:36'),(35,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-29 01:46:44'),(36,2,'registrar','ROLE_REGISTRAR','Created class: Bread Making Fundamentals in Section A 2026 (Semester 2026)','0:0:0:0:0:0:0:1','2026-08-29 01:47:17'),(37,2,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-08-29 01:50:18'),(38,3,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-08-29 01:50:23'),(39,3,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-08-29 23:26:28'),(40,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-29 23:26:28'),(41,2,'registrar','ROLE_REGISTRAR','Enrolled student STU-2026-001 in class #7','0:0:0:0:0:0:0:1','2026-08-29 23:26:52'),(42,3,'trainer','ROLE_TRAINER','Saved grades for class #7 (1 student(s))','0:0:0:0:0:0:0:1','2026-08-29 23:26:53'),(43,3,'trainer','ROLE_TRAINER','Saved grades for class #7 (1 student(s))','0:0:0:0:0:0:0:1','2026-08-29 23:26:53'),(44,3,'trainer','ROLE_TRAINER','Saved grades for class #7 (1 student(s))','0:0:0:0:0:0:0:1','2026-08-29 23:27:06'),(45,3,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-08-29 23:50:10'),(46,3,'trainer','ROLE_TRAINER','User logged out','0:0:0:0:0:0:0:1','2026-08-29 23:50:22'),(47,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-08-29 23:50:28'),(48,2,'registrar','ROLE_REGISTRAR','Enrolled student STU-2026-001 in class #7','0:0:0:0:0:0:0:1','2026-08-29 23:50:39'),(49,2,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-08-29 23:50:43'),(50,3,'trainer','ROLE_TRAINER','User logged in','0:0:0:0:0:0:0:1','2026-08-29 23:50:55'),(51,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-09-02 18:38:19'),(52,2,'registrar','ROLE_REGISTRAR','Deleted student record: Velasco, Emmanuel (ID: SR20260001)','0:0:0:0:0:0:0:1','2026-09-02 18:38:37'),(53,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-14 00:05:35'),(54,1,'admin','ROLE_ADMIN','Deactivated account: registrar','0:0:0:0:0:0:0:1','2026-09-14 00:05:56'),(55,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 01:10:02'),(56,1,'admin','ROLE_ADMIN','Re-enabled account: registrar','0:0:0:0:0:0:0:1','2026-09-19 01:10:08'),(57,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 01:14:34'),(58,1,'admin','ROLE_ADMIN','Completed security question setup','0:0:0:0:0:0:0:1','2026-09-19 01:15:05'),(59,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 01:16:42'),(60,1,'admin','ROLE_ADMIN','Completed security question setup','0:0:0:0:0:0:0:1','2026-09-19 01:17:25'),(61,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-09-19 01:17:29'),(62,1,'admin','ROLE_ADMIN','Password reset via security questions','0:0:0:0:0:0:0:1','2026-09-19 01:18:21'),(63,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-09-19 01:18:24'),(64,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 01:18:46'),(65,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 08:19:35'),(66,1,'admin','ROLE_ADMIN','Updated \'forgot password\' security questions','0:0:0:0:0:0:0:1','2026-09-19 08:20:18'),(67,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-09-19 08:21:52'),(68,1,'admin','ROLE_ADMIN','Password reset via security questions','0:0:0:0:0:0:0:1','2026-09-19 08:22:25'),(69,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 08:31:21'),(70,1,'admin','ROLE_ADMIN','Updated \'forgot password\' security questions','0:0:0:0:0:0:0:1','2026-09-19 08:31:47'),(71,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-09-19 08:31:54'),(72,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 08:36:33'),(73,1,'admin','ROLE_ADMIN','Updated \'forgot password\' security questions','0:0:0:0:0:0:0:1','2026-09-19 08:37:06'),(74,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 08:47:00'),(75,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 08:53:09'),(76,1,'admin','ROLE_ADMIN','Updated \'forgot password\' security questions','0:0:0:0:0:0:0:1','2026-09-19 08:53:28'),(77,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 08:59:41'),(78,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 09:05:46'),(79,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-09-19 09:09:34'),(80,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 09:10:43'),(81,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-09-19 09:10:53'),(82,1,'admin','ROLE_ADMIN','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 09:18:28'),(83,1,'admin','ROLE_ADMIN','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 09:18:29'),(84,1,'admin','ROLE_ADMIN','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 17:15:05'),(85,1,'admin','ROLE_ADMIN','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 17:15:05'),(86,1,'admin','ROLE_ADMIN','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 17:15:23'),(87,1,'admin','ROLE_ADMIN','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 17:15:24'),(88,1,'admin','ROLE_ADMIN','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 17:15:24'),(89,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:05:10'),(90,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-09-19 21:06:32'),(91,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:06:42'),(92,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:06:48'),(93,2,'registrar','ROLE_REGISTRAR','Completed security question setup','0:0:0:0:0:0:0:1','2026-09-19 21:07:04'),(94,2,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-09-19 21:07:20'),(95,2,'registrar','ROLE_REGISTRAR','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:07:28'),(96,2,'registrar','ROLE_REGISTRAR','User logged out','0:0:0:0:0:0:0:1','2026-09-19 21:07:45'),(97,2,'registrar','ROLE_REGISTRAR','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 21:08:02'),(98,2,'registrar','ROLE_REGISTRAR','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 21:08:03'),(99,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:08:19'),(100,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:13:37'),(101,1,'admin','ROLE_ADMIN','Unlocked account: registrar','0:0:0:0:0:0:0:1','2026-09-19 21:13:41'),(102,1,'admin','ROLE_ADMIN','Deactivated account: registrar','0:0:0:0:0:0:0:1','2026-09-19 21:13:45'),(103,1,'admin','ROLE_ADMIN','Re-enabled account: registrar','0:0:0:0:0:0:0:1','2026-09-19 21:13:49'),(104,1,'admin','ROLE_ADMIN','User logged out','0:0:0:0:0:0:0:1','2026-09-19 21:13:53'),(105,2,'registrar','ROLE_REGISTRAR','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 21:14:08'),(106,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:14:16'),(107,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:22:37'),(108,1,'admin','ROLE_ADMIN','Unlocked account: registrar','0:0:0:0:0:0:0:1','2026-09-19 21:22:50'),(109,2,'registrar','ROLE_REGISTRAR','Account locked due to repeated failed security question attempts','0:0:0:0:0:0:0:1','2026-09-19 21:39:38'),(110,1,'admin','ROLE_ADMIN','User logged in','0:0:0:0:0:0:0:1','2026-09-19 21:39:56'),(111,1,'admin','ROLE_ADMIN','Unlocked account: registrar','0:0:0:0:0:0:0:1','2026-09-19 21:40:15');
/*!40000 ALTER TABLE `system_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_security_answers`
--

DROP TABLE IF EXISTS `user_security_answers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_security_answers` (
  `answer_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `question_id` int DEFAULT NULL,
  `custom_question` varchar(255) DEFAULT NULL,
  `answer_hash` varchar(255) NOT NULL,
  `slot` tinyint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`answer_id`),
  UNIQUE KEY `uq_user_slot` (`user_id`,`slot`),
  UNIQUE KEY `uq_user_question` (`user_id`,`question_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `user_security_answers_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `user_security_answers_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `security_questions` (`question_id`),
  CONSTRAINT `chk_question_xor_custom` CHECK (((`question_id` is null) <> (`custom_question` is null)))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_security_answers`
--

LOCK TABLES `user_security_answers` WRITE;
/*!40000 ALTER TABLE `user_security_answers` DISABLE KEYS */;
INSERT INTO `user_security_answers` VALUES (11,1,1,NULL,'$2a$10$geYtXhy5ZUBzWnK3jYq7Wu4RTsPnYgOl2DxM2xGAEvueJaCqaL0zu',1,'2026-09-19 08:53:28'),(12,1,4,NULL,'$2a$10$j0Vi.Yai9ZS7wZxi9VPbGewhxJwgH8RZcqu82qN8ckUk97G0DrY2S',2,'2026-09-19 08:53:28'),(13,2,1,NULL,'$2a$10$L54yjVz/Eg/Hc/LFd53Mi.w31bkOkOXRoV6ApMfF6XKHlkEMZcZf.',1,'2026-09-19 21:07:03'),(14,2,4,NULL,'$2a$10$0.X2ssBjkhJiJl1vQ64trOQ2zWuwLwR6OOOLKCIl2f.G8CqyAq3Vq',2,'2026-09-19 21:07:04');
/*!40000 ALTER TABLE `user_security_answers` ENABLE KEYS */;
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
  `security_locked` tinyint(1) NOT NULL DEFAULT '0',
  `failed_security_attempts` tinyint NOT NULL DEFAULT '0',
  `security_lockout_started_at` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `uq_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','$2a$10$O1QG742OW82yFhOpAaAXKOu4Sky0X0KcAs2R6ABj0YIgIp/cJIw2C','Dela Cruz','Juan','Santos','1995-06-15',31,'admin@anihan.local','ROLE_ADMIN',1,'2026-09-19 08:22:25',0,0,NULL),(2,'registrar','$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6','Reyes','Maria','Garcia','1990-03-22',36,'registrar@anihan.local','ROLE_REGISTRAR',1,NULL,0,0,NULL),(3,'trainer','$2a$10$MN4FaQaQ0DaFVFHVHQ8WceI4VPzaXmZqOhcF1fai.Rr7Jbude9kz6','Santos','Carlos','Mendoza','1988-11-08',37,'trainer@anihan.local','ROLE_TRAINER',1,NULL,0,0,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'AnihanSRMS'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-19 15:58:29
