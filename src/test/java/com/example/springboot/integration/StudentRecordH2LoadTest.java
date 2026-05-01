package com.example.springboot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.StudentRecordRepository;

/**
 * H2-isolated integration test verifying that 100 dummy student records
 * can be persisted and loaded through the real JPA stack.
 *
 * <p>This test runs in a completely separate environment from the main
 * coding setup:
 * <ul>
 *   <li>Uses an in-memory H2 database in MySQL compatibility mode.</li>
 *   <li>Hibernate creates the schema fresh and drops it after the test
 *       (`spring.jpa.hibernate.ddl-auto=create-drop`).</li>
 *   <li>The live MySQL container is never connected to and never modified.</li>
 *   <li>All test data exists only inside the JVM for the test's lifetime.</li>
 * </ul>
 *
 * <p>The test verifies that:
 * <ol>
 *   <li>100 StudentRecord rows can be persisted via {@code saveAll}.</li>
 *   <li>{@code findAll()} returns exactly 100 entities back.</li>
 *   <li>Auto-generated record IDs are populated correctly.</li>
 *   <li>The unique student_id column allows 100 distinct values.</li>
 *   <li>Bulk load completes within the 5-second non-functional bound.</li>
 * </ol>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:studentRecordsTestDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StudentRecordH2LoadTest {

    @Autowired
    private StudentRecordRepository studentRecordRepository;

    @Test
    void canPersistAndLoadOneHundredStudentRecords() {
        List<StudentRecord> dummyRecords = buildDummyRecords(100);

        long startMs = System.currentTimeMillis();
        List<StudentRecord> saved = studentRecordRepository.saveAll(dummyRecords);
        List<StudentRecord> loaded = studentRecordRepository.findAll();
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertEquals(100, saved.size(), "All 100 records should be persisted");
        assertEquals(100, loaded.size(), "findAll should return all 100 persisted records");

        // Every record must have an auto-generated recordId after persistence
        for (StudentRecord r : loaded) {
            assertNotNull(r.getRecordId(),
                    "Auto-generated recordId must be populated after save");
            assertNotNull(r.getStudentId(),
                    "Business key studentId must round-trip");
            assertNotNull(r.getLastName());
            assertNotNull(r.getFirstName());
            assertNotNull(r.getMiddleName());
        }

        // Verify uniqueness of the studentId business key across all 100 rows
        long distinctStudentIds = loaded.stream()
                .map(StudentRecord::getStudentId)
                .distinct()
                .count();
        assertEquals(100L, distinctStudentIds,
                "All 100 student_id values must be unique");

        // Non-functional requirement: bulk load < 5 seconds
        assertTrue(elapsedMs < 5000,
                "Persisting and loading 100 records in H2 took " + elapsedMs
                        + "ms — exceeds 5-second limit");
    }

    /**
     * Generates 100 unique StudentRecord entities for the H2 load test.
     * FK relations (batch/course/section) are intentionally left null —
     * the columns are nullable in the schema, and this test focuses on
     * core row insertion through the JPA layer.
     */
    private List<StudentRecord> buildDummyRecords(int count) {
        String[] statuses = {"Enrolling", "Submitted", "Active"};
        List<StudentRecord> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            StudentRecord r = new StudentRecord();
            // recordId is auto-generated (IDENTITY) — do NOT set
            r.setStudentId("H2-STU-" + i);
            r.setLastName("H2Last_" + i);
            r.setFirstName("H2First_" + i);
            r.setMiddleName("H2Middle_" + i);
            r.setBaptized(false);
            r.setStudentStatus(statuses[i % statuses.length]);
            list.add(r);
        }
        return list;
    }
}
