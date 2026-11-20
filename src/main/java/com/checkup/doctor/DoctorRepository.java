package com.checkup.doctor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // "Clinic_Id" walks into the clinic relationship: WHERE clinic_id = ?
    List<Doctor> findByClinic_IdOrderByLastName(Long clinicId);

    // Optional = "might not exist". Forces the caller to handle the not-found case
    // instead of getting a null by surprise.
    Optional<Doctor> findByEmailIgnoreCase(String email);
}
