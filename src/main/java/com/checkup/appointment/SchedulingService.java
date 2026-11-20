package com.checkup.appointment;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;

/**
 * All the scheduling rules in one place: which times are free, booking, confirming,
 * completing and cancelling, and doctors' weekly hours.
 *
 * Every method that acts on an appointment takes the logged-in user's account id and
 * checks the appointment belongs to them. If it doesn't, it's treated as not found (404).
 */
@Service
public class SchedulingService {

    /** How far ahead patients can book. */
    public static final int BOOKING_WINDOW_DAYS = 30;

    private final AvailabilityRepository availability;
    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final Clock clock;

    public SchedulingService(AvailabilityRepository availability, AppointmentRepository appointments,
                             DoctorRepository doctors, PatientRepository patients, Clock clock) {
        this.availability = availability;
        this.appointments = appointments;
        this.doctors = doctors;
        this.patients = patients;
        this.clock = clock;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ================================================================== free slots

    /**
     * The times a patient can book with this doctor on this date:
     *   1. take the doctor's hours for that weekday,
     *   2. cut them into slots (e.g. every 30 minutes),
     *   3. drop slots in the past,
     *   4. drop slots that overlap an active appointment.
     */
    @Transactional(readOnly = true)
    public List<TimeSlot> freeSlots(Long doctorId, LocalDate date) {
        LocalDateTime now = now();
        if (date.isBefore(now.toLocalDate()) || date.isAfter(now.toLocalDate().plusDays(BOOKING_WINDOW_DAYS))) {
            return List.of();
        }
        List<Appointment> taken = appointments.findActiveForDoctorOverlapping(
                doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        List<TimeSlot> free = new ArrayList<>();
        for (Availability block : availability.findByDoctor_IdAndDayOfWeek(doctorId, date.getDayOfWeek())) {
            LocalDateTime slotStart = date.atTime(block.getStartTime());
            LocalDateTime blockEnd = date.atTime(block.getEndTime());
            while (!slotStart.plusMinutes(block.getSlotMinutes()).isAfter(blockEnd)) {
                TimeSlot slot = new TimeSlot(slotStart, slotStart.plusMinutes(block.getSlotMinutes()));
                boolean inFuture = slot.start().isAfter(now);
                boolean clashes = taken.stream().anyMatch(a -> slot.overlaps(a.getStartTime(), a.getEndTime()));
                if (inFuture && !clashes) {
                    free.add(slot);
                }
                slotStart = slot.end();
            }
        }
        free.sort(Comparator.comparing(TimeSlot::start));
        return free;
    }

    // ================================================================== booking

    /**
     * Books a slot for the logged-in patient.
     *
     * Two layers of protection against double-booking:
     *   1. The checks below give a friendly message in the normal case.
     *   2. If two people book the same slot at the same instant, both can pass the checks
     *      (a race condition). The database's UNIQUE(doctor_id, booked_start) rule (V3)
     *      then rejects the second insert, and we turn that error into the same message.
     */
    @Transactional
    public Appointment book(Long patientUserId, Long doctorId, LocalDateTime start, String reason) {
        Patient patient = patients.findByUser_Id(patientUserId)
                .orElseThrow(() -> new IllegalStateException("No patient profile for account " + patientUserId));
        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow(() -> new SlotUnavailableException("That doctor doesn't exist."));
        if (reason == null || reason.isBlank()) {
            throw new AppointmentRuleException("Please tell the doctor what the appointment is for.");
        }

        TimeSlot slot = freeSlots(doctorId, start.toLocalDate()).stream()
                .filter(s -> s.start().equals(start))
                .findFirst()
                .orElseThrow(() -> new SlotUnavailableException(
                        "That time is no longer available. Please pick another one."));

        if (!appointments.findActiveForPatientOverlapping(patient.getId(), slot.start(), slot.end()).isEmpty()) {
            throw new SlotUnavailableException("You already have an appointment at that time.");
        }

        try {
            // saveAndFlush sends the INSERT now (not at the end of the transaction),
            // so a UNIQUE violation surfaces here, where we can catch it.
            return appointments.saveAndFlush(
                    new Appointment(patient, doctor, slot.start(), slot.end(), reason.trim()));
        } catch (DataIntegrityViolationException | ConcurrencyFailureException raceLost) {
            // DataIntegrityViolation: the UNIQUE rule fired. ConcurrencyFailure: the database
            // made us wait for the other booking's lock and gave up. Either way, we lost.
            throw new SlotUnavailableException("Someone else just booked that time. Please pick another one.");
        }
    }

    // ================================================================== patient actions

    @Transactional
    public void cancelByPatient(Long patientUserId, Long appointmentId) {
        Appointment a = appointments.findById(appointmentId)
                .filter(x -> belongsTo(x.getPatient().getUser(), patientUserId))
                .orElseThrow(AppointmentNotFoundException::new);
        a.cancelByPatient(now());
        // No save() needed: the object is "managed" inside this transaction, so JPA
        // notices the change and writes it when the transaction commits ("dirty checking").
    }

    // ================================================================== doctor actions

    @Transactional
    public void confirm(Long doctorUserId, Long appointmentId) {
        doctorsAppointment(doctorUserId, appointmentId).confirm();
    }

    @Transactional
    public void complete(Long doctorUserId, Long appointmentId) {
        doctorsAppointment(doctorUserId, appointmentId).complete(now());
    }

    @Transactional
    public void cancelByDoctor(Long doctorUserId, Long appointmentId) {
        doctorsAppointment(doctorUserId, appointmentId).cancelByDoctor(now());
    }

    private Appointment doctorsAppointment(Long doctorUserId, Long appointmentId) {
        return appointments.findById(appointmentId)
                .filter(x -> belongsTo(x.getDoctor().getUser(), doctorUserId))
                .orElseThrow(AppointmentNotFoundException::new);
    }

    private static boolean belongsTo(com.checkup.account.UserAccount owner, Long userId) {
        return owner != null && owner.getId().equals(userId);
    }

    // ================================================================== weekly hours

    /** The doctor's weekly hours, Monday first. */
    @Transactional(readOnly = true)
    public List<Availability> weeklyHours(Long doctorId) {
        return availability.findByDoctor_Id(doctorId).stream()
                .sorted(Comparator.comparing(Availability::getDayOfWeek).thenComparing(Availability::getStartTime))
                .toList();
    }

    /** Adds a block of hours for the logged-in doctor. Blocks on the same day can't overlap. */
    @Transactional
    public Availability addHours(Long doctorUserId, DayOfWeek day, LocalTime start, LocalTime end, int slotMinutes) {
        Doctor doctor = doctors.findByUser_Id(doctorUserId)
                .orElseThrow(() -> new IllegalStateException("No doctor profile for account " + doctorUserId));
        boolean overlaps = availability.findByDoctor_IdAndDayOfWeek(doctor.getId(), day).stream()
                .anyMatch(b -> b.overlaps(day, start, end));
        if (overlaps) {
            throw new AppointmentRuleException("Those hours overlap hours you already have on that day.");
        }
        return availability.save(new Availability(doctor, day, start, end, slotMinutes));
    }

    /** Removes one of the logged-in doctor's blocks. Already-booked appointments stay. */
    @Transactional
    public void removeHours(Long doctorUserId, Long availabilityId) {
        Availability block = availability.findById(availabilityId)
                .filter(b -> belongsTo(b.getDoctor().getUser(), doctorUserId))
                .orElseThrow(AppointmentNotFoundException::new);
        availability.delete(block);
    }
}
