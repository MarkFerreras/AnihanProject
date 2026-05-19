package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.springboot.dto.trainer.GradeSummaryResponse;
import com.example.springboot.dto.trainer.SaveGradeRequest;
import com.example.springboot.model.Grade;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.Subject;
import com.example.springboot.model.Section;
import com.example.springboot.model.User;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.SchoolClassRepository;
import com.example.springboot.repository.StudentRecordRepository;

@ExtendWith(MockitoExtension.class)
public class TrainerGradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private SchoolClassRepository classRepository;

    @InjectMocks
    private TrainerGradeService gradeService;

    @BeforeEach
    void setUp() {
        SecurityContext mockContext = mock(SecurityContext.class);
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("trainer1");
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockContext.getAuthentication()).thenReturn(mockAuth);
        SecurityContextHolder.setContext(mockContext);
    }

    @Test
    void testGetGradesForClassSuccess() {
        Integer classId = 1;
        User trainer = new User();
        trainer.setUserId(100);

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");
        subject.setSubjectName("Culinary Arts");
        subject.setUnits(3);

        Section section = new Section();
        section.setSectionCode("S001");
        section.setSection("Section A");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainer);
        schoolClass.setSubject(subject);
        schoolClass.setSection(section);
        schoolClass.setSemester("2026");

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");
        student.setLastName("Doe");
        student.setFirstName("John");
        student.setMiddleName("M");

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setSchoolClass(schoolClass);
        grade.setStudent(student);
        grade.setMidtermGrade(new BigDecimal("4.0"));
        grade.setFinalsGrade(new BigDecimal("3.5"));
        grade.setFinalGrade(new BigDecimal("3.7"));
        grade.setLocked(false);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(grade));
        when(gradeRepository.findByStudentStudentId("STU001")).thenReturn(List.of(grade));

        GradeSummaryResponse response = gradeService.getGradesForClass(classId);

        assertNotNull(response);
        assertEquals(classId, response.classId());
        assertEquals("Culinary Arts", response.subjectName());
        assertEquals("Section A", response.sectionName());
        assertEquals("2026", response.semester());
        assertFalse(response.locked());
        assertEquals(1, response.students().size());
    }


    @Test
    void testGetGradesForClassNotAssigned() {
        Integer classId = 1;
        User otherTrainer = new User();
        otherTrainer.setUserId(200);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(otherTrainer);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));

        assertThrows(IllegalArgumentException.class, () -> gradeService.getGradesForClass(classId));
    }

    @Test
    void testSaveGradesSuccess() {
        Integer classId = 1;
        User trainer = new User();
        trainer.setUserId(100);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainer);

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setStudent(student);
        grade.setLocked(false);

        SaveGradeRequest request = new SaveGradeRequest(
                "STU001",
                new BigDecimal("4.0"),
                new BigDecimal("3.5"),
                null,
                new BigDecimal("5.0"),
                "Good performance");

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.of(grade));
        when(gradeRepository.save(any())).thenReturn(grade);

        gradeService.saveGrades(classId, List.of(request));

        verify(gradeRepository, times(1)).save(any());
    }

    @Test
    void testSaveGradesGradeLocked() {
        Integer classId = 1;
        User trainer = new User();
        trainer.setUserId(100);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainer);

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setLocked(true);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.of(grade));

        SaveGradeRequest request = new SaveGradeRequest(
                "STU001",
                new BigDecimal("4.0"),
                new BigDecimal("3.5"),
                null,
                new BigDecimal("5.0"),
                "Good");

        assertThrows(IllegalArgumentException.class, () -> gradeService.saveGrades(classId, List.of(request)));
    }

    @Test
    void testLockGradesSuccess() {
        Integer classId = 1;
        User trainer = new User();
        trainer.setUserId(100);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainer);

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setLocked(false);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(grade));
        when(gradeRepository.save(any())).thenReturn(grade);

        gradeService.lockGrades(classId);

        verify(gradeRepository, times(1)).save(any());
    }

    @Test
    void testUnlockGradesSuccess() {
        Integer classId = 1;
        User trainer = new User();
        trainer.setUserId(100);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainer);

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setLocked(true);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(grade));
        when(gradeRepository.save(any())).thenReturn(grade);

        gradeService.unlockGrades(classId);

        verify(gradeRepository, times(1)).save(any());
    }

    @Test
    void testComputeGwaWithMultipleGrades() {
        Integer classId = 1;
        User trainer = new User();
        trainer.setUserId(100);

        Subject subject1 = new Subject();
        subject1.setSubjectCode("CUL101");
        subject1.setSubjectName("Culinary");
        subject1.setUnits(3);

        Subject subject2 = new Subject();
        subject2.setSubjectCode("CUL102");
        subject2.setSubjectName("Baking");
        subject2.setUnits(4);

        Section section = new Section();
        section.setSectionCode("S001");
        section.setSection("Section A");

        SchoolClass class1 = new SchoolClass();
        class1.setClassId(1);
        class1.setTrainer(trainer);
        class1.setSubject(subject1);
        class1.setSection(section);
        class1.setSemester("2026");

        SchoolClass class2 = new SchoolClass();
        class2.setClassId(2);
        class2.setTrainer(trainer);
        class2.setSubject(subject2);
        class2.setSection(section);
        class2.setSemester("2026");

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");

        Grade grade1 = new Grade();
        grade1.setSchoolClass(class1);
        grade1.setStudent(student);
        grade1.setFinalGrade(new BigDecimal("4.0"));

        Grade grade2 = new Grade();
        grade2.setSchoolClass(class2);
        grade2.setStudent(student);
        grade2.setFinalGrade(new BigDecimal("3.5"));

        when(classRepository.findById(classId)).thenReturn(Optional.of(class1));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(grade1));
        when(gradeRepository.findByStudentStudentId("STU001")).thenReturn(List.of(grade1, grade2));

        gradeService.getGradesForClass(classId);

        verify(gradeRepository, times(1)).findByStudentStudentId(anyString());
    }

    @Test
    void testSaveGradesComputesFinalGrade() {
        Integer classId = 1;
        User trainer = new User();
        trainer.setUserId(100);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainer);

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setStudent(student);

        SaveGradeRequest request = new SaveGradeRequest(
                "STU001",
                new BigDecimal("4.0"),
                new BigDecimal("3.0"),
                null,
                new BigDecimal("5.0"),
                null);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.of(grade));
        when(gradeRepository.save(any())).thenReturn(grade);

        gradeService.saveGrades(classId, List.of(request));

        assertEquals(new BigDecimal("3.40"), grade.getFinalGrade());
    }

    @Test
    void testLockGradesNotAssigned() {
        Integer classId = 1;
        User otherTrainer = new User();
        otherTrainer.setUserId(200);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(otherTrainer);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));

        assertThrows(IllegalArgumentException.class, () -> gradeService.lockGrades(classId));
    }

    @Test
    void testEffectiveGradeWithReExam() {
        Integer classId = 1;
        User trainer = new User();
        trainer.setUserId(100);

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");
        subject.setSubjectName("Culinary");
        subject.setUnits(3);

        Section section = new Section();
        section.setSectionCode("S001");
        section.setSection("A");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainer);
        schoolClass.setSubject(subject);
        schoolClass.setSection(section);
        schoolClass.setSemester("2026");

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");
        student.setLastName("Doe");
        student.setFirstName("Jane");
        student.setMiddleName("M");

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setSchoolClass(schoolClass);
        grade.setStudent(student);
        grade.setFinalGrade(new BigDecimal("3.2"));
        grade.setReExamGrade(new BigDecimal("2.5"));

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(grade));
        when(gradeRepository.findByStudentStudentId("STU001")).thenReturn(List.of(grade));

        GradeSummaryResponse response = gradeService.getGradesForClass(classId);

        assertNotNull(response);
        assertEquals(1, response.students().size());
    }
}
