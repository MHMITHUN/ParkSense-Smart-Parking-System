package com.parksense.tariff.strategy;

import com.parksense.common.Money;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * All-day commuters: arrive before the cut-off, leave after the afternoon
 * threshold, pay one flat rate regardless of hours.
 */
public final class EarlyBirdTariff implements TariffStrategy {

    @Override
    public TariffKind kind() {
        return TariffKind.EARLY_BIRD;
    }

    @Override
    public BigDecimal compute(TariffPlan plan, FeeRequest request) {
        return Money.round(plan.flatFee().multiply(vehicleFactor(request)));
    }

    @Override
    public String explain(TariffPlan plan, FeeRequest request) {
        return "Early-bird flat (" + plan.earlyBirdInBefore() + " in / "
                + plan.earlyBirdOutAfter() + " out) @ " + plan.name();
    }

    /** Eligibility rule — the selector consults this before choosing the strategy. */
    public boolean eligible(TariffPlan plan, FeeRequest request) {
        LocalDateTime in = LocalDateTime.ofInstant(request.entryAt(), request.zone());
        LocalDateTime out = LocalDateTime.ofInstant(request.exitAt(), request.zone());
        return !in.toLocalTime().isAfter(plan.earlyBirdInBefore())
                && out.toLocalTime().isAfter(plan.earlyBirdOutAfter())
                && in.toLocalDate().equals(out.toLocalDate());
    }
}
