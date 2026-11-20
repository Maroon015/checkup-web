package com.checkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.checkup.appointment.Appointment;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.clinic.Clinic;
import com.checkup.clinic.ClinicRepository;
import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;
import com.checkup.patient.PhoneNumber;
import com.checkup.patient.PhoneType;

import jakarta.persistence.EntityManager;

/**
 * @DataJpaTest starts only the database layer: H2, Flyway (so the real V1 schema is used),
 * Hibernate and the repositories. Each test runs in a transaction that is rolled back
 * afterwards, so tests can't affect each other. The demo data loader does NOT run here.
 */
@DataJpaTest
class RepositoryTest {

    @Autowired ClinicRepository clinics;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired EntityManager em;

    Clinic london;
    Doctor doctor;

    @BeforeEach
    void setUp() {
        london = clinics.save(new Clinic("Test Clinic", "1 Main St", "London"));
        clinics.save(new Clinic("Other Clinic", "2 Bay St", "Toronto"));
        doctor = doctors.save(new Doctor("Test", "Doctor", "Family Medicine",
                "test.doctor@example.com", "5195550000", london));
    }

    @Test
    void findsClinicsByCityIgnoringCase() {
        assertThat(clinics.findByCityIgnoreCaseOrderByName("lONDon"))
                .extracting(Clinic::getName)
                .containsExactly("Test Clinic");
    }

    @Test
    void savingPatientAlsoSavesPhones() {
        Patient p = new Patient("Ada", "Lovelace", "ada@example.com", LocalDate.of(2000, 1, 1));
        p.addPhone(new PhoneNumber(PhoneType.PERSONAL, "5195551111"));
        p.addPhone(new PhoneNumber(PhoneType.EMERGENCY, "5195552222"));
        Long id = patients.save(p).getId();

        // Push everything to the database and forget the cached objects,
        // so the next find really reads from the tables.
        em.flush();
        em.clear();

        Patient reloaded = patients.findById(id).orElseThrow();
        assertThat(reloaded.getPhones()).hasSize(2);
        assertThat(reloaded.getPhones(PhoneType.EMERGENCY))
                .extracting(PhoneNumber::getNumber)
                .containsExactly("5195552222");
    }

    @Test
    void twoPatientsCannotShareAnEmailEvenWithDifferentCase() {
        // The old app keyed patients by name, so duplicates silently clashed.
        // Now the database itself refuses a duplicate email (UNIQUE in V1), and emails are
        // lowercased first, so a different capitalisation doesn't sneak past the rule.
        patients.save(new Patient("Alex", "Morgan", "same@example.com", LocalDate.of(1990, 1, 1)));

        // IDENTITY ids mean save() INSERTs straight away, so the error comes from save().
        Patient duplicate = new Patient("Alex", "Morgan", "SAME@Example.com", LocalDate.of(1985, 5, 5));
        assertThatThrownBy(() -> patients.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsDoctorScheduleInTimeRange() {
        Patient p = patients.save(new Patient("Sam", "Lee", "sam@example.com", LocalDate.of(1980, 2, 2)));
        LocalDateTime nineAm = LocalDateTime.of(2030, 3, 4, 9, 0);
        appointments.save(new Appointment(p, doctor, nineAm, nineAm.plusMinutes(30), "Check-up"));
        appointments.save(new Appointment(p, doctor, nineAm.plusDays(7), nineAm.plusDays(7).plusMinutes(30), "Follow-up"));

        var thatDay = appointments.findByDoctor_IdAndStartTimeBetweenOrderByStartTime(
                doctor.getId(), nineAm.toLocalDate().atStartOfDay(), nineAm.toLocalDate().atTime(23, 59));

        assertThat(thatDay).extracting(Appointment::getReason).containsExactly("Check-up");
    }

    @Test
    void invalidEntitiesAreRejectedBeforeReachingTheDatabase() {
        // Hibernate runs the @NotNull/@Pattern rules automatically on save().
        Doctor noClinic = new Doctor("No", "Clinic", "Surgery", "no.clinic@example.com", "5195550001", null);
        assertThatThrownBy(() -> doctors.save(noClinic))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class)
                .hasMessageContaining("clinic");
    }
}
