package com.checkup.patient;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Old version: printPatientRecord(), editRecord() and readPhoneNumbers() each re-read the
 * whole PatientInfo.txt, matched "Patient Name:" lines, and editRecord() rewrote the
 * whole file to change one field. Now: findById() to load, change a field, save().
 */
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // For the doctor's patient search later: "mor" finds "Alex Morgan".
    List<Patient> findByLastNameContainingIgnoreCaseOrderByLastName(String lastNamePart);

    /** The patient profile that belongs to a login account. */
    Optional<Patient> findByUser_Id(Long userId);

    /** Same, with phone numbers loaded (profile page). */
    @EntityGraph(attributePaths = "phones")
    Optional<Patient> findWithPhonesByUser_Id(Long userId);

    /** One patient with phone numbers loaded (doctor's view of a record). */
    @EntityGraph(attributePaths = "phones")
    Optional<Patient> findWithPhonesById(Long id);

    /**
     * "My patients" for a doctor: everyone with at least one appointment with this doctor
     * that wasn't cancelled. Optional last-name filter (empty string = everyone).
     * A doctor never gets a list of ALL patients: only the ones they actually see.
     */
    @Query("""
            select distinct p from Appointment a join a.patient p
            where a.doctor.id = :doctorId
              and a.status <> com.checkup.appointment.AppointmentStatus.CANCELLED
              and lower(p.lastName) like lower(concat('%', :lastName, '%'))
            order by p.lastName, p.firstName
            """)
    List<Patient> findPatientsOfDoctor(@Param("doctorId") Long doctorId, @Param("lastName") String lastName);
}
