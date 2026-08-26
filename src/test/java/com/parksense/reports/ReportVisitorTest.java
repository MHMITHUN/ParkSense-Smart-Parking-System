package com.parksense.reports;

import com.parksense.common.Money;
import com.parksense.reports.visitor.OccupancyVisitor;
import com.parksense.reports.visitor.RevenueVisitor;
import com.parksense.reports.visitor.UtilizationVisitor;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffSelector;
import com.parksense.tariff.builder.TariffPlanBuilder;
import com.parksense.testsupport.Harness;
import com.parksense.tickets.PaymentMethod;
import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;
import com.parksense.tickets.addon.CarWashAddon;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Visitor — reports walk the lot tree and ticket history. */
class ReportVisitorTest {

    private final Harness h = new Harness();

    @Test
    void occupancyVisitorCountsEveryZoneRow() {
        OccupancyVisitor visitor = new OccupancyVisitor();
        h.lot.accept(visitor);
        assertEquals(1, visitor.rows().size());
        assertEquals("8", visitor.rows().get(0).get(1));
    }

    @Test
    void utilizationVisitorComputesPercentages() {
        h.mediator.handleVehicleEntry("GATE-IN-1",
                new com.parksense.vehicles.Vehicle("V-1", VehicleType.CAR, false));
        UtilizationVisitor visitor = new UtilizationVisitor();
        h.lot.accept(visitor);
        String pct = visitor.rows().get(0).get(3);
        assertEquals("12.5%", pct); // 1 of 8
    }

    @Test
    void revenueVisitorSumsPaymentsByDay() {
        Instant now = h.clock.now();
        Ticket a = exitedTicket("R-1", now, Money.of("80.00"), PaymentMethod.CASH);
        Ticket b = exitedTicket("R-2", now, Money.of("120.00"), PaymentMethod.CARD);
        RevenueVisitor visitor = new RevenueVisitor();
        a.accept(visitor);
        b.accept(visitor);
        assertEquals(1, visitor.rows().size()); // same day → one row
        assertEquals(1, visitor.rows().stream()
                .filter(r -> r.get(5).equals("200.00")).count());
        assertEquals(200.0, visitor.seriesValues().get(0), 0.001); // day revenue sum
    }

    @Test
    void peakHourVisitorBucketsExits() {
        // seeded harness has no exits; visitor over nothing is empty but safe
        var visitor = new com.parksense.reports.visitor.PeakHourVisitor();
        h.tickets.all().forEach(t -> t.accept(visitor));
        assertEquals(0, visitor.values().stream().mapToDouble(Double::doubleValue).sum(), 0.01);
    }

    @Test
    void addonsVisitorPicksUpDecoratorLines() {
        Instant now = h.clock.now();
        Ticket t = new Ticket("T-ADD", "ADD-1", VehicleType.CAR, false, now.minus(Duration.ofHours(2)),
                "GATE-IN-1", "L1-A-01");
        t.confirmEntry();
        t.setFee(new CarWashAddon(
                        new com.parksense.tickets.addon.BaseParkingFee(Money.of("80.00"), "Parking 2h"),
                        Money.of("120.00")),
                "T-CAP", "test");
        t.pay(new PaymentRecord("T-ADD", PaymentMethod.CASH, Money.of("200.00"),
                Money.of("200.00"), BigDecimal.ZERO, now));
        t.completeExit("GATE-OUT-1", now);
        h.tickets.save(t);

        var visitor = new com.parksense.reports.visitor.AddonsVisitor();
        t.accept(visitor);
        assertTrue(visitor.rows().stream().anyMatch(r -> r.get(0).contains("wash")));
    }

    private Ticket exitedTicket(String no, Instant at, BigDecimal amount, PaymentMethod method) {
        Ticket t = new Ticket(no, no, VehicleType.CAR, false, at.minus(Duration.ofHours(2)),
                "GATE-IN-1", "L1-A-01");
        t.confirmEntry();
        t.setFee(new com.parksense.tickets.addon.BaseParkingFee(amount, "Parking"), "T-CAP", "test");
        t.pay(new PaymentRecord(no, method, amount, amount, BigDecimal.ZERO, at));
        t.completeExit("GATE-OUT-1", at);
        h.tickets.save(t);
        return t;
    }
}
