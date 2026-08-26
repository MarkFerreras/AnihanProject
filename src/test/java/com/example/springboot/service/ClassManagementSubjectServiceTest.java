package com.example.springboot.service;

import com.example.springboot.dto.registrar.CreateSubjectRequest;
import com.example.springboot.dto.registrar.SubjectResponse;
import com.example.springboot.dto.registrar.UpdateSubjectRequest;
import com.example.springboot.model.Qualification;
import com.example.springboot.model.Subject;
import com.example.springboot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassManagementSubjectServiceTest {

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

    private Qualification qual;

    @BeforeEach
    void setup() {
        qual = new Qualification();
        qual.setQualificationCode(1);
        qual.setQualificationName("Cookery NC II");
        qual.setQualificationDescription("Cookery National Certificate II");
    }

    @Test
    void createSubjectPersistsAndReturnsResponse() {
        var req = new CreateSubjectRequest("CK-101", "Basic Cookery", "CORE", 1, 3);
        when(subjectRepository.existsById("CK-101")).thenReturn(false);
        when(qualificationRepository.findById(1)).thenReturn(Optional.of(qual));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse resp = service.createSubject(req);

        assertEquals("CK-101", resp.subjectCode());
        assertEquals("Basic Cookery", resp.subjectName());
        assertEquals("CORE", resp.competencyType());
        assertEquals("Cookery NC II", resp.qualificationName());
        assertEquals(3, resp.units());
        verify(subjectRepository).save(any(Subject.class));
    }

    @Test
    void createSubjectRejectsDuplicateCode() {
        var req = new CreateSubjectRequest("CK-101", "Basic Cookery", "CORE", 1, 3);
        when(subjectRepository.existsById("CK-101")).thenReturn(true);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.createSubject(req));
        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubjectRejectsUnknownQualification() {
        var req = new CreateSubjectRequest("CK-101", "Basic Cookery", "CORE", 999, 3);
        when(subjectRepository.existsById("CK-101")).thenReturn(false);
        when(qualificationRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.createSubject(req));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubjectRejectsCoreWithoutQualification() {
        var req = new CreateSubjectRequest("CK-101", "Basic Cookery", "CORE", null, 3);
        when(subjectRepository.existsById("CK-101")).thenReturn(false);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.createSubject(req));
        assertTrue(ex.getMessage().toLowerCase().contains("qualification"));
        verify(subjectRepository, never()).save(any());
        verify(qualificationRepository, never()).findById(any());
    }

    @Test
    void createSubjectAllowsBasicWithoutQualification() {
        var req = new CreateSubjectRequest("BFS-100", "Basic Food Safety", "BASIC", null, 1);
        when(subjectRepository.existsById("BFS-100")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse resp = service.createSubject(req);

        assertEquals("BASIC", resp.competencyType());
        assertNull(resp.qualificationCode());
        assertNull(resp.qualificationName());
        verify(qualificationRepository, never()).findById(any());
        verify(subjectRepository).save(any(Subject.class));
    }

    @Test
    void createSubjectAllowsCommonWithoutQualification() {
        var req = new CreateSubjectRequest("5S-100", "5S", "COMMON", null, 1);
        when(subjectRepository.existsById("5S-100")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse resp = service.createSubject(req);

        assertEquals("COMMON", resp.competencyType());
        assertNull(resp.qualificationCode());
        verify(qualificationRepository, never()).findById(any());
    }

    @Test
    void createSubjectIgnoresQualificationForBasicSubjects() {
        // Even if a qualificationCode is submitted for a Basic subject, it must
        // not be looked up or attached — Basic subjects are never qualification-specific.
        var req = new CreateSubjectRequest("BFS-100", "Basic Food Safety", "BASIC", 1, 1);
        when(subjectRepository.existsById("BFS-100")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse resp = service.createSubject(req);

        assertNull(resp.qualificationCode());
        verify(qualificationRepository, never()).findById(any());
    }

    @Test
    void updateSubjectMutatesNameQualificationAndUnits() {
        Subject existing = new Subject();
        existing.setSubjectCode("CK-101");
        existing.setSubjectName("Old Name");
        existing.setCompetencyType("CORE");
        existing.setQualification(qual);
        existing.setUnits(2);

        Qualification newQual = new Qualification();
        newQual.setQualificationCode(2);
        newQual.setQualificationName("Bread and Pastry NC II");
        newQual.setQualificationDescription("Bread and Pastry NC II");

        when(subjectRepository.existsById("CK-101")).thenReturn(true);
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(existing));
        when(qualificationRepository.findById(2)).thenReturn(Optional.of(newQual));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse resp = service.updateSubject("CK-101",
                new UpdateSubjectRequest("CK-101", "New Name", "CORE", 2, 4));

        assertEquals("CK-101", resp.subjectCode());
        assertEquals("New Name", resp.subjectName());
        assertEquals("Bread and Pastry NC II", resp.qualificationName());
        assertEquals(4, resp.units());
        verify(subjectRepository, never()).renameSubjectCode(any(), any());
    }

    @Test
    void updateSubjectThrowsWhenSubjectMissing() {
        when(subjectRepository.existsById("MISSING")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.updateSubject("MISSING",
                new UpdateSubjectRequest("MISSING", "X", "CORE", 1, 3)));
    }

    @Test
    void updateSubjectRejectsCoreWithoutQualification() {
        Subject existing = new Subject();
        existing.setSubjectCode("CK-101");
        existing.setSubjectName("Old Name");
        existing.setCompetencyType("CORE");
        existing.setQualification(qual);
        existing.setUnits(2);
        when(subjectRepository.existsById("CK-101")).thenReturn(true);
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(existing));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.updateSubject("CK-101",
                new UpdateSubjectRequest("CK-101", "New Name", "CORE", null, 3)));
        assertTrue(ex.getMessage().toLowerCase().contains("qualification"));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void updateSubjectCanConvertCoreSubjectToBasicClearingQualification() {
        Subject existing = new Subject();
        existing.setSubjectCode("CK-101");
        existing.setSubjectName("Old Name");
        existing.setCompetencyType("CORE");
        existing.setQualification(qual);
        existing.setUnits(2);
        when(subjectRepository.existsById("CK-101")).thenReturn(true);
        when(subjectRepository.findById("CK-101")).thenReturn(Optional.of(existing));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse resp = service.updateSubject("CK-101",
                new UpdateSubjectRequest("CK-101", "New Name", "BASIC", null, 3));

        assertEquals("BASIC", resp.competencyType());
        assertNull(resp.qualificationCode());
        verify(qualificationRepository, never()).findById(any());
    }

    @Test
    void updateSubjectRenamesCodeAndReloadsUnderNewCode() {
        Subject renamed = new Subject();
        renamed.setSubjectCode("CK-101-NEW");
        renamed.setSubjectName("Old Name");
        renamed.setCompetencyType("CORE");
        renamed.setQualification(qual);
        renamed.setUnits(2);

        when(subjectRepository.existsById("CK-101")).thenReturn(true);
        when(subjectRepository.existsById("CK-101-NEW")).thenReturn(false);
        when(subjectRepository.findById("CK-101-NEW")).thenReturn(Optional.of(renamed));
        when(qualificationRepository.findById(1)).thenReturn(Optional.of(qual));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse resp = service.updateSubject("CK-101",
                new UpdateSubjectRequest("CK-101-NEW", "New Name", "CORE", 1, 4));

        assertEquals("CK-101-NEW", resp.subjectCode());
        assertEquals("New Name", resp.subjectName());
        assertEquals(4, resp.units());
        verify(subjectRepository).renameSubjectCode("CK-101", "CK-101-NEW");
        // Must reload under the new code — a managed entity's @Id can't be
        // mutated in place, so findById("CK-101") must never be used for the
        // subsequent field updates.
        verify(subjectRepository, never()).findById("CK-101");
    }

    @Test
    void updateSubjectRejectsRenameToAlreadyExistingCode() {
        when(subjectRepository.existsById("CK-101")).thenReturn(true);
        when(subjectRepository.existsById("TAKEN-CODE")).thenReturn(true);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.updateSubject("CK-101",
                new UpdateSubjectRequest("TAKEN-CODE", "New Name", "CORE", 1, 4)));
        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));
        verify(subjectRepository, never()).renameSubjectCode(any(), any());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void deleteSubjectDeletesWhenNoFkReferences() {
        when(subjectRepository.existsById("CK-101")).thenReturn(true);
        when(classRepository.existsBySubjectSubjectCode("CK-101")).thenReturn(false);
        when(subjectRepository.countGradesBySubjectCode("CK-101")).thenReturn(0L);

        service.deleteSubject("CK-101");

        verify(subjectRepository).deleteById("CK-101");
    }

    @Test
    void deleteSubjectBlockedByExistingClasses() {
        when(subjectRepository.existsById("CK-101")).thenReturn(true);
        when(classRepository.existsBySubjectSubjectCode("CK-101")).thenReturn(true);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.deleteSubject("CK-101"));
        assertTrue(ex.getMessage().toLowerCase().contains("class"));
        verify(subjectRepository, never()).deleteById(any());
    }

    @Test
    void deleteSubjectBlockedByExistingGrades() {
        when(subjectRepository.existsById("CK-101")).thenReturn(true);
        when(classRepository.existsBySubjectSubjectCode("CK-101")).thenReturn(false);
        when(subjectRepository.countGradesBySubjectCode("CK-101")).thenReturn(5L);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.deleteSubject("CK-101"));
        assertTrue(ex.getMessage().toLowerCase().contains("grade"));
        verify(subjectRepository, never()).deleteById(any());
    }

    @Test
    void deleteSubjectThrowsWhenSubjectMissing() {
        when(subjectRepository.existsById("MISSING")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.deleteSubject("MISSING"));
    }
}
