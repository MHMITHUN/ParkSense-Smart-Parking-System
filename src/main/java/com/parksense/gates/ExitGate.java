package com.parksense.gates;

import com.parksense.audit.AuditTrail;
import com.parksense.exitlane.ExitRequest;
import com.parksense.gates.hardware.GateHardwareFactory;

/**
 * An outbound lane. Staffed booths settle fares with an operator; express
 * lanes wave valid members through automatically.
 */
public final class ExitGate extends Gate {

    public ExitGate(String id, String code, LaneKind laneKind,
                    GateHardwareFactory hardwareFactory, AuditTrail audit) {
        super(id, code, GateDirection.EXIT, laneKind, hardwareFactory, audit);
    }

    /** The vehicle presented a ticket (or just a plate) — run the exit protocol. */
    public com.parksense.exitlane.ExitOutcome vehicleLeaving(ExitRequest request) {
        setDisplay("CHECKING…");
        return mediator().handleVehicleExit(id(), request);
    }
}
