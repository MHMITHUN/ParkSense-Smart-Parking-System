package com.parksense.tariff.strategy;

import com.parksense.common.Money;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;

import java.math.BigDecimal;

/**
 * One pricing algorithm (GoF Strategy). Strategies are stateless — all
 * numbers come from the {@link TariffPlan} — so the checkout code can
 * switch plans per ticket, or the operator can activate a new plan
 * mid-day, without touching any calling code.
 */
public interface TariffStrategy {

    TariffKind kind();

    /** The parking charge (vehicle-class factor already applied), before add-ons. */
    BigDecimal compute(TariffPlan plan, FeeRequest request);

    /** Receipt line explaining the number, e.g. {@code Parking 4h (Standard Hourly)}. */
    String explain(TariffPlan plan, FeeRequest request);

    /** Shared helper: billable hours after grace. */
    default long billableHours(TariffPlan plan, FeeRequest request) {
        return Money.billedHours(request.stay(), plan.graceMinutes());
    }

    /** Shared helper: vehicle-class factor (van 1.5x, motorcycle 0.5x). */
    default BigDecimal vehicleFactor(FeeRequest request) {
        return request.vehicleType().tariffFactor();
    }
}
