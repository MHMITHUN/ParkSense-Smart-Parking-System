package com.parksense.tariff.builder;

import com.parksense.common.Money;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Fluent constructor for {@link TariffPlan} (GoF Builder). A tariff has
 * many interacting parts; the builder carries sensible defaults and — in
 * {@link #build()} — rejects every combination the exit lane could
 * misprice, so a half-configured plan is unrepresentable rather than
 * merely unlikely.
 */
public final class TariffPlanBuilder {

    private final String id;
    private final String name;
    private final TariffKind kind;

    private BigDecimal baseFee = Money.of("20.00");
    private BigDecimal perHourFee = Money.of("30.00");
    private BigDecimal dailyCap;
    private int graceMinutes = 15;
    private BigDecimal flatFee;
    private LocalTime earlyBirdInBefore;
    private LocalTime earlyBirdOutAfter;
    private BigDecimal surgeMultiplier = BigDecimal.ONE;
    private boolean active = false;

    public TariffPlanBuilder(String id, String name, TariffKind kind) {
        this.id = id;
        this.name = name;
        this.kind = kind;
    }

    public TariffPlanBuilder baseFee(BigDecimal fee) {
        this.baseFee = fee;
        return this;
    }

    public TariffPlanBuilder perHour(BigDecimal fee) {
        this.perHourFee = fee;
        return this;
    }

    public TariffPlanBuilder dailyCap(BigDecimal cap) {
        this.dailyCap = cap;
        return this;
    }

    public TariffPlanBuilder graceMinutes(int minutes) {
        this.graceMinutes = minutes;
        return this;
    }

    public TariffPlanBuilder flatFee(BigDecimal fee) {
        this.flatFee = fee;
        return this;
    }

    public TariffPlanBuilder earlyBirdWindow(LocalTime inBefore, LocalTime outAfter) {
        this.earlyBirdInBefore = inBefore;
        this.earlyBirdOutAfter = outAfter;
        return this;
    }

    public TariffPlanBuilder surgeMultiplier(BigDecimal multiplier) {
        this.surgeMultiplier = multiplier;
        return this;
    }

    public TariffPlanBuilder active(boolean active) {
        this.active = active;
        return this;
    }

    /** Validate every invariant, then construct. Throws on any violation. */
    public TariffPlan build() {
        require(baseFee.signum() >= 0, "base fee cannot be negative");
        require(perHourFee.signum() >= 0, "per-hour fee cannot be negative");
        require(graceMinutes >= 0 && graceMinutes <= 60, "grace must be 0–60 minutes");
        require(surgeMultiplier.compareTo(BigDecimal.ONE) >= 0, "surge multiplier must be ≥ 1.0");
        if (dailyCap != null) {
            require(dailyCap.compareTo(baseFee) >= 0, "daily cap cannot be below the base fee");
        }
        if (kind == TariffKind.EARLY_BIRD) {
            require(flatFee != null && flatFee.signum() > 0, "early-bird needs a positive flat fee");
            require(earlyBirdInBefore != null && earlyBirdOutAfter != null,
                    "early-bird needs an in-before and out-after time");
            require(earlyBirdInBefore.isBefore(earlyBirdOutAfter),
                    "early-bird in-before must be earlier than out-after");
        }
        if (kind == TariffKind.DAILY_CAP) {
            require(dailyCap != null, "daily-cap plan needs a cap");
        }
        return new TariffPlan(id, name, kind, baseFee, perHourFee, dailyCap, graceMinutes,
                flatFee, earlyBirdInBefore, earlyBirdOutAfter, surgeMultiplier, active);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Tariff rejected: " + message);
        }
    }
}
