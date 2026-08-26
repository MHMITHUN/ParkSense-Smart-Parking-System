package com.parksense.reports;

import com.parksense.common.Money;
import com.parksense.lot.ParkingLot;
import com.parksense.occupancy.OccupancyLedger;
import com.parksense.reports.visitor.AddonsVisitor;
import com.parksense.reports.visitor.OccupancyVisitor;
import com.parksense.reports.visitor.PeakHourVisitor;
import com.parksense.reports.visitor.RevenueVisitor;
import com.parksense.reports.visitor.UtilizationVisitor;
import com.parksense.store.TicketStore;
import com.parksense.tickets.Ticket;
import com.parksense.time.SimClock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the report visitors and shapes their output into
 * {@link ReportResult}s. The service knows windows and plumbing; all
 * aggregation logic lives in the visitors themselves.
 */
public final class ReportService {

    private final TicketStore tickets;
    private final OccupancyLedger ledger;
    private final SimClock clock;

    public ReportService(TicketStore tickets, OccupancyLedger ledger, SimClock clock) {
        this.tickets = tickets;
        this.ledger = ledger;
        this.clock = clock;
    }

    public ReportResult revenue(Instant from, Instant to) {
        RevenueVisitor visitor = new RevenueVisitor();
        tickets.exitedBetween(from, to).forEach(t -> t.accept(visitor));
        return ReportResult.chart("Revenue by day", from.toString(), to.toString(),
                List.of("Date", "Tickets", "Cash", "Card", "Mobile", "Total"),
                visitor.rows(), visitor.seriesLabels(), visitor.seriesValues());
    }

    public ReportResult occupancy() {
        OccupancyVisitor visitor = new OccupancyVisitor();
        lot().accept(visitor);
        return ReportResult.table("Occupancy by zone", "now", "now",
                List.of("Zone", "Total", "Occupied", "Reserved", "Free", "Out of service"),
                visitor.rows());
    }

    public ReportResult utilization() {
        UtilizationVisitor visitor = new UtilizationVisitor();
        lot().accept(visitor);
        return ReportResult.table("Utilization by floor", "now", "now",
                List.of("Floor", "Occupied", "Serviceable", "Utilization"),
                visitor.rows());
    }

    public ReportResult peakHours(Instant from, Instant to) {
        PeakHourVisitor visitor = new PeakHourVisitor();
        tickets.exitedBetween(from, to).forEach(t -> t.accept(visitor));
        return ReportResult.chart("Peak exit hours", from.toString(), to.toString(),
                List.of("Hour", "Exits"), labelValueRows(visitor.labels(), visitor.values()),
                visitor.labels(), visitor.values());
    }

    public ReportResult addons(Instant from, Instant to) {
        AddonsVisitor visitor = new AddonsVisitor();
        tickets.exitedBetween(from, to).forEach(t -> t.accept(visitor));
        return ReportResult.table("Add-on services", from.toString(), to.toString(),
                List.of("Service", "Count", "Revenue"), visitor.rows());
    }

    /** KPI tile values for the dashboard. */
    public Map<String, Object> dashboard() {
        ParkingLot lot = lot();
        LocalDate today = clock.today();
        Instant dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigDecimal todayRevenue = tickets.exitedBetween(dayStart, clock.now()).stream()
                .map(Ticket::totalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long membersInside = tickets.open().stream()
                .filter(t -> t.tariffExplain() != null && t.tariffExplain().contains("member"))
                .count();
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("lotName", lot.name());
        kpis.put("totalSlots", lot.totalSlots());
        kpis.put("freeSlots", lot.freeSlots());
        kpis.put("occupiedSlots", lot.slots(s -> s.state() == com.parksense.lot.SlotState.OCCUPIED).size());
        kpis.put("activeTickets", tickets.open().size());
        kpis.put("revenueToday", Money.round(todayRevenue).toPlainString());
        kpis.put("membersInside", membersInside);
        kpis.put("serverTime", clock.now().toString());
        return kpis;
    }

    private ParkingLot lot() {
        return ledger.lot();
    }

    private static List<List<String>> labelValueRows(List<String> labels, List<Double> values) {
        java.util.ArrayList<List<String>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            rows.add(List.of(labels.get(i), String.valueOf(values.get(i).intValue())));
        }
        return rows;
    }
}
