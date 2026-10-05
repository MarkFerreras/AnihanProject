package com.example.springboot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.model.Batch;
import com.example.springboot.model.ClassEnrollment;
import com.example.springboot.model.Course;
import com.example.springboot.model.Document;
import com.example.springboot.model.Grade;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.Subject;
import com.example.springboot.model.User;

import jakarta.persistence.EntityManager;

/**
 * Real-JPA checks of the custom {@code @Query} methods of the section, subject, class,
 * enrollment, grade and document repositories (seeded match, no match, ordering).
 * ISO 25010 characteristic: Functional suitability (correctness) and Reliability (data integrity).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:classMgmtRepoDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:repository-h2-schema.sql"
})
class ClassManagementRepositoriesH2Test {

    @Autowired private StudentRecordRepository studentRepo;
    @Autowired private SectionRepository sectionRepo;
    @Autowired private SubjectRepository subjectRepo;
    @Autowired private SchoolClassRepository classRepo;
    @Autowired private ClassEnrollmentRepository enrollmentRepo;
    @Autowired private GradeRepository gradeRepo;
    @Autowired private DocumentRepository documentRepo;
    @Autowired private BatchRepository batchRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private EntityManager em;

    private Section secA;
    private Section secB;
    private Subject math;
    private Subject cook;
    private User trainer;
    private StudentRecord ana;
    private StudentRecord bea;

    @BeforeEach
    void seed() {
        Batch b2025 = batchRepo.save(new Batch("B2025", (short) 2025));
        Batch b2026 = batchRepo.save(new Batch("B2026", (short) 2026));
        Course course = courseRepo.save(new Course("CARS", "Culinary Arts"));
        secA = section("SEC-A", b2025, course);
        secB = section("SEC-B", b2026, course);
        math = subject("MATH1", "Math");
        cook = subject("COOK1", "Cooking");
        trainer = new User("trainer1", "hash", "t@x.com", "ROLE_TRAINER");
        trainer.setLastName("T");
        trainer.setFirstName("T");
        trainer.setMiddleName("T");
        trainer.setBirthdate(LocalDate.of(1990, 1, 1));
        trainer.setAge(36);
        trainer = userRepo.save(trainer);
        ana = student("SR20260001", "Santos", "Ana");
        bea = student("SR20260002", "Reyes", "Bea");
    }

    private Section section(String code, Batch batch, Course course) {
        Section s = new Section();
        s.setSectionCode(code);
        s.setSection(code);
        s.setBatch(batch);
        s.setCourse(course);
        return sectionRepo.save(s);
    }

    private Subject subject(String code, String name) {
        Subject s = new Subject();
        s.setSubjectCode(code);
        s.setSubjectName(name);
        s.setCompetencyType("Basic");
        s.setUnits(1);
        return subjectRepo.save(s);
    }

    private StudentRecord student(String id, String last, String first) {
        StudentRecord r = new StudentRecord();
        r.setStudentId(id);
        r.setLastName(last);
        r.setFirstName(first);
        r.setMiddleName("M");
        r.setBaptized(false);
        r.setStudentStatus("Active");
        return studentRepo.save(r);
    }

    private SchoolClass schoolClass(Section sec, Subject sub, User tr, String semester) {
        SchoolClass c = new SchoolClass();
        c.setSection(sec);
        c.setSubject(sub);
        c.setTrainer(tr);
        c.setSemester(semester);
        c.setCreatedAt(LocalDateTime.now());
        return classRepo.save(c);
    }

    private ClassEnrollment enroll(SchoolClass c, StudentRecord s) {
        ClassEnrollment e = new ClassEnrollment();
        e.setSchoolClass(c);
        e.setStudent(s);
        e.setEnrolledAt(LocalDateTime.now());
        return enrollmentRepo.saveAndFlush(e);
    }

    private Grade grade(SchoolClass c, StudentRecord s, Subject sub, boolean locked) {
        Grade g = new Grade();
        g.setSchoolClass(c);
        g.setStudent(s);
        g.setSubject(sub);
        g.setFinalGrade(new BigDecimal("1.50"));
        g.setLocked(locked);
        return gradeRepo.saveAndFlush(g);
    }

    private Document document(StudentRecord s, String type, String label) {
        Document d = new Document();
        d.setStudent(s);
        d.setDocumentType(type);
        d.setDocumentLabel(label);
        d.setFileName("f.pdf");
        d.setFileType("application/pdf");
        d.setFileSize(3);
        d.setContentData(new byte[] {1, 2, 3});
        return documentRepo.saveAndFlush(d);
    }

    private void setUploadDate(Document d, String ts) {
        em.createNativeQuery("UPDATE documents SET upload_date = TIMESTAMP '" + ts
                + "' WHERE document_id = " + d.getDocumentId()).executeUpdate();
    }

    // ---- Section / Subject ---------------------------------------------------------------

    @Test
    void sectionFindByBatchYear_matchAndEmpty() {
        assertEquals(List.of("SEC-A"),
                sectionRepo.findByBatchBatchYear((short) 2025).stream().map(Section::getSectionCode).toList());
        assertTrue(sectionRepo.findByBatchBatchYear((short) 1999).isEmpty());
    }

    @Test
    void subjectCountGradesBySubjectCode_matchAndZero() {
        SchoolClass c = schoolClass(secA, math, trainer, "2026-1");
        grade(c, ana, math, false);
        grade(c, bea, math, false);

        assertEquals(2, subjectRepo.countGradesBySubjectCode("MATH1"));
        assertEquals(0, subjectRepo.countGradesBySubjectCode("COOK1"));
        assertEquals(0, subjectRepo.countGradesBySubjectCode("NOPE"));
    }

    @Test
    void subjectRenameSubjectCode_renamesUnreferencedSubject() {
        subjectRepo.renameSubjectCode("COOK1", "COOK2");
        em.flush();
        em.clear();

        assertTrue(subjectRepo.findById("COOK2").isPresent());
        assertTrue(subjectRepo.findById("COOK1").isEmpty());
    }

    @Test
    void subjectRenameSubjectCode_unknownOldCodeChangesNothing() {
        subjectRepo.renameSubjectCode("GHOST", "GHOST2");
        em.flush();
        em.clear();
        assertTrue(subjectRepo.findById("GHOST2").isEmpty());
        assertEquals(2, subjectRepo.count());
    }

    // ---- SchoolClass ---------------------------------------------------------------------

    @Test
    void schoolClassDistinctSemesters_areDistinctAndNewestFirst() {
        schoolClass(secA, math, trainer, "2025-2");
        schoolClass(secA, cook, trainer, "2026-1");
        schoolClass(secB, math, trainer, "2026-1");
        schoolClass(secB, cook, null, "2024-1");

        assertEquals(List.of("2026-1", "2025-2", "2024-1"), classRepo.findDistinctSemesters());
    }

    @Test
    void schoolClassDistinctSemesters_emptyWhenNoClasses() {
        assertTrue(classRepo.findDistinctSemesters().isEmpty());
    }

    @Test
    void schoolClassDistinctSemestersByTrainer_filtersAndOrders() {
        schoolClass(secA, math, trainer, "2025-2");
        schoolClass(secA, cook, trainer, "2026-1");
        schoolClass(secB, cook, null, "2024-1");

        assertEquals(List.of("2026-1", "2025-2"), classRepo.findDistinctSemestersByTrainer(trainer.getUserId()));
        assertTrue(classRepo.findDistinctSemestersByTrainer(-1).isEmpty());
    }

    // ---- ClassEnrollment -----------------------------------------------------------------

    @Test
    void enrollmentDeleteByStudentAndSectionCode_deletesOnlyThatStudentInThatSection() {
        SchoolClass inA = schoolClass(secA, math, trainer, "2026-1");
        SchoolClass inB = schoolClass(secB, math, trainer, "2026-1");
        enroll(inA, ana);
        enroll(inB, ana);
        enroll(inA, bea);

        int deleted = enrollmentRepo.deleteByStudentAndSectionCode("SR20260001", "SEC-A");
        em.flush();
        em.clear();

        assertEquals(1, deleted);
        assertFalse(enrollmentRepo.existsBySchoolClassClassIdAndStudentStudentId(inA.getClassId(), "SR20260001"));
        assertTrue(enrollmentRepo.existsBySchoolClassClassIdAndStudentStudentId(inB.getClassId(), "SR20260001"));
        assertTrue(enrollmentRepo.existsBySchoolClassClassIdAndStudentStudentId(inA.getClassId(), "SR20260002"));
    }

    @Test
    void enrollmentDeleteByStudentAndSectionCode_noMatchReturnsZero() {
        enroll(schoolClass(secA, math, trainer, "2026-1"), ana);
        assertEquals(0, enrollmentRepo.deleteByStudentAndSectionCode("SR20260001", "SEC-B"));
        assertEquals(0, enrollmentRepo.deleteByStudentAndSectionCode("NOBODY", "SEC-A"));
    }

    @Test
    void classEnrollmentUniquePerStudentAndClass() {
        SchoolClass c = schoolClass(secA, math, trainer, "2026-1");
        enroll(c, ana);

        assertTrue(enrollmentRepo.existsBySchoolClassClassIdAndStudentStudentId(c.getClassId(), "SR20260001"));
        assertFalse(enrollmentRepo.existsBySchoolClassClassIdAndStudentStudentId(c.getClassId(), "SR20260002"));
        assertEquals(1, enrollmentRepo.countBySchoolClassClassId(c.getClassId()));
    }

    @Test
    void duplicateEnrollmentIsRejectedByTheSchemaUniqueKey() {
        SchoolClass c = schoolClass(secA, math, trainer, "2026-1");
        enroll(c, ana);
        assertThrows(DataIntegrityViolationException.class, () -> enroll(c, ana));
    }

    @Test
    void duplicateGradeForStudentAndClassIsRejectedByTheSchemaUniqueKey() {
        SchoolClass c = schoolClass(secA, math, trainer, "2026-1");
        grade(c, ana, math, false);
        assertThrows(DataIntegrityViolationException.class, () -> grade(c, ana, math, false));
    }

    @Test
    void renamingASubjectCodeCascadesToItsClasses() {
        SchoolClass c = schoolClass(secA, math, trainer, "2026-1");
        em.flush();
        subjectRepo.renameSubjectCode("MATH1", "MATH9");
        em.flush();
        em.clear();

        assertEquals("MATH9", classRepo.findById(c.getClassId()).orElseThrow().getSubject().getSubjectCode());
    }

    // ---- Grade ---------------------------------------------------------------------------

    @Test
    void gradeCountLockedClassesByTrainer_countsDistinctClassesWithLockedGrades() {
        SchoolClass c1 = schoolClass(secA, math, trainer, "2026-1");
        SchoolClass c2 = schoolClass(secA, cook, trainer, "2026-1");
        SchoolClass c3 = schoolClass(secB, cook, trainer, "2026-1");
        grade(c1, ana, math, true);
        grade(c1, bea, math, true);   // same class twice -> counted once
        grade(c2, ana, cook, true);
        grade(c3, ana, cook, false);  // unlocked -> not counted

        assertEquals(2, gradeRepo.countLockedGradeClassesByTrainerId(trainer.getUserId()));
        assertEquals(0, gradeRepo.countLockedGradeClassesByTrainerId(-1));
    }

    @Test
    void gradeFindByStudentAndClass() {
        SchoolClass c = schoolClass(secA, math, trainer, "2026-1");
        SchoolClass other = schoolClass(secB, math, trainer, "2026-1");
        grade(c, ana, math, false);

        assertTrue(gradeRepo.findBySchoolClassClassIdAndStudentStudentId(c.getClassId(), "SR20260001").isPresent());
        assertTrue(gradeRepo.findBySchoolClassClassIdAndStudentStudentId(c.getClassId(), "SR20260002").isEmpty());
        assertTrue(gradeRepo.findBySchoolClassClassIdAndStudentStudentId(other.getClassId(), "SR20260001").isEmpty());
        assertEquals(1, gradeRepo.findByStudentStudentId("SR20260001").size());
        assertEquals(1, gradeRepo.findBySchoolClassClassId(c.getClassId()).size());
    }

    // ---- Document ------------------------------------------------------------------------

    @Test
    void documentSearchSummaries_filtersByQueryTypeBatchAndSection() {
        ana.setBatch(batchRepo.findById("B2025").orElseThrow());
        ana.setSection(secA);
        studentRepo.saveAndFlush(ana);
        Document d1 = document(ana, "PSA Birth Certificate", null);
        document(bea, "Form 137", null);
        document(bea, "Others", "Barangay Clearance");

        assertEquals(3, documentRepo.searchSummaries(null, null, null, null).size());
        assertEquals(1, documentRepo.searchSummaries("SANTOS", null, null, null).size());
        assertEquals(1, documentRepo.searchSummaries("ana santos", null, null, null).size());
        assertEquals(1, documentRepo.searchSummaries("reyes, bea", "Form 137", null, null).size());
        assertEquals(1, documentRepo.searchSummaries("barangay", null, null, null).size());
        assertEquals(1, documentRepo.searchSummaries(null, null, "B2025", null).size());
        assertEquals(d1.getDocumentId(), documentRepo.searchSummaries(null, null, null, "SEC-A").get(0).documentId());
        assertTrue(documentRepo.searchSummaries("zzz", null, null, null).isEmpty());
        assertTrue(documentRepo.searchSummaries(null, "Form 137", "B2025", null).isEmpty());
    }

    @Test
    void documentSearchSummaries_orderedByUploadDateDescThenIdDesc() {
        Document oldest = document(ana, "PSA Birth Certificate", null);
        Document newest = document(ana, "Form 137", null);
        Document tieLow = document(bea, "Form 137", null);
        Document tieHigh = document(bea, "PSA Birth Certificate", null);
        setUploadDate(oldest, "2026-01-01 00:00:00");
        setUploadDate(newest, "2026-03-01 00:00:00");
        setUploadDate(tieLow, "2026-02-01 00:00:00");
        setUploadDate(tieHigh, "2026-02-01 00:00:00");
        em.clear();

        List<Integer> ids = documentRepo.searchSummaries(null, null, null, null).stream()
                .map(DocumentSummaryResponse::documentId).toList();

        assertEquals(List.of(newest.getDocumentId(), tieHigh.getDocumentId(), tieLow.getDocumentId(),
                oldest.getDocumentId()), ids);
    }

    @Test
    void documentFindSummariesByStudentId_isExactMatchOnly() {
        document(ana, "PSA Birth Certificate", null);
        document(bea, "Form 137", null);

        assertEquals(1, documentRepo.findSummariesByStudentId("SR20260001").size());
        assertTrue(documentRepo.findSummariesByStudentId("SR2026000").isEmpty());
        assertTrue(documentRepo.findSummariesByStudentId("NOPE").isEmpty());
    }

    @Test
    void documentFindSummariesByIds_matchesGivenIdsOnly() {
        Document d1 = document(ana, "PSA Birth Certificate", null);
        document(bea, "Form 137", null);

        List<DocumentSummaryResponse> rows = documentRepo.findSummariesByIds(List.of(d1.getDocumentId(), 9999));

        assertEquals(1, rows.size());
        assertEquals(d1.getDocumentId(), rows.get(0).documentId());
        assertTrue(documentRepo.findSummariesByIds(List.of(9999)).isEmpty());
    }

    @Test
    void documentFindDistinctLabels_distinctNonNullSorted() {
        document(ana, "Others", "Zebra Cert");
        document(bea, "Others", "Alpha Cert");
        document(bea, "Others", "Alpha Cert");
        document(ana, "Form 137", null);

        assertEquals(List.of("Alpha Cert", "Zebra Cert"), documentRepo.findDistinctDocumentLabels());
    }

    @Test
    void documentFindDistinctLabels_emptyWhenNoLabels() {
        document(ana, "Form 137", null);
        assertTrue(documentRepo.findDistinctDocumentLabels().isEmpty());
    }

    @Test
    void documentFindByStudentAndType_exactLookup() {
        document(ana, "ID Picture (1x1 / 2x2)", null);
        assertTrue(documentRepo.findByStudentStudentIdAndDocumentType("SR20260001", "ID Picture (1x1 / 2x2)").isPresent());
        assertTrue(documentRepo.findByStudentStudentIdAndDocumentType("SR20260002", "ID Picture (1x1 / 2x2)").isEmpty());
        assertNull(documentRepo.findByStudentStudentIdAndDocumentType("SR20260001", "Form 137").orElse(null));
    }

    @Test
    void deletingASubjectReferencedByAClassIsBlocked() {
        schoolClass(secA, math, trainer, "2026-1");
        em.flush();
        em.clear();
        assertThrows(DataIntegrityViolationException.class, () -> {
            subjectRepo.deleteById("MATH1");
            subjectRepo.flush();
        });
    }
}
