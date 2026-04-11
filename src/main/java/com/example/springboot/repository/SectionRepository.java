package com.example.springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.springboot.model.Section;

public interface SectionRepository extends JpaRepository<Section, String> {
}
