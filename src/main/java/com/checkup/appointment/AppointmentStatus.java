package com.checkup.appointment;

/**
 * The life of an appointment:
 *
 *   REQUESTED --(doctor confirms)--> CONFIRMED --(visit happens)--> COMPLETED
 *       \                                \
 *        `---------(cancelled)------------`--> CANCELLED
 *
 * The rules for moving between states live in Appointment (confirm, complete, cancel...).
 */
public enum AppointmentStatus {
    REQUESTED,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
