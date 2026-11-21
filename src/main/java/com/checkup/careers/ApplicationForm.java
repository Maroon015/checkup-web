package com.checkup.careers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The careers form. Rules carried over from the old Application.java, with two changes:
 *   - phone: any format with 10 digits ("519-555-0101", "(519) 555 0101"), not only xxx-xxx-xxxx;
 *   - email: any valid address, not only gmail/hotmail/outlook/yahoo (that list rejected
 *     school and work emails).
 */
public class ApplicationForm {

    @NotBlank(message = "Enter your full name")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Enter your email")
    @Email(message = "Enter a valid email address")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Enter your phone number")
    @Size(max = 25)
    private String phone;

    @NotNull(message = "Choose a position")
    private Position position;

    // Same rule as the old setExperience(): at least 2 years.
    @NotNull(message = "Enter your years of experience")
    @Min(value = 2, message = "You need at least 2 years of experience to apply")
    @Max(value = 60, message = "Please enter a realistic number of years")
    private Integer yearsExperience;

    @NotBlank(message = "Tell us when you're available")
    @Size(max = 200, message = "Please keep this under 200 characters")
    private String availability;

    /** The phone number with everything except digits removed. */
    public String phoneDigits() {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
}
