package com.example.springboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.StudentUpload;

public interface StudentUploadRepository extends JpaRepository<StudentUpload, Integer> {

    Optional<StudentUpload> findByStudentIdAndKind(String studentId, String kind);

    List<StudentUpload> findByStudentId(String studentId);

    void deleteByStudentId(String studentId);
}
