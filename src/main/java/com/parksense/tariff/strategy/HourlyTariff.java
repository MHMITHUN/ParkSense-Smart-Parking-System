package com.parksense.tariff.strategy;

import com.parksense.common.Money;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;

import java.math.BigDecimal;

/**
 * The default scheme: a small base fee plus a per-hour charge, with the
 * first {@code graceMinutes} free and partial hours billed whole.
 */
public final class HourlyTariff implements TariffStrategy {

    @Override
    public TariffKind kind() {
        return TariffKind.HOURLY;
    }

    @Override
    public BigDecimal compute(TariffPlan plan, FeeRequest request) {
        long hours = billableHours(plan, request);
        if (hours == 0) {
            return Money.ZERO;
        }
        BigDecimal raw = plan.baseFee()
                .add(plan.perHourFee().multiply(BigDecimal.valueOf(hours)))
                .multiply(vehicleFactor(request));
        return Money.round(raw);
    }

    @Override
    public String explain(TariffPlan plan, FeeRequest request) {
        long hours = billableHours(plan, request);
        if (hours == 0) {
            return "Within " + plan.graceMinutes() + " min grace (" + plan.name() + ")";
        }
        return "Parking " + hours + "h @ " + plan.name()
                + " (base " + plan.baseFee() + " + " + plan.perHourFee() + "/h)";
    }
}
