package com.parksense.tickets;

import com.parksense.tickets.state.ActiveState;
import com.parksense.tickets.state.IllegalTransitionException;
import com.parksense.tickets.state.IssuedState;
import com.parksense.tickets.state.TicketState;
import com.parksense.vehicles.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A parking session: from ANPR read at the entry gate to the barrier drop
 * at the exit. The lifecycle rules live in the {@code TicketState} classes
 * (GoF State); this class owns the data and forwards every operation to
 * the current state, so an illegal call is rejected before any field moves.
 */
public final class Ticket {

    private final String ticketNo;
    private final String plate;
    private final VehicleType vehicleType;
    private final boolean accessible;
    private final Instant entryTime;
    private final String entryGateId;
    private final String slotCode;

    private volatile String exitGateId;
    private volatile Instant exitTime;
    private volatile Instant paidAt;
    private volatile String tariffPlanId;
    private volatile String tariffExplain;
    private volatile FeeComponent feeChain;
    private final List<PaymentRecord> payments = new CopyOnWriteArrayList<>();
    private volatile TicketState state = IssuedState.INSTANCE;
    private volatile String voidReason;

    public Ticket(String ticketNo, String plate, VehicleType vehicleType, boolean accessible,
                  Instant entryTime, String entryGateId, String slotCode) {
        this.ticketNo = ticketNo;
        this.plate = plate;
        this.vehicleType = vehicleType;
        this.accessible = accessible;
        this.entryTime = entryTime;
        this.entryGateId = entryGateId;
        this.slotCode = slotCode;
    }

    // ------------------------------------------------------------------
    // State-forwarding operations (the public lifecycle API)
    // ------------------------------------------------------------------

    public void confirmEntry() {
        state.confirmEntry(this);
    }

    public void pay(PaymentRecord payment) {
        state.pay(this, payment);
    }

    public void completeExit(String gateId, Instant at) {
        this.exitGateId = gateId;
        this.exitTime = at;
        state.markExited(this);
    }

    public void reportLost() {
        state.reportLost(this);
    }

    public void voidTicket(String reason) {
        state.voidTicket(this, reason);
    }

    public void reopenFromOverstay() {
        state.reopenFromOverstay(this);
    }

    // ------------------------------------------------------------------
    // Callbacks used by the state classes — never call these directly.
    // ------------------------------------------------------------------

    public void setState(TicketState next) {
        this.state = next;
    }

    public void addPayment(PaymentRecord payment) {
        payments.add(payment);
    }

    public void markPaidAt(Instant at) {
        this.paidAt = at;
    }

    public void recordVoidReason(String reason) {
        this.voidReason = reason;
    }

    // ------------------------------------------------------------------
    // Fee chain (Decorator) management at checkout
    // ------------------------------------------------------------------

    /** Attach or replace the computed fee chain (base fee + any add-ons). */
    public void setFee(FeeComponent chain, String planId, String explain) {
        this.feeChain = chain;
        this.tariffPlanId = planId;
        this.tariffExplain = explain;
    }

    public FeeComponent feeChain() {
        return feeChain;
    }

    public BigDecimal feeTotal() {
        return feeChain == null ? BigDecimal.ZERO : feeChain.amount();
    }

    public List<FeeComponent.FeeLine> feeLines() {
        return feeChain == null ? List.of() : feeChain.lines();
    }

    public BigDecimal totalPaid() {
        return payments.stream()
                .map(PaymentRecord::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    public String ticketNo() {
        return ticketNo;
    }

    public String plate() {
        return plate;
    }

    public VehicleType vehicleType() {
        return vehicleType;
    }

    public boolean accessible() {
        return accessible;
    }

    public Instant entryTime() {
        return entryTime;
    }

    public String entryGateId() {
        return entryGateId;
    }

    public String slotCode() {
        return slotCode;
    }

    public String exitGateId() {
        return exitGateId;
    }

    public Instant exitTime() {
        return exitTime;
    }

    public Instant paidAt() {
        return paidAt;
    }

    public String tariffPlanId() {
        return tariffPlanId;
    }

    public String tariffExplain() {
        return tariffExplain;
    }

    public List<PaymentRecord> payments() {
        return List.copyOf(payments);
    }

    public TicketState state() {
        return state;
    }

    public String stateName() {
        return state.name();
    }

    public String voidReason() {
        return voidReason;
    }

    /** True while the car is still inside — includes PAID (paid, awaiting exit). */
    public boolean isOpen() {
        return state == IssuedState.INSTANCE
                || state == ActiveState.INSTANCE
                || state instanceof com.parksense.tickets.state.LostState
                || state instanceof com.parksense.tickets.state.PaidState;
    }

    /** Guard used by the exit lane before touching the barrier. */
    public void requireOpen(String action) {
        if (!isOpen()) {
            throw new IllegalTransitionException(
                    "Ticket " + ticketNo + " is " + stateName() + " — cannot " + action);
        }
    }

    /** Accept a report visitor: the ticket first, then each fee line (GoF Visitor). */
    public void accept(TicketVisitor visitor) {
        visitor.visitTicket(this);
        if (feeChain != null) {
            feeChain.lines().forEach(visitor::visitFeeLine);
        }
    }
}
