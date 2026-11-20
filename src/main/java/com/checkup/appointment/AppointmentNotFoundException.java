package com.checkup.appointment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * "Not found" for an appointment that doesn't exist OR isn't yours. Both give the same
 * 404 on purpose: answering "403 Forbidden" for someone else's appointment would confirm
 * that appointment id exists, which leaks information.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException() {
        super("Appointment not found");
    }
}
