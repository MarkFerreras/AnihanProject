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

    @Mock private GradeRepository gradeRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TrainerGradeService gradeService;

    private User trainerUser;

    @BeforeEach
    void setUp() {
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("trainer1");
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        trainerUser = new User();
        trainerUser.setUserId(100);
        trainerUser.setUsername("trainer1");
    }

    private void stubTrainerLookup() {
        when(userRepository.findByUsername("trainer1")).thenReturn(Optional.of(trainerUser));
    }

    private SchoolClass ownedClass(Integer classId) {
        Subject subject = new Subject();
        subject.setSubjectCode("CUL101");
        subject.setSubjectName("Culinary Arts");
        subject.setUnits(3);
        Section section = new Section();
        section.setSection("Section A");
        SchoolClass sc = new SchoolClass();
        sc.setClassId(classId);
        sc.setTrainer(trainerUser);
        sc.setSubject(subject);
        sc.setSection(section);
        sc.setSemester("2026");
        return sc;
    }

    private StudentRecord student(String id) {
        StudentRecord s = new StudentRecord();
        s.setStudentId(id);
        s.setLastName("Doe");
        s.setFirstName("Jane");
        return s;
    }

    private SaveGradeRequest req(String id, String finalPct, String status, String reExamPct, String hours) {
        return new SaveGradeRequest(
                id,
                finalPct == null ? null : new BigDecimal(finalPct),
                status,
                reExamPct == null ? null : new BigDecimal(reExamPct),
                hours == null ? null : new BigDecimal(hours));
    }

    private Grade stubExistingGrade(Integer classId, String studentId, SchoolClass sc) {
        Grade g = new Grade();
        g.setStudent(student(studentId));
        g.setSubject(sc.getSubject());
        g.setSchoolClass(sc);
        g.setLocked(false);
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, studentId)).thenReturn(true);
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, studentId))
                .thenReturn(Optional.of(g));
        return g;
    }

    // ----- read -----

    @Test
    void getGradesForClassMapsExistingRow() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        StudentRecord stu = student("STU001");

        Grade g = new Grade();
        g.setSchoolClass(sc);
        g.setStudent(stu);
        g.setFinalPercentage(new BigDecimal("88"));
        g.setFinalGrade(new BigDecimal("2.00"));
        g.setRemarks("COMPETENT");
        g.setHoursRendered(new BigDecimal("40"));

        ClassEnrollment e = new ClassEnrollment();
        e.setStudent(stu);
        e.setSchoolClass(sc);

        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(g));
        when(enrollmentRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(e));

        GradeSummaryResponse resp = gradeService.getGradesForClass(classId);
        assertEquals(1, resp.students().size());
        var row = resp.students().get(0);
        assertEquals(new BigDecimal("88"), row.finalPercentage());
        assertEquals(new BigDecimal("2.00"), row.finalGrade());
        assertEquals("COMPETENT", row.remarks());
        assertEquals(new BigDecimal("40"), row.hoursRendered());
    }

    @Test
    void getGradesForClassNewClassReturnsEmptyRows() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        ClassEnrollment e1 = new ClassEnrollment();
        e1.setStudent(student("STU001"));
        e1.setSchoolClass(sc);

        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of());
        when(enrollmentRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(e1));

        GradeSummaryResponse resp = gradeService.getGradesForClass(classId);
        assertEquals(1, resp.students().size());
        var row = resp.students().get(0);
        assertNull(row.finalPercentage());
        assertNull(row.finalGrade());
        assertNull(row.gradeStatus());
        assertNull(row.remarks());
        assertFalse(row.locked());
    }

    @Test
    void getGradesForClassRejectsUnassignedTrainer() {
        stubTrainerLookup();
        User other = new User();
        other.setUserId(999);
        SchoolClass sc = new SchoolClass();
        sc.setClassId(1);
        sc.setTrainer(other);
        when(classRepository.findById(1)).thenReturn(Optional.of(sc));
        assertThrows(IllegalArgumentException.class, () -> gradeService.getGradesForClass(1));
    }

    // ----- save: percentage path -----

    @Test
    void percentageIsTransmutedAndPassingRemarkIsCompetent() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        Grade g = stubExistingGrade(classId, "STU001", sc);
        when(gradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradeService.saveGrades(classId, List.of(req("STU001", "88", null, null, "40")));

        assertEquals(new BigDecimal("88"), g.getFinalPercentage());
        assertEquals(new BigDecimal("2.00"), g.getFinalGrade());
        assertEquals("COMPETENT", g.getRemarks());
        assertNull(g.getGradeStatus());
        assertEquals(new BigDecimal("40"), g.getHoursRendered());
    }

    @Test
    void failingPercentageRemarkIsNotCompetent() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        Grade g = stubExistingGrade(classId, "STU001", sc);
        when(gradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradeService.saveGrades(classId, List.of(req("STU001", "60", null, null, "10")));

        assertEquals(new BigDecimal("5.00"), g.getFinalGrade());
        assertEquals("NOT_COMPETENT", g.getRemarks());
    }

    @Test
    void reExamAcceptedWhenFinalFailedAndDrivesTheRemark() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        Grade g = stubExistingGrade(classId, "STU001", sc);
        when(gradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // final 60 -> 5.00 (fail); re-exam 80 -> 2.75 (pass) -> effective 2.75 -> Competent
        gradeService.saveGrades(classId, List.of(req("STU001", "60", null, "80", "12")));

        assertEquals(new BigDecimal("5.00"), g.getFinalGrade());
        assertEquals(new BigDecimal("80"), g.getReExamPercentage());
        assertEquals(new BigDecimal("2.75"), g.getReExamGrade());
        assertEquals("COMPETENT", g.getRemarks());
    }

    @Test
    void reExamRejectedWhenFinalPassed() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        stubExistingGrade(classId, "STU001", sc);

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(req("STU001", "85", null, "90", "20"))));
    }

    @Test
    void percentageOutOfRangeRejected() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        stubExistingGrade(classId, "STU001", sc);

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(req("STU001", "150", null, null, "20"))));
    }

    // ----- save: status path -----

    @Test
    void statusCodeStoresNoEquivalentAndDerivesRemark() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        Grade g = stubExistingGrade(classId, "STU001", sc);
        when(gradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradeService.saveGrades(classId, List.of(req("STU001", null, "C", null, "18")));
        assertEquals("C", g.getGradeStatus());
        assertNull(g.getFinalPercentage());
        assertNull(g.getFinalGrade());
        assertEquals("COMPETENT", g.getRemarks());

        gradeService.saveGrades(classId, List.of(req("STU001", null, "FA", null, "5")));
        assertEquals("FA", g.getGradeStatus());
        assertEquals("NOT_COMPETENT", g.getRemarks());

        gradeService.saveGrades(classId, List.of(req("STU001", null, "D", null, "0")));
        assertEquals("D", g.getGradeStatus());
        assertNull(g.getRemarks());
    }

    @Test
    void bothPercentageAndStatusRejected() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        stubExistingGrade(classId, "STU001", sc);

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(req("STU001", "88", "C", null, "20"))));
    }

    @Test
    void neitherPercentageNorStatusRejected() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        stubExistingGrade(classId, "STU001", sc);

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(req("STU001", null, null, null, "20"))));
    }

    @Test
    void reExamRejectedAlongsideStatusCode() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        stubExistingGrade(classId, "STU001", sc);

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(req("STU001", null, "FA", "80", "5"))));
    }

    // ----- save: hours -----

    @Test
    void hoursRenderedRequired() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        stubExistingGrade(classId, "STU001", sc);

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(req("STU001", "88", null, null, null))));
    }

    @Test
    void hoursRenderedOutOfRangeRejected() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        stubExistingGrade(classId, "STU001", sc);

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(req("STU001", "88", null, null, "150"))));
    }

    // ----- save: misc -----

    @Test
    void lockedGradeRejectsSave() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        Grade g = stubExistingGrade(classId, "STU001", sc);
        g.setLocked(true);

        assertThrows(IllegalArgumentException.class,
                () -> gradeService.saveGrades(classId, List.of(req("STU001", "88", null, null, "20"))));
    }

    @Test
    void saveAutoCreatesGradeRowForEnrolledStudent() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        StudentRecord stu = student("STU001");
        ClassEnrollment e = new ClassEnrollment();
        e.setStudent(stu);
        e.setSchoolClass(sc);

        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(classId, "STU001")).thenReturn(true);
        when(gradeRepository.findBySchoolClassClassIdAndStudentStudentId(classId, "STU001"))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(e));
        when(gradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradeService.saveGrades(classId, List.of(req("STU001", "93", null, null, "30")));

        verify(gradeRepository).save(argThat(g ->
                g.getStudent().getStudentId().equals("STU001")
                        && g.getSchoolClass().getClassId().equals(classId)
                        && g.getFinalGrade().compareTo(new BigDecimal("1.50")) == 0));
    }

    @Test
    void lockAndUnlockToggleEveryRow() {
        stubTrainerLookup();
        Integer classId = 1;
        SchoolClass sc = ownedClass(classId);
        Grade g = new Grade();
        g.setLocked(false);
        when(classRepository.findById(classId)).thenReturn(Optional.of(sc));
        when(gradeRepository.findBySchoolClassClassId(classId)).thenReturn(List.of(g));
        when(gradeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradeService.lockGrades(classId);
        assertTrue(g.isLocked());
        assertNotNull(g.getLockedAt());

        gradeService.unlockGrades(classId);
        assertFalse(g.isLocked());
        assertNull(g.getLockedAt());
    }

    @Test
    void lockRejectsUnassignedTrainer() {
        stubTrainerLookup();
        User other = new User();
        other.setUserId(999);
        SchoolClass sc = new SchoolClass();
        sc.setClassId(1);
        sc.setTrainer(other);
        when(classRepository.findById(1)).thenReturn(Optional.of(sc));
        assertThrows(IllegalArgumentException.class, () -> gradeService.lockGrades(1));
    }
}
