package com.example.springboot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.registrar.CourseCodePreview;
import com.example.springboot.dto.registrar.CreateSectionRequest;
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

/** Create Section: batch and course are resolved by what the registrar typed, or auto-created. */
@ExtendWith(MockitoExtension.class)
class ClassManagementSectionCreateServiceTest {

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

    private final Batch batch = new Batch("B2026A", (short) 2026);
    private final Course cars = new Course("CARS", "Culinary Arts and Restaurant Services");

    @BeforeEach
    void echoSaves() {
        lenient().when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(batchRepository.save(any(Batch.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateSectionRequest request(String batchCode, String course) {
        return new CreateSectionRequest("SEC-A", "Section A", batchCode, course);
    }

    @Test
    void usesAnExistingCourseMatchedByCode() {
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(batch));
        when(courseRepository.findById("CARS")).thenReturn(Optional.of(cars));

        SectionCreationResult result = service.createSection(request("B2026A", "CARS"));

        assertThat(result.section().courseCode()).isEqualTo("CARS");
        assertThat(result.courseCreated()).isFalse();
        assertThat(result.batchCreated()).isFalse();
        verify(courseRepository, never()).save(any());
        verify(batchRepository, never()).save(any());
    }

    @Test
    void usesAnExistingCourseMatchedByNameIgnoringCase() {
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(batch));
        when(courseRepository.findFirstByCourseNameIgnoreCase("culinary arts and restaurant services"))
                .thenReturn(Optional.of(cars));

        SectionCreationResult result = service.createSection(request("B2026A", "culinary arts and restaurant services"));

        assertThat(result.section().courseCode()).isEqualTo("CARS");
        assertThat(result.courseCreated()).isFalse();
    }

    @Test
    void createsANewCourseWithAGeneratedCodeAndTrimmedName() {
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(batch));

        SectionCreationResult result =
                service.createSection(request("B2026A", "  Bread and Pastry Production  "));

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(saved.capture());
        assertThat(saved.getValue().getCourseCode()).isEqualTo("BPP");
        assertThat(saved.getValue().getCourseName()).isEqualTo("Bread and Pastry Production");
        assertThat(result.courseCreated()).isTrue();
        assertThat(result.section().courseCode()).isEqualTo("BPP");
    }

    @Test
    void suffixesTheGeneratedCodeWhenItIsAlreadyTaken() {
        when(batchRepository.findById("B2026A")).thenReturn(Optional.of(batch));
        when(courseRepository.existsById("CARS")).thenReturn(true);

        SectionCreationResult result =
                service.createSection(request("B2026A", "Culinary Arts and Retail Sales"));

        assertThat(result.section().courseCode()).isEqualTo("CARS2");
        assertThat(result.courseCreated()).isTrue();
    }

    @Test
    void createsANewBatchWithTheCurrentYear() {
        when(courseRepository.findById("CARS")).thenReturn(Optional.of(cars));

        SectionCreationResult result = service.createSection(request(" B2027A ", "CARS"));

        ArgumentCaptor<Batch> saved = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepository).save(saved.capture());
        assertThat(saved.getValue().getBatchCode()).isEqualTo("B2027A");
        assertThat(saved.getValue().getBatchYear()).isEqualTo((short) LocalDate.now().getYear());
        assertThat(result.batchCreated()).isTrue();
    }

    @Test
    void rejectsADuplicateSectionCodeBeforeCreatingAnything() {
        when(sectionRepository.existsById("SEC-A")).thenReturn(true);

        assertThatThrownBy(() -> service.createSection(request("B2027A", "Brand New Course")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section code already exists");
        verify(batchRepository, never()).save(any());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void previewReturnsTheExistingCodeForAKnownName() {
        when(courseRepository.findFirstByCourseNameIgnoreCase("Culinary Arts and Restaurant Services"))
                .thenReturn(Optional.of(cars));

        CourseCodePreview preview = service.previewCourseCode(" Culinary Arts and Restaurant Services ");

        assertThat(preview.code()).isEqualTo("CARS");
        assertThat(preview.existing()).isTrue();
    }

    @Test
    void previewGeneratesACodeForANewName() {
        CourseCodePreview preview = service.previewCourseCode("Bread and Pastry Production");

        assertThat(preview.code()).isEqualTo("BPP");
        assertThat(preview.existing()).isFalse();
        verify(courseRepository, never()).save(any());
    }

    @Test
    void previewRejectsABlankName() {
        assertThatThrownBy(() -> service.previewCourseCode("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
