package com.example.springboot.service;

import com.example.springboot.dto.registrar.ClassResponse;
import com.example.springboot.dto.registrar.CreateClassRequest;
import com.example.springboot.dto.registrar.UpdateClassTrainerRequest;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.Subject;
import com.example.springboot.model.User;
import com.example.springboot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassManagementServiceTest {

    @Mock private SubjectRepository subjectRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private StudentRecordRepository studentRecordRepository;
    @Mock private QualificationRepository qualificationRepository;

    @InjectMocks private ClassManagementService service;

    private SchoolClass schoolClass;
    private User trainer;

    @BeforeEach
    void setup() {
        Section section = new Section();
        section.setSectionCode("SEC-A");
        section.setSection("Section A");

        Subject subject = new Subject();
        subject.setSubjectCode("CK-101");
        subject.setSubjectName("Basic Cookery");

        schoolClass = new SchoolClass();
        schoolClass.setClassId(1);
        schoolClass.setSection(section);
        schoolClass.setSubject(subject);
        schoolClass.setSemester("2026");
        schoolClass.setCreatedAt(LocalDateTime.now());

        trainer = new User();
        trainer.setUserId(10);
        trainer.setLastName("Dela Cruz");
        trainer.setFirstName("Juan");
        trainer.setUsername("jdelacruz");
        trainer.setRole("ROLE_TRAINER");
        trainer.setEnabled(true);
    }

    @Test
    void updateClassTrainerAssignsTrainer() {
        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(userRepository.findById(10)).thenReturn(Optional.of(trainer));
        when(classRepository.save(any(SchoolClass.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(0L);

        ClassResponse resp = service.updateClassTrainer(1, new UpdateClassTrainerRequest(10));

        assertEquals("Dela Cruz, Juan", resp.trainerName());
        assertEquals(10, resp.trainerId());
        verify(classRepository).save(schoolClass);
    }

    @Test
    void updateClassTrainerUnassignsWhenTrainerIdNull() {
        schoolClass.setTrainer(trainer);
        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(classRepository.save(any(SchoolClass.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(0L);

        ClassResponse resp = service.updateClassTrainer(1, new UpdateClassTrainerRequest(null));

        assertNull(resp.trainerName());
        assertNull(resp.trainerId());
        verify(classRepository).save(schoolClass);
    }

    @Test
    void updateClassTrainerThrowsWhenClassNotFound() {
        when(classRepository.findById(99)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateClassTrainer(99, new UpdateClassTrainerRequest(10)));
        assertTrue(ex.getMessage().toLowerCase().contains("class not found"));
        verify(classRepository, never()).save(any());
    }

    @Test
    void updateClassTrainerThrowsWhenTrainerNotFound() {
        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateClassTrainer(1, new UpdateClassTrainerRequest(99)));
        assertTrue(ex.getMessage().toLowerCase().contains("trainer not found"));
        verify(classRepository, never()).save(any());
    }

    @Test
    void updateClassTrainerThrowsWhenUserIsNotTrainer() {
        trainer.setRole("ROLE_REGISTRAR");
        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(userRepository.findById(10)).thenReturn(Optional.of(trainer));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateClassTrainer(1, new UpdateClassTrainerRequest(10)));
        assertTrue(ex.getMessage().toLowerCase().contains("not a trainer"));
        verify(classRepository, never()).save(any());
    }

    @Test
    void updateClassTrainerThrowsWhenTrainerDisabled() {
        trainer.setEnabled(false);
        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(userRepository.findById(10)).thenReturn(Optional.of(trainer));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateClassTrainer(1, new UpdateClassTrainerRequest(10)));
        assertTrue(ex.getMessage().toLowerCase().contains("disabled"));
        verify(classRepository, never()).save(any());
    }

    // ----- createClass -----

    @Test
    void createClassReturnsClassResponse() {
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(schoolClass.getSection()));
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(schoolClass.getSubject()));
        when(classRepository.existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester(
                "SEC-A", "CK-101", "2026")).thenReturn(false);

        ClassResponse resp = service.createClass(new CreateClassRequest("SEC-A", "CK-101", null, "2026"));

        assertEquals("SEC-A", resp.sectionCode());
        assertEquals("CK-101", resp.subjectCode());
        assertNull(resp.trainerId());
        assertEquals(0, resp.enrolledCount());
        verify(classRepository).save(any(SchoolClass.class));
    }

    @Test
    void createClassRejectsUnknownSection() {
        when(sectionRepository.findById("BAD-SEC")).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.createClass(new CreateClassRequest("BAD-SEC", "CK-101", null, "2026")));
        assertTrue(ex.getMessage().contains("Section not found"));
        verify(classRepository, never()).save(any());
    }

    @Test
    void createClassRejectsUnknownSubject() {
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(schoolClass.getSection()));
        when(subjectRepository.findById("BAD-SUB")).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.createClass(new CreateClassRequest("SEC-A", "BAD-SUB", null, "2026")));
        assertTrue(ex.getMessage().contains("Subject not found"));
        verify(classRepository, never()).save(any());
    }

    @Test
    void createClassRejectsDuplicateSectionSubjectSemester() {
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(schoolClass.getSection()));
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(schoolClass.getSubject()));
        when(classRepository.existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester(
                "SEC-A", "CK-101", "2026")).thenReturn(true);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.createClass(new CreateClassRequest("SEC-A", "CK-101", null, "2026")));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(classRepository, never()).save(any());
    }

    @Test
    void createClassRejectsUnknownTrainer() {
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(schoolClass.getSection()));
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(schoolClass.getSubject()));
        when(classRepository.existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester(
                "SEC-A", "CK-101", "2026")).thenReturn(false);
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.createClass(new CreateClassRequest("SEC-A", "CK-101", 99, "2026")));
        assertTrue(ex.getMessage().contains("Trainer not found"));
        verify(classRepository, never()).save(any());
    }

    @Test
    void createClassRejectsNonTrainerRole() {
        trainer.setRole("ROLE_REGISTRAR");
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(schoolClass.getSection()));
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(schoolClass.getSubject()));
        when(classRepository.existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester(
                "SEC-A", "CK-101", "2026")).thenReturn(false);
        when(userRepository.findById(10)).thenReturn(Optional.of(trainer));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.createClass(new CreateClassRequest("SEC-A", "CK-101", 10, "2026")));
        assertTrue(ex.getMessage().toLowerCase().contains("not a trainer"));
        verify(classRepository, never()).save(any());
    }

    @Test
    void createClassRejectsDisabledTrainer() {
        trainer.setEnabled(false);
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(schoolClass.getSection()));
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(schoolClass.getSubject()));
        when(classRepository.existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester(
                "SEC-A", "CK-101", "2026")).thenReturn(false);
        when(userRepository.findById(10)).thenReturn(Optional.of(trainer));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.createClass(new CreateClassRequest("SEC-A", "CK-101", 10, "2026")));
        assertTrue(ex.getMessage().contains("disabled"));
        verify(classRepository, never()).save(any());
    }

    // ----- getClasses -----

    @Test
    void getClassesFiltersBySemester() {
        when(classRepository.findBySemester("2026")).thenReturn(List.of(schoolClass));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(5L);

        List<ClassResponse> result = service.getClasses("2026");

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).enrolledCount());
        verify(classRepository, never()).findAll();
    }

    @Test
    void getClassesReturnsAllWithNoFilter() {
        when(classRepository.findAll()).thenReturn(List.of(schoolClass));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(2L);

        List<ClassResponse> result = service.getClasses(null);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).enrolledCount());
        verify(classRepository, never()).findBySemester(any());
    }

    // ----- getEligibleStudents -----

    @Test
    void getEligibleStudentsFiltersBySectionStatusAndEnrollment() {
        Section otherSection = new Section();
        otherSection.setSectionCode("SEC-B");

        StudentRecord alpha = new StudentRecord();
        alpha.setStudentId("S2");
        alpha.setLastName("Alpha");
        alpha.setFirstName("Second");
        alpha.setSection(schoolClass.getSection());
        alpha.setStudentStatus("Submitted");

        StudentRecord bravo = new StudentRecord();
        bravo.setStudentId("S1");
        bravo.setLastName("Bravo");
        bravo.setFirstName("First");
        bravo.setSection(schoolClass.getSection());
        bravo.setStudentStatus("Active");

        StudentRecord graduated = new StudentRecord();
        graduated.setStudentId("S3");
        graduated.setLastName("Charlie");
        graduated.setSection(schoolClass.getSection());
        graduated.setStudentStatus("Graduated");

        StudentRecord wrongSection = new StudentRecord();
        wrongSection.setStudentId("S4");
        wrongSection.setLastName("Delta");
        wrongSection.setSection(otherSection);
        wrongSection.setStudentStatus("Active");

        StudentRecord alreadyEnrolled = new StudentRecord();
        alreadyEnrolled.setStudentId("S5");
        alreadyEnrolled.setLastName("Echo");
        alreadyEnrolled.setSection(schoolClass.getSection());
        alreadyEnrolled.setStudentStatus("Active");

        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(studentRecordRepository.findAll()).thenReturn(
                List.of(alpha, bravo, graduated, wrongSection, alreadyEnrolled));
        // Only alpha/bravo/alreadyEnrolled pass the section+status filters and actually reach
        // this check — all three are stubbed explicitly since Mockito's strict stubbing flags
        // a partially-stubbed method (some args covered, others left to the boolean default)
        // as a likely typo rather than silently defaulting the rest to false.
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(1, "S1")).thenReturn(false);
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(1, "S2")).thenReturn(false);
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(1, "S5")).thenReturn(true);

        var result = service.getEligibleStudents(1);

        assertEquals(2, result.size());
        assertEquals("Alpha", result.get(0).lastName());
        assertEquals("Bravo", result.get(1).lastName());
    }

    @Test
    void getEligibleStudentsRejectsUnknownClass() {
        when(classRepository.findById(99)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.getEligibleStudents(99));
        assertTrue(ex.getMessage().toLowerCase().contains("class not found"));
    }
}
