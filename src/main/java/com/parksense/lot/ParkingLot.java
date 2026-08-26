package com.parksense.lot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The root of the lot tree (GoF Composite): a named parking facility that
 * groups {@link Floor}s. The live map, capacity rollups and every report
 * visitor start their walk here.
 */
public final class ParkingLot implements LotNode {

    private final String id;
    private final String name;
    private final List<Floor> floors;

    public ParkingLot(String id, String name, List<Floor> floors) {
        this.id = id;
        this.name = name;
        this.floors = new ArrayList<>(floors);
        this.floors.sort(Comparator.comparing(Floor::code));
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    @Override
    public String code() {
        return id;
    }

    @Override
    public String label() {
        return name;
    }

    @Override
    public int totalSlots() {
        return floors.stream().mapToInt(LotNode::totalSlots).sum();
    }

    @Override
    public int freeSlots() {
        return floors.stream().mapToInt(LotNode::freeSlots).sum();
    }

    @Override
    public int freeSlots(SlotType type) {
        return floors.stream().mapToInt(f -> f.freeSlots(type)).sum();
    }

    @Override
    public List<Slot> slots() {
        return floors.stream().flatMap(f -> f.slots().stream()).toList();
    }

    @Override
    public List<Slot> slots(Predicate<Slot> filter) {
        return floors.stream().flatMap(f -> f.slots(filter).stream()).toList();
    }

    @Override
    public Optional<Slot> slot(String slotCode) {
        for (Floor floor : floors) {
            Optional<Slot> hit = floor.slot(slotCode);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }

    @Override
    public void accept(SlotVisitor visitor) {
        visitor.visitLot(this);
        floors.forEach(f -> f.accept(visitor));
    }

    public List<Floor> floors() {
        return List.copyOf(floors);
    }

    /** Floor code that owns a slot, e.g. {@code L2} for {@code L2-B-11}. */
    public Optional<String> floorOf(Slot slot) {
        return floors.stream()
                .filter(f -> f.slots(s -> s == slot).size() == 1)
                .map(Floor::code)
                .findFirst();
    }
}
