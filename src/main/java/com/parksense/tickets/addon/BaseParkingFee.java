package com.parksense.tickets.addon;

import com.parksense.tickets.FeeComponent;

import java.math.BigDecimal;
import java.util.List;

/**
 * The concrete core every add-on decorates: the parking charge itself,
 * produced by the active tariff strategy at checkout.
 */
public final class BaseParkingFee implements FeeComponent {

    private final BigDecimal amount;
    private final String describe;

    public BaseParkingFee(BigDecimal amount, String describe) {
        this.amount = amount;
        this.describe = describe;
    }

    @Override
    public BigDecimal amount() {
        return amount;
    }

    @Override
    public String describe() {
        return describe;
    }

    @Override
    public List<FeeLine> lines() {
        return List.of(new FeeLine(describe, amount));
    }
}
