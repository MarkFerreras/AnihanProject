package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.example.springboot.service.StudentNumberSheetParser.ParsedRow;

/**
 * The most important suite in this feature: the sheet format is expected to change once
 * the school's real records arrive, and these tests are what will say whether an edit to
 * {@link StudentNumberImportMapping} still parses correctly.
 */
class StudentNumberSheetParserTest {

    private final StudentNumberSheetParser parser = new StudentNumberSheetParser();

    private byte[] csv(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    // ----- Header detection -----

    @Test
    void parsesACleanExportedSheet() {
        List<ParsedRow> rows = parser.parse(csv("""
                Reference No.,Record ID,Last Name,First Name,Student Number
                SR20260001,6,Ligan,Sean,2026-001
                SR20260002,7,Reyes,Anna,2026-002
                """), "csv");

        assertEquals(2, rows.size());
        assertEquals("SR20260001", rows.get(0).reference());
        assertEquals("2026-001", rows.get(0).studentNumber());
        assertEquals("Ligan", rows.get(0).lastName());
        assertEquals("Sean", rows.get(0).firstName());
        assertEquals(2, rows.get(0).rowNumber(), "Row numbers are 1-based to match Excel");
    }

    /** School-supplied files routinely carry a title block above the real header. */
    @Test
    void findsTheHeaderBelowTitleRows() {
        List<ParsedRow> rows = parser.parse(csv("""
                ANIHAN TECHNICAL SCHOOL
                Student Masterlist — Batch 2026

                Reference No.,Last Name,Student Number
                SR20260001,Ligan,2026-001
                """), "csv");

        assertEquals(1, rows.size());
        assertEquals("SR20260001", rows.get(0).reference());
        assertEquals(5, rows.get(0).rowNumber());
    }

    @Test
    void acceptsAliasSpellingsAndIgnoresPunctuationAndCase() {
        List<ParsedRow> rows = parser.parse(csv("""
                STUDENT ID,student no
                SR20260001,2026-001
                """), "csv");

        assertEquals(1, rows.size());
        assertEquals("SR20260001", rows.get(0).reference());
        assertEquals("2026-001", rows.get(0).studentNumber());
    }

    @Test
    void doesNotCareAboutColumnOrder() {
        List<ParsedRow> rows = parser.parse(csv("""
                Student Number,First Name,Last Name,Reference No.
                2026-001,Sean,Ligan,SR20260001
                """), "csv");

        assertEquals("SR20260001", rows.get(0).reference());
        assertEquals("2026-001", rows.get(0).studentNumber());
        assertEquals("Ligan", rows.get(0).lastName());
    }

    @Test
    void nameColumnsAreOptional() {
        List<ParsedRow> rows = parser.parse(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                """), "csv");

        assertEquals(1, rows.size());
        assertNull(rows.get(0).lastName());
        assertNull(rows.get(0).firstName());
    }

    @Test
    void reportsAMissingStudentNumberColumnClearly() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(csv("""
                        Reference No.,Last Name
                        SR20260001,Ligan
                        """), "csv"));

        assertTrue(ex.getMessage().contains("Student Number"), ex.getMessage());
    }

    @Test
    void reportsAnUnrecognisableSheetClearly() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(csv("""
                        Colour,Shape
                        red,round
                        """), "csv"));

        assertTrue(ex.getMessage().contains("header row"), ex.getMessage());
    }

    // ----- Value handling -----

    @Test
    void trailingBlankRowsAreIgnored() {
        List<ParsedRow> rows = parser.parse(csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                ,
                ,
                """), "csv");

        assertEquals(1, rows.size());
    }

    @Test
    void blankStudentNumberIsNullNotEmptyString() {
        List<ParsedRow> rows = parser.parse(csv("""
                Reference No.,Student Number
                SR20260001,
                """), "csv");

        assertEquals(1, rows.size());
        assertNull(rows.get(0).studentNumber());
    }

    @Test
    void handlesQuotedFieldsContainingCommas() {
        List<ParsedRow> rows = parser.parse(csv("""
                Reference No.,Last Name,Student Number
                SR20260001,"Ligan, Jr.",2026-001
                """), "csv");

        assertEquals("Ligan, Jr.", rows.get(0).lastName());
        assertEquals("2026-001", rows.get(0).studentNumber());
    }

    @Test
    void stripsAUtf8ByteOrderMarkFromTheFirstHeader() {
        List<ParsedRow> rows = parser.parse(csv("﻿Reference No.,Student Number\nSR20260001,2026-001\n"), "csv");

        assertEquals(1, rows.size());
        assertEquals("SR20260001", rows.get(0).reference());
    }

    @Test
    void trimsSurroundingWhitespace() {
        List<ParsedRow> rows = parser.parse(csv("""
                Reference No.,Student Number
                  SR20260001  ,  2026-001
                """), "csv");

        assertEquals("SR20260001", rows.get(0).reference());
        assertEquals("2026-001", rows.get(0).studentNumber());
    }

    // ----- XLSX -----

    @Test
    void readsAnXlsxSheet() throws Exception {
        byte[] xlsx = workbook(sheet -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Reference No.");
            header.createCell(1).setCellValue("Student Number");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("SR20260001");
            row.createCell(1).setCellValue("2026-001");
        });

        List<ParsedRow> rows = parser.parse(xlsx, "xlsx");

        assertEquals(1, rows.size());
        assertEquals("SR20260001", rows.get(0).reference());
        assertEquals("2026-001", rows.get(0).studentNumber());
    }

    /** Excel stores a bare number as a double; it must not come back as "2026001.0". */
    @Test
    void numericCellsDoNotPickUpADecimalTail() throws Exception {
        byte[] xlsx = workbook(sheet -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Reference No.");
            header.createCell(1).setCellValue("Student Number");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("SR20260001");
            row.createCell(1).setCellValue(2026001d);
        });

        assertEquals("2026001", parser.parse(xlsx, "xlsx").get(0).studentNumber());
    }

    /** "0012" is a legitimate student number; the text cell style must preserve it. */
    @Test
    void leadingZerosSurviveInTextCells() throws Exception {
        byte[] xlsx = workbook(sheet -> {
            XSSFWorkbook wb = sheet.getWorkbook();
            CellStyle text = wb.createCellStyle();
            text.setDataFormat(wb.createDataFormat().getFormat("@"));

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Reference No.");
            header.createCell(1).setCellValue("Student Number");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("SR20260001");
            Cell cell = row.createCell(1);
            cell.setCellValue("0012");
            cell.setCellStyle(text);
        });

        assertEquals("0012", parser.parse(xlsx, "xlsx").get(0).studentNumber());
    }

    @Test
    void skipsRowsWithGapsWithoutLosingLaterRows() throws Exception {
        byte[] xlsx = workbook(sheet -> {
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Reference No.");
            header.createCell(1).setCellValue("Student Number");
            sheet.createRow(1); // completely empty row in the middle
            Row row = sheet.createRow(2);
            row.createCell(0).setCellValue("SR20260002");
            row.createCell(1).setCellValue("2026-002");
        });

        List<ParsedRow> rows = parser.parse(xlsx, "xlsx");
        assertEquals(1, rows.size());
        assertEquals("SR20260002", rows.get(0).reference());
    }

    // ----- Guards -----

    @Test
    void rejectsAnEmptyFile() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new byte[0], "csv"));
    }

    @Test
    void rejectsAnUnsupportedExtension() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(csv("anything"), "pdf"));
        assertTrue(ex.getMessage().contains("csv, xlsx"), ex.getMessage());
    }

    // ----- helpers -----

    private interface SheetBuilder {
        void build(XSSFSheet sheet);
    }

    private byte[] workbook(SheetBuilder builder) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            builder.build(wb.createSheet("Sheet1"));
            wb.write(out);
            return out.toByteArray();
        }
    }
}
