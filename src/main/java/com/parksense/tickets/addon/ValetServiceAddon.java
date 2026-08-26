package com.parksense.tickets.addon;

import com.parksense.tickets.FeeComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Valet retrieval service booked on a ticket (GoF Decorator).
 */
public final class ValetServiceAddon implements FeeComponent {

    private final FeeComponent inner;
    private final BigDecimal price;

    public ValetServiceAddon(FeeComponent inner, BigDecimal price) {
        this.inner = inner;
        this.price = price;
    }

    @Override
    public BigDecimal amount() {
        return inner.amount().add(price);
    }

    @Override
    public String describe() {
        return inner.describe() + " + valet";
    }

    @Override
    public List<FeeLine> lines() {
        List<FeeLine> all = new ArrayList<>(inner.lines());
        all.add(new FeeLine("Valet service", price));
        return all;
    }
}
