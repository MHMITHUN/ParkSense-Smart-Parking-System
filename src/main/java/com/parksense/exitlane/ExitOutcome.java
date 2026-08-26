package com.parksense.exitlane;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of the exit protocol: verdict, driver-facing line, amount
 * settled, and the ordered step log (the report's sequence diagram in
 * miniature).
 */
public record ExitOutcome(boolean allowed, String line, String ticketNo,
                          BigDecimal charged, List<String> steps) {

    public static ExitOutcome rejected(String line, List<String> steps) {
        return new ExitOutcome(false, line, null, BigDecimal.ZERO, steps);
    }
}
