package com.parksense.reports.visitor;

import com.parksense.tickets.PaymentRecord;
import com.parksense.tickets.Ticket;
import com.parksense.tickets.TicketVisitor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GoF Visitor over exited tickets: revenue per day, split by payment
 * method. Accepted by each ticket in the report window.
 */
public final class RevenueVisitor implements TicketVisitor {

    /** date → [tickets, cash, card, mobile, total] */
    private final Map<LocalDate, BigDecimal[]> perDay = new LinkedHashMap<>();

    @Override
    public void visitTicket(Ticket ticket) {
        if (ticket.exitTime() == null) {
            return;
        }
        LocalDate day = LocalDate.from(java.time.ZonedDateTime.ofInstant(
                ticket.exitTime(), java.time.ZoneId.systemDefault()));
        BigDecimal[] row = perDay.computeIfAbsent(day,
                d -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO});
        row[0] = row[0].add(BigDecimal.ONE);
        for (PaymentRecord payment : ticket.payments()) {
            if (payment.method() == null) {
                continue;
            }
            switch (payment.method()) {
                case CASH -> row[1] = row[1].add(payment.amount());
                case CARD -> row[2] = row[2].add(payment.amount());
                case MOBILE -> row[3] = row[3].add(payment.amount());
            }
        }
        row[4] = row[4].add(ticket.totalPaid());
    }

    public List<List<String>> rows() {
        List<List<String>> rows = new ArrayList<>();
        perDay.forEach((day, v) -> rows.add(List.of(day.toString(),
                v[0].toPlainString(), v[1].toPlainString(), v[2].toPlainString(),
                v[3].toPlainString(), v[4].toPlainString())));
        return rows;
    }

    public List<String> seriesLabels() {
        return perDay.keySet().stream().map(LocalDate::toString).toList();
    }

    public List<Double> seriesValues() {
        return perDay.values().stream().map(v -> v[4].doubleValue()).toList();
    }
}
