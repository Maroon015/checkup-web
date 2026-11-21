package com.checkup.audit;

import java.util.List;

import org.springframework.data.repository.Repository;

/**
 * Append-only access to the audit log.
 *
 * Extends the bare Repository interface instead of JpaRepository, so ONLY the methods
 * listed here exist. There is no delete(), deleteAll() or saveAll(): no code in this app
 * can remove or rewrite history, because the method to do it was never created.
 */
public interface AuditLogRepository extends Repository<AuditEntry, Long> {

    AuditEntry save(AuditEntry entry);

    /** Everything that happened to one patient's record, newest first. */
    List<AuditEntry> findByPatientIdOrderByOccurredAtDescIdDesc(Long patientId);

    /** The most recent entries across the whole system (admin page). */
    List<AuditEntry> findTop100ByOrderByOccurredAtDescIdDesc();

    long count();
}
