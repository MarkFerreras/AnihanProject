package com.example.springboot.repository;

import java.util.Optional;

import com.example.springboot.model.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualificationRepository extends JpaRepository<Qualification, Integer> {

    Optional<Qualification> findByQualificationNameIgnoreCase(String qualificationName);
}
