package com.parksense.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The system's single source of "now". Every fee calculation, ticket
 * timestamp and report window goes through this clock. Wrapping the JDK
 * clock lets tests freeze or fast-forward time (a 26-hour parking stay can
 * be simulated in milliseconds) without touching business code.
 */
public final class SimClock {

    private volatile Clock clock = Clock.systemDefaultZone();

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime localNow() {
        return LocalDateTime.now(clock);
    }

    public ZoneId zone() {
        return clock.getZone();
    }

    public Clock clock() {
        return clock;
    }

    /** Freeze time at a fixed instant (tests, deterministic seeds). */
    public void fixAt(Instant fixed) {
        clock = Clock.fixed(fixed, ZoneId.systemDefault());
    }

    /** Fast-forward simulated time by the given duration. */
    public void advance(Duration by) {
        clock = Clock.offset(clock, by);
    }

    /** Return to the real system clock. */
    public void resetToSystem() {
        clock = Clock.systemDefaultZone();
    }
}
