package com.parksense.tickets.addon;

import com.parksense.tickets.FeeComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Exterior + interior wash booked at the checkout kiosk (GoF Decorator).
 */
public final class CarWashAddon implements FeeComponent {

    private final FeeComponent inner;
    private final BigDecimal price;

    public CarWashAddon(FeeComponent inner, BigDecimal price) {
        this.inner = inner;
        this.price = price;
    }

    @Override
    public BigDecimal amount() {
        return inner.amount().add(price);
    }

    @Override
    public String describe() {
        return inner.describe() + " + car wash";
    }

    @Override
    public List<FeeLine> lines() {
        List<FeeLine> all = new ArrayList<>(inner.lines());
        all.add(new FeeLine("Car wash (exterior + interior)", price));
        return all;
    }
}
