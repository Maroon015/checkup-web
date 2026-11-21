package com.checkup.doctor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.checkup.account.CurrentUser;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;
import com.checkup.patient.PatientService;
import com.checkup.web.NotFoundException;

/**
 * A doctor's patients and their records (read-only for now; doctor edits come with the
 * audit log in step 6, so every change to a record is recorded).
 *
 * Old version: Doctor.viewPatientRecords(fName, lName) printed ANY patient's full record
 * to any doctor who typed a name. Here a doctor only sees patients booked with them.
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
        Patient patient = patientService.recordForDoctor(me.getId(), patientId); // 404 if not theirs
        Doctor doctor = doctors.findByUser_Id(me.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("patient", patient);
        model.addAttribute("appointments",
                appointments.findByDoctor_IdAndPatient_IdOrderByStartTimeDesc(doctor.getId(), patientId));
        return "doctor/patient";
    }
}
