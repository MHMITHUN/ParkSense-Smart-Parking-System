package com.parksense.exitlane;

import com.parksense.audit.AuditTrail;
import com.parksense.common.Money;
import com.parksense.members.MemberRegistry;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.store.TicketStore;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffSelector;
import com.parksense.tariff.strategy.TariffStrategy;
import com.parksense.tickets.FeeComponent;
import com.parksense.tickets.PaymentMethod;
import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;
import com.parksense.tickets.addon.BaseParkingFee;
import com.parksense.time.SimClock;
import com.parksense.vehicles.PlateRegistry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * GoF Template Method. Every exit lane runs the same fixed pipeline —
 * find ticket → verify state → price → settle → free the slot → open the
 * barrier — because the order encodes safety rules ("the slot frees only
 * after settlement"). Lanes differ only in the hook methods, so a new lane
 * type can never invent an unsafe order.
 */
public abstract class ExitProcessor {

    protected final OccupancyLedger ledger;
    protected final TicketStore tickets;
    protected final TariffSelector selector;
    protected final MemberRegistry members;
    protected final PlateRegistry plates;
    protected final SimClock clock;
    protected final AuditTrail audit;

    protected ExitProcessor(OccupancyLedger ledger, TicketStore tickets, TariffSelector selector,
                            MemberRegistry members, PlateRegistry plates, SimClock clock,
                            AuditTrail audit) {
        this.ledger = ledger;
        this.tickets = tickets;
        this.selector = selector;
        this.members = members;
        this.plates = plates;
        this.clock = clock;
        this.audit = audit;
    }

    /** Lane identity for logs. */
    public abstract String laneName();

    // ------------------------------------------------------------------
    // The template — final, the step order is the safety rule
    // ------------------------------------------------------------------

    public final ExitOutcome process(ExitRequest request) {
        List<String> steps = new ArrayList<>();
        steps.add("[" + laneName() + "] exit requested at gate " + request.gateId());

        Ticket ticket = fetchTicket(request, steps);
        if (ticket == null) {
            steps.add("REJECT no matching open ticket");
            return ExitOutcome.rejected("NO OPEN TICKET FOR THIS VEHICLE", steps);
        }

        String problem = verifyState(ticket, request, steps);
        if (problem != null) {
            steps.add("REJECT " + problem);
            return ExitOutcome.rejected(problem, steps);
        }

        FeeComponent fee = computeFee(ticket, request, steps);

        PaymentRecord payment = collectPayment(ticket, fee, request, steps);
        if (payment == null) {
            steps.add("REJECT payment required before exit");
            return ExitOutcome.rejected("PAYMENT REQUIRED — SETTLE AT BOOTH", steps);
        }

        completeExit(ticket, request, steps);
        return new ExitOutcome(true, exitLine(ticket, payment), ticket.ticketNo(),
                payment.amount(), steps);
    }

    // ------------------------------------------------------------------
    // Hooks
    // ------------------------------------------------------------------

    /** Locate the open ticket by number or plate; null = none. */
    protected Ticket fetchTicket(ExitRequest request, List<String> steps) {
        String ref = request.plateOrTicketNo() == null ? "" : request.plateOrTicketNo().trim();
        Ticket ticket = tickets.find(ref.toUpperCase()).orElse(null);
        if (ticket == null) {
            // plates are stored normalised (upper case, dashes)
            ticket = tickets.findOpenByPlate(ref.toUpperCase().replace(' ', '-')).orElse(null);
        }
        steps.add(ticket == null ? "ticket: not found"
                : "ticket: " + ticket.ticketNo() + " (" + ticket.stateName() + ")");
        return ticket;
    }

    /** @return null when fine, else the rejection line. */
    protected String verifyState(Ticket ticket, ExitRequest request, List<String> steps) {
        return switch (ticket.stateName()) {
            case "EXITED" -> "ALREADY EXITED";
            case "VOID" -> "TICKET VOIDED";
            case "PAID" -> checkOverstay(ticket, steps);
            default -> null; // ACTIVE, ISSUED, LOST — all settleable at the lane
        };
    }

    private String checkOverstay(Ticket ticket, List<String> steps) {
        int grace = selector.select(FeeRequest.of(ticket.entryTime(), clock.now(), ticket.vehicleType()),
                members.hasValidPass(ticket.plate())).plan().graceMinutes();
        Duration sincePaid = Duration.between(ticket.paidAt(), clock.now());
        if (sincePaid.toMinutes() > grace) {
            ticket.reopenFromOverstay();
            ticket.setFee(null, null, null); // stale pricing — force a fresh computation
            steps.add("overstay " + sincePaid.toMinutes() + "m > grace " + grace
                    + "m — fare reopened (state PAID → ACTIVE)");
            return null;
        }
        steps.add("paid " + sincePaid.toMinutes() + "m ago — within grace, free exit");
        return null;
    }

    /** Price the stay and attach the fee chain to the ticket. */
    protected FeeComponent computeFee(Ticket ticket, ExitRequest request, List<String> steps) {
        if (ticket.feeChain() != null) {
            // already priced (kiosk checkout with add-ons) — keep the settled chain
            steps.add("tariff: already priced " + ticket.feeTotal()
                    + " (" + ticket.tariffExplain() + ")");
            return ticket.feeChain();
        }
        FeeRequest feeRequest = FeeRequest.of(ticket.entryTime(), clock.now(), ticket.vehicleType());
        boolean memberPass = members.hasValidPass(ticket.plate());
        TariffSelector.Selected selected = selector.select(feeRequest, memberPass);
        BigDecimal amount = selected.strategy().compute(selected.plan(), feeRequest);
        FeeComponent chain = new BaseParkingFee(amount, selected.strategy().explain(selected.plan(), feeRequest));
        ticket.setFee(chain, selected.plan().id(), selected.reason());
        steps.add("tariff: " + selected.plan().name() + " (" + selected.reason() + ") → " + amount);
        return chain;
    }

    /**
     * Settle the fare. Return the payment record, or null to reject (no
     * payment possible at this lane). Zero-fee exits still return a
     * zero-amount record so the ticket legally reaches PAID.
     */
    protected abstract PaymentRecord collectPayment(Ticket ticket, FeeComponent fee,
                                                    ExitRequest request, List<String> steps);

    /** Shared helper: record a payment and move the ticket to PAID. */
    protected PaymentRecord settle(Ticket ticket, FeeComponent fee, ExitRequest request,
                                   List<String> steps) {
        BigDecimal due = Money.round(fee.amount());
        BigDecimal tendered = request.tendered() == null ? due : request.tendered();
        if (tendered.compareTo(due) < 0) {
            steps.add("tender " + tendered + " < due " + due);
            return null;
        }
        PaymentMethod method = request.method() == null ? PaymentMethod.CASH : request.method();
        PaymentRecord payment = new PaymentRecord(ticket.ticketNo(), method, due,
                tendered, Money.round(tendered.subtract(due)), clock.now());
        ticket.pay(payment);
        steps.add("payment: " + method.label() + " " + due
                + (payment.changeDue().signum() > 0 ? " (change " + payment.changeDue() + ")" : ""));
        audit.record("system", "TICKET_PAID", ticket.ticketNo() + " settled " + due, true);
        return payment;
    }

    /** Free the slot, close out registries, mark the ticket exited. */
    protected void completeExit(Ticket ticket, ExitRequest request, List<String> steps) {
        ledger.slot(ticket.slotCode()).ifPresent(slot -> {
            ledger.release(slot, "exit completed at " + request.gateId());
            steps.add("slot " + slot.code() + " freed");
        });
        plates.markExited(ticket.plate());
        ticket.completeExit(request.gateId(), clock.now());
        steps.add("ticket " + ticket.ticketNo() + " EXITED at " + request.gateId());
        audit.record("system", "TICKET_EXITED", ticket.ticketNo() + " via " + request.gateId(), true);
    }

    protected String exitLine(Ticket ticket, PaymentRecord payment) {
        BigDecimal amount = payment == null ? BigDecimal.ZERO : payment.amount();
        return amount.signum() == 0
                ? "PASS — NO CHARGE. GOODBYE"
                : "FEE " + amount + " — RECEIPT PRINTED. GOODBYE";
    }
}
