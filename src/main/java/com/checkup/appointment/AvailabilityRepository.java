package com.checkup.appointment;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    // Not sorted by day here: the database would sort "FRIDAY" before "MONDAY"
    // alphabetically. SchedulingService sorts by the enum's real order instead.
    List<Availability> findByDoctor_Id(Long doctorId);

    List<Availability> findByDoctor_IdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
}
