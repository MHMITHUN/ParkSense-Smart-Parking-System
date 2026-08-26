package com.parksense.tickets.addon;

import com.parksense.common.Money;
import com.parksense.tickets.FeeComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Electricity delivered at an EV bay, priced per kWh (GoF Decorator). The
 * energy amount comes from the bay's charging session.
 */
public final class EvChargeAddon implements FeeComponent {

    private final FeeComponent inner;
    private final BigDecimal kWh;
    private final BigDecimal ratePerKWh;

    public EvChargeAddon(FeeComponent inner, BigDecimal kWh, BigDecimal ratePerKWh) {
        this.inner = inner;
        this.kWh = kWh;
        this.ratePerKWh = ratePerKWh;
    }

    @Override
    public BigDecimal amount() {
        return inner.amount().add(charge());
    }

    @Override
    public String describe() {
        return inner.describe() + " + EV charge";
    }

    @Override
    public List<FeeLine> lines() {
        List<FeeLine> all = new ArrayList<>(inner.lines());
        all.add(new FeeLine("EV top-up " + kWh + " kWh @ " + ratePerKWh + "/kWh", charge()));
        return all;
    }

    private BigDecimal charge() {
        return Money.round(kWh.multiply(ratePerKWh));
    }
}
