package com.checkup.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.checkup.account.Role;
import com.checkup.account.UserAccount;
import com.checkup.account.UserAccountRepository;
import com.checkup.clinic.Clinic;
import com.checkup.clinic.ClinicRepository;
import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;

/**
 * The race condition, for real: two patients on two threads book the same slot at the
 * same moment, each in its own transaction (no test transaction wrapping them, so the
 * database sees two genuinely separate bookings). Exactly one must win.
 *
 * Repeated a few times, because a race that passes once could just be lucky timing.
 */
@SpringBootTest(properties = "checkup.demo-data.enabled=false")
class ConcurrentBookingTest {

    @Autowired SchedulingService scheduling;
    @Autowired AppointmentRepository appointments;
    @Autowired AvailabilityRepository availability;
    @Autowired ClinicRepository clinics;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    @Autowired UserAccountRepository accounts;

    Doctor doctor;
    UserAccount aliceLogin;
    UserAccount bobLogin;
    LocalDateTime slot;

    @BeforeEach
    void setUp() {
        Clinic clinic = clinics.save(new Clinic("Race Clinic", "1 Main St", "London"));
        doctor = doctors.save(new Doctor("Race", "Doctor", "Family Medicine", "race.doc@example.com", "5195550000", clinic));
        aliceLogin = accounts.save(new UserAccount("race.alice@example.com", "{noop}x", Role.PATIENT));
        bobLogin = accounts.save(new UserAccount("race.bob@example.com", "{noop}x", Role.PATIENT));
        Patient alice = new Patient("Alice", "Race", "race.alice@example.com", LocalDate.of(1990, 1, 1));
        alice.setUser(aliceLogin);
        patients.save(alice);
        Patient bob = new Patient("Bob", "Race", "race.bob@example.com", LocalDate.of(1990, 1, 1));
        bob.setUser(bobLogin);
        patients.save(bob);

        availability.save(new Availability(doctor, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), 30));
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        slot = nextMonday.atTime(9, 0);
    }

    @AfterEach
    void cleanUp() {
        appointments.deleteAll();
        availability.deleteAll();
        patients.deleteAll();
        doctors.deleteAll();
        clinics.deleteAll();
        accounts.deleteAll();
    }

    @RepeatedTest(5)
    void twoPatientsBookingTheSameSlotAtOnceOnlyOneWins() throws Exception {
        CountDownLatch startTogether = new CountDownLatch(1);
        Callable<String> alice = () -> attempt(startTogether, aliceLogin);
        Callable<String> bob = () -> attempt(startTogether, bobLogin);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> results = new ArrayList<>();
            results.add(pool.submit(alice));
            results.add(pool.submit(bob));
            startTogether.countDown(); // release both threads at the same instant

            List<String> outcomes = new ArrayList<>();
            for (Future<String> r : results) {
                outcomes.add(r.get(20, TimeUnit.SECONDS));
            }

            assertThat(outcomes).containsExactlyInAnyOrder("booked", "slot taken");
            assertThat(appointments.findActiveForDoctorOverlapping(doctor.getId(), slot, slot.plusMinutes(30)))
                    .hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private String attempt(CountDownLatch startTogether, UserAccount who) throws InterruptedException {
        startTogether.await();
        try {
            scheduling.book(who.getId(), doctor.getId(), slot, "Race");
            return "booked";
        } catch (SlotUnavailableException e) {
            return "slot taken";
        }
    }
}
