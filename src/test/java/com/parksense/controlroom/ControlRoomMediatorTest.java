package com.parksense.controlroom;

import com.parksense.entrycheck.EntryOutcome;
import com.parksense.exitlane.ExitOutcome;
import com.parksense.exitlane.ExitRequest;
import com.parksense.testsupport.Harness;
import com.parksense.vehicles.Vehicle;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Mediator — the entry/exit protocols coordinate colleagues end to end. */
class ControlRoomMediatorTest {

    private final Harness h = new Harness();

    @Test
    void entryIssuesTicketOpensBarrierUpdatesBoards() {
        EntryOutcome out = h.mediator.handleVehicleEntry("GATE-IN-1",
                new Vehicle("MED-1", VehicleType.CAR, false));
        assertTrue(out.accepted());
        assertTrue(h.gateIn1.barrier().isOpen() || h.gateIn1.commandQueue().history().size() >= 2);
        assertEquals(3, h.gateIn1.commandQueue().history().size()); // print + open + close
        assertTrue(h.tickets.find(out.ticketNo()).isPresent());
        assertTrue(h.plates.isInside("MED-1"));
        assertEquals("WELCOME — SLOT " + out.slotCode(), h.gateIn1.displayLine());
    }

    @Test
    void entryPublishesOccupancyEventAndFeedSeesIt() {
        int before = h.feed.size();
        h.mediator.handleVehicleEntry("GATE-IN-1", new Vehicle("MED-2", VehicleType.CAR, false));
        assertTrue(h.feed.size() > before);
    }

    @Test
    void fullExitFlowFreesSlotAndMarksTicketExited() {
        EntryOutcome in = h.mediator.handleVehicleEntry("GATE-IN-1",
                new Vehicle("MED-3", VehicleType.CAR, false));
        // stay 2h then exit paying cash
        h.clock.advance(java.time.Duration.ofHours(2));
        ExitOutcome out = h.mediator.handleVehicleExit("GATE-OUT-1",
                ExitRequest.paying("GATE-OUT-1", "MED-3", com.parksense.tickets.PaymentMethod.CASH,
                        new java.math.BigDecimal("500")));
        assertTrue(out.allowed(), out.line());
        assertEquals("EXITED", h.tickets.find(in.ticketNo()).orElseThrow().stateName());
        assertEquals(com.parksense.lot.SlotState.FREE,
                h.lot.slot(in.slotCode()).orElseThrow().state());
        assertFalse(h.plates.isInside("MED-3"));
    }

    @Test
    void unknownGateIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                h.mediator.handleVehicleEntry("NOPE", new Vehicle("X", VehicleType.CAR, false)));
    }

    @Test
    void sensorSignalConfirmsReservation() {
        EntryOutcome in = h.mediator.handleVehicleEntry("GATE-IN-1",
                new Vehicle("MED-4", VehicleType.CAR, false));
        var slot = h.lot.slot(in.slotCode()).orElseThrow();
        // mediator already confirmed arrival via the sensor feed during entry
        assertEquals(com.parksense.lot.SlotState.OCCUPIED, slot.state());
    }
}
