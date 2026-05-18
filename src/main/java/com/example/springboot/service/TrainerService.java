package com.example.springboot.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.springboot.dto.trainer.TrainerClassResponse;
import com.example.springboot.dto.trainer.TrainerClassStudentResponse;
import com.example.springboot.dto.trainer.TrainerSubjectResponse;
import com.example.springboot.dto.trainer.TrainerSubjectStudentResponse;
import com.example.springboot.model.SchoolClass;
import com.example.springboot.model.User;
import com.example.springboot.repository.ClassEnrollmentRepository;
import com.example.springboot.repository.SchoolClassRepository;
import com.example.springboot.repository.UserRepository;

@Service
public class TrainerService {

    private final SchoolClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public TrainerService(SchoolClassRepository classRepository,
                          ClassEnrollmentRepository enrollmentRepository,
                          UserRepository userRepository) {
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    Integer resolveCurrentTrainerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user has no matching User row: " + username));
        return user.getUserId();
    }

    public List<TrainerSubjectResponse> getMyAssignedSubjects() {
        Integer trainerId = resolveCurrentTrainerId();
        List<SchoolClass> myClasses = classRepository.findByTrainerUserId(trainerId);

        Map<String, List<SchoolClass>> bySubject = myClasses.stream()
                .collect(Collectors.groupingBy(c -> c.getSubject().getSubjectCode(),
                        LinkedHashMap::new, Collectors.toList()));

        return bySubject.entrySet().stream()
                .map(entry -> buildSubjectRow(entry.getValue()))
                .sorted(Comparator.comparing(TrainerSubjectResponse::subjectCode))
                .toList();
    }

    private TrainerSubjectResponse buildSubjectRow(List<SchoolClass> classesForSubject) {
        SchoolClass first = classesForSubject.get(0);
        var subject = first.getSubject();

        long enrolled = classesForSubject.stream()
                .mapToLong(c -> enrollmentRepository.countBySchoolClassClassId(c.getClassId()))
                .sum();

        List<String> sectionNames = classesForSubject.stream()
                .map(c -> c.getSection().getSection())
                .distinct()
                .sorted()
                .toList();

        List<String> courseNames = classesForSubject.stream()
                .map(c -> c.getSection().getCourse() != null
                        ? c.getSection().getCourse().getCourseName() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        String qualificationName = subject.getQualification() != null
                ? subject.getQualification().getQualificationName() : null;

        return new TrainerSubjectResponse(
                subject.getSubjectCode(),
                subject.getSubjectName(),
                qualificationName,
                subject.getUnits(),
                enrolled,
                sectionNames,
                courseNames
        );
    }

    public List<TrainerSubjectStudentResponse> getStudentsForSubject(String subjectCode) {
        Integer trainerId = resolveCurrentTrainerId();
        List<SchoolClass> myClasses =
                classRepository.findByTrainerUserIdAndSubjectSubjectCode(trainerId, subjectCode);
        if (myClasses.isEmpty()) {
            throw new IllegalArgumentException(
                    "You are not assigned to any class for subject: " + subjectCode);
        }

        return myClasses.stream()
                .flatMap(c -> enrollmentRepository.findBySchoolClassClassId(c.getClassId()).stream()
                        .map(e -> {
                            var s = e.getStudent();
                            return new TrainerSubjectStudentResponse(
                                    s.getStudentId(),
                                    s.getLastName(),
                                    s.getFirstName(),
                                    s.getMiddleName(),
                                    c.getSection().getSectionCode(),
                                    c.getSection().getSection());
                        }))
                .sorted(Comparator
                        .comparing(TrainerSubjectStudentResponse::sectionName,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TrainerSubjectStudentResponse::lastName,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TrainerSubjectStudentResponse::firstName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<TrainerClassResponse> getMyClasses() {
        Integer trainerId = resolveCurrentTrainerId();
        List<SchoolClass> myClasses = classRepository.findByTrainerUserId(trainerId);

        return myClasses.stream()
                .map(this::buildClassRow)
                .sorted(Comparator
                        .comparing(TrainerClassResponse::semester,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TrainerClassResponse::sectionName,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TrainerClassResponse::subjectCode,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private TrainerClassResponse buildClassRow(SchoolClass c) {
        long enrolled = enrollmentRepository.countBySchoolClassClassId(c.getClassId());
        String courseName = (c.getSection() != null && c.getSection().getCourse() != null)
                ? c.getSection().getCourse().getCourseName() : null;
        return new TrainerClassResponse(
                c.getClassId(),
                c.getSection().getSectionCode(),
                c.getSection().getSection(),
                c.getSubject().getSubjectCode(),
                c.getSubject().getSubjectName(),
                courseName,
                c.getSemester(),
                enrolled
        );
    }

    public List<TrainerClassStudentResponse> getStudentsForClass(Integer classId) {
        Integer trainerId = resolveCurrentTrainerId();
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        if (schoolClass.getTrainer() == null
                || !trainerId.equals(schoolClass.getTrainer().getUserId())) {
            throw new IllegalArgumentException(
                    "You are not assigned to class #" + classId);
        }

        return enrollmentRepository.findBySchoolClassClassId(classId).stream()
                .map(e -> {
                    var s = e.getStudent();
                    return new TrainerClassStudentResponse(
                            s.getStudentId(),
                            s.getLastName(),
                            s.getFirstName(),
                            s.getMiddleName());
                })
                .sorted(Comparator
                        .comparing(TrainerClassStudentResponse::lastName,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TrainerClassStudentResponse::firstName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
