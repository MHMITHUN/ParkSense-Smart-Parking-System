package com.parksense.lot;

import com.parksense.testsupport.Harness;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Composite — uniform rollups and traversal over the lot tree. */
class CompositeLotTest {

    private final Harness h = new Harness();

    @Test
    void treeHasEightSlotsAcrossOneZone() {
        assertEquals(8, h.lot.totalSlots());
        assertEquals(8, h.lot.freeSlots());
        assertEquals(1, h.lot.floors().size());
        assertEquals(4, h.lot.freeSlots(SlotType.STANDARD));
    }

    @Test
    void slotLookupByCodeWorksAtEveryLevel() {
        assertTrue(h.lot.slot("L1-A-05").isPresent());
        assertEquals(SlotType.COMPACT, h.lot.slot("L1-A-05").orElseThrow().type());
        assertTrue(h.lot.slot("L1-A-99").isEmpty());
    }

    @Test
    void visitorWalksEveryNodeDepthFirst() {
        AtomicInteger lots = new AtomicInteger();
        AtomicInteger floors = new AtomicInteger();
        AtomicInteger zones = new AtomicInteger();
        AtomicInteger slots = new AtomicInteger();
        h.lot.accept(new SlotVisitor() {
            @Override
            public void visitLot(ParkingLot lot) {
                lots.incrementAndGet();
            }

            @Override
            public void visitFloor(Floor floor) {
                floors.incrementAndGet();
            }

            @Override
            public void visitZone(Zone zone) {
                zones.incrementAndGet();
            }

            @Override
            public void visitSlot(Slot slot) {
                slots.incrementAndGet();
            }
        });
        assertEquals(1, lots.get());
        assertEquals(1, floors.get());
        assertEquals(1, zones.get());
        assertEquals(8, slots.get());
    }

    @Test
    void filteredQueryMatchesOnlyPredicate() {
        assertEquals(1, h.lot.slots(s -> s.type() == SlotType.EV_CHARGE).size());
        assertEquals(4, h.lot.slots(s -> s.type() == SlotType.STANDARD).size());
    }

    @Test
    void floorOfReportsOwnerFloor() {
        Slot slot = h.lot.slot("L1-A-07").orElseThrow();
        assertEquals("L1", h.lot.floorOf(slot).orElseThrow());
    }
}
