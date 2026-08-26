package com.parksense.app;

import com.parksense.common.Money;
import com.parksense.lot.Floor;
import com.parksense.lot.ParkingLot;
import com.parksense.lot.Slot;
import com.parksense.lot.SlotType;
import com.parksense.lot.Zone;
import com.parksense.members.Member;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.store.MemberStore;
import com.parksense.store.TariffStore;
import com.parksense.store.TicketStore;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffSelector;
import com.parksense.tariff.builder.TariffPlanBuilder;
import com.parksense.tickets.PaymentMethod;
import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;
import com.parksense.tickets.TicketNoGenerator;
import com.parksense.tickets.addon.CarWashAddon;
import com.parksense.tickets.addon.EvChargeAddon;
import com.parksense.tickets.addon.ValetServiceAddon;
import com.parksense.time.SimClock;
import com.parksense.vehicles.PlateRegistry;
import com.parksense.vehicles.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Builds the demo facility and populates it: "ParkSense Central Plaza" —
 * three levels, five zones, 132 mixed slots, ~35% occupied right now, a
 * week of ticket history so the reports have data on first load. The
 * layout and history are deterministic (fixed random seed), so the UI,
 * the tests and the report screenshots all describe the same lot.
 */
final class SeedData {

    private SeedData() {
    }

    private static final Random RANDOM = new Random(42);

    /** Slot counts per zone: [STANDARD, COMPACT, ACCESSIBLE, EV_CHARGE, MOTORCYCLE]. */
    private record ZoneSpec(String code, String label, int[] mix) {
    }

    private static final List<ZoneSpec> LAYOUT = List.of(
            new ZoneSpec("L1-A", "Level 1 — Zone A (Main)", new int[]{16, 3, 2, 1, 2}),
            new ZoneSpec("L1-B", "Level 1 — Zone B (Main)", new int[]{18, 3, 2, 1, 0}),
            new ZoneSpec("L2-A", "Level 2 — Zone A (Main)", new int[]{16, 3, 2, 1, 2}),
            new ZoneSpec("L2-B", "Level 2 — Zone B (Main)", new int[]{18, 3, 2, 1, 0}),
            new ZoneSpec("L3-A", "Level 3 — Zone A (Rooftop)", new int[]{14, 4, 2, 8, 8}));

    static ParkingLot buildLot() {
        return new ParkingLot("PLAZA-01", "ParkSense Central Plaza", List.of(
                new Floor("L1", "Level 1", List.of(zone(LAYOUT.get(0)), zone(LAYOUT.get(1)))),
                new Floor("L2", "Level 2", List.of(zone(LAYOUT.get(2)), zone(LAYOUT.get(3)))),
                new Floor("L3", "Level 3 — Rooftop", List.of(zone(LAYOUT.get(4))))));
    }

    private static Zone zone(ZoneSpec spec) {
        SlotType[] types = {SlotType.STANDARD, SlotType.COMPACT, SlotType.ACCESSIBLE,
                SlotType.EV_CHARGE, SlotType.MOTORCYCLE};
        List<Slot> slots = new ArrayList<>();
        int number = 1;
        for (int i = 0; i < types.length; i++) {
            for (int count = 0; count < spec.mix()[i]; count++) {
                slots.add(new Slot(spec.code() + "-" + String.format("%02d", number++),
                        types[i], "PTN-" + spec.code().replace("-", "") + number));
            }
        }
        return new Zone(spec.code(), spec.label(), slots);
    }

    // ------------------------------------------------------------------
    // Static catalogue seeds
    // ------------------------------------------------------------------

    static void seedTariffs(TariffStore store) {
        store.save(new TariffPlanBuilder("TAR-HOURLY", "Standard Hourly", TariffKind.HOURLY)
                .baseFee(Money.of("20.00")).perHour(Money.of("30.00")).graceMinutes(15)
                .active(false)
                .build());
        store.save(new TariffPlanBuilder("TAR-CAP", "Hourly + Daily Cap", TariffKind.DAILY_CAP)
                .baseFee(Money.of("20.00")).perHour(Money.of("30.00"))
                .dailyCap(Money.of("300.00")).graceMinutes(15)
                .active(true)
                .build());
        store.save(new TariffPlanBuilder("TAR-EARLY", "Commuter Early-Bird", TariffKind.EARLY_BIRD)
                .perHour(Money.of("30.00")).flatFee(Money.of("150.00"))
                .earlyBirdWindow(LocalTime.of(10, 0), LocalTime.of(16, 0))
                .graceMinutes(15).active(true)
                .build());
        store.save(new TariffPlanBuilder("TAR-SURGE", "Event Night Surge", TariffKind.EVENT_SURGE)
                .baseFee(Money.of("20.00")).perHour(Money.of("30.00"))
                .surgeMultiplier(new BigDecimal("1.5")).graceMinutes(15)
                .active(false)
                .build());
        store.save(new TariffPlanBuilder("TAR-MEMBER", "Monthly Member Pass", TariffKind.MEMBER_PASS)
                .perHour(Money.of("0.00")).graceMinutes(15)
                .active(true)
                .build());
    }

    static void seedMembers(MemberStore store) {
        LocalDate today = LocalDate.now();
        store.save(new Member("MEM-001", "Farhana Ahmed", "+8801711002201",
                Set.of("DHAKA-METRO-GA-31-4471"), "MONTHLY", today.plusDays(24)));
        store.save(new Member("MEM-002", "Kamrul Hasan", "+8801811002202",
                Set.of("DHAKA-METRO-GA-18-2093", "DHAKA-METRO-CHA-12-7754"), "MONTHLY",
                today.plusDays(11)));
        store.save(new Member("MEM-003", "Nusrat Jahan", "+8801911002203",
                Set.of("DHAKA-METRO-GA-45-8812"), "MONTHLY", today.plusDays(58)));
        store.save(new Member("MEM-004", "Rafiul Islam", "+8801611002204",
                Set.of("DHAKA-METRO-GA-07-1156"), "MONTHLY", today.minusDays(3))); // expired
    }

    static void seedBlacklist(PlateRegistry registry) {
        registry.blacklist("STOLEN-01");
        registry.blacklist("FINES-DUE-7");
    }

    // ------------------------------------------------------------------
    // Runtime seed: a week of history + today's live occupancy
    // ------------------------------------------------------------------

    static void seedRuntime(OccupancyLedger ledger, TicketStore tickets,
                            PlateRegistry plates, TicketNoGenerator ticketNos,
                            SimClock clock, TariffSelector selector) {
        LocalDate today = LocalDate.now();
        Instant weekStart = today.minusDays(6).atTime(LocalTime.of(6, 0))
                .atZone(ZoneId.systemDefault()).toInstant();

        // ---- six full days of exited tickets -----------------------------
        clock.fixAt(weekStart);
        for (int day = 0; day < 6; day++) {
            int exits = 24 + RANDOM.nextInt(10);
            for (int i = 0; i < exits; i++) {
                Instant entry = weekStart.plus(Duration.ofDays(day))
                        .plus(Duration.ofMinutes(RANDOM.nextInt(16 * 60)));
                Duration stay = Duration.ofMinutes(45 + RANDOM.nextInt(7 * 60));
                Instant exit = entry.plus(stay);
                VehicleType type = randomType();
                seedExitedTicket(tickets, ticketNos, selector, clock,
                        plate(day, i), type, entry, exit);
            }
        }

        // ---- recent exits: the last few hours before now ------------------
        Instant nowBaseline = Instant.now();
        for (int i = 0; i < 14; i++) {
            Duration stay = Duration.ofMinutes(45 + RANDOM.nextInt(7 * 60));
            Instant exit = nowBaseline.minus(Duration.ofMinutes(RANDOM.nextInt(180)));
            Instant entry = exit.minus(stay);
            seedExitedTicket(tickets, ticketNos, selector, clock,
                    plate(6, i), randomType(), entry, exit);
        }

        // ---- maintenance first: three bays out of service ------------------
        // (marked before occupancy so allocation never picks them)
        ledger.slot("L3-A-31").ifPresent(s -> ledger.markOutOfService(s.code()));
        ledger.slot("L3-A-32").ifPresent(s -> ledger.markOutOfService(s.code()));
        ledger.slot("L2-B-15").ifPresent(s -> ledger.markOutOfService(s.code()));

        // ---- live occupancy: cars inside right now -----------------------
        clock.resetToSystem();
        Instant now = clock.now();
        int liveCars = 46;
        for (int i = 0; i < liveCars; i++) {
            VehicleType type = i == 0 ? VehicleType.CAR : randomType();
            String plateNo = i == 0 ? "DHAKA-METRO-GA-31-4471" : livePlate(i); // first is a member
            Instant entry = now.minus(Duration.ofMinutes(60 + RANDOM.nextInt(5 * 60)));
            String ticketNo = ticketNoAt(ticketNos, clock, entry);
            clock.resetToSystem();
            var allocated = ledger.reserveFor(type, RANDOM.nextInt(20) == 0, plateNo, ticketNo, entry);
            if (allocated.isEmpty()) {
                continue;
            }
            Slot slot = allocated.get();
            Ticket ticket = new Ticket(ticketNo, plateNo, type, false, entry,
                    RANDOM.nextBoolean() ? "GATE-IN-1" : "GATE-IN-2", slot.code());
            ticket.confirmEntry();
            tickets.save(ticket);
            ledger.confirmArrival(slot, entry.plus(Duration.ofMinutes(3)));
            plates.markInside(plateNo);
        }

        // ---- a couple of reservations waiting on drivers ------------------
        Instant nowAgain = clock.now();
        for (int i = 0; i < 2; i++) {
            String ticketNo = ticketNos.next();
            var reserved = ledger.reserveFor(VehicleType.CAR, false,
                    "PENDING-0" + (i + 1), ticketNo, nowAgain);
            if (reserved.isPresent()) {
                Ticket ticket = new Ticket(ticketNo, "PENDING-0" + (i + 1), VehicleType.CAR,
                        false, nowAgain, "GATE-IN-1", reserved.get().code());
                ticket.confirmEntry();
                tickets.save(ticket);
                plates.markInside("PENDING-0" + (i + 1));
            }
        }

        clock.resetToSystem();
    }

    private static void seedExitedTicket(TicketStore tickets, TicketNoGenerator ticketNos,
                                         TariffSelector selector, SimClock clock,
                                         String plateNo, VehicleType type,
                                         Instant entry, Instant exit) {
        clock.fixAt(entry);
        String ticketNo = ticketNos.next();
        Ticket ticket = new Ticket(ticketNo, plateNo, type, false, entry,
                RANDOM.nextBoolean() ? "GATE-IN-1" : "GATE-IN-2",
                "L" + (1 + RANDOM.nextInt(3)) + "-" + "AB".charAt(RANDOM.nextInt(2))
                        + "-" + String.format("%02d", 1 + RANDOM.nextInt(24)));
        ticket.confirmEntry();

        clock.fixAt(exit);
        FeeRequest request = FeeRequest.of(entry, exit, type);
        TariffSelector.Selected selected = selector.select(request, false);
        BigDecimal amount = selected.strategy().compute(selected.plan(), request);
        com.parksense.tickets.FeeComponent chain = new com.parksense.tickets.addon.BaseParkingFee(
                amount, selected.strategy().explain(selected.plan(), request));
        int roll = RANDOM.nextInt(100);
        if (roll < 22) {
            chain = new CarWashAddon(chain, Money.of("120.00"));
        } else if (roll < 32) {
            chain = new ValetServiceAddon(chain, Money.of("200.00"));
        } else if (roll < 40 && type == VehicleType.EV) {
            chain = new EvChargeAddon(chain, new BigDecimal("18.5"), Money.of("22.00"));
        }
        ticket.setFee(chain, selected.plan().id(), selected.reason());

        PaymentMethod method = switch (RANDOM.nextInt(3)) {
            case 0 -> PaymentMethod.CASH;
            case 1 -> PaymentMethod.CARD;
            default -> PaymentMethod.MOBILE;
        };
        ticket.pay(new PaymentRecord(ticketNo, method, Money.round(chain.amount()),
                Money.round(chain.amount()), BigDecimal.ZERO, exit));
        ticket.completeExit(RANDOM.nextBoolean() ? "GATE-OUT-1" : "GATE-OUT-2", exit);
        tickets.save(ticket);
    }

    /** Ticket number as of a given moment (the generator is date-stamped). */
    private static String ticketNoAt(TicketNoGenerator ticketNos, SimClock clock, Instant at) {
        clock.fixAt(at);
        return ticketNos.next();
    }

    private static VehicleType randomType() {
        int roll = RANDOM.nextInt(100);
        if (roll < 60) {
            return VehicleType.CAR;
        }
        if (roll < 75) {
            return VehicleType.SUV;
        }
        if (roll < 87) {
            return VehicleType.MOTORCYCLE;
        }
        if (roll < 95) {
            return VehicleType.EV;
        }
        return VehicleType.VAN;
    }

    private static String plate(int day, int index) {
        return String.format("DHAKA-METRO-GA-%02d-%04d", 11 + (day * 7) % 30, 1000 + index * 37 + day * 131);
    }

    private static String livePlate(int index) {
        if (index % 9 == 0) {
            return "DHAKA-METRO-B-12-" + String.format("%04d", 7300 + index);
        }
        if (index % 11 == 0) {
            return "DHAKA-METRO-GA-30-" + String.format("%04d", 4100 + index * 3);
        }
        return "DHAKA-METRO-GA-" + String.format("%02d", 12 + index % 28) + "-"
                + String.format("%04d", 2200 + index * 53);
    }
}
