package com.parksense.gates;

import com.parksense.audit.AuditTrail;
import com.parksense.gates.hardware.GateHardwareFactory;
import com.parksense.vehicles.Vehicle;

/**
 * An inbound lane. Its single job: read the plate and hand the vehicle to
 * the mediator — the gate itself holds no entry logic.
 */
public final class EntryGate extends Gate {

    public EntryGate(String id, String code, GateHardwareFactory hardwareFactory, AuditTrail audit) {
        super(id, code, GateDirection.ENTRY, LaneKind.STAFFED, hardwareFactory, audit);
    }

    /** The operator pressed "Camera Read" — run the full entry protocol. */
    public com.parksense.entrycheck.EntryOutcome vehicleArrived(Vehicle vehicle) {
        setDisplay("READING " + vehicle.plateNo());
        return mediator().handleVehicleEntry(id(), vehicle);
    }
}
