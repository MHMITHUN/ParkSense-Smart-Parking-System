package com.parksense.tickets;

import com.parksense.common.Money;
import com.parksense.tickets.addon.BaseParkingFee;
import com.parksense.tickets.addon.CarWashAddon;
import com.parksense.tickets.addon.EvChargeAddon;
import com.parksense.tickets.addon.LostTicketPenalty;
import com.parksense.tickets.addon.ValetServiceAddon;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Decorator — add-ons stack in any order and print as their own lines. */
class AddonDecoratorTest {

    @Test
    void baseFeeAlone() {
        FeeComponent base = new BaseParkingFee(Money.of("50.00"), "Parking 1h");
        assertEquals(Money.of("50.00"), base.amount());
        assertEquals(1, base.lines().size());
    }

    @Test
    void singleAddonWrapsBase() {
        FeeComponent wash = new CarWashAddon(
                new BaseParkingFee(Money.of("50.00"), "Parking 1h"), Money.of("120.00"));
        assertEquals(Money.of("170.00"), wash.amount());
        assertEquals(2, wash.lines().size());
        assertEquals("Car wash (exterior + interior)", wash.lines().get(1).label());
    }

    @Test
    void stackedAddonsSumInOrder() {
        FeeComponent chain = new ValetServiceAddon(
                new CarWashAddon(new BaseParkingFee(Money.of("50.00"), "Parking 1h"),
                        Money.of("120.00")),
                Money.of("200.00"));
        assertEquals(Money.of("370.00"), chain.amount());
        assertEquals(3, chain.lines().size());
    }

    @Test
    void evChargeIsKilowattHourBased() {
        FeeComponent ev = new EvChargeAddon(
                new BaseParkingFee(Money.of("0.00"), "Within grace"),
                new BigDecimal("18.5"), Money.of("22.00"));
        assertEquals(Money.of("407.00"), ev.amount());
        assertTrue(ev.lines().get(1).label().contains("18.5 kWh"));
    }

    @Test
    void lostPenaltyAddsTwoLines() {
        FeeComponent lost = new LostTicketPenalty(
                new BaseParkingFee(BigDecimal.ZERO, "Parking (duration unprovable)"),
                Money.of("500.00"), Money.of("300.00"));
        assertEquals(Money.of("800.00"), lost.amount());
        assertEquals(3, lost.lines().size());
    }

    @Test
    void describeComposesThroughTheChain() {
        FeeComponent chain = new CarWashAddon(
                new BaseParkingFee(Money.of("50.00"), "Parking 1h"), Money.of("120.00"));
        assertTrue(chain.describe().contains("car wash"));
        assertTrue(chain.describe().contains("Parking 1h"));
    }
}
