package com.checkup.doctor;

import com.checkup.clinic.Clinic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A doctor who works at one clinic.
 *
 * Old version: the clinic was just a name string ("salem-clinic") and only ONE doctor
 * could exist, because outputNewAccount() overwrote doctorAccount.txt. Now any number of
 * doctors can exist, and each one points to a real Clinic row (a foreign key).
 * Login details (username/password) move to a separate User table in step 3.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "first_name")
    private String firstName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "last_name")
    private String lastName;

    @NotBlank
    @Size(max = 100)
    private String specialty;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    // Same rule as the old setDoctorPhoneNumber(): exactly 10 digits.
    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
    private String phone;

    // Optional now (null is allowed); if given, 10 digits like the old setter.
    @Pattern(regexp = "\\d{10}", message = "Fax number must be exactly 10 digits")
    private String fax;

    // Many doctors -> one clinic. LAZY: only load the clinic from the database when asked.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    protected Doctor() {
    }

    public Doctor(String firstName, String lastName, String specialty, String email, String phone, Clinic clinic) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.specialty = specialty;
        setEmail(email);
        this.phone = phone;
        this.clinic = clinic;
    }

    public String getFullName() {
        return "Dr. " + firstName + " " + lastName;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getSpecialty() { return specialty; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getFax() { return fax; }
    public Clinic getClinic() { return clinic; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    /** Stored trimmed and lowercase so the UNIQUE rule is effectively case-insensitive. */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
    public void setPhone(String phone) { this.phone = phone; }
    public void setFax(String fax) { this.fax = fax; }
    public void setClinic(Clinic clinic) { this.clinic = clinic; }
}
