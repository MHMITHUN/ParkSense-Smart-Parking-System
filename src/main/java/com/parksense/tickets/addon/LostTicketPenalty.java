package com.parksense.tickets.addon;

import com.parksense.tickets.FeeComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The fixed penalty applied when a driver cannot present a ticket (GoF
 * Decorator): a lost-ticket fine plus one full day rate replace the
 * unprovable stay duration.
 */
public final class LostTicketPenalty implements FeeComponent {

    private final FeeComponent inner;
    private final BigDecimal penalty;
    private final BigDecimal dayRate;

    public LostTicketPenalty(FeeComponent inner, BigDecimal penalty, BigDecimal dayRate) {
        this.inner = inner;
        this.penalty = penalty;
        this.dayRate = dayRate;
    }

    @Override
    public BigDecimal amount() {
        return inner.amount().add(penalty).add(dayRate);
    }

    @Override
    public String describe() {
        return inner.describe() + " + lost-ticket penalty";
    }

    @Override
    public List<FeeLine> lines() {
        List<FeeLine> all = new ArrayList<>(inner.lines());
        all.add(new FeeLine("Lost ticket administrative penalty", penalty));
        all.add(new FeeLine("Lost ticket day rate (max 24h assumed)", dayRate));
        return all;
    }
}
