package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Section;

public interface SectionRepository extends JpaRepository<Section, String> {

    List<Section> findByBatchBatchYear(Short batchYear);
}
