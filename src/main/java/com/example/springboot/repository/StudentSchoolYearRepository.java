package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.StudentSchoolYear;

public interface StudentSchoolYearRepository extends JpaRepository<StudentSchoolYear, Integer> {

    List<StudentSchoolYear> findByStudentIdOrderByRowIndex(String studentId);

    void deleteByStudentId(String studentId);
}
