package com.example.springboot.dto.registrar;

/**
 * What the import decided about one row of the uploaded sheet.
 *
 * <p>{@link #applicable()} is the single definition of "this row causes a write", used by
 * both the preview counts and the apply step so the two can never disagree.
 */
public enum StudentNumberImportOutcome {

    /** Reference resolves, the number is free, and the student has none yet. */
    WILL_ASSIGN(true),

    /** Student already has a different number and overwriting was explicitly allowed. */
    WILL_OVERWRITE(true),

    /** Applied, but the name in the sheet disagrees with the record — worth a look. */
    NAME_MISMATCH(true),

    /** The number already belongs to a different student. */
    CONFLICT_IN_USE(false),

    /** Student already has a different number and overwriting was not allowed. */
    CONFLICT_EXISTING(false),

    /** The sheet value already matches what is stored — nothing to do. */
    UNCHANGED(false),

    /** No student has that reference. */
    UNKNOWN_REFERENCE(false),

    /** The same number appears on more than one row of this file. */
    DUPLICATE_IN_FILE(false),

    /** Fails the same length/character rules as a single assignment. */
    INVALID_FORMAT(false),

    /** No number supplied on this row — skipped, not an error. */
    BLANK(false);

    private final boolean applicable;

    StudentNumberImportOutcome(boolean applicable) {
        this.applicable = applicable;
    }

    /** True when this row results in a write. */
    public boolean applicable() {
        return applicable;
    }
}
