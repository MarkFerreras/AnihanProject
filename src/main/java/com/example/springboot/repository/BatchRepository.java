package com.example.springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Batch;

public interface BatchRepository extends JpaRepository<Batch, String> {
}
