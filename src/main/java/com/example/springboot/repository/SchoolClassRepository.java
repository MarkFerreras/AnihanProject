package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.SchoolClass;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Integer> {

    List<SchoolClass> findBySemester(String semester);

    boolean existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester(
            String sectionCode, String subjectCode, String semester);

    boolean existsBySectionSectionCode(String sectionCode);
}
