package com.example.springboot.dto.registrar;

/**
 * A real document type for an "Others" label, or null when the label matches none.
 *
 * <p>{@code "Form IX"} is a generic answer, not a document type: there are three Form IX types,
 * so the upload page shows the hint but offers no one-click switch for it — the Registrar picks
 * the specific Form IX from the type dropdown.
 */
public record TypeSuggestionResponse(String suggestedType) {
}
