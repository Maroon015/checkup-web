package com.checkup.patient;

/**
 * Old version: phone strings were prefixed with "Personal: " or "Emergency: " and split
 * apart with substring(indexOf(":") + 2). Now the type is its own field.
 */
public enum PhoneType {
    PERSONAL,
    EMERGENCY
}
