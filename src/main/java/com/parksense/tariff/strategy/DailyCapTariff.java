package com.parksense.tariff.strategy;

import com.parksense.common.Money;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;

import java.math.BigDecimal;

/**
 * Hourly billing with a safety net: charges stop at the daily cap for every
 * started 24-hour block, so a lost car does not produce a lost-week bill.
 */
public final class DailyCapTariff implements TariffStrategy {

    @Override
    public TariffKind kind() {
        return TariffKind.DAILY_CAP;
    }

    @Override
    public BigDecimal compute(TariffPlan plan, FeeRequest request) {
        long hours = billableHours(plan, request);
        if (hours == 0) {
            return Money.ZERO;
        }
        BigDecimal uncapped = plan.baseFee()
                .add(plan.perHourFee().multiply(BigDecimal.valueOf(hours)));
        long dayBlocks = (hours + 23) / 24;
        BigDecimal capTotal = plan.dailyCap().multiply(BigDecimal.valueOf(dayBlocks));
        BigDecimal capped = uncapped.min(capTotal);
        return Money.round(capped.multiply(vehicleFactor(request)));
    }

    @Override
    public String explain(TariffPlan plan, FeeRequest request) {
        long hours = billableHours(plan, request);
        if (hours == 0) {
            return "Within " + plan.graceMinutes() + " min grace (" + plan.name() + ")";
        }
        long dayBlocks = (hours + 23) / 24;
        return "Parking " + hours + "h @ " + plan.name() + " (cap " + plan.dailyCap()
                + " x " + dayBlocks + " day-blocks)";
    }
}
