package com.checkup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.appointment.Appointment;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.appointment.AppointmentStatus;
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

    // Constructor injection: Spring sees these parameters and passes in the repositories.
    public DemoDataLoader(ClinicRepository clinics, DoctorRepository doctors,
                          PatientRepository patients, AppointmentRepository appointments) {
        this.clinics = clinics;
        this.doctors = doctors;
        this.patients = patients;
        this.appointments = appointments;
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
                "jordan.patel@example.com", "5195550199", northside));
        Doctor chen = doctors.save(new Doctor("Mei", "Chen", "Pediatrics",
                "mei.chen@example.com", "5195550188", northside));
        Doctor okafor = doctors.save(new Doctor("Daniel", "Okafor", "Cardiology",
                "daniel.okafor@example.com", "5195550177", riverside));
        doctors.save(new Doctor("Sofia", "Rossi", "Dermatology",
                "sofia.rossi@example.com", "4165550166", lakeshore));

        Patient alex = new Patient("Alex", "Morgan", "alex.morgan@example.com", LocalDate.of(1991, 4, 12));
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

        LocalDate today = LocalDate.now();
        Appointment checkup = new Appointment(alex, patel,
                at(today.plusDays(2), 9, 0), at(today.plusDays(2), 9, 30), "Annual check-up");
        checkup.setStatus(AppointmentStatus.CONFIRMED);
        appointments.save(checkup);

        appointments.save(new Appointment(sam, okafor,
                at(today.plusDays(3), 14, 0), at(today.plusDays(3), 14, 30), "Follow-up on chest pain"));

        Appointment past = new Appointment(sam, patel,
                at(today.minusDays(10), 10, 0), at(today.minusDays(10), 10, 30), "Knee pain");
        past.setStatus(AppointmentStatus.COMPLETED);
        appointments.save(past);

        log.info("Loaded demo data: {} clinics, {} doctors, {} patients, {} appointments",
                clinics.count(), doctors.count(), patients.count(), appointments.count());
    }

    private static LocalDateTime at(LocalDate day, int hour, int minute) {
        return LocalDateTime.of(day, LocalTime.of(hour, minute));
    }
}
