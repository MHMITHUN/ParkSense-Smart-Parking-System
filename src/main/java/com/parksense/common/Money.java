package com.parksense.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Small money helpers. Every amount in ParkSense is a {@code BigDecimal}
 * in BDT with two decimal places — floating point never touches money.
 */
public final class Money {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    /** Round to the taka paisa (2 dp, half-up). */
    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String amount) {
        return round(new BigDecimal(amount));
    }

    /** Billed hours for a stay: partial hours always round up, grace time is free. */
    public static long billedHours(Duration stay, int graceMinutes) {
        long minutes = stay.toMinutes();
        if (minutes <= graceMinutes) {
            return 0;
        }
        long billable = minutes - graceMinutes;
        return (billable + 59) / 60;
    }

    /** Human-readable stay, e.g. {@code 4h 25m}. */
    public static String formatStay(Duration stay) {
        long h = stay.toHours();
        long m = stay.toMinutesPart();
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }
}
