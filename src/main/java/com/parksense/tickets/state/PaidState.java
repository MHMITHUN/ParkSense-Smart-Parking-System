package com.parksense.tickets.state;

import com.parksense.tickets.Ticket;

/** Paid, inside the 15-minute exit grace window. */
public final class PaidState implements TicketState {

    public static final PaidState INSTANCE = new PaidState();

    private PaidState() {
    }

    @Override
    public String name() {
        return "PAID";
    }

    @Override
    public void markExited(Ticket ticket) {
        ticket.setState(ExitedState.INSTANCE);
    }

    @Override
    public void reopenFromOverstay(Ticket ticket) {
        ticket.setState(ActiveState.INSTANCE);
    }

    @Override
    public void voidTicket(Ticket ticket, String reason) {
        ticket.recordVoidReason(reason);
        ticket.setState(VoidState.INSTANCE);
    }

    @Override
    public String toString() {
        return name();
    }
}
