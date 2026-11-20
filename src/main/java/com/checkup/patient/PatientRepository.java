package com.checkup.patient;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
