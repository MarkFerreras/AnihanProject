package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.model.ClassEnrollment;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Integer> {

    List<ClassEnrollment> findBySchoolClassClassId(Integer classId);

    boolean existsBySchoolClassClassIdAndStudentStudentId(Integer classId, String studentId);

    long countBySchoolClassClassId(Integer classId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClassEnrollment ce " +
           "WHERE ce.student.studentId = :studentId " +
           "AND ce.schoolClass.section.sectionCode = :sectionCode")
    int deleteByStudentAndSectionCode(@Param("studentId") String studentId,
                                       @Param("sectionCode") String sectionCode);
}
