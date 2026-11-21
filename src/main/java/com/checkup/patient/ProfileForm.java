package com.checkup.patient;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * The "edit my record" form. A form object (not the Patient entity itself) so only these
 * fields can be changed from the page: not the email (it's the login), not the id, not
 * the account link.
 *
 * Old version: updatePatientRecord() in Main.java asked which field to change by name
 * ("blood type", "height"...) and validated each with its own if-statements.
 */
public class ProfileForm {

    @NotBlank(message = "Enter your first name")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Enter your last name")
    @Size(max = 50)
    private String lastName;

    @NotNull(message = "Enter your date of birth")
    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String street;

    @Size(max = 10, message = "Unit number is too long")
    private String unitNumber;

    private BloodType bloodType; // optional; the page offers the 8 types or "Not sure"

    @Positive(message = "Height must be more than 0")
    @DecimalMax(value = "272", message = "Height must be in centimetres (max 272)")
    private Double heightCm;

    @Positive(message = "Weight must be more than 0")
    @DecimalMax(value = "650", message = "Weight must be in kilograms (max 650)")
    private Double weightKg;

    @Size(max = 2000, message = "Please keep this under 2000 characters")
    private String medicalHistory;

    /** Pre-fills the form with the patient's current record. */
    public static ProfileForm from(Patient p) {
        ProfileForm f = new ProfileForm();
        f.firstName = p.getFirstName();
        f.lastName = p.getLastName();
        f.dateOfBirth = p.getDateOfBirth();
        f.city = p.getCity();
        f.street = p.getStreet();
        f.unitNumber = p.getUnitNumber();
        f.bloodType = p.getBloodType();
        f.heightCm = p.getHeightCm();
        f.weightKg = p.getWeightKg();
        f.medicalHistory = p.getMedicalHistory();
        return f;
    }

    /** Copies the form onto the patient. Blank optional text becomes null, not "". */
    void applyTo(Patient p) {
        p.setFirstName(firstName.trim());
        p.setLastName(lastName.trim());
        p.setDateOfBirth(dateOfBirth);
        p.setCity(blankToNull(city));
        p.setStreet(blankToNull(street));
        p.setUnitNumber(blankToNull(unitNumber));
        p.setBloodType(bloodType);
        p.setHeightCm(heightCm);
        p.setWeightKg(weightKg);
        p.setMedicalHistory(blankToNull(medicalHistory));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }
    public BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodType bloodType) { this.bloodType = bloodType; }
    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
}
