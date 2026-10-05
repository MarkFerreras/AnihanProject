package com.example.springboot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.OtherGuardianRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentSchoolYearRepository;

/**
 * ISO 25010 characteristic: Functional suitability (correctness) - the internal reference number
 * {@code SR{year}{seq}} is generated per calendar year and never reused.
 */
@ExtendWith(MockitoExtension.class)
class StudentIdGenerationServiceTest {

    @Mock private StudentRecordRepository studentRecordRepo;
    @Mock private ParentRepository parentRepo;
    @Mock private OtherGuardianRepository guardianRepo;
    @Mock private StudentEducationRepository educationRepo;
    @Mock private StudentSchoolYearRepository schoolYearRepo;

    @InjectMocks private StudentDetailsService service;

    private static final int YEAR = LocalDate.now().getYear();
    private static final String PREFIX = "SR" + YEAR;

    private String startAndGetId() {
        when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                any(), any(), any())).thenReturn(List.of());
        service.startOrResume("Santos", "Ana", "Reyes");
        ArgumentCaptor<StudentRecord> saved = ArgumentCaptor.forClass(StudentRecord.class);
        verify(studentRecordRepo).save(saved.capture());
        assertThat(saved.getValue().getStudentStatus()).isEqualTo("Enrolling");
        return saved.getValue().getStudentId();
    }

    @Test
    void studentIdGeneratesSrYearSequenceAndRollsOverAtNewYear() {
        // First record of the year: sequence starts at 0001.
        when(studentRecordRepo.findMaxStudentIdWithPrefix(PREFIX)).thenReturn(Optional.empty());
        assertThat(startAndGetId()).isEqualTo(PREFIX + "0001");
    }

    @Test
    void studentIdIncrementsFromTheHighestExistingSequenceOfTheYear() {
        when(studentRecordRepo.findMaxStudentIdWithPrefix(PREFIX)).thenReturn(Optional.of(PREFIX + "0007"));
        assertThat(startAndGetId()).isEqualTo(PREFIX + "0008");
    }

    @Test
    void studentIdSequenceRestartsAtTheNewYearAndIgnoresPriorYears() {
        // Records from the previous year exist, but the lookup is scoped to the current year's
        // prefix, so a new year starts again at 0001 and the old prefix is never consulted.
        String previousPrefix = "SR" + (YEAR - 1);
        lenient().when(studentRecordRepo.findMaxStudentIdWithPrefix(previousPrefix))
                .thenReturn(Optional.of(previousPrefix + "0099"));
        when(studentRecordRepo.findMaxStudentIdWithPrefix(PREFIX)).thenReturn(Optional.empty());

        assertThat(startAndGetId()).isEqualTo(PREFIX + "0001");
        verify(studentRecordRepo, never()).findMaxStudentIdWithPrefix(previousPrefix);
    }

    @Test
    void studentIdSequenceIsZeroPaddedToFourDigits() {
        when(studentRecordRepo.findMaxStudentIdWithPrefix(PREFIX)).thenReturn(Optional.of(PREFIX + "0099"));
        assertThat(startAndGetId()).isEqualTo(PREFIX + "0100");
    }
}
