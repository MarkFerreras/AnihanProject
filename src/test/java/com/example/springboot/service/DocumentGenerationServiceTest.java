package com.example.springboot.service;

import com.example.springboot.dto.registrar.DocumentGenerateDataResponse;
import com.example.springboot.model.Grade;
import com.example.springboot.model.Parent;
import com.example.springboot.model.StudentEducation;
import com.example.springboot.model.StudentOjt;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.StudentTesdaQualification;
import com.example.springboot.model.Subject;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentOjtRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentTesdaQualificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentGenerationServiceTest {

    @Mock private StudentRecordRepository studentRecordRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private StudentEducationRepository educationRepository;
    @Mock private StudentTesdaQualificationRepository tesdaRepository;
    @Mock private StudentOjtRepository ojtRepository;
    @Mock private GradeRepository gradeRepository;

    @InjectMocks private DocumentGenerationService service;

    @Test
    void aggregatesAllPartsForStudent() {
        StudentRecord record = new StudentRecord();
        record.setStudentId("SR20260001");
        record.setLastName("Dela Cruz");
        record.setFirstName("Maria");

        Parent father = new Parent();
        father.setRelation("Father");
        father.setFirstName("Juan");
        father.setFamilyName("Dela Cruz");
        father.setAddress("Calamba City");

        StudentEducation elem = new StudentEducation();
        elem.setStudentId("SR20260001");
        elem.setLevel("Elementary");
        elem.setSchoolName("Calamba Elementary School");
        elem.setEndedYear("2016");

        StudentTesdaQualification tesda = new StudentTesdaQualification();
        tesda.setStudentId("SR20260001");
        tesda.setSlot(1);
        tesda.setTitle("Bread and Pastry Production NC II");
        tesda.setResult("Competent");

        StudentOjt ojt = new StudentOjt();
        ojt.setStudentId("SR20260001");
        ojt.setCompanyName("Hotel ABC");
        ojt.setHoursRendered(new BigDecimal("900.00"));

        Subject subject = new Subject();
        subject.setSubjectCode("TRS741342");
        Grade grade = new Grade();
        grade.setSubject(subject);
        grade.setFinalPercentage(new BigDecimal("93"));
        grade.setFinalGrade(new BigDecimal("1.50"));
        grade.setRemarks("COMPETENT");

        when(studentRecordRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(record));
        when(parentRepository.findByStudentStudentId("SR20260001")).thenReturn(List.of(father));
        when(educationRepository.findByStudentIdOrderByLevel("SR20260001")).thenReturn(List.of(elem));
        when(tesdaRepository.findByStudentIdOrderBySlot("SR20260001")).thenReturn(List.of(tesda));
        when(ojtRepository.findByStudentId("SR20260001")).thenReturn(Optional.of(ojt));
        when(gradeRepository.findByStudentStudentId("SR20260001")).thenReturn(List.of(grade));

        DocumentGenerateDataResponse response = service.getGenerateData("SR20260001");

        assertEquals("SR20260001", response.student().studentId());
        assertNull(response.student().batchCode());

        assertEquals(1, response.parents().size());
        assertEquals("Juan Dela Cruz", response.parents().get(0).fullName());

        assertEquals(1, response.education().size());
        assertEquals("Elementary", response.education().get(0).level());

        assertEquals(1, response.tesdaQualifications().size());
        assertEquals("Competent", response.tesdaQualifications().get(0).result());

        assertNotNull(response.ojt());
        assertEquals("Hotel ABC", response.ojt().companyName());

        assertEquals(1, response.grades().size());
        assertEquals("TRS741342", response.grades().get(0).subjectCode());
        assertEquals("1.50", response.grades().get(0).finalGrade());
        assertEquals("Competent", response.grades().get(0).remarks());
    }

    @Test
    void returnsNullOjtWhenStudentHasNone() {
        StudentRecord record = new StudentRecord();
        record.setStudentId("SR20260002");

        when(studentRecordRepository.findByStudentId("SR20260002")).thenReturn(Optional.of(record));
        when(parentRepository.findByStudentStudentId("SR20260002")).thenReturn(List.of());
        when(educationRepository.findByStudentIdOrderByLevel("SR20260002")).thenReturn(List.of());
        when(tesdaRepository.findByStudentIdOrderBySlot("SR20260002")).thenReturn(List.of());
        when(ojtRepository.findByStudentId("SR20260002")).thenReturn(Optional.empty());
        when(gradeRepository.findByStudentStudentId("SR20260002")).thenReturn(List.of());

        DocumentGenerateDataResponse response = service.getGenerateData("SR20260002");

        assertNull(response.ojt());
        assertTrue(response.parents().isEmpty());
        assertTrue(response.grades().isEmpty());
    }

    @Test
    void throwsWhenStudentMissing() {
        when(studentRecordRepository.findByStudentId("NOPE")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getGenerateData("NOPE"));
    }
}