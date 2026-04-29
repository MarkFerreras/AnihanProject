package com.example.springboot.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.model.StudentUpload;

import jakarta.annotation.PostConstruct;

@Service
public class StorageService {

    private static final long ID_PHOTO_MAX_BYTES = 2L * 1024 * 1024;
    private static final long BAPT_CERT_MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> BAPT_CERT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf");

    @Value("${app.storage.root:./uploads}")
    private String storageRoot;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(storageRoot));
    }

    public StudentUpload store(String studentId, String kind, MultipartFile file) throws IOException {
        validateFile(kind, file);

        String mimeType = resolveMime(file);
        String ext = extensionFor(mimeType);
        String filename = kind + "_" + UUID.randomUUID() + ext;

        Path dir = Paths.get(storageRoot, "students", studentId);
        Files.createDirectories(dir);
        Path dest = dir.resolve(filename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        StudentUpload upload = new StudentUpload();
        upload.setStudentId(studentId);
        upload.setKind(kind);
        upload.setFilePath(dest.toString());
        upload.setOriginalName(file.getOriginalFilename());
        upload.setMimeType(mimeType);
        upload.setSizeBytes(file.getSize());
        upload.setUploadedAt(LocalDateTime.now());
        return upload;
    }

    public Resource load(StudentUpload upload) throws IOException {
        Path path = Paths.get(upload.getFilePath());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("File not found: " + upload.getFilePath());
        }
        return resource;
    }

    public void delete(StudentUpload upload) {
        try {
            Files.deleteIfExists(Paths.get(upload.getFilePath()));
        } catch (IOException ignored) {
        }
    }

    private void validateFile(String kind, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }
        String mime = resolveMime(file);
        if ("ID_PHOTO".equals(kind)) {
            if (file.getSize() > ID_PHOTO_MAX_BYTES) {
                throw new IllegalArgumentException("ID photo must not exceed 2 MB.");
            }
            if (!IMAGE_TYPES.contains(mime)) {
                throw new IllegalArgumentException("ID photo must be a JPEG, PNG, GIF, or WebP image.");
            }
        } else if ("BAPTISMAL_CERT".equals(kind)) {
            if (file.getSize() > BAPT_CERT_MAX_BYTES) {
                throw new IllegalArgumentException("Baptismal certificate must not exceed 5 MB.");
            }
            if (!BAPT_CERT_TYPES.contains(mime)) {
                throw new IllegalArgumentException("Baptismal certificate must be an image or PDF.");
            }
        } else {
            throw new IllegalArgumentException("Unknown upload kind: " + kind);
        }
    }

    private String resolveMime(MultipartFile file) {
        String declared = file.getContentType();
        if (declared != null && !declared.isBlank() && !"application/octet-stream".equals(declared)) {
            return declared.toLowerCase();
        }
        // Fallback: probe by filename extension
        String name = file.getOriginalFilename();
        if (name != null) {
            if (name.endsWith(".pdf")) return "application/pdf";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".gif")) return "image/gif";
            if (name.endsWith(".webp")) return "image/webp";
        }
        return "image/jpeg";
    }

    private String extensionFor(String mime) {
        return switch (mime) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
