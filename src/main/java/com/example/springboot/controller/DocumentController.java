package com.example.springboot.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.dto.registrar.DocumentGenerateDataResponse;
import com.example.springboot.dto.registrar.DocumentSummaryResponse;
import com.example.springboot.dto.registrar.GenerateDocumentRequest;
import com.example.springboot.model.Document;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.DocumentGenerationService;
import com.example.springboot.service.DocumentService;
import com.example.springboot.service.SystemLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/registrar/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentGenerationService documentGenerationService;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService,
                              DocumentGenerationService documentGenerationService,
                              SystemLogService systemLogService,
                              UserRepository userRepository) {
        this.documentService = documentService;
        this.documentGenerationService = documentGenerationService;
        this.systemLogService = systemLogService;
        this.userRepository = userRepository;
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

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Integer documentId,
                                           HttpServletRequest httpRequest) {
        Document document = documentService.getDocument(documentId);

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Downloaded document '" + document.getFileName() + "' (" + document.getDocumentType()
                        + ") of student " + document.getStudent().getStudentId(),
                httpRequest.getRemoteAddr());

        return fileResponse(document, false);
    }

    @GetMapping("/{documentId}/view")
    public ResponseEntity<byte[]> view(@PathVariable Integer documentId) {
        return fileResponse(documentService.getDocument(documentId), true);
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
                request.studentId(), request.documentType(), request.fileName(), request.html());

        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                "Generated document '" + saved.fileName() + "' (" + saved.documentType()
                        + ") for student " + saved.studentId(),
                httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private ResponseEntity<byte[]> fileResponse(Document document, boolean inline) {
        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(document.getFileName())
                .build();

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(document.getFileType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(document.getContentData());
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