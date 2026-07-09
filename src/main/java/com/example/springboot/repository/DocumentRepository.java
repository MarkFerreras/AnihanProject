package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.model.Document;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    /**
     * BLOB-free listing with optional search and filters. The constructor
     * expression never touches content_data, so this stays cheap no matter
     * how large the stored files are.
     */
    @Query("""
            SELECT new com.example.springboot.dto.registrar.DocumentSummaryResponse(
                d.documentId, s.studentId, s.lastName, s.firstName,
                d.documentType, d.fileName, d.fileType, d.fileSize, d.uploadDate)
            FROM Document d
            JOIN d.student s
            LEFT JOIN s.batch b
            LEFT JOIN s.section sec
            WHERE (:q IS NULL
                   OR LOWER(s.studentId) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(s.lastName, ', ', s.firstName)) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:documentType IS NULL OR d.documentType = :documentType)
              AND (:batchCode IS NULL OR b.batchCode = :batchCode)
              AND (:sectionCode IS NULL OR sec.sectionCode = :sectionCode)
            ORDER BY d.uploadDate DESC, d.documentId DESC
            """)
    List<DocumentSummaryResponse> searchSummaries(@Param("q") String q,
                                                  @Param("documentType") String documentType,
                                                  @Param("batchCode") String batchCode,
                                                  @Param("sectionCode") String sectionCode);
}