package com.checkup.careers;

/** The jobs offered on the careers page (same three as the original app). */
public enum Position {
    SURGEON("Surgeon"),
    PHARMACIST("Pharmacist"),
    DOCTORS_ASSISTANT("Doctor's assistant");

    private final String label;

    Position(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
