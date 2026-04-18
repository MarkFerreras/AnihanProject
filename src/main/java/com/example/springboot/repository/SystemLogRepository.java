package com.example.springboot.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.springboot.model.SystemLog;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Integer> {

    List<SystemLog> findAllByOrderByTimestampDesc();

    List<SystemLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
