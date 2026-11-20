package com.checkup.doctor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.checkup.account.CurrentUser;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.appointment.SchedulingService;

/**
 * The doctor's home page: their schedule from today on. Only reachable with the DOCTOR
 * role. Like the patient page, the doctor comes from the login, so a doctor only sees
 * appointments booked with them.
 */
@Controller
public class DoctorHomeController {

    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final SchedulingService scheduling;

    public DoctorHomeController(DoctorRepository doctors, AppointmentRepository appointments,
                                SchedulingService scheduling) {
        this.doctors = doctors;
        this.appointments = appointments;
        this.scheduling = scheduling;
    }

    @GetMapping("/doctor")
    public String home(@AuthenticationPrincipal CurrentUser me, Model model) {
        Doctor doctor = doctors.findByUser_Id(me.getId())
                .orElseThrow(() -> new IllegalStateException("No doctor profile for account " + me.getId()));
        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", appointments
                .findByDoctor_IdAndStartTimeGreaterThanEqualOrderByStartTime(doctor.getId(), scheduling.today().atStartOfDay()));
        model.addAttribute("now", scheduling.now()); // decides which action buttons show
        return "doctor/home";
    }
}
