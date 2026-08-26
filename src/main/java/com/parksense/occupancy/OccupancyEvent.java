package com.parksense.occupancy;

import com.parksense.lot.SlotState;
import com.parksense.lot.SlotType;

import java.time.Instant;

/**
 * Immutable fact: "slot X moved from state A to state B at time T".
 * Published by {@code OccupancyLedger} on every legal transition and fanned
 * out to all subscribers (GoF Observer) — display boards, the dashboard
 * feed and the capacity alert watcher all react to this one type.
 */
public record OccupancyEvent(
        String slotCode,
        String floor,
        String zone,
        SlotType slotType,
        String plate,
        SlotState oldState,
        SlotState newState,
        Instant at,
        String reason) {

    public boolean isEntry() {
        return newState == SlotState.OCCUPIED;
    }

    public boolean isExit() {
        return oldState == SlotState.OCCUPIED && newState == SlotState.FREE;
    }
}
