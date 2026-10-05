package com.example.springboot.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.model.Document;
import com.example.springboot.model.StudentRecord;

import jakarta.persistence.EntityManager;

/**
 * Real-JPA checks of {@link StudentRecordRepository} on H2 (MySQL mode).
 * ISO 25010 characteristic: Reliability (data integrity) and Functional suitability.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:studentRecordRepoDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:repository-h2-schema.sql"
})
class StudentRecordRepositoryH2Test {

    @Autowired private StudentRecordRepository repo;
    @Autowired private DocumentRepository documentRepo;
    @Autowired private EntityManager em;

    private StudentRecord student(String studentId, String last, String first, String middle) {
        StudentRecord r = new StudentRecord();
        r.setStudentId(studentId);
        r.setLastName(last);
        r.setFirstName(first);
        r.setMiddleName(middle);
        r.setBaptized(false);
        r.setStudentStatus("Active");
        return r;
    }

    private Document doc(StudentRecord s, String type) {
        Document d = new Document();
        d.setStudent(s);
        d.setDocumentType(type);
        d.setFileName("f.pdf");
        d.setFileType("application/pdf");
        d.setFileSize(3);
        d.setContentData(new byte[] {1, 2, 3});
        return d;
    }

    @Test
    void studentIdIsUnique() {
        repo.saveAndFlush(student("SR20260001", "Santos", "Ana", "B"));
        assertThrows(DataIntegrityViolationException.class,
                () -> repo.saveAndFlush(student("SR20260001", "Reyes", "Bea", "C")));
    }

    @Test
    void multipleNullStudentNumbersAreAllowed() {
        repo.saveAndFlush(student("SR20260001", "Santos", "Ana", "B"));
        repo.saveAndFlush(student("SR20260002", "Reyes", "Bea", "C"));
        assertEquals(2, repo.count());
        assertTrue(repo.findAll().stream().allMatch(s -> s.getStudentNumber() == null));
    }

    @Test
    void nameSearchIsCaseInsensitive() {
        repo.saveAndFlush(student("SR20260001", "Santos", "Ana", "Bautista"));
        assertTrue(repo.existsByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                "SANTOS", "ana", "BAUTISTA"));
        assertEquals(1, repo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                "santos", "ANA", "bautista").size());
        assertFalse(repo.existsByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                "Santos", "Ana", "Other"));
    }

    @Test
    void findMaxStudentIdWithPrefix_returnsMatchEmptyAndOrdering() {
        repo.saveAndFlush(student("SR20260002", "A", "A", "A"));
        repo.saveAndFlush(student("SR20260010", "B", "B", "B"));
        repo.saveAndFlush(student("SR20250099", "C", "C", "C"));

        assertEquals("SR20260010", repo.findMaxStudentIdWithPrefix("SR2026").orElseThrow());
        assertTrue(repo.findMaxStudentIdWithPrefix("SR2030").isEmpty());
    }

    @Test
    void nativeDeleteQueries_removeOnlyTheGivenStudentsRows() {
        StudentRecord ana = repo.saveAndFlush(student("SR20260001", "Santos", "Ana", "B"));
        StudentRecord bea = repo.saveAndFlush(student("SR20260002", "Reyes", "Bea", "C"));
        documentRepo.saveAndFlush(doc(ana, "PSA"));
        documentRepo.saveAndFlush(doc(bea, "PSA"));

        repo.deleteDocumentsByStudentId("SR20260001");
        em.flush();
        em.clear();

        assertTrue(documentRepo.findSummariesByStudentId("SR20260001").isEmpty());
        assertEquals(1, documentRepo.findSummariesByStudentId("SR20260002").size());
        // grades / class_enrollments natives run against empty tables without error
        repo.deleteGradesByStudentId("SR20260001");
        repo.deleteClassEnrollmentsByStudentId("SR20260001");
    }

    @Test
    void deleteStudentWithChildrenBehavesAsSchemaDefines() {
        StudentRecord ana = repo.saveAndFlush(student("SR20260001", "Santos", "Ana", "B"));
        documentRepo.saveAndFlush(doc(ana, "PSA"));
        em.clear();

        // Entity declares no cascade, so the FK documents.student_id -> student_records blocks the delete.
        assertThrows(DataIntegrityViolationException.class, () -> {
            repo.deleteById(ana.getRecordId());
            repo.flush();
        });
    }

    @Test
    void deleteStudentAfterChildCleanupLeavesNoOrphans() {
        StudentRecord ana = repo.saveAndFlush(student("SR20260001", "Santos", "Ana", "B"));
        documentRepo.saveAndFlush(doc(ana, "PSA"));

        repo.deleteDocumentsByStudentId("SR20260001");
        em.flush();
        em.clear();
        repo.deleteById(ana.getRecordId());
        repo.flush();

        assertEquals(0, repo.count());
        assertEquals(0, documentRepo.count());
    }
}
