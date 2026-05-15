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
}
