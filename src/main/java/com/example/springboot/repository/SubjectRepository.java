package com.example.springboot.repository;

import com.example.springboot.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {

    @Query(value = "SELECT COUNT(*) FROM grades WHERE subject_code = :subjectCode", nativeQuery = true)
    long countGradesBySubjectCode(@Param("subjectCode") String subjectCode);
}
