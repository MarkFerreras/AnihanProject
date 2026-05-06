package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.OtherGuardianRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentUploadRepository;

/**
 * Bulk load tests for the Registrar Student Records dashboard.
 *
 * Verifies that {@link RegistrarService#getAllRecords()} can handle 200
 * student records correctly — DTO mapping, free-text search filtering,
 * and the 5-second non-functional performance requirement.
 *
 * All record data is generated programmatically in JVM memory via Mockito
 * mocks. No real database is touched and no hard-coded dummy records are
 * persisted anywhere.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarBulkLoadTest {

    @Mock
    private StudentRecordRepository studentRecordRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private com.example.springboot.repository.StudentOjtRepository studentOjtRepository;

    @Mock
    private com.example.springboot.repository.StudentTesdaQualificationRepository tesdaQualRepository;

    @Mock
    private com.example.springboot.repository.StudentSchoolYearRepository schoolYearRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private OtherGuardianRepository guardianRepository;

    @Mock
    private StudentEducationRepository educationRepository;

    @Mock
    private StudentUploadRepository uploadRepository;

    @Mock
    private com.example.springboot.service.StorageService storageService;

    @InjectMocks
    private RegistrarService registrarService;

    @Test
    void getAllRecordsReturnsTwoHundredRecords() {
        when(studentRecordRepository.findAll()).thenReturn(buildRecordList(200));

        List<StudentRecordSummaryResponse> result = registrarService.getAllRecords();

        assertEquals(200, result.size(),
                "getAllRecords should return exactly 200 student record DTOs");
        assertEquals("STU-0", result.get(0).studentId());
        assertEquals("STU-199", result.get(199).studentId());
        // Spot-check that FK relations were flattened to codes correctly
        assertEquals("BATCH-0", result.get(0).batchCode());
        assertEquals("CARS", result.get(0).courseCode());
        assertEquals("SEC-0", result.get(0).sectionCode());
    }

    @Test
    void getAllRecordsCompletesWithinPerformanceBound() {
        when(studentRecordRepository.findAll()).thenReturn(buildRecordList(200));

        long startMs = System.currentTimeMillis();
        List<StudentRecordSummaryResponse> result = registrarService.getAllRecords();
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertEquals(200, result.size());
        // Non-functional requirement: record retrieval < 5 seconds
        assertTrue(elapsedMs < 5000,
                "Retrieving 200 student records took " + elapsedMs
                        + "ms — exceeds 5-second limit");
    }

    @Test
    void batchYearRangeFilterRestrictsResults() {
        when(studentRecordRepository.findAll()).thenReturn(buildRecordList(200));

        // The dummy data assigns each record a batchYear of 2026 + (i % 5),
        // so years cycle through 2026, 2027, 2028, 2029, 2030.

        // Range that includes only 2027 + 2028
        List<StudentRecordSummaryResponse> mid = registrarService.getAllRecords(null, 2027, 2028);
        assertTrue(mid.size() > 0, "Expected at least one record in 2027–2028 range");
        assertTrue(mid.size() < 200, "Year filter must reduce the full set");

        // Open-ended fromYear (>= 2030) — only the year-2030 batch
        List<StudentRecordSummaryResponse> tail = registrarService.getAllRecords(null, 2030, null);
        assertTrue(tail.stream().allMatch(r -> r.batchCode() != null),
                "Year-filtered results must always have a batch");

        // Range that excludes everything
        List<StudentRecordSummaryResponse> none = registrarService.getAllRecords(null, 1900, 1901);
        assertEquals(0, none.size(), "Year range with no matching batches should yield empty list");

        // Combined text query + year filter — both must match
        List<StudentRecordSummaryResponse> combined =
                registrarService.getAllRecords("Last_42", 2024, 2030);
        // batch year for index 42 is 2026 + (42 % 5) = 2028, which is inside the range
        assertEquals(1, combined.size(), "Expected one record matching both name and year range");
        assertEquals("STU-42", combined.get(0).studentId());
    }

    @Test
    void searchByQueryFiltersAcrossAllSearchableFields() {
        when(studentRecordRepository.findAll()).thenReturn(buildRecordList(200));

        // Last name match — every record has lastName "Last_<i>", so "Last_42" is unique
        List<StudentRecordSummaryResponse> byName = registrarService.getAllRecords("Last_42");
        assertEquals(1, byName.size(), "Expected one match for 'Last_42'");
        assertEquals("STU-42", byName.get(0).studentId());

        // Student ID match — case-insensitive
        List<StudentRecordSummaryResponse> byStudentId = registrarService.getAllRecords("stu-99");
        assertTrue(byStudentId.stream().anyMatch(r -> "STU-99".equals(r.studentId())),
                "Expected case-insensitive match on studentId");

        // Status match — every 7th record is "Active"
        List<StudentRecordSummaryResponse> byStatus = registrarService.getAllRecords("Active");
        assertTrue(byStatus.size() > 0, "Expected at least one 'Active' record");
        assertTrue(byStatus.stream().allMatch(r -> "Active".equals(r.studentStatus())),
                "Every result must have Active status when filtering by 'Active'");

        // Course code match
        List<StudentRecordSummaryResponse> byCourse = registrarService.getAllRecords("CARS");
        assertEquals(200, byCourse.size(),
                "All 200 records share course code 'CARS'");

        // No matches — query that does not appear anywhere
        List<StudentRecordSummaryResponse> none = registrarService.getAllRecords("zzznomatchzzz");
        assertEquals(0, none.size(),
                "Expected no matches for unseen query string");

        // Blank query returns everything
        List<StudentRecordSummaryResponse> all = registrarService.getAllRecords("   ");
        assertEquals(200, all.size(),
                "Blank query should return all records");
    }

    @Test
    void statusFilterRestrictsResultsByStudentStatus() {
        when(studentRecordRepository.findAll()).thenReturn(buildRecordList(200));

        // Statuses cycle "Enrolling", "Submitted", "Active" — 200 records → ~67 each
        List<StudentRecordSummaryResponse> active = registrarService.getAllRecords(null, null, null, "Active");
        assertTrue(active.size() > 0, "Expected at least one Active record");
        assertTrue(active.stream().allMatch(r -> "Active".equals(r.studentStatus())),
                "Every result must have Active status when filtering by 'Active'");
        assertTrue(active.size() < 200, "Status filter must reduce the full set");

        // Case-insensitive match
        List<StudentRecordSummaryResponse> enrollingLower = registrarService.getAllRecords(null, null, null, "enrolling");
        assertTrue(enrollingLower.size() > 0, "Status filter should be case-insensitive");
        assertTrue(enrollingLower.stream().allMatch(r -> "Enrolling".equals(r.studentStatus())),
                "Case-insensitive filter 'enrolling' should match only Enrolling records");

        // Null status returns everything
        List<StudentRecordSummaryResponse> all = registrarService.getAllRecords(null, null, null, null);
        assertEquals(200, all.size(), "Null status should return all records");

        // Blank status returns everything
        List<StudentRecordSummaryResponse> blank = registrarService.getAllRecords(null, null, null, "  ");
        assertEquals(200, blank.size(), "Blank status should return all records");

        // Unknown status returns nothing
        List<StudentRecordSummaryResponse> none = registrarService.getAllRecords(null, null, null, "Graduated");
        assertEquals(0, none.size(), "Unknown status should return empty list");
    }

    /**
     * Generates a list of unique StudentRecord objects for bulk testing.
     * All data lives only in JVM memory — nothing is persisted to any database.
     * Every record gets a deterministic studentId, names, batch/course/section,
     * and a status that cycles through "Enrolling" / "Submitted" / "Active".
     */
    private List<StudentRecord> buildRecordList(int count) {
        String[] statuses = {"Enrolling", "Submitted", "Active"};
        Course sharedCourse = new Course("CARS", "Culinary Arts and Restaurant Services");
        List<StudentRecord> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            StudentRecord r = new StudentRecord();
            r.setRecordId(i + 1);
            r.setStudentId("STU-" + i);
            r.setLastName("Last_" + i);
            r.setFirstName("First_" + i);
            r.setMiddleName("Middle_" + i);
            r.setStudentStatus(statuses[i % statuses.length]);

            Batch b = new Batch();
            b.setBatchCode("BATCH-" + i);
            b.setBatchYear((short) (2026 + (i % 5)));
            r.setBatch(b);

            r.setCourse(sharedCourse);

            Section s = new Section();
            s.setSectionCode("SEC-" + i);
            r.setSection(s);

            records.add(r);
        }
        return records;
    }
}
