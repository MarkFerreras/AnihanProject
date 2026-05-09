package com.example.springboot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Batch;

public interface BatchRepository extends JpaRepository<Batch, String> {

    Optional<Batch> findFirstByBatchYear(Short batchYear);

    Optional<Batch> findTopByOrderByBatchYearDesc();
}
