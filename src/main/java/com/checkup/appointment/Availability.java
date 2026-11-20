package com.checkup.appointment;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

import com.checkup.doctor.Doctor;

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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * One block of a doctor's weekly hours, e.g. "Monday 09:00-12:00, 30-minute slots".
 * Free time slots are worked out from these blocks (see SchedulingService).
 *
 * Old version: a patient typed a weekday ("Monday") and that string WAS the appointment.
 * Doctors had no hours at all.
 */
@Entity
@Table(name = "doctor_availability")
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    // java.time.DayOfWeek is a built-in enum (MONDAY..SUNDAY), stored by name.
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    @NotNull
    @Column(name = "start_time")
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time")
    private LocalTime endTime;

    @Min(5)
    @Max(240)
    @Column(name = "slot_minutes")
    private int slotMinutes;

    protected Availability() {
    }

    public Availability(Doctor doctor, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, int slotMinutes) {
        if (!endTime.isAfter(startTime)) {
            throw new AppointmentRuleException("The end time must be after the start time.");
        }
        if (slotMinutes < 5 || slotMinutes > 240) {
            throw new AppointmentRuleException("Appointments must be between 5 and 240 minutes long.");
        }
        if (Duration.between(startTime, endTime).toMinutes() < slotMinutes) {
            throw new AppointmentRuleException("The block is shorter than one appointment.");
        }
        this.doctor = doctor;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotMinutes = slotMinutes;
    }

    /** True if this block and the other share any time on the same day. */
    public boolean overlaps(DayOfWeek day, LocalTime otherStart, LocalTime otherEnd) {
        return dayOfWeek == day && startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }

    public Long getId() { return id; }
    public Doctor getDoctor() { return doctor; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getSlotMinutes() { return slotMinutes; }
}
