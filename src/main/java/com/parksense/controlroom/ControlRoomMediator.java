package com.parksense.controlroom;

import com.parksense.audit.AuditTrail;
import com.parksense.entrycheck.EntryChainFactory;
import com.parksense.entrycheck.EntryContext;
import com.parksense.entrycheck.EntryOutcome;
import com.parksense.entrycheck.EntryRuleHandler;
import com.parksense.exitlane.ExitOutcome;
import com.parksense.exitlane.ExitProcessor;
import com.parksense.exitlane.ExitRequest;
import com.parksense.exitlane.ExpressMemberLaneProcessor;
import com.parksense.exitlane.LostTicketProcessor;
import com.parksense.exitlane.StaffedLaneProcessor;
import com.parksense.gates.EntryGate;
import com.parksense.gates.ExitGate;
import com.parksense.gates.Gate;
import com.parksense.gates.LaneKind;
import com.parksense.gates.PaymentKiosk;
import com.parksense.gates.SensorFeed;
import com.parksense.gates.command.CloseBarrierCommand;
import com.parksense.gates.command.OpenBarrierCommand;
import com.parksense.hardware.spi.SlotSignal;
import com.parksense.lot.Slot;
import com.parksense.lot.SlotState;
import com.parksense.members.MemberRegistry;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.store.TicketStore;
import com.parksense.tariff.TariffSelector;
import com.parksense.tickets.Ticket;
import com.parksense.tickets.TicketNoGenerator;
import com.parksense.time.SimClock;
import com.parksense.vehicles.PlateRegistry;
import com.parksense.vehicles.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GoF Mediator — the control room itself. Every colleague (entry gates,
 * exit gates, kiosk, sensor feed, boards) talks only to this object; the
 * mediator runs the entry chain, allocates slots, issues tickets, queues
 * barrier commands, settles exits through the lane processors and keeps
 * the registries in step. Colleague-to-colleague calls do not exist.
 */
public final class ControlRoomMediator implements ParkingMediator {

    private final Map<String, EntryGate> entryGates;
    private final Map<String, ExitGate> exitGates;
    private final PaymentKiosk kiosk;
    private final SensorFeed sensors;
    private final EntryRuleHandler entryChain;

    private final OccupancyLedger ledger;
    private final TicketStore tickets;
    private final TicketNoGenerator ticketNos;
    private final TariffSelector selector;
    private final MemberRegistry members;
    private final PlateRegistry plates;
    private final SimClock clock;
    private final AuditTrail audit;

    private final ExitProcessor staffedLane;
    private final ExitProcessor expressLane;
    private final ExitProcessor lostDesk;

    public ControlRoomMediator(List<EntryGate> entries, List<ExitGate> exits,
                               PaymentKiosk kiosk, SensorFeed sensors,
                               OccupancyLedger ledger, TicketStore tickets,
                               TicketNoGenerator ticketNos, TariffSelector selector,
                               MemberRegistry members, PlateRegistry plates,
                               SimClock clock, AuditTrail audit) {
        this.entryGates = entries.stream()
                .collect(Collectors.toUnmodifiableMap(Gate::id, g -> g));
        this.exitGates = exits.stream()
                .collect(Collectors.toUnmodifiableMap(Gate::id, g -> g));
        this.kiosk = kiosk;
        this.sensors = sensors;
        this.ledger = ledger;
        this.tickets = tickets;
        this.ticketNos = ticketNos;
        this.selector = selector;
        this.members = members;
        this.plates = plates;
        this.clock = clock;
        this.audit = audit;
        this.entryChain = EntryChainFactory.build(plates, members, ledger);
        this.staffedLane = new StaffedLaneProcessor(ledger, tickets, selector, members,
                plates, clock, audit);
        this.expressLane = new ExpressMemberLaneProcessor(ledger, tickets, selector, members,
                plates, clock, audit);
        this.lostDesk = new LostTicketProcessor(ledger, tickets, selector, members,
                plates, clock, audit);
    }

    /** Wire this mediator into every colleague (call once after construction). */
    public void attachColleagues() {
        entryGates.values().forEach(g -> g.setMediator(this));
        exitGates.values().forEach(g -> g.setMediator(this));
        kiosk.setMediator(this);
        sensors.setMediator(this);
    }

    // ------------------------------------------------------------------
    // Entry protocol
    // ------------------------------------------------------------------

    @Override
    public EntryOutcome handleVehicleEntry(String gateId, Vehicle vehicle) {
        EntryGate gate = requireEntryGate(gateId);
        EntryContext context = new EntryContext(gateId, vehicle);

        if (!entryChain.handle(context)) {
            String reason = lastFailLine(context);
            gate.setDisplay(reason);
            audit.record("system", "ENTRY_DENIED", vehicle.plateNo() + " — " + reason, false);
            return EntryOutcome.rejected(reason, context.trace());
        }

        String ticketNo = ticketNos.next();
        Optional<Slot> allocated = ledger.reserveFor(vehicle.type(), vehicle.accessibleBadge(),
                vehicle.plateNo(), ticketNo, clock.now());
        if (allocated.isEmpty()) {
            gate.setDisplay("LOT FULL");
            audit.record("system", "ENTRY_DENIED", vehicle.plateNo() + " — lot full at allocation", false);
            return EntryOutcome.rejected("LOT FULL", context.trace());
        }
        Slot slot = allocated.get();

        Ticket ticket = new Ticket(ticketNo, vehicle.plateNo(), vehicle.type(),
                vehicle.accessibleBadge(), clock.now(), gateId, slot.code());
        ticket.confirmEntry();
        tickets.save(ticket);
        kiosk.ticketIssued(ticket);

        gate.commandQueue().submit(new com.parksense.gates.command.PrintTicketCommand(gate, ticket));
        gate.commandQueue().submit(new OpenBarrierCommand(gate));
        gate.commandQueue().submit(new CloseBarrierCommand(gate));

        sensors.driveAndReport(slot, true);
        plates.markInside(vehicle.plateNo());

        String welcome = "WELCOME — SLOT " + slot.code();
        gate.setDisplay(welcome);
        audit.record("system", "ENTRY", vehicle.plateNo() + " → " + slot.code()
                + " (" + ticketNo + ")", true);
        return new EntryOutcome(true, welcome, slot.code(), ticketNo,
                context.memberPlate(), context.trace());
    }

    // ------------------------------------------------------------------
    // Exit protocol
    // ------------------------------------------------------------------

    @Override
    public ExitOutcome handleVehicleExit(String gateId, ExitRequest request) {
        ExitGate gate = requireExitGate(gateId);
        ExitProcessor processor = pickProcessor(gate, request);
        ExitOutcome outcome = processor.process(request);

        if (outcome.allowed()) {
            tickets.find(outcome.ticketNo()).ifPresent(kiosk::ticketSettled);
            gate.setDisplay(outcome.line());
            gate.commandQueue().submit(new OpenBarrierCommand(gate));
            gate.commandQueue().submit(new CloseBarrierCommand(gate));
        } else {
            gate.setDisplay(outcome.line());
            audit.record("system", "EXIT_DENIED",
                    request.plateOrTicketNo() + " — " + outcome.line(), false);
        }
        return outcome;
    }

    private ExitProcessor pickProcessor(ExitGate gate, ExitRequest request) {
        if (request.lostTicket()) {
            return lostDesk;
        }
        return gate.laneKind() == LaneKind.EXPRESS ? expressLane : staffedLane;
    }

    // ------------------------------------------------------------------
    // Sensor + display callbacks
    // ------------------------------------------------------------------

    @Override
    public void handleSensorSignal(SlotSignal signal) {
        ledger.slot(signal.slotCode()).ifPresent(slot -> {
            if (signal.vehiclePresent() && slot.state() == SlotState.RESERVED) {
                ledger.confirmArrival(slot, signal.signalAt());
                audit.record("system", "SENSOR", slot.code() + " arrival confirmed", true);
            } else if (!signal.vehiclePresent() && slot.state() == SlotState.OCCUPIED) {
                // departure without an exit protocol — control room note
                audit.record("system", "SENSOR", slot.code()
                        + " reports empty while ticket open — verify", false);
            }
        });
    }

    @Override
    public void displayOnGate(String gateId, String message) {
        Optional<Gate> gate = Optional.<Gate>ofNullable(entryGates.get(gateId))
                .or(() -> Optional.ofNullable(exitGates.get(gateId)));
        gate.ifPresent(g -> g.setDisplay(message));
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private EntryGate requireEntryGate(String gateId) {
        EntryGate gate = entryGates.get(gateId);
        if (gate == null) {
            throw new IllegalArgumentException("No entry gate " + gateId);
        }
        return gate;
    }

    private ExitGate requireExitGate(String gateId) {
        ExitGate gate = exitGates.get(gateId);
        if (gate == null) {
            throw new IllegalArgumentException("No exit gate " + gateId);
        }
        return gate;
    }

    private static String lastFailLine(EntryContext context) {
        return context.trace().stream()
                .filter(line -> line.startsWith("FAIL"))
                .reduce((first, second) -> second)
                .map(line -> line.substring(line.indexOf(':') + 2))
                .orElse("ENTRY REFUSED");
    }
}
