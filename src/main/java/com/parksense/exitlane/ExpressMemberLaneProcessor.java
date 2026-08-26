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

import java.math.BigDecimal;
import java.util.List;

/**
 * The express lane (Template Method concrete class): pass holders only,
 * no booth stop. The MemberPass strategy prices the stay at zero and the
 * lane records a zero-value settlement so the ticket still walks the legal
 * ACTIVE → PAID → EXITED path.
 */
public final class ExpressMemberLaneProcessor extends ExitProcessor {

    public ExpressMemberLaneProcessor(OccupancyLedger ledger, TicketStore tickets,
                                      TariffSelector selector, MemberRegistry members,
                                      PlateRegistry plates, SimClock clock, AuditTrail audit) {
        super(ledger, tickets, selector, members, plates, clock, audit);
    }

    @Override
    public String laneName() {
        return "EXPRESS-MEMBER";
    }

    @Override
    protected PaymentRecord collectPayment(Ticket ticket, FeeComponent fee,
                                           ExitRequest request, List<String> steps) {
        if (!members.hasValidPass(ticket.plate())) {
            steps.add("plate has no valid pass — express lane refused");
            return null;
        }
        steps.add("valid pass — express settlement (0.00)");
        PaymentRecord record = new PaymentRecord(ticket.ticketNo(), null, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, clock.now());
        ticket.pay(record);
        return record;
    }
}
