package com.parksense.lot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * One parking level of the lot (GoF Composite — groups {@link Zone}s).
 */
public final class Floor implements LotNode {

    private final String code;
    private final String label;
    private final List<Zone> zones;

    public Floor(String code, String label, List<Zone> zones) {
        this.code = code;
        this.label = label;
        this.zones = new ArrayList<>(zones);
        this.zones.sort(Comparator.comparing(Zone::code));
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public int totalSlots() {
        return zones.stream().mapToInt(LotNode::totalSlots).sum();
    }

    @Override
    public int freeSlots() {
        return zones.stream().mapToInt(LotNode::freeSlots).sum();
    }

    @Override
    public int freeSlots(SlotType type) {
        return zones.stream().mapToInt(z -> z.freeSlots(type)).sum();
    }

    @Override
    public List<Slot> slots() {
        return zones.stream().flatMap(z -> z.slots().stream()).toList();
    }

    @Override
    public List<Slot> slots(Predicate<Slot> filter) {
        return zones.stream().flatMap(z -> z.slots(filter).stream()).toList();
    }

    @Override
    public Optional<Slot> slot(String slotCode) {
        for (Zone zone : zones) {
            Optional<Slot> hit = zone.slot(slotCode);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }

    @Override
    public void accept(SlotVisitor visitor) {
        visitor.visitFloor(this);
        zones.forEach(z -> z.accept(visitor));
    }

    public List<Zone> zones() {
        return List.copyOf(zones);
    }
}
