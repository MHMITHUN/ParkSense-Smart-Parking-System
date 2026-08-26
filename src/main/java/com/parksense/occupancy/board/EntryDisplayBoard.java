package com.parksense.occupancy.board;

import com.parksense.lot.SlotType;
import com.parksense.occupancy.OccupancyEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The big board at the lot entrance: total free count plus a per-type
 * breakdown, flipping to a FULL banner when nothing is available.
 */
public final class EntryDisplayBoard implements DisplayBoard {

    private final com.parksense.lot.ParkingLot lot;
    private volatile Instant updatedAt;

    public EntryDisplayBoard(com.parksense.lot.ParkingLot lot) {
        this.lot = lot;
        this.updatedAt = Instant.now();
    }

    @Override
    public String boardId() {
        return "BOARD-ENTRANCE";
    }

    @Override
    public String title() {
        return "Entrance Board";
    }

    @Override
    public void onOccupancyEvent(OccupancyEvent event) {
        updatedAt = event.at();
    }

    @Override
    public DisplaySnapshot render() {
        List<String> lines = new ArrayList<>();
        int free = lot.freeSlots();
        if (free == 0) {
            lines.add("*** LOT FULL — TRY AGAIN LATER ***");
        } else {
            lines.add("FREE SLOTS: " + free + " / " + lot.totalSlots());
        }
        lines.add("STD " + lot.freeSlots(SlotType.STANDARD)
                + "  CMP " + lot.freeSlots(SlotType.COMPACT)
                + "  ACC " + lot.freeSlots(SlotType.ACCESSIBLE)
                + "  EV " + lot.freeSlots(SlotType.EV_CHARGE)
                + "  MC " + lot.freeSlots(SlotType.MOTORCYCLE));
        return new DisplaySnapshot(boardId(), title(), lines, updatedAt);
    }
}
