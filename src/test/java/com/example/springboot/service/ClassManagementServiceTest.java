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
}
