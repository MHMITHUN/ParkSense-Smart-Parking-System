package com.parksense.tariff.strategy;

import com.parksense.common.Money;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;

import java.math.BigDecimal;

/**
 * Concert-and-matchday pricing: the normal hourly arithmetic multiplied by
 * a configured surge factor while the operator keeps the plan active.
 */
public final class EventSurgeTariff implements TariffStrategy {

    @Override
    public TariffKind kind() {
        return TariffKind.EVENT_SURGE;
    }

    @Override
    public BigDecimal compute(TariffPlan plan, FeeRequest request) {
        long hours = billableHours(plan, request);
        if (hours == 0) {
            return Money.ZERO;
        }
        BigDecimal raw = plan.baseFee()
                .add(plan.perHourFee().multiply(BigDecimal.valueOf(hours)))
                .multiply(plan.surgeMultiplier())
                .multiply(vehicleFactor(request));
        return Money.round(raw);
    }

    @Override
    public String explain(TariffPlan plan, FeeRequest request) {
        long hours = billableHours(plan, request);
        return "Event surge x" + plan.surgeMultiplier() + ", " + hours + "h @ " + plan.name();
    }
}
