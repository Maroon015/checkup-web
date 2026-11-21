package com.checkup.appointment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * A patient's appointments, soonest first ("My appointments" page).
     * @EntityGraph loads each appointment's doctor and clinic in the SAME query. Without it,
     * the page would need one extra query per appointment (the "N+1 query" problem), and
     * because doctor/clinic are LAZY, the page would crash once the database session closed.
     */
    @EntityGraph(attributePaths = {"doctor", "doctor.clinic"})
    List<Appointment> findByPatient_IdOrderByStartTime(Long patientId);

    /** A doctor's schedule between two times. */
    List<Appointment> findByDoctor_IdAndStartTimeBetweenOrderByStartTime(
            Long doctorId, LocalDateTime from, LocalDateTime to);

    /** A doctor's appointments from a time onwards, with the patient loaded (doctor dashboard). */
    @EntityGraph(attributePaths = {"patient"})
    List<Appointment> findByDoctor_IdAndStartTimeGreaterThanEqualOrderByStartTime(Long doctorId, LocalDateTime from);

    /** Has this patient got any non-cancelled appointment with this doctor? (record access rule) */
    boolean existsByDoctor_IdAndPatient_IdAndStatusNot(Long doctorId, Long patientId, AppointmentStatus status);

    /** One patient's appointments with one doctor, newest first (doctor's view of a record). */
    List<Appointment> findByDoctor_IdAndPatient_IdOrderByStartTimeDesc(Long doctorId, Long patientId);

    /**
     * The doctor's active (not cancelled or completed) appointments that overlap [from, to).
     * Two time ranges overlap when each one starts before the other ends.
     * Written in JPQL: like SQL, but using Java class and field names.
     */
    @Query("""
            select a from Appointment a
            where a.doctor.id = :doctorId
              and a.status in (com.checkup.appointment.AppointmentStatus.REQUESTED,
                               com.checkup.appointment.AppointmentStatus.CONFIRMED)
              and a.startTime < :to and a.endTime > :from
            """)
    List<Appointment> findActiveForDoctorOverlapping(@Param("doctorId") Long doctorId,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);

    /** Same, for one patient: stops a patient booking two places at once. */
    @Query("""
            select a from Appointment a
            where a.patient.id = :patientId
              and a.status in (com.checkup.appointment.AppointmentStatus.REQUESTED,
                               com.checkup.appointment.AppointmentStatus.CONFIRMED)
              and a.startTime < :to and a.endTime > :from
            """)
    List<Appointment> findActiveForPatientOverlapping(@Param("patientId") Long patientId,
                                                      @Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);
}
