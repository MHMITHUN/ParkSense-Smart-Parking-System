package com.parksense.entrycheck;

import com.parksense.testsupport.Harness;
import com.parksense.vehicles.Vehicle;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Chain of Responsibility — ordered rules, first failure wins. */
class EntryChainTest {

    private final Harness h = new Harness();

    private EntryOutcome enter(String plate, VehicleType type) {
        return h.mediator.handleVehicleEntry("GATE-IN-1", new Vehicle(plate, type, false));
    }

    @Test
    void cleanCarIsAcceptedWithTrace() {
        EntryOutcome out = enter("OK-CAR-1", VehicleType.CAR);
        assertTrue(out.accepted());
        assertNotNull(out.ticketNo());
        assertNotNull(out.slotCode());
        assertTrue(out.trace().stream().anyMatch(l -> l.startsWith("PASS Blacklist")));
        assertTrue(out.trace().stream().anyMatch(l -> l.startsWith("PASS Duplicate")));
    }

    @Test
    void blacklistedPlateIsBlockedFirst() {
        h.plates.blacklist("STOLEN-9");
        EntryOutcome out = enter("STOLEN-9", VehicleType.CAR);
        assertFalse(out.accepted());
        assertEquals(BlacklistHandler.REASON, out.line());
        assertTrue(out.trace().stream().anyMatch(l -> l.startsWith("FAIL Blacklist")));
    }

    @Test
    void duplicateEntryIsRefused() {
        assertTrue(enter("DUP-1", VehicleType.CAR).accepted());
        EntryOutcome second = enter("DUP-1", VehicleType.CAR);
        assertFalse(second.accepted());
        assertEquals(DuplicateEntryHandler.REASON, second.line());
    }

    @Test
    void motorcycleNeedsMotorcycleBay() {
        // exhaust the one motorcycle bay
        assertTrue(enter("MC-1", VehicleType.MOTORCYCLE).accepted());
        EntryOutcome second = enter("MC-2", VehicleType.MOTORCYCLE);
        assertFalse(second.accepted());
        assertEquals(SlotAvailabilityHandler.REASON, second.line());
    }

    @Test
    void evFallsBackToStandardWhenChargerTaken() {
        assertTrue(enter("EV-1", VehicleType.EV).accepted());
        EntryOutcome second = enter("EV-2", VehicleType.EV);
        assertTrue(second.accepted());
        assertTrue(second.trace().stream().anyMatch(l -> l.contains("standard bay will be assigned")),
                "EV fallback note expected");
    }

    @Test
    void fullLotRefusesWithSlotReason() {
        // 4 STANDARD + 1 COMPACT bays serve cars
        for (int i = 0; i < 5; i++) {
            assertTrue(enter("FILL-" + i, VehicleType.CAR).accepted(), "car " + i + " should fit");
        }
        EntryOutcome sixth = enter("FILL-5", VehicleType.CAR);
        assertFalse(sixth.accepted());
        assertEquals(SlotAvailabilityHandler.REASON, sixth.line());
        // and the motorcycle bay is still untouched by cars
        assertTrue(enter("MC-OK", VehicleType.MOTORCYCLE).accepted());
    }
}
