package com.example.springboot.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.dto.registrar.DocumentAuditContext;
import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.model.Document;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.DocumentRepository;
import com.example.springboot.repository.StudentRecordRepository;

@Service
public class DocumentService {

    /** Document type reserved for the student's 1x1 / 2x2 ID picture. */
    public static final String ID_PICTURE_TYPE = "ID Picture (1x1 / 2x2)";

    public static final String TOR_TYPE = "Transcript of Records (TOR)";
    public static final String FORM_IX_BPP_TYPE = "Form IX - Bread and Pastry Production NC II";
    public static final String FORM_IX_COOKERY_TYPE = "Form IX - Cookery NC II";
    public static final String FORM_IX_FBS_TYPE = "Form IX - Food and Beverage Services NC II";
    public static final String FORM_137_TYPE = "Form 137";
    public static final String PSA_BIRTH_CERTIFICATE_TYPE = "PSA Birth Certificate";
    public static final String OJT_REPORT_TYPE = "OJT Report";
    public static final String TVET_CERTIFICATE_TYPE = "Certificate of TVET Program";
    /** The only type that may carry a custom {@code document_label}. */
    public static final String OTHERS_TYPE = "Others";

    /** Document categories per R3.2 (AGILE-76) — the four generated templates plus common uploads. */
    private static final List<String> DOCUMENT_TYPES = List.of(
            TOR_TYPE,
            FORM_IX_BPP_TYPE,
            FORM_IX_COOKERY_TYPE,
            FORM_IX_FBS_TYPE,
            FORM_137_TYPE,
            PSA_BIRTH_CERTIFICATE_TYPE,
            OJT_REPORT_TYPE,
            TVET_CERTIFICATE_TYPE,
            OTHERS_TYPE,
            ID_PICTURE_TYPE
    );

    /** Upload whitelist per R3.1 (AGILE-75): pdf, docx, xlsx. */
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    /** Bulk upload limits per spec §1/§4: 20 files max, 10 MiB/file, 50 MiB combined. */
    private static final int MAX_BATCH_FILES = 20;
    private static final long MAX_BATCH_TOTAL_BYTES = 50L * 1024 * 1024;

    /** Custom "Others" name limit — matches documents.document_label VARCHAR(100). */
    private static final int MAX_LABEL_LENGTH = 100;
    private static final Pattern LABEL_CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");

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
            TOR_TYPE, "TOR",
            FORM_IX_BPP_TYPE, "FormIX-BPP",
            FORM_IX_COOKERY_TYPE, "FormIX-Cookery",
            FORM_IX_FBS_TYPE, "FormIX-FBS"
    );

    private final DocumentRepository documentRepository;
    private final StudentRecordRepository studentRecordRepository;
    private final SystemLogService systemLogService;

    public DocumentService(DocumentRepository documentRepository,
                           StudentRecordRepository studentRecordRepository,
                           SystemLogService systemLogService) {
        this.documentRepository = documentRepository;
        this.studentRecordRepository = studentRecordRepository;
        this.systemLogService = systemLogService;
    }

    public List<String> getDocumentTypes() {
        return DOCUMENT_TYPES;
    }

    public List<String> getDocumentLabels() {
        return documentRepository.findDistinctDocumentLabels();
    }

    public List<DocumentSummaryResponse> getDocuments(String q, String documentType,
                                                      String batchCode, String sectionCode) {
        return documentRepository.searchSummaries(
                blankToNull(q), blankToNull(documentType),
                blankToNull(batchCode), blankToNull(sectionCode));
    }

    /**
     * Exact-reference document listing for a single student, used by the
     * folder explorer's student panel. Unlike {@link #getDocuments}, this
     * never does a substring/LIKE match.
     */
    public List<DocumentSummaryResponse> getStudentDocuments(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("A student ID is required.");
        }
        String trimmed = studentId.trim();
        if (!studentRecordRepository.existsByStudentId(trimmed)) {
            throw new NoSuchElementException("No student record found for ID: " + trimmed);
        }
        return documentRepository.findSummariesByStudentId(trimmed);
    }

    /** One validated, ready-to-persist file from a {@link #uploadBatch} request. */
    private record ValidatedFile(MultipartFile file, String documentType, String documentLabel,
                                 String fileName, String mimeType) {
    }

    /** Batch upload without custom names — every file is stored unlabelled. */
    @Transactional
    public List<DocumentSummaryResponse> uploadBatch(String studentId, List<String> documentTypes,
                                                      List<MultipartFile> files, DocumentAuditContext audit) {
        return uploadBatch(studentId, documentTypes, null, files, audit);
    }

    /**
     * Validates and saves an entire batch of files for one student inside a
     * single database transaction: either every file and the one success
     * audit row are committed, or nothing is (spec §4). All validation runs
     * before any file is read or persisted. {@code documentLabels} is
     * optional (null = no names) and parallel to {@code files}; a label is
     * only allowed on an "Others" document (spec 2026-10-01 §5.2).
     */
    @Transactional
    public List<DocumentSummaryResponse> uploadBatch(String studentId, List<String> documentTypes,
                                                      List<String> documentLabels,
                                                      List<MultipartFile> files, DocumentAuditContext audit) {
        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("A student ID is required.");
        }
        // Unlike upload()/requireStudent() (400), an unknown-but-well-formed
        // reference here is a 404 per spec §4 — the field itself was valid.
        String trimmedStudentId = studentId.trim();
        StudentRecord student = studentRecordRepository.findByStudentId(trimmedStudentId)
                .orElseThrow(() -> new NoSuchElementException("No student record found for ID: " + trimmedStudentId));

        if (files == null || documentTypes == null) {
            throw new IllegalArgumentException("Files and document types are required.");
        }
        if (files.size() != documentTypes.size()) {
            throw new IllegalArgumentException("The number of files and document types must match.");
        }
        if (documentLabels != null && documentLabels.size() != files.size()) {
            throw new IllegalArgumentException("The number of files and document labels must match.");
        }
        int count = files.size();
        if (count == 0) {
            throw new IllegalArgumentException("At least one file is required.");
        }
        if (count > MAX_BATCH_FILES) {
            throw new IllegalArgumentException("A maximum of " + MAX_BATCH_FILES + " files may be uploaded at once.");
        }

        List<ValidatedFile> validated = new ArrayList<>(count);
        long totalSize = 0;
        for (int i = 0; i < count; i++) {
            MultipartFile file = files.get(i);
            String documentType = documentTypes.get(i);
            int position = i + 1;

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File at position " + position + " is empty.");
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("File at position " + position + " exceeds the 10MB size limit.");
            }
            totalSize += file.getSize();

            if (ID_PICTURE_TYPE.equals(documentType)) {
                throw new IllegalArgumentException(
                        "ID Picture uploads use a dedicated endpoint, not batch upload (file " + position + ").");
            }
            requireKnownType(documentType);
            String documentLabel = normalizeLabel(
                    documentLabels == null ? null : documentLabels.get(i), documentType, position);

            String originalName = requireValidFileName(file.getOriginalFilename(), position);

            String extension = extensionOf(originalName);
            String mimeType = ALLOWED_EXTENSIONS.get(extension);
            if (mimeType == null) {
                throw new IllegalArgumentException(
                        "Unsupported file type at position " + position + ". Allowed: "
                                + String.join(", ", ALLOWED_EXTENSIONS.keySet()));
            }

            validated.add(new ValidatedFile(file, documentType, documentLabel, originalName, mimeType));
        }

        if (totalSize > MAX_BATCH_TOTAL_BYTES) {
            throw new IllegalArgumentException("Combined upload size exceeds the 50MB limit.");
        }

        List<Integer> savedIds = new ArrayList<>(count);
        for (ValidatedFile vf : validated) {
            byte[] content;
            try {
                content = vf.file().getBytes();
            } catch (IOException e) {
                // Unchecked so the surrounding @Transactional method still rolls
                // back — Spring only auto-rolls-back on RuntimeException/Error.
                throw new UncheckedIOException("Could not read an uploaded file. Please try again.", e);
            }
            Document saved = save(student, vf.documentType(), vf.documentLabel(), vf.fileName(), vf.mimeType(),
                    content);
            savedIds.add(saved.getDocumentId());
        }

        systemLogService.logAction(audit.userId(), audit.username(), audit.role(),
                "Uploaded " + count + " document(s) for student " + student.getStudentId(),
                audit.ipAddress());

        documentRepository.flush();
        Map<Integer, DocumentSummaryResponse> byId = new LinkedHashMap<>();
        for (DocumentSummaryResponse summary : documentRepository.findSummariesByIds(savedIds)) {
            byId.put(summary.documentId(), summary);
        }
        return savedIds.stream().map(byId::get).toList();
    }

    /**
     * Trims a custom "Others" name; blank becomes null. Rejects a name on any
     * other type, an over-long name, control characters, and a name equal to
     * a real type — a real required document must never hide under "Others".
     */
    private static String normalizeLabel(String rawLabel, String documentType, int position) {
        if (!StringUtils.hasText(rawLabel)) {
            return null;
        }
        String label = rawLabel.trim();
        if (!OTHERS_TYPE.equals(documentType)) {
            throw new IllegalArgumentException(
                    "A document name can only be given to 'Others' documents (file " + position + ").");
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException(
                    "Document name exceeds " + MAX_LABEL_LENGTH + " characters (file " + position + ").");
        }
        if (LABEL_CONTROL_CHARS.matcher(label).find()) {
            throw new IllegalArgumentException(
                    "Document name contains invalid characters (file " + position + ").");
        }
        for (String type : DOCUMENT_TYPES) {
            if (type.equalsIgnoreCase(label)) {
                throw new IllegalArgumentException("'" + label + "' is a document type — pick it from the type "
                        + "list instead of using Others (file " + position + ").");
            }
        }
        return label;
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

        String originalName = requireValidFileName(file.getOriginalFilename(), null);

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

        return toSummary(save(student, documentType, null, originalName, mimeType, content));
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
        return toSummary(save(student, documentType, null, cleanName, "text/html", content));
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

    private Document save(StudentRecord student, String documentType, String documentLabel,
                          String fileName, String mimeType, byte[] content) {
        Document document = new Document();
        document.setStudent(student);
        document.setDocumentType(documentType);
        document.setDocumentLabel(documentLabel);
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

    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[\\x00-\\x1F<>:\"/\\\\|?*]");
    private static final int MAX_FILE_NAME_LENGTH = 255;

    /**
     * Strips any browser-supplied path prefix and rejects a basename that is
     * blank, dot-only, over 255 characters, or contains a control character
     * or a Windows-reserved separator/wildcard (spec §4).
     */
    private static String requireValidFileName(String rawName, Integer position) {
        String suffix = position == null ? "." : (" at position " + position + ".");
        String name = StringUtils.cleanPath(rawName == null ? "" : rawName);
        int lastSeparator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSeparator >= 0) {
            name = name.substring(lastSeparator + 1);
        }
        if (name.isBlank() || name.contains("..") || INVALID_FILENAME_CHARS.matcher(name).find()) {
            throw new IllegalArgumentException("Invalid file name" + suffix);
        }
        if (name.replaceAll("\\.+$", "").isBlank()) {
            throw new IllegalArgumentException("Invalid file name" + suffix);
        }
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("File name exceeds " + MAX_FILE_NAME_LENGTH + " characters" + suffix);
        }
        return name;
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
                d.getUploadDate(),
                d.getDocumentLabel());
    }
}
