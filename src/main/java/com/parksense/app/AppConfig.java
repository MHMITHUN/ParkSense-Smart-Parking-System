package com.parksense.app;

import com.parksense.audit.AuditTrail;
import com.parksense.auth.PasswordHasher;
import com.parksense.auth.RequestRoles;
import com.parksense.auth.Role;
import com.parksense.auth.TokenService;
import com.parksense.auth.User;
import com.parksense.controlroom.ControlRoomMediator;
import com.parksense.gates.EntryGate;
import com.parksense.gates.ExitGate;
import com.parksense.gates.LaneKind;
import com.parksense.gates.PaymentKiosk;
import com.parksense.gates.SensorFeed;
import com.parksense.gates.BarrierController;
import com.parksense.guard.GateControlProxy;
import com.parksense.hardware.adapter.ParktronSensorAdapter;
import com.parksense.hardware.vendor.ParktronSensorNetwork;
import com.parksense.lot.ParkingLot;
import com.parksense.members.MemberRegistry;
import com.parksense.occupancy.CapacityAlertWatcher;
import com.parksense.occupancy.DashboardFeed;
import com.parksense.occupancy.OccupancyEventPublisher;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.occupancy.board.DisplayBoard;
import com.parksense.occupancy.board.EntryDisplayBoard;
import com.parksense.occupancy.board.FloorBoard;
import com.parksense.gates.hardware.GateHardwareFactory;
import com.parksense.gates.hardware.SimulatedHardwareFactory;
import com.parksense.reports.ReportService;
import com.parksense.store.MemberStore;
import com.parksense.store.TariffStore;
import com.parksense.store.TicketStore;
import com.parksense.store.UserStore;
import com.parksense.tariff.TariffSelector;
import com.parksense.tickets.TicketNoGenerator;
import com.parksense.time.SimClock;
import com.parksense.vehicles.PlateRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single place where the object graph is wired. Everything the system
 * does is plain Java; this configuration is the only Spring-aware seam of
 * the domain, which keeps every design pattern visible and unit-testable
 * without a container.
 */
@Configuration
public class AppConfig {

    @Bean
    public SimClock simClock() {
        return new SimClock();
    }

    // ------------------------------------------------------------------
    // Lot + occupancy backbone
    // ------------------------------------------------------------------

    @Bean
    public ParkingLot parkingLot() {
        return SeedData.buildLot();
    }

    @Bean
    public OccupancyEventPublisher occupancyEventPublisher() {
        return new OccupancyEventPublisher();
    }

    @Bean
    public OccupancyLedger occupancyLedger(ParkingLot lot, OccupancyEventPublisher publisher) {
        OccupancyLedger ledger = OccupancyLedger.getInstance();
        ledger.bind(lot, publisher);
        return ledger;
    }

    @Bean
    public DashboardFeed dashboardFeed(OccupancyEventPublisher publisher) {
        DashboardFeed feed = new DashboardFeed(100);
        publisher.subscribe(feed);
        return feed;
    }

    @Bean
    public CapacityAlertWatcher capacityAlertWatcher(ParkingLot lot, OccupancyEventPublisher publisher) {
        CapacityAlertWatcher watcher = new CapacityAlertWatcher(lot);
        publisher.subscribe(watcher);
        return watcher;
    }

    @Bean
    public List<DisplayBoard> displayBoards(ParkingLot lot, OccupancyEventPublisher publisher) {
        EntryDisplayBoard entrance = new EntryDisplayBoard(lot);
        publisher.subscribe(entrance);
        FloorBoard l1 = new FloorBoard(floor(lot, "L1"));
        FloorBoard l2 = new FloorBoard(floor(lot, "L2"));
        publisher.subscribe(l1);
        publisher.subscribe(l2);
        return List.of(entrance, l1, l2);
    }

    // ------------------------------------------------------------------
    // Stores + registries
    // ------------------------------------------------------------------

    @Bean
    public TicketStore ticketStore() {
        return new TicketStore();
    }

    @Bean
    public TariffStore tariffStore() {
        TariffStore store = new TariffStore();
        SeedData.seedTariffs(store);
        return store;
    }

    @Bean
    public MemberStore memberStore() {
        MemberStore store = new MemberStore();
        SeedData.seedMembers(store);
        return store;
    }

    @Bean
    public UserStore userStore() {
        UserStore store = new UserStore();
        store.save(new User("admin", "System Administrator",
                PasswordHasher.hash("admin123".toCharArray()), Role.ADMIN));
        store.save(new User("operator", "Gate Operator",
                PasswordHasher.hash("operator123".toCharArray()), Role.OPERATOR));
        store.save(new User("dev", "Lead Software Developer",
                PasswordHasher.hash("dev123".toCharArray()), Role.DEVELOPER));
        return store;
    }

    @Bean
    public PlateRegistry plateRegistry() {
        PlateRegistry registry = new PlateRegistry();
        SeedData.seedBlacklist(registry);
        return registry;
    }

    @Bean
    public MemberRegistry memberRegistry(MemberStore members, SimClock clock) {
        return new MemberRegistry(members, clock);
    }

    // ------------------------------------------------------------------
    // Optional MongoDB persistence (parksense database; in-memory fallback)
    // ------------------------------------------------------------------

    @Bean(destroyMethod = "shutdown")
    public com.parksense.persistence.MongoSync mongoSync(TicketStore tickets,
                                                         MemberStore members,
                                                         TariffStore tariffs,
                                                         UserStore users,
                                                         AuditTrail audit) {
        com.parksense.persistence.MongoSync sync = com.parksense.persistence.MongoSync.connect();
        sync.attachStores(tickets, members, tariffs, users, audit);
        return sync;
    }

    @Bean
    public TicketNoGenerator ticketNoGenerator(SimClock clock) {
        return new TicketNoGenerator(clock);
    }

    @Bean
    public TokenService tokenService() {
        return new TokenService();
    }

    // ------------------------------------------------------------------
    // Hardware + lanes
    // ------------------------------------------------------------------

    @Bean
    public GateHardwareFactory gateHardwareFactory() {
        // one-line swap to VendorHardwareFactory() for real hardware
        return new SimulatedHardwareFactory();
    }

    @Bean
    public ParktronSensorNetwork parktronNetwork() {
        return new ParktronSensorNetwork();
    }

    @Bean
    public ParktronSensorAdapter parktronSensorAdapter(ParktronSensorNetwork network) {
        return new ParktronSensorAdapter(network);
    }

    @Bean
    public SensorFeed sensorFeed(ParktronSensorAdapter adapter) {
        return new SensorFeed(adapter);
    }

    @Bean
    public EntryGate gateInOne(GateHardwareFactory factory, AuditTrail audit) {
        return new EntryGate("GATE-IN-1", "GATE-IN-1", factory, audit);
    }

    @Bean
    public EntryGate gateInTwo(GateHardwareFactory factory, AuditTrail audit) {
        return new EntryGate("GATE-IN-2", "GATE-IN-2", factory, audit);
    }

    @Bean
    public ExitGate gateOutOne(GateHardwareFactory factory, AuditTrail audit) {
        return new ExitGate("GATE-OUT-1", "GATE-OUT-1", LaneKind.STAFFED, factory, audit);
    }

    @Bean
    public ExitGate gateOutTwo(GateHardwareFactory factory, AuditTrail audit) {
        return new ExitGate("GATE-OUT-2", "GATE-OUT-2", LaneKind.EXPRESS, factory, audit);
    }

    @Bean
    public PaymentKiosk paymentKiosk() {
        return new PaymentKiosk();
    }

    // ------------------------------------------------------------------
    // Guarded barrier control (protection proxies)
    // ------------------------------------------------------------------

    @Bean
    public Map<String, GateControlProxy> gateControlProxies(
            List<EntryGate> entries, List<ExitGate> exits, AuditTrail audit) {
        Map<String, GateControlProxy> proxies = new LinkedHashMap<>();
        var allGates = new java.util.ArrayList<com.parksense.gates.Gate>();
        allGates.addAll(entries);
        allGates.addAll(exits);
        for (var gate : allGates) {
            BarrierController controller = new BarrierController(gate);
            proxies.put(gate.id(), new GateControlProxy(controller, audit,
                    RequestRoles::isAdmin, RequestRoles::currentActor));
        }
        return proxies;
    }

    // ------------------------------------------------------------------
    // Brain: mediator, selector, reports, facade
    // ------------------------------------------------------------------

    @Bean
    public TariffSelector tariffSelector(TariffStore store) {
        return new TariffSelector(store);
    }

    @Bean
    public AuditTrail auditTrail() {
        return new AuditTrail();
    }

    @Bean
    public ReportService reportService(TicketStore tickets, OccupancyLedger ledger, SimClock clock) {
        return new ReportService(tickets, ledger, clock);
    }

    @Bean
    public ControlRoomMediator controlRoomMediator(
            List<EntryGate> entries, List<ExitGate> exits,
            PaymentKiosk kiosk, SensorFeed sensors,
            OccupancyLedger ledger, TicketStore tickets, TicketNoGenerator ticketNos,
            TariffSelector selector, MemberRegistry members, PlateRegistry plates,
            SimClock clock, AuditTrail audit,
            com.parksense.persistence.MongoSync persistence) {
        ControlRoomMediator mediator = new ControlRoomMediator(
                entries, exits, kiosk, sensors,
                ledger, tickets, ticketNos, selector, members, plates, clock, audit);
        mediator.attachColleagues();

        // Boot: restore persisted state when present, otherwise seed the demo
        boolean restored = persistence.loadInto(ticketNos);
        if (!restored) {
            SeedData.seedRuntime(ledger, tickets, plates, ticketNos, clock, selector);
            persistence.snapshotAll();
        } else {
            persistence.restoreRuntimeState(ledger, plates);
        }
        persistence.trackTicketSequence(ticketNos);
        persistence.trackLotState(ledger);
        persistence.startSync();
        return mediator;
    }

    @Bean
    public SystemResetService systemResetService(OccupancyLedger ledger, TicketStore tickets,
                                                  PlateRegistry plates, TicketNoGenerator ticketNos,
                                                  SimClock clock, TariffSelector selector,
                                                  com.parksense.persistence.MongoSync persistence) {
        return new SystemResetService(ledger, tickets, plates, ticketNos, clock, selector, persistence);
    }

    @Bean
    public OperationsFacade operationsFacade(
            ControlRoomMediator mediator,
            List<EntryGate> entries, List<ExitGate> exits,
            Map<String, GateControlProxy> proxies, SensorFeed sensors,
            OccupancyLedger ledger, TicketStore tickets, MemberStore memberStore,
            MemberRegistry members, TariffStore tariffStore, TariffSelector selector,
            ReportService reports, DashboardFeed feed, CapacityAlertWatcher alerts,
            SimClock clock, AuditTrail audit) {
        return new OperationsFacade(mediator, entries, exits,
                proxies, sensors, ledger, tickets, memberStore, members, tariffStore,
                selector, reports, feed, alerts, clock, audit);
    }

    private static com.parksense.lot.Floor floor(ParkingLot lot, String code) {
        return lot.floors().stream()
                .filter(f -> f.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Floor " + code + " missing"));
    }
}
