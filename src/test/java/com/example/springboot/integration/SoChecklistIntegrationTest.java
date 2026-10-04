package com.example.springboot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
import com.example.springboot.dto.registrar.SoChecklistItem;
import com.example.springboot.dto.registrar.SoChecklistResponse;
import com.example.springboot.service.SoChecklistService;

/**
 * Runs SoChecklistService's real SQL on H2 (MySQL mode): section-first course/batch,
 * newest-TOR selection, enrollment LEFT JOIN grades, and no BLOB columns.
 * Never touches the live MySQL database.
 */
@SpringBootTest(classes = SpringbootApplication.class)
@Import(SoChecklistIntegrationTest.RecordingJdbcTemplateConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:soChecklistTestDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:document-storage-h2-schema.sql,classpath:so-checklist-h2-schema.sql"
})
class SoChecklistIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private SoChecklistService soChecklistService;

    @BeforeEach
    void clearRows() {
        for (String table : List.of("grades", "class_enrollments", "classes", "documents",
                "student_records", "sections", "courses", "batches")) {
            jdbc.update("DELETE FROM " + table);
        }
        ((RecordingJdbcTemplate) jdbc).clear();

        jdbc.update("INSERT INTO batches (batch_code, batch_year) VALUES ('B2026A', 2026)");
        jdbc.update("INSERT INTO courses (course_code, course_name) VALUES ('CARS', 'Culinary Arts and Restaurant Services')");
        jdbc.update("INSERT INTO sections (section_code, section, batch_code, course_code) VALUES ('S1', 'Section 1', 'B2026A', 'CARS')");
    }

    @Test
    void aCompleteStudentReadsCourseAndBatchFromTheSectionAndTheNewestTor() {
        // Own course_code and batch_code are NULL: Student Information is only met if the
        // query resolves them through the section (section-first, spec §6.2).
        insertStudent("SR1", "S1", "Completed");
        document("SR1", "PSA Birth Certificate", "2026-03-01 08:00:00");
        document("SR1", "Form IX - Cookery NC II", "2026-03-01 08:00:00");
        document("SR1", "Transcript of Records (TOR)", "2026-03-10 08:00:00"); // older than the locks
        document("SR1", "Transcript of Records (TOR)", "2026-03-25 08:00:00"); // newest: after the locks
        document("SR1", "Certificate of TVET Program", "2026-03-26 08:00:00");
        document("SR1", "OJT Report", "2026-03-26 08:00:00");
        grade(enroll("SR1", "BPP-101"), "SR1", "BPP-101", "COMPETENT", null, true, "2026-03-20 09:00:00");
        grade(enroll("SR1", "COOK-102"), "SR1", "COOK-102", "COMPETENT", null, true, "2026-03-21 09:00:00");
        Integer recordId = recordId("SR1");
        ((RecordingJdbcTemplate) jdbc).clear(); // record only the service's own reads

        SoChecklistResponse result = soChecklistService.checklist(recordId);

        assertEquals("FINAL", result.stage());
        assertEquals(Boolean.TRUE, result.complete());
        assertEquals(0, result.warningCount());
        assertEquals("MET", item(result, "student_info").state());
        assertEquals("On file; all 2 grades final", item(result, "tor").detail());
        assertEquals("Unemployed", item(result, "employment_status").detail());

        List<String> sql = ((RecordingJdbcTemplate) jdbc).executedSql();
        assertEquals(3, sql.size(), "student, documents, enrollments — one read each");
        sql.forEach(statement -> {
            assertFalse(statement.contains("content_data"), statement);
            assertFalse(statement.contains("profile_picture"), statement);
        });
    }

    @Test
    void anEnrollmentWithoutAGradeRowMakesTheTorUnmet() {
        insertStudent("SR1", "S1", "Completed");
        document("SR1", "Transcript of Records (TOR)", "2026-03-25 08:00:00");
        grade(enroll("SR1", "BPP-101"), "SR1", "BPP-101", "COMPETENT", null, true, "2026-03-20 09:00:00");
        enroll("SR1", "FBS-103"); // enrolled, never graded

        SoChecklistItem tor = item(soChecklistService.checklist(recordId("SR1")), "tor");

        assertEquals("UNMET", tor.state());
        assertEquals("TOR on file, but FBS-103 has no grade", tor.detail());
    }

    @Test
    void theNewestTorIsComparedWithTheLatestLock() {
        insertStudent("SR1", "S1", "Graduated");
        document("SR1", "Transcript of Records (TOR)", "2026-03-20 12:00:00");
        grade(enroll("SR1", "BPP-101"), "SR1", "BPP-101", "COMPETENT", null, true, "2026-03-20 09:00:00");
        grade(enroll("SR1", "COOK-102"), "SR1", "COOK-102", "COMPETENT", null, true, "2026-03-21 09:00:00");

        SoChecklistItem tor = item(soChecklistService.checklist(recordId("SR1")), "tor");

        assertEquals("WARNING", tor.state());
    }

    @Test
    void aTorUploadedAfterTheLockIsNotStaleWhenTheDbClockRunsEightHoursBehindTheApp() {
        // Docker MySQL in UTC, app in UTC+8: upload_date reads 8h earlier than the app-clock locked_at.
        LocalDateTime appNow = LocalDateTime.now().withNano(0).plusHours(8);
        lockedGraduateWithTor(appNow.minusHours(2), appNow.minusHours(1).minusHours(8)); // real upload: 1h after the lock

        SoChecklistItem tor = item(serviceWithAppClockAt(appNow).checklist(recordId("SR1")), "tor");

        assertEquals("MET", tor.state());
    }

    @Test
    void aTorUploadedBeforeTheLockStillWarnsWhenTheDbClockRunsEightHoursBehindTheApp() {
        LocalDateTime appNow = LocalDateTime.now().withNano(0).plusHours(8);
        lockedGraduateWithTor(appNow.minusHours(2), appNow.minusHours(3).minusHours(8)); // real upload: 1h before the lock

        SoChecklistItem tor = item(serviceWithAppClockAt(appNow).checklist(recordId("SR1")), "tor");

        assertEquals("WARNING", tor.state());
    }

    @Test
    void withNoClockOffsetTheTorComparisonIsUnchanged() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        lockedGraduateWithTor(now.minusHours(2), now.minusHours(1)); // uploaded after the lock
        assertEquals("MET", item(serviceWithAppClockAt(now).checklist(recordId("SR1")), "tor").state());

        jdbc.update("DELETE FROM documents");
        document("SR1", "Transcript of Records (TOR)", Timestamp.valueOf(now.minusHours(3)).toString()); // before the lock
        assertEquals("WARNING", item(serviceWithAppClockAt(now).checklist(recordId("SR1")), "tor").state());
    }

    @Test
    void subMinuteClockSkewIsNotTreatedAsAnOffset() {
        // App clock 20s ahead of the DB. The upload (20s ago) is 10s older than the lock; an
        // unrounded 20s shift would flip that to "after the lock", so the offset must round to zero.
        LocalDateTime dbNow = LocalDateTime.now().withNano(0);
        lockedGraduateWithTor(dbNow.minusSeconds(10), dbNow.minusSeconds(20));

        SoChecklistItem tor = item(serviceWithAppClockAt(dbNow.plusSeconds(20)).checklist(recordId("SR1")), "tor");

        assertEquals("WARNING", tor.state());
    }

    @Test
    void anUnknownRecordIsNotFound() {
        assertThrows(NoSuchElementException.class, () -> soChecklistService.checklist(999_999));
    }

    // ----- fixtures -----

    private void insertStudent(String studentId, String sectionCode, String status) {
        jdbc.update("""
                INSERT INTO student_records (student_id, last_name, first_name, middle_name, birthdate, sex,
                    permanent_address, section_code, enrollment_date, completion_date, student_status,
                    employment_status, baptized)
                VALUES (?, 'Dela Cruz', 'Ana', 'Reyes', DATE '2005-02-14', 'Female', 'Quezon City',
                    ?, DATE '2025-06-02', DATE '2026-03-20', ?, 'Unemployed', 0)
                """, studentId, sectionCode, status);
    }

    /** One Graduated student with a single locked COMPETENT grade and a single TOR upload (DB-clock time). */
    private void lockedGraduateWithTor(LocalDateTime lockedAt, LocalDateTime torUploadedAt) {
        insertStudent("SR1", "S1", "Graduated");
        grade(enroll("SR1", "BPP-101"), "SR1", "BPP-101", "COMPETENT", null, true, Timestamp.valueOf(lockedAt).toString());
        document("SR1", "Transcript of Records (TOR)", Timestamp.valueOf(torUploadedAt).toString());
    }

    /** The real service on the H2 data, with the app clock pinned to a wall-clock time in the JVM zone. */
    private SoChecklistService serviceWithAppClockAt(LocalDateTime appNow) {
        Clock system = Clock.systemDefaultZone();
        return new SoChecklistService(jdbc, Clock.fixed(appNow.atZone(system.getZone()).toInstant(), system.getZone()));
    }

    private Integer recordId(String studentId) {
        return jdbc.queryForObject("SELECT record_id FROM student_records WHERE student_id = ?",
                Integer.class, studentId);
    }

    private void document(String studentId, String type, String uploadedAt) {
        jdbc.update("INSERT INTO documents (student_id, document_type, file_name, file_type, file_size,"
                        + " content_data, upload_date) VALUES (?, ?, 'f.pdf', 'application/pdf', 1, X'00', ?)",
                studentId, type, Timestamp.valueOf(uploadedAt));
    }

    private int enroll(String studentId, String subjectCode) {
        jdbc.update("INSERT INTO classes (section_code, subject_code, semester) VALUES ('S1', ?, '1st')", subjectCode);
        Integer classId = jdbc.queryForObject("SELECT class_id FROM classes WHERE subject_code = ?",
                Integer.class, subjectCode);
        jdbc.update("INSERT INTO class_enrollments (class_id, student_id) VALUES (?, ?)", classId, studentId);
        return classId;
    }

    private void grade(int classId, String studentId, String subjectCode, String remarks, String gradeStatus,
                       boolean locked, String lockedAt) {
        jdbc.update("INSERT INTO grades (student_id, subject_code, remarks, grade_status, class_id, locked, locked_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                studentId, subjectCode, remarks, gradeStatus, classId, locked ? 1 : 0,
                lockedAt == null ? null : Timestamp.valueOf(lockedAt));
    }

    private static SoChecklistItem item(SoChecklistResponse response, String key) {
        return response.items().stream().filter(i -> i.key().equals(key)).findFirst().orElseThrow();
    }

    /** Wraps the real JdbcTemplate to record each parameterized query's SQL. */
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
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            executedSql.add(sql);
            return super.query(sql, rowMapper, args);
        }

        List<String> executedSql() {
            return executedSql;
        }

        void clear() {
            executedSql.clear();
        }
    }
}
