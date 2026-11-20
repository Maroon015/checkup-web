package com.checkup.clinic;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access for clinics. There's no code to write: Spring Data reads the method
 * names and generates the SQL. JpaRepository already gives save(), findById(), findAll(),
 * deleteById(), count() and more.
 *
 * Old version: Clinic.findClinic() was ~40 lines of BufferedReader loops over clinicInfo.txt.
 */
public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    // -> SELECT * FROM clinics WHERE LOWER(city) = LOWER(?) ORDER BY name
    List<Clinic> findByCityIgnoreCaseOrderByName(String city);
}
