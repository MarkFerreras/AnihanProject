package com.example.springboot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.registrar.AssignTrainerRequest;
import com.example.springboot.dto.registrar.ClassResponse;
import com.example.springboot.dto.registrar.CreateClassRequest;
import com.example.springboot.dto.registrar.CreateSectionRequest;
import com.example.springboot.dto.registrar.CreateSubjectRequest;
import com.example.springboot.dto.registrar.EnrollStudentRequest;
import com.example.springboot.dto.registrar.QualificationResponse;
import com.example.springboot.dto.registrar.SectionResponse;
import com.example.springboot.dto.registrar.SubjectResponse;
import com.example.springboot.dto.registrar.TrainerResponse;
import com.example.springboot.dto.registrar.AssignStudentsToSectionRequest;
import com.example.springboot.dto.registrar.BulkEnrollSectionResponse;
import com.example.springboot.dto.registrar.EligibleSectionStudentResponse;
import com.example.springboot.dto.registrar.SectionAssignmentResultResponse;
import com.example.springboot.dto.registrar.SectionStudentResponse;
import com.example.springboot.dto.registrar.UpdateClassTrainerRequest;
import com.example.springboot.dto.registrar.UpdateSectionRequest;
import com.example.springboot.dto.registrar.UpdateSubjectRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.ClassManagementService;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Registrar endpoints for managing Subjects, Classes, Sections, and Class Enrollment.
 * All endpoints require ROLE_REGISTRAR.
 */
@RestController
@RequestMapping("/api/registrar")
public class ClassManagementController {

    private final ClassManagementService classManagementService;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    public ClassManagementController(ClassManagementService classManagementService,
                                     SystemLogService systemLogService,
                                     UserRepository userRepository) {
        this.classManagementService = classManagementService;
        this.systemLogService = systemLogService;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------
    // Subjects
    // -------------------------------------------------------

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectResponse>> listSubjects() {
        return ResponseEntity.ok(classManagementService.getAllSubjects());
    }

    @GetMapping("/qualifications")
    public ResponseEntity<List<QualificationResponse>> listQualifications() {
        return ResponseEntity.ok(classManagementService.getAllQualifications());
    }

    @PostMapping("/subjects")
    public ResponseEntity<SubjectResponse> createSubject(
            @Valid @RequestBody CreateSubjectRequest request,
            HttpServletRequest httpRequest) {
        SubjectResponse result = classManagementService.createSubject(request);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Created subject " + result.subjectCode() + " (" + result.subjectName() + ")",
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/subjects/{code}")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable String code,
            @Valid @RequestBody UpdateSubjectRequest request,
            HttpServletRequest httpRequest) {
        SubjectResponse result = classManagementService.updateSubject(code, request);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Updated subject " + code,
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/subjects/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(
            @PathVariable String code,
            HttpServletRequest httpRequest) {
        classManagementService.deleteSubject(code);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Deleted subject " + code,
                httpRequest.getRemoteAddr());
    }

    @PutMapping("/subjects/{code}/trainer")
    public ResponseEntity<SubjectResponse> assignTrainer(
            @PathVariable String code,
            @RequestBody AssignTrainerRequest request,
            HttpServletRequest httpRequest) {
        SubjectResponse result = classManagementService.assignTrainer(code, request);

        LogContext ctx = getLogContext();
        String action = request.trainerId() != null
                ? "Assigned trainer " + result.trainerName() + " to subject " + code
                : "Unassigned trainer from subject " + code;
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                action, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // Trainers (lookup)
    // -------------------------------------------------------

    @GetMapping("/trainers")
    public ResponseEntity<List<TrainerResponse>> listTrainers() {
        return ResponseEntity.ok(classManagementService.getActiveTrainers());
    }

    // -------------------------------------------------------
    // Classes
    // -------------------------------------------------------

    @GetMapping("/classes/current-semester")
    public ResponseEntity<Map<String, String>> getCurrentSemester() {
        String semester = classManagementService.getCurrentSemester();
        return ResponseEntity.ok(Map.of("semester", semester));
    }

    @GetMapping("/classes")
    public ResponseEntity<List<ClassResponse>> listClasses(
            @RequestParam(value = "semester", required = false) String semester) {
        return ResponseEntity.ok(classManagementService.getClasses(semester));
    }

    @PostMapping("/classes")
    public ResponseEntity<ClassResponse> createClass(
            @Valid @RequestBody CreateClassRequest request,
            HttpServletRequest httpRequest) {
        ClassResponse result = classManagementService.createClass(request);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Created class: " + result.subjectName() + " in " + result.sectionName()
                        + " (Semester " + result.semester() + ")",
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/classes/{classId}/trainer")
    public ResponseEntity<ClassResponse> updateClassTrainer(
            @PathVariable Integer classId,
            @RequestBody UpdateClassTrainerRequest request,
            HttpServletRequest httpRequest) {
        ClassResponse result = classManagementService.updateClassTrainer(classId, request);

        LogContext ctx = getLogContext();
        String action = request.trainerId() != null
                ? "Assigned trainer " + result.trainerName() + " to class #" + classId
                        + " (" + result.subjectCode() + " / " + result.sectionCode() + ")"
                : "Unassigned trainer from class #" + classId
                        + " (" + result.subjectCode() + " / " + result.sectionCode() + ")";
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                action, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // Class Enrollment
    // -------------------------------------------------------

    @GetMapping("/classes/{classId}/enrollments")
    public ResponseEntity<List<ClassManagementService.ClassEnrollmentResponse>> listEnrollments(
            @PathVariable Integer classId) {
        return ResponseEntity.ok(classManagementService.getClassEnrollments(classId));
    }

    @GetMapping("/classes/{classId}/eligible-students")
    public ResponseEntity<List<ClassManagementService.StudentSummary>> listEligibleStudents(
            @PathVariable Integer classId) {
        return ResponseEntity.ok(classManagementService.getEligibleStudents(classId));
    }

    @PostMapping("/classes/enroll")
    public ResponseEntity<Map<String, String>> enrollStudent(
            @Valid @RequestBody EnrollStudentRequest request,
            HttpServletRequest httpRequest) {
        classManagementService.enrollStudent(request);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Enrolled student " + request.studentId() + " in class #" + request.classId(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.ok(Map.of("message", "Student enrolled successfully."));
    }

    @PostMapping("/classes/{classId}/enroll-section")
    public ResponseEntity<BulkEnrollSectionResponse> bulkEnrollSectionIntoClass(
            @PathVariable Integer classId,
            HttpServletRequest httpRequest) {
        BulkEnrollSectionResponse result = classManagementService.bulkEnrollSectionIntoClass(classId);
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Bulk-enrolled section into class #" + classId + ": "
                        + result.enrolledCount() + " enrolled, "
                        + result.skippedAlreadyEnrolled() + " already enrolled, "
                        + result.skippedIneligible() + " ineligible",
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenrollStudent(@PathVariable Integer enrollmentId, HttpServletRequest httpRequest) {
        classManagementService.unenrollStudent(enrollmentId);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Removed enrollment #" + enrollmentId,
                httpRequest.getRemoteAddr());
    }

    // -------------------------------------------------------
    // Sections
    // -------------------------------------------------------

    @GetMapping("/sections")
    public ResponseEntity<List<SectionResponse>> listSections(
            @RequestParam(value = "semester", required = false) String semester) {
        return ResponseEntity.ok(classManagementService.getSections(semester));
    }

    @PostMapping("/sections")
    public ResponseEntity<SectionResponse> createSection(
            @Valid @RequestBody CreateSectionRequest request,
            HttpServletRequest httpRequest) {
        SectionResponse result = classManagementService.createSection(request);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Created section: " + result.sectionName() + " (" + result.sectionCode() + ")",
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/sections/{sectionCode}")
    public ResponseEntity<SectionResponse> updateSection(
            @PathVariable String sectionCode,
            @Valid @RequestBody UpdateSectionRequest request,
            HttpServletRequest httpRequest) {
        SectionResponse response = classManagementService.updateSection(sectionCode, request);
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Updated section " + sectionCode + " name to '" + response.sectionName() + "'",
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sections/eligible-students")
    public ResponseEntity<List<EligibleSectionStudentResponse>> getEligibleStudentsForSection(
            @RequestParam(value = "batchCode", required = false) String batchCode,
            @RequestParam(value = "courseCode", required = false) String courseCode) {
        return ResponseEntity.ok(classManagementService.getEligibleStudentsForSection(batchCode, courseCode));
    }

    @GetMapping("/sections/{sectionCode}/students")
    public ResponseEntity<List<SectionStudentResponse>> getStudentsInSection(
            @PathVariable String sectionCode) {
        return ResponseEntity.ok(classManagementService.getStudentsInSection(sectionCode));
    }

    @PostMapping("/sections/{sectionCode}/students")
    public ResponseEntity<SectionAssignmentResultResponse> assignStudentsToSection(
            @PathVariable String sectionCode,
            @Valid @RequestBody AssignStudentsToSectionRequest request,
            HttpServletRequest httpRequest) {
        SectionAssignmentResultResponse result =
                classManagementService.assignStudentsToSection(sectionCode, request);
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Assigned " + result.assignedCount()
                        + " students to section " + sectionCode
                        + " (skipped " + result.skippedStudentIds().size() + ")",
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/sections/{sectionCode}/students/{studentId}")
    public ResponseEntity<Void> removeStudentFromSection(
            @PathVariable String sectionCode,
            @PathVariable String studentId,
            HttpServletRequest httpRequest) {
        int cascaded = classManagementService.removeStudentFromSection(sectionCode, studentId);
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Removed student " + studentId + " from section " + sectionCode
                        + " (cascaded " + cascaded + " class enrollment(s))",
                httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sections/{sectionCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable String sectionCode, HttpServletRequest httpRequest) {
        classManagementService.deleteSection(sectionCode);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Deleted section: " + sectionCode,
                httpRequest.getRemoteAddr());
    }

    // -------------------------------------------------------
    // Logging helper
    // -------------------------------------------------------

    private LogContext getLogContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_UNKNOWN");
        Integer userId = userRepository.findByUsername(username)
                .map(User::getUserId)
                .orElse(null);
        return new LogContext(userId, username, role);
    }

    private record LogContext(Integer userId, String username, String role) {}
}
