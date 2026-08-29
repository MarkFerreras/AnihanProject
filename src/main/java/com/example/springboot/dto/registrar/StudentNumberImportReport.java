package com.example.springboot.dto.registrar;

import java.util.List;
import java.util.Map;

/**
 * Result of a preview or an apply.
 *
 * @param fileName       the uploaded file, echoed back for the UI heading
 * @param applied        false for a preview (nothing was written), true after an apply
 * @param totalRows      data rows read from the sheet
 * @param applicableRows rows that will be (preview) or were (apply) written
 * @param countsByOutcome how many rows landed in each outcome, for the summary line
 * @param rows           per-row detail, in sheet order
 */
public record StudentNumberImportReport(
        String fileName,
        boolean applied,
        int totalRows,
        int applicableRows,
        Map<StudentNumberImportOutcome, Integer> countsByOutcome,
        List<StudentNumberImportRowResult> rows
) {
}
