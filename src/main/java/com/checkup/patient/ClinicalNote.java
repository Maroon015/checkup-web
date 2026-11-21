package com.checkup.patient;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import com.checkup.doctor.Doctor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A note a doctor writes in a patient's chart. Immutable, like a real chart: a mistake
 * is corrected by writing a new note, never by editing or deleting the old one.
 */
@Entity
@Immutable
@Table(name = "clinical_notes")
public class ClinicalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotBlank
    @Size(max = 4000)
    private String body;

    protected ClinicalNote() {
    }

    public ClinicalNote(Patient patient, Doctor doctor, LocalDateTime createdAt, String body) {
        this.patient = patient;
        this.doctor = doctor;
        this.createdAt = createdAt;
        this.body = body;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getBody() { return body; }
}
