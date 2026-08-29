package com.example.springboot.dto.registrar;

/**
 * One row of the import report.
 *
 * @param rowNumber     1-based row in the uploaded sheet, so the registrar can find it in Excel
 * @param reference     the Reference No. read from the sheet
 * @param studentName   the stored student's name when the reference resolved, else the sheet's
 * @param studentNumber the number proposed by the sheet
 * @param outcome       what the import decided
 * @param message       human-readable explanation, always populated for non-applicable outcomes
 * @param recordId      the matched record, when the reference resolved
 */
public record StudentNumberImportRowResult(
        int rowNumber,
        String reference,
        String studentName,
        String studentNumber,
        StudentNumberImportOutcome outcome,
        String message,
        Integer recordId
) {
}
