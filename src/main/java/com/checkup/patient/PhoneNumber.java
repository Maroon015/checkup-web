package com.checkup.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * One phone number belonging to a patient. A patient can have several (personal and
 * emergency), so they get their own table with a patient_id column pointing back.
 */
@Entity
@Table(name = "phone_numbers")
public class PhoneNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING) // store "PERSONAL", not 0, so reordering the enum can't corrupt data
    @Column(name = "phone_type")
    private PhoneType type;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    protected PhoneNumber() {
    }

    public PhoneNumber(PhoneType type, String number) {
        this.type = type;
        this.number = number;
    }

    public Long getId() { return id; }
    public PhoneType getType() { return type; }
    public String getNumber() { return number; }
    public Patient getPatient() { return patient; }

    public void setType(PhoneType type) { this.type = type; }
    public void setNumber(String number) { this.number = number; }

    // Package-private: only Patient.addPhone()/removePhone() should set this,
    // so both sides of the relationship always stay in sync.
    void setPatient(Patient patient) { this.patient = patient; }
}
