package com.checkup.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for the status rules in Appointment. No Spring, no database: the rules
 * are ordinary Java, so they can be tested in milliseconds.
 */
class AppointmentRulesTest {

    static final LocalDateTime START = LocalDateTime.of(2030, 3, 4, 9, 0);

    static Appointment requested() {
        return new Appointment(null, null, START, START.plusMinutes(30), "Check-up");
    }

    @Test
    void newAppointmentIsRequestedAndHoldsItsSlot() {
        Appointment a = requested();
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(a.getBookedStart()).isEqualTo(START);
        assertThat(a.isActive()).isTrue();
    }

    @Test
    void happyPathRequestedConfirmedCompleted() {
        Appointment a = requested();
        a.confirm();
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        a.complete(START.plusMinutes(5));
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(a.isActive()).isFalse();
    }

    @Test
    void cannotConfirmTwiceOrCompleteWithoutConfirming() {
        Appointment a = requested();
        assertThatThrownBy(() -> a.complete(START.plusHours(1))).isInstanceOf(AppointmentRuleException.class);
        a.confirm();
        assertThatThrownBy(a::confirm).isInstanceOf(AppointmentRuleException.class);
    }

    @Test
    void cannotCompleteBeforeItStarts() {
        Appointment a = requested();
        a.confirm();
        assertThat(a.canBeCompleted(START.minusMinutes(1))).isFalse();
        assertThatThrownBy(() -> a.complete(START.minusMinutes(1))).isInstanceOf(AppointmentRuleException.class);
    }

    @Test
    void patientCanCancelAtLeast24HoursAheadAndThatFreesTheSlot() {
        Appointment a = requested();
        LocalDateTime exactly24hBefore = START.minusHours(24);
        assertThat(a.canBeCancelledByPatient(exactly24hBefore)).isTrue();

        a.cancelByPatient(exactly24hBefore);

        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(a.getBookedStart()).isNull(); // the UNIQUE rule no longer counts it
    }

    @Test
    void patientCannotCancelWithin24Hours() {
        Appointment a = requested();
        LocalDateTime oneMinuteTooLate = START.minusHours(24).plusMinutes(1);
        assertThat(a.canBeCancelledByPatient(oneMinuteTooLate)).isFalse();
        assertThatThrownBy(() -> a.cancelByPatient(oneMinuteTooLate))
                .isInstanceOf(AppointmentRuleException.class)
                .hasMessageContaining("24 hours");
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
    }

    @Test
    void doctorCanCancelUntilTheAppointmentStarts() {
        Appointment a = requested();
        assertThat(a.canBeCancelledByDoctor(START.minusMinutes(1))).isTrue();
        assertThat(a.canBeCancelledByDoctor(START)).isFalse();
        a.cancelByDoctor(START.minusMinutes(1));
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void finishedAppointmentsCannotBeCancelled() {
        Appointment a = requested();
        a.confirm();
        a.complete(START);
        assertThatThrownBy(() -> a.cancelByDoctor(START.minusDays(1))).isInstanceOf(AppointmentRuleException.class);
        assertThatThrownBy(() -> a.cancelByPatient(START.minusDays(2))).isInstanceOf(AppointmentRuleException.class);
    }
}
