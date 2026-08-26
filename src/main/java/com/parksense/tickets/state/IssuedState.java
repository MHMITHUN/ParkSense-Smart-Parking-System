package com.parksense.tickets.state;

import com.parksense.tickets.Ticket;

/** Printed but the barrier has not yet opened. */
public final class IssuedState implements TicketState {

    public static final IssuedState INSTANCE = new IssuedState();

    private IssuedState() {
    }

    @Override
    public String name() {
        return "ISSUED";
    }

    @Override
    public void confirmEntry(Ticket ticket) {
        ticket.setState(ActiveState.INSTANCE);
    }

    @Override
    public String toString() {
        return name();
    }
}
