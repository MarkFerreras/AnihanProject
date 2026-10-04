package com.example.springboot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.service.SoReadinessPolicy.Enrollment;
import com.example.springboot.service.SoReadinessPolicy.Student;

/**
 * Gathers the facts for one student's SO checklist (spec 2026-10-01 SO checklist §7) and
 * hands them to {@link SoReadinessPolicy}. Three flat {@link JdbcTemplate} reads — never
 * an entity load, never a BLOB column — written in plain SQL (no GROUP_CONCAT or window
 * functions) so they run identically on MySQL and the H2 tests. Read-only: writes no
 * audit row, like the pre-export check.
 */
@Service
public class SoChecklistService {

    /** Course and batch resolve section-first, matching the document folder-tree ownership rules. */
    private static final String STUDENT_SQL = """
            SELECT s.student_id, s.student_status, s.last_name, s.first_name, s.middle_name,
                   s.birthdate, s.sex, s.permanent_address,
                   COALESCE(sec.course_code, s.course_code) AS course_code,
                   COALESCE(sec.batch_code, s.batch_code) AS batch_code,
                   s.enrollment_date, s.completion_date, s.employment_status
            FROM student_records s
            LEFT JOIN sections sec ON sec.section_code = s.section_code
            WHERE s.record_id = ?
            """;

    private static final String DOCUMENT_SQL = """
            SELECT document_type, upload_date
            FROM documents
            WHERE student_id = ?
            """;

    /** One row per class enrollment; grade columns are NULL when the enrollment has no grade row. */
    private static final String ENROLLMENT_SQL = """
            SELECT c.subject_code, g.grade_id, g.locked, g.locked_at, g.remarks, g.grade_status
            FROM class_enrollments ce
            JOIN classes c ON c.class_id = ce.class_id
            LEFT JOIN grades g ON g.class_id = ce.class_id AND g.student_id = ce.student_id
            WHERE ce.student_id = ?
            ORDER BY c.subject_code ASC, ce.class_id ASC
            """;

    private static final RowMapper<Student> STUDENT_MAPPER = (rs, rowNum) -> new Student(
            rs.getString("student_id"),
            rs.getString("student_status"),
            rs.getString("last_name"),
            rs.getString("first_name"),
            rs.getString("middle_name"),
            rs.getObject("birthdate", LocalDate.class),
            rs.getString("sex"),
            rs.getString("permanent_address"),
            rs.getString("course_code"),
            rs.getString("batch_code"),
            rs.getObject("enrollment_date", LocalDate.class),
            rs.getObject("completion_date", LocalDate.class),
            rs.getString("employment_status"));

    private static final RowMapper<Enrollment> ENROLLMENT_MAPPER = (rs, rowNum) -> new Enrollment(
            rs.getString("subject_code"),
            rs.getObject("grade_id") != null,
            rs.getBoolean("locked"),
            rs.getObject("locked_at", LocalDateTime.class),
            rs.getString("remarks"),
            rs.getString("grade_status"));

    private record DocumentRow(String documentType, LocalDateTime uploadDate) {
    }

    private final JdbcTemplate jdbcTemplate;

    public SoChecklistService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public SoChecklistResponse checklist(Integer recordId) {
        Student student = jdbcTemplate.query(STUDENT_SQL, STUDENT_MAPPER, recordId).stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

        // Caveat: documents.upload_date is stamped by the database's NOW(), while the grade lock
        // time (locked_at) it is later compared against is stamped by the app clock. If the two
        // clocks drift, a TOR uploaded moments after locking could read as older (or newer) than
        // it is. Acceptable here: the server and the DB run on the same on-premise machine.
        Set<String> documentTypes = new HashSet<>();
        LocalDateTime newestTorUpload = null;
        List<DocumentRow> documents = jdbcTemplate.query(DOCUMENT_SQL, (rs, rowNum) -> new DocumentRow(
                rs.getString("document_type"),
                rs.getObject("upload_date", LocalDateTime.class)), student.studentId());
        for (DocumentRow document : documents) {
            documentTypes.add(document.documentType());
            if (DocumentService.TOR_TYPE.equals(document.documentType()) && document.uploadDate() != null
                    && (newestTorUpload == null || document.uploadDate().isAfter(newestTorUpload))) {
                newestTorUpload = document.uploadDate();
            }
        }

        List<Enrollment> enrollments = jdbcTemplate.query(ENROLLMENT_SQL, ENROLLMENT_MAPPER, student.studentId());
        return SoReadinessPolicy.evaluate(student, documentTypes, newestTorUpload, enrollments);
    }
}
