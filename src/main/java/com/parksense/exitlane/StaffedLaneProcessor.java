package com.parksense.exitlane;

import com.parksense.audit.AuditTrail;
import com.parksense.members.MemberRegistry;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.store.TicketStore;
import com.parksense.tariff.TariffSelector;
import com.parksense.tickets.FeeComponent;
import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;
import com.parksense.time.SimClock;
import com.parksense.vehicles.PlateRegistry;

import java.util.List;

/**
 * The staffed exit booth (Template Method concrete class). A human takes
 * the fare: anything with a positive balance must be paid here and now —
 * cash with change, card, or mobile. Pre-paid tickets within grace sail
 * through on the template's overstay check alone.
 */
public final class StaffedLaneProcessor extends ExitProcessor {

    public StaffedLaneProcessor(OccupancyLedger ledger, TicketStore tickets, TariffSelector selector,
                                MemberRegistry members, PlateRegistry plates, SimClock clock,
                                AuditTrail audit) {
        super(ledger, tickets, selector, members, plates, clock, audit);
    }

    @Override
    public String laneName() {
        return "STAFFED-BOOTH";
    }

    @Override
    protected PaymentRecord collectPayment(Ticket ticket, FeeComponent fee,
                                           ExitRequest request, List<String> steps) {
        if ("PAID".equals(ticket.stateName())) {
            steps.add("already paid — no booth settlement needed");
            return new PaymentRecord(ticket.ticketNo(), null, ticket.totalPaid(),
                    ticket.totalPaid(), java.math.BigDecimal.ZERO, clock.now());
        }
        return settle(ticket, fee, request, steps);
    }
}
