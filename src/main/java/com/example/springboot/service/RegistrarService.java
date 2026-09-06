package com.example.springboot.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.dto.student.GuardianDto;
import com.example.springboot.dto.student.OjtDto;
import com.example.springboot.dto.student.ParentDto;
import com.example.springboot.dto.student.SchoolYearDto;
import com.example.springboot.dto.student.TesdaQualDto;
import com.example.springboot.model.Batch;
import com.example.springboot.model.Course;
import com.example.springboot.model.OtherGuardian;
import com.example.springboot.model.Parent;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentOjt;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.StudentSchoolYear;
import com.example.springboot.model.StudentTesdaQualification;
import com.example.springboot.model.StudentUpload;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.GradeRepository;
import com.example.springboot.repository.OtherGuardianRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentOjtRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentSchoolYearRepository;
import com.example.springboot.repository.StudentTesdaQualificationRepository;
import com.example.springboot.repository.StudentUploadRepository;

@Service
public class RegistrarService {

    private final StudentRecordRepository studentRecordRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final StudentOjtRepository studentOjtRepository;
    private final StudentTesdaQualificationRepository tesdaQualRepository;
    private final StudentSchoolYearRepository schoolYearRepository;
    private final ParentRepository parentRepository;
    private final OtherGuardianRepository guardianRepository;
    private final StudentEducationRepository educationRepository;
    private final StudentUploadRepository uploadRepository;
    private final StorageService storageService;
    private final GradeRepository gradeRepository;

    public RegistrarService(StudentRecordRepository studentRecordRepository,
                            BatchRepository batchRepository,
                            CourseRepository courseRepository,
                            SectionRepository sectionRepository,
                            StudentOjtRepository studentOjtRepository,
                            StudentTesdaQualificationRepository tesdaQualRepository,
                            StudentSchoolYearRepository schoolYearRepository,
                            ParentRepository parentRepository,
                            OtherGuardianRepository guardianRepository,
                            StudentEducationRepository educationRepository,
                            StudentUploadRepository uploadRepository,
                            StorageService storageService,
                            GradeRepository gradeRepository) {
        this.studentRecordRepository = studentRecordRepository;
        this.batchRepository = batchRepository;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.studentOjtRepository = studentOjtRepository;
        this.tesdaQualRepository = tesdaQualRepository;
        this.schoolYearRepository = schoolYearRepository;
        this.parentRepository = parentRepository;
        this.guardianRepository = guardianRepository;
        this.educationRepository = educationRepository;
        this.uploadRepository = uploadRepository;
        this.storageService = storageService;
        this.gradeRepository = gradeRepository;
    }

    public List<StudentRecordSummaryResponse> getAllRecords() {
        return getAllRecords(null, null, null, null);
    }

    public List<StudentRecordSummaryResponse> getAllRecords(String query) {
        return getAllRecords(query, null, null, null);
    }

    public List<StudentRecordSummaryResponse> getAllRecords(String query, Integer fromYear, Integer toYear) {
        return getAllRecords(query, fromYear, toYear, null, null);
    }

    public List<StudentRecordSummaryResponse> getAllRecords(String query, Integer fromYear, Integer toYear, String status) {
        return getAllRecords(query, fromYear, toYear, status, null);
    }

    /**
     * Returns all student records filtered by free-text query, batch year range, student
     * status, and/or whether a student number has been assigned. All filters are optional
     * and combined with AND logic.
     *
     * @param hasStudentNumber {@code null} for all records, {@code TRUE} for students who
     *                         have a student number, {@code FALSE} for those still missing
     *                         one (the registrar's "who still needs a number?" view)
     */
    public List<StudentRecordSummaryResponse> getAllRecords(String query, Integer fromYear, Integer toYear,
                                                            String status, Boolean hasStudentNumber) {
        List<StudentRecord> records = studentRecordRepository.findAll();
        String q = (query == null || query.isBlank()) ? null : query.toLowerCase().trim();
        boolean yearFilterActive = fromYear != null || toYear != null;
        String normalizedStatus = (status == null || status.isBlank()) ? null : status.trim();

        return records.stream()
                .filter(r -> q == null || matchesQuery(r, q))
                .filter(r -> !yearFilterActive || matchesYearRange(r, fromYear, toYear))
                .filter(r -> normalizedStatus == null || matchesStatus(r, normalizedStatus))
                .filter(r -> hasStudentNumber == null || hasStudentNumber == hasAssignedStudentNumber(r))
                .map(StudentRecordSummaryResponse::from)
                .toList();
    }

    private boolean hasAssignedStudentNumber(StudentRecord r) {
        return r.getStudentNumber() != null && !r.getStudentNumber().isBlank();
    }

    private boolean matchesQuery(StudentRecord r, String q) {
        return contains(String.valueOf(r.getRecordId()), q)
                || contains(r.getStudentId(), q)
                || contains(r.getStudentNumber(), q)
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

    /**
     * Assigns, changes, or clears a student's registrar-controlled student number.
     *
     * <p>This is the ONLY write path for {@code student_number}. The edit form deliberately
     * shows it read-only so the change is always a deliberate, separately audited action.
     * A blank or null value clears the number — students are allowed to have none.
     *
     * <p>Uniqueness is pre-checked here rather than left to the unique index, so a clash
     * reaches the registrar as an actionable 400 ("already assigned to ...") instead of a
     * generic 409 from {@code GlobalExceptionHandler}.
     */
    @Transactional
    public StudentRecordDetailsResponse assignStudentNumber(Integer recordId, String studentNumber) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

        String normalized = emptyToNull(studentNumber);

        if (normalized != null) {
            studentRecordRepository.findByStudentNumber(normalized)
                    .filter(other -> !other.getRecordId().equals(recordId))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException(
                                "Student number " + normalized + " is already assigned to "
                                        + other.getLastName() + ", " + other.getFirstName() + ".");
                    });
        }

        record.setStudentNumber(normalized);
        return buildDetailsResponse(studentRecordRepository.save(record));
    }

    @Transactional
    public StudentRecordDetailsResponse updateRecord(Integer recordId, StudentRecordUpdateRequest request) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

        String studentId = record.getStudentId();

        if (!studentId.equals(request.studentId())) {
            throw new IllegalArgumentException("Student ID cannot be changed.");
        }

        record.setLastName(request.lastName());
        record.setFirstName(request.firstName());
        record.setMiddleName(emptyToNull(request.middleName()));
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

        saveOjt(studentId, studentId, request.ojt());
        saveTesda(studentId, studentId, request.tesdaQualifications());
        saveSchoolYears(studentId, studentId, request.schoolYears());
        saveParents(saved, request.father(), request.mother());
        saveGuardian(saved, request.guardian());

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

        ParentDto father = parentRepository.findByStudentStudentIdAndRelation(studentId, "FATHER")
                .map(this::toParentDto).orElse(null);
        ParentDto mother = parentRepository.findByStudentStudentIdAndRelation(studentId, "MOTHER")
                .map(this::toParentDto).orElse(null);
        GuardianDto guardian = guardianRepository.findByStudentStudentId(studentId).stream()
                .findFirst().map(this::toGuardianDto).orElse(null);

        java.math.BigDecimal totalGwa = GradeEquivalent.gwa(gradeRepository.findByStudentStudentId(studentId));

        return StudentRecordDetailsResponse.from(record, ojt, tesda, schoolYears, father, mother, guardian, totalGwa);
    }

    // ----- Parents / Guardian -----

    private void saveParents(StudentRecord record, ParentDto fatherDto, ParentDto motherDto) {
        if (fatherDto != null) upsertParent(record, "FATHER", fatherDto);
        if (motherDto != null) upsertParent(record, "MOTHER", motherDto);
    }

    private void upsertParent(StudentRecord record, String relation, ParentDto dto) {
        Parent parent = parentRepository.findByStudentStudentIdAndRelation(record.getStudentId(), relation)
                .orElseGet(Parent::new);
        parent.setStudent(record);
        parent.setRelation(relation);
        parent.setFamilyName(emptyToNull(dto.familyName()));
        parent.setFirstName(emptyToNull(dto.firstName()));
        parent.setMiddleName(emptyToNull(dto.middleName()));
        parent.setBirthdate(dto.birthdate());
        parent.setOccupation(emptyToNull(dto.occupation()));
        parent.setEstIncome(dto.estIncome());
        parent.setContactNo(emptyToNull(dto.contactNo()));
        parent.setEmail(emptyToNull(dto.email()));
        parent.setAddress(emptyToNull(dto.address()));
        parentRepository.save(parent);
    }

    private void saveGuardian(StudentRecord record, GuardianDto dto) {
        if (dto == null) return;
        List<OtherGuardian> existing = guardianRepository.findByStudentStudentId(record.getStudentId());
        OtherGuardian guardian = existing.isEmpty() ? new OtherGuardian() : existing.get(0);
        guardian.setStudent(record);
        guardian.setRelation(dto.relation());
        guardian.setLastName(emptyToNull(dto.lastName()));
        guardian.setFirstName(emptyToNull(dto.firstName()));
        guardian.setMiddleName(emptyToNull(dto.middleName()));
        guardian.setBirthdate(dto.birthdate());
        guardian.setAddress(emptyToNull(dto.address()));
        guardianRepository.save(guardian);
    }

    private ParentDto toParentDto(Parent p) {
        return new ParentDto(p.getFamilyName(), p.getFirstName(), p.getMiddleName(),
                p.getBirthdate(), p.getOccupation(), p.getEstIncome(),
                p.getContactNo(), p.getEmail(), p.getAddress());
    }

    private GuardianDto toGuardianDto(OtherGuardian g) {
        return new GuardianDto(g.getRelation(), g.getLastName(), g.getFirstName(),
                g.getMiddleName(), g.getBirthdate(), g.getAddress());
    }

    // ----- Delete -----

    @Transactional
    public void deleteRecord(Integer recordId) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));

        String studentId = record.getStudentId();

        // Delete physical files before removing DB rows
        List<StudentUpload> uploads = uploadRepository.findByStudentId(studentId);
        uploads.forEach(storageService::delete);

        // Delete child rows in FK dependency order
        uploadRepository.deleteByStudentId(studentId);
        parentRepository.deleteByStudentStudentId(studentId);
        guardianRepository.deleteByStudentStudentId(studentId);
        educationRepository.deleteByStudentId(studentId);
        schoolYearRepository.deleteByStudentId(studentId);
        tesdaQualRepository.deleteByStudentId(studentId);
        studentOjtRepository.deleteByStudentId(studentId);
        studentRecordRepository.deleteDocumentsByStudentId(studentId);
        studentRecordRepository.deleteGradesByStudentId(studentId);
        studentRecordRepository.deleteClassEnrollmentsByStudentId(studentId);

        studentRecordRepository.deleteById(recordId);
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
