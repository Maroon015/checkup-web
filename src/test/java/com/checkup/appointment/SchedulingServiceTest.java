package com.checkup.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

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
 * SchedulingService against the real database schema (Flyway V1-V3), with the clock
 * frozen at Monday 4 March 2030, 08:00, so "today", "past" and "24 hours ahead" are exact.
 */
@DataJpaTest
@Import({SchedulingService.class, SchedulingServiceTest.FrozenClock.class})
class SchedulingServiceTest {

    static final LocalDate MONDAY = LocalDate.of(2030, 3, 4);
    static final LocalDate NEXT_MONDAY = MONDAY.plusWeeks(1);

    @TestConfiguration
    static class FrozenClock {
        @Bean
        Clock clock() {
            ZoneId zone = ZoneId.systemDefault();
            return Clock.fixed(MONDAY.atTime(8, 0).atZone(zone).toInstant(), zone);
        }
    }

    @Autowired SchedulingService scheduling;
    @Autowired AppointmentRepository appointments;
    @Autowired AvailabilityRepository availability;
    @Autowired ClinicRepository clinics;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    @Autowired UserAccountRepository accounts;

    Doctor doctor;
    Doctor otherDoctor;
    UserAccount doctorLogin;
    UserAccount otherDoctorLogin;
    UserAccount aliceLogin;
    UserAccount bobLogin;

    @BeforeEach
    void setUp() {
        Clinic clinic = clinics.save(new Clinic("Test Clinic", "1 Main St", "London"));
        doctorLogin = accounts.save(new UserAccount("doc@example.com", "{noop}x", Role.DOCTOR));
        otherDoctorLogin = accounts.save(new UserAccount("doc2@example.com", "{noop}x", Role.DOCTOR));
        doctor = newDoctor("doc@example.com", clinic, doctorLogin);
        otherDoctor = newDoctor("doc2@example.com", clinic, otherDoctorLogin);

        aliceLogin = accounts.save(new UserAccount("alice@example.com", "{noop}x", Role.PATIENT));
        bobLogin = accounts.save(new UserAccount("bob@example.com", "{noop}x", Role.PATIENT));
        newPatient("alice@example.com", aliceLogin);
        newPatient("bob@example.com", bobLogin);

        // Mondays 09:00-11:00 in 30-minute slots, for both doctors.
        availability.save(new Availability(doctor, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 30));
        availability.save(new Availability(otherDoctor, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), 30));
    }

    private Doctor newDoctor(String email, Clinic clinic, UserAccount login) {
        Doctor d = new Doctor("Test", email.substring(0, 4), "Family Medicine", email, "5195550000", clinic);
        d.setUser(login);
        return doctors.save(d);
    }

    private void newPatient(String email, UserAccount login) {
        Patient p = new Patient("Pat", email.substring(0, 3), email, LocalDate.of(1990, 1, 1));
        p.setUser(login);
        patients.save(p);
    }

    private static LocalDateTime at(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute);
    }

    // ------------------------------------------------------------------ free slots

    @Test
    void freeSlotsComeFromTheWeeklyHours() {
        assertThat(scheduling.freeSlots(doctor.getId(), MONDAY))
                .extracting(TimeSlot::start)
                .containsExactly(at(MONDAY, 9, 0), at(MONDAY, 9, 30), at(MONDAY, 10, 0), at(MONDAY, 10, 30));
        assertThat(scheduling.freeSlots(doctor.getId(), MONDAY.plusDays(1))).isEmpty(); // no Tuesday hours
    }

    @Test
    void noSlotsInThePastOrBeyondTheBookingWindow() {
        assertThat(scheduling.freeSlots(doctor.getId(), MONDAY.minusWeeks(1))).isEmpty();
        assertThat(scheduling.freeSlots(doctor.getId(), MONDAY.plusWeeks(5))).isEmpty(); // 35 days > 30
    }

    @Test
    void bookedSlotIsNoLongerFree() {
        scheduling.book(aliceLogin.getId(), doctor.getId(), at(MONDAY, 9, 0), "Check-up");

        assertThat(scheduling.freeSlots(doctor.getId(), MONDAY))
                .extracting(TimeSlot::start)
                .doesNotContain(at(MONDAY, 9, 0))
                .hasSize(3);
    }

    // ------------------------------------------------------------------ booking rules

    @Test
    void sameSlotCannotBeBookedTwice() {
        scheduling.book(aliceLogin.getId(), doctor.getId(), at(MONDAY, 9, 0), "Check-up");

        assertThatThrownBy(() -> scheduling.book(bobLogin.getId(), doctor.getId(), at(MONDAY, 9, 0), "Flu"))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void patientCannotBeInTwoPlacesAtOnce() {
        scheduling.book(aliceLogin.getId(), doctor.getId(), at(MONDAY, 9, 0), "Check-up");

        assertThatThrownBy(() -> scheduling.book(aliceLogin.getId(), otherDoctor.getId(), at(MONDAY, 9, 0), "Second opinion"))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("already have an appointment");
    }

    @Test
    void timesOutsideTheDoctorsHoursCannotBeBooked() {
        assertThatThrownBy(() -> scheduling.book(aliceLogin.getId(), doctor.getId(), at(MONDAY, 12, 0), "Lunch"))
                .isInstanceOf(SlotUnavailableException.class);
        assertThatThrownBy(() -> scheduling.book(aliceLogin.getId(), doctor.getId(), at(MONDAY, 9, 15), "Off-grid"))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    void aReasonIsRequired() {
        assertThatThrownBy(() -> scheduling.book(aliceLogin.getId(), doctor.getId(), at(MONDAY, 9, 0), "   "))
                .isInstanceOf(AppointmentRuleException.class);
    }

    @Test
    void theDatabaseItselfBlocksDoubleBooking() {
        // Skip every Java check and insert two appointments for the same doctor and time
        // straight through the repository: exactly what two racing requests would do.
        Patient alice = patients.findByUser_Id(aliceLogin.getId()).orElseThrow();
        Patient bob = patients.findByUser_Id(bobLogin.getId()).orElseThrow();
        LocalDateTime nine = at(MONDAY, 9, 0);
        appointments.saveAndFlush(new Appointment(alice, doctor, nine, nine.plusMinutes(30), "First"));

        assertThatThrownBy(() -> appointments.saveAndFlush(new Appointment(bob, doctor, nine, nine.plusMinutes(30), "Second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------ cancelling

    @Test
    void cancelledSlotCanBeBookedAgain() {
        LocalDateTime nextWeekNine = at(NEXT_MONDAY, 9, 0);
        Appointment first = scheduling.book(aliceLogin.getId(), doctor.getId(), nextWeekNine, "Check-up");

        scheduling.cancelByPatient(aliceLogin.getId(), first.getId());
        appointments.flush();

        // booked_start is now NULL, so the UNIQUE rule lets Bob have the same slot.
        Appointment second = scheduling.book(bobLogin.getId(), doctor.getId(), nextWeekNine, "Flu");
        assertThat(second.getId()).isNotEqualTo(first.getId());
    }

    @Test
    void patientCannotCancelOnTheSameDay() {
        Appointment a = scheduling.book(aliceLogin.getId(), doctor.getId(), at(MONDAY, 9, 0), "Check-up");

        assertThatThrownBy(() -> scheduling.cancelByPatient(aliceLogin.getId(), a.getId()))
                .isInstanceOf(AppointmentRuleException.class)
                .hasMessageContaining("24 hours");
    }

    // ------------------------------------------------------------------ ownership

    @Test
    void patientsCannotTouchSomeoneElsesAppointment() {
        Appointment alices = scheduling.book(aliceLogin.getId(), doctor.getId(), at(NEXT_MONDAY, 9, 0), "Check-up");

        assertThatThrownBy(() -> scheduling.cancelByPatient(bobLogin.getId(), alices.getId()))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void doctorsCanOnlyActOnTheirOwnAppointments() {
        Appointment a = scheduling.book(aliceLogin.getId(), doctor.getId(), at(NEXT_MONDAY, 9, 0), "Check-up");

        assertThatThrownBy(() -> scheduling.confirm(otherDoctorLogin.getId(), a.getId()))
                .isInstanceOf(AppointmentNotFoundException.class);

        scheduling.confirm(doctorLogin.getId(), a.getId());
        assertThat(appointments.findById(a.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    // ------------------------------------------------------------------ weekly hours

    @Test
    void overlappingHoursAreRejected() {
        assertThatThrownBy(() -> scheduling.addHours(doctorLogin.getId(), DayOfWeek.MONDAY,
                        LocalTime.of(10, 30), LocalTime.of(12, 0), 30))
                .isInstanceOf(AppointmentRuleException.class)
                .hasMessageContaining("overlap");

        // Right after the existing block is fine.
        scheduling.addHours(doctorLogin.getId(), DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0), 30);
        assertThat(scheduling.freeSlots(doctor.getId(), MONDAY)).hasSize(6);
    }

    @Test
    void doctorsCannotRemoveSomeoneElsesHours() {
        Availability block = availability.findByDoctor_Id(doctor.getId()).getFirst();
        assertThatThrownBy(() -> scheduling.removeHours(otherDoctorLogin.getId(), block.getId()))
                .isInstanceOf(AppointmentNotFoundException.class);
    }
}
