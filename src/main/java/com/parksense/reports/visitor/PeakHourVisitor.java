package com.parksense.reports.visitor;

import com.parksense.tickets.Ticket;
import com.parksense.tickets.TicketVisitor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * GoF Visitor over exited tickets: how busy each hour of the day is —
 * the exit-lane histogram the operator staffs against.
 */
public final class PeakHourVisitor implements TicketVisitor {

    private final int[] exitsPerHour = new int[24];

    @Override
    public void visitTicket(Ticket ticket) {
        if (ticket.exitTime() == null) {
            return;
        }
        LocalDateTime at = LocalDateTime.ofInstant(ticket.exitTime(), ZoneId.systemDefault());
        exitsPerHour[at.getHour()]++;
    }

    public List<String> labels() {
        List<String> labels = new ArrayList<>();
        for (int h = 6; h < 24; h++) {
            labels.add(String.format("%02d:00", h));
        }
        return labels;
    }

    public List<Double> values() {
        List<Double> values = new ArrayList<>();
        for (int h = 6; h < 24; h++) {
            values.add((double) exitsPerHour[h]);
        }
        return values;
    }
}
