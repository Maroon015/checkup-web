package com.checkup.appointment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A free time a patient can book, e.g. Mon 23 Nov 09:00-09:30.
 * A Java "record": a small immutable class where Java writes the constructor, getters,
 * equals() and toString() for you.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    /** The start as text for links and hidden form fields, e.g. "2026-11-23T09:00". */
    public String param() {
        return start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }
}
