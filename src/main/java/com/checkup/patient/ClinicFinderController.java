package com.checkup.patient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.checkup.clinic.Clinic;
import com.checkup.clinic.ClinicRepository;
import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;

/**
 * "Find a clinic": pick a city, see its clinics and the doctors at each, with a link
 * straight to booking.
 *
 * Old version: Clinic.findClinic() scanned clinicInfo.txt line by line and printed the
 * matching addresses as one comma-separated string.
 */
@Controller
public class ClinicFinderController {

    private final ClinicRepository clinics;
    private final DoctorRepository doctors;

    public ClinicFinderController(ClinicRepository clinics, DoctorRepository doctors) {
        this.clinics = clinics;
        this.doctors = doctors;
    }

    @GetMapping("/patient/clinics")
    public String find(@RequestParam(required = false) String city, Model model) {
        model.addAttribute("cities", clinics.findAllCities());
        model.addAttribute("city", city);
        if (city != null && !city.isBlank()) {
            List<Doctor> found = doctors.findByClinic_CityIgnoreCaseOrderByClinic_NameAscLastNameAsc(city.trim());
            // Group doctors by clinic, keeping the sorted order (LinkedHashMap keeps insertion order).
            Map<Clinic, List<Doctor>> byClinic = found.stream().collect(
                    Collectors.groupingBy(Doctor::getClinic, LinkedHashMap::new, Collectors.toList()));
            model.addAttribute("clinics", byClinic);
        }
        return "patient/clinics";
    }
}
