package com.example.springboot.exception;

/**
 * Thrown when a ZIP export scope exists (student/section/batch reference is
 * valid) but has zero documents. Distinct from "scope doesn't exist" (404) —
 * this is a 409 per spec §3/§5.
 */
public class EmptyDocumentExportException extends RuntimeException {
    public EmptyDocumentExportException(String message) {
        super(message);
    }
}
