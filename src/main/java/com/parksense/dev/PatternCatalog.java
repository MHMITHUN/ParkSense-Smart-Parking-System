package com.parksense.dev;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-describing catalogue of every GoF pattern ParkSense implements —
 * the Patterns Guide page and the report's design chapter are both
 * generated from here, so documentation can never drift from the code.
 */
public final class PatternCatalog {

    private PatternCatalog() {
    }

    public static List<Map<String, Object>> catalog() {
        return List.of(
                entry("Singleton", "Creational",
                        "Exactly one occupancy ledger may exist, or two allocators would sell the same slot twice.",
                        List.of("occupancy.OccupancyLedger"),
                        "Every entry allocation, sensor confirmation and exit release writes the one ledger instance."),
                entry("Factory Method", "Creational",
                        "A lane's equipment stack (camera, barrier, printer) must be swappable between the demo simulation kit and vendor hardware without lane logic knowing which.",
                        List.of("gates.hardware.GateHardwareFactory",
                                "gates.hardware.SimulatedHardwareFactory",
                                "gates.hardware.VendorHardwareFactory"),
                        "AppConfig boots all four gates through SimulatedHardwareFactory; switching to VendorHardwareFactory is a one-line change."),
                entry("Builder", "Creational",
                        "A tariff plan has many interacting parts; a half-configured plan must be unrepresentable.",
                        List.of("tariff.builder.TariffPlanBuilder", "tariff.TariffPlan"),
                        "The Tariff Admin form posts raw fields; build() validates every invariant (cap ≥ base, grace 0–60, early-bird window) or refuses."),
                entry("Adapter", "Structural",
                        "Vendor SDKs (PlateSense camera, Parktron sensors, Parktron barrier) speak alien interfaces that must plug into ParkSense's ports.",
                        List.of("hardware.adapter.PlateSenseAdapter",
                                "hardware.adapter.ParktronSensorAdapter",
                                "hardware.adapter.ParktronBarrierAdapter"),
                        "The gate simulator's camera-read button pushes a vendor frame through PlateSenseAdapter into the PlateReader port."),
                entry("Decorator", "Structural",
                        "Add-on services (car wash, valet, EV top-up, lost-ticket penalty) must stack in any combination on any ticket.",
                        List.of("tickets.FeeComponent", "tickets.addon.BaseParkingFee",
                                "tickets.addon.CarWashAddon", "tickets.addon.ValetServiceAddon",
                                "tickets.addon.EvChargeAddon", "tickets.addon.LostTicketPenalty"),
                        "Checkout add-on toggles wrap the base fee; the receipt prints each decorator as its own line."),
                entry("Facade", "Structural",
                        "One entry touches chain, ledger, tariff, tickets, commands and boards — controllers must stay one call thin.",
                        List.of("app.OperationsFacade"),
                        "Every SPA button maps to exactly one facade method: simulateEntry(), requestExit(), payTicket(), forceBarrier()…"),
                entry("Proxy", "Structural",
                        "Emergency barrier force-open/close strands revenue; it must be ADMIN-only at the object boundary.",
                        List.of("gates.GateControl", "gates.BarrierController",
                                "guard.GateControlProxy"),
                        "An OPERATOR pressing force-open gets the proxy's refusal (403) and an audit line; ADMIN passes through."),
                entry("Composite", "Structural",
                        "The facility is a tree (lot → floors → zones → slots) that rollups, the live map and reports must walk uniformly.",
                        List.of("lot.LotNode", "lot.ParkingLot", "lot.Floor", "lot.Zone", "lot.Slot"),
                        "freeCount rollups and the Live Map render recurse over LotNode; report visitors accept() the same tree."),
                entry("Mediator", "Behavioral",
                        "Gates, kiosk, boards and sensor feeds need coordinated reactions to the same events without N×M coupling.",
                        List.of("controlroom.ParkingMediator", "controlroom.ControlRoomMediator",
                                "gates.EntryGate", "gates.ExitGate", "gates.PaymentKiosk",
                                "gates.SensorFeed"),
                        "The whole entry protocol — chain, allocation, ticket, print, barrier, board refresh — runs inside ControlRoomMediator; colleagues never reference each other."),
                entry("Observer", "Behavioral",
                        "Slot state changes must fan out to LED boards, the dashboard feed and capacity alerts, autonomously.",
                        List.of("occupancy.OccupancyEventPublisher", "occupancy.OccupancyListener",
                                "occupancy.board.EntryDisplayBoard", "occupancy.board.FloorBoard",
                                "occupancy.DashboardFeed", "occupancy.CapacityAlertWatcher"),
                        "Every ledger transition publishes one OccupancyEvent; boards re-render, the feed keeps the last 100, the watcher raises LOT FULL."),
                entry("Strategy", "Behavioral",
                        "Fee rules differ per business scenario and must be hot-swappable per ticket.",
                        List.of("tariff.strategy.TariffStrategy", "tariff.strategy.HourlyTariff",
                                "tariff.strategy.DailyCapTariff", "tariff.strategy.EarlyBirdTariff",
                                "tariff.strategy.EventSurgeTariff", "tariff.strategy.MemberPassTariff",
                                "tariff.TariffSelector"),
                        "At payment, TariffSelector picks the eligible active plan — member → pass, event → surge, commuter → early-bird, else capped hourly."),
                entry("Command", "Behavioral",
                        "Lane hardware instructions must be queued, audited and reversible.",
                        List.of("gates.command.GateCommand", "gates.command.OpenBarrierCommand",
                                "gates.command.CloseBarrierCommand", "gates.command.PrintTicketCommand",
                                "gates.command.ForceSignalCommand", "gates.command.GateCommandQueue"),
                        "Each entry enqueues print + open + close; the simulator panel lists history with Undo buttons that reverse barrier commands."),
                entry("Template Method", "Behavioral",
                        "All exit lanes share one pipeline whose order is a safety rule; only payment handling differs.",
                        List.of("exitlane.ExitProcessor", "exitlane.StaffedLaneProcessor",
                                "exitlane.ExpressMemberLaneProcessor", "exitlane.LostTicketProcessor"),
                        "EXIT-1 (staffed) collects cash/card; EXIT-2 waves members through at zero; the lost-ticket desk prices the flat penalty — same fixed steps."),
                entry("Chain of Responsibility", "Behavioral",
                        "Entry authorisation is an ordered list of independent rules; the lane must show the first failing one.",
                        List.of("entrycheck.EntryRuleHandler", "entrycheck.BlacklistHandler",
                                "entrycheck.DuplicateEntryHandler", "entrycheck.MemberRecognitionHandler",
                                "entrycheck.SlotAvailabilityHandler", "entrycheck.EvChargerHandler",
                                "entrycheck.CapacityAnnouncementHandler",
                                "entrycheck.EntryChainFactory"),
                        "Blacklisted plate STOLEN-01 is refused with VEHICLE BLOCKED; a motorcycle with no MC bay gets NO SLOT FOR THIS VEHICLE TYPE — verbatim on the display."),
                entry("State", "Behavioral",
                        "A ticket's legal operations depend on where it is in its lifecycle.",
                        List.of("tickets.state.TicketState", "tickets.state.IssuedState",
                                "tickets.state.ActiveState", "tickets.state.PaidState",
                                "tickets.state.ExitedState", "tickets.state.LostState",
                                "tickets.state.VoidState"),
                        "pay() on an EXITED ticket throws; overstay flips PAID → ACTIVE with a recalculated fare before the barrier opens."),
                entry("Visitor", "Behavioral",
                        "The lot tree and ticket fee chains must feed several reports without the domain growing per-report code.",
                        List.of("lot.SlotVisitor", "reports.visitor.OccupancyVisitor",
                                "reports.visitor.UtilizationVisitor", "tickets.TicketVisitor",
                                "reports.visitor.RevenueVisitor", "reports.visitor.PeakHourVisitor",
                                "reports.visitor.AddonsVisitor"),
                        "The Reports page renders four visitors' output tables; new reports mean a new visitor, not domain edits."));
    }

    private static Map<String, Object> entry(String name, String category, String intent,
                                             List<String> classes, String flow) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("category", category);
        row.put("intent", intent);
        row.put("classes", classes);
        row.put("flow", flow);
        return row;
    }
}
