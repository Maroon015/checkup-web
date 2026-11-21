package com.checkup.doctor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.checkup.account.CurrentUser;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.patient.InvalidNoteException;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;
import com.checkup.patient.PatientService;
import com.checkup.web.NotFoundException;

/**
 * A doctor's patients, their records (each view is audit-logged) and clinical notes.
 *
 * Old version: Doctor.viewPatientRecords(fName, lName) printed ANY patient's full record
 * to any doctor who typed a name, and nothing recorded that it happened.
 */
@Controller
public class DoctorPatientsController {

    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final PatientService patientService;

    public DoctorPatientsController(DoctorRepository doctors, PatientRepository patients,
                                    AppointmentRepository appointments, PatientService patientService) {
        this.doctors = doctors;
        this.patients = patients;
        this.appointments = appointments;
        this.patientService = patientService;
    }

    @GetMapping("/doctor/patients")
    public String list(@AuthenticationPrincipal CurrentUser me,
                       @RequestParam(required = false, defaultValue = "") String lastName,
                       Model model) {
        Doctor doctor = doctors.findByUser_Id(me.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("lastName", lastName);
        model.addAttribute("patients", patients.findPatientsOfDoctor(doctor.getId(), lastName.trim()));
        return "doctor/patients";
    }

    @GetMapping("/doctor/patients/{patientId}")
    public String record(@AuthenticationPrincipal CurrentUser me, @PathVariable Long patientId, Model model) {
        Patient patient = patientService.recordForDoctor(me.getId(), patientId); // 404 if not theirs; logged
        Doctor doctor = doctors.findByUser_Id(me.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("patient", patient);
        model.addAttribute("notes", patientService.notesForDoctor(me.getId(), patientId));
        model.addAttribute("appointments",
                appointments.findByDoctor_IdAndPatient_IdOrderByStartTimeDesc(doctor.getId(), patientId));
        return "doctor/patient";
    }

    @PostMapping("/doctor/patients/{patientId}/notes")
    public String addNote(@AuthenticationPrincipal CurrentUser me, @PathVariable Long patientId,
                          @RequestParam String body, RedirectAttributes redirect) {
        try {
            patientService.addNote(me.getId(), patientId, body);
            redirect.addFlashAttribute("success", "Note added.");
        } catch (InvalidNoteException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/doctor/patients/" + patientId + "#notes";
    }
}
