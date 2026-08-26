package com.parksense.lot;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Common interface of every node in the parking lot tree (GoF Composite).
 * A {@code ParkingLot} contains {@code Floor}s, a floor contains {@code Zone}s
 * and a zone contains {@code Slot}s — every level answers the same questions,
 * which is what lets the live map, capacity rollups and reports treat the
 * whole lot uniformly.
 */
public interface LotNode {

    /** Stable, human-readable code, e.g. {@code L1}, {@code L1-A}, {@code L1-A-04}. */
    String code();

    /** Display label, e.g. {@code "Level 1 — Zone A"}. */
    String label();

    /** Total number of slots in this subtree. */
    int totalSlots();

    /** Number of FREE slots in this subtree. */
    int freeSlots();

    /** Number of FREE slots of a given type in this subtree. */
    int freeSlots(SlotType type);

    /** All slots in this subtree, in code order. */
    List<Slot> slots();

    /** All slots in this subtree matching the filter. */
    List<Slot> slots(Predicate<Slot> filter);

    /** Locate a slot by its code, e.g. {@code L2-B-11}. */
    Optional<Slot> slot(String slotCode);

    /** Depth-first traversal used by report visitors. */
    void accept(SlotVisitor visitor);
}
