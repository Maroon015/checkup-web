package com.checkup.patient;

/** A clinical note that can't be saved (empty or too long). Message is user-facing. */
public class InvalidNoteException extends RuntimeException {

    public InvalidNoteException(String message) {
        super(message);
    }
}
