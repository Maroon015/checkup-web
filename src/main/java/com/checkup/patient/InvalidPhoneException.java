package com.checkup.patient;

/** A phone number that can't be added (wrong length, duplicate, too many). Message is user-facing. */
public class InvalidPhoneException extends RuntimeException {

    public InvalidPhoneException(String message) {
        super(message);
    }
}
