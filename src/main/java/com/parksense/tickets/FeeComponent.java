package com.parksense.tickets;

import java.math.BigDecimal;
import java.util.List;

/**
 * A chargeable line on a ticket (GoF Decorator). The base parking fee and
 * every add-on service implement this interface; add-ons wrap another
 * component and add their own amount and line, so any combination of
 * services can stack on any ticket without the ticket knowing them.
 */
public interface FeeComponent {

    /** Total of this component including everything it wraps. */
    BigDecimal amount();

    /** Short label for the receipt header line. */
    String describe();

    /** Ordered, flat breakdown: wrapped lines first, own line last. */
    List<FeeLine> lines();

    /** One receipt row. */
    record FeeLine(String label, BigDecimal amount) {
    }
}
