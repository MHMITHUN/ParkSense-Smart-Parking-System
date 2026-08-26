package com.parksense.lot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A single physical parking slot — the leaf of the composite tree.
 * Slot objects hold current occupancy data; every state change goes through
 * {@code OccupancyLedger}, which is the only writer of {@link #state}.
 */
public final class Slot implements LotNode {

    private final String code;
    private final SlotType type;
    private final String sensorNodeId;

    private volatile SlotState state = SlotState.FREE;
    private volatile String currentTicketNo;
    private volatile String currentPlate;
    private volatile Instant occupiedSince;

    public Slot(String code, SlotType type, String sensorNodeId) {
        this.code = code;
        this.type = type;
        this.sensorNodeId = sensorNodeId;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String label() {
        return code + " (" + type.label() + ")";
    }

    @Override
    public int totalSlots() {
        return 1;
    }

    @Override
    public int freeSlots() {
        return state == SlotState.FREE ? 1 : 0;
    }

    @Override
    public int freeSlots(SlotType slotType) {
        return type == slotType && state == SlotState.FREE ? 1 : 0;
    }

    @Override
    public List<Slot> slots() {
        return List.of(this);
    }

    @Override
    public List<Slot> slots(Predicate<Slot> filter) {
        return filter.test(this) ? List.of(this) : List.of();
    }

    @Override
    public Optional<Slot> slot(String slotCode) {
        return code.equals(slotCode) ? Optional.of(this) : Optional.empty();
    }

    @Override
    public void accept(SlotVisitor visitor) {
        visitor.visitSlot(this);
    }

    public SlotType type() {
        return type;
    }

    public SlotState state() {
        return state;
    }

    /**
     * Transition state. Public so the cross-package {@code OccupancyLedger}
     * can drive transitions — the ledger is the only sanctioned caller, and
     * it validates every step against {@link SlotState#legalNext()}.
     */
    public void transitionTo(SlotState newState) {
        this.state = newState;
    }

    public String sensorNodeId() {
        return sensorNodeId;
    }

    public String currentTicketNo() {
        return currentTicketNo;
    }

    public String currentPlate() {
        return currentPlate;
    }

    public Instant occupiedSince() {
        return occupiedSince;
    }

    public boolean isFree() {
        return state == SlotState.FREE;
    }

    /** Attach occupancy data; called by the ledger when the slot becomes RESERVED/OCCUPIED. */
    public void occupy(String ticketNo, String plate, Instant since) {
        this.currentTicketNo = ticketNo;
        this.currentPlate = plate;
        this.occupiedSince = since;
    }

    /** Clear occupancy data; called by the ledger when the slot returns to FREE. */
    public void vacate() {
        this.currentTicketNo = null;
        this.currentPlate = null;
        this.occupiedSince = null;
    }
}
