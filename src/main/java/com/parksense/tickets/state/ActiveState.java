package com.parksense.tickets.state;

import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;

/** The car is inside the lot and the fare is still open. */
public final class ActiveState implements TicketState {

    public static final ActiveState INSTANCE = new ActiveState();

    private ActiveState() {
    }

    @Override
    public String name() {
        return "ACTIVE";
    }

    @Override
    public void pay(Ticket ticket, PaymentRecord payment) {
        ticket.addPayment(payment);
        ticket.markPaidAt(payment.at());
        ticket.setState(PaidState.INSTANCE);
    }

    @Override
    public void reportLost(Ticket ticket) {
        ticket.setState(LostState.INSTANCE);
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
