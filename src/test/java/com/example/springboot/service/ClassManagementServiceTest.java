package com.example.springboot.service;

import com.example.springboot.dto.registrar.ClassResponse;
import com.example.springboot.dto.registrar.UpdateClassTrainerRequest;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.Section;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    // ========== Task 12: class creation, enrollment, section membership ==========

    private com.example.springboot.model.StudentRecord student(String id, String status, Section section) {
        com.example.springboot.model.StudentRecord s = new com.example.springboot.model.StudentRecord();
        s.setStudentId(id);
        s.setStudentStatus(status);
        s.setSection(section);
        return s;
    }

    @Test
    void createClassDuplicateSubjectSectionSemesterRejected() {
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(schoolClass.getSection()));
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(schoolClass.getSubject()));
        when(classRepository.existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester("SEC-A", "CK-101", "2026"))
                .thenReturn(true);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.createClass(
                        new com.example.springboot.dto.registrar.CreateClassRequest("SEC-A", "CK-101", null, "2026")));

        assertEquals("A class for this section, subject, and semester already exists.", ex.getMessage());
        verify(classRepository, never()).save(any());
    }

    @Test
    void reassignTrainerValidatesTrainerRoleAndLogsAudit() {
        // Service half of the contract: a non-trainer is rejected and nothing is saved.
        // The single audit-log row is written by the controller (see
        // ServiceMutationLogAuditWebMvcTest, which asserts exactly one logAction call).
        trainer.setRole("ROLE_ADMIN");
        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(userRepository.findById(10)).thenReturn(Optional.of(trainer));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateClassTrainer(1, new UpdateClassTrainerRequest(10)));

        assertTrue(ex.getMessage().contains("User is not a trainer"));
        verify(classRepository, never()).save(any());
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void bulkEnrollSectionReportsPartialSuccess() {
        Section sec = schoolClass.getSection();
        var active = student("SR1", "Active", sec);
        var submitted = student("SR2", "Submitted", sec);
        var already = student("SR3", "Active", sec);
        var completed = student("SR4", "Completed", sec);
        var enrolling = student("SR5", "Enrolling", sec);
        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(studentRecordRepository.findBySectionSectionCode("SEC-A"))
                .thenReturn(java.util.List.of(active, submitted, already, completed, enrolling));
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(eq(1), anyString()))
                .thenAnswer(inv -> "SR3".equals(inv.getArgument(1)));

        var report = service.bulkEnrollSectionIntoClass(1);

        assertEquals(2, report.enrolledCount());
        assertEquals(1, report.skippedAlreadyEnrolled());
        assertEquals(2, report.skippedIneligible());
        assertEquals(5, report.totalConsidered());
        var saved = org.mockito.ArgumentCaptor.forClass(com.example.springboot.model.ClassEnrollment.class);
        verify(enrollmentRepository, times(2)).save(saved.capture());
        assertEquals(java.util.List.of("SR1", "SR2"),
                saved.getAllValues().stream().map(e -> e.getStudent().getStudentId()).toList());
    }

    @Test
    void enrollSameStudentTwiceIsIdempotent() {
        // Observed behaviour: the second call is rejected (not silently accepted) and creates no row.
        var s = student("SR1", "Active", schoolClass.getSection());
        when(classRepository.findById(1)).thenReturn(Optional.of(schoolClass));
        when(studentRecordRepository.findByStudentId("SR1")).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(1, "SR1"))
                .thenReturn(false, true);
        var request = new com.example.springboot.dto.registrar.EnrollStudentRequest(1, "SR1");

        service.enrollStudent(request);
        var ex = assertThrows(IllegalArgumentException.class, () -> service.enrollStudent(request));

        assertEquals("Student is already enrolled in this class.", ex.getMessage());
        verify(enrollmentRepository, times(1)).save(any(com.example.springboot.model.ClassEnrollment.class));
    }

    @Test
    void removeCompletedOrGraduatedFromSectionRejected() {
        Section sec = schoolClass.getSection();
        when(sectionRepository.existsById("SEC-A")).thenReturn(true);
        for (String status : new String[] {"Completed", "Graduated"}) {
            var s = student("SR-" + status, status, sec);
            when(studentRecordRepository.findByStudentId("SR-" + status)).thenReturn(Optional.of(s));

            var ex = assertThrows(IllegalArgumentException.class,
                    () -> service.removeStudentFromSection("SEC-A", s.getStudentId()));

            assertTrue(ex.getMessage().contains("cannot be removed from a section"));
            assertEquals(status, s.getStudentStatus());
            assertSame(sec, s.getSection());
        }
        verify(studentRecordRepository, never()).save(any());
        verify(enrollmentRepository, never()).deleteByStudentAndSectionCode(anyString(), anyString());

        // An Active student is removed and reset to Submitted.
        var active = student("SR-A", "Active", sec);
        when(studentRecordRepository.findByStudentId("SR-A")).thenReturn(Optional.of(active));
        when(enrollmentRepository.deleteByStudentAndSectionCode("SR-A", "SEC-A")).thenReturn(3);

        assertEquals(3, service.removeStudentFromSection("SEC-A", "SR-A"));
        assertNull(active.getSection());
        assertEquals("Submitted", active.getStudentStatus());
        verify(studentRecordRepository).save(active);
    }

    @Test
    void deleteSectionWithClassesBlocked() {
        when(sectionRepository.existsById("SEC-A")).thenReturn(true);
        when(classRepository.existsBySectionSectionCode("SEC-A")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.deleteSection("SEC-A"));

        verify(sectionRepository, never()).deleteById(anyString());
    }

    @Test
    void deleteSectionWithStudentsCurrentlyReliesOnDatabaseFk() {
        // FINDING: deleteSection() checks only for classes referencing the section; it has no
        // explicit "section still has students" guard. Expected (per the brief): blocked with a
        // friendly IllegalArgumentException. Observed: deleteById is invoked and protection comes
        // solely from the students.section_code foreign key (a DB error, not a clean message).
        when(sectionRepository.existsById("SEC-A")).thenReturn(true);
        when(classRepository.existsBySectionSectionCode("SEC-A")).thenReturn(false);

        service.deleteSection("SEC-A");

        verify(sectionRepository).deleteById("SEC-A");
        verifyNoInteractions(studentRecordRepository);
    }
}
