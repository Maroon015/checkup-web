package com.checkup.patient;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A patient and their medical record.
 *
 * Main changes from the old Patient.java:
 *  - Each patient has an id. The old code found records by full name, so two
 *    "Alex Morgan"s would overwrite each other. Email is also unique.
 *  - Date of birth instead of age, because a stored age goes out of date every birthday.
 *    getAge() works it out.
 *  - Phones are a list of PhoneNumber rows instead of "Personal: ..." strings.
 *  - Validation is annotations; the old constructor silently replaced bad values
 *    (bad email -> "A+", bad blood type -> "A+"). Now bad data is rejected.
 *  - No file reading/writing in this class at all. Saving is PatientRepository's job.
 */
@Entity
@Table(name = "patients")
public class Patient {

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
    @Email
    @Size(max = 254)
    private String email;

    @NotNull
    @Past
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String street;

    // A String, not an int, so units like "4B" work.
    @Size(max = 10)
    @Column(name = "unit_number")
    private String unitNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type")
    private BloodType bloodType;

    @Positive
    @Column(name = "height_cm")
    private Double heightCm;

    @Positive
    @Column(name = "weight_kg")
    private Double weightKg;

    @Size(max = 2000)
    @Column(name = "medical_history")
    private String medicalHistory;

    // One patient -> many phone numbers.
    // cascade = ALL: saving/deleting the patient also saves/deletes their phones.
    // orphanRemoval: a phone removed from this list is deleted from the database.
    // List<@Valid PhoneNumber>: also check the rules on every phone inside the list.
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<@Valid PhoneNumber> phones = new ArrayList<>();

    protected Patient() {
    }

    public Patient(String firstName, String lastName, String email, LocalDate dateOfBirth) {
        this.firstName = firstName;
        this.lastName = lastName;
        setEmail(email);
        this.dateOfBirth = dateOfBirth;
    }

    /** Emails are stored trimmed and lowercase, so the database's UNIQUE rule also
     *  catches "Alex@Example.com" vs "alex@example.com". */
    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public void addPhone(PhoneNumber phone) {
        phones.add(phone);
        phone.setPatient(this);
    }

    public void removePhone(PhoneNumber phone) {
        phones.remove(phone);
        phone.setPatient(null);
    }

    /** Phones of one type, e.g. all EMERGENCY numbers. */
    public List<PhoneNumber> getPhones(PhoneType type) {
        return phones.stream().filter(p -> p.getType() == type).toList();
    }

    /** Age in whole years today, worked out from the date of birth. */
    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getCity() { return city; }
    public String getStreet() { return street; }
    public String getUnitNumber() { return unitNumber; }
    public BloodType getBloodType() { return bloodType; }
    public Double getHeightCm() { return heightCm; }
    public Double getWeightKg() { return weightKg; }
    public String getMedicalHistory() { return medicalHistory; }
    /** Read-only view: use addPhone()/removePhone() to change the list. */
    public List<PhoneNumber> getPhones() { return Collections.unmodifiableList(phones); }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = normalizeEmail(email); }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setCity(String city) { this.city = city; }
    public void setStreet(String street) { this.street = street; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
    public void setBloodType(BloodType bloodType) { this.bloodType = bloodType; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
}
