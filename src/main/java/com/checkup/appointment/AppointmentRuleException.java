package com.checkup.appointment;

/**
 * Thrown when an action breaks a scheduling rule (e.g. cancelling less than 24 hours
 * ahead, or completing an appointment that was never confirmed). The message is written
 * for the user, so pages can show it as-is.
 */
public class AppointmentRuleException extends RuntimeException {

    public AppointmentRuleException(String message) {
        super(message);
    }
}
