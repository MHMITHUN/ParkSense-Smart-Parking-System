package com.parksense.app;

import com.parksense.audit.AuditTrail;
import com.parksense.auth.RequestRoles;
import com.parksense.controlroom.ControlRoomMediator;
import com.parksense.entrycheck.EntryOutcome;
import com.parksense.exitlane.ExitOutcome;
import com.parksense.exitlane.ExitRequest;
import com.parksense.gates.EntryGate;
import com.parksense.gates.ExitGate;
import com.parksense.gates.Gate;
import com.parksense.gates.SensorFeed;
import com.parksense.guard.GateControlProxy;
import com.parksense.common.Money;
import com.parksense.hardware.spi.PlateScan;
import com.parksense.lot.Slot;
import com.parksense.members.Member;
import com.parksense.members.MemberRegistry;
import com.parksense.occupancy.CapacityAlertWatcher;
import com.parksense.occupancy.DashboardFeed;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.reports.ReportResult;
import com.parksense.reports.ReportService;
import com.parksense.store.MemberStore;
import com.parksense.store.TariffStore;
import com.parksense.store.TicketStore;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffPlan;
import com.parksense.tariff.TariffSelector;
import com.parksense.tariff.builder.TariffPlanBuilder;
import com.parksense.tickets.FeeComponent;
import com.parksense.tickets.PaymentMethod;
import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;
import com.parksense.tickets.addon.BaseParkingFee;
import com.parksense.tickets.addon.CarWashAddon;
import com.parksense.tickets.addon.EvChargeAddon;
import com.parksense.tickets.addon.LostTicketPenalty;
import com.parksense.tickets.addon.ValetServiceAddon;
import com.parksense.time.SimClock;
import com.parksense.vehicles.Vehicle;
import com.parksense.vehicles.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * GoF Facade. One method per button in the control room UI — every call
 * reads like the operator's sentence, and all the cooperating subsystems
 * (chain, ledger, mediator, tariff, command queues, registries) stay
 * hidden behind it. Controllers are one line each because of this class.
 */
public class OperationsFacade {

    /** Add-on catalogue (prices in BDT). */
    public static final BigDecimal CAR_WASH_PRICE = Money.of("120.00");
    public static final BigDecimal VALET_PRICE = Money.of("200.00");
    public static final BigDecimal EV_KWH_RATE = Money.of("22.00");

    private final ControlRoomMediator mediator;
    private final Map<String, EntryGate> entryGates;
    private final Map<String, ExitGate> exitGates;
    private final Map<String, GateControlProxy> gateProxies;
    private final SensorFeed sensors;
    private final OccupancyLedger ledger;
    private final TicketStore tickets;
    private final MemberStore memberStore;
    private final MemberRegistry members;
    private final TariffStore tariffStore;
    private final TariffSelector selector;
    private final ReportService reports;
    private final DashboardFeed feed;
    private final CapacityAlertWatcher alerts;
    private final SimClock clock;
    private final AuditTrail audit;

    /** Base-fee memory so add-ons can be toggled without recomputing the tariff. */
    private final Map<String, AddonState> addonStates = new LinkedHashMap<>();

    public OperationsFacade(ControlRoomMediator mediator,
                            List<EntryGate> entryGates, List<ExitGate> exitGates,
                            Map<String, GateControlProxy> gateProxies,
                            SensorFeed sensors,
                            OccupancyLedger ledger, TicketStore tickets,
                            MemberStore memberStore, MemberRegistry members,
                            TariffStore tariffStore, TariffSelector selector,
                            ReportService reports, DashboardFeed feed,
                            CapacityAlertWatcher alerts, SimClock clock, AuditTrail audit) {
        this.mediator = mediator;
        this.entryGates = toMap(entryGates);
        this.exitGates = toMap(exitGates);
        this.gateProxies = gateProxies;
        this.sensors = sensors;
        this.ledger = ledger;
        this.tickets = tickets;
        this.memberStore = memberStore;
        this.members = members;
        this.tariffStore = tariffStore;
        this.selector = selector;
        this.reports = reports;
        this.feed = feed;
        this.alerts = alerts;
        this.clock = clock;
        this.audit = audit;
    }

    private static <G extends Gate> Map<String, G> toMap(List<G> gates) {
        Map<String, G> map = new LinkedHashMap<>();
        gates.forEach(g -> map.put(g.id(), g));
        return map;
    }

    // ------------------------------------------------------------------
    // Gates — entry / exit
    // ------------------------------------------------------------------

    /** "A vehicle pulled up at gate X": camera read → chain → ticket → barrier. */
    public EntryOutcome simulateEntry(String gateId, String plateInput,
                                      VehicleType type, boolean accessible) {
        EntryGate gate = requireEntry(gateId);
        PlateScan scan = gate.readPlate(plateInput);          // through the hardware port
        Vehicle vehicle = new Vehicle(scan.plateNo(), type, accessible);
        return gate.vehicleArrived(vehicle);
    }

    /** "The vehicle is leaving": ticket or plate, optional tender, lost flag. */
    public ExitOutcome requestExit(String gateId, String ref, PaymentMethod method,
                                   BigDecimal tendered, boolean lostTicket) {
        ExitGate gate = requireExit(gateId);
        ExitRequest request = lostTicket
                ? ExitRequest.lost(gateId, ref)
                : new ExitRequest(gateId, ref, method, tendered, false);
        return gate.vehicleLeaving(request);
    }

    // ------------------------------------------------------------------
    // Tickets — checkout, add-ons, void
    // ------------------------------------------------------------------

    /** Settle a fare at the kiosk/checkout page (any state that can pay). */
    public CheckoutReceipt payTicket(String ticketNo, PaymentMethod method, BigDecimal tendered) {
        Ticket ticket = ticket(ticketNo);
        if ("EXITED".equals(ticket.stateName()) || "VOID".equals(ticket.stateName())) {
            throw new IllegalStateException("Ticket " + ticketNo + " is " + ticket.stateName());
        }
        if ("LOST".equals(ticket.stateName()) && ticket.feeChain() == null) {
            ticket.setFee(new LostTicketPenalty(
                            new BaseParkingFee(BigDecimal.ZERO, "Parking (duration unprovable)"),
                            Money.of("500.00"), Money.of("300.00")),
                    "LOST", "lost-ticket penalty tariff");
        } else if ("ACTIVE".equals(ticket.stateName()) && ticket.feeChain() == null) {
            computeBaseFee(ticket);
        }
        FeeComponent chain = ticket.feeChain();
        BigDecimal due = Money.round(chain.amount());
        BigDecimal paid = tendered == null ? due : tendered;
        if (paid.compareTo(due) < 0) {
            throw new IllegalStateException(
                    "Tender " + paid + " is below the due amount " + due);
        }
        PaymentMethod how = method == null ? PaymentMethod.CASH : method;
        PaymentRecord payment = new PaymentRecord(ticket.ticketNo(), how, due, paid,
                Money.round(paid.subtract(due)), clock.now());
        ticket.pay(payment);
        audit.record(RequestRoles.currentActor(), "TICKET_PAID",
                ticketNo + " settled " + due + " (" + how.label() + ")", true);
        int grace = activeGraceMinutes(ticket);
        return new CheckoutReceipt(
                ticket.ticketNo(), ticket.plate(), ticket.stateName(),
                feeLines(ticket), due, how.label(), paid, payment.changeDue(),
                payment.at(), payment.at().plus(Duration.ofMinutes(grace)));
    }

    /** Toggle an add-on on an open ticket and return the fresh breakdown. */
    public List<String[]> toggleAddon(String ticketNo, String addonCode, boolean add) {
        Ticket ticket = ticket(ticketNo);
        if ("PAID".equals(ticket.stateName()) || "EXITED".equals(ticket.stateName())
                || "VOID".equals(ticket.stateName())) {
            throw new IllegalStateException("Add-ons only apply before payment");
        }
        if ("LOST".equals(ticket.stateName())) {
            throw new IllegalStateException("Lost tickets settle at the flat penalty only");
        }
        AddonState state = addonStates.get(ticketNo);
        if (state == null) {
            state = computeBaseFee(ticket);
        }
        if (add) {
            state.codes().add(addonCode.toUpperCase());
        } else {
            state.codes().remove(addonCode.toUpperCase());
        }
        ticket.setFee(buildChain(state), state.planId(), state.reason());
        audit.record(RequestRoles.currentActor(), "ADDON_" + (add ? "ADD" : "REMOVE"),
                ticketNo + " " + addonCode, true);
        return feeLines(ticket);
    }

    /** ADMIN: cancel a ticket entirely, freeing its slot. */
    public void voidTicket(String ticketNo, String reason) {
        Ticket ticket = ticket(ticketNo);
        ledger.slot(ticket.slotCode()).ifPresent(slot -> {
            if (slot.state() == com.parksense.lot.SlotState.OCCUPIED
                    || slot.state() == com.parksense.lot.SlotState.RESERVED) {
                ledger.release(slot, "ticket voided");
            }
        });
        ticket.voidTicket(reason);
        audit.record(RequestRoles.currentActor(), "TICKET_VOID",
                ticketNo + " — " + reason, true);
    }

    /** Mark a ticket lost from the checkout page (flat penalty at settlement). */
    public void reportTicketLost(String ticketNo) {
        ticket(ticketNo).reportLost();
        audit.record(RequestRoles.currentActor(), "TICKET_LOST", ticketNo, true);
    }

    // ------------------------------------------------------------------
    // Manual gate control (through the protection proxy)
    // ------------------------------------------------------------------

    public void forceBarrier(String gateId, String action) {
        GateControlProxy proxy = gateProxies.get(gateId);
        if (proxy == null) {
            throw new IllegalArgumentException("No gate " + gateId);
        }
        String actor = RequestRoles.currentActor();
        switch (action.toUpperCase()) {
            case "OPEN" -> proxy.forceOpen(actor);
            case "CLOSE" -> proxy.forceClose(actor);
            case "RESUME" -> proxy.resumeAutomatic(actor);
            default -> throw new IllegalArgumentException("Unknown action " + action);
        }
    }

    public boolean undoGateCommand(String gateId, String commandId) {
        Gate gate = gate(gateId);
        return gate.commandQueue().undo(commandId).orElse(false);
    }

    // ------------------------------------------------------------------
    // Catalogue reads for the UI
    // ------------------------------------------------------------------

    public Map<String, Object> gatePanel(String gateId) {
        Gate gate = gate(gateId);
        Map<String, Object> panel = new LinkedHashMap<>();
        panel.put("id", gate.id());
        panel.put("code", gate.code());
        panel.put("direction", gate.direction().name());
        panel.put("laneKind", gate.laneKind().name());
        panel.put("display", gate.displayLine());
        panel.put("barrierOpen", gate.barrier().isOpen());
        panel.put("hardwareFamily", hardwareFamilyOf(gate));
        panel.put("commands", gate.commandQueue().history().stream()
                .map(c -> Map.of(
                        "id", c.id(),
                        "describe", c.describe(),
                        "at", String.valueOf(c.executedAt()),
                        "undoSupported", c.undoSupported(),
                        "undone", c.undone()))
                .toList());
        return panel;
    }

    public List<Map<String, Object>> gatePanels() {
        List<Map<String, Object>> panels = new ArrayList<>();
        entryGates.values().forEach(g -> panels.add(gatePanel(g.id())));
        exitGates.values().forEach(g -> panels.add(gatePanel(g.id())));
        return panels;
    }

    public Ticket ticket(String ticketNo) {
        return tickets.find(ticketNo).orElseThrow(
                () -> new IllegalArgumentException("No ticket " + ticketNo));
    }

    public List<String[]> feeLines(Ticket ticket) {
        List<String[]> lines = new ArrayList<>();
        ticket.feeLines().forEach(l -> lines.add(new String[]{l.label(), l.amount().toPlainString()}));
        return lines;
    }

    public List<Ticket> openTickets() {
        return tickets.open();
    }

    public List<Ticket> ticketHistory(int limit) {
        return tickets.all().stream().limit(limit).toList();
    }

    // ------------------------------------------------------------------
    // Members & tariffs
    // ------------------------------------------------------------------

    public List<Member> members() {
        return memberStore.all();
    }

    public Member saveMember(Member member) {
        memberStore.save(member);
        audit.record(RequestRoles.currentActor(), "MEMBER_SAVE", member.id() + " " + member.name(), true);
        return member;
    }

    public List<TariffPlan> tariffs() {
        return tariffStore.all();
    }

    public TariffPlan saveTariff(TariffPlanBuilder builder) {
        TariffPlan plan = builder.build();
        tariffStore.save(plan);
        audit.record(RequestRoles.currentActor(), "TARIFF_SAVE",
                plan.id() + " " + plan.kind() + (plan.active() ? " (active)" : ""), true);
        return plan;
    }

    public void setTariffActive(String planId, boolean active) {
        TariffPlan plan = tariffStore.find(planId)
                .orElseThrow(() -> new IllegalArgumentException("No tariff " + planId));
        if (active) {
            plan.activate();
        } else {
            plan.deactivate();
        }
        tariffStore.save(plan);
        audit.record(RequestRoles.currentActor(), "TARIFF_" + (active ? "ACTIVATE" : "DEACTIVATE"),
                planId, true);
    }

    // ------------------------------------------------------------------
    // Live feeds & reports
    // ------------------------------------------------------------------

    public List<Map<String, Object>> recentEvents(int count) {
        return feed.latest(count).stream().map(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("at", e.at().toString());
            row.put("slot", e.slotCode());
            row.put("type", e.slotType().name());
            row.put("plate", e.plate());
            row.put("transition", e.oldState() + " → " + e.newState());
            row.put("reason", e.reason());
            return row;
        }).toList();
    }

    public List<Map<String, Object>> capacityAlerts(int count) {
        return alerts.history().stream()
                .limit(count)
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("at", a.at().toString());
                    row.put("code", a.code());
                    row.put("message", a.message());
                    row.put("active", a.active());
                    return row;
                }).toList();
    }

    public ReportResult runReport(String name, Instant from, Instant to) {
        return switch (name) {
            case "revenue" -> reports.revenue(from, to);
            case "occupancy" -> reports.occupancy();
            case "utilization" -> reports.utilization();
            case "peak-hours" -> reports.peakHours(from, to);
            case "addons" -> reports.addons(from, to);
            default -> throw new IllegalArgumentException("Unknown report " + name);
        };
    }

    public Map<String, Object> dashboard() {
        return reports.dashboard();
    }

    public void advanceClock(long minutes) {
        clock.advance(Duration.ofMinutes(minutes));
        audit.record(RequestRoles.currentActor(), "CLOCK_ADVANCE", minutes + " minutes", true);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private AddonState computeBaseFee(Ticket ticket) {
        FeeRequest request = FeeRequest.of(ticket.entryTime(), clock.now(), ticket.vehicleType());
        boolean memberPass = members.hasValidPass(ticket.plate());
        TariffSelector.Selected selected = selector.select(request, memberPass);
        BigDecimal amount = selected.strategy().compute(selected.plan(), request);
        String explain = selected.strategy().explain(selected.plan(), request);
        AddonState state = new AddonState(amount, explain, selected.plan().id(), selected.reason());
        addonStates.put(ticket.ticketNo(), state);
        ticket.setFee(buildChain(state), state.planId(), state.reason());
        return state;
    }

    private FeeComponent buildChain(AddonState state) {
        FeeComponent chain = new BaseParkingFee(state.baseAmount(), state.baseDescribe());
        if (state.codes().contains("CAR_WASH")) {
            chain = new CarWashAddon(chain, CAR_WASH_PRICE);
        }
        if (state.codes().contains("VALET")) {
            chain = new ValetServiceAddon(chain, VALET_PRICE);
        }
        if (state.codes().contains("EV_CHARGE")) {
            chain = new EvChargeAddon(chain, new BigDecimal("18.5"), EV_KWH_RATE);
        }
        return chain;
    }

    private int activeGraceMinutes(Ticket ticket) {
        return tariffStore.all().stream()
                .filter(p -> p.id().equals(ticket.tariffPlanId()))
                .map(TariffPlan::graceMinutes)
                .findFirst()
                .orElse(15);
    }

    private String hardwareFamilyOf(Gate gate) {
        return gate.plateReader() instanceof com.parksense.hardware.adapter.PlateSenseAdapter
                ? "VENDOR" : "SIM-KIT";
    }

    private Gate gate(String gateId) {
        Gate gate = entryGates.get(gateId);
        if (gate == null) {
            gate = exitGates.get(gateId);
        }
        if (gate == null) {
            throw new IllegalArgumentException("No gate " + gateId);
        }
        return gate;
    }

    private EntryGate requireEntry(String gateId) {
        EntryGate gate = entryGates.get(gateId);
        if (gate == null) {
            throw new IllegalArgumentException("No entry gate " + gateId);
        }
        return gate;
    }

    private ExitGate requireExit(String gateId) {
        ExitGate gate = exitGates.get(gateId);
        if (gate == null) {
            throw new IllegalArgumentException("No exit gate " + gateId);
        }
        return gate;
    }

    /** Base fee + active add-on codes for one open ticket. */
    record AddonState(BigDecimal baseAmount, String baseDescribe,
                      String planId, String reason,
                      Set<String> codes) {
        AddonState(BigDecimal baseAmount, String baseDescribe, String planId, String reason) {
            this(baseAmount, baseDescribe, planId, reason, new LinkedHashSet<>());
        }
    }
}
