package com.parksense.lot;

/**
 * Visitor over the lot tree (GoF Visitor). Report generators implement this
 * interface and are accepted by every {@link LotNode} through
 * {@link LotNode#accept(SlotVisitor)}, which walks the composite depth-first.
 * All methods have empty default bodies, so a report only overrides the
 * levels it actually needs.
 */
public interface SlotVisitor {

    default void visitLot(ParkingLot lot) {
    }

    default void visitFloor(Floor floor) {
    }

    default void visitZone(Zone zone) {
    }

    default void visitSlot(Slot slot) {
    }
}
