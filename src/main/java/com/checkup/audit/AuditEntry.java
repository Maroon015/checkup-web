package com.checkup.audit;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import com.checkup.account.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One line in the audit log: "Dr. Jordan Patel (DOCTOR) viewed Alex Morgan's record at 10:42".
 *
 * Can't be changed once written:
 *   - no setters, only a constructor;
 *   - @Immutable tells Hibernate never to send an UPDATE for this entity;
 *   - AuditLogRepository has no update or delete methods at all.
 *
 * actorUserId and patientId are plain ids rather than relationships, so writing a log
 * line never has to load the account or patient, and the log stays readable on its own.
 */
@Entity
@Immutable
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false)
    private Role actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(name = "patient_id")
    private Long patientId;

    private String details;

    protected AuditEntry() {
    }

    AuditEntry(LocalDateTime occurredAt, Long actorUserId, String actorName, Role actorRole,
               AuditAction action, Long patientId, String details) {
        this.occurredAt = occurredAt;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.action = action;
        this.patientId = patientId;
        this.details = details;
    }

    public Long getId() { return id; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public Long getActorUserId() { return actorUserId; }
    public String getActorName() { return actorName; }
    public Role getActorRole() { return actorRole; }
    public AuditAction getAction() { return action; }
    public Long getPatientId() { return patientId; }
    public String getDetails() { return details; }
}
