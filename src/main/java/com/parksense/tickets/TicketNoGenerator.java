package com.parksense.tickets;

import com.parksense.time.SimClock;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sequential, human-readable ticket numbers:
 * {@code PS-260825-00042} — prefix, date, daily sequence. Unique per run
 * without any database sequence.
 */
public final class TicketNoGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final SimClock clock;
    private final AtomicLong sequence = new AtomicLong();

    public TicketNoGenerator(SimClock clock) {
        this.clock = clock;
    }

    public String next() {
        long n = sequence.incrementAndGet();
        return "PS-" + clock.today().format(DATE) + "-" + String.format("%05d", n);
    }

    /** Current sequence value (persisted so numbers never repeat across restarts). */
    public long currentCount() {
        return sequence.get();
    }

    /** Restore the sequence after loading persisted state. */
    public void restore(long count) {
        sequence.set(count);
    }
}
