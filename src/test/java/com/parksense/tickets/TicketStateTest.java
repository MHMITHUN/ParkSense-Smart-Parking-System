package com.parksense.tickets;

import com.parksense.common.Money;
import com.parksense.testsupport.Harness;
import com.parksense.tickets.state.IllegalTransitionException;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** GoF State — the ticket lifecycle accepts legal moves and rejects all others. */
class TicketStateTest {

    private final Harness h = new Harness();

    private Ticket newTicket() {
        Ticket t = new Ticket("T-1", "TEST-1", VehicleType.CAR, false,
                h.clock.now(), "GATE-IN-1", "L1-A-01");
        t.setFee(new com.parksense.tickets.addon.BaseParkingFee(
                Money.of("50.00"), "Parking 1h"), "T-CAP", "test");
        return t;
    }

    private PaymentRecord cash(BigDecimal amount) {
        return new PaymentRecord("T-1", PaymentMethod.CASH, amount, amount,
                BigDecimal.ZERO, h.clock.now());
    }

    @Test
    void issuedConfirmsToActive() {
        Ticket t = newTicket();
        t.confirmEntry();
        assertEquals("ACTIVE", t.stateName());
    }

    @Test
    void activePaysToPaidAndExits() {
        Ticket t = newTicket();
        t.confirmEntry();
        t.pay(cash(Money.of("50.00")));
        assertEquals("PAID", t.stateName());
        t.completeExit("GATE-OUT-1", h.clock.now());
        assertEquals("EXITED", t.stateName());
        assertTrue(t.isOpen() == false);
    }

    @Test
    void exitedTicketCannotBePaidAgain() {
        Ticket t = newTicket();
        t.confirmEntry();
        t.pay(cash(Money.of("50.00")));
        t.completeExit("GATE-OUT-1", h.clock.now());
        assertThrows(IllegalTransitionException.class, () -> t.pay(cash(Money.of("50.00"))));
    }

    @Test
    void exitedTicketCannotExitTwice() {
        Ticket t = newTicket();
        t.confirmEntry();
        t.pay(cash(Money.of("50.00")));
        t.completeExit("GATE-OUT-1", h.clock.now());
        assertThrows(IllegalTransitionException.class,
                () -> t.completeExit("GATE-OUT-1", h.clock.now()));
    }

    @Test
    void unpaidActiveCannotExit() {
        Ticket t = newTicket();
        t.confirmEntry();
        assertThrows(IllegalTransitionException.class,
                () -> t.completeExit("GATE-OUT-1", h.clock.now()));
    }

    @Test
    void activeCanBeReportedLostThenSettled() {
        Ticket t = newTicket();
        t.confirmEntry();
        t.reportLost();
        assertEquals("LOST", t.stateName());
        t.pay(cash(Money.of("800.00")));
        t.completeExit("GATE-OUT-1", h.clock.now());
        assertEquals("EXITED", t.stateName());
    }

    @Test
    void paidReopensOnOverstay() {
        Ticket t = newTicket();
        t.confirmEntry();
        t.pay(cash(Money.of("50.00")));
        t.reopenFromOverstay();
        assertEquals("ACTIVE", t.stateName());
        t.pay(cash(Money.of("80.00")));
        t.completeExit("GATE-OUT-1", h.clock.now());
        assertEquals("EXITED", t.stateName());
        assertEquals(Money.of("130.00"), t.totalPaid());
    }

    @Test
    void voidWorksFromActiveAndPaid() {
        Ticket a = newTicket();
        a.confirmEntry();
        a.voidTicket("test");
        assertEquals("VOID", a.stateName());
        assertEquals("test", a.voidReason());

        Ticket b = newTicket();
        b.confirmEntry();
        b.pay(cash(Money.of("50.00")));
        b.voidTicket("goodwill");
        assertEquals("VOID", b.stateName());
    }

    @Test
    void exitedCannotBeVoided() {
        Ticket t = newTicket();
        t.confirmEntry();
        t.pay(cash(Money.of("50.00")));
        t.completeExit("GATE-OUT-1", h.clock.now());
        assertThrows(IllegalTransitionException.class, () -> t.voidTicket("late"));
    }
}
