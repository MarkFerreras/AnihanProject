package com.example.springboot.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot.dto.registrar.AssignStudentNumberRequest;
import com.example.springboot.dto.registrar.StudentNumberImportOutcome;
import com.example.springboot.dto.registrar.StudentNumberImportReport;
import com.example.springboot.dto.registrar.StudentNumberImportRowResult;
import com.example.springboot.model.StudentRecord;
import com.example.springboot.repository.StudentRecordRepository;

/**
 * Bulk-assigns student numbers from a sheet the Registrar exported, encoded, and uploaded back.
 *
 * <p>Preview and apply share one classification pass ({@link #classify}), so what the preview
 * promises and what the apply writes cannot drift apart. Apply re-reads and re-classifies the
 * uploaded file rather than trusting anything the client sends back.
 *
 * <p>All knowledge of the file's shape lives in {@link StudentNumberImportMapping} and
 * {@link StudentNumberSheetParser}; this class only decides what each row means.
 */
@Service
public class StudentNumberImportService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xlsx");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Pattern VALID_NUMBER = Pattern.compile(AssignStudentNumberRequest.PATTERN);

    private final StudentNumberSheetParser parser;
    private final StudentRecordRepository studentRecordRepository;

    public StudentNumberImportService(StudentNumberSheetParser parser,
                                      StudentRecordRepository studentRecordRepository) {
        this.parser = parser;
        this.studentRecordRepository = studentRecordRepository;
    }

    /** Validates and classifies the file. Writes nothing. */
    @Transactional(readOnly = true)
    public StudentNumberImportReport preview(MultipartFile file, boolean allowOverwrite) {
        UploadedFile upload = readUpload(file);
        List<StudentNumberImportRowResult> rows = classify(upload, allowOverwrite);
        return buildReport(upload.fileName(), false, rows);
    }

    /**
     * Re-classifies the file from scratch and writes the rows that are applicable.
     * Rows that are not applicable are reported and left alone — with the preview step in
     * front of this, applying the good rows beats failing 200 of them over one typo.
     */
    @Transactional
    public StudentNumberImportReport apply(MultipartFile file, boolean allowOverwrite) {
        UploadedFile upload = readUpload(file);
        List<StudentNumberImportRowResult> rows = classify(upload, allowOverwrite);

        for (StudentNumberImportRowResult row : rows) {
            if (!row.outcome().applicable() || row.recordId() == null) {
                continue;
            }
            studentRecordRepository.findById(row.recordId()).ifPresent(record -> {
                record.setStudentNumber(row.studentNumber());
                studentRecordRepository.save(record);
            });
        }

        return buildReport(upload.fileName(), true, rows);
    }

    // ----- Classification -----

    private List<StudentNumberImportRowResult> classify(UploadedFile upload, boolean allowOverwrite) {
        List<StudentNumberSheetParser.ParsedRow> parsed = parser.parse(upload.content(), upload.extension());

        // A number appearing on two rows of the same sheet is an encoding mistake; neither
        // row is applied, because we cannot know which one was intended.
        Map<String, Integer> occurrences = new HashMap<>();
        for (StudentNumberSheetParser.ParsedRow row : parsed) {
            if (row.studentNumber() != null) {
                occurrences.merge(row.studentNumber(), 1, Integer::sum);
            }
        }

        List<StudentNumberImportRowResult> results = new ArrayList<>(parsed.size());
        for (StudentNumberSheetParser.ParsedRow row : parsed) {
            results.add(classifyRow(row, occurrences, allowOverwrite));
        }
        return results;
    }

    private StudentNumberImportRowResult classifyRow(StudentNumberSheetParser.ParsedRow row,
                                                     Map<String, Integer> occurrences,
                                                     boolean allowOverwrite) {
        String reference = row.reference();
        String number = row.studentNumber();
        String sheetName = joinName(row.lastName(), row.firstName());

        if (number == null) {
            return result(row, sheetName, StudentNumberImportOutcome.BLANK,
                    "No student number supplied — row skipped.", null);
        }
        if (number.length() > AssignStudentNumberRequest.MAX_LENGTH) {
            return result(row, sheetName, StudentNumberImportOutcome.INVALID_FORMAT,
                    "Student number must be at most " + AssignStudentNumberRequest.MAX_LENGTH
                            + " characters.", null);
        }
        if (!VALID_NUMBER.matcher(number).matches()) {
            return result(row, sheetName, StudentNumberImportOutcome.INVALID_FORMAT,
                    AssignStudentNumberRequest.ALLOWED_CHARS_MESSAGE + ".", null);
        }
        if (occurrences.getOrDefault(number, 0) > 1) {
            return result(row, sheetName, StudentNumberImportOutcome.DUPLICATE_IN_FILE,
                    "Student number " + number + " appears more than once in this file.", null);
        }
        if (reference == null) {
            return result(row, sheetName, StudentNumberImportOutcome.UNKNOWN_REFERENCE,
                    "This row has no " + StudentNumberImportMapping.COLUMN_REFERENCE + ".", null);
        }

        Optional<StudentRecord> match = studentRecordRepository.findByStudentId(reference);
        if (match.isEmpty()) {
            return result(row, sheetName, StudentNumberImportOutcome.UNKNOWN_REFERENCE,
                    "No student has reference " + reference + ".", null);
        }

        StudentRecord student = match.get();
        Integer recordId = student.getRecordId();
        String storedName = joinName(student.getLastName(), student.getFirstName());
        String existing = blankToNull(student.getStudentNumber());

        if (number.equals(existing)) {
            return result(row, storedName, StudentNumberImportOutcome.UNCHANGED,
                    "Already set to " + number + ".", recordId);
        }

        Optional<StudentRecord> holder = studentRecordRepository.findByStudentNumber(number);
        if (holder.isPresent() && !holder.get().getRecordId().equals(recordId)) {
            StudentRecord other = holder.get();
            return result(row, storedName, StudentNumberImportOutcome.CONFLICT_IN_USE,
                    "Student number " + number + " is already assigned to "
                            + joinName(other.getLastName(), other.getFirstName()) + ".", recordId);
        }

        if (existing != null && !allowOverwrite) {
            return result(row, storedName, StudentNumberImportOutcome.CONFLICT_EXISTING,
                    "Already has " + existing + ". Tick \"Allow overwriting existing numbers\""
                            + " to replace it with " + number + ".", recordId);
        }

        // The name in the sheet disagreeing with the record usually means rows have slipped
        // out of alignment. It is applied, but surfaced so the registrar can spot-check.
        if (nameDisagrees(row, student)) {
            String action = existing != null
                    ? "Replacing " + existing + " with " + number
                    : "Assigning " + number;
            return result(row, storedName, StudentNumberImportOutcome.NAME_MISMATCH,
                    action + ", but the sheet says \"" + sheetName + "\" and the record says \""
                            + storedName + "\". Please verify.", recordId);
        }

        if (existing != null) {
            return result(row, storedName, StudentNumberImportOutcome.WILL_OVERWRITE,
                    "Replacing " + existing + " with " + number + ".", recordId);
        }
        return result(row, storedName, StudentNumberImportOutcome.WILL_ASSIGN,
                "Assigning " + number + ".", recordId);
    }

    /** Only compares the parts the sheet actually supplied. */
    private boolean nameDisagrees(StudentNumberSheetParser.ParsedRow row, StudentRecord student) {
        if (row.lastName() != null && !row.lastName().equalsIgnoreCase(safe(student.getLastName()))) {
            return true;
        }
        return row.firstName() != null && !row.firstName().equalsIgnoreCase(safe(student.getFirstName()));
    }

    // ----- Upload handling -----

    private record UploadedFile(String fileName, String extension, byte[] content) {
    }

    private UploadedFile readUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was provided.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds the 5MB size limit.");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        if (originalName.isBlank() || originalName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS) + ".");
        }

        try {
            return new UploadedFile(originalName, extension, file.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file.", e);
        }
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    // ----- Report assembly -----

    private StudentNumberImportReport buildReport(String fileName, boolean applied,
                                                  List<StudentNumberImportRowResult> rows) {
        Map<StudentNumberImportOutcome, Integer> counts = new EnumMap<>(StudentNumberImportOutcome.class);
        int applicable = 0;
        for (StudentNumberImportRowResult row : rows) {
            counts.merge(row.outcome(), 1, Integer::sum);
            if (row.outcome().applicable()) {
                applicable++;
            }
        }
        return new StudentNumberImportReport(fileName, applied, rows.size(), applicable, counts, rows);
    }

    private StudentNumberImportRowResult result(StudentNumberSheetParser.ParsedRow row, String name,
                                                StudentNumberImportOutcome outcome, String message,
                                                Integer recordId) {
        return new StudentNumberImportRowResult(
                row.rowNumber(), row.reference(), name, row.studentNumber(), outcome, message, recordId);
    }

    private String joinName(String lastName, String firstName) {
        String last = safe(lastName);
        String first = safe(firstName);
        if (last.isEmpty() && first.isEmpty()) {
            return "";
        }
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return last + ", " + first;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
