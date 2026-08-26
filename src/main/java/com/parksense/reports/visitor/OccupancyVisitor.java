package com.parksense.reports.visitor;

import com.parksense.lot.Floor;
import com.parksense.lot.Slot;
import com.parksense.lot.SlotState;
import com.parksense.lot.SlotVisitor;
import com.parksense.lot.Zone;

import java.util.ArrayList;
import java.util.List;

/**
 * GoF Visitor over the lot composite: counts each zone's slots by state —
 * the occupancy table report is just this visitor's collected rows.
 */
public final class OccupancyVisitor implements SlotVisitor {

    private final List<List<String>> rows = new ArrayList<>();

    @Override
    public void visitZone(Zone zone) {
        int total = zone.totalSlots();
        int occupied = 0;
        int reserved = 0;
        int free = 0;
        int out = 0;
        for (Slot slot : zone.slots()) {
            SlotState state = slot.state();
            switch (state) {
                case OCCUPIED -> occupied++;
                case RESERVED -> reserved++;
                case FREE -> free++;
                case OUT_OF_SERVICE -> out++;
            }
        }
        rows.add(List.of(zone.code(), String.valueOf(total), String.valueOf(occupied),
                String.valueOf(reserved), String.valueOf(free), String.valueOf(out)));
    }

    public List<List<String>> rows() {
        return List.copyOf(rows);
    }
}
