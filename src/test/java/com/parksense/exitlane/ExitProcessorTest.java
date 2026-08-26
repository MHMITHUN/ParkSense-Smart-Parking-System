package com.parksense.exitlane;

import com.parksense.testsupport.Harness;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Template Method — fixed step order, lane-specific payment hooks. */
class ExitProcessorTest {

    private final Harness h = new Harness();

    private String enter(String plate) {
        return h.mediator.handleVehicleEntry("GATE-IN-1",
                new com.parksense.vehicles.Vehicle(plate, VehicleType.CAR, false)).ticketNo();
    }

    @Test
    void staffedLaneCollectsCashAndFreesTheSlot() {
        String ticket = enter("EXIT-1");
        h.clock.advance(Duration.ofHours(3));
        ExitOutcome out = h.mediator.handleVehicleExit("GATE-OUT-1",
                ExitRequest.paying("GATE-OUT-1", ticket, com.parksense.tickets.PaymentMethod.CASH,
                        new BigDecimal("200")));
        assertTrue(out.allowed(), out.line());
        // 3h → 20 + 3*30 = 110, tendered 200 → change 90
        assertEquals(0, out.charged().compareTo(new BigDecimal("110.00")));
        assertEquals("EXITED", h.tickets.find(ticket).orElseThrow().stateName());
    }

    @Test
    void staffedLaneRejectsShortTender() {
        String ticket = enter("EXIT-2");
        h.clock.advance(Duration.ofHours(3));
        ExitOutcome out = h.mediator.handleVehicleExit("GATE-OUT-1",
                ExitRequest.paying("GATE-OUT-1", ticket, com.parksense.tickets.PaymentMethod.CASH,
                        new BigDecimal("10")));
        assertFalse(out.allowed());
        assertEquals("PAYMENT REQUIRED — SETTLE AT BOOTH", out.line());
    }

    @Test
    void unpaidCarCannotLeaveWithoutTender() {
        String ticket = enter("EXIT-3");
        h.clock.advance(Duration.ofHours(1));
        ExitOutcome out = h.mediator.handleVehicleExit("GATE-OUT-1",
                ExitRequest.paying("GATE-OUT-1", ticket, com.parksense.tickets.PaymentMethod.CASH,
                        new BigDecimal("1")));
        assertFalse(out.allowed());
    }

    @Test
    void expressLaneWavesMembersThroughAtZero() {
        h.memberStore.save(new com.parksense.members.Member("M-1", "Member One", "+880",
                java.util.Set.of("EXPRESS-1"), "MONTHLY",
                h.clock.today().plusDays(10)));
        String ticket = enter("EXPRESS-1");
        h.clock.advance(Duration.ofHours(4));
        ExitOutcome out = h.mediator.handleVehicleExit("GATE-OUT-2",
                ExitRequest.of("GATE-OUT-2", "EXPRESS-1"));
        assertTrue(out.allowed(), out.line());
        assertEquals(0, out.charged().compareTo(BigDecimal.ZERO));
        assertTrue(out.line().contains("NO CHARGE"));
    }

    @Test
    void expressLaneRefusesNonMembers() {
        String ticket = enter("EXPRESS-2");
        h.clock.advance(Duration.ofHours(1));
        ExitOutcome out = h.mediator.handleVehicleExit("GATE-OUT-2",
                ExitRequest.of("GATE-OUT-2", "EXPRESS-2"));
        assertFalse(out.allowed());
    }

    @Test
    void lostTicketDeskChargesFlatPenalty() {
        enter("LOST-1");
        h.clock.advance(Duration.ofHours(6));
        ExitOutcome out = h.mediator.handleVehicleExit("GATE-OUT-1",
                new ExitRequest("GATE-OUT-1", "LOST-1", com.parksense.tickets.PaymentMethod.CARD,
                        new BigDecimal("1000"), true));
        assertTrue(out.allowed(), out.line());
        assertEquals(0, out.charged().compareTo(new BigDecimal("800.00"))); // 500 + 300
        assertTrue(out.steps().stream().anyMatch(s -> s.contains("LOST")));
        assertTrue(h.tickets.findOpenByPlate("LOST-1").isEmpty());
    }

    @Test
    void templateStepsRunInFixedOrder() {
        String ticket = enter("EXIT-4");
        h.clock.advance(Duration.ofHours(1));
        ExitOutcome out = h.mediator.handleVehicleExit("GATE-OUT-1",
                ExitRequest.paying("GATE-OUT-1", ticket, com.parksense.tickets.PaymentMethod.CARD, null));
        assertTrue(out.allowed());
        int ticketIdx = indexOfContaining(out.steps(), "ticket:");
        int tariffIdx = indexOfContaining(out.steps(), "tariff:");
        int payIdx = indexOfContaining(out.steps(), "payment:");
        int slotIdx = indexOfContaining(out.steps(), "freed");
        int exitIdx = indexOfContaining(out.steps(), "EXITED at");
        assertTrue(ticketIdx < tariffIdx);
        assertTrue(tariffIdx < payIdx);
        assertTrue(payIdx < slotIdx);
        assertTrue(slotIdx < exitIdx);
    }

    private static int indexOfContaining(java.util.List<String> steps, String needle) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).contains(needle)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }
}
