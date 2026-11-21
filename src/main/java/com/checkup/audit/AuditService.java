package com.checkup.audit;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.account.Role;

/**
 * Writes audit log entries.
 *
 * Propagation.MANDATORY: this method refuses to run unless it's called from inside an
 * existing transaction, i.e. the same transaction as the change being logged. So the
 * change and its log line are saved together or not at all: you can't end up with a
 * changed record and no log entry, or a log entry for a change that was rolled back.
 */
@Service
public class AuditService {

    private final AuditLogRepository log;
    private final Clock clock;

    public AuditService(AuditLogRepository log, Clock clock) {
        this.log = log;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorUserId, String actorName, Role actorRole,
                       AuditAction action, Long patientId, String details) {
        log.save(new AuditEntry(LocalDateTime.now(clock), actorUserId, actorName, actorRole,
                action, patientId, details));
    }
}
