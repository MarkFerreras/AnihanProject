package com.example.springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.springboot.model.StudentRecord;

public interface StudentRecordRepository extends JpaRepository<StudentRecord, String> {

    boolean existsByLastNameIgnoreCaseAndFirstNameIgnoreCaseAndMiddleNameIgnoreCase(
            String lastName, String firstName, String middleName);
}
