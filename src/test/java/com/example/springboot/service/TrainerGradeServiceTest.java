package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.springboot.dto.trainer.GradeSummaryResponse;
import com.example.springboot.dto.trainer.SaveGradeRequest;
import com.example.springboot.model.ClassEnrollment;
import com.example.springboot.model.Grade;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.Subject;
import com.example.springboot.model.User;
import com.example.springboot.repository.ClassEnrollmentRepository;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.SchoolClassRepository;
import com.example.springboot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class TrainerGradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private SchoolClassRepository classRepository;

    @Mock
    private ClassEnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TrainerGradeService gradeService;

    private User trainerUser;

    @BeforeEach
    void setUp() {
        // Set up SecurityContext to return "trainer1" as current username
        SecurityContext mockContext = mock(SecurityContext.class);
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("trainer1");
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockContext.getAuthentication()).thenReturn(mockAuth);
        SecurityContextHolder.setContext(mockContext);

        // Create the trainer User object that resolveCurrentTrainerId will find
        trainerUser = new User();
        trainerUser.setUserId(100);
        trainerUser.setUsername("trainer1");
    }

    /**
     * Helper to stub the UserRepository lookup for resolveCurrentTrainerId().
     */
    private void stubTrainerLookup() {
        when(userRepository.findByUsername("trainer1")).thenReturn(Optional.of(trainerUser));
    }

    @Test
    void testGetGradesForClassSuccess() {
        stubTrainerLookup();
        Integer classId = 1;

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");
        subject.setSubjectName("Culinary Arts");
        subject.setUnits(3);

        Section section = new Section();
        section.setSectionCode("S001");
        section.setSection("Section A");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);
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

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setStudent(student);
        enrollment.setSchoolClass(schoolClass);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(grade));
        when(enrollmentRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(enrollment));
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
    void testGetGradesForClassNewClassNoGrades() {
        stubTrainerLookup();
        Integer classId = 1;

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");
        subject.setSubjectName("Culinary Arts");
        subject.setUnits(3);

        Section section = new Section();
        section.setSectionCode("S001");
        section.setSection("Section A");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);
        schoolClass.setSubject(subject);
        schoolClass.setSection(section);
        schoolClass.setSemester("2026");

        StudentRecord student1 = new StudentRecord();
        student1.setStudentId("STU001");
        student1.setLastName("Doe");
        student1.setFirstName("John");

        StudentRecord student2 = new StudentRecord();
        student2.setStudentId("STU002");
        student2.setLastName("Smith");
        student2.setFirstName("Jane");

        ClassEnrollment enrollment1 = new ClassEnrollment();
        enrollment1.setStudent(student1);
        enrollment1.setSchoolClass(schoolClass);

        ClassEnrollment enrollment2 = new ClassEnrollment();
        enrollment2.setStudent(student2);
        enrollment2.setSchoolClass(schoolClass);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        // No grades exist yet
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of());
        when(enrollmentRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(enrollment1, enrollment2));

        GradeSummaryResponse response = gradeService.getGradesForClass(classId);

        assertNotNull(response);
        assertEquals(2, response.students().size());
        assertFalse(response.locked());
        // All grade fields should be null for students without grade rows
        response.students().forEach(s -> {
            assertNull(s.midtermGrade());
            assertNull(s.finalsGrade());
            assertNull(s.finalGrade());
            assertNull(s.gwa());
        });
    }

    @Test
    void testGetGradesForClassNotAssigned() {
        stubTrainerLookup();
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
        stubTrainerLookup();
        Integer classId = 1;

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);
        schoolClass.setSubject(subject);

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
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, "STU001")).thenReturn(true);
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.of(grade));
        when(gradeRepository.save(any())).thenReturn(grade);

        gradeService.saveGrades(classId, List.of(request));

        verify(gradeRepository, times(1)).save(any());
    }

    @Test
    void testSaveGradesAutoCreatesGradeRow() {
        stubTrainerLookup();
        Integer classId = 1;

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);
        schoolClass.setSubject(subject);

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setStudent(student);
        enrollment.setSchoolClass(schoolClass);

        SaveGradeRequest request = new SaveGradeRequest(
                "STU001",
                new BigDecimal("4.0"),
                new BigDecimal("3.5"),
                null,
                null,
                null);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, "STU001")).thenReturn(true);
        // No existing grade — triggers auto-create
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(enrollment));
        when(gradeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        gradeService.saveGrades(classId, List.of(request));

        verify(gradeRepository, times(1)).save(argThat(g ->
                g.getStudent().getStudentId().equals("STU001") &&
                g.getSchoolClass().getClassId().equals(classId) &&
                g.getMidtermGrade().compareTo(new BigDecimal("4.0")) == 0));
    }

    @Test
    void testSaveGradesGradeLocked() {
        stubTrainerLookup();
        Integer classId = 1;

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setLocked(true);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, "STU001")).thenReturn(true);
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
        stubTrainerLookup();
        Integer classId = 1;

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);

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
        stubTrainerLookup();
        Integer classId = 1;

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);

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
        stubTrainerLookup();
        Integer classId = 1;

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
        class1.setTrainer(trainerUser);
        class1.setSubject(subject1);
        class1.setSection(section);
        class1.setSemester("2026");

        SchoolClass class2 = new SchoolClass();
        class2.setClassId(2);
        class2.setTrainer(trainerUser);
        class2.setSubject(subject2);
        class2.setSection(section);
        class2.setSemester("2026");

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");
        student.setLastName("Doe");
        student.setFirstName("John");

        Grade grade1 = new Grade();
        grade1.setSchoolClass(class1);
        grade1.setStudent(student);
        grade1.setFinalGrade(new BigDecimal("4.0"));

        Grade grade2 = new Grade();
        grade2.setSchoolClass(class2);
        grade2.setStudent(student);
        grade2.setFinalGrade(new BigDecimal("3.5"));

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setStudent(student);
        enrollment.setSchoolClass(class1);

        when(classRepository.findById(classId)).thenReturn(Optional.of(class1));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(grade1));
        when(enrollmentRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(enrollment));
        when(gradeRepository.findByStudentStudentId("STU001")).thenReturn(List.of(grade1, grade2));

        GradeSummaryResponse response = gradeService.getGradesForClass(classId);

        assertNotNull(response);
        assertEquals(1, response.students().size());
        // GWA = (4.0*3 + 3.5*4) / (3+4) = (12+14)/7 = 26/7 = 3.71
        assertEquals(new BigDecimal("3.71"), response.students().get(0).gwa());
    }

    @Test
    void testSaveGradesComputesFinalGrade() {
        stubTrainerLookup();
        Integer classId = 1;

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);
        schoolClass.setSubject(subject);

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
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, "STU001")).thenReturn(true);
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.of(grade));
        when(gradeRepository.save(any())).thenReturn(grade);

        gradeService.saveGrades(classId, List.of(request));

        assertEquals(new BigDecimal("3.40"), grade.getFinalGrade());
    }

    @Test
    void testLockGradesNotAssigned() {
        stubTrainerLookup();
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
        stubTrainerLookup();
        Integer classId = 1;

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");
        subject.setSubjectName("Culinary");
        subject.setUnits(3);

        Section section = new Section();
        section.setSectionCode("S001");
        section.setSection("A");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);
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

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setStudent(student);
        enrollment.setSchoolClass(schoolClass);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(grade));
        when(enrollmentRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(enrollment));
        when(gradeRepository.findByStudentStudentId("STU001")).thenReturn(List.of(grade));

        GradeSummaryResponse response = gradeService.getGradesForClass(classId);

        assertNotNull(response);
        assertEquals(1, response.students().size());
        // Since final_grade=3.2 > 3.0 and re_exam=2.5, effective=2.5 → GWA=2.5
        assertEquals(new BigDecimal("2.50"), response.students().get(0).gwa());
    }

    @Test
    void rejectsGradeOutsideValidRange() {
        stubTrainerLookup();
        Integer classId = 1;

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setStudent(student);
        grade.setLocked(false);

        SaveGradeRequest request = new SaveGradeRequest(
                "STU001",
                new BigDecimal("0.5"),
                new BigDecimal("3.5"),
                null, null, null);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, "STU001")).thenReturn(true);
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.of(grade));

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(request)));
    }

    @Test
    void computesFinalGradeUnconditionallyWhenOneGradeNull() {
        stubTrainerLookup();
        Integer classId = 1;

        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(classId);
        schoolClass.setTrainer(trainerUser);
        schoolClass.setSubject(subject);

        StudentRecord student = new StudentRecord();
        student.setStudentId("STU001");

        Grade grade = new Grade();
        grade.setGradeId(1);
        grade.setStudent(student);
        grade.setLocked(false);

        SaveGradeRequest request = new SaveGradeRequest(
                "STU001",
                new BigDecimal("3.0"),
                null,
                null, null, null);

        when(classRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, "STU001")).thenReturn(true);
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.of(grade));
        when(gradeRepository.save(any())).thenReturn(grade);

        gradeService.saveGrades(classId, List.of(request));

        assertNull(grade.getFinalGrade());
    }
}
