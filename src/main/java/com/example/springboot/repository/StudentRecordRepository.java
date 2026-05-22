package com.example.springboot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot.model.StudentRecord;

public interface StudentRecordRepository extends JpaRepository<StudentRecord, Integer> {

    boolean existsByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            String lastName, String firstName, String middleName);

    Optional<StudentRecord> findByStudentId(String studentId);

    java.util.List<StudentRecord> findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            String lastName, String firstName, String middleName);

    @Modifying
    @Query(value = "DELETE FROM documents WHERE student_id = :studentId", nativeQuery = true)
    void deleteDocumentsByStudentId(@Param("studentId") String studentId);

    @Modifying
    @Query(value = "DELETE FROM grades WHERE student_id = :studentId", nativeQuery = true)
    void deleteGradesByStudentId(@Param("studentId") String studentId);

    @Modifying
    @Query(value = "DELETE FROM class_enrollments WHERE student_id = :studentId", nativeQuery = true)
    void deleteClassEnrollmentsByStudentId(@Param("studentId") String studentId);

    @Query("SELECT MAX(s.studentId) FROM StudentRecord s WHERE s.studentId LIKE :prefix%")
    java.util.Optional<String> findMaxStudentIdWithPrefix(@Param("prefix") String prefix);

    java.util.List<StudentRecord> findBySectionSectionCode(String sectionCode);

    java.util.List<StudentRecord> findBySectionIsNullAndStudentStatusIgnoreCase(String status);

    java.util.List<StudentRecord> findBySectionIsNullAndStudentStatusIgnoreCaseAndBatchBatchCode(String status, String batchCode);

    java.util.List<StudentRecord> findBySectionIsNullAndStudentStatusIgnoreCaseAndCourseCourseCode(String status, String courseCode);

    java.util.List<StudentRecord> findBySectionIsNullAndStudentStatusIgnoreCaseAndBatchBatchCodeAndCourseCourseCode(
            String status, String batchCode, String courseCode);

    // ---- Sectionless-student finders accepting multiple eligible statuses ----
    // Derived-name queries can't express a case-insensitive IN, so these use JPQL
    // with LOWER(...) against a pre-lowercased status collection.

    @Query("SELECT s FROM StudentRecord s WHERE s.section IS NULL "
            + "AND LOWER(s.studentStatus) IN :statuses")
    java.util.List<StudentRecord> findSectionlessByStatuses(
            @Param("statuses") java.util.Collection<String> statuses);

    @Query("SELECT s FROM StudentRecord s WHERE s.section IS NULL "
            + "AND LOWER(s.studentStatus) IN :statuses "
            + "AND s.batch.batchCode = :batchCode")
    java.util.List<StudentRecord> findSectionlessByStatusesAndBatch(
            @Param("statuses") java.util.Collection<String> statuses,
            @Param("batchCode") String batchCode);

    @Query("SELECT s FROM StudentRecord s WHERE s.section IS NULL "
            + "AND LOWER(s.studentStatus) IN :statuses "
            + "AND s.course.courseCode = :courseCode")
    java.util.List<StudentRecord> findSectionlessByStatusesAndCourse(
            @Param("statuses") java.util.Collection<String> statuses,
            @Param("courseCode") String courseCode);

    @Query("SELECT s FROM StudentRecord s WHERE s.section IS NULL "
            + "AND LOWER(s.studentStatus) IN :statuses "
            + "AND s.batch.batchCode = :batchCode "
            + "AND s.course.courseCode = :courseCode")
    java.util.List<StudentRecord> findSectionlessByStatusesAndBatchAndCourse(
            @Param("statuses") java.util.Collection<String> statuses,
            @Param("batchCode") String batchCode,
            @Param("courseCode") String courseCode);
}
