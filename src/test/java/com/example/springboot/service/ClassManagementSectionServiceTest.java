package com.example.springboot.service;

import com.example.springboot.dto.registrar.SectionResponse;
import com.example.springboot.dto.registrar.UpdateSectionRequest;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.Section;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.ClassEnrollmentRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.QualificationRepository;
import com.example.springboot.repository.SchoolClassRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.SubjectRepository;
import com.example.springboot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassManagementSectionServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock QualificationRepository qualificationRepository;
    @Mock SchoolClassRepository classRepository;
    @Mock ClassEnrollmentRepository enrollmentRepository;
    @Mock SectionRepository sectionRepository;
    @Mock BatchRepository batchRepository;
    @Mock CourseRepository courseRepository;
    @Mock UserRepository userRepository;
    @Mock StudentRecordRepository studentRecordRepository;

    @InjectMocks ClassManagementService service;

    Section sampleSection() {
        Batch b = new Batch();
        b.setBatchCode("B2026");
        b.setBatchYear((short) 2026);
        Course c = new Course();
        c.setCourseCode("CARS");
        c.setCourseName("Culinary Arts and Restaurant Services");
        Section s = new Section();
        s.setSectionCode("SEC-A");
        s.setSection("Section A");
        s.setBatch(b);
        s.setCourse(c);
        return s;
    }

    @Test
    void updateSectionRenamesAndReturnsResponse() {
        Section s = sampleSection();
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(s));
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> inv.getArgument(0));

        SectionResponse resp = service.updateSection("SEC-A", new UpdateSectionRequest("Section A - Morning"));

        assertThat(resp.sectionName()).isEqualTo("Section A - Morning");
        assertThat(resp.sectionCode()).isEqualTo("SEC-A");
        verify(sectionRepository).save(s);
        assertThat(s.getSection()).isEqualTo("Section A - Morning");
    }

    @Test
    void updateSectionThrowsWhenNotFound() {
        when(sectionRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSection("MISSING", new UpdateSectionRequest("X")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section not found");
    }

    // Task 5: getStudentsInSection
    @Test
    void getStudentsInSectionMapsAndSortsByLastName() {
        com.example.springboot.model.StudentRecord a = new com.example.springboot.model.StudentRecord();
        a.setStudentId("S0001"); a.setLastName("Cruz"); a.setFirstName("Ana"); a.setStudentStatus("Active");
        com.example.springboot.model.StudentRecord b = new com.example.springboot.model.StudentRecord();
        b.setStudentId("S0002"); b.setLastName("Bautista"); b.setFirstName("Bea"); b.setStudentStatus("Active");
        when(sectionRepository.existsById("SEC-A")).thenReturn(true);
        when(studentRecordRepository.findBySectionSectionCode("SEC-A")).thenReturn(java.util.List.of(a, b));

        var result = service.getStudentsInSection("SEC-A");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).lastName()).isEqualTo("Bautista");
        assertThat(result.get(1).studentId()).isEqualTo("S0001");
    }

    // Task 6: getEligibleStudentsForSection
    @Test
    void getEligibleStudentsForSectionNoFilters() {
        com.example.springboot.model.StudentRecord a = new com.example.springboot.model.StudentRecord();
        a.setStudentId("S0001"); a.setLastName("Cruz"); a.setFirstName("Ana"); a.setStudentStatus("Submitted");
        when(studentRecordRepository.findBySectionIsNullAndStudentStatusIgnoreCase("Submitted"))
                .thenReturn(java.util.List.of(a));

        var result = service.getEligibleStudentsForSection(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo("S0001");
    }

    @Test
    void getEligibleStudentsForSectionWithBothFilters() {
        when(studentRecordRepository
                .findBySectionIsNullAndStudentStatusIgnoreCaseAndBatchBatchCodeAndCourseCourseCode(
                        "Submitted", "B2026", "CARS"))
                .thenReturn(java.util.List.of());

        var result = service.getEligibleStudentsForSection("B2026", "CARS");

        assertThat(result).isEmpty();
    }

    // Task 7: assignStudentsToSection
    @Test
    void assignStudentsToSectionPromotesSubmittedToActive() {
        Section s = sampleSection();
        com.example.springboot.model.StudentRecord eligible = new com.example.springboot.model.StudentRecord();
        eligible.setStudentId("S0001"); eligible.setStudentStatus("Submitted"); eligible.setSection(null);

        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(s));
        when(studentRecordRepository.findByStudentId("S0001")).thenReturn(Optional.of(eligible));

        var result = service.assignStudentsToSection("SEC-A",
                new com.example.springboot.dto.registrar.AssignStudentsToSectionRequest(
                        java.util.List.of("S0001")));

        assertThat(result.assignedCount()).isEqualTo(1);
        assertThat(result.skippedStudentIds()).isEmpty();
        assertThat(eligible.getStudentStatus()).isEqualTo("Active");
        assertThat(eligible.getSection()).isSameAs(s);
        verify(studentRecordRepository).save(eligible);
    }

    @Test
    void assignStudentsToSectionSkipsStudentAlreadyInASection() {
        Section s = sampleSection();
        Section other = new Section();
        other.setSectionCode("SEC-B");
        com.example.springboot.model.StudentRecord taken = new com.example.springboot.model.StudentRecord();
        taken.setStudentId("S0002"); taken.setStudentStatus("Active"); taken.setSection(other);

        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(s));
        when(studentRecordRepository.findByStudentId("S0002")).thenReturn(Optional.of(taken));

        var result = service.assignStudentsToSection("SEC-A",
                new com.example.springboot.dto.registrar.AssignStudentsToSectionRequest(
                        java.util.List.of("S0002")));

        assertThat(result.assignedCount()).isZero();
        assertThat(result.skippedStudentIds()).containsExactly("S0002");
        assertThat(result.reasons().get(0)).contains("already assigned to section SEC-B");
    }

    @Test
    void assignStudentsToSectionSkipsUnknownStudentId() {
        Section s = sampleSection();
        when(sectionRepository.findById("SEC-A")).thenReturn(Optional.of(s));
        when(studentRecordRepository.findByStudentId("S9999")).thenReturn(Optional.empty());

        var result = service.assignStudentsToSection("SEC-A",
                new com.example.springboot.dto.registrar.AssignStudentsToSectionRequest(
                        java.util.List.of("S9999")));

        assertThat(result.assignedCount()).isZero();
        assertThat(result.skippedStudentIds()).containsExactly("S9999");
        assertThat(result.reasons().get(0)).contains("not found");
    }

    @Test
    void assignStudentsToSectionThrowsWhenSectionMissing() {
        when(sectionRepository.findById("MISSING")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assignStudentsToSection("MISSING",
                new com.example.springboot.dto.registrar.AssignStudentsToSectionRequest(
                        java.util.List.of("S0001"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section not found");
    }

    // Task 8: removeStudentFromSection
    @Test
    void removeStudentFromSectionCascadesEnrollmentsAndRevertsStatus() {
        Section s = sampleSection();
        com.example.springboot.model.StudentRecord student = new com.example.springboot.model.StudentRecord();
        student.setStudentId("S0001"); student.setStudentStatus("Active"); student.setSection(s);

        when(sectionRepository.existsById("SEC-A")).thenReturn(true);
        when(studentRecordRepository.findByStudentId("S0001")).thenReturn(Optional.of(student));
        when(enrollmentRepository.deleteByStudentAndSectionCode("S0001", "SEC-A")).thenReturn(3);

        int removed = service.removeStudentFromSection("SEC-A", "S0001");

        assertThat(removed).isEqualTo(3);
        assertThat(student.getSection()).isNull();
        assertThat(student.getStudentStatus()).isEqualTo("Submitted");
        verify(studentRecordRepository).save(student);
    }

    @Test
    void removeStudentFromSectionThrowsWhenStudentNotInThatSection() {
        Section other = new Section(); other.setSectionCode("SEC-B");
        com.example.springboot.model.StudentRecord student = new com.example.springboot.model.StudentRecord();
        student.setStudentId("S0001"); student.setStudentStatus("Active"); student.setSection(other);

        when(sectionRepository.existsById("SEC-A")).thenReturn(true);
        when(studentRecordRepository.findByStudentId("S0001")).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.removeStudentFromSection("SEC-A", "S0001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not assigned to section SEC-A");
    }

    // Task 9: bulkEnrollSectionIntoClass
    @Test
    void bulkEnrollSectionIntoClassEnrollsEligibleAndSkipsRest() {
        Section s = sampleSection();
        com.example.springboot.model.SchoolClass cls = new com.example.springboot.model.SchoolClass();
        cls.setClassId(42);
        cls.setSection(s);

        com.example.springboot.model.StudentRecord activeNew = new com.example.springboot.model.StudentRecord();
        activeNew.setStudentId("S0001"); activeNew.setStudentStatus("Active"); activeNew.setSection(s);
        com.example.springboot.model.StudentRecord activeDup = new com.example.springboot.model.StudentRecord();
        activeDup.setStudentId("S0002"); activeDup.setStudentStatus("Active"); activeDup.setSection(s);
        com.example.springboot.model.StudentRecord ineligible = new com.example.springboot.model.StudentRecord();
        ineligible.setStudentId("S0003"); ineligible.setStudentStatus("Graduated"); ineligible.setSection(s);

        when(classRepository.findById(42)).thenReturn(Optional.of(cls));
        when(studentRecordRepository.findBySectionSectionCode("SEC-A"))
                .thenReturn(java.util.List.of(activeNew, activeDup, ineligible));
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(42, "S0001"))
                .thenReturn(false);
        when(enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(42, "S0002"))
                .thenReturn(true);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.bulkEnrollSectionIntoClass(42);

        assertThat(result.totalConsidered()).isEqualTo(3);
        assertThat(result.enrolledCount()).isEqualTo(1);
        assertThat(result.skippedAlreadyEnrolled()).isEqualTo(1);
        assertThat(result.skippedIneligible()).isEqualTo(1);
    }

    @Test
    void bulkEnrollSectionIntoClassThrowsWhenClassMissing() {
        when(classRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.bulkEnrollSectionIntoClass(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Class not found");
    }
}
