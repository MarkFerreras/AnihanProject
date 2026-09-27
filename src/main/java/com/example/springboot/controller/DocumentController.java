package com.example.springboot.controller;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.dto.registrar.DocumentAuditContext;
import com.example.springboot.dto.registrar.DocumentExportScope;
import com.example.springboot.dto.registrar.DocumentFolderHierarchyResponse;
import com.example.springboot.dto.registrar.DocumentGenerateDataResponse;
import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.dto.registrar.GenerateDocumentRequest;
import com.example.springboot.model.Document;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.DocumentExportService;
import com.example.springboot.service.DocumentFolderService;
import com.example.springboot.service.DocumentGenerationService;
import com.example.springboot.service.DocumentService;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/registrar/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final DocumentFolderService documentFolderService;
    private final DocumentExportService documentExportService;
    private final DocumentGenerationService documentGenerationService;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService,
                              DocumentFolderService documentFolderService,
                              DocumentExportService documentExportService,
                              DocumentGenerationService documentGenerationService,
                              SystemLogService systemLogService,
                              UserRepository userRepository) {
        this.documentService = documentService;
        this.documentFolderService = documentFolderService;
        this.documentExportService = documentExportService;
        this.documentGenerationService = documentGenerationService;
        this.systemLogService = systemLogService;
        this.userRepository = userRepository;
    }

    @GetMapping("/export/student/{studentId}")
    public void exportStudent(@PathVariable String studentId, HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) throws IOException {
        streamExport(DocumentExportScope.STUDENT, studentId, httpRequest, httpResponse);
    }

    @GetMapping("/export/section/{sectionCode}")
    public void exportSection(@PathVariable String sectionCode, HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) throws IOException {
        streamExport(DocumentExportScope.SECTION, sectionCode, httpRequest, httpResponse);
    }

    @GetMapping("/export/batch/{batchCode}/unassigned")
    public void exportBatchUnassigned(@PathVariable String batchCode, HttpServletRequest httpRequest,
                                      HttpServletResponse httpResponse) throws IOException {
        streamExport(DocumentExportScope.UNASSIGNED, batchCode, httpRequest, httpResponse);
    }

    @GetMapping("/export/batch/{batchCode}")
    public void exportBatch(@PathVariable String batchCode, HttpServletRequest httpRequest,
                            HttpServletResponse httpResponse) throws IOException {
        streamExport(DocumentExportScope.BATCH, batchCode, httpRequest, httpResponse);
    }

    /**
     * Prepares the manifest, writes one "Requested ZIP export" audit row
     * before any headers are sent, then streams. A failure after the
     * response is already committed (bytes flushed) cannot be turned into a
     * JSON error any more — it is logged server-side and the connection is
     * simply left to end; an uncommitted failure resets the response and
     * rethrows so {@code GlobalExceptionHandler} produces a normal error.
     */
    private void streamExport(DocumentExportScope scope, String key,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        DocumentExportService.PreparedExport prepared = documentExportService.prepareExport(scope, key);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Requested ZIP export (" + scope.name().toLowerCase(Locale.ROOT) + " " + key + ", "
                        + prepared.entries().size() + " document(s))",
                httpRequest.getRemoteAddr());

        httpResponse.setContentType("application/zip");
        httpResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(prepared.fileName()).build().toString());
        httpResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        try {
            documentExportService.writeZip(prepared, httpResponse.getOutputStream());
        } catch (IOException | RuntimeException e) {
            log.error("ZIP export failed for scope={} key={}", scope, key, e);
            if (!httpResponse.isCommitted()) {
                httpResponse.reset();
                throw e;
            }
            // Already committed: no further bytes can be sent as a normal
            // error response — never append JSON to a partial ZIP.
        }
    }

    @GetMapping("/folders/tree")
    public ResponseEntity<List<DocumentFolderHierarchyResponse>> folderTree() {
        return ResponseEntity.ok(documentFolderService.getFolderHierarchy());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<DocumentSummaryResponse>> studentDocuments(@PathVariable String studentId) {
        return ResponseEntity.ok(documentService.getStudentDocuments(studentId));
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummaryResponse>> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "batchCode", required = false) String batchCode,
            @RequestParam(value = "sectionCode", required = false) String sectionCode
    ) {
        return ResponseEntity.ok(documentService.getDocuments(q, type, batchCode, sectionCode));
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> types() {
        return ResponseEntity.ok(documentService.getDocumentTypes());
    }

    @PostMapping
    public ResponseEntity<DocumentSummaryResponse> upload(
            @RequestParam("studentId") String studentId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        DocumentSummaryResponse saved = documentService.upload(studentId, documentType, file);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Uploaded document '" + saved.fileName() + "' (" + saved.documentType()
                        + ") for student " + saved.studentId(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<DocumentSummaryResponse>> uploadBatch(
            @RequestParam("studentId") String studentId,
            @RequestParam("documentTypes") List<String> documentTypes,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest httpRequest
    ) {
        LogContext ctx = getLogContext();
        DocumentAuditContext audit = new DocumentAuditContext(ctx.userId(), ctx.username(), ctx.role(),
                httpRequest.getRemoteAddr());

        List<DocumentSummaryResponse> saved = documentService.uploadBatch(
                studentId, documentTypes, files, audit);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Integer documentId,
                                           HttpServletRequest httpRequest) {
        DocumentService.DownloadPayload payload = documentService.prepareDownload(documentId);
        Document document = payload.document();

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Downloaded document '" + payload.fileName() + "' (" + document.getDocumentType()
                        + ") of student " + document.getStudent().getStudentId(),
                httpRequest.getRemoteAddr());

        return fileResponse(payload.fileName(), payload.contentType(), payload.content(), false);
    }

    @GetMapping("/{documentId}/view")
    public ResponseEntity<byte[]> view(@PathVariable Integer documentId) {
        Document document = documentService.getDocument(documentId);
        return fileResponse(document.getFileName(), document.getFileType(),
                document.getContentData(), true);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable Integer documentId,
                                       HttpServletRequest httpRequest) {
        DocumentSummaryResponse deleted = documentService.delete(documentId);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Deleted document '" + deleted.fileName() + "' (" + deleted.documentType()
                        + ") of student " + deleted.studentId(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.noContent().build();
    }

    /**
     * Uploads (or replaces) a student's 1x1 / 2x2 ID picture. Lives under the
     * documents API because the picture is stored as a document row, but it is
     * driven from the student-record screens, not the Documents page.
     */
    @PostMapping("/id-picture")
    public ResponseEntity<DocumentSummaryResponse> uploadIdPicture(
            @RequestParam("studentId") String studentId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        DocumentSummaryResponse saved = documentService.uploadIdPicture(studentId, file);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Uploaded ID picture '" + saved.fileName() + "' for student " + saved.studentId(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** Serves a student's ID picture inline for the record screens. 404 when none. */
    @GetMapping("/id-picture/{studentId}")
    public ResponseEntity<byte[]> idPicture(@PathVariable String studentId) {
        return documentService.findIdPicture(studentId)
                .map(d -> fileResponse(d.getFileName(), d.getFileType(), d.getContentData(), true))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Removes a student's ID picture. */
    @DeleteMapping("/id-picture/{studentId}")
    public ResponseEntity<Void> deleteIdPicture(@PathVariable String studentId,
                                                HttpServletRequest httpRequest) {
        documentService.deleteIdPicture(studentId);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Removed ID picture for student " + studentId,
                httpRequest.getRemoteAddr());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/generate-data/{studentId}")
    public ResponseEntity<DocumentGenerateDataResponse> generateData(@PathVariable String studentId) {
        return ResponseEntity.ok(documentGenerationService.getGenerateData(studentId));
    }

    @PostMapping("/generate")
    public ResponseEntity<DocumentSummaryResponse> generate(
            @Valid @RequestBody GenerateDocumentRequest request,
            HttpServletRequest httpRequest
    ) {
        DocumentSummaryResponse saved = documentService.saveGenerated(
                request.studentId(), request.documentType(), request.fileName(),
                request.html(), request.documentId());

        String action = request.documentId() == null ? "Generated" : "Updated generated";
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                action + " document '" + saved.fileName() + "' (" + saved.documentType()
                        + ") for student " + saved.studentId(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private ResponseEntity<byte[]> fileResponse(String fileName, String contentType,
                                                byte[] content, boolean inline) {
        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(fileName)
                .build();

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(content);
    }

    private LogContext getLogContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_UNKNOWN");
        Integer userId = userRepository.findByUsername(username)
                .map(User::getUserId)
                .orElse(null);
        return new LogContext(userId, username, role);
    }

    private record LogContext(Integer userId, String username, String role) {}
}