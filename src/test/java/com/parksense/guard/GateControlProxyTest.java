package com.parksense.guard;

import com.parksense.audit.AuditTrail;
import com.parksense.gates.BarrierController;
import com.parksense.gates.EntryGate;
import com.parksense.gates.hardware.SimulatedHardwareFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Proxy — force-open/close is ADMIN-only, enforced and audited. */
class GateControlProxyTest {

    private final AuditTrail audit = new AuditTrail();
    private final EntryGate gate = new EntryGate("G", "G", new SimulatedHardwareFactory(), audit);
    private final BarrierController real = new BarrierController(gate);

    private GateControlProxy proxyFor(boolean admin, String actor) {
        return new GateControlProxy(real, audit, () -> admin, () -> actor);
    }

    @Test
    void operatorIsDeniedAndAudited() {
        GateControlProxy proxy = proxyFor(false, "operator1");
        SecurityException ex = assertThrows(SecurityException.class,
                () -> proxy.forceOpen("operator1"));
        assertTrue(ex.getMessage().contains("ADMIN"));
        assertTrue(audit.latest(1).get(0).detail().contains("denied"));
        assertFalse(audit.latest(1).get(0).allowed());
    }

    @Test
    void adminPassesThrough() {
        GateControlProxy proxy = proxyFor(true, "boss");
        proxy.forceOpen("boss");
        assertTrue(gate.barrier().isOpen());
        assertTrue(gate.displayLine().contains("boss"));
        assertTrue(audit.latest(1).get(0).allowed());
    }

    @Test
    void forceCloseWorksAndResumeIsOperatorAllowed() {
        GateControlProxy proxy = proxyFor(true, "boss");
        proxy.forceClose("boss");
        assertFalse(gate.barrier().isOpen());

        GateControlProxy operatorProxy = proxyFor(false, "op");
        assertDoesNotThrow(() -> operatorProxy.resumeAutomatic("op"));
        assertEquals("READY", gate.displayLine());
    }

    @Test
    void denialDoesNotTouchTheBarrier() {
        GateControlProxy proxy = proxyFor(false, "op");
        assertThrows(SecurityException.class, () -> proxy.forceOpen("op"));
        assertFalse(gate.barrier().isOpen());
        assertFalse(real.manualHold());
    }
}
