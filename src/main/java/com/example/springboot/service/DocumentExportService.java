package com.example.springboot.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.example.springboot.dto.registrar.DocumentExportScope;
import com.example.springboot.dto.registrar.ExportCheckResponse;
import com.example.springboot.dto.registrar.FlaggedStudent;
import com.example.springboot.exception.EmptyDocumentExportException;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.DocumentContentRepository;
import com.example.springboot.repository.DocumentFolderRepository;
import com.example.springboot.repository.DocumentFolderRepository.CheckRow;
import com.example.springboot.repository.DocumentFolderRepository.ExportRow;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;

/**
 * Builds and streams the four ZIP export scopes (spec §5). The manifest —
 * which documents, and their allocated entry names — is fixed the moment
 * {@link #prepareExport} returns; later uploads never appear, and a deleted
 * manifested row fails the subsequent {@link #writeZip}.
 */
@Service
public class DocumentExportService {

    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private static final int MAX_COMPONENT_LENGTH = 120;

    private final DocumentFolderRepository documentFolderRepository;
    private final DocumentContentRepository documentContentRepository;
    private final StudentRecordRepository studentRecordRepository;
    private final SectionRepository sectionRepository;
    private final BatchRepository batchRepository;

    public DocumentExportService(DocumentFolderRepository documentFolderRepository,
                                 DocumentContentRepository documentContentRepository,
                                 StudentRecordRepository studentRecordRepository,
                                 SectionRepository sectionRepository,
                                 BatchRepository batchRepository) {
        this.documentFolderRepository = documentFolderRepository;
        this.documentContentRepository = documentContentRepository;
        this.studentRecordRepository = studentRecordRepository;
        this.sectionRepository = sectionRepository;
        this.batchRepository = batchRepository;
    }

    public record ExportEntry(Integer documentId, String entryName) {
    }

    /**
     * {@code studentsInScope}/{@code flaggedCount} come from the same
     * missing-documents check the page runs, recomputed server-side for the
     * export audit row.
     */
    public record PreparedExport(String fileName, List<ExportEntry> entries,
                                 int studentsInScope, int flaggedCount) {
    }

    /**
     * Validates the scope's key exists, reads its BLOB-free manifest, and
     * allocates every ZIP entry path up front — nothing here touches
     * {@code content_data}. It also runs the missing-documents check query to
     * compute the {@code studentsInScope}/{@code flaggedCount} recorded in the
     * export audit row (skipped when the export is empty).
     */
    public PreparedExport prepareExport(DocumentExportScope scope, String key) {
        requireScopeExists(scope, key);

        List<ExportRow> rows = documentFolderRepository.findExportRows(scope, key);
        if (rows.isEmpty()) {
            throw new EmptyDocumentExportException("No documents to export.");
        }

        String zipFileName = buildZipFileName(scope, key, rows.get(0));
        List<ExportEntry> entries = buildEntries(scope, rows);
        ExportCheckResponse check = evaluate(documentFolderRepository.findCheckRows(scope, key));
        return new PreparedExport(zipFileName, entries, check.studentsInScope(), check.flagged().size());
    }

    /**
     * Pre-export check (spec 2026-10-01 §3): every student in the scope —
     * including students with zero documents — who is missing a required
     * document per {@link RequiredDocumentPolicy}.
     */
    public ExportCheckResponse checkMissing(DocumentExportScope scope, String key) {
        requireScopeExists(scope, key);
        return evaluate(documentFolderRepository.findCheckRows(scope, key));
    }

    /** Groups check rows per student (preserving query order) and applies the policy. */
    private static ExportCheckResponse evaluate(List<CheckRow> rows) {
        Map<String, StudentDocuments> byStudent = new LinkedHashMap<>();
        for (CheckRow row : rows) {
            StudentDocuments docs = byStudent.computeIfAbsent(row.studentId(), id -> new StudentDocuments(row));
            if (row.documentId() != null) {
                docs.documentCount++;
                docs.types.add(row.documentType());
            }
        }

        List<FlaggedStudent> flagged = new ArrayList<>();
        for (StudentDocuments docs : byStudent.values()) {
            List<String> missing = RequiredDocumentPolicy.missing(docs.student.studentStatus(), docs.types);
            if (!missing.isEmpty()) {
                CheckRow s = docs.student;
                flagged.add(new FlaggedStudent(s.studentId(), s.studentNumber(), s.lastName(), s.firstName(),
                        s.studentStatus(), s.sectionCode(), docs.documentCount, missing));
            }
        }
        return new ExportCheckResponse(byStudent.size(), flagged);
    }

    private static final class StudentDocuments {
        private final CheckRow student;
        private final Set<String> types = new HashSet<>();
        private long documentCount;

        private StudentDocuments(CheckRow student) {
            this.student = student;
        }
    }

    /**
     * Streams the archive one document at a time — never a buffered archive
     * or a loaded list of documents. Memory is bounded by one stored BLOB
     * plus ZIP bookkeeping, not by the copy buffer alone.
     */
    public void writeZip(PreparedExport prepared, OutputStream output) throws IOException {
        AbortableZipOutputStream zip = new AbortableZipOutputStream(output);
        boolean completed = false;
        try {
            for (ExportEntry entry : prepared.entries()) {
                // No try/finally around closeEntry(): if copyTo fails, the
                // export aborts anyway (completed stays false), so a second
                // exception from closeEntry() must never replace the real
                // root cause in the log below.
                zip.putNextEntry(new ZipEntry(entry.entryName()));
                documentContentRepository.copyTo(entry.documentId(), zip);
                zip.closeEntry();
            }
            zip.finish();
            completed = true;
        } finally {
            if (!completed) {
                // Release the Deflater's native resources without writing a
                // (misleading) trailer or touching the servlet-owned stream.
                zip.abort();
            }
        }
    }

    private void requireScopeExists(DocumentExportScope scope, String key) {
        boolean exists = switch (scope) {
            case STUDENT -> studentRecordRepository.existsByStudentId(key);
            case SECTION -> sectionRepository.existsById(key);
            case UNASSIGNED, BATCH -> batchRepository.existsById(key);
        };
        if (!exists) {
            throw new NoSuchElementException(scope.name().toLowerCase(Locale.ROOT) + " not found: " + key);
        }
    }

    private String buildZipFileName(DocumentExportScope scope, String key, ExportRow anyRow) {
        return switch (scope) {
            case STUDENT -> "Documents_" + numberOrId(anyRow) + "_"
                    + sanitizeComponent(anyRow.lastName(), anyRow.studentId()) + ".zip";
            case SECTION -> "Documents_Section_" + key + ".zip";
            case UNASSIGNED -> "Documents_Batch_" + key + "_Unassigned.zip";
            case BATCH -> "Documents_Batch_" + key + ".zip";
        };
    }

    private List<ExportEntry> buildEntries(DocumentExportScope scope, List<ExportRow> rows) {
        Map<String, Set<String>> usedByParent = new HashMap<>();
        Map<String, String> studentFolderByStudentId = new HashMap<>();
        Map<String, String> sectionFolderByCode = new HashMap<>();

        List<ExportEntry> entries = new ArrayList<>(rows.size());
        for (ExportRow row : rows) {
            String fileComponent = sanitizeComponent(row.fileName(), "document-" + row.documentId());

            String entryName = switch (scope) {
                case STUDENT -> allocateUnique(usedByParent, "", fileComponent);
                case SECTION, UNASSIGNED -> {
                    String studentFolder = studentFolderByStudentId.computeIfAbsent(row.studentId(),
                            id -> allocateUnique(usedByParent, " folders", studentFolderName(row)));
                    String uniqueFile = allocateUnique(usedByParent, studentFolder, fileComponent);
                    yield studentFolder + "/" + uniqueFile;
                }
                case BATCH -> {
                    String parent;
                    if (row.sectionCode() == null) {
                        parent = "Unassigned";
                    } else {
                        String sectionFolder = sectionFolderByCode.computeIfAbsent(row.sectionCode(),
                                code -> allocateUnique(usedByParent, " sections",
                                        sanitizeComponent(code, "section-" + code)));
                        parent = "Sections/" + sectionFolder;
                    }
                    String studentFolder = studentFolderByStudentId.computeIfAbsent(row.studentId(),
                            id -> allocateUnique(usedByParent, parent + " folders", studentFolderName(row)));
                    String uniqueFile = allocateUnique(usedByParent, parent + "/" + studentFolder, fileComponent);
                    yield parent + "/" + studentFolder + "/" + uniqueFile;
                }
            };

            entries.add(new ExportEntry(row.documentId(), entryName));
        }
        return entries;
    }

    private String studentFolderName(ExportRow row) {
        String raw = row.lastName() + "_" + row.firstName() + "_" + numberOrId(row) + "_" + row.studentId();
        return sanitizeComponent(raw, "student-" + row.studentId());
    }

    private static String numberOrId(ExportRow row) {
        return row.studentNumber() != null && !row.studentNumber().isBlank()
                ? row.studentNumber() : row.studentId();
    }

    /**
     * Case-insensitive unique-path allocation within one parent "directory"
     * (an opaque scope key — "" for the ZIP root, a folder path otherwise),
     * suffixing " (1)", " (2)", ... on collision, rechecking against
     * previously suffixed names too.
     */
    private static String allocateUnique(Map<String, Set<String>> usedByParent, String parent, String candidate) {
        Set<String> used = usedByParent.computeIfAbsent(parent, p -> new HashSet<>());
        String base = candidate;
        String extension = "";
        int dot = candidate.lastIndexOf('.');
        if (dot > 0) {
            base = candidate.substring(0, dot);
            extension = candidate.substring(dot);
        }
        String attempt = candidate;
        int suffix = 1;
        while (!used.add(attempt.toLowerCase(Locale.ROOT))) {
            attempt = base + " (" + suffix + ")" + extension;
            suffix++;
        }
        return attempt;
    }

    /**
     * Discards path prefixes/drive markers, strips control and reserved
     * separator/wildcard characters, trims trailing dots/spaces, renames
     * Windows-reserved device names, and caps length at 120 chars while
     * preserving a short extension.
     */
    private static String sanitizeComponent(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("^[A-Za-z]:", "");
        name = name.replaceAll("[\\x00-\\x1F<>:\"/\\\\|?*]", "");
        name = name.replaceAll("[ .]+$", "");
        name = name.replaceAll("^[ ]+", "");
        if (name.isBlank()) {
            return fallback;
        }
        name = capLength(name, MAX_COMPONENT_LENGTH);
        int dot = name.lastIndexOf('.');
        String baseNoExt = dot > 0 ? name.substring(0, dot) : name;
        if (WINDOWS_RESERVED_NAMES.contains(baseNoExt.toUpperCase(Locale.ROOT))) {
            name = "_" + name;
        }
        return name;
    }

    private static String capLength(String name, int max) {
        if (name.length() <= max) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0 && name.length() - dot <= 10) {
            String extension = name.substring(dot);
            String base = name.substring(0, dot);
            int keep = Math.max(1, max - extension.length());
            return base.substring(0, Math.min(base.length(), keep)) + extension;
        }
        return name.substring(0, max);
    }

    /**
     * Exposes {@code Deflater.end()} so a failed export can release native
     * resources without writing the ZIP trailer (which {@code close()}/
     * {@code finish()} would do) or closing the underlying stream, which
     * stays servlet-owned.
     */
    private static final class AbortableZipOutputStream extends ZipOutputStream {
        AbortableZipOutputStream(OutputStream out) {
            super(out);
        }

        void abort() {
            def.end();
        }
    }
}
