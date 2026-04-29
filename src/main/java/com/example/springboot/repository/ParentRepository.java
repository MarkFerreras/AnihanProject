package com.example.springboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Parent;

public interface ParentRepository extends JpaRepository<Parent, Integer> {

    List<Parent> findByStudentStudentId(String studentId);

    Optional<Parent> findByStudentStudentIdAndRelation(String studentId, String relation);

    void deleteByStudentStudentId(String studentId);
}
