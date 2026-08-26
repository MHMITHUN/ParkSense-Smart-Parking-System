package com.parksense.tickets.state;

import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;

/** The driver cannot present the ticket; a penalty fare applies. */
public final class LostState implements TicketState {

    public static final LostState INSTANCE = new LostState();

    private LostState() {
    }

    @Override
    public String name() {
        return "LOST";
    }

    @Override
    public void pay(Ticket ticket, PaymentRecord payment) {
        ticket.addPayment(payment);
        ticket.markPaidAt(payment.at());
        ticket.setState(PaidState.INSTANCE);
    }

    @Override
    public String toString() {
        return name();
    }
}
