package com.parksense.persistence;

import com.parksense.tickets.FeeComponent;

import java.math.BigDecimal;
import java.util.List;

/**
 * A fee chain reconstructed from persisted lines (used when reloading a
 * closed ticket from the database). Open tickets are deliberately left
 * unpriced so checkout reprices them against the live clock.
 */
public final class RestoredFee implements FeeComponent {

    private final BigDecimal total;
    private final String describe;
    private final List<FeeLine> lines;

    public RestoredFee(BigDecimal total, String describe, List<FeeLine> lines) {
        this.total = total;
        this.describe = describe;
        this.lines = List.copyOf(lines);
    }

    @Override
    public BigDecimal amount() {
        return total;
    }

    @Override
    public String describe() {
        return describe;
    }

    @Override
    public List<FeeLine> lines() {
        return lines;
    }
}
