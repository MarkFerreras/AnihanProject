package com.example.springboot.service;

import com.example.springboot.dto.trainer.TrainerClassResponse;
import com.example.springboot.dto.trainer.TrainerClassStudentResponse;
import com.example.springboot.dto.trainer.TrainerSubjectResponse;
import com.example.springboot.dto.trainer.TrainerSubjectStudentResponse;
import com.example.springboot.model.Batch;
import com.example.springboot.model.ClassEnrollment;
import com.example.springboot.model.Course;
import com.example.springboot.model.Qualification;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.Subject;
import com.example.springboot.model.User;
import com.example.springboot.repository.ClassEnrollmentRepository;
import com.example.springboot.repository.SchoolClassRepository;
import com.example.springboot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    @Mock private SchoolClassRepository classRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TrainerService service;

    private User trainer;

    @BeforeEach
    void setup() {
        trainer = new User();
        trainer.setUserId(42);
        trainer.setUsername("trainer");
        trainer.setLastName("Reyes");
        trainer.setFirstName("Lara");
        trainer.setRole("ROLE_TRAINER");
        trainer.setEnabled(true);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("trainer");
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    @Test
    void resolveCurrentTrainerIdReturnsUserIdForLoggedInTrainer() {
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));

        Integer id = service.resolveCurrentTrainerId();

        assertEquals(42, id);
    }

    @Test
    void resolveCurrentTrainerIdThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.resolveCurrentTrainerId());
    }

    @Test
    void getMyAssignedSubjectsAggregatesAcrossClasses() {
        Course cars = new Course();
        cars.setCourseCode("CARS");
        cars.setCourseName("Culinary Arts and Restaurant Services");

        Batch batch = new Batch();
        batch.setBatchYear((short) 2026);

        Section secA = new Section();
        secA.setSectionCode("CARS-2026-A");
        secA.setSection("Section A");
        secA.setCourse(cars);
        secA.setBatch(batch);

        Section secB = new Section();
        secB.setSectionCode("CARS-2026-B");
        secB.setSection("Section B");
        secB.setCourse(cars);
        secB.setBatch(batch);

        Qualification qual = new Qualification();
        qual.setQualificationCode(1);
        qual.setQualificationName("Cookery NC II");

        Subject cookery = new Subject();
        cookery.setSubjectCode("CK-101");
        cookery.setSubjectName("Basic Cookery");
        cookery.setQualification(qual);
        cookery.setUnits(3);

        SchoolClass c1 = new SchoolClass();
        c1.setClassId(1);
        c1.setSection(secA);
        c1.setSubject(cookery);
        c1.setSemester("2026");

        SchoolClass c2 = new SchoolClass();
        c2.setClassId(2);
        c2.setSection(secB);
        c2.setSubject(cookery);
        c2.setSemester("2026");

        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findByTrainerUserId(42)).thenReturn(List.of(c1, c2));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(12L);
        when(enrollmentRepository.countBySchoolClassClassId(2)).thenReturn(10L);

        List<TrainerSubjectResponse> result = service.getMyAssignedSubjects();

        assertEquals(1, result.size());
        TrainerSubjectResponse row = result.get(0);
        assertEquals("CK-101", row.subjectCode());
        assertEquals("Basic Cookery", row.subjectName());
        assertEquals("Cookery NC II", row.qualificationName());
        assertEquals(3, row.units());
        assertEquals(22L, row.enrolledCount());
        assertEquals(List.of("Section A", "Section B"), row.sectionNames());
        assertEquals(List.of("Culinary Arts and Restaurant Services"), row.courseNames());
    }

    @Test
    void getMyAssignedSubjectsReturnsEmptyListWhenNoClasses() {
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findByTrainerUserId(42)).thenReturn(List.of());

        List<TrainerSubjectResponse> result = service.getMyAssignedSubjects();

        assertTrue(result.isEmpty());
    }

    @Test
    void getStudentsForSubjectReturnsRosterAcrossSections() {
        Section secA = new Section();
        secA.setSectionCode("CARS-2026-A");
        secA.setSection("Section A");
        Section secB = new Section();
        secB.setSectionCode("CARS-2026-B");
        secB.setSection("Section B");

        Subject cookery = new Subject();
        cookery.setSubjectCode("CK-101");

        SchoolClass c1 = new SchoolClass();
        c1.setClassId(1);
        c1.setSection(secA);
        c1.setSubject(cookery);
        SchoolClass c2 = new SchoolClass();
        c2.setClassId(2);
        c2.setSection(secB);
        c2.setSubject(cookery);

        StudentRecord s1 = new StudentRecord();
        s1.setStudentId("2026-001");
        s1.setLastName("Cruz");
        s1.setFirstName("Maria");
        s1.setMiddleName("L");
        StudentRecord s2 = new StudentRecord();
        s2.setStudentId("2026-002");
        s2.setLastName("Reyes");
        s2.setFirstName("Ana");
        s2.setMiddleName("B");
        StudentRecord s3 = new StudentRecord();
        s3.setStudentId("2026-015");
        s3.setLastName("Santos");
        s3.setFirstName("Lara");
        s3.setMiddleName("C");

        ClassEnrollment e1 = new ClassEnrollment();
        e1.setEnrollmentId(100);
        e1.setSchoolClass(c1);
        e1.setStudent(s1);
        ClassEnrollment e2 = new ClassEnrollment();
        e2.setEnrollmentId(101);
        e2.setSchoolClass(c1);
        e2.setStudent(s2);
        ClassEnrollment e3 = new ClassEnrollment();
        e3.setEnrollmentId(102);
        e3.setSchoolClass(c2);
        e3.setStudent(s3);

        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findByTrainerUserIdAndSubjectSubjectCode(42, "CK-101"))
                .thenReturn(List.of(c1, c2));
        when(enrollmentRepository.findBySchoolClassClassId(1)).thenReturn(List.of(e1, e2));
        when(enrollmentRepository.findBySchoolClassClassId(2)).thenReturn(List.of(e3));

        List<TrainerSubjectStudentResponse> result = service.getStudentsForSubject("CK-101");

        assertEquals(3, result.size());
        assertEquals("2026-001", result.get(0).studentId());
        assertEquals("Section A", result.get(0).sectionName());
        assertEquals("Section B", result.get(2).sectionName());
    }

    @Test
    void getStudentsForSubjectThrowsWhenTrainerDoesNotTeachSubject() {
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findByTrainerUserIdAndSubjectSubjectCode(42, "CK-101"))
                .thenReturn(List.of());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.getStudentsForSubject("CK-101"));
        assertTrue(ex.getMessage().toLowerCase().contains("not assigned"));
    }

    @Test
    void getMyClassesReturnsClassListSortedBySemesterDescThenSection() {
        Course cars = new Course();
        cars.setCourseCode("CARS");
        cars.setCourseName("Culinary Arts and Restaurant Services");

        Section secA = new Section();
        secA.setSectionCode("CARS-2025-A");
        secA.setSection("Section A");
        secA.setCourse(cars);
        Section secB = new Section();
        secB.setSectionCode("CARS-2026-A");
        secB.setSection("Section A");
        secB.setCourse(cars);

        Subject cookery = new Subject();
        cookery.setSubjectCode("CK-101");
        cookery.setSubjectName("Basic Cookery");

        SchoolClass older = new SchoolClass();
        older.setClassId(1);
        older.setSection(secA);
        older.setSubject(cookery);
        older.setSemester("2025");
        SchoolClass newer = new SchoolClass();
        newer.setClassId(2);
        newer.setSection(secB);
        newer.setSubject(cookery);
        newer.setSemester("2026");

        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findByTrainerUserId(42)).thenReturn(List.of(older, newer));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(15L);
        when(enrollmentRepository.countBySchoolClassClassId(2)).thenReturn(12L);

        List<TrainerClassResponse> result = service.getMyClasses();

        assertEquals(2, result.size());
        assertEquals("2026", result.get(0).semester());
        assertEquals(12L, result.get(0).enrolledCount());
        assertEquals("Culinary Arts and Restaurant Services", result.get(0).courseName());
        assertEquals("2025", result.get(1).semester());
    }

    @Test
    void getMyClassesReturnsEmptyListWhenNoneAssigned() {
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findByTrainerUserId(42)).thenReturn(List.of());

        List<TrainerClassResponse> result = service.getMyClasses();

        assertTrue(result.isEmpty());
    }

    @Test
    void getStudentsForClassReturnsRosterWhenTrainerOwnsClass() {
        Section sec = new Section();
        sec.setSectionCode("CARS-2026-A");
        sec.setSection("Section A");
        Subject cookery = new Subject();
        cookery.setSubjectCode("CK-101");

        SchoolClass c = new SchoolClass();
        c.setClassId(1);
        c.setSection(sec);
        c.setSubject(cookery);
        c.setTrainer(trainer);

        StudentRecord s = new StudentRecord();
        s.setStudentId("2026-001");
        s.setLastName("Cruz");
        s.setFirstName("Maria");
        s.setMiddleName("L");

        ClassEnrollment e = new ClassEnrollment();
        e.setEnrollmentId(100);
        e.setSchoolClass(c);
        e.setStudent(s);

        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findById(1)).thenReturn(Optional.of(c));
        when(enrollmentRepository.findBySchoolClassClassId(1)).thenReturn(List.of(e));

        List<TrainerClassStudentResponse> result = service.getStudentsForClass(1);

        assertEquals(1, result.size());
        assertEquals("2026-001", result.get(0).studentId());
        assertEquals("Cruz", result.get(0).lastName());
    }

    @Test
    void getStudentsForClassThrowsWhenClassNotFound() {
        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findById(99)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.getStudentsForClass(99));
        assertTrue(ex.getMessage().toLowerCase().contains("class not found"));
    }

    @Test
    void getStudentsForClassThrowsWhenAnotherTrainerOwnsClass() {
        User otherTrainer = new User();
        otherTrainer.setUserId(99);
        otherTrainer.setRole("ROLE_TRAINER");

        SchoolClass c = new SchoolClass();
        c.setClassId(1);
        c.setTrainer(otherTrainer);

        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findById(1)).thenReturn(Optional.of(c));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.getStudentsForClass(1));
        assertTrue(ex.getMessage().toLowerCase().contains("not assigned"));
    }

    @Test
    void getStudentsForClassThrowsWhenClassHasNoTrainer() {
        SchoolClass c = new SchoolClass();
        c.setClassId(1);
        c.setTrainer(null);

        when(userRepository.findByUsername("trainer")).thenReturn(Optional.of(trainer));
        when(classRepository.findById(1)).thenReturn(Optional.of(c));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.getStudentsForClass(1));
        assertTrue(ex.getMessage().toLowerCase().contains("not assigned"));
    }
}
