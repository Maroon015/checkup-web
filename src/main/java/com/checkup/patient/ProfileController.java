package com.checkup.patient;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.checkup.account.CurrentUser;

import jakarta.validation.Valid;

/**
 * "My profile": the patient's own medical record and phone numbers.
 * Under /patient/**, so PATIENT role only; and every lookup goes through the login id.
 */
@Controller
public class ProfileController {

    private final PatientService patientService;

    public ProfileController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/patient/profile")
    public String profile(@AuthenticationPrincipal CurrentUser me, Model model) {
        Patient patient = patientService.ownRecord(me.getId());
        model.addAttribute("form", ProfileForm.from(patient));
        addPageData(model, patient);
        return "patient/profile";
    }

    @PostMapping("/patient/profile")
    public String update(@AuthenticationPrincipal CurrentUser me,
                         @Valid @ModelAttribute("form") ProfileForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            // Show the page again with the typed values and the error messages.
            addPageData(model, patientService.ownRecord(me.getId()));
            return "patient/profile";
        }
        patientService.updateProfile(me.getId(), form);
        redirect.addFlashAttribute("success", "Your record has been saved.");
        return "redirect:/patient/profile";
    }

    @PostMapping("/patient/profile/phones")
    public String addPhone(@AuthenticationPrincipal CurrentUser me,
                           @RequestParam PhoneType type, @RequestParam String number,
                           RedirectAttributes redirect) {
        try {
            patientService.addPhone(me.getId(), type, number);
            redirect.addFlashAttribute("success", "Phone number added.");
        } catch (InvalidPhoneException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient/profile#phones";
    }

    @PostMapping("/patient/profile/phones/{phoneId}/delete")
    public String removePhone(@AuthenticationPrincipal CurrentUser me, @PathVariable Long phoneId,
                              RedirectAttributes redirect) {
        patientService.removePhone(me.getId(), phoneId);
        redirect.addFlashAttribute("success", "Phone number removed.");
        return "redirect:/patient/profile#phones";
    }

    /** "Who has seen or changed my record" (from the audit log). */
    @GetMapping("/patient/access-log")
    public String accessLog(@AuthenticationPrincipal CurrentUser me, Model model) {
        model.addAttribute("entries", patientService.accessLog(me.getId()));
        model.addAttribute("myUserId", me.getId());
        return "patient/access-log";
    }

    private void addPageData(Model model, Patient patient) {
        model.addAttribute("patient", patient);
        model.addAttribute("notes", patientService.notesForOwnRecord(patient.getUser().getId()));
        model.addAttribute("bloodTypes", BloodType.values());
        model.addAttribute("phoneTypes", PhoneType.values());
    }
}
