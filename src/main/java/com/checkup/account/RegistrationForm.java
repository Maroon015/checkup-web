package com.checkup.account;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * What the sign-up form sends. A separate "form object" instead of binding straight onto
 * Patient, so a visitor can't sneak extra fields (like a role or someone else's id) into
 * the request, and so the password never touches the Patient class.
 *
 * Old version: signUp() in Main.java asked for each value with Scanner and its own
 * while-loops for re-asking. Here the annotations do the checking and the page shows
 * the messages next to each field.
 */
public class RegistrationForm {

    @NotBlank(message = "Enter your first name")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Enter your last name")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Enter your email")
    @Email(message = "Enter a valid email address")
    @Size(max = 254)
    private String email;

    @NotNull(message = "Enter your date of birth")
    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // matches <input type="date">
    private LocalDate dateOfBirth;

    // 72 is BCrypt's limit; longer passwords would be silently cut off.
    @NotBlank(message = "Choose a password")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
    private String password;

    @NotBlank(message = "Type your password again")
    private String confirmPassword;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
