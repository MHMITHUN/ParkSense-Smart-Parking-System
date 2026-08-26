package com.parksense.audit;

import java.time.Instant;

/**
 * One line of the append-only audit trail: who did (or attempted) what,
 * when, and whether it was allowed. Every gate command, proxy denial,
 * tariff change and ticket void lands here.
 */
public record AuditEntry(Instant at, String actor, String action, String detail, boolean allowed) {

    public static AuditEntry allowed(String actor, String action, String detail) {
        return new AuditEntry(Instant.now(), actor, action, detail, true);
    }

    public static AuditEntry denied(String actor, String action, String detail) {
        return new AuditEntry(Instant.now(), actor, action, detail, false);
    }
}
