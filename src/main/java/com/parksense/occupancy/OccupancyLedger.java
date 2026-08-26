package com.parksense.occupancy;

import com.parksense.lot.ParkingLot;
import com.parksense.lot.Slot;
import com.parksense.lot.SlotState;
import com.parksense.lot.SlotType;
import com.parksense.vehicles.VehicleType;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The single authority over slot state (GoF Singleton — classic
 * private-constructor + {@code getInstance()} form).
 *
 * Two ledgers would happily sell the same slot to two cars, so the process
 * must own exactly one. Every transition is validated against
 * {@link SlotState#legalNext()} and published as an {@link OccupancyEvent}
 * so boards, dashboard and alerts stay in step without anyone polling.
 */
public final class OccupancyLedger {

    private static volatile OccupancyLedger instance;

    private ParkingLot lot;
    private OccupancyEventPublisher publisher;

    private OccupancyLedger() {
        // singleton — no external construction
    }

    public static OccupancyLedger getInstance() {
        if (instance == null) {
            synchronized (OccupancyLedger.class) {
                if (instance == null) {
                    instance = new OccupancyLedger();
                }
            }
        }
        return instance;
    }

    /** Called once at boot to point the ledger at the lot tree and the event hub. */
    public synchronized void bind(ParkingLot lot, OccupancyEventPublisher publisher) {
        this.lot = lot;
        this.publisher = publisher;
    }

    // ------------------------------------------------------------------
    // Allocation (entry flow)
    // ------------------------------------------------------------------

    /**
     * Reserve the best free slot for a vehicle. Preference order keeps
     * specialized slots for the vehicles that need them: a motorcycle never
     * takes a standard bay and an EV gets a charger before a plain bay.
     */
    public synchronized Optional<Slot> reserveFor(VehicleType type, boolean accessible,
                                                  String plate, String ticketNo, Instant now) {
        requireBound();
        for (SlotType preferred : preferenceOrder(type, accessible)) {
            Optional<Slot> candidate = lot.slots().stream()
                    .filter(s -> s.state() == SlotState.FREE && s.type() == preferred)
                    .findFirst();
            if (candidate.isPresent()) {
                Slot slot = candidate.get();
                slot.occupy(ticketNo, plate, now);
                transition(slot, SlotState.RESERVED, "reserved for " + plate);
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    /** Sensor confirmed the car physically entered the reserved slot. */
    public synchronized void confirmArrival(Slot slot, Instant now) {
        if (slot.state() != SlotState.RESERVED) {
            throw new IllegalStateException(
                    "Cannot confirm arrival on a " + slot.state() + " slot (" + slot.code() + ")");
        }
        if (slot.occupiedSince() == null) {
            slot.occupy(slot.currentTicketNo(), slot.currentPlate(), now);
        }
        transition(slot, SlotState.OCCUPIED, "sensor confirmed arrival");
    }

    /** Would {@link #reserveFor} find a slot for this vehicle right now? */
    public synchronized boolean hasSlotFor(VehicleType type, boolean accessible) {
        requireBound();
        for (SlotType preferred : preferenceOrder(type, accessible)) {
            boolean any = lot.slots().stream()
                    .anyMatch(s -> s.state() == SlotState.FREE && s.type() == preferred);
            if (any) {
                return true;
            }
        }
        return false;
    }

    /** The car left the slot (exit completed). */
    public synchronized void release(Slot slot, String reason) {
        slot.vacate();
        transition(slot, SlotState.FREE, reason);
    }

    /** A reservation the driver never used (10-minute no-show). */
    public synchronized void expireReservation(Slot slot) {
        if (slot.state() == SlotState.RESERVED) {
            slot.vacate();
            transition(slot, SlotState.FREE, "reservation expired (no-show)");
        }
    }

    /**
     * Persistence support: put a bay back into its persisted occupancy
     * (used when reloading open tickets from the database). Walks the same
     * legal transitions the live flow uses, so a restored bay is
     * indistinguishable from a freshly reserved one.
     */
    public synchronized void restoreOccupancy(Slot slot, String ticketNo, String plate,
                                              Instant since, boolean arrived) {
        requireBound();
        if (slot.state() != SlotState.FREE) {
            return; // already spoken for — never overwrite live truth
        }
        slot.occupy(ticketNo, plate, since);
        transition(slot, SlotState.RESERVED, "restored from persistent store");
        if (arrived) {
            transition(slot, SlotState.OCCUPIED, "restored arrival");
        }
    }

    // ------------------------------------------------------------------
    // Maintenance
    // ------------------------------------------------------------------

    public synchronized void markOutOfService(String slotCode) {
        slot(slotCode).ifPresent(s -> {
            if (s.state() == SlotState.FREE) {
                transition(s, SlotState.OUT_OF_SERVICE, "taken out of service");
            } else if (s.state() == SlotState.OCCUPIED || s.state() == SlotState.RESERVED) {
                throw new IllegalStateException(
                        "Slot " + slotCode + " is still in use — cannot take it out of service");
            }
        });
    }

    public synchronized void returnToService(String slotCode) {
        slot(slotCode).ifPresent(s -> {
            if (s.state() == SlotState.OUT_OF_SERVICE) {
                transition(s, SlotState.FREE, "returned to service");
            }
        });
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public ParkingLot lot() {
        requireBound();
        return lot;
    }

    public Optional<Slot> slot(String slotCode) {
        requireBound();
        return lot.slot(slotCode);
    }

    public Optional<Slot> slotWithTicket(String ticketNo) {
        requireBound();
        return lot.slots().stream()
                .filter(s -> ticketNo.equals(s.currentTicketNo()))
                .findFirst();
    }

    public List<Slot> occupiedSlots() {
        requireBound();
        return lot.slots(s -> s.state() == SlotState.OCCUPIED);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private List<SlotType> preferenceOrder(VehicleType type, boolean accessible) {
        if (type == VehicleType.MOTORCYCLE) {
            return List.of(SlotType.MOTORCYCLE);
        }
        if (type == VehicleType.EV) {
            return List.of(SlotType.EV_CHARGE, SlotType.STANDARD);
        }
        if (accessible) {
            return List.of(SlotType.ACCESSIBLE, SlotType.STANDARD);
        }
        return switch (type) {
            case CAR -> List.of(SlotType.COMPACT, SlotType.STANDARD);
            case SUV, VAN -> List.of(SlotType.STANDARD);
            default -> List.of(SlotType.STANDARD);
        };
    }

    private void transition(Slot slot, SlotState newState, String reason) {
        SlotState old = slot.state();
        boolean legal = Arrays.stream(old.legalNext()).anyMatch(next -> next == newState);
        if (!legal) {
            throw new IllegalStateException(
                    "Illegal slot transition " + old + " -> " + newState + " on " + slot.code());
        }
        slot.transitionTo(newState);
        if (publisher != null) {
            publisher.publish(new OccupancyEvent(
                    slot.code(), floorOf(slot.code()), zoneOf(slot.code()), slot.type(),
                    slot.currentPlate(), old, newState, Instant.now(), reason));
        }
    }

    /** Slot codes look like {@code L1-A-04}: floor is the first segment. */
    static String floorOf(String slotCode) {
        String[] parts = slotCode.split("-");
        return parts.length > 0 ? parts[0] : slotCode;
    }

    /** Zone is the first two segments: {@code L1-A}. */
    static String zoneOf(String slotCode) {
        String[] parts = slotCode.split("-");
        return parts.length >= 2 ? parts[0] + "-" + parts[1] : slotCode;
    }

    private void requireBound() {
        if (lot == null) {
            throw new IllegalStateException("OccupancyLedger is not bound to a lot yet");
        }
    }
}
