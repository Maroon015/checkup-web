package com.checkup.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * The login and sign-up pages. Checking the login itself is done by Spring Security
 * (POST /login), not by code here.
 */
@Controller
public class AuthController {

    private final AccountService accountService;
    private final boolean demoLoginEnabled;

    public AuthController(AccountService accountService,
                          @Value("${checkup.demo-data.enabled:true}") boolean demoLoginEnabled) {
        this.accountService = accountService;
        this.demoLoginEnabled = demoLoginEnabled;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("demoLoginEnabled", demoLoginEnabled);
        model.addAttribute("demoPatientEmail", DemoAccounts.PATIENT_EMAIL);
        model.addAttribute("demoDoctorEmail", DemoAccounts.DOCTOR_EMAIL);
        model.addAttribute("demoPassword", DemoAccounts.PASSWORD);
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    /**
     * @Valid runs the annotations on RegistrationForm; any problems land in BindingResult,
     * and we show the same page again with the messages instead of crashing.
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult errors, RedirectAttributes redirect) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Passwords don't match");
        }
        if (errors.hasErrors()) {
            return "register";
        }
        try {
            accountService.registerPatient(form);
        } catch (EmailAlreadyUsedException e) {
            errors.rejectValue("email", "taken", "An account with this email already exists");
            return "register";
        }
        // Post/Redirect/Get: redirect after a successful POST, so refreshing the page
        // doesn't submit the form twice.
        redirect.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}
