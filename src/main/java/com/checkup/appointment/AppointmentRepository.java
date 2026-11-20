package com.checkup.appointment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * A patient's appointments, soonest first ("My appointments" page).
     * @EntityGraph loads each appointment's doctor and clinic in the SAME query. Without it,
     * the page would need one extra query per appointment (the "N+1 query" problem), and
     * because doctor/clinic are LAZY, the page would crash once the database session closed.
     */
    @EntityGraph(attributePaths = {"doctor", "doctor.clinic"})
    List<Appointment> findByPatient_IdOrderByStartTime(Long patientId);

    /** A doctor's schedule between two times (finding free slots, step 4). */
    List<Appointment> findByDoctor_IdAndStartTimeBetweenOrderByStartTime(
            Long doctorId, LocalDateTime from, LocalDateTime to);

    /** A doctor's upcoming appointments with the patient loaded (doctor dashboard). */
    @EntityGraph(attributePaths = {"patient"})
    List<Appointment> findByDoctor_IdAndStartTimeGreaterThanEqualOrderByStartTime(Long doctorId, LocalDateTime from);
}
