package com.checkup.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.appointment.AppointmentStatus;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;
import com.checkup.web.NotFoundException;

/**
 * Reading and changing patient records, with the access rules:
 *   - a patient can only read and change their own record;
 *   - a doctor can read (not change) the record of a patient who has a non-cancelled
 *     appointment with them, and nobody else's.
 */
@Service
public class PatientService {

    /** Old app allowed any number; a cap stops a form being abused to fill the database. */
    public static final int MAX_PHONES = 5;

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public PatientService(PatientRepository patients, DoctorRepository doctors, AppointmentRepository appointments) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
    }

    // ================================================================== patient: own record

    @Transactional(readOnly = true)
    public Patient ownRecord(Long patientUserId) {
        return patients.findWithPhonesByUser_Id(patientUserId).orElseThrow(NotFoundException::new);
    }

    @Transactional
    public void updateProfile(Long patientUserId, ProfileForm form) {
        Patient patient = patients.findByUser_Id(patientUserId).orElseThrow(NotFoundException::new);
        form.applyTo(patient); // saved automatically when the transaction commits
    }

    /**
     * Adds a phone number. Accepts "519-555-0101", "(519) 555 0101" etc.: everything that
     * isn't a digit is removed first, then it must be exactly 10 digits (the old rule).
     */
    @Transactional
    public void addPhone(Long patientUserId, PhoneType type, String rawNumber) {
        Patient patient = patients.findWithPhonesByUser_Id(patientUserId).orElseThrow(NotFoundException::new);
        String digits = rawNumber == null ? "" : rawNumber.replaceAll("\\D", "");
        if (digits.length() != 10) {
            throw new InvalidPhoneException("Phone numbers must have exactly 10 digits, e.g. 519-555-0101.");
        }
        boolean duplicate = patient.getPhones().stream()
                .anyMatch(p -> p.getType() == type && p.getNumber().equals(digits));
        if (duplicate) {
            throw new InvalidPhoneException("You already have that number.");
        }
        if (patient.getPhones().size() >= MAX_PHONES) {
            throw new InvalidPhoneException("You can save up to " + MAX_PHONES + " phone numbers.");
        }
        patient.addPhone(new PhoneNumber(type, digits)); // cascade saves the new row
    }

    @Transactional
    public void removePhone(Long patientUserId, Long phoneId) {
        Patient patient = patients.findWithPhonesByUser_Id(patientUserId).orElseThrow(NotFoundException::new);
        // Only look inside THIS patient's phones, so another patient's phone id just isn't found.
        PhoneNumber phone = patient.getPhones().stream()
                .filter(p -> p.getId().equals(phoneId))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        patient.removePhone(phone); // orphanRemoval deletes the row
    }

    // ================================================================== doctor: read access

    /**
     * A patient's record as seen by a doctor. 404 unless the patient has a non-cancelled
     * appointment with this doctor ("need to know").
     */
    @Transactional(readOnly = true)
    public Patient recordForDoctor(Long doctorUserId, Long patientId) {
        Doctor doctor = doctors.findByUser_Id(doctorUserId).orElseThrow(NotFoundException::new);
        boolean isTheirPatient = appointments.existsByDoctor_IdAndPatient_IdAndStatusNot(
                doctor.getId(), patientId, AppointmentStatus.CANCELLED);
        if (!isTheirPatient) {
            throw new NotFoundException();
        }
        return patients.findWithPhonesById(patientId).orElseThrow(NotFoundException::new);
    }
}
