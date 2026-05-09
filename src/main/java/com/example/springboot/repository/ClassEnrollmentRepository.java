package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.ClassEnrollment;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Integer> {

    List<ClassEnrollment> findBySchoolClassClassId(Integer classId);

    boolean existsBySchoolClassClassIdAndStudentStudentId(Integer classId, String studentId);

    long countBySchoolClassClassId(Integer classId);
}
