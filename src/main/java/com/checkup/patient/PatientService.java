package com.checkup.patient;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.account.Role;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.appointment.AppointmentStatus;
import com.checkup.audit.AuditAction;
import com.checkup.audit.AuditEntry;
import com.checkup.audit.AuditLogRepository;
import com.checkup.audit.AuditService;
import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;
import com.checkup.web.NotFoundException;

/**
 * Reading and changing patient records, with the access rules:
 *   - a patient can only read and change their own record;
 *   - a doctor can read the record of, and add notes for, a patient who has a
 *     non-cancelled appointment with them, and nobody else.
 *
 * Every doctor view and every change is written to the audit log in the SAME transaction
 * (see AuditService). A patient looking at their own record isn't logged: they are the
 * person the log protects.
 */
@Service
public class PatientService {

    /** Old app allowed any number; a cap stops a form being abused to fill the database. */
    public static final int MAX_PHONES = 5;

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final ClinicalNoteRepository notes;
    private final AuditService audit;
    private final AuditLogRepository auditLog;
    private final Clock clock;

    public PatientService(PatientRepository patients, DoctorRepository doctors, AppointmentRepository appointments,
                          ClinicalNoteRepository notes, AuditService audit, AuditLogRepository auditLog, Clock clock) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.notes = notes;
        this.audit = audit;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    // ================================================================== patient: own record

    @Transactional(readOnly = true)
    public Patient ownRecord(Long patientUserId) {
        return patients.findWithPhonesByUser_Id(patientUserId).orElseThrow(NotFoundException::new);
    }

    @Transactional
    public void updateProfile(Long patientUserId, ProfileForm form) {
        Patient patient = patients.findByUser_Id(patientUserId).orElseThrow(NotFoundException::new);
        List<String> changed = form.changedFieldsComparedTo(patient);
        if (changed.isEmpty()) {
            return; // nothing to save, nothing to log
        }
        form.applyTo(patient); // saved automatically when the transaction commits
        audit.record(patientUserId, patient.getFullName(), Role.PATIENT, AuditAction.UPDATE_RECORD,
                patient.getId(), "Changed: " + String.join(", ", changed));
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
        audit.record(patientUserId, patient.getFullName(), Role.PATIENT, AuditAction.ADD_PHONE,
                patient.getId(), type.name().toLowerCase() + " number");
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
        audit.record(patientUserId, patient.getFullName(), Role.PATIENT, AuditAction.REMOVE_PHONE,
                patient.getId(), phone.getType().name().toLowerCase() + " number");
    }

    /** "Who has looked at or changed my record?" for the patient themselves. */
    @Transactional(readOnly = true)
    public List<AuditEntry> accessLog(Long patientUserId) {
        Patient patient = patients.findByUser_Id(patientUserId).orElseThrow(NotFoundException::new);
        return auditLog.findByPatientIdOrderByOccurredAtDescIdDesc(patient.getId());
    }

    @Transactional(readOnly = true)
    public List<ClinicalNote> notesForOwnRecord(Long patientUserId) {
        Patient patient = patients.findByUser_Id(patientUserId).orElseThrow(NotFoundException::new);
        return notes.findByPatient_IdOrderByCreatedAtDescIdDesc(patient.getId());
    }

    // ================================================================== doctor access

    /**
     * A patient's record as seen by a doctor. 404 unless the patient has a non-cancelled
     * appointment with this doctor ("need to know"). Every successful view is logged,
     * so it's a read-write transaction, not readOnly.
     */
    @Transactional
    public Patient recordForDoctor(Long doctorUserId, Long patientId) {
        Doctor doctor = treatingDoctor(doctorUserId, patientId);
        Patient patient = patients.findWithPhonesById(patientId).orElseThrow(NotFoundException::new);
        audit.record(doctorUserId, doctor.getFullName(), Role.DOCTOR, AuditAction.VIEW_RECORD, patientId, null);
        return patient;
    }

    @Transactional(readOnly = true)
    public List<ClinicalNote> notesForDoctor(Long doctorUserId, Long patientId) {
        treatingDoctor(doctorUserId, patientId);
        return notes.findByPatient_IdOrderByCreatedAtDescIdDesc(patientId);
    }

    /** A doctor adds a note to their patient's chart. Logged (without the note text). */
    @Transactional
    public ClinicalNote addNote(Long doctorUserId, Long patientId, String body) {
        Doctor doctor = treatingDoctor(doctorUserId, patientId);
        if (body == null || body.isBlank()) {
            throw new InvalidNoteException("The note is empty.");
        }
        if (body.length() > 4000) {
            throw new InvalidNoteException("Notes can be up to 4000 characters.");
        }
        Patient patient = patients.findById(patientId).orElseThrow(NotFoundException::new);
        ClinicalNote note = notes.save(new ClinicalNote(patient, doctor, LocalDateTime.now(clock), body.trim()));
        audit.record(doctorUserId, doctor.getFullName(), Role.DOCTOR, AuditAction.ADD_NOTE, patientId, null);
        return note;
    }

    /** The logged-in doctor, if (and only if) this patient is theirs. Otherwise 404. */
    private Doctor treatingDoctor(Long doctorUserId, Long patientId) {
        Doctor doctor = doctors.findByUser_Id(doctorUserId).orElseThrow(NotFoundException::new);
        boolean isTheirPatient = appointments.existsByDoctor_IdAndPatient_IdAndStatusNot(
                doctor.getId(), patientId, AppointmentStatus.CANCELLED);
        if (!isTheirPatient) {
            throw new NotFoundException();
        }
        return doctor;
    }
}
