package com.example.springboot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "batches")
public class Batch {

    @Id
    @Column(name = "batch_code", length = 20)
    private String batchCode;

    @Column(name = "batch_year", nullable = false)
    private Short batchYear;

    public Batch() {
    }

    public Batch(String batchCode, Short batchYear) {
        this.batchCode = batchCode;
        this.batchYear = batchYear;
    }

    // Getters and Setters

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public Short getBatchYear() {
        return batchYear;
    }

    public void setBatchYear(Short batchYear) {
        this.batchYear = batchYear;
    }
}
