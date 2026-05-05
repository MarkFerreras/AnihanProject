package com.example.springboot.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.student.EducationItemDto;
import com.example.springboot.dto.student.GuardianDto;
import com.example.springboot.dto.student.ParentDto;
import com.example.springboot.dto.student.SchoolYearDto;
import com.example.springboot.dto.student.StudentDetailsRequest;
import com.example.springboot.dto.student.StudentDetailsResponse;
import com.example.springboot.dto.student.UploadRefDto;
import com.example.springboot.model.OtherGuardian;
import com.example.springboot.model.Parent;
import com.example.springboot.model.StudentEducation;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.StudentSchoolYear;
import com.example.springboot.model.StudentUpload;
import com.example.springboot.repository.OtherGuardianRepository;
import com.example.springboot.repository.ParentRepository;
import com.example.springboot.repository.StudentEducationRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.StudentSchoolYearRepository;
import com.example.springboot.repository.StudentUploadRepository;

@Service
public class StudentDetailsService {

    private final StudentRecordRepository studentRecordRepo;
    private final ParentRepository parentRepo;
    private final OtherGuardianRepository guardianRepo;
    private final StudentEducationRepository educationRepo;
    private final StudentSchoolYearRepository schoolYearRepo;
    private final StudentUploadRepository uploadRepo;

    public StudentDetailsService(
            StudentRecordRepository studentRecordRepo,
            ParentRepository parentRepo,
            OtherGuardianRepository guardianRepo,
            StudentEducationRepository educationRepo,
            StudentSchoolYearRepository schoolYearRepo,
            StudentUploadRepository uploadRepo) {
        this.studentRecordRepo = studentRecordRepo;
        this.parentRepo = parentRepo;
        this.guardianRepo = guardianRepo;
        this.educationRepo = educationRepo;
        this.schoolYearRepo = schoolYearRepo;
        this.uploadRepo = uploadRepo;
    }

    /**
     * Creates a minimal "Enrolling" student record so that file uploads
     * can reference student_id (FK constraint in student_uploads table).
     * Only name + status are persisted at this point.
     *
     * If a record with the same name already exists and is still in an
     * editable state (Enrolling/Draft), it is treated as a resume.
     */
    @Transactional
    public StudentDetailsResponse startOrResume(String lastName, String firstName, String middleName) {
        var existing = studentRecordRepo
                .findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
                        lastName.trim(), firstName.trim(), middleName.trim());

        if (existing.isPresent()) {
            return buildResponse(existing.get());
        }

        StudentRecord record = new StudentRecord();
        record.setStudentId(generateStudentId());
        record.setLastName(lastName.trim());
        record.setFirstName(firstName.trim());
        record.setMiddleName(middleName.trim());
        record.setStudentStatus("Enrolling");
        studentRecordRepo.save(record);
        return buildResponse(record);
    }

    @Transactional(readOnly = true)
    public StudentDetailsResponse load(String studentId) {
        StudentRecord record = findOrThrow(studentId);
        return buildResponse(record);
    }

    /**
     * Final submit: applies ALL form data and sets status to "Submitted"
     * in a single transaction. This is the only point at which substantive
     * student data (personal details, family, education) is persisted.
     *
     * Replaces the old saveDraft + submit two-step flow.
     */
    @Transactional
    public StudentDetailsResponse submitEnrollment(String studentId, StudentDetailsRequest req) {
        StudentRecord record = findOrThrow(studentId);

        if ("Submitted".equals(record.getStudentStatus())) {
            throw new IllegalStateException("This enrollment has already been submitted.");
        }

        // Apply all data in one transaction
        applyPersonal(record, req);
        applyReligion(record, req);
        record.setStudentStatus("Submitted");
        studentRecordRepo.save(record);

        applyFamily(record, req);
        applyEducation(studentId, req);

        return buildResponse(studentRecordRepo.findByStudentId(studentId).orElseThrow());
    }

    @Transactional
    public UploadRefDto saveUpload(String studentId, StudentUpload upload) {
        findOrThrow(studentId);
        // Replace existing upload of same kind
        uploadRepo.findByStudentIdAndKind(studentId, upload.getKind())
                .ifPresent(uploadRepo::delete);
        uploadRepo.save(upload);
        return toUploadRef(upload);
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private StudentRecord findOrThrow(String studentId) {
        return studentRecordRepo.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student record not found: " + studentId));
    }

    private String generateStudentId() {
        int year = LocalDate.now().getYear();
        String prefix = "SR" + year;
        long count = studentRecordRepo.countByStudentIdStartingWith(prefix);
        return String.format("%s%04d", prefix, count + 1);
    }

    private void applyPersonal(StudentRecord r, StudentDetailsRequest req) {
        if (req.contactNo() != null) r.setContactNo(req.contactNo().trim());
        if (req.birthdate() != null) {
            r.setBirthdate(req.birthdate());
            r.setAge(AgeCalculator.calculateAge(req.birthdate()));
        }
        if (req.sex() != null) r.setSex(req.sex());
        if (req.civilStatus() != null) r.setCivilStatus(req.civilStatus());
        if (req.permanentAddress() != null) r.setPermanentAddress(req.permanentAddress().trim());
        r.setTemporaryAddress(req.temporaryAddress() != null ? req.temporaryAddress().trim() : null);
        if (req.siblingCount() != null) r.setSiblingCount(req.siblingCount());
        r.setBrotherCount(req.brotherCount());
        r.setSisterCount(req.sisterCount());
    }

    private void applyReligion(StudentRecord r, StudentDetailsRequest req) {
        if (req.religion() != null) r.setReligion(req.religion().trim());
        if (req.baptized() != null) {
            r.setBaptized(req.baptized());
            if (Boolean.TRUE.equals(req.baptized())) {
                r.setBaptismDate(req.baptismDate());
                if (req.baptismPlace() != null) r.setBaptismPlace(req.baptismPlace().trim());
            } else {
                r.setBaptismDate(null);
                r.setBaptismPlace(null);
            }
        }
    }

    private void applyFamily(StudentRecord record, StudentDetailsRequest req) {
        if (req.father() != null) upsertParent(record, "FATHER", req.father());
        if (req.mother() != null) upsertParent(record, "MOTHER", req.mother());
        if (req.guardian() != null) upsertGuardian(record, req.guardian());
    }

    private void upsertParent(StudentRecord record, String relation, ParentDto dto) {
        Parent parent = parentRepo
                .findByStudentStudentIdAndRelation(record.getStudentId(), relation)
                .orElseGet(Parent::new);
        parent.setStudent(record);
        parent.setRelation(relation);
        parent.setFamilyName(dto.familyName());
        parent.setFirstName(dto.firstName());
        parent.setMiddleName(dto.middleName());
        parent.setBirthdate(dto.birthdate());
        parent.setOccupation(dto.occupation());
        parent.setEstIncome(dto.estIncome());
        parent.setContactNo(dto.contactNo());
        parent.setEmail(dto.email());
        parent.setAddress(dto.address());
        parentRepo.save(parent);
    }

    private void upsertGuardian(StudentRecord record, GuardianDto dto) {
        List<OtherGuardian> existing = guardianRepo.findByStudentStudentId(record.getStudentId());
        OtherGuardian guardian = existing.isEmpty() ? new OtherGuardian() : existing.get(0);
        guardian.setStudent(record);
        guardian.setRelation(dto.relation());
        guardian.setLastName(dto.lastName());
        guardian.setFirstName(dto.firstName());
        guardian.setMiddleName(dto.middleName());
        guardian.setBirthdate(dto.birthdate());
        guardian.setAddress(dto.address());
        guardianRepo.save(guardian);
    }

    private void applyEducation(String studentId, StudentDetailsRequest req) {
        if (req.educationHistory() != null) {
            for (EducationItemDto dto : req.educationHistory()) {
                if (dto.level() == null) continue;
                StudentEducation edu = educationRepo
                        .findByStudentIdAndLevel(studentId, dto.level())
                        .orElseGet(StudentEducation::new);
                edu.setStudentId(studentId);
                edu.setLevel(dto.level());
                edu.setSchoolName(dto.schoolName());
                edu.setSchoolAddress(dto.schoolAddress());
                edu.setGradeYear(dto.gradeYear());
                edu.setSemester(dto.semester());
                edu.setEndedYear(dto.endedYear());
                educationRepo.save(edu);
            }
        }

        if (req.schoolYears() != null) {
            for (SchoolYearDto dto : req.schoolYears()) {
                if (dto.rowIndex() == null) continue;
                var existing = schoolYearRepo.findByStudentIdOrderByRowIndex(studentId)
                        .stream().filter(s -> s.getRowIndex().equals(dto.rowIndex())).findFirst()
                        .orElseGet(StudentSchoolYear::new);
                existing.setStudentId(studentId);
                existing.setRowIndex(dto.rowIndex());
                existing.setSyStart(dto.syStart());
                existing.setSemStart(dto.semStart());
                existing.setSyEnd(dto.syEnd());
                existing.setSemEnd(dto.semEnd());
                existing.setRemarks(dto.remarks());
                schoolYearRepo.save(existing);
            }
        }
    }

    private StudentDetailsResponse buildResponse(StudentRecord r) {
        String sid = r.getStudentId();

        ParentDto father = parentRepo.findByStudentStudentIdAndRelation(sid, "FATHER")
                .map(this::toParentDto).orElse(null);
        ParentDto mother = parentRepo.findByStudentStudentIdAndRelation(sid, "MOTHER")
                .map(this::toParentDto).orElse(null);
        GuardianDto guardian = guardianRepo.findByStudentStudentId(sid).stream()
                .findFirst().map(this::toGuardianDto).orElse(null);

        List<EducationItemDto> education = educationRepo.findByStudentIdOrderByLevel(sid)
                .stream().map(this::toEducationDto).toList();
        List<SchoolYearDto> schoolYears = schoolYearRepo.findByStudentIdOrderByRowIndex(sid)
                .stream().map(this::toSchoolYearDto).toList();

        UploadRefDto idPhoto = uploadRepo.findByStudentIdAndKind(sid, "ID_PHOTO")
                .map(this::toUploadRef).orElse(null);
        UploadRefDto baptCert = uploadRepo.findByStudentIdAndKind(sid, "BAPTISMAL_CERT")
                .map(this::toUploadRef).orElse(null);

        return new StudentDetailsResponse(
                sid, r.getLastName(), r.getFirstName(), r.getMiddleName(), r.getStudentStatus(),
                r.getContactNo(), r.getBirthdate(), r.getAge(), r.getSex(), r.getCivilStatus(),
                r.getPermanentAddress(), r.getTemporaryAddress(),
                r.getSiblingCount(), r.getBrotherCount(), r.getSisterCount(),
                r.getReligion(), r.getBaptized(), r.getBaptismDate(), r.getBaptismPlace(),
                idPhoto, baptCert,
                father, mother, guardian,
                education, schoolYears
        );
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

    private EducationItemDto toEducationDto(StudentEducation e) {
        return new EducationItemDto(e.getLevel(), e.getSchoolName(), e.getSchoolAddress(),
                e.getGradeYear(), e.getSemester(), e.getEndedYear());
    }

    private SchoolYearDto toSchoolYearDto(StudentSchoolYear s) {
        return new SchoolYearDto(s.getRowIndex(), s.getSyStart(), s.getSemStart(),
                s.getSyEnd(), s.getSemEnd(), s.getRemarks());
    }

    private UploadRefDto toUploadRef(StudentUpload u) {
        return new UploadRefDto(u.getUploadId(), u.getKind(), u.getOriginalName(),
                u.getMimeType(), u.getSizeBytes(), u.getUploadedAt());
    }
}
