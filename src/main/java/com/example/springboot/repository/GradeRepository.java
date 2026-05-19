package com.example.springboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Grade;

public interface GradeRepository extends JpaRepository<Grade, Integer> {
    List<Grade> findBySchoolClassClassId(Integer classId);

    Optional<Grade> findBySchoolClassClassIdAndStudentStudentId(Integer classId, String studentId);

    List<Grade> findByStudentStudentId(String studentId);
}
