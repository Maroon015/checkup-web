package com.checkup;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.account.AccountService;
import com.checkup.account.DemoAccounts;
import com.checkup.appointment.Appointment;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.appointment.Availability;
import com.checkup.appointment.AvailabilityRepository;
import com.checkup.clinic.Clinic;
import com.checkup.clinic.ClinicRepository;
import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;
import com.checkup.patient.BloodType;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;
import com.checkup.patient.PhoneNumber;
import com.checkup.patient.PhoneType;

/**
 * Fills an empty database with made-up demo data when the app starts, so there's always
 * something to click through (and, later, the "log in as demo patient/doctor" buttons).
 *
 * Why Java and not a Flyway SQL file? Appointment times are relative to today
 * ("in 2 days at 9:00"), so the demo never shows only past appointments, and in step 3
 * the demo passwords need to be hashed with BCrypt, which is easy in Java.
 *
 * Turn it off with checkup.demo-data.enabled=false. All names, emails and numbers are fake
 * (555 numbers and example.com emails are reserved for exactly this).
 */
@Component
@ConditionalOnProperty(name = "checkup.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final ClinicRepository clinics;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final AvailabilityRepository availability;
    private final AccountService accountService;

    // Constructor injection: Spring sees these parameters and passes in the repositories.
    public DemoDataLoader(ClinicRepository clinics, DoctorRepository doctors,
                          PatientRepository patients, AppointmentRepository appointments,
                          AvailabilityRepository availability, AccountService accountService) {
        this.clinics = clinics;
        this.doctors = doctors;
        this.patients = patients;
        this.appointments = appointments;
        this.availability = availability;
        this.accountService = accountService;
    }

    @Override
    @Transactional // all of this succeeds together, or none of it is saved
    public void run(ApplicationArguments args) {
        if (clinics.count() > 0) {
            return; // already has data (e.g. a real database later); don't touch it
        }

        Clinic northside = clinics.save(new Clinic("Northside Family Clinic", "120 Maple Ave", "London"));
        Clinic riverside = clinics.save(new Clinic("Riverside Health Centre", "45 King St", "London"));
        Clinic lakeshore = clinics.save(new Clinic("Lakeshore Medical", "8 Harbour Rd", "Toronto"));

        Doctor patel = doctors.save(new Doctor("Jordan", "Patel", "Family Medicine",
                DemoAccounts.DOCTOR_EMAIL, "5195550199", northside));
        Doctor chen = doctors.save(new Doctor("Mei", "Chen", "Pediatrics",
                "mei.chen@example.com", "5195550188", northside));
        Doctor okafor = doctors.save(new Doctor("Daniel", "Okafor", "Cardiology",
                "daniel.okafor@example.com", "5195550177", riverside));
        Doctor rossi = doctors.save(new Doctor("Sofia", "Rossi", "Dermatology",
                "sofia.rossi@example.com", "4165550166", lakeshore));

        Patient alex = new Patient("Alex", "Morgan", DemoAccounts.PATIENT_EMAIL, LocalDate.of(1991, 4, 12));
        alex.setCity("London");
        alex.setStreet("Maple Ave");
        alex.setUnitNumber("12");
        alex.setBloodType(BloodType.O_POSITIVE);
        alex.setHeightCm(172.0);
        alex.setWeightKg(68.0);
        alex.setMedicalHistory("Seasonal allergies.");
        alex.addPhone(new PhoneNumber(PhoneType.PERSONAL, "5195550101"));
        patients.save(alex); // also saves the phone (cascade)

        Patient sam = new Patient("Sam", "Lee", "sam.lee@example.com", LocalDate.of(1974, 9, 30));
        sam.setCity("London");
        sam.setStreet("Oak St");
        sam.setUnitNumber("3B");
        sam.setBloodType(BloodType.B_NEGATIVE);
        sam.setHeightCm(180.0);
        sam.setWeightKg(82.0);
        sam.setMedicalHistory("Knee surgery in 2019.");
        sam.addPhone(new PhoneNumber(PhoneType.PERSONAL, "5195550102"));
        sam.addPhone(new PhoneNumber(PhoneType.EMERGENCY, "5195550103"));
        patients.save(sam);

        // Logins for the "Log in as demo ..." buttons. AccountService hashes the password.
        accountService.createPatientLogin(alex, DemoAccounts.PASSWORD);
        accountService.createPatientLogin(sam, DemoAccounts.PASSWORD);
        accountService.createDoctorLogin(patel, DemoAccounts.PASSWORD);
        accountService.createAdmin(DemoAccounts.ADMIN_EMAIL, DemoAccounts.PASSWORD);

        // Weekly hours (step 4). Free slots are worked out from these.
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                     DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            availability.save(new Availability(patel, day, LocalTime.of(9, 0), LocalTime.of(12, 0), 30));
            availability.save(new Availability(patel, day, LocalTime.of(13, 0), LocalTime.of(16, 0), 30));
        }
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)) {
            availability.save(new Availability(chen, day, LocalTime.of(9, 0), LocalTime.of(15, 0), 20));
        }
        for (DayOfWeek day : List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)) {
            availability.save(new Availability(okafor, day, LocalTime.of(10, 0), LocalTime.of(16, 0), 45));
        }
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)) {
            availability.save(new Availability(rossi, day, LocalTime.of(9, 0), LocalTime.of(17, 0), 30));
        }

        // Status changes go through the rule methods (confirm, complete), never setStatus.
        LocalDate today = LocalDate.now();
        Appointment checkup = new Appointment(alex, patel,
                at(today.plusDays(2), 9, 0), at(today.plusDays(2), 9, 30), "Annual check-up");
        checkup.confirm();
        appointments.save(checkup);

        appointments.save(new Appointment(sam, okafor,
                at(today.plusDays(3), 14, 0), at(today.plusDays(3), 14, 45), "Follow-up on chest pain"));

        appointments.save(new Appointment(sam, patel,
                at(today.plusDays(4), 10, 0), at(today.plusDays(4), 10, 30), "Knee pain follow-up"));

        Appointment past = new Appointment(sam, patel,
                at(today.minusDays(10), 10, 0), at(today.minusDays(10), 10, 30), "Knee pain");
        past.confirm();
        past.complete(LocalDateTime.now());
        appointments.save(past);

        log.info("Loaded demo data: {} clinics, {} doctors, {} patients, {} appointments",
                clinics.count(), doctors.count(), patients.count(), appointments.count());
    }

    private static LocalDateTime at(LocalDate day, int hour, int minute) {
        return LocalDateTime.of(day, LocalTime.of(hour, minute));
    }
}
