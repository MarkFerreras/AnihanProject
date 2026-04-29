package com.example.springboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.StudentEducation;

public interface StudentEducationRepository extends JpaRepository<StudentEducation, Integer> {

    List<StudentEducation> findByStudentIdOrderByLevel(String studentId);

    Optional<StudentEducation> findByStudentIdAndLevel(String studentId, String level);

    void deleteByStudentId(String studentId);
}
