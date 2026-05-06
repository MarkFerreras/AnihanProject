package com.example.springboot.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.dto.student.OjtDto;
import com.example.springboot.dto.student.SchoolYearDto;
import com.example.springboot.dto.student.TesdaQualDto;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentOjt;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.StudentSchoolYear;
import com.example.springboot.model.StudentTesdaQualification;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentOjtRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentSchoolYearRepository;
import com.example.springboot.repository.StudentTesdaQualificationRepository;

@Service
public class RegistrarService {

    private final StudentRecordRepository studentRecordRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final StudentOjtRepository studentOjtRepository;
    private final StudentTesdaQualificationRepository tesdaQualRepository;
    private final StudentSchoolYearRepository schoolYearRepository;

    public RegistrarService(StudentRecordRepository studentRecordRepository,
                            BatchRepository batchRepository,
                            CourseRepository courseRepository,
                            SectionRepository sectionRepository,
                            StudentOjtRepository studentOjtRepository,
                            StudentTesdaQualificationRepository tesdaQualRepository,
                            StudentSchoolYearRepository schoolYearRepository) {
        this.studentRecordRepository = studentRecordRepository;
        this.batchRepository = batchRepository;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.studentOjtRepository = studentOjtRepository;
        this.tesdaQualRepository = tesdaQualRepository;
        this.schoolYearRepository = schoolYearRepository;
    }

    public List<StudentRecordSummaryResponse> getAllRecords() {
        return getAllRecords(null, null, null, null);
    }

    public List<StudentRecordSummaryResponse> getAllRecords(String query) {
        return getAllRecords(query, null, null, null);
    }

    public List<StudentRecordSummaryResponse> getAllRecords(String query, Integer fromYear, Integer toYear) {
        return getAllRecords(query, fromYear, toYear, null);
    }

    /**
     * Returns all student records filtered by free-text query, batch year range, and/or student status.
     * All three filters are optional and combined with AND logic.
     */
    public List<StudentRecordSummaryResponse> getAllRecords(String query, Integer fromYear, Integer toYear, String status) {
        List<StudentRecord> records = studentRecordRepository.findAll();
        String q = (query == null || query.isBlank()) ? null : query.toLowerCase().trim();
        boolean yearFilterActive = fromYear != null || toYear != null;
        String normalizedStatus = (status == null || status.isBlank()) ? null : status.trim();

        return records.stream()
                .filter(r -> q == null || matchesQuery(r, q))
                .filter(r -> !yearFilterActive || matchesYearRange(r, fromYear, toYear))
                .filter(r -> normalizedStatus == null || matchesStatus(r, normalizedStatus))
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

    private boolean matchesYearRange(StudentRecord r, Integer fromYear, Integer toYear) {
        if (r.getBatch() == null || r.getBatch().getBatchYear() == null) {
            return false;
        }
        int year = r.getBatch().getBatchYear().intValue();
        if (fromYear != null && year < fromYear) {
            return false;
        }
        if (toYear != null && year > toYear) {
            return false;
        }
        return true;
    }

    private boolean matchesStatus(StudentRecord r, String status) {
        return r.getStudentStatus() != null && r.getStudentStatus().equalsIgnoreCase(status);
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    public StudentRecordDetailsResponse getRecordById(Integer recordId) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));
        return buildDetailsResponse(record);
    }

    @Transactional
    public StudentRecordDetailsResponse updateRecord(Integer recordId, StudentRecordUpdateRequest request) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

        String oldStudentId = record.getStudentId();

        if (!oldStudentId.equalsIgnoreCase(request.studentId())) {
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
        String newStudentId = saved.getStudentId();

        saveOjt(oldStudentId, newStudentId, request.ojt());
        saveTesda(oldStudentId, newStudentId, request.tesdaQualifications());
        saveSchoolYears(oldStudentId, newStudentId, request.schoolYears());

        return buildDetailsResponse(saved);
    }

    // ----- OJT -----

    private void saveOjt(String oldStudentId, String newStudentId, OjtDto dto) {
        if (dto == null || isOjtBlank(dto)) {
            studentOjtRepository.deleteByStudentId(oldStudentId);
            studentOjtRepository.flush();
            return;
        }
        StudentOjt ojt = studentOjtRepository.findByStudentId(oldStudentId)
                .orElseGet(StudentOjt::new);
        ojt.setStudentId(newStudentId);
        ojt.setCompanyName(emptyToNull(dto.companyName()));
        ojt.setCompanyAddress(emptyToNull(dto.companyAddress()));
        ojt.setHoursRendered(dto.hoursRendered());
        studentOjtRepository.save(ojt);
    }

    private boolean isOjtBlank(OjtDto dto) {
        return (dto.companyName() == null || dto.companyName().isBlank())
                && (dto.companyAddress() == null || dto.companyAddress().isBlank())
                && dto.hoursRendered() == null;
    }

    // ----- TESDA -----

    private void saveTesda(String oldStudentId, String newStudentId, List<TesdaQualDto> slots) {
        tesdaQualRepository.deleteByStudentId(oldStudentId);
        tesdaQualRepository.flush();
        if (slots == null) return;
        for (TesdaQualDto dto : slots) {
            if (isTesdaBlank(dto)) continue;
            StudentTesdaQualification q = new StudentTesdaQualification();
            q.setStudentId(newStudentId);
            q.setSlot(dto.slot());
            q.setTitle(emptyToNull(dto.title()));
            q.setCenterAddress(emptyToNull(dto.centerAddress()));
            q.setAssessmentDate(dto.assessmentDate());
            q.setResult(emptyToNull(dto.result()));
            tesdaQualRepository.save(q);
        }
    }

    private boolean isTesdaBlank(TesdaQualDto dto) {
        return (dto.title() == null || dto.title().isBlank())
                && (dto.centerAddress() == null || dto.centerAddress().isBlank())
                && dto.assessmentDate() == null
                && (dto.result() == null || dto.result().isBlank());
    }

    // ----- School Years -----

    private void saveSchoolYears(String oldStudentId, String newStudentId, List<SchoolYearDto> rows) {
        schoolYearRepository.deleteByStudentId(oldStudentId);
        schoolYearRepository.flush();
        if (rows == null) return;
        int index = 1;
        for (SchoolYearDto dto : rows) {
            if (isSchoolYearBlank(dto)) continue;
            StudentSchoolYear sy = new StudentSchoolYear();
            sy.setStudentId(newStudentId);
            sy.setRowIndex(index++);
            sy.setSyStart(emptyToNull(dto.syStart()));
            sy.setSemStart(emptyToNull(dto.semStart()));
            sy.setSyEnd(emptyToNull(dto.syEnd()));
            sy.setSemEnd(emptyToNull(dto.semEnd()));
            sy.setRemarks(emptyToNull(dto.remarks()));
            schoolYearRepository.save(sy);
        }
    }

    private boolean isSchoolYearBlank(SchoolYearDto dto) {
        return (dto.syStart() == null || dto.syStart().isBlank())
                && (dto.semStart() == null || dto.semStart().isBlank())
                && (dto.syEnd() == null || dto.syEnd().isBlank())
                && (dto.semEnd() == null || dto.semEnd().isBlank())
                && (dto.remarks() == null || dto.remarks().isBlank());
    }

    // ----- Response builder -----

    private StudentRecordDetailsResponse buildDetailsResponse(StudentRecord record) {
        String studentId = record.getStudentId();

        OjtDto ojt = studentOjtRepository.findByStudentId(studentId)
                .map(e -> new OjtDto(e.getCompanyName(), e.getCompanyAddress(), e.getHoursRendered()))
                .orElse(null);

        List<TesdaQualDto> tesda = tesdaQualRepository.findByStudentIdOrderBySlot(studentId)
                .stream()
                .map(e -> new TesdaQualDto(e.getSlot(), e.getTitle(), e.getCenterAddress(),
                        e.getAssessmentDate(), e.getResult()))
                .toList();

        List<SchoolYearDto> schoolYears = schoolYearRepository.findByStudentIdOrderByRowIndex(studentId)
                .stream()
                .map(e -> new SchoolYearDto(e.getRowIndex(), e.getSyStart(), e.getSemStart(),
                        e.getSyEnd(), e.getSemEnd(), e.getRemarks()))
                .toList();

        return StudentRecordDetailsResponse.from(record, ojt, tesda, schoolYears);
    }

    // ----- FK resolvers -----

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
