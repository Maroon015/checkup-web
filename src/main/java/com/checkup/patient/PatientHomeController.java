package com.checkup.patient;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.checkup.account.CurrentUser;
import com.checkup.appointment.AppointmentRepository;

/**
 * The patient's home page. Only reachable with the PATIENT role (SecurityConfig).
 *
 * Notice there's no patient id in the URL. The patient is found from the login itself
 * (me.getId()), so there is no way to ask for someone else's record by changing a number.
 */
@Controller
public class PatientHomeController {

    private final PatientRepository patients;
    private final AppointmentRepository appointments;

    public PatientHomeController(PatientRepository patients, AppointmentRepository appointments) {
        this.patients = patients;
        this.appointments = appointments;
    }

    @GetMapping("/patient")
    public String home(@AuthenticationPrincipal CurrentUser me, Model model) {
        Patient patient = patients.findByUser_Id(me.getId())
                .orElseThrow(() -> new IllegalStateException("No patient profile for account " + me.getId()));
        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointments.findByPatient_IdOrderByStartTime(patient.getId()));
        return "patient/home";
    }
}
