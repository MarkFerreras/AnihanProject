package com.example.springboot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot.model.StudentRecord;

public interface StudentRecordRepository extends JpaRepository<StudentRecord, Integer> {

    boolean existsByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            String lastName, String firstName, String middleName);

    Optional<StudentRecord> findByStudentId(String studentId);

    Optional<StudentRecord> findByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            String lastName, String firstName, String middleName);

    @Query("SELECT COUNT(s) FROM StudentRecord s WHERE s.studentId LIKE :prefix%")
    long countByStudentIdStartingWith(@Param("prefix") String prefix);
}
