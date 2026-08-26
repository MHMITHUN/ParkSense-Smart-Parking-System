package com.parksense.gates;

import com.parksense.controlroom.Colleague;
import com.parksense.controlroom.ParkingMediator;
import com.parksense.tickets.Ticket;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The pay-station colleague: tracks which open tickets still need payment
 * so the dashboard can show the unpaid queue. Purely reactive — the
 * mediator notifies it when tickets are created or settled.
 */
public final class PaymentKiosk implements Colleague {

    private final String id = "KIOSK-01";
    private ParkingMediator mediator;
    private final List<String> awaitingPayment = new CopyOnWriteArrayList<>();
    private volatile String displayLine = "PAY HERE";

    @Override
    public String colleagueId() {
        return id;
    }

    @Override
    public void setMediator(ParkingMediator mediator) {
        this.mediator = mediator;
    }

    public void ticketIssued(Ticket ticket) {
        awaitingPayment.add(ticket.ticketNo());
    }

    public void ticketSettled(Ticket ticket) {
        awaitingPayment.remove(ticket.ticketNo());
    }

    public List<String> awaitingPayment() {
        return List.copyOf(awaitingPayment);
    }

    public String displayLine() {
        return displayLine;
    }

    public void setDisplay(String line) {
        this.displayLine = line;
    }
}
