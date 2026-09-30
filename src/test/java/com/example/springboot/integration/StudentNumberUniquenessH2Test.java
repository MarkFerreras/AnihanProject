package com.example.springboot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.service.RegistrarService;

/**
 * Real-JPA guarantee that two students can never share a student number, and that a number
 * becomes reusable only once its holder's number is cleared, changed, or the holder deleted.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@Import(RegistrarService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:studentNumberUniqueDb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StudentNumberUniquenessH2Test {

    @Autowired private RegistrarService registrarService;
    @Autowired private StudentRecordRepository studentRecordRepository;

    private Integer newStudent(String studentId, String lastName) {
        StudentRecord r = new StudentRecord();
        r.setStudentId(studentId);
        r.setLastName(lastName);
        r.setFirstName("Test");
        r.setMiddleName("H2");
        r.setBaptized(false);
        r.setStudentStatus("Active");
        return studentRecordRepository.saveAndFlush(r).getRecordId();
    }

    @Test
    void aTakenNumberIsRejectedUntilItsHolderClearsIt() {
        Integer ana = newStudent("SR20260001", "Santos");
        Integer bea = newStudent("SR20260002", "Reyes");
        registrarService.assignStudentNumber(ana, "2026-001");

        IllegalArgumentException clash = assertThrows(IllegalArgumentException.class,
                () -> registrarService.assignStudentNumber(bea, "2026-001"));
        assertTrue(clash.getMessage().contains("already assigned to Santos"));

        registrarService.assignStudentNumber(ana, "");
        assertEquals("2026-001", registrarService.assignStudentNumber(bea, "2026-001").studentNumber());
    }

    @Test
    void aNumberIsFreedWhenItsHolderIsChangedToAnotherNumber() {
        Integer ana = newStudent("SR20260001", "Santos");
        Integer bea = newStudent("SR20260002", "Reyes");
        registrarService.assignStudentNumber(ana, "2026-001");
        registrarService.assignStudentNumber(ana, "2026-002");

        assertEquals("2026-001", registrarService.assignStudentNumber(bea, "2026-001").studentNumber());
    }

    @Test
    void aNumberIsFreedWhenItsHolderIsDeleted() {
        Integer ana = newStudent("SR20260001", "Santos");
        Integer bea = newStudent("SR20260002", "Reyes");
        registrarService.assignStudentNumber(ana, "2026-001");
        studentRecordRepository.deleteById(ana);
        studentRecordRepository.flush();

        assertEquals("2026-001", registrarService.assignStudentNumber(bea, "2026-001").studentNumber());
    }

    @Test
    void theUniqueIndexRejectsADuplicateThatBypassesTheService() {
        Integer ana = newStudent("SR20260001", "Santos");
        Integer bea = newStudent("SR20260002", "Reyes");
        StudentRecord a = studentRecordRepository.findById(ana).orElseThrow();
        a.setStudentNumber("2026-001");
        studentRecordRepository.saveAndFlush(a);

        StudentRecord b = studentRecordRepository.findById(bea).orElseThrow();
        b.setStudentNumber("2026-001");
        assertThrows(DataIntegrityViolationException.class, () -> studentRecordRepository.saveAndFlush(b));
    }
}
