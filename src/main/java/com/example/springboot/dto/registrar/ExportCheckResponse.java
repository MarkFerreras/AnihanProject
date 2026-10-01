package com.example.springboot.dto.registrar;

import java.util.List;

/**
 * Pre-export check result for one export scope. An empty {@code flagged}
 * list means the page can start the download immediately.
 */
public record ExportCheckResponse(int studentsInScope, List<FlaggedStudent> flagged) {
}
