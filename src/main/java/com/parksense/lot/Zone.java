package com.parksense.lot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A labelled block of slots on one floor (GoF Composite — the intermediate
 * node between a {@link Floor} and its {@link Slot}s).
 */
public final class Zone implements LotNode {

    private final String code;
    private final String label;
    private final List<Slot> slotList;

    public Zone(String code, String label, List<Slot> slots) {
        this.code = code;
        this.label = label;
        this.slotList = new ArrayList<>(slots);
        this.slotList.sort(Comparator.comparing(Slot::code));
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
        return slotList.size();
    }

    @Override
    public int freeSlots() {
        return (int) slotList.stream().filter(Slot::isFree).count();
    }

    @Override
    public int freeSlots(SlotType type) {
        return (int) slotList.stream().filter(s -> s.isFree() && s.type() == type).count();
    }

    @Override
    public List<Slot> slots() {
        return List.copyOf(slotList);
    }

    @Override
    public List<Slot> slots(Predicate<Slot> filter) {
        return slotList.stream().filter(filter).toList();
    }

    @Override
    public Optional<Slot> slot(String slotCode) {
        return slotList.stream().filter(s -> s.code().equals(slotCode)).findFirst();
    }

    @Override
    public void accept(SlotVisitor visitor) {
        visitor.visitZone(this);
        slotList.forEach(s -> s.accept(visitor));
    }
}
