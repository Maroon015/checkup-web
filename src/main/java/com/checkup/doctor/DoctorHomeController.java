package com.checkup.doctor;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.checkup.account.CurrentUser;
import com.checkup.appointment.AppointmentRepository;

/**
 * The doctor's home page: their upcoming schedule. Only reachable with the DOCTOR role.
 * Like the patient page, the doctor comes from the login, so a doctor only sees
 * appointments booked with them.
 */
@Controller
public class DoctorHomeController {

    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public DoctorHomeController(DoctorRepository doctors, AppointmentRepository appointments) {
        this.doctors = doctors;
        this.appointments = appointments;
    }

    @GetMapping("/doctor")
    public String home(@AuthenticationPrincipal CurrentUser me, Model model) {
        Doctor doctor = doctors.findByUser_Id(me.getId())
                .orElseThrow(() -> new IllegalStateException("No doctor profile for account " + me.getId()));
        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", appointments
                .findByDoctor_IdAndStartTimeGreaterThanEqualOrderByStartTime(doctor.getId(), LocalDate.now().atStartOfDay()));
        return "doctor/home";
    }
}
