package com.parksense.tariff;

import com.parksense.common.Money;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

/**
 * A configured pricing plan. Plans are pure data — the arithmetic of a kind
 * lives in its {@code TariffStrategy} — and are created only through
 * {@code TariffPlanBuilder}, which refuses half-configured combinations.
 */
public final class TariffPlan {

    private final String id;
    private final String name;
    private final TariffKind kind;
    private final BigDecimal baseFee;
    private final BigDecimal perHourFee;
    private final BigDecimal dailyCap;
    private final int graceMinutes;
    private final BigDecimal flatFee;
    private final LocalTime earlyBirdInBefore;
    private final LocalTime earlyBirdOutAfter;
    private final BigDecimal surgeMultiplier;
    private boolean active;

    /** Public for the builder (same module); callers should use {@code TariffPlanBuilder}. */
    public TariffPlan(String id, String name, TariffKind kind, BigDecimal baseFee,
                      BigDecimal perHourFee, BigDecimal dailyCap, int graceMinutes,
                      BigDecimal flatFee, LocalTime earlyBirdInBefore, LocalTime earlyBirdOutAfter,
                      BigDecimal surgeMultiplier, boolean active) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.kind = Objects.requireNonNull(kind);
        this.baseFee = Money.round(baseFee);
        this.perHourFee = Money.round(perHourFee);
        this.dailyCap = dailyCap == null ? null : Money.round(dailyCap);
        this.graceMinutes = graceMinutes;
        this.flatFee = flatFee == null ? null : Money.round(flatFee);
        this.earlyBirdInBefore = earlyBirdInBefore;
        this.earlyBirdOutAfter = earlyBirdOutAfter;
        this.surgeMultiplier = surgeMultiplier;
        this.active = active;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TariffKind kind() {
        return kind;
    }

    public BigDecimal baseFee() {
        return baseFee;
    }

    public BigDecimal perHourFee() {
        return perHourFee;
    }

    public BigDecimal dailyCap() {
        return dailyCap;
    }

    public int graceMinutes() {
        return graceMinutes;
    }

    public BigDecimal flatFee() {
        return flatFee;
    }

    public LocalTime earlyBirdInBefore() {
        return earlyBirdInBefore;
    }

    public LocalTime earlyBirdOutAfter() {
        return earlyBirdOutAfter;
    }

    public BigDecimal surgeMultiplier() {
        return surgeMultiplier;
    }

    public boolean active() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
