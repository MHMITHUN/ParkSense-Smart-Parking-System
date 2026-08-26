package com.parksense.reports.visitor;

import com.parksense.lot.Floor;
import com.parksense.lot.Slot;
import com.parksense.lot.SlotState;
import com.parksense.lot.SlotVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GoF Visitor over the lot composite: per-floor utilisation — occupied
 * share of serviceable slots at the moment of the walk.
 */
public final class UtilizationVisitor implements SlotVisitor {

    private final Map<String, int[]> perFloor = new LinkedHashMap<>();
    private final List<List<String>> rows = new ArrayList<>();

    @Override
    public void visitFloor(Floor floor) {
        perFloor.putIfAbsent(floor.code(), new int[2]); // [occupied, serviceable]
    }

    @Override
    public void visitSlot(Slot slot) {
        int[] counts = perFloor.get(slot.code().split("-")[0]);
        if (counts == null) {
            return;
        }
        if (slot.state() != SlotState.OUT_OF_SERVICE) {
            counts[1]++;
            if (slot.state() == SlotState.OCCUPIED) {
                counts[0]++;
            }
        }
    }

    /** Call after the walk to freeze the computed rows. */
    public List<List<String>> rows() {
        if (rows.isEmpty()) {
            perFloor.forEach((floor, counts) -> {
                double pct = counts[1] == 0 ? 0.0 : counts[0] * 100.0 / counts[1];
                rows.add(List.of(floor, String.valueOf(counts[0]), String.valueOf(counts[1]),
                        String.format("%.1f%%", pct)));
            });
        }
        return List.copyOf(rows);
    }
}
