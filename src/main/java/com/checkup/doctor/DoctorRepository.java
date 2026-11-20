package com.checkup.doctor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // "Clinic_Id" walks into the clinic relationship: WHERE clinic_id = ?
    List<Doctor> findByClinic_IdOrderByLastName(Long clinicId);

    // Optional = "might not exist". Forces the caller to handle the not-found case
    // instead of getting a null by surprise.
    Optional<Doctor> findByEmailIgnoreCase(String email);

    /** All doctors with their clinics, for the "choose a doctor" page. */
    @EntityGraph(attributePaths = "clinic")
    List<Doctor> findAllByOrderByLastNameAscFirstNameAsc();

    /** One doctor with their clinic. ("WithClinic" is just a readable name; Spring ignores it.) */
    @EntityGraph(attributePaths = "clinic")
    Optional<Doctor> findWithClinicById(Long id);

    /** The doctor profile that belongs to a login account, with its clinic loaded. */
    @EntityGraph(attributePaths = "clinic")
    Optional<Doctor> findByUser_Id(Long userId);
}
