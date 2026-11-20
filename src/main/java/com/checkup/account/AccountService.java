package com.checkup.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;

/**
 * Creates accounts. The ONLY place passwords get hashed, so no other code can accidentally
 * save a plain-text password.
 *
 * This is the "service layer": controllers handle web requests, repositories talk to the
 * database, and services hold the rules in between (here: unique email, hash the password,
 * link the account to a profile, all-or-nothing).
 */
@Service
public class AccountService {

    private final UserAccountRepository accounts;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserAccountRepository accounts, PatientRepository patients,
                          DoctorRepository doctors, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.patients = patients;
        this.doctors = doctors;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Sign-up: creates a PATIENT login and its patient profile together.
     * @Transactional: if saving the patient fails, the login isn't saved either.
     */
    @Transactional
    public Patient registerPatient(RegistrationForm form) {
        String email = form.getEmail();
        if (accounts.existsByEmailIgnoreCase(email) || patients.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        UserAccount account = accounts.save(
                new UserAccount(email, passwordEncoder.encode(form.getPassword()), Role.PATIENT));

        Patient patient = new Patient(form.getFirstName().trim(), form.getLastName().trim(),
                email, form.getDateOfBirth());
        patient.setUser(account);
        return patients.save(patient);
    }

    /** Gives an existing doctor profile a DOCTOR login (used by demo data; an admin page later). */
    @Transactional
    public UserAccount createDoctorLogin(Doctor doctor, String rawPassword) {
        if (accounts.existsByEmailIgnoreCase(doctor.getEmail())) {
            throw new EmailAlreadyUsedException(doctor.getEmail());
        }
        UserAccount account = accounts.save(
                new UserAccount(doctor.getEmail(), passwordEncoder.encode(rawPassword), Role.DOCTOR));
        doctor.setUser(account);
        doctors.save(doctor);
        return account;
    }

    /** Gives an existing patient profile a PATIENT login (used by demo data). */
    @Transactional
    public UserAccount createPatientLogin(Patient patient, String rawPassword) {
        UserAccount account = accounts.save(
                new UserAccount(patient.getEmail(), passwordEncoder.encode(rawPassword), Role.PATIENT));
        patient.setUser(account);
        patients.save(patient);
        return account;
    }

    @Transactional
    public UserAccount createAdmin(String email, String rawPassword) {
        return accounts.save(new UserAccount(email, passwordEncoder.encode(rawPassword), Role.ADMIN));
    }
}
