package com.checkup.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.checkup.account.UserAccountRepository;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.clinic.ClinicRepository;
import com.checkup.doctor.DoctorRepository;
import com.checkup.patient.PatientRepository;

/** A simple admin overview. Only reachable with the ADMIN role. More admin tools come later. */
@Controller
public class AdminHomeController {

    private final UserAccountRepository accounts;
    private final ClinicRepository clinics;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;

    public AdminHomeController(UserAccountRepository accounts, ClinicRepository clinics, DoctorRepository doctors,
                               PatientRepository patients, AppointmentRepository appointments) {
        this.accounts = accounts;
        this.clinics = clinics;
        this.doctors = doctors;
        this.patients = patients;
        this.appointments = appointments;
    }

    @GetMapping("/admin")
    public String home(Model model) {
        model.addAttribute("accountCount", accounts.count());
        model.addAttribute("clinicCount", clinics.count());
        model.addAttribute("doctorCount", doctors.count());
        model.addAttribute("patientCount", patients.count());
        model.addAttribute("appointmentCount", appointments.count());
        return "admin/home";
    }
}
