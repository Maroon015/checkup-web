package com.checkup.account;

/**
 * What a logged-in user is allowed to do. Each part of the site is locked to one role
 * in SecurityConfig: /patient/** needs PATIENT, /doctor/** needs DOCTOR, /admin/** needs ADMIN.
 *
 * Old version: the main menu let anyone pick "Doctor" and the only check was a password
 * read from a text file. Job applications were "protected" by a random number printed
 * on screen.
 */
public enum Role {
    PATIENT,
    DOCTOR,
    ADMIN
}
