package com.checkup.audit;

/** The kinds of record access that get logged, with how they read on screen. */
public enum AuditAction {
    VIEW_RECORD("Viewed record"),
    UPDATE_RECORD("Updated record"),
    ADD_PHONE("Added phone number"),
    REMOVE_PHONE("Removed phone number"),
    ADD_NOTE("Added clinical note");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
