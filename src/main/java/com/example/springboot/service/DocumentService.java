package com.example.springboot.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.model.Document;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.DocumentRepository;
import com.example.springboot.repository.StudentRecordRepository;

@Service
public class DocumentService {

    /** Document categories per R3.2 (AGILE-76) — the four generated templates plus common uploads. */
    private static final List<String> DOCUMENT_TYPES = List.of(
            "Transcript of Records (TOR)",
            "Form IX - Bread and Pastry Production NC II",
            "Form IX - Cookery NC II",
            "Form IX - Food and Beverage Services NC II",
            "Form 137",
            "PSA Birth Certificate",
            "OJT Report",
            "Certificate of TVET Program",
            "Others"
    );

    /** Upload whitelist per R3.1 (AGILE-75): pdf, docx, xlsx. */
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final StudentRecordRepository studentRecordRepository;

    public DocumentService(DocumentRepository documentRepository,
                           StudentRecordRepository studentRecordRepository) {
        this.documentRepository = documentRepository;
        this.studentRecordRepository = studentRecordRepository;
    }

    public List<String> getDocumentTypes() {
        return DOCUMENT_TYPES;
    }

    public List<DocumentSummaryResponse> getDocuments(String q, String documentType,
                                                      String batchCode, String sectionCode) {
        return documentRepository.searchSummaries(
                blankToNull(q), blankToNull(documentType),
                blankToNull(batchCode), blankToNull(sectionCode));
    }

    public DocumentSummaryResponse upload(String studentId, String documentType, MultipartFile file) {
        StudentRecord student = requireStudent(studentId);
        requireKnownType(documentType);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was provided.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds the 10MB size limit.");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (originalName.isBlank() || originalName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        String extension = extensionOf(originalName);
        String mimeType = ALLOWED_EXTENSIONS.get(extension);
        if (mimeType == null) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS.keySet()));
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file. Please try again.");
        }

        return toSummary(save(student, documentType, originalName, mimeType, content));
    }

    /**
     * Persists a generated, self-contained HTML document (TOR / Form IX) so it
     * immediately appears in the R3.3–R3.7 listing, view, search, and download flows.
     */
    public DocumentSummaryResponse saveGenerated(String studentId, String documentType,
                                                 String fileName, String htmlContent) {
        StudentRecord student = requireStudent(studentId);
        requireKnownType(documentType);

        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("A file name is required.");
        }
        if (!StringUtils.hasText(htmlContent)) {
            throw new IllegalArgumentException("The generated document is empty.");
        }

        byte[] content = htmlContent.getBytes(StandardCharsets.UTF_8);
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Generated document exceeds the 10MB size limit.");
        }

        String cleanName = StringUtils.cleanPath(fileName);
        if (cleanName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name.");
        }
        if (!cleanName.toLowerCase(Locale.ROOT).endsWith(".html")) {
            cleanName = cleanName + ".html";
        }

        return toSummary(save(student, documentType, cleanName, "text/html", content));
    }

    /** Full entity fetch (including BLOB) — only for view/download of a single document. */
    public Document getDocument(Integer documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + documentId));
    }

    private Document save(StudentRecord student, String documentType,
                          String fileName, String mimeType, byte[] content) {
        Document document = new Document();
        document.setStudent(student);
        document.setDocumentType(documentType);
        document.setFileName(fileName);
        document.setFileType(mimeType);
        document.setFileSize(content.length);
        document.setContentData(content);
        return documentRepository.save(document);
    }

    private StudentRecord requireStudent(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("A student ID is required.");
        }
        return studentRecordRepository.findByStudentId(studentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("No student record found for ID: " + studentId));
    }

    private void requireKnownType(String documentType) {
        if (!DOCUMENT_TYPES.contains(documentType)) {
            throw new IllegalArgumentException(
                    "Unknown document type. Allowed types: " + String.join(", ", DOCUMENT_TYPES));
        }
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static DocumentSummaryResponse toSummary(Document d) {
        return new DocumentSummaryResponse(
                d.getDocumentId(),
                d.getStudent().getStudentId(),
                d.getStudent().getLastName(),
                d.getStudent().getFirstName(),
                d.getDocumentType(),
                d.getFileName(),
                d.getFileType(),
                d.getFileSize(),
                d.getUploadDate());
    }
}
