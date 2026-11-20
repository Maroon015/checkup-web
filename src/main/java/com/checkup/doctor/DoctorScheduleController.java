package com.checkup.doctor;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.checkup.account.CurrentUser;
import com.checkup.appointment.AppointmentRuleException;
import com.checkup.appointment.SchedulingService;

/**
 * Doctor actions on appointments (confirm / complete / cancel) and their weekly hours.
 * All under /doctor/**, so SecurityConfig already requires the DOCTOR role; the service
 * also checks each appointment belongs to THIS doctor.
 *
 * Every action is a POST (with the CSRF token), never a GET link: a GET must never change
 * data, or a link in an email or a browser prefetch could cancel appointments.
 */
@Controller
public class DoctorScheduleController {

    private final DoctorRepository doctors;
    private final SchedulingService scheduling;

    public DoctorScheduleController(DoctorRepository doctors, SchedulingService scheduling) {
        this.doctors = doctors;
        this.scheduling = scheduling;
    }

    @PostMapping("/doctor/appointments/{id}/confirm")
    public String confirm(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id, RedirectAttributes redirect) {
        return run(redirect, "Appointment confirmed.", () -> scheduling.confirm(me.getId(), id));
    }

    @PostMapping("/doctor/appointments/{id}/complete")
    public String complete(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id, RedirectAttributes redirect) {
        return run(redirect, "Appointment marked as completed.", () -> scheduling.complete(me.getId(), id));
    }

    @PostMapping("/doctor/appointments/{id}/cancel")
    public String cancel(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id, RedirectAttributes redirect) {
        return run(redirect, "Appointment cancelled.", () -> scheduling.cancelByDoctor(me.getId(), id));
    }

    private String run(RedirectAttributes redirect, String successMessage, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("success", successMessage);
        } catch (AppointmentRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor";
    }

    // ------------------------------------------------------------------ weekly hours

    @GetMapping("/doctor/hours")
    public String hours(@AuthenticationPrincipal CurrentUser me, Model model) {
        Doctor doctor = doctors.findByUser_Id(me.getId())
                .orElseThrow(() -> new IllegalStateException("No doctor profile for account " + me.getId()));
        model.addAttribute("doctor", doctor);
        model.addAttribute("hours", scheduling.weeklyHours(doctor.getId()));
        model.addAttribute("days", DayOfWeek.values());
        return "doctor/hours";
    }

    @PostMapping("/doctor/hours")
    public String addHours(@AuthenticationPrincipal CurrentUser me,
                           @RequestParam DayOfWeek day,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end,
                           @RequestParam int slotMinutes,
                           RedirectAttributes redirect) {
        try {
            scheduling.addHours(me.getId(), day, start, end, slotMinutes);
            redirect.addFlashAttribute("success", "Hours added.");
        } catch (AppointmentRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/hours";
    }

    @PostMapping("/doctor/hours/{id}/delete")
    public String removeHours(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id, RedirectAttributes redirect) {
        scheduling.removeHours(me.getId(), id);
        redirect.addFlashAttribute("success", "Hours removed. Appointments already booked are not affected.");
        return "redirect:/doctor/hours";
    }
}
