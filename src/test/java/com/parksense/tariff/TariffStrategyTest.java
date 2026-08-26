package com.parksense.tariff;

import com.parksense.common.Money;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Strategy — each tariff kind prices the same stay differently. */
class TariffStrategyTest {

    private static final Instant BASE = LocalDate.of(2026, 8, 20)
            .atTime(LocalTime.of(9, 0)).atZone(ZoneId.systemDefault()).toInstant();

    private static FeeRequest stay(long minutes) {
        return FeeRequest.of(BASE, BASE.plus(Duration.ofMinutes(minutes)), VehicleType.CAR);
    }

    private static TariffPlan hourly() {
        return plan(TariffKind.HOURLY, null, null);
    }

    private static TariffPlan plan(TariffKind kind, BigDecimal cap, BigDecimal flat) {
        var b = new com.parksense.tariff.builder.TariffPlanBuilder("T", "T", kind)
                .baseFee(Money.of("20.00")).perHour(Money.of("30.00")).graceMinutes(15);
        if (cap != null) {
            b.dailyCap(cap);
        }
        if (flat != null) {
            b.flatFee(flat).earlyBirdWindow(LocalTime.of(10, 0), LocalTime.of(16, 0));
        }
        return b.build();
    }

    @Test
    void withinGraceIsFree() {
        assertEquals(0, new com.parksense.tariff.strategy.HourlyTariff()
                .compute(hourly(), stay(14)).compareTo(BigDecimal.ZERO));
    }

    @Test
    void partialHoursRoundUp() {
        // 75 min → 60 billable → 20 + 30 = 50
        assertEquals(Money.of("50.00"),
                new com.parksense.tariff.strategy.HourlyTariff().compute(hourly(), stay(75)));
    }

    @Test
    void vehicleFactorApplies() {
        var van = FeeRequest.of(BASE, BASE.plus(Duration.ofMinutes(75)), VehicleType.VAN);
        assertEquals(Money.of("75.00"),
                new com.parksense.tariff.strategy.HourlyTariff().compute(hourly(), van));
        var moto = FeeRequest.of(BASE, BASE.plus(Duration.ofMinutes(75)), VehicleType.MOTORCYCLE);
        assertEquals(Money.of("25.00"),
                new com.parksense.tariff.strategy.HourlyTariff().compute(hourly(), moto));
    }

    @Test
    void dailyCapStopsLongStays() {
        // 30h uncapped: 20 + 30*30 = 920; capped: min(920, 300*2) = 600
        assertEquals(Money.of("600.00"),
                new com.parksense.tariff.strategy.DailyCapTariff()
                        .compute(plan(TariffKind.DAILY_CAP, Money.of("300.00"), null), stay(30 * 60)));
    }

    @Test
    void earlyBirdIsFlatAndEligibilityWindowWorks() {
        var early = new com.parksense.tariff.strategy.EarlyBirdTariff();
        var plan = plan(TariffKind.EARLY_BIRD, null, Money.of("150.00"));
        var commuter = new FeeRequest(
                LocalDate.of(2026, 8, 20).atTime(9, 30).atZone(ZoneId.systemDefault()).toInstant(),
                LocalDate.of(2026, 8, 20).atTime(17, 30).atZone(ZoneId.systemDefault()).toInstant(),
                VehicleType.CAR, ZoneId.systemDefault());
        assertTrue(early.eligible(plan, commuter));
        assertEquals(Money.of("150.00"), early.compute(plan, commuter));

        var lateArrival = new FeeRequest(
                LocalDate.of(2026, 8, 20).atTime(11, 0).atZone(ZoneId.systemDefault()).toInstant(),
                LocalDate.of(2026, 8, 20).atTime(17, 30).atZone(ZoneId.systemDefault()).toInstant(),
                VehicleType.CAR, ZoneId.systemDefault());
        assertFalse(early.eligible(plan, lateArrival));
    }

    @Test
    void surgeMultipliesTheHourlyMath() {
        var b = new com.parksense.tariff.builder.TariffPlanBuilder("S", "S", TariffKind.EVENT_SURGE)
                .baseFee(Money.of("20.00")).perHour(Money.of("30.00")).graceMinutes(15)
                .surgeMultiplier(new BigDecimal("1.5"));
        assertEquals(Money.of("75.00"),
                new com.parksense.tariff.strategy.EventSurgeTariff().compute(b.build(), stay(75)));
    }

    @Test
    void memberPassIsZero() {
        var b = new com.parksense.tariff.builder.TariffPlanBuilder("M", "M", TariffKind.MEMBER_PASS)
                .perHour(Money.of("0.00")).graceMinutes(15);
        assertEquals(0, new com.parksense.tariff.strategy.MemberPassTariff()
                .compute(b.build(), stay(8 * 60)).compareTo(BigDecimal.ZERO));
    }

    @Test
    void billedHoursMathMatchesGraceAndRounding() {
        assertEquals(0, Money.billedHours(Duration.ofMinutes(15), 15));
        assertEquals(1, Money.billedHours(Duration.ofMinutes(16), 15));
        assertEquals(1, Money.billedHours(Duration.ofMinutes(75), 15));
        assertEquals(4, Money.billedHours(Duration.ofMinutes(200), 15)); // 185m → ceil 4h
    }
}
