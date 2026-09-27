package com.example.springboot.integration;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.SpringbootApplication;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.Document;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.DocumentFolderRepository;
import com.example.springboot.repository.DocumentRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real H2 (MySQL-mode) verification for the Documents folder-explorer scalar
 * reads. Runs entirely against an in-memory database — the live MySQL
 * container is never touched — but exercises the actual SQL text through
 * {@link DocumentFolderRepository} rather than mocking it.
 */
@SpringBootTest(classes = SpringbootApplication.class)
@Import(DocumentStorageIntegrationTest.RecordingJdbcTemplateConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:documentStorageTestDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // Hibernate create-drop cannot build this schema (H2 has no MySQL YEAR
        // type, and never reproduces schema.sql's upload_date DEFAULT). Load a
        // real DDL script instead — the same ddl-auto=none as production.
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:document-storage-h2-schema.sql"
})
class DocumentStorageIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DocumentFolderRepository documentFolderRepository;
    @Autowired private StudentRecordRepository studentRecordRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private SectionRepository sectionRepository;

    @BeforeEach
    void resetFixturesBetweenTests() {
        // spring.sql.init.mode=always only re-runs the schema script once per
        // ApplicationContext, and @SpringBootTest reuses that context across
        // this class's test methods — clear rows so each test starts clean.
        jdbcTemplate.update("DELETE FROM documents");
        jdbcTemplate.update("DELETE FROM student_records");
        jdbcTemplate.update("DELETE FROM sections");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM batches");
        recordingJdbcTemplate().clear();
    }

    private RecordingJdbcTemplate recordingJdbcTemplate() {
        return (RecordingJdbcTemplate) jdbcTemplate;
    }

    @Test
    void threeScalarReadsRegardlessOfStudentCountAndNoBlobColumns() {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        Course course = courseRepository.save(new Course("C1", "Culinary Arts and Restaurant Services"));
        Section section = new Section();
        section.setSectionCode("S1");
        section.setSection("Section 1");
        section.setBatch(batch);
        section.setCourse(course);
        sectionRepository.save(section);

        StudentRecord assigned = newStudent("SR20260001", "Dela Cruz", "Maria", batch, section);
        studentRecordRepository.save(assigned);
        saveDocument(assigned, "Transcript of Records (TOR)", "tor.pdf");
        saveDocument(assigned, "PSA Birth Certificate", "psa.pdf");

        // 50 more unassigned students — the folder tree must still execute
        // exactly three SQL reads, never one per student.
        List<StudentRecord> bulk = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            bulk.add(newStudent("H2BULK-" + i, "Last" + i, "First" + i, null, null));
        }
        studentRecordRepository.saveAll(bulk);

        recordingJdbcTemplate().clear();

        List<DocumentFolderRepository.BatchRow> batches = documentFolderRepository.findBatchRows();
        List<DocumentFolderRepository.SectionRow> sections = documentFolderRepository.findSectionRows();
        List<DocumentFolderRepository.StudentRow> students = documentFolderRepository.findStudentRows();

        assertEquals(1, batches.size());
        assertEquals(1, sections.size());
        assertEquals(51, students.size());

        List<String> executed = recordingJdbcTemplate().getExecutedSql();
        assertEquals(3, executed.size(), "exactly three scalar reads, independent of student count");
        for (String sql : executed) {
            String lower = sql.toLowerCase(java.util.Locale.ROOT);
            assertFalse(lower.contains("content_data"), "must never select the document BLOB");
            assertFalse(lower.contains("profile_picture"), "must never select the student profile picture BLOB");
            assertFalse(lower.contains("select *"), "must never SELECT *");
        }
    }

    @Test
    void exactStudentIdLookupDoesNotMatchAPrefixCollidingLongerId() {
        StudentRecord shorter = newStudent("SR20260001", "Dela Cruz", "Maria", null, null);
        StudentRecord longer = newStudent("SR202600010", "Reyes", "Ana", null, null);
        studentRecordRepository.save(shorter);
        studentRecordRepository.save(longer);

        saveDocument(shorter, "Transcript of Records (TOR)", "shorter.pdf");
        saveDocument(longer, "Transcript of Records (TOR)", "longer.pdf");
        saveDocument(longer, "PSA Birth Certificate", "longer-psa.pdf");

        List<com.example.springboot.dto.registrar.DocumentSummaryResponse> shorterDocs =
                documentRepository.findSummariesByStudentId("SR20260001");

        assertEquals(1, shorterDocs.size());
        assertEquals("shorter.pdf", shorterDocs.get(0).fileName());
        assertTrue(shorterDocs.stream().allMatch(d -> "SR20260001".equals(d.studentId())));

        assertTrue(studentRecordRepository.existsByStudentId("SR20260001"));
        assertTrue(studentRecordRepository.existsByStudentId("SR202600010"));
        assertFalse(studentRecordRepository.existsByStudentId("SR2026000"));
    }

    @Test
    void folderHierarchyAggregatesRealDatabaseCounts() {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        Course course = courseRepository.save(new Course("C1", "Culinary Arts and Restaurant Services"));
        Section section = new Section();
        section.setSectionCode("S1");
        section.setSection("Section 1");
        section.setBatch(batch);
        section.setCourse(course);
        sectionRepository.save(section);

        StudentRecord assigned = newStudent("SR20260001", "Dela Cruz", "Maria", batch, section);
        StudentRecord unassignedWithBatch = newStudent("SR20260002", "Santos", "Juan", batch, null);
        StudentRecord noBatchStudent = newStudent("SR20260003", "Reyes", "Ana", null, null);
        studentRecordRepository.saveAll(List.of(assigned, unassignedWithBatch, noBatchStudent));

        saveDocument(assigned, "Transcript of Records (TOR)", "tor.pdf");
        saveDocument(assigned, "PSA Birth Certificate", "psa.pdf");
        saveDocument(unassignedWithBatch, "OJT Report", "ojt.pdf");

        var tree = documentFolderRepository.findBatchRows();
        assertEquals(1, tree.size());

        List<DocumentFolderRepository.StudentRow> studentRows = documentFolderRepository.findStudentRows();
        long noBatchCount = studentRows.stream()
                .filter(r -> r.batchCode() == null && r.sectionCode() == null)
                .count();
        assertEquals(1, noBatchCount);

        long assignedDocs = studentRows.stream()
                .filter(r -> "SR20260001".equals(r.studentId()))
                .mapToLong(DocumentFolderRepository.StudentRow::documentCount)
                .sum();
        assertEquals(2, assignedDocs);
    }

    private StudentRecord newStudent(String studentId, String lastName, String firstName,
                                     Batch batch, Section section) {
        StudentRecord student = new StudentRecord();
        student.setStudentId(studentId);
        student.setLastName(lastName);
        student.setFirstName(firstName);
        student.setBaptized(false);
        student.setStudentStatus("Active");
        student.setBatch(batch);
        student.setSection(section);
        return student;
    }

    private void saveDocument(StudentRecord student, String documentType, String fileName) {
        Document document = new Document();
        document.setStudent(student);
        document.setDocumentType(documentType);
        document.setFileName(fileName);
        document.setFileType("application/pdf");
        byte[] content = "pdf-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        document.setFileSize(content.length);
        document.setContentData(content);
        documentRepository.save(document);
    }

    /** Wraps the real JdbcTemplate to record every executed scalar query for assertion. */
    @TestConfiguration
    static class RecordingJdbcTemplateConfig {
        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new RecordingJdbcTemplate(dataSource);
        }
    }

    static class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<String> executedSql = new ArrayList<>();

        RecordingJdbcTemplate(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper) {
            executedSql.add(sql);
            return super.query(sql, rowMapper);
        }

        List<String> getExecutedSql() {
            return executedSql;
        }

        void clear() {
            executedSql.clear();
        }
    }
}
