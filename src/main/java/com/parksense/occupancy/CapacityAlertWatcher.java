package com.parksense.occupancy;

import com.parksense.lot.SlotType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Watches free capacity and raises control-room alerts (GoF Observer
 * subscriber): "LOT FULL" and "EV BAYS FULL" the moment the last matching
 * slot goes, and a clearing note when space returns.
 */
public final class CapacityAlertWatcher implements OccupancyListener {

    /** One raised or cleared alert. */
    public record Alert(Instant at, String code, String message, boolean active) {
    }

    private final com.parksense.lot.ParkingLot lot;
    private final List<Alert> history = new ArrayList<>();
    private volatile boolean lotFullAnnounced = false;
    private volatile boolean evFullAnnounced = false;

    public CapacityAlertWatcher(com.parksense.lot.ParkingLot lot) {
        this.lot = lot;
    }

    @Override
    public synchronized void onOccupancyEvent(OccupancyEvent event) {
        Instant at = event.at();
        if (lot.freeSlots() == 0) {
            if (!lotFullAnnounced) {
                history.add(new Alert(at, "LOT_FULL", "All slots occupied — entrance board shows FULL", true));
                lotFullAnnounced = true;
            }
        } else if (lotFullAnnounced) {
            history.add(new Alert(at, "LOT_FULL", "Space available again", false));
            lotFullAnnounced = false;
        }

        if (lot.freeSlots(SlotType.EV_CHARGE) == 0) {
            if (!evFullAnnounced) {
                history.add(new Alert(at, "EV_FULL", "All EV charging bays occupied", true));
                evFullAnnounced = true;
            }
        } else if (evFullAnnounced) {
            history.add(new Alert(at, "EV_FULL", "EV bays available again", false));
            evFullAnnounced = false;
        }
    }

    /** Newest-first alert history. */
    public synchronized List<Alert> history() {
        List<Alert> copy = new ArrayList<>(history);
        java.util.Collections.reverse(copy);
        return copy;
    }

    public synchronized boolean lotFull() {
        return lotFullAnnounced;
    }
}
