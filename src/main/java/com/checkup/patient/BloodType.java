package com.checkup.patient;

/**
 * The 8 blood types.
 *
 * Old version: a String checked with eight equalsIgnoreCase() calls, and an invalid value
 * silently became "A+" (a dangerous default for medical data!). An enum makes invalid
 * values impossible: the compiler and the database only accept these 8.
 */
public enum BloodType {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String label;

    BloodType(String label) {
        this.label = label;
    }

    /** What to show on screen, e.g. "AB-". */
    public String getLabel() {
        return label;
    }
}
