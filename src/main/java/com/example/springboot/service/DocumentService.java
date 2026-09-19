package com.example.springboot.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

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

    /** Document type reserved for the student's 1x1 / 2x2 ID picture. */
    public static final String ID_PICTURE_TYPE = "ID Picture (1x1 / 2x2)";

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
            "Others",
            ID_PICTURE_TYPE
    );

    /** Upload whitelist per R3.1 (AGILE-75): pdf, docx, xlsx. */
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    /**
     * Image whitelist for the ID picture only. Deliberately separate from
     * {@link #ALLOWED_EXTENSIONS} so the general Documents page keeps accepting
     * exactly pdf/docx/xlsx and nothing else.
     */
    private static final Map<String, String> ID_PICTURE_EXTENSIONS = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "webp", "image/webp"
    );

    /** ID pictures are small by nature; 2MB matches the limit the old student portal used. */
    private static final long ID_PICTURE_MAX_BYTES = 2L * 1024 * 1024;

    /**
     * Short template names used for friendly download filenames of generated
     * documents — mirrors TEMPLATES[*].shortName in curriculum-templates.js.
     */
    private static final Map<String, String> TYPE_SHORT_NAMES = Map.of(
            "Transcript of Records (TOR)", "TOR",
            "Form IX - Bread and Pastry Production NC II", "FormIX-BPP",
            "Form IX - Cookery NC II", "FormIX-Cookery",
            "Form IX - Food and Beverage Services NC II", "FormIX-FBS"
    );

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
        return saveGenerated(studentId, documentType, fileName, htmlContent, null);
    }

    /**
     * As above; when {@code documentId} is present the existing generated document is
     * updated in place (edit flow) instead of inserting a new row.
     */
    public DocumentSummaryResponse saveGenerated(String studentId, String documentType,
                                                 String fileName, String htmlContent,
                                                 Integer documentId) {
        requireKnownType(documentType);

        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("A student ID is required.");
        }
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

        if (documentId != null) {
            Document existing = getDocument(documentId);
            String owner = existing.getStudent().getStudentId();
            if (!owner.equalsIgnoreCase(studentId.trim())) {
                throw new IllegalArgumentException(
                        "Document " + documentId + " belongs to student " + owner + ", not " + studentId);
            }
            String existingType = existing.getFileType() == null
                    ? "" : existing.getFileType().toLowerCase(Locale.ROOT);
            if (!existingType.startsWith("text/html")) {
                throw new IllegalArgumentException(
                        "Only generated (HTML) documents can be updated in place; document "
                                + documentId + " is an uploaded file.");
            }
            existing.setDocumentType(documentType);
            existing.setFileName(cleanName);
            existing.setFileType("text/html");
            existing.setFileSize(content.length);
            existing.setContentData(content);
            return toSummary(documentRepository.save(existing));
        }

        StudentRecord student = requireStudent(studentId);
        return toSummary(save(student, documentType, cleanName, "text/html", content));
    }

    /** Deletes a document and returns its summary so the caller can write the audit log. */
    public DocumentSummaryResponse delete(Integer documentId) {
        Document document = getDocument(documentId);
        DocumentSummaryResponse summary = toSummary(document);
        documentRepository.delete(document);
        return summary;
    }

    /** What the download endpoint should send: name, MIME type, and content bytes. */
    public record DownloadPayload(Document document, String fileName,
                                  String contentType, byte[] content) {
    }

    /**
     * Generated HTML documents download as an editable Word .docx named
     * "{ShortType}-{LastName} {FirstName}.docx"; uploaded files keep their
     * original name, type, and bytes.
     */
    public DownloadPayload prepareDownload(Integer documentId) {
        Document document = getDocument(documentId);
        String fileType = document.getFileType() == null
                ? "" : document.getFileType().toLowerCase(Locale.ROOT);

        if (fileType.startsWith("text/html")) {
            String shortType = TYPE_SHORT_NAMES.getOrDefault(
                    document.getDocumentType(), document.getDocumentType());
            String studentName = joinNonBlank(
                    document.getStudent().getLastName(), document.getStudent().getFirstName());
            if (studentName.isEmpty()) {
                studentName = document.getStudent().getStudentId();
            }
            String docxName = sanitizeFileName(shortType + "-" + studentName) + ".docx";
            return new DownloadPayload(document, docxName, HtmlDocxConverter.DOCX_MIME,
                    HtmlDocxConverter.toDocx(document.getContentData()));
        }

        return new DownloadPayload(document, document.getFileName(),
                document.getFileType(), document.getContentData());
    }

    /** Full entity fetch (including BLOB) — only for view/download of a single document. */
    public Document getDocument(Integer documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + documentId));
    }

    /** The student's ID picture, if one has been uploaded. */
    public Optional<Document> findIdPicture(String studentId) {
        return documentRepository.findByStudentStudentIdAndDocumentType(studentId, ID_PICTURE_TYPE);
    }

    /**
     * Stores (or replaces) a student's 1x1 / 2x2 ID picture as a row in the
     * {@code documents} table. One picture per student: re-uploading updates the
     * existing row in place rather than accumulating copies, mirroring the
     * replace-on-reupload behaviour the student portal used to have.
     */
    public DocumentSummaryResponse uploadIdPicture(String studentId, MultipartFile file) {
        StudentRecord student = requireStudent(studentId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No picture was provided.");
        }
        if (file.getSize() > ID_PICTURE_MAX_BYTES) {
            throw new IllegalArgumentException("ID picture exceeds the 2MB size limit.");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (originalName.isBlank() || originalName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        String mimeType = ID_PICTURE_EXTENSIONS.get(extensionOf(originalName));
        if (mimeType == null) {
            throw new IllegalArgumentException(
                    "Unsupported picture type. Allowed: jpg, jpeg, png, webp");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded picture. Please try again.");
        }

        Document document = documentRepository
                .findByStudentStudentIdAndDocumentType(studentId, ID_PICTURE_TYPE)
                .orElseGet(Document::new);

        document.setStudent(student);
        document.setDocumentType(ID_PICTURE_TYPE);
        document.setFileName(originalName);
        document.setFileType(mimeType);
        document.setFileSize(content.length);
        document.setContentData(content);

        return toSummary(documentRepository.save(document));
    }

    /** Removes a student's ID picture. No-op when none exists. */
    public void deleteIdPicture(String studentId) {
        documentRepository.findByStudentStudentIdAndDocumentType(studentId, ID_PICTURE_TYPE)
                .ifPresent(documentRepository::delete);
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

    /** Strips characters that are invalid in Windows/macOS filenames. */
    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", " ").trim();
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(part.trim());
            }
        }
        return sb.toString();
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
