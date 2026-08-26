package com.parksense.tickets.state;

import com.parksense.tickets.Ticket;

/** Terminal: cancelled by an ADMIN (misuse, test entry, goodwill). */
public final class VoidState implements TicketState {

    public static final VoidState INSTANCE = new VoidState();

    private VoidState() {
    }

    @Override
    public String name() {
        return "VOID";
    }

    @Override
    public String toString() {
        return name();
    }
}
