package com.parksense.occupancy;

import com.parksense.occupancy.board.DisplayBoard;
import com.parksense.occupancy.board.EntryDisplayBoard;
import com.parksense.occupancy.board.FloorBoard;
import com.parksense.testsupport.Harness;
import com.parksense.vehicles.VehicleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** GoF Observer — boards and watchers react to ledger events autonomously. */
class OccupancyObserverTest {

    private final Harness h = new Harness();
    private final EntryDisplayBoard entrance = new EntryDisplayBoard(h.lot);
    private final FloorBoard l1 = new FloorBoard(h.lot.floors().get(0));

    @Test
    void boardsSubscribeAndReRenderOnEvents() {
        h.publisher.subscribe(entrance);
        h.publisher.subscribe(l1);

        var out = h.mediator.handleVehicleEntry("GATE-IN-1",
                new com.parksense.vehicles.Vehicle("OBS-1", VehicleType.CAR, false));
        assertTrue(out.accepted());

        String board = String.join(" | ", entrance.render().lines());
        assertTrue(board.contains("FREE SLOTS: 7"), "one car in → 7 free: " + board);
        String floor = String.join(" | ", l1.render().lines());
        assertTrue(floor.contains("7 / 8"));
    }

    @Test
    void fullLotFlipsEntranceBoardToFull() {
        DisplayBoard board = new EntryDisplayBoard(h.lot);
        h.publisher.subscribe(board);
        fillLot();
        assertTrue(String.join(" ", board.render().lines()).contains("FULL"));
    }

    /** Fill all 8 bays: 5 plain cars, 1 accessible-badge car, 1 EV, 1 motorcycle. */
    private void fillLot() {
        for (int i = 0; i < 5; i++) {
            h.mediator.handleVehicleEntry("GATE-IN-1",
                    new com.parksense.vehicles.Vehicle("FULL-" + i, VehicleType.CAR, false));
        }
        h.mediator.handleVehicleEntry("GATE-IN-1",
                new com.parksense.vehicles.Vehicle("FULL-ACC", VehicleType.CAR, true));
        h.mediator.handleVehicleEntry("GATE-IN-1",
                new com.parksense.vehicles.Vehicle("FULL-EV", VehicleType.EV, false));
        h.mediator.handleVehicleEntry("GATE-IN-1",
                new com.parksense.vehicles.Vehicle("FULL-MC", VehicleType.MOTORCYCLE, false));
    }

    @Test
    void dashboardFeedKeepsNewestFirst() {
        h.mediator.handleVehicleEntry("GATE-IN-1",
                new com.parksense.vehicles.Vehicle("OBS-A", VehicleType.CAR, false));
        h.mediator.handleVehicleEntry("GATE-IN-1",
                new com.parksense.vehicles.Vehicle("OBS-B", VehicleType.CAR, false));
        var latest = h.feed.latest(2);
        assertEquals(2, latest.size());
        assertEquals("OBS-B", latest.get(0).plate());
    }

    @Test
    void capacityWatcherAnnouncesAndClears() {
        fillLot();
        assertTrue(h.alerts.lotFull());
        assertTrue(h.alerts.history().stream().anyMatch(a -> a.code().equals("LOT_FULL") && a.active()));

        // free one slot → clearing alert appears
        var open = h.tickets.open().get(0);
        h.clock.advance(java.time.Duration.ofHours(2));
        h.mediator.handleVehicleExit("GATE-OUT-1",
                com.parksense.exitlane.ExitRequest.paying("GATE-OUT-1", open.plate(),
                        com.parksense.tickets.PaymentMethod.CASH,
                        new java.math.BigDecimal("1000")));
        assertTrue(h.alerts.history().stream()
                .anyMatch(a -> a.code().equals("LOT_FULL") && !a.active()));
    }
}
