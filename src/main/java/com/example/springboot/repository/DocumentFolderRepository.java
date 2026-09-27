package com.example.springboot.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
}
