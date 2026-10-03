package com.example.springboot.dto.registrar;

import java.util.List;

/**
 * Per-student SO checklist (spec 2026-10-01 SO checklist §7). {@code stage} is NOT_APPLICABLE
 * (no items), PREVIEW (Active: completion items NOT_DUE, no verdict) or FINAL
 * (Completed/Graduated). {@code complete} is null unless FINAL, and covers student
 * requirements only — batch-level SO items are not checked.
 */
public record SoChecklistResponse(String stage, Boolean complete, int warningCount, List<SoChecklistItem> items) {
}
