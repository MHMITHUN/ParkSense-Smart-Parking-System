package com.parksense.exitlane;

import com.parksense.audit.AuditTrail;
import com.parksense.common.Money;
import com.parksense.members.MemberRegistry;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.store.TicketStore;
import com.parksense.tariff.TariffSelector;
import com.parksense.tickets.FeeComponent;
import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;
import com.parksense.tickets.addon.BaseParkingFee;
import com.parksense.tickets.addon.LostTicketPenalty;
import com.parksense.time.SimClock;
import com.parksense.vehicles.PlateRegistry;

import java.math.BigDecimal;
import java.util.List;

/**
 * The lost-ticket path (Template Method concrete class): the driver
 * identifies by plate, the ticket is marked LOST, and the fare becomes a
 * fixed penalty plus one day rate — the stay duration is unprovable, so
 * the plan charges for the worst reasonable day.
 */
public final class LostTicketProcessor extends ExitProcessor {

    private final BigDecimal penalty = Money.of("500.00");
    private final BigDecimal dayRate = Money.of("300.00");

    public LostTicketProcessor(OccupancyLedger ledger, TicketStore tickets,
                               TariffSelector selector, MemberRegistry members,
                               PlateRegistry plates, SimClock clock, AuditTrail audit) {
        super(ledger, tickets, selector, members, plates, clock, audit);
    }

    @Override
    public String laneName() {
        return "LOST-TICKET-DESK";
    }

    @Override
    protected Ticket fetchTicket(ExitRequest request, List<String> steps) {
        Ticket ticket = tickets.findOpenByPlate(request.plateOrTicketNo()).orElse(null);
        steps.add(ticket == null ? "lost-ticket: no open stay for plate"
                : "lost-ticket: open stay found for plate");
        return ticket;
    }

    @Override
    protected String verifyState(Ticket ticket, ExitRequest request, List<String> steps) {
        if ("LOST".equals(ticket.stateName())) {
            steps.add("ticket already marked LOST");
            return null;
        }
        ticket.reportLost();
        steps.add("ticket marked LOST — penalty tariff applies");
        audit.record("system", "TICKET_LOST", ticket.ticketNo() + " reported lost", true);
        return null;
    }

    @Override
    protected FeeComponent computeFee(Ticket ticket, ExitRequest request, List<String> steps) {
        if (ticket.feeChain() != null && "LOST".equals(ticket.stateName())
                && ticket.feeChain() instanceof LostTicketPenalty) {
            return ticket.feeChain(); // priced already (add-ons kept)
        }
        FeeComponent chain = new LostTicketPenalty(
                new BaseParkingFee(BigDecimal.ZERO, "Parking (duration unprovable)"),
                penalty, dayRate);
        ticket.setFee(chain, "LOST", "lost-ticket penalty tariff");
        steps.add("lost-ticket tariff: penalty " + penalty + " + day rate " + dayRate);
        return chain;
    }

    @Override
    protected PaymentRecord collectPayment(Ticket ticket, FeeComponent fee,
                                           ExitRequest request, List<String> steps) {
        return settle(ticket, fee, request, steps);
    }
}
