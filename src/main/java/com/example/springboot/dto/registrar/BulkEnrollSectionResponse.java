package com.example.springboot.dto.registrar;

public record BulkEnrollSectionResponse(
        int enrolledCount,
        int skippedAlreadyEnrolled,
        int skippedIneligible,
        int totalConsidered
) {
}
