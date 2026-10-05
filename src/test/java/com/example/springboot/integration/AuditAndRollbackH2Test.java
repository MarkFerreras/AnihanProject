package com.example.springboot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.registrar.DocumentExportScope;
import com.example.springboot.dto.registrar.StudentNumberImportOutcome;
import com.example.springboot.dto.registrar.StudentNumberImportReport;
import com.example.springboot.dto.student.ParentDto;
import com.example.springboot.dto.student.StudentDetailsRequest;
import com.example.springboot.dto.student.StudentDetailsResponse;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Document;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.DocumentContentRepository;
import com.example.springboot.repository.DocumentFolderRepository;
import com.example.springboot.repository.DocumentRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.service.DocumentExportService;
import com.example.springboot.service.StudentDetailsService;
import com.example.springboot.service.StudentNumberImportService;
import com.example.springboot.service.StudentNumberSheetParser;
import com.example.springboot.service.SystemLogQueryResult;
import com.example.springboot.service.SystemLogService;

/**
 * Cross-cutting H2 integration checks for transactional rollback, concurrent id allocation,
 * log-range performance and the 160-student export (Task 14).
 *
 * <p>ISO 25010 characteristic: Reliability (fault tolerance, recoverability) and Performance
 * efficiency (time behaviour).
 *
 * <p>The class runs with {@code NOT_SUPPORTED} so the services' own {@code @Transactional}
 * boundaries are the real ones: nothing is wrapped in a test transaction that would be rolled
 * back for us. Rows are therefore really committed and are deleted in {@link #cleanUp()}.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({StudentDetailsService.class, StudentNumberImportService.class, StudentNumberSheetParser.class,
        SystemLogService.class, DocumentExportService.class, DocumentFolderRepository.class,
        DocumentContentRepository.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auditRollbackTestDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS \"year\" AS SMALLINT",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AuditAndRollbackH2Test {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StudentDetailsService studentDetailsService;
    @Autowired private StudentNumberImportService importService;
    @Autowired private SystemLogService systemLogService;
    @Autowired private DocumentExportService documentExportService;
    @Autowired private BatchRepository batchRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentContentRepository documentContentRepository;
    @MockitoSpyBean private ParentRepository parentRepository;
    @MockitoSpyBean private StudentRecordRepository studentRecordRepository;

    @AfterEach
    void cleanUp() {
        Mockito.reset(parentRepository, studentRecordRepository);
        for (String table : List.of("documents", "parents", "other_guardians", "student_education",
                "student_school_years", "student_records", "batches", "system_logs")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    private long count(String table) {
        Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return n == null ? 0 : n;
    }

    private StudentRecord newStudent(String studentId, String last, String first) {
        StudentRecord r = new StudentRecord();
        r.setStudentId(studentId);
        r.setLastName(last);
        r.setFirstName(first);
        r.setMiddleName("H2");
        r.setBaptized(false);
        r.setStudentStatus("Active");
        return r;
    }

    // ---------------------------------------------------------------- T14-01

    @Test
    void enrollmentFailureMidwayLeavesNoPartialRows() {
        StudentDetailsResponse started = studentDetailsService.startOrResume("Santos", "Ana", "Reyes");
        String studentId = started.studentId();

        // Child save fails after the parent record has already been updated inside the transaction.
        Mockito.doThrow(new IllegalStateException("simulated child save failure"))
                .when(parentRepository).save(Mockito.any());

        ParentDto father = new ParentDto("Santos", "Juan", null, null, "Farmer", null, null, null, null);
        StudentDetailsRequest req = new StudentDetailsRequest(
                "Santos", "Ana", "Reyes", "09171234567", LocalDate.of(2005, 1, 1), "Female", "Single",
                "Permanent address", null, 1, 0, 1, "Catholic", false, null, null,
                father, null, null, null, null);

        assertThrows(IllegalStateException.class, () -> studentDetailsService.submitEnrollment(studentId, req));

        assertEquals(0, count("parents"), "no child row may survive the failed submit");
        assertEquals(0, count("student_education"));
        StudentRecord stored = studentRecordRepository.findByStudentId(studentId).orElseThrow();
        assertEquals("Enrolling", stored.getStudentStatus(), "parent row must be rolled back to Enrolling");
        assertNull(stored.getContactNo(), "personal data applied before the failure must be rolled back");
        assertNull(stored.getPermanentAddress());
    }

    // ---------------------------------------------------------------- T14-02

    private MockMultipartFile csv(String body) {
        return new MockMultipartFile("file", "sheet.csv", "text/csv", body.getBytes(StandardCharsets.UTF_8));
    }

    private void seedThreeStudents() {
        studentRecordRepository.save(newStudent("SR20260001", "Abad", "Ana"));
        studentRecordRepository.save(newStudent("SR20260002", "Bautista", "Bea"));
        studentRecordRepository.save(newStudent("SR20260003", "Cruz", "Cia"));
    }

    @Test
    void importApplyFailureRollsBackAllRows() {
        seedThreeStudents();
        // The third row's write fails; the two earlier writes happened in the same transaction.
        Mockito.doThrow(new IllegalStateException("simulated write failure on last row"))
                .when(studentRecordRepository)
                .save(Mockito.argThat((StudentRecord r) -> r != null && "SR20260003".equals(r.getStudentId())));

        MockMultipartFile file = csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                SR20260002,2026-002
                SR20260003,2026-003
                """);

        assertThrows(IllegalStateException.class, () -> importService.apply(file, false));

        Mockito.reset(studentRecordRepository);
        Long assigned = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_records WHERE student_number IS NOT NULL", Long.class);
        assertEquals(0L, assigned, "a failing apply must leave no student number written");
    }

    @Test
    void importApplyWithAnInvalidLastRowKeepsEarlierRows_currentlyPartialApply() {
        // FINDING (by design, documented on StudentNumberImportService.apply): a row that merely
        // fails validation is reported and skipped; the good rows are still written. Only an
        // exception rolls back everything (see importApplyFailureRollsBackAllRows).
        seedThreeStudents();
        MockMultipartFile file = csv("""
                Reference No.,Student Number
                SR20260001,2026-001
                SR20260002,2026-002
                SR20260003,bad number!
                """);

        StudentNumberImportReport report = importService.apply(file, false);

        assertEquals(1, report.countsByOutcome().get(StudentNumberImportOutcome.INVALID_FORMAT));
        assertEquals("2026-001", studentRecordRepository.findByStudentId("SR20260001").orElseThrow().getStudentNumber());
        assertEquals("2026-002", studentRecordRepository.findByStudentId("SR20260002").orElseThrow().getStudentNumber());
        assertNull(studentRecordRepository.findByStudentId("SR20260003").orElseThrow().getStudentNumber());
    }

    // ---------------------------------------------------------------- T14-03

    @Test
    void twoParallelEnrollmentsGetDistinctStudentIds() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (String last : List.of("Parallel-A", "Parallel-B")) {
                Callable<String> task = () -> {
                    ready.countDown();
                    go.await();
                    return studentDetailsService.startOrResume(last, "Pat", "Q").studentId();
                };
                futures.add(pool.submit(task));
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();

            List<String> ids = new ArrayList<>();
            int failures = 0;
            for (Future<String> f : futures) {
                try {
                    ids.add(f.get(30, TimeUnit.SECONDS));
                } catch (java.util.concurrent.ExecutionException e) {
                    failures++;
                    // The only acceptable failure is a unique-key rejection, never silent duplication.
                    assertTrue(e.getCause() instanceof org.springframework.dao.DataAccessException,
                            "unexpected failure type: " + e.getCause());
                }
            }

            // FINDING (conditional): generateStudentId() is read-max-then-insert with no lock, so
            // two simultaneous first-time enrolments can compute the same id and one is rejected
            // by the unique index (failures == 1). Whichever way the race goes, a duplicate id
            // must never be stored.
            assertEquals(ids.size(), ids.stream().distinct().count(), "ids handed out must be distinct");
            assertEquals(ids.size(), count("student_records"));
            assertEquals(2, ids.size() + failures);
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- T14-04

    @Test
    void logQueryOver100kRowsUsesRangeAndFinishesQuickly() {
        LocalDate firstDay = LocalDate.of(2026, 1, 1);
        List<Object[]> batch = new ArrayList<>(100_000);
        for (int i = 0; i < 100_000; i++) {
            // 1,000 rows per day across 100 days
            LocalDateTime ts = firstDay.plusDays(i / 1000).atTime(12, 0).plusSeconds(i % 1000);
            batch.add(new Object[] {1, "user" + (i % 10), "ROLE_ADMIN", "action " + i, "127.0.0.1",
                    Timestamp.valueOf(ts)});
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO system_logs (user_id, username, role, action, ip_address, timestamp) VALUES (?,?,?,?,?,?)",
                batch);
        assertEquals(100_000, count("system_logs"));

        LocalDate day = firstDay.plusDays(40);
        long start = System.nanoTime();
        SystemLogQueryResult result = systemLogService.queryLogs(null, day, day);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(1000, result.logs().size(), "only the selected day's rows may be returned");
        assertTrue(elapsedMs < 5000, "range query took " + elapsedMs + "ms");
    }

    // ---------------------------------------------------------------- T14-05

    @Test
    void zipExportOf160StudentsCompletes() throws Exception {
        Batch batch = batchRepository.save(new Batch("B2026A", (short) 2026));
        for (int i = 0; i < 160; i++) {
            StudentRecord s = newStudent(String.format("SR2026%04d", i + 1), "Last" + i, "First" + i);
            s.setBatch(batch);
            studentRecordRepository.save(s);
            Document doc = new Document();
            doc.setStudent(s);
            doc.setDocumentType("PSA Birth Certificate");
            doc.setFileName("psa-" + i + ".pdf");
            doc.setFileType("application/pdf");
            byte[] content = ("content-" + i).getBytes(StandardCharsets.UTF_8);
            doc.setFileSize(content.length);
            doc.setContentData(content);
            documentRepository.save(doc);
        }

        var prepared = documentExportService.prepareExport(DocumentExportScope.UNASSIGNED, "B2026A");
        assertEquals(160, prepared.entries().size());
        assertEquals(160, prepared.studentsInScope());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        documentExportService.writeZip(prepared, out);

        int entries = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            while (zis.getNextEntry() != null) {
                entries++;
                zis.readAllBytes();
            }
        }
        assertEquals(160, entries);
    }
}
