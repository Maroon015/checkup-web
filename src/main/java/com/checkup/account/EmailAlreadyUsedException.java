package com.checkup.account;

/** Thrown when someone signs up with an email that already has an account. */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("An account already exists for " + email);
    }
}
