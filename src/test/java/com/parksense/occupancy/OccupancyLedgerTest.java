package com.parksense.occupancy;

import com.parksense.lot.Slot;
import com.parksense.lot.SlotState;
import com.parksense.testsupport.Harness;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Singleton — one ledger, legal transitions only, concurrency safe. */
class OccupancyLedgerTest {

    private final Harness h = new Harness();

    @Test
    void getInstanceAlwaysReturnsSameObject() {
        assertSame(OccupancyLedger.getInstance(), OccupancyLedger.getInstance());
    }

    @Test
    void reserveForGivesMotorcycleAMotorcycleSlot() {
        Optional<Slot> slot = h.ledger.reserveFor(VehicleType.MOTORCYCLE, false, "MC-1", "T1", h.clock.now());
        assertTrue(slot.isPresent());
        assertEquals(SlotState.RESERVED, slot.get().state());
        assertEquals(com.parksense.lot.SlotType.MOTORCYCLE, slot.get().type());
    }

    @Test
    void reserveForPrefersChargerForEvThenStandard() {
        Optional<Slot> ev = h.ledger.reserveFor(VehicleType.EV, false, "EV-1", "T1", h.clock.now());
        assertEquals(com.parksense.lot.SlotType.EV_CHARGE, ev.orElseThrow().type());
        Optional<Slot> ev2 = h.ledger.reserveFor(VehicleType.EV, false, "EV-2", "T2", h.clock.now());
        assertEquals(com.parksense.lot.SlotType.STANDARD, ev2.orElseThrow().type());
    }

    @Test
    void confirmThenReleaseIsTheLegalLifecycle() {
        Slot slot = h.ledger.reserveFor(VehicleType.CAR, false, "C-1", "T1", h.clock.now()).orElseThrow();
        h.ledger.confirmArrival(slot, h.clock.now());
        assertEquals(SlotState.OCCUPIED, slot.state());
        h.ledger.release(slot, "exit");
        assertEquals(SlotState.FREE, slot.state());
        assertNull(slot.currentPlate());
    }

    @Test
    void illegalTransitionIsRejected() {
        Slot slot = h.ledger.reserveFor(VehicleType.CAR, false, "C-1", "T1", h.clock.now()).orElseThrow();
        h.ledger.release(slot, "exit");
        assertThrows(IllegalStateException.class, () -> h.ledger.confirmArrival(slot, h.clock.now()));
    }

    @Test
    void outOfServiceRoundTripAndGuard() {
        Slot slot = h.lot.slots(s -> s.type() == com.parksense.lot.SlotType.STANDARD).get(0);
        h.ledger.markOutOfService(slot.code());
        assertEquals(SlotState.OUT_OF_SERVICE, slot.state());
        h.ledger.returnToService(slot.code());
        assertEquals(SlotState.FREE, slot.state());
        Slot busy = h.ledger.reserveFor(VehicleType.CAR, false, "C-9", "T9", h.clock.now()).orElseThrow();
        assertThrows(IllegalStateException.class, () -> h.ledger.markOutOfService(busy.code()));
    }

    @Test
    void concurrentAllocationNeverDoubleSells() throws Exception {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        var assigned = new java.util.concurrent.CopyOnWriteArrayList<String>();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    h.ledger.reserveFor(VehicleType.CAR, false, "P", "T", h.clock.now())
                            .ifPresent(s -> assigned.add(s.code()));
                } catch (InterruptedException ignored) {
                }
            });
        }
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(assigned.size(), assigned.stream().distinct().count(),
                "no slot may be assigned twice");
    }

    @Test
    void everyTransitionPublishesAnEvent() {
        int before = h.feed.size();
        Slot slot = h.ledger.reserveFor(VehicleType.CAR, false, "E-1", "TE", h.clock.now()).orElseThrow();
        h.ledger.confirmArrival(slot, h.clock.now());
        h.ledger.release(slot, "done");
        assertTrue(h.feed.size() >= before + 3);
    }
}
