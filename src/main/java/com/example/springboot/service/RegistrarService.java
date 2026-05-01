package com.example.springboot.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.example.springboot.dto.registrar.StudentRecordDetailsResponse;
import com.example.springboot.dto.registrar.StudentRecordSummaryResponse;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.StudentRecordRepository;

@Service
public class RegistrarService {

    private final StudentRecordRepository studentRecordRepository;

    public RegistrarService(StudentRecordRepository studentRecordRepository) {
        this.studentRecordRepository = studentRecordRepository;
    }

    public List<StudentRecordSummaryResponse> getAllRecords() {
        return studentRecordRepository.findAll().stream()
                .map(StudentRecordSummaryResponse::from)
                .toList();
    }

    public StudentRecordDetailsResponse getRecordById(Integer recordId) {
        StudentRecord record = studentRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Student record not found: " + recordId));
        return StudentRecordDetailsResponse.from(record);
    }
}
