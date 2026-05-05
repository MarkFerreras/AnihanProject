package com.example.springboot.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.dto.student.StudentDetailsRequest;
import com.example.springboot.dto.student.StudentDetailsResponse;
import com.example.springboot.dto.student.UploadRefDto;
import com.example.springboot.model.StudentUpload;
import com.example.springboot.repository.StudentUploadRepository;
import com.example.springboot.service.StorageService;
import com.example.springboot.service.StudentDetailsService;

@RestController
@RequestMapping("/api/student")
public class StudentDetailsController {

    private final StudentDetailsService studentDetailsService;
    private final StorageService storageService;
    private final StudentUploadRepository uploadRepo;

    public StudentDetailsController(StudentDetailsService studentDetailsService,
                                    StorageService storageService,
                                    StudentUploadRepository uploadRepo) {
        this.studentDetailsService = studentDetailsService;
        this.storageService = storageService;
        this.uploadRepo = uploadRepo;
    }

    /**
     * Creates a minimal "Enrolling" student record (name + status only)
     * so that uploads can reference the student_id FK.
     * No substantive data is persisted at this stage.
     */
    @PostMapping("/start")
    public ResponseEntity<StudentDetailsResponse> start(@RequestBody Map<String, String> body) {
        String lastName = body.getOrDefault("lastName", "").trim();
        String firstName = body.getOrDefault("firstName", "").trim();
        String middleName = body.getOrDefault("middleName", "").trim();
        if (lastName.isBlank() || firstName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        StudentDetailsResponse response = studentDetailsService.startOrResume(lastName, firstName, middleName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<StudentDetailsResponse> load(@PathVariable String studentId) {
        try {
            return ResponseEntity.ok(studentDetailsService.load(studentId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{studentId}/upload")
    public ResponseEntity<UploadRefDto> upload(
            @PathVariable String studentId,
            @RequestParam String kind,
            @RequestParam("file") MultipartFile file) {
        try {
            StudentUpload upload = storageService.store(studentId, kind, file);
            UploadRefDto ref = studentDetailsService.saveUpload(studentId, upload);
            return ResponseEntity.ok(ref);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/files/{uploadId}")
    public ResponseEntity<Resource> serveFile(@PathVariable Integer uploadId) {
        var opt = uploadRepo.findById(uploadId);
        if (opt.isEmpty()) return ResponseEntity.<Resource>notFound().build();
        var upload = opt.get();
        try {
            Resource resource = storageService.load(upload);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + upload.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(upload.getMimeType()))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.<Resource>notFound().build();
        }
    }

    /**
     * Final submit: accepts the full enrollment payload, persists all data
     * (student record, parents, education) in one transaction, and sets
     * status to "Submitted".
     */
    @PostMapping("/{studentId}/submit")
    public ResponseEntity<StudentDetailsResponse> submit(
            @PathVariable String studentId,
            @RequestBody StudentDetailsRequest req) {
        try {
            return ResponseEntity.ok(studentDetailsService.submitEnrollment(studentId, req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
