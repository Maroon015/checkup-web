package com.checkup.appointment;

import java.time.Duration;
import java.time.LocalDateTime;

import com.checkup.doctor.Doctor;
import com.checkup.patient.Patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A booked visit: one patient, one doctor, a start and end time.
 *
 * Old version: makeAppointment() wrote "Patient / Purpose / Availability: Monday" to
 * Appointments.txt WITHOUT append mode, so every booking replaced the previous one and
 * only one appointment could ever exist. There was no doctor and no actual time.
 *
 * The clinic isn't stored here: it's appointment.getDoctor().getClinic(). Storing it
 * twice would let the two copies disagree.
 *
 * Status can only change through the methods below (confirm, complete, cancel...), never
 * by setting it directly, so the rules can't be skipped:
 *
 *   REQUESTED --confirm()--> CONFIRMED --complete()--> COMPLETED
 *       \                       \
 *        `---- cancel...() -----`--> CANCELLED
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    /** Patients can cancel online only this far ahead; after that they must call. */
    public static final Duration PATIENT_CANCELLATION_NOTICE = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @NotNull
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @NotNull
    @Column(name = "end_time")
    private LocalDateTime endTime;

    // Copy of startTime while the appointment is active, null once cancelled.
    // The database's UNIQUE(doctor_id, booked_start) uses it to block double-booking
    // (see V3__scheduling.sql for why it's a separate column).
    @Column(name = "booked_start")
    private LocalDateTime bookedStart;

    // Was "patientPurpose" in the old code.
    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Appointment() {
    }

    public Appointment(Patient patient, Doctor doctor, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.patient = patient;
        this.doctor = doctor;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bookedStart = startTime;
        this.reason = reason;
    }

    // ------------------------------------------------------------------ status rules

    /** Still going to happen (not cancelled or completed). */
    public boolean isActive() {
        return status == AppointmentStatus.REQUESTED || status == AppointmentStatus.CONFIRMED;
    }

    /** Doctor accepts a requested appointment. */
    public void confirm() {
        if (status != AppointmentStatus.REQUESTED) {
            throw new AppointmentRuleException("Only requested appointments can be confirmed.");
        }
        status = AppointmentStatus.CONFIRMED;
    }

    public boolean canBeCompleted(LocalDateTime now) {
        return status == AppointmentStatus.CONFIRMED && !now.isBefore(startTime);
    }

    /** Doctor marks a confirmed appointment as done, once it has started. */
    public void complete(LocalDateTime now) {
        if (status != AppointmentStatus.CONFIRMED) {
            throw new AppointmentRuleException("Only confirmed appointments can be marked as completed.");
        }
        if (now.isBefore(startTime)) {
            throw new AppointmentRuleException("An appointment can't be completed before it starts.");
        }
        status = AppointmentStatus.COMPLETED;
    }

    public boolean canBeCancelledByPatient(LocalDateTime now) {
        return isActive() && !now.plus(PATIENT_CANCELLATION_NOTICE).isAfter(startTime);
    }

    /** Patient cancels: allowed only at least 24 hours ahead. */
    public void cancelByPatient(LocalDateTime now) {
        if (!isActive()) {
            throw new AppointmentRuleException("This appointment can't be cancelled any more.");
        }
        if (now.plus(PATIENT_CANCELLATION_NOTICE).isAfter(startTime)) {
            throw new AppointmentRuleException(
                    "Appointments can only be cancelled online at least 24 hours ahead. Please call the clinic.");
        }
        cancel();
    }

    public boolean canBeCancelledByDoctor(LocalDateTime now) {
        return isActive() && now.isBefore(startTime);
    }

    /** Doctor cancels: allowed any time before the appointment starts. */
    public void cancelByDoctor(LocalDateTime now) {
        if (!isActive()) {
            throw new AppointmentRuleException("This appointment can't be cancelled any more.");
        }
        if (!now.isBefore(startTime)) {
            throw new AppointmentRuleException("This appointment has already started.");
        }
        cancel();
    }

    private void cancel() {
        status = AppointmentStatus.CANCELLED;
        bookedStart = null; // frees the slot for someone else (see V3)
    }

    // ------------------------------------------------------------------ getters

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getBookedStart() { return bookedStart; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setReason(String reason) { this.reason = reason; }
}
