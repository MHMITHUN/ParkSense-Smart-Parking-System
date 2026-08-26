package com.parksense.tariff.builder;

import com.parksense.common.Money;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Builder — a half-configured plan is unrepresentable. */
class TariffPlanBuilderTest {

    @Test
    void validCappedPlanBuilds() {
        TariffPlan plan = new TariffPlanBuilder("T1", "Capped", TariffKind.DAILY_CAP)
                .baseFee(Money.of("20.00")).perHour(Money.of("30.00"))
                .dailyCap(Money.of("300.00")).graceMinutes(15).active(true)
                .build();
        assertEquals(TariffKind.DAILY_CAP, plan.kind());
        assertTrue(plan.active());
        assertEquals(Money.of("300.00"), plan.dailyCap());
    }

    @Test
    void capBelowBaseIsRejected() {
        assertThrows(IllegalStateException.class, () ->
                new TariffPlanBuilder("T2", "Bad", TariffKind.DAILY_CAP)
                        .baseFee(Money.of("100.00")).perHour(Money.of("30.00"))
                        .dailyCap(Money.of("50.00")).build());
    }

    @Test
    void negativeGraceIsRejected() {
        assertThrows(IllegalStateException.class, () ->
                new TariffPlanBuilder("T3", "Bad", TariffKind.HOURLY).graceMinutes(-5).build());
    }

    @Test
    void graceAboveSixtyMinutesIsRejected() {
        assertThrows(IllegalStateException.class, () ->
                new TariffPlanBuilder("T4", "Bad", TariffKind.HOURLY).graceMinutes(61).build());
    }

    @Test
    void earlyBirdWithoutFlatFeeIsRejected() {
        assertThrows(IllegalStateException.class, () ->
                new TariffPlanBuilder("T5", "Bad", TariffKind.EARLY_BIRD)
                        .earlyBirdWindow(LocalTime.of(10, 0), LocalTime.of(16, 0)).build());
    }

    @Test
    void surgeBelowOneIsRejected() {
        assertThrows(IllegalStateException.class, () ->
                new TariffPlanBuilder("T6", "Bad", TariffKind.EVENT_SURGE)
                        .surgeMultiplier(new BigDecimal("0.8")).build());
    }

    @Test
    void negativeFeesAreRejected() {
        assertThrows(IllegalStateException.class, () ->
                new TariffPlanBuilder("T7", "Bad", TariffKind.HOURLY)
                        .baseFee(Money.of("-1.00")).build());
    }
}
