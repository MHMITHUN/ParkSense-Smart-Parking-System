package com.parksense.controlroom;

import com.parksense.hardware.spi.SlotSignal;
import com.parksense.vehicles.Vehicle;

/**
 * The coordination contract of the control room (GoF Mediator). Gates,
 * kiosks and sensor feeds call these methods instead of each other; the
 * concrete mediator runs the entry chain, allocation, ticketing, barrier
 * commands and event publication in one place.
 */
public interface ParkingMediator {

    /** A vehicle arrived at an entry gate: validate, allocate, ticket, open. */
    com.parksense.entrycheck.EntryOutcome handleVehicleEntry(String gateId, Vehicle vehicle);

    /** A vehicle presented itself at an exit gate: price, settle, free, open. */
    com.parksense.exitlane.ExitOutcome handleVehicleExit(String gateId,
                                                         com.parksense.exitlane.ExitRequest request);

    /** A slot sensor reported a presence change. */
    void handleSensorSignal(SlotSignal signal);

    /** Broadcast a message to a lane display. */
    void displayOnGate(String gateId, String message);
}
