package com.example.springboot.repository;

import com.example.springboot.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {

    @Query(value = "SELECT COUNT(*) FROM grades WHERE subject_code = :subjectCode", nativeQuery = true)
    long countGradesBySubjectCode(@Param("subjectCode") String subjectCode);

    // Bulk rename, not a load-mutate-save on the entity: subjectCode is the
    // @Id, and JPA has no well-defined way to change a managed entity's own
    // identity. This issues a direct UPDATE on subjects.subject_code; the
    // ON UPDATE CASCADE foreign keys on classes.subject_code and
    // grades.subject_code (see migrations/2026-08-26-subjects-code-update-
    // cascade.sql) then ripple the rename into every referencing row.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Subject s SET s.subjectCode = :newCode WHERE s.subjectCode = :oldCode")
    void renameSubjectCode(@Param("oldCode") String oldCode, @Param("newCode") String newCode);
}
