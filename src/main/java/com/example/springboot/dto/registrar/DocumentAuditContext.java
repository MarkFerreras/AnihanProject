package com.example.springboot.dto.registrar;

/**
 * Identity of the caller performing an atomic bulk upload, carried into the
 * service so the single success audit row is written inside the same
 * transaction as the document inserts (spec §4).
 */
public record DocumentAuditContext(Integer userId, String username, String role, String ipAddress) {
}
