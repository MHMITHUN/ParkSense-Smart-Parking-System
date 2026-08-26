package com.parksense.tickets.state;

import com.parksense.tickets.Ticket;

/** Terminal: the vehicle has left and the slot is free again. */
public final class ExitedState implements TicketState {

    public static final ExitedState INSTANCE = new ExitedState();

    private ExitedState() {
    }

    @Override
    public String name() {
        return "EXITED";
    }

    @Override
    public String toString() {
        return name();
    }
}
