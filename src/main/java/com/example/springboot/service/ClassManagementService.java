package com.example.springboot.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.dto.registrar.AssignTrainerRequest;
import com.example.springboot.dto.registrar.ClassResponse;
import com.example.springboot.dto.registrar.CreateClassRequest;
import com.example.springboot.dto.registrar.UpdateClassTrainerRequest;
import com.example.springboot.dto.registrar.CreateSectionRequest;
import com.example.springboot.dto.registrar.EnrollStudentRequest;
import com.example.springboot.dto.registrar.SectionResponse;
import com.example.springboot.dto.registrar.SubjectResponse;
import com.example.springboot.dto.registrar.TrainerResponse;
import com.example.springboot.model.Batch;
import com.example.springboot.model.ClassEnrollment;
import com.example.springboot.model.Course;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.Section;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.model.Subject;
import com.example.springboot.model.User;
import com.example.springboot.dto.registrar.CreateSubjectRequest;
import com.example.springboot.dto.registrar.QualificationResponse;
import com.example.springboot.dto.registrar.UpdateSubjectRequest;
import com.example.springboot.model.Qualification;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.ClassEnrollmentRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.QualificationRepository;
import com.example.springboot.repository.SchoolClassRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.SubjectRepository;
import com.example.springboot.repository.UserRepository;

/**
 * Service handling Subjects (trainer assignment), Classes (CRUD + enrollment),
 * and Sections (CRUD) for the registrar portal.
 */
@Service
public class ClassManagementService {

    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;
    private final SchoolClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final StudentRecordRepository studentRecordRepository;
    private final QualificationRepository qualificationRepository;

    public ClassManagementService(SubjectRepository subjectRepository,
                                  SectionRepository sectionRepository,
                                  SchoolClassRepository classRepository,
                                  ClassEnrollmentRepository enrollmentRepository,
                                  UserRepository userRepository,
                                  BatchRepository batchRepository,
                                  CourseRepository courseRepository,
                                  StudentRecordRepository studentRecordRepository,
                                  QualificationRepository qualificationRepository) {
        this.subjectRepository = subjectRepository;
        this.sectionRepository = sectionRepository;
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.courseRepository = courseRepository;
        this.studentRecordRepository = studentRecordRepository;
        this.qualificationRepository = qualificationRepository;
    }

    // -------------------------------------------------------
    // Subjects
    // -------------------------------------------------------

    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(SubjectResponse::from)
                .collect(Collectors.toList());
    }

    public List<QualificationResponse> getAllQualifications() {
        return qualificationRepository.findAll().stream()
                .map(QualificationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        String code = request.subjectCode().trim();
        if (subjectRepository.existsById(code)) {
            throw new IllegalArgumentException("Subject code already exists: " + code);
        }
        Qualification qualification = qualificationRepository.findById(request.qualificationCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Qualification not found: " + request.qualificationCode()));

        Subject subject = new Subject();
        subject.setSubjectCode(code);
        subject.setSubjectName(request.subjectName().trim());
        subject.setQualification(qualification);
        subject.setUnits(request.units());

        Subject saved = subjectRepository.save(subject);
        return SubjectResponse.from(saved);
    }

    @Transactional
    public SubjectResponse updateSubject(String subjectCode, UpdateSubjectRequest request) {
        Subject subject = subjectRepository.findById(subjectCode)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectCode));

        Qualification qualification = qualificationRepository.findById(request.qualificationCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Qualification not found: " + request.qualificationCode()));

        subject.setSubjectName(request.subjectName().trim());
        subject.setQualification(qualification);
        subject.setUnits(request.units());

        Subject saved = subjectRepository.save(subject);
        return SubjectResponse.from(saved);
    }

    @Transactional
    public void deleteSubject(String subjectCode) {
        if (!subjectRepository.existsById(subjectCode)) {
            throw new IllegalArgumentException("Subject not found: " + subjectCode);
        }
        if (classRepository.existsBySubjectSubjectCode(subjectCode)) {
            throw new IllegalArgumentException(
                    "Cannot delete subject: one or more classes still reference it. "
                            + "Remove those classes first.");
        }
        if (subjectRepository.countGradesBySubjectCode(subjectCode) > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete subject: grades have already been recorded under it.");
        }
        subjectRepository.deleteById(subjectCode);
    }

    @Transactional
    public SubjectResponse assignTrainer(String subjectCode, AssignTrainerRequest request) {
        Subject subject = subjectRepository.findById(subjectCode)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectCode));

        if (request.trainerId() == null) {
            subject.setTrainer(null);
        } else {
            User trainer = userRepository.findById(request.trainerId())
                    .orElseThrow(() -> new IllegalArgumentException("Trainer not found: " + request.trainerId()));
            if (!"ROLE_TRAINER".equals(trainer.getRole())) {
                throw new IllegalArgumentException("User is not a trainer: " + trainer.getUsername());
            }
            if (!Boolean.TRUE.equals(trainer.getEnabled())) {
                throw new IllegalArgumentException("Trainer account is disabled: " + trainer.getUsername());
            }
            subject.setTrainer(trainer);
        }

        subjectRepository.save(subject);
        return SubjectResponse.from(subject);
    }

    // -------------------------------------------------------
    // Trainers (lookup)
    // -------------------------------------------------------

    public List<TrainerResponse> getActiveTrainers() {
        return userRepository.findByRoleAndEnabledTrue("ROLE_TRAINER").stream()
                .map(TrainerResponse::from)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // Classes
    // -------------------------------------------------------

    public String getCurrentSemester() {
        return batchRepository.findTopByOrderByBatchYearDesc()
                .map(b -> String.valueOf(b.getBatchYear()))
                .orElse(String.valueOf(java.time.Year.now().getValue()));
    }

    public List<ClassResponse> getClasses(String semester) {
        List<SchoolClass> classes;
        if (semester != null && !semester.isBlank()) {
            classes = classRepository.findBySemester(semester);
        } else {
            classes = classRepository.findAll();
        }
        return classes.stream()
                .map(c -> ClassResponse.from(c, enrollmentRepository.countBySchoolClassClassId(c.getClassId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        // Validate section
        Section section = sectionRepository.findById(request.sectionCode())
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + request.sectionCode()));

        // Validate subject
        Subject subject = subjectRepository.findById(request.subjectCode())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + request.subjectCode()));

        // Check uniqueness
        if (classRepository.existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester(
                request.sectionCode(), request.subjectCode(), request.semester())) {
            throw new IllegalArgumentException(
                    "A class for this section, subject, and semester already exists.");
        }

        // Validate trainer if provided
        User trainer = null;
        if (request.trainerId() != null) {
            trainer = userRepository.findById(request.trainerId())
                    .orElseThrow(() -> new IllegalArgumentException("Trainer not found: " + request.trainerId()));
            if (!"ROLE_TRAINER".equals(trainer.getRole())) {
                throw new IllegalArgumentException("User is not a trainer.");
            }
        }

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSection(section);
        schoolClass.setSubject(subject);
        schoolClass.setTrainer(trainer);
        schoolClass.setSemester(request.semester());
        schoolClass.setCreatedAt(LocalDateTime.now());

        classRepository.save(schoolClass);
        return ClassResponse.from(schoolClass, 0);
    }

    @Transactional
    public ClassResponse updateClassTrainer(Integer classId, UpdateClassTrainerRequest request) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        if (request.trainerId() == null) {
            schoolClass.setTrainer(null);
        } else {
            User trainer = userRepository.findById(request.trainerId())
                    .orElseThrow(() -> new IllegalArgumentException("Trainer not found: " + request.trainerId()));
            if (!"ROLE_TRAINER".equals(trainer.getRole())) {
                throw new IllegalArgumentException("User is not a trainer: " + trainer.getUsername());
            }
            if (!Boolean.TRUE.equals(trainer.getEnabled())) {
                throw new IllegalArgumentException("Trainer account is disabled: " + trainer.getUsername());
            }
            schoolClass.setTrainer(trainer);
        }

        classRepository.save(schoolClass);
        return ClassResponse.from(schoolClass, enrollmentRepository.countBySchoolClassClassId(classId));
    }

    // -------------------------------------------------------
    // Class Enrollment
    // -------------------------------------------------------

    public List<ClassEnrollmentResponse> getClassEnrollments(Integer classId) {
        return enrollmentRepository.findBySchoolClassClassId(classId).stream()
                .map(e -> new ClassEnrollmentResponse(
                        e.getEnrollmentId(),
                        e.getStudent().getStudentId(),
                        e.getStudent().getLastName(),
                        e.getStudent().getFirstName(),
                        e.getEnrolledAt() != null ? e.getEnrolledAt().toString() : null
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void enrollStudent(EnrollStudentRequest request) {
        SchoolClass schoolClass = classRepository.findById(request.classId())
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + request.classId()));

        StudentRecord student = studentRecordRepository.findByStudentId(request.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + request.studentId()));

        if (enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(
                request.classId(), request.studentId())) {
            throw new IllegalArgumentException("Student is already enrolled in this class.");
        }

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setSchoolClass(schoolClass);
        enrollment.setStudent(student);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void unenrollStudent(Integer enrollmentId) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new IllegalArgumentException("Enrollment not found: " + enrollmentId);
        }
        enrollmentRepository.deleteById(enrollmentId);
    }

    /**
     * Get students eligible for enrollment in a class.
     * Returns active students from the same section as the class.
     */
    public List<StudentSummary> getEligibleStudents(Integer classId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        String sectionCode = schoolClass.getSection().getSectionCode();

        // Get all students in this section that are Active or Submitted
        return studentRecordRepository.findAll().stream()
                .filter(sr -> sr.getSection() != null && sectionCode.equals(sr.getSection().getSectionCode()))
                .filter(sr -> {
                    String status = sr.getStudentStatus();
                    return "Active".equalsIgnoreCase(status) || "Submitted".equalsIgnoreCase(status);
                })
                .filter(sr -> !enrollmentRepository.existsBySchoolClassClassIdAndStudentStudentId(
                        classId, sr.getStudentId()))
                .map(sr -> new StudentSummary(sr.getStudentId(), sr.getLastName(), sr.getFirstName()))
                .sorted(Comparator.comparing(StudentSummary::lastName))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // Sections
    // -------------------------------------------------------

    public List<SectionResponse> getSections(String semester) {
        if (semester != null && !semester.isBlank()) {
            try {
                Short year = Short.parseShort(semester);
                return sectionRepository.findByBatchBatchYear(year).stream()
                        .map(SectionResponse::from)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                // Fall through to all sections
            }
        }
        return sectionRepository.findAll().stream()
                .map(SectionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public SectionResponse createSection(CreateSectionRequest request) {
        // Check uniqueness
        if (sectionRepository.existsById(request.sectionCode())) {
            throw new IllegalArgumentException("Section code already exists: " + request.sectionCode());
        }

        Batch batch = batchRepository.findById(request.batchCode())
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + request.batchCode()));

        Course course = courseRepository.findById(request.courseCode())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + request.courseCode()));

        Section section = new Section();
        section.setSectionCode(request.sectionCode());
        section.setSection(request.sectionName());
        section.setBatch(batch);
        section.setCourse(course);

        sectionRepository.save(section);
        return SectionResponse.from(section);
    }

    @Transactional
    public void deleteSection(String sectionCode) {
        if (!sectionRepository.existsById(sectionCode)) {
            throw new IllegalArgumentException("Section not found: " + sectionCode);
        }
        if (classRepository.existsBySectionSectionCode(sectionCode)) {
            throw new IllegalArgumentException(
                    "Cannot delete section: one or more classes still reference it. Remove those classes first.");
        }
        sectionRepository.deleteById(sectionCode);
    }

    // -------------------------------------------------------
    // Inner record types
    // -------------------------------------------------------

    public record ClassEnrollmentResponse(
            Integer enrollmentId,
            String studentId,
            String lastName,
            String firstName,
            String enrolledAt
    ) {}

    public record StudentSummary(
            String studentId,
            String lastName,
            String firstName
    ) {}
}
