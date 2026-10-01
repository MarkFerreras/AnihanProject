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
import com.example.springboot.dto.registrar.DocumentAuditContext;
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
import com.example.springboot.dto.registrar.DocumentExportScope;
import com.example.springboot.repository.DocumentContentRepository;
import com.example.springboot.repository.SystemLogRepository;
import com.example.springboot.service.DocumentExportService;
import com.example.springboot.service.DocumentService;

import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @Autowired private DocumentService documentService;
    @Autowired private DocumentExportService documentExportService;
    @Autowired private DocumentContentRepository documentContentRepository;
    @MockitoSpyBean private SystemLogRepository systemLogRepository;

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
        jdbcTemplate.update("DELETE FROM system_logs");
        recordingJdbcTemplate().clear();
    }

    private DocumentAuditContext sampleAudit() {
        return new DocumentAuditContext(1, "registrar", "ROLE_REGISTRAR", "127.0.0.1");
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

    // -------------------------------------------------------
    // Atomic bulk upload — real transaction commit/rollback
    // -------------------------------------------------------

    @Test
    void uploadBatchCommitsAllDocumentsAndExactlyOneAuditLogOnSuccess() {
        StudentRecord student = newStudent("SR20260001", "Dela Cruz", "Maria", null, null);
        studentRecordRepository.save(student);

        var f1 = new org.springframework.mock.web.MockMultipartFile(
                "files", "a.pdf", "application/pdf", "aaa".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var f2 = new org.springframework.mock.web.MockMultipartFile(
                "files", "b.pdf", "application/pdf", "bbb".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        documentService.uploadBatch("SR20260001",
                List.of("Transcript of Records (TOR)", "PSA Birth Certificate"),
                List.of(f1, f2), sampleAudit());

        List<com.example.springboot.dto.registrar.DocumentSummaryResponse> docs =
                documentRepository.findSummariesByStudentId("SR20260001");
        assertEquals(2, docs.size());
        assertEquals(1, systemLogRepository.count());
    }

    @Test
    void uploadBatchRollsBackEverythingWhenASubsequentFileFailsToRead() throws Exception {
        StudentRecord student = newStudent("SR20260001", "Dela Cruz", "Maria", null, null);
        studentRecordRepository.save(student);

        var f1 = new org.springframework.mock.web.MockMultipartFile(
                "files", "a.pdf", "application/pdf", "aaa".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getSize()).thenReturn(3L);
        when(broken.getOriginalFilename()).thenReturn("broken.pdf");
        when(broken.getBytes()).thenThrow(new java.io.IOException("disk failure on second file"));

        assertThrows(java.io.UncheckedIOException.class, () -> documentService.uploadBatch("SR20260001",
                List.of("Transcript of Records (TOR)", "PSA Birth Certificate"),
                List.of(f1, broken), sampleAudit()));

        List<com.example.springboot.dto.registrar.DocumentSummaryResponse> docs =
                documentRepository.findSummariesByStudentId("SR20260001");
        assertTrue(docs.isEmpty(), "the first file's insert must roll back too");
        assertEquals(0, systemLogRepository.count());
    }

    @Test
    void uploadBatchRollsBackEverythingWhenAuditPersistenceFails() {
        StudentRecord student = newStudent("SR20260001", "Dela Cruz", "Maria", null, null);
        studentRecordRepository.save(student);

        doThrow(new RuntimeException("audit table unavailable")).when(systemLogRepository).save(any());

        var f1 = new org.springframework.mock.web.MockMultipartFile(
                "files", "a.pdf", "application/pdf", "aaa".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThrows(RuntimeException.class, () -> documentService.uploadBatch("SR20260001",
                List.of("Transcript of Records (TOR)"), List.of(f1), sampleAudit()));

        List<com.example.springboot.dto.registrar.DocumentSummaryResponse> docs =
                documentRepository.findSummariesByStudentId("SR20260001");
        assertTrue(docs.isEmpty(), "documents must roll back when the audit write fails");
    }

    // -------------------------------------------------------
    // ZIP export scope SQL and content-byte copying
    // -------------------------------------------------------

    @Test
    void sectionScopeIncludesAssignedStudentRegardlessOfOwnMismatchedBatch() {
        Batch sectionBatch = batchRepository.save(new Batch("B2026A", (short) 2026));
        Batch ownBatch = batchRepository.save(new Batch("B2025A", (short) 2025));
        Course course = courseRepository.save(new Course("C1", "Culinary Arts and Restaurant Services"));
        Section section = new Section();
        section.setSectionCode("S1");
        section.setSection("Section 1");
        section.setBatch(sectionBatch);
        section.setCourse(course);
        sectionRepository.save(section);

        // Assigned to S1 (batch B2026A) but the student's own batch is B2025A.
        StudentRecord mismatched = newStudent("SR20260001", "Dela Cruz", "Maria", ownBatch, section);
        studentRecordRepository.save(mismatched);
        saveDocument(mismatched, "Transcript of Records (TOR)", "tor.pdf");

        var rows = documentFolderRepository.findExportRows(DocumentExportScope.SECTION, "S1");
        assertEquals(1, rows.size());
        assertEquals("SR20260001", rows.get(0).studentId());
    }

    @Test
    void unassignedScopeUsesStudentsOwnBatchNotAnySectionBatch() {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        StudentRecord unassigned = newStudent("SR20260002", "Santos", "Juan", batch, null);
        StudentRecord otherBatch = newStudent("SR20260003", "Reyes", "Ana", batchRepository.save(
                new Batch("B2025A", (short) 2025)), null);
        studentRecordRepository.saveAll(List.of(unassigned, otherBatch));
        saveDocument(unassigned, "OJT Report", "ojt.pdf");
        saveDocument(otherBatch, "OJT Report", "other.pdf");

        var rows = documentFolderRepository.findExportRows(DocumentExportScope.UNASSIGNED, "B2026A");

        assertEquals(1, rows.size());
        assertEquals("SR20260002", rows.get(0).studentId());
    }

    @Test
    void batchScopeCombinesSectionAssignedAndUnassignedStudentsOfThatBatchOnly() {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        Batch otherBatch = batchRepository.save(new Batch("B2025A", (short) 2025));
        Course course = courseRepository.save(new Course("C1", "Culinary Arts and Restaurant Services"));
        Section section = new Section();
        section.setSectionCode("S1");
        section.setSection("Section 1");
        section.setBatch(batch);
        section.setCourse(course);
        sectionRepository.save(section);

        StudentRecord assigned = newStudent("SR1", "Dela Cruz", "Maria", batch, section);
        StudentRecord unassignedSameBatch = newStudent("SR2", "Santos", "Juan", batch, null);
        StudentRecord unassignedOtherBatch = newStudent("SR3", "Reyes", "Ana", otherBatch, null);
        studentRecordRepository.saveAll(List.of(assigned, unassignedSameBatch, unassignedOtherBatch));
        saveDocument(assigned, "Transcript of Records (TOR)", "a.pdf");
        saveDocument(unassignedSameBatch, "OJT Report", "b.pdf");
        saveDocument(unassignedOtherBatch, "OJT Report", "c.pdf");

        var rows = documentFolderRepository.findExportRows(DocumentExportScope.BATCH, "B2026A");
        var studentIds = rows.stream().map(DocumentFolderRepository.ExportRow::studentId)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(java.util.Set.of("SR1", "SR2"), studentIds);
    }

    @Test
    void documentContentRepositoryCopiesTheExactStoredBytes() throws Exception {
        StudentRecord student = newStudent("SR1", "Dela Cruz", "Maria", null, null);
        studentRecordRepository.save(student);
        byte[] original = "the exact original bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Document doc = new Document();
        doc.setStudent(student);
        doc.setDocumentType("Transcript of Records (TOR)");
        doc.setFileName("tor.pdf");
        doc.setFileType("application/pdf");
        doc.setFileSize(original.length);
        doc.setContentData(original);
        Document saved = documentRepository.save(doc);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        documentContentRepository.copyTo(saved.getDocumentId(), out);

        assertTrue(java.util.Arrays.equals(original, out.toByteArray()));
    }

    @Test
    void exportEndToEndProducesAZipWithTheStoredBytesForStudentScope() throws Exception {
        StudentRecord student = newStudent("SR1", "Dela Cruz", "Maria", null, null);
        studentRecordRepository.save(student);
        byte[] content = "real stored content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Document doc = new Document();
        doc.setStudent(student);
        doc.setDocumentType("Transcript of Records (TOR)");
        doc.setFileName("tor.pdf");
        doc.setFileType("application/pdf");
        doc.setFileSize(content.length);
        doc.setContentData(content);
        documentRepository.save(doc);

        var prepared = documentExportService.prepareExport(DocumentExportScope.STUDENT, "SR1");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        documentExportService.writeZip(prepared, out);

        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(out.toByteArray()))) {
            var entry = zis.getNextEntry();
            assertEquals("tor.pdf", entry.getName());
            assertTrue(java.util.Arrays.equals(content, zis.readAllBytes()));
        }
    }

    // -------------------------------------------------------
    // Pre-export missing-documents check (real SQL)
    // -------------------------------------------------------

    @Test
    void checkScopesMatchExportMembershipAndIncludeZeroDocumentStudents() {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        Batch otherBatch = batchRepository.save(new Batch("B2025A", (short) 2025));
        Course course = courseRepository.save(new Course("C1", "Culinary Arts and Restaurant Services"));
        Section section = new Section();
        section.setSectionCode("S1");
        section.setSection("Section 1");
        section.setBatch(batch);
        section.setCourse(course);
        sectionRepository.save(section);

        // In S1 although the student's own batch is B2025A — section membership wins, as in export.
        StudentRecord inSectionMismatched = newStudent("SR1", "Dela Cruz", "Maria", otherBatch, section);
        // Unassigned, own batch B2026A, and NO documents at all.
        StudentRecord unassignedSameBatch = newStudent("SR2", "Santos", "Juan", batch, null);
        StudentRecord unassignedOtherBatch = newStudent("SR3", "Reyes", "Ana", otherBatch, null);
        studentRecordRepository.saveAll(List.of(inSectionMismatched, unassignedSameBatch, unassignedOtherBatch));
        saveDocument(inSectionMismatched, "PSA Birth Certificate", "psa.pdf");
        saveDocument(inSectionMismatched, "Form 137", "f137.pdf");
        saveDocument(unassignedOtherBatch, "OJT Report", "ojt.pdf");

        assertEquals(java.util.Set.of("SR1"), checkStudentIds(DocumentExportScope.SECTION, "S1"));
        assertEquals(java.util.Set.of("SR2"), checkStudentIds(DocumentExportScope.UNASSIGNED, "B2026A"));
        assertEquals(java.util.Set.of("SR1", "SR2"), checkStudentIds(DocumentExportScope.BATCH, "B2026A"));
        assertEquals(java.util.Set.of("SR3"), checkStudentIds(DocumentExportScope.STUDENT, "SR3"));

        // Parity: the check covers exactly the students the ZIP covers (those with >= 1 document).
        assertCheckMatchesExport(DocumentExportScope.SECTION, "S1");
        assertCheckMatchesExport(DocumentExportScope.UNASSIGNED, "B2026A");
        assertCheckMatchesExport(DocumentExportScope.BATCH, "B2026A");
        assertCheckMatchesExport(DocumentExportScope.STUDENT, "SR3");
    }

    private void assertCheckMatchesExport(DocumentExportScope scope, String key) {
        java.util.Set<String> exported = documentFolderRepository.findExportRows(scope, key).stream()
                .map(DocumentFolderRepository.ExportRow::studentId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> checkedWithDocuments = documentFolderRepository.findCheckRows(scope, key).stream()
                .filter(row -> row.documentId() != null)
                .map(DocumentFolderRepository.CheckRow::studentId)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(exported, checkedWithDocuments, "check/export membership differs for " + scope + " " + key);
    }

    @Test
    void checkMissingOnRealDatabaseCountsDocumentsAndFlagsZeroDocumentStudent() {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        StudentRecord complete = newStudent("SR1", "Abad", "Ana", batch, null);
        StudentRecord empty = newStudent("SR2", "Bautista", "Bea", batch, null);
        studentRecordRepository.saveAll(List.of(complete, empty));
        saveDocument(complete, "PSA Birth Certificate", "psa.pdf");
        saveDocument(complete, "Form 137", "f137.pdf");
        saveDocument(complete, "ID Picture (1x1 / 2x2)", "id.png");
        saveDocument(complete, "Others", "extra.pdf");

        var result = documentExportService.checkMissing(DocumentExportScope.UNASSIGNED, "B2026A");

        assertEquals(2, result.studentsInScope());
        assertEquals(1, result.flagged().size());
        var flagged = result.flagged().get(0);
        assertEquals("SR2", flagged.studentId());
        assertEquals(0, flagged.documentCount());
        assertEquals(List.of("PSA Birth Certificate", "Form 137", "ID Picture (1x1 / 2x2)"), flagged.missing());
    }

    private java.util.Set<String> checkStudentIds(DocumentExportScope scope, String key) {
        return documentFolderRepository.findCheckRows(scope, key).stream()
                .map(DocumentFolderRepository.CheckRow::studentId)
                .collect(java.util.stream.Collectors.toSet());
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
