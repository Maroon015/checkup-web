package com.checkup.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

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
import com.checkup.appointment.AppointmentNotFoundException;
import com.checkup.appointment.AppointmentRuleException;
import com.checkup.appointment.SchedulingService;
import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;

/**
 * Booking, in three pages:
 *   /patient/book                        choose a doctor
 *   /patient/book/{doctorId}?date=...    choose a day and a free time
 *   /patient/book/{doctorId}/confirm     say what it's for, then POST to book
 * Plus POST /patient/appointments/{id}/cancel.
 *
 * All under /patient/**, so SecurityConfig already requires the PATIENT role.
 */
@Controller
public class BookingController {

    /** How many days the day picker shows. */
    private static final int DAYS_SHOWN = 14;

    private final DoctorRepository doctors;
    private final SchedulingService scheduling;

    public BookingController(DoctorRepository doctors, SchedulingService scheduling) {
        this.doctors = doctors;
        this.scheduling = scheduling;
    }

    @GetMapping("/patient/book")
    public String chooseDoctor(Model model) {
        model.addAttribute("doctors", doctors.findAllByOrderByLastNameAscFirstNameAsc());
        return "patient/book-doctors";
    }

    @GetMapping("/patient/book/{doctorId}")
    public String chooseTime(@PathVariable Long doctorId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             Model model) {
        Doctor doctor = doctors.findWithClinicById(doctorId).orElseThrow(AppointmentNotFoundException::new);
        LocalDate today = scheduling.today();
        LocalDate day = (date == null || date.isBefore(today)) ? today : date;

        model.addAttribute("doctor", doctor);
        model.addAttribute("day", day);
        model.addAttribute("days", IntStream.range(0, DAYS_SHOWN).mapToObj(today::plusDays).toList());
        model.addAttribute("slots", scheduling.freeSlots(doctorId, day));
        return "patient/book-slots";
    }

    @GetMapping("/patient/book/{doctorId}/confirm")
    public String confirmForm(@PathVariable Long doctorId,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                              Model model) {
        Doctor doctor = doctors.findWithClinicById(doctorId).orElseThrow(AppointmentNotFoundException::new);
        model.addAttribute("doctor", doctor);
        model.addAttribute("start", start);
        return "patient/book-confirm";
    }

    @PostMapping("/patient/book/{doctorId}")
    public String book(@AuthenticationPrincipal CurrentUser me,
                       @PathVariable Long doctorId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                       @RequestParam String reason,
                       RedirectAttributes redirect) {
        if (reason.length() > 500) {
            redirect.addFlashAttribute("error", "Please keep the reason under 500 characters.");
            return "redirect:/patient/book/" + doctorId + "?date=" + start.toLocalDate();
        }
        try {
            scheduling.book(me.getId(), doctorId, start, reason);
        } catch (AppointmentRuleException e) {
            // e.g. someone else took the slot a moment ago: back to the times page with the reason.
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/patient/book/" + doctorId + "?date=" + start.toLocalDate();
        }
        redirect.addFlashAttribute("success", "Appointment requested. The doctor's office will confirm it.");
        return "redirect:/patient";
    }

    @PostMapping("/patient/appointments/{id}/cancel")
    public String cancel(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            scheduling.cancelByPatient(me.getId(), id);
            redirect.addFlashAttribute("success", "Appointment cancelled.");
        } catch (AppointmentRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient";
    }
}
