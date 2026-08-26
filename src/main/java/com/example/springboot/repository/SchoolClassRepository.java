package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.SchoolClass;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Integer> {

    List<SchoolClass> findBySemester(String semester);

    List<SchoolClass> findBySubjectSubjectCode(String subjectCode);

    List<SchoolClass> findBySemesterAndSubjectSubjectCode(String semester, String subjectCode);

    boolean existsBySectionSectionCodeAndSubjectSubjectCodeAndSemester(
            String sectionCode, String subjectCode, String semester);

    boolean existsBySectionSectionCode(String sectionCode);

    boolean existsBySubjectSubjectCode(String subjectCode);

    List<SchoolClass> findByTrainerUserId(Integer userId);

    List<SchoolClass> findByTrainerUserIdAndSubjectSubjectCode(Integer userId, String subjectCode);

    List<SchoolClass> findByTrainerUserIdAndSemester(Integer userId, String semester);

    long countByTrainerUserIdAndSemester(Integer userId, String semester);
}
