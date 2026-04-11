package com.example.springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Subject;

public interface SubjectRepository extends JpaRepository<Subject, String> {
}
