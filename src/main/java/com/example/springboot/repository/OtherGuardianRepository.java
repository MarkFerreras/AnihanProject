package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.OtherGuardian;

public interface OtherGuardianRepository extends JpaRepository<OtherGuardian, Integer> {

    List<OtherGuardian> findByStudentStudentId(String studentId);

    void deleteByStudentStudentId(String studentId);
}
