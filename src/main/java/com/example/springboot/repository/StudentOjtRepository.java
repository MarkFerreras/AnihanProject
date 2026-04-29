package com.example.springboot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.StudentOjt;

public interface StudentOjtRepository extends JpaRepository<StudentOjt, Integer> {

    Optional<StudentOjt> findByStudentId(String studentId);

    void deleteByStudentId(String studentId);
}
