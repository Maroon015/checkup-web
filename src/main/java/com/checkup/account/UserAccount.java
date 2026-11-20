package com.checkup.account;

import java.time.LocalDateTime;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A login account: email + hashed password + role.
 *
 * Kept separate from Patient and Doctor on purpose: "who can log in, and as what" is a
 * security concern, while Patient/Doctor hold medical and contact details. A Patient or
 * Doctor points to its UserAccount (see their "user" field).
 *
 * Named UserAccount, not User, because "user" is a reserved word in SQL and Spring
 * Security already has a class called User.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    // Never the real password: a BCrypt hash like "{bcrypt}$2a$10$N9qo8uLO...".
    // The "{bcrypt}" prefix says which algorithm made it, so it can be upgraded later.
    @NotBlank
    @Column(name = "password_hash")
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = true;

    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    protected UserAccount() {
    }

    /** passwordHash must already be hashed (use AccountService, which does it for you). */
    public UserAccount(String email, String passwordHash, Role role) {
        setEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Lowercased so "Alex@Example.com" and "alex@example.com" are the same login. */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
