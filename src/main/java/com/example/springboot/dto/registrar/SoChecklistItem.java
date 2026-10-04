package com.example.springboot.dto.registrar;

/**
 * One SO checklist row. {@code state} is MET, WARNING, UNMET or NOT_DUE;
 * {@code detail} is a short sentence the Registrar can act on.
 */
public record SoChecklistItem(String key, String label, String state, String detail) {
}
