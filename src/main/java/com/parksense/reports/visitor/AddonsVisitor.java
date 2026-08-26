package com.parksense.reports.visitor;

import com.parksense.tickets.FeeComponent;
import com.parksense.tickets.TicketVisitor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GoF Visitor over ticket fee chains: add-on service uptake and revenue —
 * which extras actually sell, at what margin line.
 */
public final class AddonsVisitor implements TicketVisitor {

    private final Map<String, BigDecimal[]> perLabel = new LinkedHashMap<>(); // label → [count, total]

    @Override
    public void visitFeeLine(FeeComponent.FeeLine line) {
        if (isAddon(line.label())) {
            BigDecimal[] row = perLabel.computeIfAbsent(line.label(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            row[0] = row[0].add(BigDecimal.ONE);
            row[1] = row[1].add(line.amount());
        }
    }

    private static boolean isAddon(String label) {
        String l = label.toLowerCase();
        return l.contains("wash") || l.contains("valet") || l.contains("ev top-up")
                || l.contains("penalty");
    }

    public List<List<String>> rows() {
        List<List<String>> rows = new ArrayList<>();
        perLabel.forEach((label, v) -> rows.add(
                List.of(label, v[0].toPlainString(), v[1].toPlainString())));
        return rows;
    }
}
