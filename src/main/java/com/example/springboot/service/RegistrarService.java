package com.example.springboot.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;

@Service
public class RegistrarService {

    private final StudentRecordRepository studentRecordRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;

    public RegistrarService(StudentRecordRepository studentRecordRepository,
                            BatchRepository batchRepository,
                            CourseRepository courseRepository,
                            SectionRepository sectionRepository) {
        this.studentRecordRepository = studentRecordRepository;
        this.batchRepository = batchRepository;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
    }

    public List<StudentRecordSummaryResponse> getAllRecords() {
        return getAllRecords(null);
    }

    /**
     * Returns all student records, optionally filtered by a free-text query.
     * The query is matched case-insensitively against: recordId, studentId,
     * lastName, firstName, middleName, studentStatus, batchCode, courseCode,
     * and sectionCode.
     */
    public List<StudentRecordSummaryResponse> getAllRecords(String query) {
        List<StudentRecord> records = studentRecordRepository.findAll();
        if (query == null || query.isBlank()) {
            return records.stream()
                    .map(StudentRecordSummaryResponse::from)
                    .toList();
        }
        String q = query.toLowerCase().trim();
        return records.stream()
                .filter(r -> matchesQuery(r, q))
                .map(StudentRecordSummaryResponse::from)
                .toList();
    }

    private boolean matchesQuery(StudentRecord r, String q) {
        return contains(String.valueOf(r.getRecordId()), q)
                || contains(r.getStudentId(), q)
                || contains(r.getLastName(), q)
                || contains(r.getFirstName(), q)
                || contains(r.getMiddleName(), q)
                || contains(r.getStudentStatus(), q)
                || (r.getBatch() != null && contains(r.getBatch().getBatchCode(), q))
                || (r.getCourse() != null && contains(r.getCourse().getCourseCode(), q))
                || (r.getSection() != null && contains(r.getSection().getSectionCode(), q));
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    public StudentRecordDetailsResponse getRecordById(Integer recordId) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));
        return StudentRecordDetailsResponse.from(record);
    }

    public StudentRecordDetailsResponse updateRecord(Integer recordId, StudentRecordUpdateRequest request) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

        if (!record.getStudentId().equalsIgnoreCase(request.studentId())) {
            studentRecordRepository.findByStudentId(request.studentId())
                    .filter(other -> !other.getRecordId().equals(recordId))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Student ID is already in use: " + request.studentId());
                    });
            record.setStudentId(request.studentId());
        }

        record.setLastName(request.lastName());
        record.setFirstName(request.firstName());
        record.setMiddleName(request.middleName());
        record.setBirthdate(request.birthdate());
        record.setAge(AgeCalculator.calculateAge(request.birthdate()));
        record.setSex(emptyToNull(request.sex()));
        record.setCivilStatus(emptyToNull(request.civilStatus()));
        record.setPermanentAddress(emptyToNull(request.permanentAddress()));
        record.setTemporaryAddress(emptyToNull(request.temporaryAddress()));
        record.setEmail(emptyToNull(request.email()));
        record.setContactNo(emptyToNull(request.contactNo()));
        record.setReligion(emptyToNull(request.religion()));
        record.setBaptized(Boolean.TRUE.equals(request.baptized()));
        record.setBaptismDate(request.baptismDate());
        record.setBaptismPlace(emptyToNull(request.baptismPlace()));
        record.setSiblingCount(request.siblingCount());
        record.setBrotherCount(request.brotherCount());
        record.setSisterCount(request.sisterCount());
        record.setStudentStatus(request.studentStatus());

        record.setBatch(resolveBatch(request.batchCode()));
        record.setCourse(resolveCourse(request.courseCode()));
        record.setSection(resolveSection(request.sectionCode()));

        StudentRecord saved = studentRecordRepository.save(record);
        return StudentRecordDetailsResponse.from(saved);
    }

    private Batch resolveBatch(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return batchRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Batch code does not exist: " + code));
    }

    private Course resolveCourse(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return courseRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Course code does not exist: " + code));
    }

    private Section resolveSection(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return sectionRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Section code does not exist: " + code));
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
