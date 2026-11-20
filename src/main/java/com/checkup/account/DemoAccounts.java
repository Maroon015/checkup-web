package com.checkup.account;

/**
 * The demo logins, shared by DemoDataLoader (creates them) and the login page (its
 * "Log in as demo ..." buttons). They are fake accounts with fake data, published on
 * purpose so recruiters can click straight in. Never reuse this pattern for real accounts.
 */
public final class DemoAccounts {

    public static final String PATIENT_EMAIL = "alex.morgan@example.com";
    public static final String DOCTOR_EMAIL = "jordan.patel@example.com";
    public static final String ADMIN_EMAIL = "admin@example.com";
    public static final String PASSWORD = "demo-password";

    private DemoAccounts() {
    }
}
