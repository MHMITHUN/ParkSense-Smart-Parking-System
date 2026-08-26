package com.parksense.occupancy.board;

import com.parksense.lot.Floor;
import com.parksense.lot.Zone;
import com.parksense.occupancy.OccupancyEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Level-mounted board: free count for one floor with a per-zone breakdown,
 * so a driver circling the ramp knows where to head before arriving there.
 */
public final class FloorBoard implements DisplayBoard {

    private final Floor floor;
    private volatile Instant updatedAt;

    public FloorBoard(Floor floor) {
        this.floor = floor;
        this.updatedAt = Instant.now();
    }

    @Override
    public String boardId() {
        return "BOARD-" + floor.code();
    }

    @Override
    public String title() {
        return "Level " + floor.code() + " Board";
    }

    @Override
    public void onOccupancyEvent(OccupancyEvent event) {
        if (floor.code().equals(event.floor())) {
            updatedAt = event.at();
        }
    }

    @Override
    public DisplaySnapshot render() {
        List<String> lines = new ArrayList<>();
        lines.add("LEVEL " + floor.code() + "  FREE: " + floor.freeSlots() + " / " + floor.totalSlots());
        List<String> zoneParts = new ArrayList<>();
        for (Zone zone : floor.zones()) {
            zoneParts.add(zone.code() + " " + zone.freeSlots());
        }
        lines.add(String.join("  |  ", zoneParts));
        return new DisplaySnapshot(boardId(), title(), lines, updatedAt);
    }
}
