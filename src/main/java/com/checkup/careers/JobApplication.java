package com.checkup.careers;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A job application from the public careers page.
 *
 * Old version (Application.java, by Hamza): appended to applicationInfo.txt; doctors could
 * read the file by typing a random number printed on screen. Now only ADMIN accounts can.
 */
@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;

    @Column(nullable = false)
    private String availability;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    protected JobApplication() {
    }

    public JobApplication(String fullName, String email, String phone, Position position,
                          int yearsExperience, String availability, LocalDateTime submittedAt) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.position = position;
        this.yearsExperience = yearsExperience;
        this.availability = availability;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Position getPosition() { return position; }
    public int getYearsExperience() { return yearsExperience; }
    public String getAvailability() { return availability; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
