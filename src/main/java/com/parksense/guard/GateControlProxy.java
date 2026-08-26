package com.parksense.guard;

import com.parksense.audit.AuditTrail;
import com.parksense.gates.BarrierController;
import com.parksense.gates.GateControl;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * GoF Proxy (protection proxy) in front of {@link BarrierController}.
 * Force-open and force-close strand revenue and void safety logic, so they
 * are ADMIN-only; the check happens at the object boundary — even buggy or
 * reused controller code cannot bypass it. Every attempt, allowed or
 * denied, lands in the audit trail.
 */
public final class GateControlProxy implements GateControl {

    private final BarrierController real;
    private final AuditTrail audit;
    private final BooleanSupplier adminCheck;
    private final Supplier<String> actorName;

    public GateControlProxy(BarrierController real, AuditTrail audit,
                            BooleanSupplier adminCheck, Supplier<String> actorName) {
        this.real = real;
        this.audit = audit;
        this.adminCheck = adminCheck;
        this.actorName = actorName;
    }

    @Override
    public void forceOpen(String actor) {
        guard("FORCE_OPEN", actor);
        real.forceOpen(actor);
    }

    @Override
    public void forceClose(String actor) {
        guard("FORCE_CLOSE", actor);
        real.forceClose(actor);
    }

    @Override
    public void resumeAutomatic(String actor) {
        // returning to automatic is safe at operator level
        audit.record(actor, "RESUME_AUTOMATIC", "gate control back to automatic", true);
        real.resumeAutomatic(actor);
    }

    private void guard(String action, String actor) {
        if (!adminCheck.getAsBoolean()) {
            audit.record(actor, action, "denied: ADMIN role required", false);
            throw new SecurityException(
                    "Barrier override requires ADMIN role — attempt recorded");
        }
        audit.record(actor, action, "allowed", true);
    }

    /** Exposed for tests and the UI (read-only peek at the actor source). */
    public Supplier<String> actorSource() {
        return actorName;
    }
}
