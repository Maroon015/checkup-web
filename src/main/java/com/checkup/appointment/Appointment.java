package com.checkup.appointment;

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
 */
@Entity
@Table(name = "appointments")
public class Appointment {

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
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setReason(String reason) { this.reason = reason; }
    // Status changes get proper rules (confirm(), cancel(), ...) in step 4.
    public void setStatus(AppointmentStatus status) { this.status = status; }
}
