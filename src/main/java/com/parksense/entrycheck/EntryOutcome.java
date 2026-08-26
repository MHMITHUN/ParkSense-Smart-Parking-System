package com.parksense.entrycheck;

import java.util.List;

/**
 * Result of the entry protocol: the verdict, the driver-facing line, the
 * assigned slot/ticket on success, and the full rule trace for the gate
 * simulator panel.
 */
public record EntryOutcome(boolean accepted, String line, String slotCode,
                           String ticketNo, boolean member, List<String> trace) {

    public static EntryOutcome rejected(String line, List<String> trace) {
        return new EntryOutcome(false, line, null, null, false, trace);
    }
}
