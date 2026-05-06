package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.springboot.dto.student.EducationItemDto;
import com.example.springboot.dto.student.GuardianDto;
import com.example.springboot.dto.student.ParentDto;
import com.example.springboot.dto.student.SchoolYearDto;
import com.example.springboot.dto.student.StudentDetailsRequest;
import com.example.springboot.dto.student.StudentDetailsResponse;
import com.example.springboot.model.OtherGuardian;
import com.example.springboot.model.Parent;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.OtherGuardianRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentSchoolYearRepository;
import com.example.springboot.repository.StudentUploadRepository;

@ExtendWith(MockitoExtension.class)
class StudentDetailsServiceTest {

    @Mock private StudentRecordRepository studentRecordRepo;
    @Mock private ParentRepository parentRepo;
    @Mock private OtherGuardianRepository guardianRepo;
    @Mock private StudentEducationRepository educationRepo;
    @Mock private StudentSchoolYearRepository schoolYearRepo;
    @Mock private StudentUploadRepository uploadRepo;
    @Mock private BatchRepository batchRepo;

    @InjectMocks
    private StudentDetailsService service;

    // ─── helpers ────────────────────────────────────────────────────────────

    private StudentRecord buildMinimalRecord(String studentId, String status) {
        StudentRecord r = new StudentRecord();
        r.setStudentId(studentId);
        r.setLastName("Reyes");
        r.setFirstName("Anna");
        r.setMiddleName("Cruz");
        r.setStudentStatus(status);
        return r;
    }

    private void stubRelationsLenient() {
        Mockito.lenient().when(parentRepo.findByStudentStudentIdAndRelation(anyString(), anyString()))
                .thenReturn(Optional.empty());
        Mockito.lenient().when(guardianRepo.findByStudentStudentId(anyString()))
                .thenReturn(Collections.emptyList());
        Mockito.lenient().when(educationRepo.findByStudentIdOrderByLevel(anyString()))
                .thenReturn(Collections.emptyList());
        Mockito.lenient().when(schoolYearRepo.findByStudentIdOrderByRowIndex(anyString()))
                .thenReturn(Collections.emptyList());
        Mockito.lenient().when(uploadRepo.findByStudentIdAndKind(anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    private StudentDetailsRequest buildFullRequest() {
        ParentDto father = new ParentDto("Reyes", "Jose", "M",
                LocalDate.of(1975, 3, 15), "Engineer", new BigDecimal("50000"),
                "+63 917 123 4567", "jose@test.com", "123 Main St");
        ParentDto mother = new ParentDto("Reyes", "Maria", "S",
                LocalDate.of(1978, 7, 20), "Teacher", new BigDecimal("35000"),
                "+63 917 765 4321", "maria@test.com", "123 Main St");
        GuardianDto guardian = new GuardianDto("Uncle", "Santos", "Pedro", "L",
                LocalDate.of(1970, 1, 1), "456 Other St");

        List<EducationItemDto> education = List.of(
                new EducationItemDto("Elementary", "ABC School", "Manila",
                        "Grade 6", null, "2018"),
                new EducationItemDto("Secondary", "XYZ High", "Quezon City",
                        "Grade 12", null, "2022")
        );

        List<SchoolYearDto> schoolYears = List.of(
                new SchoolYearDto(1, "2025-2026", "1st", "2025-2026", "2nd", null)
        );

        return new StudentDetailsRequest(
                "Reyes", "Anna", "Cruz",
                "+63 917 000 0000",
                LocalDate.of(2004, 6, 15),
                "Female", "Single",
                "123 Permanent St", "456 Temporary St",
                2, 1, 1,
                "Roman Catholic",
                true, LocalDate.of(2004, 8, 1), "San Pedro Parish",
                father, mother, guardian,
                education, schoolYears
        );
    }

    // ─── startOrResume ──────────────────────────────────────────────────────

    @Nested
    class StartOrResume {

        @Test
        void createsMinimalRecordWhenNoExistingStudent() {
            when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                    "Reyes", "Anna", "Cruz")).thenReturn(Optional.empty());
            when(studentRecordRepo.countByStudentIdStartingWith(anyString())).thenReturn(0L);
            when(studentRecordRepo.save(any(StudentRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // buildResponse will query relations — use lenient stubs
            stubRelationsLenient();

            StudentDetailsResponse result = service.startOrResume("Reyes", "Anna", "Cruz");

            assertNotNull(result);
            assertEquals("Reyes", result.lastName());
            assertEquals("Anna", result.firstName());
            assertEquals("Cruz", result.middleName());
            assertEquals("Enrolling", result.studentStatus());

            // Verify only name + status saved — no personal details
            ArgumentCaptor<StudentRecord> captor = ArgumentCaptor.forClass(StudentRecord.class);
            verify(studentRecordRepo).save(captor.capture());
            StudentRecord saved = captor.getValue();
            assertEquals("Enrolling", saved.getStudentStatus());
            assertNull(saved.getBirthdate(), "Birthdate must NOT be saved at start");
            assertNull(saved.getContactNo(), "Contact must NOT be saved at start");
            assertNull(saved.getSex(), "Sex must NOT be saved at start");
        }

        @Test
        void resumesExistingRecordWithoutCreatingNew() {
            StudentRecord existing = buildMinimalRecord("SR20260001", "Enrolling");
            when(studentRecordRepo.findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                    "Reyes", "Anna", "Cruz")).thenReturn(Optional.of(existing));
            stubRelationsLenient();

            StudentDetailsResponse result = service.startOrResume("Reyes", "Anna", "Cruz");

            assertEquals("SR20260001", result.studentId());
            verify(studentRecordRepo, never()).save(any());
        }
    }

    // ─── submitEnrollment ───────────────────────────────────────────────────

    @Nested
    class SubmitEnrollment {

        @Test
        void persistsAllDataAndSetsStatusToSubmitted() {
            StudentRecord record = buildMinimalRecord("SR20260001", "Enrolling");

            // findByStudentId is called twice: once at top, once at bottom for buildResponse
            when(studentRecordRepo.findByStudentId("SR20260001")).thenReturn(Optional.of(record));
            when(studentRecordRepo.save(any(StudentRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Parent upserts
            when(parentRepo.findByStudentStudentIdAndRelation("SR20260001", "FATHER"))
                    .thenReturn(Optional.empty());
            when(parentRepo.findByStudentStudentIdAndRelation("SR20260001", "MOTHER"))
                    .thenReturn(Optional.empty());
            when(parentRepo.save(any(Parent.class))).thenAnswer(inv -> inv.getArgument(0));

            // Guardian
            when(guardianRepo.findByStudentStudentId("SR20260001")).thenReturn(Collections.emptyList());
            when(guardianRepo.save(any(OtherGuardian.class))).thenAnswer(inv -> inv.getArgument(0));

            // Education
            when(educationRepo.findByStudentIdAndLevel(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            Mockito.lenient().when(educationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // School years
            when(schoolYearRepo.findByStudentIdOrderByRowIndex("SR20260001"))
                    .thenReturn(Collections.emptyList());
            Mockito.lenient().when(schoolYearRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Uploads (for buildResponse)
            Mockito.lenient().when(uploadRepo.findByStudentIdAndKind(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            // Education list (for buildResponse)
            Mockito.lenient().when(educationRepo.findByStudentIdOrderByLevel(anyString()))
                    .thenReturn(Collections.emptyList());

            StudentDetailsRequest req = buildFullRequest();
            StudentDetailsResponse result = service.submitEnrollment("SR20260001", req);

            // Verify status changed on saved record
            ArgumentCaptor<StudentRecord> captor = ArgumentCaptor.forClass(StudentRecord.class);
            verify(studentRecordRepo, atLeastOnce()).save(captor.capture());
            StudentRecord saved = captor.getValue();
            assertEquals("Submitted", saved.getStudentStatus());
            assertEquals("Female", saved.getSex());
            assertEquals(LocalDate.of(2004, 6, 15), saved.getBirthdate());
            assertNotNull(saved.getAge());
            assertEquals("+63 917 000 0000", saved.getContactNo());

            // Verify parents saved (father + mother = 2 calls)
            verify(parentRepo, Mockito.times(2)).save(any(Parent.class));

            // Verify guardian saved
            verify(guardianRepo).save(any(OtherGuardian.class));
        }

        @Test
        void rejectsDoubleSubmit() {
            StudentRecord record = buildMinimalRecord("SR20260001", "Submitted");
            when(studentRecordRepo.findByStudentId("SR20260001")).thenReturn(Optional.of(record));

            StudentDetailsRequest req = buildFullRequest();

            assertThrows(IllegalStateException.class,
                    () -> service.submitEnrollment("SR20260001", req));
            verify(studentRecordRepo, never()).save(any());
        }

        @Test
        void throwsForNonExistentStudent() {
            when(studentRecordRepo.findByStudentId("INVALID")).thenReturn(Optional.empty());

            StudentDetailsRequest req = buildFullRequest();

            assertThrows(IllegalArgumentException.class,
                    () -> service.submitEnrollment("INVALID", req));
        }
    }

    // ─── load ───────────────────────────────────────────────────────────────

    @Nested
    class Load {

        @Test
        void returnsPopulatedResponseForExistingStudent() {
            StudentRecord record = buildMinimalRecord("SR20260001", "Enrolling");
            record.setBirthdate(LocalDate.of(2004, 6, 15));
            record.setSex("Female");
            when(studentRecordRepo.findByStudentId("SR20260001")).thenReturn(Optional.of(record));
            stubRelationsLenient();

            StudentDetailsResponse result = service.load("SR20260001");

            assertEquals("SR20260001", result.studentId());
            assertEquals("Female", result.sex());
        }

        @Test
        void throwsForNonExistentStudent() {
            when(studentRecordRepo.findByStudentId("INVALID")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.load("INVALID"));
        }
    }
}
