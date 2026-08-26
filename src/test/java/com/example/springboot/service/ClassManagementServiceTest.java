package com.example.springboot.service;

import com.example.springboot.dto.registrar.ClassResponse;
import com.example.springboot.dto.registrar.TrainerSummaryResponse;
import com.example.springboot.dto.registrar.UpdateClassTrainerRequest;
import com.example.springboot.model.Batch;
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

    @Test
    void getTrainerSummariesReturnsCountsForCurrentSemesterOnly() {
        trainer.setEmail("jdelacruz@example.com");
        when(batchRepository.findTopByOrderByBatchYearDesc())
                .thenReturn(Optional.of(new Batch("B2026A", (short) 2026)));
        when(userRepository.findByRoleAndEnabledTrue("ROLE_TRAINER")).thenReturn(List.of(trainer));
        when(classRepository.countByTrainerUserIdAndSemester(10, "2026")).thenReturn(3L);

        List<TrainerSummaryResponse> result = service.getTrainerSummaries();

        assertEquals(1, result.size());
        TrainerSummaryResponse summary = result.get(0);
        assertEquals(10, summary.userId());
        assertEquals("Dela Cruz", summary.lastName());
        assertEquals("Juan", summary.firstName());
        assertEquals("jdelacruz@example.com", summary.email());
        assertEquals(3L, summary.classCount());
    }

    @Test
    void getTrainerSummariesReturnsZeroWhenTrainerHasNoCurrentSemesterClasses() {
        when(batchRepository.findTopByOrderByBatchYearDesc())
                .thenReturn(Optional.of(new Batch("B2026A", (short) 2026)));
        when(userRepository.findByRoleAndEnabledTrue("ROLE_TRAINER")).thenReturn(List.of(trainer));
        when(classRepository.countByTrainerUserIdAndSemester(10, "2026")).thenReturn(0L);

        List<TrainerSummaryResponse> result = service.getTrainerSummaries();

        assertEquals(0L, result.get(0).classCount());
    }

    @Test
    void getClassesForTrainerScopesToCurrentSemester() {
        schoolClass.setTrainer(trainer);
        when(batchRepository.findTopByOrderByBatchYearDesc())
                .thenReturn(Optional.of(new Batch("B2026A", (short) 2026)));
        when(classRepository.findByTrainerUserIdAndSemester(10, "2026")).thenReturn(List.of(schoolClass));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(5L);

        List<ClassResponse> result = service.getClassesForTrainer(10);

        assertEquals(1, result.size());
        assertEquals("CK-101", result.get(0).subjectCode());
        assertEquals(5L, result.get(0).enrolledCount());
        verify(classRepository).findByTrainerUserIdAndSemester(10, "2026");
    }

    @Test
    void getClassesWithNoFiltersReturnsAll() {
        when(classRepository.findAll()).thenReturn(List.of(schoolClass));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(0L);

        List<ClassResponse> result = service.getClasses(null, null);

        assertEquals(1, result.size());
        verify(classRepository).findAll();
        verify(classRepository, never()).findBySemester(any());
        verify(classRepository, never()).findBySubjectSubjectCode(any());
    }

    @Test
    void getClassesFiltersBySemesterOnly() {
        when(classRepository.findBySemester("2026")).thenReturn(List.of(schoolClass));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(0L);

        List<ClassResponse> result = service.getClasses("2026", null);

        assertEquals(1, result.size());
        verify(classRepository).findBySemester("2026");
        verify(classRepository, never()).findAll();
    }

    @Test
    void getClassesFiltersBySubjectCodeOnly() {
        when(classRepository.findBySubjectSubjectCode("CK-101")).thenReturn(List.of(schoolClass));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(0L);

        List<ClassResponse> result = service.getClasses(null, "CK-101");

        assertEquals(1, result.size());
        assertEquals("CK-101", result.get(0).subjectCode());
        verify(classRepository).findBySubjectSubjectCode("CK-101");
        verify(classRepository, never()).findAll();
    }

    @Test
    void getClassesFiltersBySemesterAndSubjectCodeTogether() {
        when(classRepository.findBySemesterAndSubjectSubjectCode("2026", "CK-101"))
                .thenReturn(List.of(schoolClass));
        when(enrollmentRepository.countBySchoolClassClassId(1)).thenReturn(0L);

        List<ClassResponse> result = service.getClasses("2026", "CK-101");

        assertEquals(1, result.size());
        verify(classRepository).findBySemesterAndSubjectSubjectCode("2026", "CK-101");
        verify(classRepository, never()).findBySemester(any());
        verify(classRepository, never()).findBySubjectSubjectCode(any());
    }
}
