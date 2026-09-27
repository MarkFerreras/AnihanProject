package com.example.springboot.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.springboot.dto.registrar.DocumentExportScope;

/**
 * Scalar, BLOB-free reads backing the Documents folder explorer. Deliberately
 * uses {@link JdbcTemplate} instead of Spring Data JPA / entity loading: the
 * tree needs three flat projections (batches, sections, students with a
 * document count), never full {@code StudentRecord} entities or
 * {@code documents.content_data}.
 */
@Repository
public class DocumentFolderRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentFolderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record BatchRow(String batchCode, Short batchYear) {
    }

    public record SectionRow(String sectionCode, String sectionName, String courseName, String batchCode) {
    }

    public record StudentRow(String studentId, String studentNumber, String firstName, String lastName,
                             String studentStatus, String batchCode, String sectionCode, long documentCount) {
    }

    public record ExportRow(Integer documentId, String studentId, String studentNumber,
                            String firstName, String lastName, String sectionCode, String fileName) {
    }

    private static final String BATCH_SQL = """
            SELECT batch_code, batch_year
            FROM batches
            ORDER BY batch_year DESC, batch_code ASC
            """;

    private static final String SECTION_SQL = """
            SELECT sec.section_code, sec.section, c.course_name, sec.batch_code
            FROM sections sec
            JOIN courses c ON c.course_code = sec.course_code
            ORDER BY sec.section_code ASC
            """;

    private static final String STUDENT_SQL = """
            SELECT s.student_id, s.student_number, s.first_name, s.last_name,
                   s.student_status, s.batch_code, s.section_code,
                   COUNT(d.document_id) AS document_count
            FROM student_records s
            LEFT JOIN documents d ON d.student_id = s.student_id
            GROUP BY s.student_id, s.student_number, s.first_name, s.last_name,
                     s.student_status, s.batch_code, s.section_code
            ORDER BY s.last_name ASC, s.first_name ASC, s.student_id ASC
            """;

    public List<BatchRow> findBatchRows() {
        // getShort(), not getObject(): MySQL Connector/J returns java.sql.Date
        // for YEAR columns via getObject() by default (yearIsDateType=true),
        // which getShort() coerces correctly regardless of driver/DB.
        return jdbcTemplate.query(BATCH_SQL, (rs, rowNum) -> new BatchRow(
                rs.getString("batch_code"),
                rs.getShort("batch_year")));
    }

    public List<SectionRow> findSectionRows() {
        return jdbcTemplate.query(SECTION_SQL, (rs, rowNum) -> new SectionRow(
                rs.getString("section_code"),
                rs.getString("section"),
                rs.getString("course_name"),
                rs.getString("batch_code")));
    }

    public List<StudentRow> findStudentRows() {
        return jdbcTemplate.query(STUDENT_SQL, (rs, rowNum) -> new StudentRow(
                rs.getString("student_id"),
                rs.getString("student_number"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("student_status"),
                rs.getString("batch_code"),
                rs.getString("section_code"),
                rs.getLong("document_count")));
    }

    private static final String EXPORT_ROW_SELECT = """
            SELECT d.document_id, s.student_id, s.student_number, s.first_name, s.last_name,
                   s.section_code, d.file_name
            FROM documents d
            JOIN student_records s ON s.student_id = d.student_id
            """;

    private static final String EXPORT_STUDENT_SQL = EXPORT_ROW_SELECT
            + " WHERE s.student_id = ? ORDER BY d.document_id ASC";

    private static final String EXPORT_SECTION_SQL = EXPORT_ROW_SELECT
            + " WHERE s.section_code = ? ORDER BY s.last_name ASC, s.first_name ASC, s.student_id ASC, d.document_id ASC";

    private static final String EXPORT_UNASSIGNED_SQL = EXPORT_ROW_SELECT
            + " WHERE s.section_code IS NULL AND s.batch_code = ?"
            + " ORDER BY s.last_name ASC, s.first_name ASC, s.student_id ASC, d.document_id ASC";

    /**
     * Entire displayed batch (spec §5): assigned students via their
     * section's batch, plus unassigned students whose own batch matches —
     * the same membership rule the folder tree uses (see spec §2).
     */
    private static final String EXPORT_BATCH_SQL = EXPORT_ROW_SELECT
            + " LEFT JOIN sections sec ON sec.section_code = s.section_code"
            + " WHERE sec.batch_code = ? OR (s.section_code IS NULL AND s.batch_code = ?)"
            + " ORDER BY sec.section_code ASC, s.last_name ASC, s.first_name ASC, s.student_id ASC, d.document_id ASC";

    /** One parameterized metadata query per scope — never a per-student read. */
    public List<ExportRow> findExportRows(DocumentExportScope scope, String key) {
        String sql;
        Object[] args;
        switch (scope) {
            case STUDENT -> {
                sql = EXPORT_STUDENT_SQL;
                args = new Object[] { key };
            }
            case SECTION -> {
                sql = EXPORT_SECTION_SQL;
                args = new Object[] { key };
            }
            case UNASSIGNED -> {
                sql = EXPORT_UNASSIGNED_SQL;
                args = new Object[] { key };
            }
            case BATCH -> {
                sql = EXPORT_BATCH_SQL;
                args = new Object[] { key, key };
            }
            default -> throw new IllegalArgumentException("Unknown export scope: " + scope);
        }
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ExportRow(
                rs.getInt("document_id"),
                rs.getString("student_id"),
                rs.getString("student_number"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("section_code"),
                rs.getString("file_name")), args);
    }
}
