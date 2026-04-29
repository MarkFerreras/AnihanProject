package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.StudentTesdaQualification;

public interface StudentTesdaQualificationRepository extends JpaRepository<StudentTesdaQualification, Integer> {

    List<StudentTesdaQualification> findByStudentIdOrderBySlot(String studentId);

    void deleteByStudentId(String studentId);
}
