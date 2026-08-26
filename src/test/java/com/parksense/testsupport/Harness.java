package com.parksense.testsupport;

import com.parksense.audit.AuditTrail;
import com.parksense.controlroom.ControlRoomMediator;
import com.parksense.common.Money;
import com.parksense.gates.EntryGate;
import com.parksense.gates.ExitGate;
import com.parksense.gates.LaneKind;
import com.parksense.gates.PaymentKiosk;
import com.parksense.gates.SensorFeed;
import com.parksense.gates.hardware.SimulatedHardwareFactory;
import com.parksense.hardware.adapter.ParktronSensorAdapter;
import com.parksense.hardware.vendor.ParktronSensorNetwork;
import com.parksense.lot.Floor;
import com.parksense.lot.ParkingLot;
import com.parksense.lot.Slot;
import com.parksense.lot.SlotType;
import com.parksense.lot.Zone;
import com.parksense.members.MemberRegistry;
import com.parksense.occupancy.CapacityAlertWatcher;
import com.parksense.occupancy.DashboardFeed;
import com.parksense.occupancy.OccupancyEventPublisher;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.store.MemberStore;
import com.parksense.store.TariffStore;
import com.parksense.store.TicketStore;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffSelector;
import com.parksense.tariff.builder.TariffPlanBuilder;
import com.parksense.tickets.TicketNoGenerator;
import com.parksense.time.SimClock;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles a small but complete ParkSense graph for unit tests: one
 * floor, eight slots, two gates, one tariff of each kind. Every test class
 * builds its own harness so the shared singleton ledger is rebound to a
 * fresh lot each time.
 */
public class Harness {

    public final SimClock clock = new SimClock();
    public final ParkingLot lot;
    public final OccupancyEventPublisher publisher = new OccupancyEventPublisher();
    public final OccupancyLedger ledger = OccupancyLedger.getInstance();
    public final TicketStore tickets = new TicketStore();
    public final TariffStore tariffStore = new TariffStore();
    public final MemberStore memberStore = new MemberStore();
    public final PlateRegistryProxy plates = new PlateRegistryProxy();
    public final AuditTrail audit = new AuditTrail();
    public final TariffSelector selector;
    public final MemberRegistry members;
    public final TicketNoGenerator ticketNos = new TicketNoGenerator(clock);
    public final DashboardFeed feed = new DashboardFeed(50);
    public final CapacityAlertWatcher alerts;
    public final EntryGate gateIn1;
    public final ExitGate gateOut1;
    public final ExitGate gateOut2;
    public final PaymentKiosk kiosk = new PaymentKiosk();
    public final SensorFeed sensors;
    public final ControlRoomMediator mediator;

    public Harness() {
        List<Slot> slots = new ArrayList<>();
        int n = 1;
        for (int i = 0; i < 4; i++) {
            slots.add(new Slot("L1-A-" + String.format("%02d", n++), SlotType.STANDARD, "PTN-N" + n));
        }
        slots.add(new Slot("L1-A-" + String.format("%02d", n++), SlotType.COMPACT, "PTN-N" + n));
        slots.add(new Slot("L1-A-" + String.format("%02d", n++), SlotType.ACCESSIBLE, "PTN-N" + n));
        slots.add(new Slot("L1-A-" + String.format("%02d", n++), SlotType.EV_CHARGE, "PTN-N" + n));
        slots.add(new Slot("L1-A-" + String.format("%02d", n++), SlotType.MOTORCYCLE, "PTN-N" + n));
        lot = new ParkingLot("TEST-01", "Test Plaza", List.of(
                new Floor("L1", "Level 1", List.of(new Zone("L1-A", "Zone A", slots)))));

        ledger.bind(lot, publisher);
        publisher.subscribe(feed);
        alerts = new CapacityAlertWatcher(lot);
        publisher.subscribe(alerts);

        tariffStore.save(new TariffPlanBuilder("T-HOUR", "Hourly", TariffKind.HOURLY)
                .baseFee(Money.of("20.00")).perHour(Money.of("30.00")).graceMinutes(15).build());
        tariffStore.save(new TariffPlanBuilder("T-CAP", "Capped", TariffKind.DAILY_CAP)
                .baseFee(Money.of("20.00")).perHour(Money.of("30.00"))
                .dailyCap(Money.of("300.00")).graceMinutes(15).active(true).build());
        tariffStore.save(new TariffPlanBuilder("T-EARLY", "Early", TariffKind.EARLY_BIRD)
                .flatFee(Money.of("150.00")).perHour(Money.of("30.00"))
                .earlyBirdWindow(LocalTime.of(10, 0), LocalTime.of(16, 0))
                .graceMinutes(15).active(true).build());
        tariffStore.save(new TariffPlanBuilder("T-MEMBER", "Member", TariffKind.MEMBER_PASS)
                .perHour(Money.of("0.00")).graceMinutes(15).active(true).build());
        selector = new TariffSelector(tariffStore);
        members = new MemberRegistry(memberStore, clock);

        var factory = new SimulatedHardwareFactory();
        gateIn1 = new EntryGate("GATE-IN-1", "GATE-IN-1", factory, audit);
        gateOut1 = new ExitGate("GATE-OUT-1", "GATE-OUT-1", LaneKind.STAFFED, factory, audit);
        gateOut2 = new ExitGate("GATE-OUT-2", "GATE-OUT-2", LaneKind.EXPRESS, factory, audit);
        sensors = new SensorFeed(new ParktronSensorAdapter(new ParktronSensorNetwork()));

        mediator = new ControlRoomMediator(List.of(gateIn1), List.of(gateOut1, gateOut2),
                kiosk, sensors, ledger, tickets, ticketNos, selector, members, plates, clock, audit);
        mediator.attachColleagues();
    }

    /** tiny alias so the harness field name reads naturally */
    public static final class PlateRegistryProxy extends com.parksense.vehicles.PlateRegistry {
    }
}
