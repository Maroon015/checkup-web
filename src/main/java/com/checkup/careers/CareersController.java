package com.checkup.careers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * Public careers page: anyone can apply (no login), only admins can read applications
 * (see AdminController).
 *
 * Being public, this form is the one most exposed to spam bots. CSRF is still on, which
 * blocks forms posted from other sites; a CAPTCHA or rate limit would be the next step
 * for a real deployment.
 */
@Controller
public class CareersController {

    private final JobApplicationRepository applications;
    private final Clock clock;

    public CareersController(JobApplicationRepository applications, Clock clock) {
        this.applications = applications;
        this.clock = clock;
    }

    @GetMapping("/careers")
    public String form(Model model) {
        model.addAttribute("form", new ApplicationForm());
        model.addAttribute("positions", Position.values());
        return "careers";
    }

    @PostMapping("/careers")
    public String apply(@Valid @ModelAttribute("form") ApplicationForm form, BindingResult errors,
                        Model model, RedirectAttributes redirect) {
        if (!errors.hasFieldErrors("phone") && form.phoneDigits().length() != 10) {
            errors.rejectValue("phone", "digits", "Phone numbers must have exactly 10 digits");
        }
        if (errors.hasErrors()) {
            model.addAttribute("positions", Position.values());
            return "careers";
        }
        applications.save(new JobApplication(
                form.getFullName().trim(),
                form.getEmail().trim().toLowerCase(Locale.ROOT),
                form.phoneDigits(),
                form.getPosition(),
                form.getYearsExperience(),
                form.getAvailability().trim(),
                LocalDateTime.now(clock)));
        redirect.addFlashAttribute("success", "Thanks for applying! We'll be in touch by email.");
        return "redirect:/careers";
    }
}
