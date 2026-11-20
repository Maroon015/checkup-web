package com.checkup.appointment;

/** Thrown when a booking can't be made because the time is taken or not offered. */
public class SlotUnavailableException extends AppointmentRuleException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}
