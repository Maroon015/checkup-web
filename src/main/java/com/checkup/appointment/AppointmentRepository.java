package com.checkup.appointment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** A patient's appointments, soonest first ("My appointments" page). */
    List<Appointment> findByPatient_IdOrderByStartTime(Long patientId);

    /** A doctor's schedule between two times (doctor dashboard, finding free slots). */
    List<Appointment> findByDoctor_IdAndStartTimeBetweenOrderByStartTime(
            Long doctorId, LocalDateTime from, LocalDateTime to);
}
